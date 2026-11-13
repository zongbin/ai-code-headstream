package com.leon.aicodeheadstream.model.model.dto.chat;

import com.leon.aicodeheadstream.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 对话历史查询请求（管理员使用，支持按应用、用户账号、消息类型筛选）。
 *
 * @author leon
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 创建用户账号
     */
    private String userAccount;

    /**
     * 消息类型（枚举 MessageTypeEnum）：user/ai
     */
    private String messageType;

    private static final long serialVersionUID = 1L;
}
