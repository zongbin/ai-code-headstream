package com.leon.aicodeheadstream.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.leon.aicodeheadstream.annotation.AuthCheck;
import com.leon.aicodeheadstream.common.BaseResponse;
import com.leon.aicodeheadstream.common.ResultUtils;
import com.leon.aicodeheadstream.constant.AppConstant;
import com.leon.aicodeheadstream.constant.UserConstant;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.exception.ThrowUtils;
import com.leon.aicodeheadstream.model.dto.DeleteRequest;
import com.leon.aicodeheadstream.model.dto.app.*;
import com.leon.aicodeheadstream.model.entity.App;
import com.leon.aicodeheadstream.model.entity.User;
import com.leon.aicodeheadstream.model.vo.AppVO;
import com.leon.aicodeheadstream.ratelimit.annotation.RateLimit;
import com.leon.aicodeheadstream.ratelimit.enums.RateLimitType;
import com.leon.aicodeheadstream.service.AppService;
import com.leon.aicodeheadstream.service.ProjectDownloadService;
import com.leon.aicodeheadstream.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 控制层。
 *
 * @author leon
 */
@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    /**
     * 错误信息最大长度
     */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;


    /**
     * 应用聊天生成代码（流式 SSE）
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 2, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务生成代码（流式）
        Flux<String> content = appService.chatToGenCode(appId, message, loginUser);
        return content
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(JSONUtil.toJsonStr(Map.of("d", chunk)))
                        .build())
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build())
                );
    }


    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 下载应用代码
     *
     * @param appId    应用 ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

    /**
     * 创建应用（须填写 initPrompt）
     *
     * @param appAddRequest 创建请求
     * @param request       请求
     * @return 新应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long appId = appService.createApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
    }

    /**
     * 根据 id 修改自己的应用（支持修改应用名称、可见状态）
     *
     * @param appUpdateRequest 更新请求
     * @param request          请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = appService.updateMyApp(appUpdateRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 删除自己的应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = appService.deleteMyApp(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 查看应用详情
     * <p>
     * 公开应用所有人可见，私有应用只有创建者和管理员可见。
     *
     * @param id      应用 id
     * @param request 请求
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 可见性校验
        User loginUser = userService.getLoginUser(request);
        appService.checkAppVisible(app, loginUser);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表（支持根据名称查询，每页最多 20 个）
     *
     * @param appQueryRequest 查询请求
     * @param request         请求
     * @return 应用分页
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > AppConstant.MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        appQueryRequest.setUserId(loginUser.getId());
        // 用户侧只按名称筛选，避免传入其他条件扩大查询范围
        appQueryRequest.setId(null);
        appQueryRequest.setCover(null);
        appQueryRequest.setInitPrompt(null);
        appQueryRequest.setCodeGenType(null);
        appQueryRequest.setDeployKey(null);
        appQueryRequest.setPriority(null);
        appQueryRequest.setVisible(null);
        appQueryRequest.setLabel(null);
        return ResultUtils.success(getAppVOPage(appQueryRequest, pageNum, pageSize));
    }

    /**
     * 分页查询案例广场的应用列表（所有用户公开的应用，按优先级从大到小排序，每页最多 20 个）
     *
     * <p>支持按标签（label）筛选，不传或传空表示查询全部标签。</p>
     *
     * @param appQueryRequest 查询请求
     * @return 案例广场应用分页
     */
    @PostMapping("/square/list/page/vo")
    @Cacheable(
            value = "square_app_page",
            key = "T(com.leon.aicodeheadstream.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listSquareAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > AppConstant.MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 案例广场只展示公开的应用
        appQueryRequest.setVisible(AppConstant.APP_VISIBLE_PUBLIC);
        appQueryRequest.setId(null);
        appQueryRequest.setCover(null);
        appQueryRequest.setInitPrompt(null);
        appQueryRequest.setCodeGenType(null);
        appQueryRequest.setDeployKey(null);
        appQueryRequest.setPriority(null);
        appQueryRequest.setUserId(null);
        // 精选应用（优先级高）排在最前面
        appQueryRequest.setSortField("priority");
        appQueryRequest.setSortOrder("descend");
        return ResultUtils.success(getAppVOPage(appQueryRequest, pageNum, pageSize));
    }

    /**
     * 管理员根据 id 删除任意应用
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        boolean result = appService.deleteAppByAdmin(id);
        return ResultUtils.success(result);
    }

    /**
     * 管理员根据 id 更新任意应用（支持更新应用名称、应用封面、优先级）
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 是否更新成功
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = appService.updateAppByAdmin(appAdminUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询应用列表（支持根据除时间外的任何字段查询，每页数量不限）
     *
     * @param appQueryRequest 查询请求
     * @return 应用分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        return ResultUtils.success(getAppVOPage(appQueryRequest, pageNum, pageSize));
    }

    /**
     * 管理员根据 id 查看应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询并封装应用视图
     *
     * @param appQueryRequest 查询条件
     * @param pageNum         页号
     * @param pageSize        页大小
     * @return 应用视图分页
     */
    private Page<AppVO> getAppVOPage(AppQueryRequest appQueryRequest, long pageNum, long pageSize) {
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }
}
