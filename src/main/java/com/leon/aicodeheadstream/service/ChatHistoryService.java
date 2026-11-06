package com.leon.aicodeheadstream.service;

import com.leon.aicodeheadstream.model.dto.chat.ChatHistoryAppQueryRequest;
import com.leon.aicodeheadstream.model.dto.chat.ChatHistoryQueryRequest;
import com.leon.aicodeheadstream.model.entity.User;
import com.leon.aicodeheadstream.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.leon.aicodeheadstream.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 *  对话历史服务层。
 *
 * @author leon
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 保存用户消息
     *
     * @param appId   应用id
     * @param userId  用户id
     * @param message 消息内容
     */
    void saveUserMessage(Long appId, Long userId, String message);

    /**
     * 保存 AI 回复
     *
     * @param appId   应用id
     * @param userId  用户id
     * @param message AI 回复内容
     */
    void saveAiMessage(Long appId, Long userId, String message);

    /**
     * 保存 AI 生成失败时的错误信息，保证对话的完整性
     *
     * <p>消息类型仍为 AI（ai），只是消息内容为错误描述。</p>
     *
     * @param appId        应用id
     * @param userId       用户id
     * @param errorMessage 错误信息
     */
    void saveErrorMessage(Long appId, Long userId, String errorMessage);

    /**
     * 分页查看某个应用的对话历史（游标分页，按时间正序返回）
     *
     * <p>首次查询不传 cursor，返回最新的一批消息；
     * 需要更多历史记录时传入上一次返回的 nextCursor，即可向前加载更早的消息。
     * 游标只基于 createTime（消息 id 为雪花算法生成，不作为分页依据）。</p>
     *
     * @param request   查询请求
     * @param loginUser 当前登录用户，只有应用创建者和管理员可以查看
     * @return 对话历史游标分页结果
     */
    Page<ChatHistoryVO> getAppChatHistory(ChatHistoryAppQueryRequest request, User loginUser);

    /**
     * 获取应用对话历史的查询条件
     *
     * @param request 查询请求
     * @return 查询条件
     */
    QueryWrapper getAppQueryWrapper(ChatHistoryAppQueryRequest request);

    /**
     * 获取对话历史视图列表，并补齐应用名称与用户昵称
     *
     * @param chatHistoryList 对话历史列表
     * @return 对话历史视图列表
     */
    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList);

    /**
     * 获取查询条件
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 删除某个应用下的所有对话历史（删除应用时级联调用）
     *
     * @param appId 应用id
     * @return 是否删除成功
     */
    boolean removeByAppId(Long appId);
}
