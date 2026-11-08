package com.leon.aicodeheadstream.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目下载抽象服务
 * <p>
 * 该类定义了将项目目录打包成 ZIP 文件并提供下载的抽象方法。
 * 具体的下载实现类应继承此类并实现 {@link #downloadProjectAsZip} 方法。
 * </p>
 *
 * @author Leon
 * @since 1.0.0
 */
public abstract class ProjectDownloadService {

    /**
     * 将项目打包成 ZIP 文件并下载到客户端
     * <p>
     * 该方法将指定路径的项目目录打包成 ZIP 格式，并通过 HTTP 响应流
     * 将打包后的文件下载到客户端。下载文件名由参数指定。
     * </p>
     *
     * @param projectPath      项目的绝对路径，包含需要目录结构和文件
     * @param downloadFileName 下载的文件名
     * @param response           HTTP 响应对象，用于输出文件流
     * @throws IllegalArgumentException 如果路径无效
     * @throws RuntimeException             如果打包过程中发生
     * @see HttpServletResponse
     */
    public abstract void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
