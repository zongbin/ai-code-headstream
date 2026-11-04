package com.leon.aicodeheadstream.service;

import com.leon.aicodeheadstream.model.dto.app.AppAddRequest;
import com.leon.aicodeheadstream.model.dto.app.AppAdminUpdateRequest;
import com.leon.aicodeheadstream.model.dto.app.AppQueryRequest;
import com.leon.aicodeheadstream.model.dto.app.AppUpdateRequest;
import com.leon.aicodeheadstream.model.entity.User;
import com.leon.aicodeheadstream.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.leon.aicodeheadstream.model.entity.App;

import java.util.List;

/**
 * 服务层。
 *
 * @author leon
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param appAddRequest 创建请求，须填写 initPrompt
     * @param loginUser     当前登录用户
     * @return 新应用 id
     */
    long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 用户修改自己的应用（仅应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param loginUser        当前登录用户
     * @return 是否成功
     */
    boolean updateMyApp(AppUpdateRequest appUpdateRequest, User loginUser);

    /**
     * 用户删除自己的应用
     *
     * @param id        应用 id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean deleteMyApp(Long id, User loginUser);

    /**
     * 管理员更新应用（名称、封面、优先级）
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 是否成功
     */
    boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest);

    /**
     * 获取应用视图
     *
     * @param app 应用
     * @return 应用视图
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用视图列表
     *
     * @param appList 应用列表
     * @return 应用视图列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
