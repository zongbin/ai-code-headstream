package com.leon.aicodeheadstream.model.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 对话消息类型枚举。
 *
 * <p>枚举值直接存储在数据库 chat_history.messageType 字段中，
 * 前端据此区分用户消息与 AI 回复。AI 生成失败时同样记为 AI 回复，
 * 只是消息内容为错误描述。</p>
 *
 * @author leon
 */
@Getter
public enum ChatHistoryMessageTypeEnum {

    USER("用户消息", "user"),
    AI("AI 回复", "ai");

    /**
     * 类型描述
     */
    private final String text;

    /**
     * 类型值（同时作为数据库存储值）
     */
    private final String value;

    ChatHistoryMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值，未匹配时返回 null
     */
    public static ChatHistoryMessageTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChatHistoryMessageTypeEnum anEnum : ChatHistoryMessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 校验消息类型是否合法
     *
     * @param value 消息类型
     * @return 是否合法
     */
    public static boolean isValid(String value) {
        return getEnumByValue(value) != null;
    }
}
