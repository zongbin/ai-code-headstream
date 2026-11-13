package com.leon.aicodeheadstream.model.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新应用。支持修改应用名称、封面、优先级、可见状态、应用标签。
 *
 * @author leon
 */
@Data
public class AppAdminUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 优先级
     */
    private Integer priority;

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
