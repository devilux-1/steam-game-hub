package com.steamhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.steamhub.config.NexusProperties;
import com.steamhub.entity.DTO.ModTranslationDTO;
import com.steamhub.entity.PO.NexusModCache;
import com.steamhub.mapper.NexusModCacheMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mod 汉化服务：调用 DeepSeek 把同步下来的英文标题与简介翻译成简体中文并写回缓存表。
 * 只补缺失字段，已有译文不会重复送翻；模型调用失败时保留英文，等下一次同步再补。
 */
@Slf4j
@Service
public class ModTranslationService {

    /** 简介只送翻前 200 个字符：卡片最多展示两行，同时控制单次请求长度 */
    private static final int SUMMARY_SOURCE_LIMIT = 200;

    /** 中文标题字段长度上限，与表结构一致 */
    private static final int NAME_LIMIT = 300;

    /** 中文简介字段长度上限，与表结构一致 */
    private static final int SUMMARY_LIMIT = 1000;

    /** 单批翻译的最大尝试次数：首次失败后退避重试一次 */
    private static final int MAX_ATTEMPTS = 2;

    /** 失败重试前的退避毫秒数 */
    private static final long RETRY_BACKOFF_MS = 1000L;

    /** 系统提示词：固定译者身份与术语表，并要求只输出 JSON 数组 */
    private static final String SYSTEM_PROMPT = """
            你是中文游戏 Mod 站点的本地化译者，负责把英文 Mod 标题和简介翻译成简体中文。
            规则：
            1. 标题必须让中文用户看得懂：工具名、缩写、作者名可以保留英文，但要补上中文说明，
               例如 SkyUI（天际界面）、RaceMenu（捏脸菜单）、Content Patcher（内容包修补器）；
               除纯人名或纯版本编号外，标题不能与英文原名完全相同；
            2. 术语统一：Trainer 译作修改器、Overlay 译作悬浮窗、Loader 译作加载器、
               Cheat Table 译作 CE 修改表、Save 译作存档、Inventory 译作物品栏；
            3. 简介译成一句通顺口语化的中文，不超过 60 字，不要保留 HTML 标签；
            4. 只输出 JSON 数组，元素形如 {"id":1,"nameCn":"中文标题","summaryCn":"中文简介"}，
               不要输出解释、不要加代码块围栏。
            """;

    /** Nexus 缓存数据访问接口 */
    private final NexusModCacheMapper nexusModCacheMapper;

    /** Nexus 配置，含批量条数与请求间隔 */
    private final NexusProperties properties;

    /** JSON 解析器（Jackson 3），由 Spring Boot 自动装配 */
    private final ObjectMapper objectMapper;

    /** 已完成系统提示词预置的对话客户端 */
    private final ChatClient chatClient;

    /** DeepSeek API Key，为空表示未配置，此时整体跳过翻译 */
    private final String apiKey;

    /** 未配置 Key 的提示只打一次，避免每次同步刷屏 */
    private boolean apiKeyWarned;

    /**
     * 构造翻译服务，预置系统提示词。
     *
     * @param nexusModCacheMapper 缓存表访问接口，非空
     * @param properties          Nexus 配置，非空
     * @param objectMapper        JSON 解析器，非空
     * @param chatClientBuilder   Spring AI 提供的对话客户端构造器，非空
     * @param apiKey              DeepSeek API Key，可为空（为空时不翻译）
     */
    public ModTranslationService(NexusModCacheMapper nexusModCacheMapper,
                                 NexusProperties properties,
                                 ObjectMapper objectMapper,
                                 ChatClient.Builder chatClientBuilder,
                                 @Value("${spring.ai.deepseek.api-key:}") String apiKey) {
        this.nexusModCacheMapper = nexusModCacheMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    /**
     * 翻译某个游戏下尚未汉化的 Mod 条目（标题与简介分别判断）。
     *
     * @param gameId     站内游戏 ID，非空
     * @param gameNameCn 游戏中文名，用于让模型译准游戏名，可空
     * @return 本次写入译文的条目数；未配置 Key、演示模式或无待翻译数据时返回 0
     */
    public int translateGame(Long gameId, String gameNameCn) {
        if (!isAvailable()) {
            return 0;
        }
        List<NexusModCache> rows = nexusModCacheMapper.selectList(new LambdaQueryWrapper<NexusModCache>()
                .eq(NexusModCache::getGameId, gameId)
                .and(wrapper -> wrapper.isNull(NexusModCache::getNameCn)
                        .or().isNull(NexusModCache::getSummaryCn)));
        if (rows.isEmpty()) {
            return 0;
        }

        int batchSize = Math.max(properties.getTranslateBatchSize(), 1);
        int translated = 0;
        for (int from = 0; from < rows.size(); from += batchSize) {
            List<NexusModCache> chunk = rows.subList(from, Math.min(from + batchSize, rows.size()));
            Map<Long, ModTranslationDTO> results = requestTranslation(chunk, gameNameCn);
            if (results.isEmpty()) {
                // 模型不可用时保留英文原文，等下一次同步再补，不影响已同步的数据
                log.warn("AI 翻译无可用结果，本批保留英文 gameId={} 起始下标={}", gameId, from);
            }
            translated += applyResults(chunk, results);
            sleepMillis(properties.getTranslateIntervalMs());
        }
        if (translated > 0) {
            log.info("Mod 汉化完成 gameId={} 条目={}", gameId, translated);
        }
        return translated;
    }

    /**
     * 判断翻译能力是否可用：未配置 Key 或处于 Nexus 演示模式时整体跳过。
     *
     * @return true 表示可以调用模型
     */
    private boolean isAvailable() {
        if (properties.isDemoMode()) {
            return false;
        }
        if (apiKey != null && !apiKey.isBlank()) {
            return true;
        }
        if (!apiKeyWarned) {
            log.warn("未配置 DEEPSEEK_API_KEY，跳过 Mod 汉化，页面展示英文原标题");
            apiKeyWarned = true;
        }
        return false;
    }

    /**
     * 把一批条目送给模型翻译，失败时退避重试一次。
     *
     * @param chunk      待翻译条目，非空
     * @param gameNameCn 游戏中文名，可空
     * @return 以缓存表主键为键的译文集合；请求失败或解析失败时返回空集合
     */
    private Map<Long, ModTranslationDTO> requestTranslation(List<NexusModCache> chunk, String gameNameCn) {
        String userMessage;
        try {
            userMessage = objectMapper.writeValueAsString(buildItems(chunk, gameNameCn));
        } catch (Exception e) {
            log.warn("翻译请求体序列化失败：{}", e.getMessage());
            return Map.of();
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String content = chatClient.prompt().user(userMessage).call().content();
                Map<Long, ModTranslationDTO> parsed = parse(content);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception e) {
                log.warn("AI 翻译请求失败（第 {} 次）：{}", attempt, e.getMessage());
            }
            sleepMillis(RETRY_BACKOFF_MS);
        }
        return Map.of();
    }

    /**
     * 组装送翻的条目列表：id 用于回填，game 帮助模型译准游戏名。
     *
     * @param chunk      待翻译条目，非空
     * @param gameNameCn 游戏中文名，可空
     * @return 可直接序列化为 JSON 数组的条目列表
     */
    private List<Map<String, Object>> buildItems(List<NexusModCache> chunk, String gameNameCn) {
        List<Map<String, Object>> items = new ArrayList<>(chunk.size());
        for (NexusModCache row : chunk) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("game", gameNameCn == null ? "" : gameNameCn);
            item.put("name", row.getName());
            if (row.getSummary() != null) {
                String summary = truncateSource(clean(row.getSummary()));
                // 已经清洗成空串的简介没有翻译价值，直接省略字段
                if (!summary.isEmpty()) {
                    item.put("summary", summary);
                }
            }
            items.add(item);
        }
        return items;
    }

    /**
     * 解析模型返回内容。
     *
     * @param content 模型输出的原始文本，可空
     * @return 以缓存表主键为键的译文集合；找不到 JSON 数组或解析失败时返回空集合
     */
    private Map<Long, ModTranslationDTO> parse(String content) {
        String json = extractJsonArray(content);
        if (json == null) {
            log.warn("AI 翻译响应中没有 JSON 数组，响应片段：{}", abbreviate(content));
            return Map.of();
        }
        List<ModTranslationDTO> parsed;
        try {
            parsed = objectMapper.readValue(json, new TypeReference<List<ModTranslationDTO>>() {
            });
        } catch (Exception e) {
            log.warn("AI 翻译响应解析失败：{}", e.getMessage());
            return Map.of();
        }
        Map<Long, ModTranslationDTO> results = new LinkedHashMap<>();
        for (ModTranslationDTO dto : parsed) {
            if (dto != null && dto.getId() != null) {
                results.put(dto.getId(), dto);
            }
        }
        return results;
    }

    /**
     * 从模型输出中截取 JSON 数组部分，兼容偶发的代码块围栏或前后说明文字。
     *
     * @param content 模型输出，可空
     * @return 首尾中括号之间的文本；不存在合法数组时返回 null
     */
    private String extractJsonArray(String content) {
        if (content == null) {
            return null;
        }
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return null;
        }
        return content.substring(start, end + 1);
    }

    /**
     * 把译文按主键回写到缓存表，只更新仍缺失的字段。
     *
     * @param chunk   本批条目，非空
     * @param results 本批译文，可空
     * @return 实际更新的条目数
     */
    private int applyResults(List<NexusModCache> chunk, Map<Long, ModTranslationDTO> results) {
        int updated = 0;
        for (NexusModCache row : chunk) {
            ModTranslationDTO dto = results.get(row.getId());
            if (dto == null) {
                continue;
            }
            NexusModCache update = new NexusModCache();
            update.setId(row.getId());
            boolean touched = false;
            if (row.getNameCn() == null) {
                String nameCn = postProcess(dto.getNameCn());
                if (!nameCn.isEmpty()) {
                    update.setNameCn(trimLength(nameCn, NAME_LIMIT));
                    touched = true;
                }
            }
            if (row.getSummaryCn() == null) {
                String summaryCn = postProcess(dto.getSummaryCn());
                if (!summaryCn.isEmpty()) {
                    update.setSummaryCn(trimLength(summaryCn, SUMMARY_LIMIT));
                    touched = true;
                }
            }
            if (!touched) {
                continue;
            }
            nexusModCacheMapper.updateById(update);
            updated++;
        }
        return updated;
    }

    /**
     * 清洗英文原文：去掉 Nexus 简介里常见的 HTML / BBCode 标记，压缩空白。
     *
     * @param text 原始文本，可空
     * @return 适合送翻的纯文本；没有英文字母时返回空串
     */
    private String clean(String text) {
        if (text == null) {
            return "";
        }
        String result = text
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\[/?[a-z*][^\\]]*\\]", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
        // 已是中文或纯符号的内容不需要翻译
        return result.matches(".*[A-Za-z].*") ? result : "";
    }

    /**
     * 截断过长的简介原文，尽量在词边界处断开。
     *
     * @param text 清洗后的原文，非空
     * @return 不超过 SUMMARY_SOURCE_LIMIT 的文本；超长时以省略号结尾
     */
    private String truncateSource(String text) {
        if (text.length() <= SUMMARY_SOURCE_LIMIT) {
            return text;
        }
        String cut = text.substring(0, SUMMARY_SOURCE_LIMIT);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > SUMMARY_SOURCE_LIMIT / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut + "…";
    }

    /**
     * 译文后处理：去掉首尾空白与包裹的引号。
     *
     * @param text 模型返回的译文，可空
     * @return 可直接入库的文本；为空时返回空串
     */
    private String postProcess(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "").trim();
    }

    /**
     * 按字段长度上限裁剪译文，防止超长内容写库失败。
     *
     * @param text  译文，非空
     * @param limit 字段最大字符数，必须大于 0
     * @return 裁剪后的文本
     */
    private String trimLength(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit);
    }

    /**
     * 截取响应片段用于日志，避免打印整段模型输出。
     *
     * @param content 模型输出，可空
     * @return 最多 120 字符的片段
     */
    private String abbreviate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= 120 ? content : content.substring(0, 120) + "...";
    }

    /**
     * 休眠指定毫秒，被中断时恢复中断标记让任务尽快结束。
     *
     * @param millis 休眠毫秒数，小于等于 0 时直接返回
     */
    private void sleepMillis(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
