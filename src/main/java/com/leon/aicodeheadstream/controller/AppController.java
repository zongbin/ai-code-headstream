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
import com.leon.aicodeheadstream.service.AppService;
import com.leon.aicodeheadstream.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
     * SSE 事件类型字段
     */
    private static final String EVENT_TYPE = "t";

    /**
     * SSE 事件数据字段
     */
    private static final String EVENT_DATA = "d";

    /**
     * SSE 事件类型：正常内容
     */
    private static final String EVENT_TYPE_CONTENT = "content";

    /**
     * SSE 事件类型：错误
     */
    private static final String EVENT_TYPE_ERROR = "error";

    /**
     * 错误信息最大长度
     */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;


    /**
     * 应用聊天生成代码（流式 SSE）
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        Flux<String> contentFlux;
        try {
            // 参数校验
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
            ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
            // 获取当前登录用户
            User loginUser = userService.getLoginUser(request);
            // 调用服务生成代码（流式）
            contentFlux = appService.chatToGenCode(appId, message, loginUser);
        } catch (Exception e) {
            // 流开始前就出错（如未登录、无权限、应用不存在），响应尚未提交，
            // 直接以 SSE 的方式把错误信息推给前端，保证前端能展示具体原因
            log.error("生成代码失败", e);
            return Flux.just(buildErrorEvent(e));
        }
        // 转换为 ServerSentEvent 格式
        return contentFlux
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(JSONUtil.toJsonStr(Map.of(EVENT_TYPE, EVENT_TYPE_CONTENT, EVENT_DATA, chunk)))
                        .build())
                // 流式过程中出错（AI 调用失败、代码解析/保存失败等），同样以 SSE 事件通知前端
                .onErrorResume(e -> {
                    log.error("生成代码失败", e);
                    return Flux.just(buildErrorEvent(e));
                });
    }

    /**
     * 构造错误事件
     *
     * @param e 异常（onErrorResume 回调里拿到的是 Throwable，所以这里用 Throwable 接收）
     * @return SSE 错误事件
     */
    private ServerSentEvent<String> buildErrorEvent(Throwable e) {
        String errorMessage = ErrorCode.SYSTEM_ERROR.getMessage();
        if (e instanceof BusinessException be && StrUtil.isNotBlank(be.getMessage())) {
            errorMessage = be.getMessage();
        } else if (e != null && StrUtil.isNotBlank(e.getMessage())) {
            errorMessage = e.getMessage();
        }
        // 截断过长的异常信息，避免刷屏
        if (errorMessage.length() > MAX_ERROR_MESSAGE_LENGTH) {
            errorMessage = errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...";
        }
        Map<String, String> wrapper = Map.of(
                EVENT_TYPE, EVENT_TYPE_ERROR,
                EVENT_DATA, errorMessage
        );
        return ServerSentEvent.<String>builder()
                .data(JSONUtil.toJsonStr(wrapper))
                .build();
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
     * 根据 id 修改自己的应用（目前只支持修改应用名称）
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
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
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
        return ResultUtils.success(getAppVOPage(appQueryRequest, pageNum, pageSize));
    }

    /**
     * 分页查询精选的应用列表（支持根据名称查询，每页最多 20 个）
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用分页
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > AppConstant.MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        appQueryRequest.setId(null);
        appQueryRequest.setCover(null);
        appQueryRequest.setInitPrompt(null);
        appQueryRequest.setCodeGenType(null);
        appQueryRequest.setDeployKey(null);
        appQueryRequest.setUserId(null);
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
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
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
