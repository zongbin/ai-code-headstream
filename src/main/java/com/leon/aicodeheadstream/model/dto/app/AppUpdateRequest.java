package com.leon.aicodeheadstream.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新自己的应用。支持修改应用名称、可见状态、应用标签。
 *
 * @author leon
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 可见状态：0-私有，1-公开
     */
    private Integer visible;

    /**
     * 应用标签（枚举）。null 表示不修改标签，空字符串表示清空标签
     */
    private String label;

    private static final long serialVersionUID = 1L;
}
