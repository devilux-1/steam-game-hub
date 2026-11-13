package com.steamhub.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steamhub.config.NexusProperties;
import com.steamhub.entity.DTO.NexusModDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Nexus Mods API 客户端。
 * 使用 JDK 内置 HttpClient，避免第三方客户端与 CDN 的兼容性问题；
 * 处于演示模式时不发起网络请求，直接返回内置示例数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NexusClient {

    /** 热度接口路径模板，取该游戏当前最热门的 Mod */
    private static final String TRENDING_PATH = "/v1/games/%s/mods/trending.json";

    /** Nexus 页面地址模板，用于在响应未给出详情页地址时自行拼接 */
    private static final String MOD_PAGE_TEMPLATE = "https://www.nexusmods.com/%s/mods/%d";

    /** 演示模式下的示例 Mod 数量 */
    private static final int MOCK_MOD_COUNT = 6;

    /** Nexus 配置 */
    private final NexusProperties properties;

    /** JSON 解析器，由 Spring Boot 自动装配 */
    private final ObjectMapper objectMapper;

    /** HTTP 客户端，连接超时在创建时固定，读超时按请求设置 */
    private HttpClient httpClient;

    /**
     * 拉取指定游戏的热门 Mod 列表。
     *
     * @param domain 游戏在 Nexus 的 URL 标识，如 eldenring，非空
     * @param limit  最多返回条数，必须大于 0
     * @return Mod 列表，按接口返回顺序；无数据时返回空列表
     * @throws NexusApiException 请求失败、鉴权失败或响应无法解析时
     */
    public List<NexusModDTO> fetchTrendingMods(String domain, int limit) {
        if (properties.isDemoMode()) {
            return buildMockMods(domain, limit);
        }
        String url = properties.getBaseUrl() + String.format(TRENDING_PATH, domain);
        String body = sendGet(url, domain);
        return parseMods(body, domain, limit);
    }

    /**
     * 发送带鉴权头的 GET 请求。
     *
     * @param url    完整请求地址，非空
     * @param domain 游戏标识，仅用于异常信息
     * @return 响应体字符串
     * @throws NexusApiException 网络异常或 HTTP 状态非 200 时
     */
    private String sendGet(String url, String domain) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("apikey", properties.getKey())
                .header("Application-Name", properties.getAppName())
                .header("Application-Version", properties.getAppVersion())
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client().send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 200) {
                return response.body();
            }
            // 把常见状态码翻译成可执行的排查提示，方便运维定位
            throw new NexusApiException(String.format(
                    "Nexus 请求失败 domain=%s status=%d 原因=%s", domain, status, describeStatus(status)));
        } catch (InterruptedException e) {
            // 恢复中断标记，避免吞掉取消信号
            Thread.currentThread().interrupt();
            throw new NexusApiException("Nexus 请求被中断 domain=" + domain, e);
        } catch (java.io.IOException e) {
            throw new NexusApiException("Nexus 网络异常 domain=" + domain + " 原因=" + e.getMessage(), e);
        }
    }

    /**
     * 把 HTTP 状态码翻译为排查提示。
     *
     * @param status HTTP 状态码
     * @return 中文提示，未知状态返回通用描述
     */
    private String describeStatus(int status) {
        return switch (status) {
            case 401 -> "API Key 无效或已失效";
            case 403 -> "请求被拒绝，可能缺少 Application-Name 头或被 Cloudflare 拦截";
            case 404 -> "该游戏在 Nexus 不存在或 domain 填写错误";
            case 429 -> "超出接口配额，请降低同步频率";
            default -> "未预期的响应状态";
        };
    }

    /**
     * 解析接口响应为 Mod 列表。
     * Nexus 不同接口的字段名存在差异，这里按候选名逐个尝试，避免接口微调导致整批失败。
     *
     * @param body   响应体 JSON 字符串，非空
     * @param domain 游戏标识，用于拼接详情页地址
     * @param limit  最多解析条数，必须大于 0
     * @return Mod 列表；没有可用条目时返回空列表
     * @throws NexusApiException 响应不是合法 JSON 数组时
     */
    private List<NexusModDTO> parseMods(String body, String domain, int limit) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new NexusApiException("Nexus 响应解析失败 domain=" + domain, e);
        }
        if (root == null || !root.isArray()) {
            throw new NexusApiException("Nexus 响应格式异常 domain=" + domain + " 期望数组");
        }
        List<NexusModDTO> mods = new ArrayList<>();
        for (JsonNode node : root) {
            if (mods.size() >= limit) {
                break;
            }
            NexusModDTO dto = toDTO(node, domain);
            // 缺少 mod id 的条目无法唯一定位，直接跳过
            if (dto != null) {
                mods.add(dto);
            }
        }
        return mods;
    }

    /**
     * 把单个 JSON 节点转换为 Mod 传输对象。
     *
     * @param node   条目节点，非空
     * @param domain 游戏标识，用于拼接详情页地址
     * @return 转换结果；缺少 mod id 时返回 null
     */
    private NexusModDTO toDTO(JsonNode node, String domain) {
        Integer modId = firstInt(node, "mod_id", "id");
        if (modId == null) {
            return null;
        }
        NexusModDTO dto = new NexusModDTO();
        dto.setModId(modId);
        dto.setName(firstText(node, "name", "mod_name"));
        dto.setAuthor(firstText(node, "author", "uploaded_by"));
        dto.setEndorsements(firstInt(node, "endorsement_count", "endorsements"));
        dto.setDownloads(firstLong(node, "downloads", "download_count"));
        dto.setSummary(firstText(node, "summary", "description"));
        String modUrl = firstText(node, "mod_page_url");
        dto.setModUrl(modUrl == null ? String.format(MOD_PAGE_TEMPLATE, domain, modId) : modUrl);
        return dto;
    }

    /**
     * 演示模式：生成可预期的示例数据，便于在无 API Key 时验证同步与展示链路。
     *
     * @param domain 游戏标识，用于生成可区分的名称
     * @param limit  最多返回条数，必须大于 0
     * @return 示例 Mod 列表
     */
    private List<NexusModDTO> buildMockMods(String domain, int limit) {
        int count = Math.min(limit, MOCK_MOD_COUNT);
        List<NexusModDTO> mods = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            NexusModDTO dto = new NexusModDTO();
            dto.setModId(9000 + i);
            dto.setName(domain + " 演示 Mod " + i);
            dto.setAuthor("Demo Author " + i);
            // 点赞数递减，保证排序稳定且可预期
            dto.setEndorsements(1200 - i * 100);
            dto.setDownloads(80000L - i * 5000L);
            dto.setSummary("这是演示数据：配置 Nexus API Key 后将替换为真实热门 Mod。");
            dto.setModUrl(String.format(MOD_PAGE_TEMPLATE, domain, dto.getModId()));
            mods.add(dto);
        }
        log.debug("Nexus 演示模式返回 {} 条示例数据 domain={}", mods.size(), domain);
        return mods;
    }

    /**
     * 懒加载 HTTP 客户端，避免在配置未就绪时创建连接池。
     *
     * @return 复用的 HttpClient 实例
     */
    private HttpClient client() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        return httpClient;
    }

    /**
     * 按候选字段名依次读取字符串。
     *
     * @param node   条目节点，非空
     * @param fields 候选字段名，按优先级排列
     * @return 首个非空白字段值；都不存在时返回 null
     */
    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    /**
     * 按候选字段名依次读取整数。
     *
     * @param node   条目节点，非空
     * @param fields 候选字段名，按优先级排列
     * @return 首个可解析的整数值；都不存在时返回 null
     */
    private Integer firstInt(JsonNode node, String... fields) {
        Long value = firstLong(node, fields);
        return value == null ? null : value.intValue();
    }

    /**
     * 按候选字段名依次读取长整数，兼容数值与字符串两种返回形式。
     *
     * @param node   条目节点，非空
     * @param fields 候选字段名，按优先级排列
     * @return 首个可解析的长整数值；都不存在时返回 null
     */
    private Long firstLong(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.asLong();
            }
            if (value.isTextual()) {
                try {
                    return Long.parseLong(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    // 字段存在但不是数字，继续尝试下一个候选名
                }
            }
        }
        return null;
    }
}
