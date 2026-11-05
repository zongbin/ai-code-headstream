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
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 服务层。
 *
 * @author leon
 */
public interface AppService extends IService<App> {


    /**
     * 通过聊天交互生成代码（流式响应）
     *
     * @param appId     应用 ID，指定要生成代码的目标应用
     * @param message   用户发送的聊天消息，用于引导代码生成
     * @param loginUser 当前登录用户
     * @return 响应式流，流式返回生成的代码片段
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     *
     * @param appId     应用 ID
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    String deployApp(Long appId, User loginUser);
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
     * 管理员删除任意应用（同时清理应用产生的临时文件）
     *
     * @param id 应用 id
     * @return 是否成功
     */
    boolean deleteAppByAdmin(Long id);

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
