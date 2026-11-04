package com.leon.aicodeheadstream.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leon.aicodeheadstream.constant.AppConstant;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.exception.ThrowUtils;
import com.leon.aicodeheadstream.model.dto.app.AppAddRequest;
import com.leon.aicodeheadstream.model.dto.app.AppAdminUpdateRequest;
import com.leon.aicodeheadstream.model.dto.app.AppQueryRequest;
import com.leon.aicodeheadstream.model.dto.app.AppUpdateRequest;
import com.leon.aicodeheadstream.model.entity.User;
import com.leon.aicodeheadstream.model.vo.AppVO;
import com.leon.aicodeheadstream.model.vo.UserVO;
import com.leon.aicodeheadstream.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.leon.aicodeheadstream.model.entity.App;
import com.leon.aicodeheadstream.mapper.AppMapper;
import com.leon.aicodeheadstream.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

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
        app.setUserId(loginUser.getId());
        app.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
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
        App oldApp = this.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!Objects.equals(oldApp.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App updateApp = new App();
        updateApp.setId(oldApp.getId());
        updateApp.setAppName(appName);
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
        return this.removeById(id);
    }

    @Override
    public boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest) {
        ThrowUtils.throwIf(appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null
                || appAdminUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        String appName = appAdminUpdateRequest.getAppName();
        String cover = appAdminUpdateRequest.getCover();
        Integer priority = appAdminUpdateRequest.getPriority();
        boolean hasUpdate = StrUtil.isNotBlank(appName) || cover != null || priority != null;
        ThrowUtils.throwIf(!hasUpdate, ErrorCode.PARAMS_ERROR, "没有要更新的内容");
        if (StrUtil.isNotBlank(appName)) {
            ThrowUtils.throwIf(appName.length() > 256, ErrorCode.PARAMS_ERROR, "应用名称过长");
        }
        if (cover != null) {
            ThrowUtils.throwIf(cover.length() > 512, ErrorCode.PARAMS_ERROR, "应用封面地址过长");
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
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }
}
