package com.leon.aicodeheadstream.service.impl;

import com.leon.aicodeheadstream.innerservice.InnerUserService;
import com.leon.aicodeheadstream.model.entity.User;
import com.leon.aicodeheadstream.model.vo.UserVO;
import com.leon.aicodeheadstream.service.UserService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        return userService.listByIds(ids);
    }

    @Override
    public User getById(Serializable id) {
        return userService.getById(id);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userService.getUserVO(user);
    }
}
