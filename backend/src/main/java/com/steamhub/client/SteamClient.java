package com.steamhub.client;

import com.steamhub.config.SteamProperties;
import com.steamhub.entity.DTO.SteamOwnedGameDTO;
import com.steamhub.entity.DTO.SteamPlayerDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Steam 接口客户端：拼 OpenID 登录地址、校验回调断言、读取玩家资料与拥有游戏。
 * 使用 JDK HttpClient，配置了代理时全部请求经代理发出（国内访问 OpenID 必须如此）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SteamClient {

    /** OpenID 2.0 命名空间 */
    private static final String OPENID_NS = "http://specs.openid.net/auth/2.0";

    /** OpenID 的 identifier_select 占位值，表示由 Steam 决定身份 */
    private static final String IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";

    /** claimed_id 中的 SteamID64 片段，17 位数字 */
    private static final Pattern STEAM_ID_PATTERN = Pattern.compile("steamcommunity\\.com/openid/id/(\\d{17})");

    /** 接口对非浏览器 UA 较敏感，这里固定用桌面版 Chrome 标识 */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    /** 本地时区，Steam 返回的时间戳按该时区换算成 LocalDateTime */
    private static final ZoneId ZONE = ZoneId.systemDefault();

    /** Steam 配置 */
    private final SteamProperties properties;

    /** JSON 解析器（Jackson 3） */
    private final ObjectMapper objectMapper;

    /** HTTP 客户端，懒加载 */
    private HttpClient httpClient;

    /**
     * 构造跳转 Steam 的登录地址。
     *
     * @param state 随机串，用于回调时防 CSRF，非空
     * @return 完整的 OpenID 登录地址
     */
    public String buildLoginUrl(String state) {
        return UriComponentsBuilder.fromUriString(properties.getOpenidUrl())
                .queryParam("openid.ns", OPENID_NS)
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", properties.getCallbackUrl() + "?state=" + state)
                .queryParam("openid.realm", realm())
                .queryParam("openid.identity", IDENTIFIER_SELECT)
                .queryParam("openid.claimed_id", IDENTIFIER_SELECT)
                .build()
                .toUriString();
    }

    /**
     * 把回调参数原样交回 Steam 校验断言是否真实。
     *
     * @param params 回调收到的全部 OpenID 参数，非空
     * @return true 表示 Steam 返回 is_valid:true
     */
    public boolean verifyAssertion(Map<String, String> params) {
        StringBuilder form = new StringBuilder("openid.mode=check_authentication");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            // state 是本站自定义参数、mode 已由上面固定为 check_authentication，
            // 两者都不能回传：重复的 mode 会让 Steam 按 id_res 解析并判定签名无效
            if ("state".equals(entry.getKey()) || "openid.mode".equals(entry.getKey())
                    || entry.getValue() == null) {
                continue;
            }
            form.append('&')
                    .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getOpenidUrl()))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(form.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client().send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("OpenID 校验请求失败 status={}", response.statusCode());
                return false;
            }
            if (!response.body().contains("is_valid:true")) {
                log.warn("OpenID 校验返回无效：{}", abbreviate(response.body()));
            }
            return response.body().contains("is_valid:true");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            log.warn("OpenID 校验请求异常：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 claimed_id 中提取 SteamID64。
     *
     * @param claimedId 回调参数 openid.claimed_id，可空
     * @return 17 位 SteamID64；格式不匹配时返回 null
     */
    public String extractSteamId(String claimedId) {
        if (claimedId == null) {
            return null;
        }
        Matcher matcher = STEAM_ID_PATTERN.matcher(claimedId);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 读取玩家昵称与头像。
     *
     * @param steamId SteamID64，非空
     * @return 玩家资料；接口无数据或 Key 未配置时返回 null
     */
    public SteamPlayerDTO fetchPlayerSummary(String steamId) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return null;
        }
        String url = properties.getApiBaseUrl() + "/ISteamUser/GetPlayerSummaries/v2/?key="
                + properties.getApiKey() + "&steamids=" + steamId;
        String body = sendGet(url, "GetPlayerSummaries");
        if (body == null) {
            return null;
        }
        JsonNode players = objectMapper.readTree(body).path("response").path("players");
        if (!players.isArray() || players.isEmpty()) {
            return null;
        }
        JsonNode node = players.get(0);
        SteamPlayerDTO dto = new SteamPlayerDTO();
        dto.setSteamId(steamId);
        dto.setPersonaName(text(node, "personaname"));
        dto.setAvatarUrl(text(node, "avatarfull"));
        return dto;
    }

    /**
     * 读取用户拥有的游戏列表。
     *
     * @param steamId SteamID64，非空
     * @return 游戏列表，按 Steam 返回顺序；接口失败或返回空对象时返回空列表
     */
    public List<SteamOwnedGameDTO> fetchOwnedGames(String steamId) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return List.of();
        }
        String url = properties.getApiBaseUrl() + "/IPlayerService/GetOwnedGames/v1/?key="
                + properties.getApiKey() + "&steamid=" + steamId
                + "&include_appinfo=1&include_played_free_games=1";
        String body = sendGet(url, "GetOwnedGames");
        if (body == null) {
            return List.of();
        }
        JsonNode games = objectMapper.readTree(body).path("response").path("games");
        // 用户把「游戏详情」设为私密时，Steam 返回 {"response":{}}，这里按空列表处理
        if (!games.isArray()) {
            return List.of();
        }
        List<SteamOwnedGameDTO> result = new ArrayList<>(games.size());
        for (JsonNode node : games) {
            SteamOwnedGameDTO dto = new SteamOwnedGameDTO();
            dto.setAppid(node.path("appid").asInt());
            dto.setName(text(node, "name"));
            dto.setPlaytimeForever(node.path("playtime_forever").asInt(0));
            dto.setPlaytime2weeks(node.path("playtime_2weeks").asInt(0));
            long lastPlayed = node.path("rtime_last_played").asLong(0);
            // 从未游玩时 Steam 返回 0，按空处理便于前端展示「从未游玩」
            dto.setLastPlayedAt(lastPlayed > 0
                    ? LocalDateTime.ofInstant(Instant.ofEpochSecond(lastPlayed), ZONE)
                    : null);
            result.add(dto);
        }
        return result;
    }

    /**
     * 发送 GET 请求。
     *
     * @param url  完整地址，非空
     * @param what 接口名，仅用于日志
     * @return 响应体；网络异常或状态非 200 时返回 null
     */
    private String sendGet(String url, String what) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client().send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                return response.body();
            }
            log.warn("Steam {} 请求失败 status={}", what, response.statusCode());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            log.warn("Steam {} 网络异常：{}", what, e.getMessage());
            return null;
        }
    }

    /**
     * 读取字符串字段，缺失或为 null 时返回 null。
     *
     * @param node  节点，非空
     * @param field 字段名，非空
     * @return 字段值；不存在时返回 null
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    /**
     * 计算 OpenID realm：取回调地址的协议、主机与端口。
     *
     * @return 形如 http://localhost:8080 的字符串
     */
    private String realm() {
        URI uri = URI.create(properties.getCallbackUrl());
        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        return uri.getScheme() + "://" + uri.getHost() + port;
    }

    /**
     * 懒加载 HTTP 客户端；配置了代理时所有请求经代理发出。
     *
     * @return 复用的 HttpClient 实例
     */
    private HttpClient client() {
        if (httpClient == null) {
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    // 本地代理隧道只协商 HTTP/1.1，默认的 HTTP/2 会在握手阶段被中断
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL);
            if (properties.getProxyHost() != null && !properties.getProxyHost().isBlank()
                    && properties.getProxyPort() > 0) {
                builder.proxy(ProxySelector.of(
                        new InetSocketAddress(properties.getProxyHost(), properties.getProxyPort())));
            }
            httpClient = builder.build();
        }
        return httpClient;
    }

    /**
     * 截断日志内容，避免把整段响应打出来。
     *
     * @param text 原始文本，可空
     * @return 最多 200 字符的片段
     */
    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() <= 200 ? flat : flat.substring(0, 200) + "...";
    }
}
