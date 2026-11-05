package com.leon.aicodeheadstream.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新应用。支持修改应用名称、封面、优先级、可见状态。
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

    private static final long serialVersionUID = 1L;
}
