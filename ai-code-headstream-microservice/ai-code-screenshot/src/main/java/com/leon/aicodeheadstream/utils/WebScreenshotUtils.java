package com.leon.aicodeheadstream.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class WebScreenshotUtils {

    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

    /**
     * 同时存活的 Chrome 实例上限。
     * 建议按机器内存设置：单 Chrome 约 200~500MB，8G 机器建议 <= 5。
     * 超过上限的线程会在 acquire 处等待，而不是无限起 Chrome 把机器打爆。
     */
    private static final int MAX_CONCURRENT_DRIVERS = 5;

    /**
     * 等待浏览器资源的超时时间，避免高并发下线程无限阻塞。
     */
    private static final long ACQUIRE_TIMEOUT_SECONDS = 30;

    private static final Semaphore DRIVER_SEMAPHORE = new Semaphore(MAX_CONCURRENT_DRIVERS, true);

    private static final AtomicBoolean WDM_INITIALIZED = new AtomicBoolean(false);

    private WebScreenshotUtils() {
        // 工具类禁止实例化
    }

    /**
     * 生成网页截图（线程安全）。
     * <p>
     * 每次调用独立创建并销毁 WebDriver，天然隔离 Cookie / localStorage / 页面状态，
     * 同时通过信号量限制并发 Chrome 数量。
     *
     * @param webUrl 网页URL
     * @return 压缩后的截图文件路径，失败返回 null
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页URL不能为空");
            return null;
        }

        boolean acquired = false;
        WebDriver driver = null;
        File rootDir = null;

        try {
            // 1. 限流：拿不到就等，等不到就放弃，避免线程无限堆积
            acquired = DRIVER_SEMAPHORE.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取浏览器资源超时，当前并发已达上限 {}, url={}", MAX_CONCURRENT_DRIVERS, webUrl);
                return null;
            }

            // 2. 每个任务独立目录，避免并发写同一目录
            String rootPath = System.getProperty("user.dir") + File.separator
                    + "tmp" + File.separator + "screenshots" + File.separator
                    + UUID.randomUUID().toString().substring(0, 8);
            rootDir = FileUtil.mkdir(rootPath);

            // 3. 独立 Driver，状态天然隔离
            driver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT, webUrl);

            driver.get(webUrl);
            waitForPageLoad(driver);

            String imageSavePath = rootPath + File.separator
                    + RandomUtil.randomNumbers(5) + ".png";
            String compressedImagePath = rootPath + File.separator
                    + RandomUtil.randomNumbers(5) + "_compressed.jpg";

            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功: {}", imageSavePath);

            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功: {}", compressedImagePath);

            FileUtil.del(imageSavePath);
            return compressedImagePath;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待浏览器资源被中断, url={}", webUrl, e);
            if (rootDir != null) {
                FileUtil.del(rootDir);
            }
            return null;
        } catch (Exception e) {
            log.error("网页截图失败: {}", webUrl, e);
            // 4. 失败时清理临时目录，避免残留垃圾文件
            if (rootDir != null) {
                FileUtil.del(rootDir);
            }
            return null;
        } finally {
            // 5. 无论成功失败，Driver 必须退出
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("关闭 Chrome 失败", e);
                }
            }
            // 6. 释放信号量
            if (acquired) {
                DRIVER_SEMAPHORE.release();
            }
        }
    }

    /**
     * WebDriverManager 只初始化一次。
     * 用双重检查避免多线程并发首次调用时重复下载/解压 driver。
     */
    private static void ensureDriverManagerReady() {
        if (!WDM_INITIALIZED.get()) {
            synchronized (WebScreenshotUtils.class) {
                if (!WDM_INITIALIZED.get()) {
                    WebDriverManager.chromedriver().setup();
                    WDM_INITIALIZED.set(true);
                    log.info("WebDriverManager 初始化完成");
                }
            }
        }
    }

    /**
     * 初始化 Chrome 浏览器驱动。
     * 注意：调用方负责 quit，本方法不缓存 Driver。
     */
    private static WebDriver initChromeDriver(int width, int height, String webUrl) {
        try {
            // 自动管理 ChromeDriver
            ensureDriverManagerReady();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless=new");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--screen-info={0,0 %dx%d}", width, height));
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 忽略证书错误，并放行目标 HTTP 源，避免无头模式拦截明文地址导致截图空白
            options.addArguments("--ignore-certificate-errors");
            try {
                URI uri = new URI(webUrl);
                String origin = uri.getPort() > 0
                        ? uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort()
                        : uri.getScheme() + "://" + uri.getHost();
                options.addArguments("--unsafely-treat-insecure-origin-as-secure=" + origin);
            } catch (Exception ignore) {
                // 解析失败则不加该参数
            }
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;

        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 保存图片到文件。
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片。
     */
    private static void compressImage(String originalImagePath, String compressedImagePath) {
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败: {} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    /**
     * 等待页面加载完成。
     */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(webDriver ->
                    "complete".equals(
                            ((JavascriptExecutor) webDriver)
                                    .executeScript("return document.readyState"))
            );
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待页面加载被中断");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }
}