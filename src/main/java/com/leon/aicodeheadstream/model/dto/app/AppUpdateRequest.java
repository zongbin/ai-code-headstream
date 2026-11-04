package com.leon.aicodeheadstream.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新自己的应用。目前只允许修改应用名称。
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

    private static final long serialVersionUID = 1L;
}
