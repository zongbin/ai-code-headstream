package com.leon.aicodeheadstream.controller;

import com.leon.aicodeheadstream.annotation.AuthCheck;
import com.leon.aicodeheadstream.common.BaseResponse;
import com.leon.aicodeheadstream.common.ResultUtils;
import com.leon.aicodeheadstream.constant.UserConstant;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.exception.ThrowUtils;
import com.leon.aicodeheadstream.model.dto.chat.ChatHistoryAppQueryRequest;
import com.leon.aicodeheadstream.model.dto.chat.ChatHistoryQueryRequest;
import com.leon.aicodeheadstream.model.entity.ChatHistory;
import com.leon.aicodeheadstream.model.entity.User;
import com.leon.aicodeheadstream.model.vo.ChatHistoryVO;
import com.leon.aicodeheadstream.service.ChatHistoryService;
import com.leon.aicodeheadstream.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史控制层。
 *
 * @author leon
 */
@RestController
@RequestMapping("/chatHistory")
@Slf4j
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页查看某个应用的对话历史（游标分页，仅应用创建者和管理员可见）
     *
     * <p>首次查询不传 cursor，返回最新的一批消息（默认 10 条）；
     * 需要更多历史记录时带上上一次返回的 nextCursor，即可向前加载更早的消息。
     * 游标只基于创建时间（消息 id 为雪花算法生成，不作为分页依据）。</p>
     *
     * @param chatHistoryAppQueryRequest 查询请求
     * @param request                   请求
     * @return 对话历史游标分页结果
     */
    @GetMapping("/app/list")
    public BaseResponse<Page<ChatHistoryVO>> listAppChatHistory(ChatHistoryAppQueryRequest chatHistoryAppQueryRequest,
                                                                HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistoryVO> chatHistoryPage = chatHistoryService.getAppChatHistory(chatHistoryAppQueryRequest, loginUser);
        return ResultUtils.success(chatHistoryPage);
    }

    /**
     * 管理员分页查看所有应用的对话历史（按创建时间降序，便于内容监管）
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryPageByAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize <= 0, ErrorCode.PARAMS_ERROR, "每页数量必须大于 0");
        // 查询条件
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 转换为视图
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        chatHistoryVOPage.setRecords(chatHistoryService.getChatHistoryVOList(chatHistoryPage.getRecords()));
        return ResultUtils.success(chatHistoryVOPage);
    }
}
