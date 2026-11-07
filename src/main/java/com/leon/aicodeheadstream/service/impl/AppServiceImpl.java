package com.leon.aicodeheadstream.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.leon.aicodeheadstream.constant.AppConstant;
import com.leon.aicodeheadstream.constant.UserConstant;
import com.leon.aicodeheadstream.core.AiCodeGeneratorFacade;
import com.leon.aicodeheadstream.core.builder.VueProjectBuilder;
import com.leon.aicodeheadstream.core.handler.StreamHandlerExecutor;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.exception.ThrowUtils;
import com.leon.aicodeheadstream.model.dto.app.AppAddRequest;
import com.leon.aicodeheadstream.model.dto.app.AppAdminUpdateRequest;
import com.leon.aicodeheadstream.model.dto.app.AppQueryRequest;
import com.leon.aicodeheadstream.model.dto.app.AppUpdateRequest;
import com.leon.aicodeheadstream.model.entity.User;
import com.leon.aicodeheadstream.model.enums.AppLabelEnum;
import com.leon.aicodeheadstream.model.enums.ChatHistoryMessageTypeEnum;
import com.leon.aicodeheadstream.model.enums.CodeGenTypeEnum;
import com.leon.aicodeheadstream.model.vo.AppVO;
import com.leon.aicodeheadstream.model.vo.UserVO;
import com.leon.aicodeheadstream.service.ChatHistoryService;
import com.leon.aicodeheadstream.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.leon.aicodeheadstream.model.entity.App;
import com.leon.aicodeheadstream.mapper.AppMapper;
import com.leon.aicodeheadstream.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 服务层实现。
 *
 * @author leon
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    /**
     * 允许排序的字段白名单（防止非法字段拼接进 SQL）
     */
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "appName", "priority", "visible", "deployedTime", "editTime", "createTime", "updateTime");

    @Resource
    private UserService userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 保存用户消息
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER, loginUser.getId());
        // 6. 调用 AI 生成代码（流式）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7. 收集 AI 回复内容，流正常结束时保存 AI 消息，流异常结束时保存错误信息
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }


    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, String.format("应用代码不存在，请先生成代码。path:%s", sourceDirPath));
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9. 返回可访问的 URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }


    @Override
    public long createApp(AppAddRequest appAddRequest, User loginUser) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        String appName = appAddRequest.getAppName();
        if (StrUtil.isBlank(appName)) {
            appName = initPrompt.substring(0, Math.min(initPrompt.length(), 12));
        }
        ThrowUtils.throwIf(appName.length() > 256, ErrorCode.PARAMS_ERROR, "应用名称过长");
        App app = new App();
        app.setAppName(appName);
        app.setInitPrompt(initPrompt);
        // 暂时用这个
        app.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue());
        app.setUserId(loginUser.getId());
        app.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
        // 应用创建时默认为私有，用户可在应用修改页面调整为公开
        app.setVisible(AppConstant.APP_VISIBLE_PRIVATE);
        // 应用创建时标签为空，用户可在应用修改页面设置标签
        app.setLabel(AppConstant.DEFAULT_APP_LABEL);
        app.setEditTime(LocalDateTime.now());
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建应用失败");
        return app.getId();
    }

    @Override
    public boolean updateMyApp(AppUpdateRequest appUpdateRequest, User loginUser) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null || appUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(appUpdateRequest.getAppName()), ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        String appName = appUpdateRequest.getAppName();
        ThrowUtils.throwIf(appName.length() > 256, ErrorCode.PARAMS_ERROR, "应用名称过长");
        Integer visible = appUpdateRequest.getVisible();
        if (visible != null) {
            ThrowUtils.throwIf(!isValidVisible(visible), ErrorCode.PARAMS_ERROR, "可见状态无效");
        }
        App oldApp = this.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!Objects.equals(oldApp.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App updateApp = new App();
        updateApp.setId(oldApp.getId());
        updateApp.setAppName(appName);
        if (visible != null) {
            updateApp.setVisible(visible);
        }
        // 标签：null 表示不修改，空字符串表示清空标签
        String label = appUpdateRequest.getLabel();
        if (label != null) {
            checkAppLabel(label);
            updateApp.setLabel(label);
        }
        updateApp.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public boolean deleteMyApp(Long id, User loginUser) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        App oldApp = this.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!Objects.equals(oldApp.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除应用失败");
        cleanTempFiles(oldApp);
        return true;
    }

    @Override
    public boolean deleteAppByAdmin(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        App oldApp = this.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除应用失败");
        cleanTempFiles(oldApp);
        return true;
    }

    @Override
    public boolean removeById(@NonNull Serializable id) {
        long appId = Long.parseLong(id.toString());
        try {
            chatHistoryService.removeByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用对话历史失败：{}", e.getMessage(), e);
        }
        return super.removeById(id);
    }


    /**
     * 清理应用产生的临时文件：代码生成目录（{codeGenType}_{appId}）与部署目录（{deployKey}）。
     * 清理失败只记录日志，不影响应用本身的删除结果。
     *
     * @param app 被删除的应用
     */
    private void cleanTempFiles(App app) {
        if (app == null) {
            return;
        }
        // 1、清理代码生成目录
        String codeGenType = app.getCodeGenType();
        if (StrUtil.isNotBlank(codeGenType)) {
            String sourceDirName = codeGenType + "_" + app.getId();
            delTempDir(AppConstant.CODE_OUTPUT_ROOT_DIR, sourceDirName);
        }
        // 2、清理部署目录
        String deployKey = app.getDeployKey();
        if (StrUtil.isNotBlank(deployKey)) {
            delTempDir(AppConstant.CODE_DEPLOY_ROOT_DIR, deployKey);
        }
    }

    /**
     * 删除临时目录（不存在时忽略）
     *
     * @param rootDir 根目录
     * @param dirName 目录名
     */
    private void delTempDir(String rootDir, String dirName) {
        String dirPath = rootDir + File.separator + dirName;
        try {
            if (FileUtil.exist(dirPath)) {
                FileUtil.del(dirPath);
                log.info("已清理应用临时目录：{}", dirPath);
            }
        } catch (Exception e) {
            log.warn("清理应用临时目录失败：{}，原因：{}", dirPath, e.getMessage());
        }
    }

    @Override
    public boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest) {
        ThrowUtils.throwIf(appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null
                || appAdminUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        String appName = appAdminUpdateRequest.getAppName();
        String cover = appAdminUpdateRequest.getCover();
        Integer priority = appAdminUpdateRequest.getPriority();
        Integer visible = appAdminUpdateRequest.getVisible();
        String label = appAdminUpdateRequest.getLabel();
        boolean hasUpdate = StrUtil.isNotBlank(appName) || cover != null || priority != null || visible != null
                || label != null;
        ThrowUtils.throwIf(!hasUpdate, ErrorCode.PARAMS_ERROR, "没有要更新的内容");
        if (StrUtil.isNotBlank(appName)) {
            ThrowUtils.throwIf(appName.length() > 256, ErrorCode.PARAMS_ERROR, "应用名称过长");
        }
        if (cover != null) {
            ThrowUtils.throwIf(cover.length() > 512, ErrorCode.PARAMS_ERROR, "应用封面地址过长");
        }
        if (visible != null) {
            ThrowUtils.throwIf(!isValidVisible(visible), ErrorCode.PARAMS_ERROR, "可见状态无效");
        }
        if (label != null) {
            checkAppLabel(label);
        }
        App oldApp = this.getById(appAdminUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App updateApp = new App();
        updateApp.setId(oldApp.getId());
        updateApp.setEditTime(LocalDateTime.now());
        if (StrUtil.isNotBlank(appName)) {
            updateApp.setAppName(appName);
        }
        if (cover != null) {
            updateApp.setCover(cover);
        }
        if (priority != null) {
            updateApp.setPriority(priority);
        }
        if (visible != null) {
            updateApp.setVisible(visible);
        }
        // 标签：null 表示不修改，空字符串表示清空标签
        if (label != null) {
            updateApp.setLabel(label);
        }
        boolean result = this.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = Map.of();
        if (CollUtil.isNotEmpty(userIds)) {
            userVOMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, userService::getUserVO, (left, right) -> left));
        }
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            appVO.setUser(finalUserVOMap.get(app.getUserId()));
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Integer visible = appQueryRequest.getVisible();
        Long userId = appQueryRequest.getUserId();
        String label = appQueryRequest.getLabel();
        // 标签为空字符串时视为不按标签筛选
        if (StrUtil.isBlank(label)) {
            label = null;
        } else {
            checkAppLabel(label);
        }
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("visible", visible)
                .eq("userId", userId)
                .eq("label", label);
        if (StrUtil.isNotBlank(sortField) && SORTABLE_FIELDS.contains(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
            // 次要排序，保证分页结果稳定
            queryWrapper.orderBy("id", false);
        }
        return queryWrapper;
    }

    @Override
    public void checkAppVisible(App app, User loginUser) {
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        // 公开应用所有人可见
        if (AppConstant.APP_VISIBLE_PUBLIC.equals(app.getVisible())) {
            return;
        }
        // 私有应用：只有创建者和管理员可见
        boolean isAdmin = loginUser != null && UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isOwner = loginUser != null && Objects.equals(app.getUserId(), loginUser.getId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该应用未公开，无法查看");
        }
    }

    /**
     * 校验可见状态是否合法
     *
     * @param visible 可见状态
     * @return 是否合法
     */
    private boolean isValidVisible(Integer visible) {
        return AppConstant.APP_VISIBLE_PRIVATE.equals(visible) || AppConstant.APP_VISIBLE_PUBLIC.equals(visible);
    }

    /**
     * 校验应用标签是否合法（空字符串表示清空标签，视为合法）
     *
     * @param label 应用标签
     */
    private void checkAppLabel(String label) {
        if (StrUtil.isBlank(label)) {
            return;
        }
        ThrowUtils.throwIf(!AppLabelEnum.isValid(label), ErrorCode.PARAMS_ERROR, "应用标签无效");
    }
}
