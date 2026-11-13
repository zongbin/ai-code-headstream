package com.leon.aicodeheadstream.service;

/**
 * 截图服务接口
 * 提供网页截图并上传的功能
 */
public interface ScreenshotService {

    /**
     * 根据指定的网页URL生成截图并上传
     *
     * @param webUrl 需要截图的网页URL
     * @return 截图上传后的访问URL或文件标识
     */
    String generateAndUploadScreenshot(String webUrl);
}
