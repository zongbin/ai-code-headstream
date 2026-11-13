package com.leon.aicodeheadstream.model.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 应用标签枚举。
 *
 * <p>标签取值直接使用中文，与数据库 app.label 字段存储的值保持一致，
 * 便于直接展示在案例广场的应用名称后面。</p>
 *
 * @author leon
 */
@Getter
public enum AppLabelEnum {

    TOOL("工具"),
    WEBSITE("网站"),
    DATA_ANALYSIS("数据分析"),
    ACTIVITY_PAGE("活动页面"),
    MANAGEMENT_PLATFORM("管理平台"),
    USER_APP("用户应用"),
    PERSONAL_MANAGEMENT("个人管理"),
    GAME("游戏");

    /**
     * 标签名称（同时作为数据库存储值）
     */
    private final String text;

    AppLabelEnum(String text) {
        this.text = text;
    }

    /**
     * 根据标签名称获取枚举
     *
     * @param text 标签名称
     * @return 枚举值，未匹配时返回 null
     */
    public static AppLabelEnum getEnumByText(String text) {
        if (ObjUtil.isEmpty(text)) {
            return null;
        }
        for (AppLabelEnum anEnum : AppLabelEnum.values()) {
            if (anEnum.text.equals(text)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 校验标签名称是否合法
     *
     * @param text 标签名称
     * @return 是否合法
     */
    public static boolean isValid(String text) {
        return getEnumByText(text) != null;
    }

    /**
     * 获取所有标签名称
     *
     * @return 标签名称列表
     */
    public static List<String> getTexts() {
        return Arrays.stream(AppLabelEnum.values()).map(AppLabelEnum::getText).toList();
    }
}
