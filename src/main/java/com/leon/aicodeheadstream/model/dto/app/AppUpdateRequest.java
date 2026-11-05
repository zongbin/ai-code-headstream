package com.leon.aicodeheadstream.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新自己的应用。支持修改应用名称、可见状态。
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

    private static final long serialVersionUID = 1L;
}
