package com.leon.aicodeheadstream.model.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户更新个人信息的请求。
 * 不包含 id / userRole / userPassword 等敏感字段，由后端从登录态中获取。
 *
 * @author leon
 */
@Data
public class UserProfileUpdateRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    private static final long serialVersionUID = 1L;
}
