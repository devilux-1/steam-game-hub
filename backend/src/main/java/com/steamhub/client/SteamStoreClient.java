package com.steamhub.client;

import com.steamhub.config.SteamProperties;
import com.steamhub.entity.VO.StoreGameVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Steam 商店详情客户端：按 AppID 读取商店页的简介、截图与基础信息。
 * 只用于补全站内未收录的游戏，接口失败一律降级为返回 null，由调用方决定展示什么。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SteamStoreClient {

    /** 接口对非浏览器 UA 较敏感，这里固定用桌面版 Chrome 标识 */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    /** 商店详情页地址前缀，用于拼给前端的「在 Steam 商店打开」链接 */
    private static final String STORE_APP_URL = "https://store.steampowered.com/app/";

    /** 截图最多保留张数，避免单页塞入过多大图 */
    private static final int MAX_SCREENSHOTS = 8;

    /** Steam 配置 */
    private final SteamProperties properties;

    /** JSON 解析器（Jackson 3） */
    private final ObjectMapper objectMapper;

    /** HTTP 客户端，懒加载 */
    private HttpClient httpClient;

    /**
     * 读取某 AppID 的商店详情。
     *
     * @param appid Steam AppID，非空且大于 0
     * @return 商店信息；接口异常、无数据或 Steam 标记 success=false 时返回 null
     */
    public StoreGameVO fetch(Integer appid) {
        if (appid == null || appid <= 0) {
            return null;
        }
        String url = properties.getStoreApiUrl() + "?appids=" + appid + "&l=schinese";
        String body = sendGet(url, "商店详情 " + appid);
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode entry = root.get(String.valueOf(appid));
            if (entry == null) {
                return null;
            }
            JsonNode success = entry.get("success");
            if (success == null || !success.asBoolean(false)) {
                // 下架、区域限制或 AppID 非法时 Steam 会返回 success=false
                log.debug("Steam 商店详情无数据 appid={}", appid);
                return null;
            }
            JsonNode data = entry.get("data");
            if (data == null || data.isNull()) {
                return null;
            }
            return toVO(appid, data);
        } catch (RuntimeException e) {
            // 解析异常按「拿不到数据」处理，不影响详情页其它区块
            log.warn("Steam 商店详情解析失败 appid={}：{}", appid, e.getMessage());
            return null;
        }
    }

    /**
     * 把接口返回的 data 节点转换成展示对象。
     *
     * @param appid Steam AppID，非空
     * @param data  接口 data 节点，非空
     * @return 商店信息对象
     */
    private StoreGameVO toVO(Integer appid, JsonNode data) {
        StoreGameVO vo = new StoreGameVO();
        vo.setName(text(data, "name"));
        vo.setShortDescription(text(data, "short_description"));
        vo.setHeaderImage(text(data, "header_image"));
        vo.setStoreUrl(STORE_APP_URL + appid);

        JsonNode developers = data.get("developers");
        if (developers != null && developers.isArray()) {
            List<String> names = new ArrayList<>();
            for (JsonNode item : developers) {
                if (item.isString()) {
                    names.add(item.asString());
                }
            }
            vo.setDevelopers(String.join("、", names));
        }

        JsonNode genres = data.get("genres");
        if (genres != null && genres.isArray()) {
            List<String> labels = new ArrayList<>();
            for (JsonNode item : genres) {
                String description = text(item, "description");
                if (description != null) {
                    labels.add(description);
                }
            }
            vo.setGenres(labels);
        }

        JsonNode screenshots = data.get("screenshots");
        if (screenshots != null && screenshots.isArray()) {
            List<String> urls = new ArrayList<>();
            for (JsonNode item : screenshots) {
                String path = text(item, "path_full");
                if (path != null) {
                    urls.add(path);
                }
                if (urls.size() >= MAX_SCREENSHOTS) {
                    break;
                }
            }
            vo.setScreenshots(urls);
        }

        JsonNode releaseDate = data.get("release_date");
        if (releaseDate != null && !releaseDate.isNull()) {
            vo.setReleaseDate(text(releaseDate, "date"));
        }
        return vo;
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
}
