package com.leon.aicodeheadstream.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leon.aicodeheadstream.langgraph4j.state.ImageCategoryEnum;
import com.leon.aicodeheadstream.langgraph4j.state.ImageResource;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Pexafy 的图片搜索工具。
 * <p>
 * Pexafy 是 Pexels / Unsplash / Pixabay 等免费图库的统一语义搜索代理，
 * 一次请求即可完成多图库检索、去重与结构归一化。
 * 文档：https://docs.pexafy.com
 */
@Slf4j
@Component
public class ImageSearchTool {

    @Value("${pexafy.api-key}")
    private String pexafyApiKey;

    /**
     * Pexafy 接口地址
     */
    private String url = "https://api.pexafy.com/api/v1/search/photos";

    @Tool("搜索内容相关的图片，用于网站内容展示")
    public List<ImageResource> searchContentImages(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        if (StrUtil.isBlank(query)) {
            return imageList;
        }
        try (HttpResponse response = HttpRequest.get(url)
                .header("x-api-key", pexafyApiKey)
                .form("q", query)
                .form("per_page", Math.max(1, 5))
                .form("sort_by", "relevance")
                .timeout(15000)
                .execute()) {
            String body = response.body();
            if (!response.isOk()) {
                log.error("Pexafy API 调用失败，状态码: {}，响应: {}", response.getStatus(), body);
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(body);
            // Pexafy 业务错误统一放在 error 字段中，HTTP 状态码仍为 200
            JSONObject error = result.getJSONObject("error");
            if (error != null) {
                log.error("Pexafy API 返回错误: {}", error.toString());
                return imageList;
            }
            JSONArray photos = result.getJSONArray("data");
            if (photos == null) {
                return imageList;
            }
            for (int i = 0; i < photos.size(); i++) {
                JSONObject photo = photos.getJSONObject(i);
                String imageUrl = extractUrl(photo);
                if (StrUtil.isBlank(imageUrl)) {
                    continue;
                }
                imageList.add(ImageResource.builder()
                        .category(ImageCategoryEnum.CONTENT)
                        .description(extractDescription(photo, query))
                        .url(imageUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("Pexafy API 调用失败: {}", e.getMessage(), e);
        }
        return imageList;
    }

    /**
     * 优先取 regular 尺寸（宽度 1280），其次依次降级
     */
    private String extractUrl(JSONObject photo) {
        JSONObject urls = photo.getJSONObject("urls");
        if (urls != null) {
            for (String size : new String[]{"regular", "large", "small", "thumb"}) {
                String url = urls.getStr(size);
                if (StrUtil.isNotBlank(url)) {
                    return url;
                }
            }
        }
        return photo.getStr("image_url");
    }

    /**
     * 优先取无障碍描述，其次 AI 生成描述，最后使用关键词兜底
     */
    private String extractDescription(JSONObject photo, String query) {
        for (String field : new String[]{"alt_description", "description", "source_description"}) {
            String value = photo.getStr(field);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return query;
    }
}
