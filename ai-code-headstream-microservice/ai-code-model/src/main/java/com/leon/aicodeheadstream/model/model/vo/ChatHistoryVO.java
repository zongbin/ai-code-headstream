package com.leon.aicodeheadstream.model.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史视图（附带应用名称、用户昵称）。
 *
 * @author leon
 */
@Data
public class ChatHistoryVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型（枚举 MessageTypeEnum）：user/ai
     */
    private String messageType;

    /**
     * 消息类型描述
     */
    private String messageTypeText;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建用户账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
