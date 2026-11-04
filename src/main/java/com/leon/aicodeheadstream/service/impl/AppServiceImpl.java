package com.leon.aicodeheadstream.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.leon.aicodeheadstream.model.entity.App;
import com.leon.aicodeheadstream.mapper.AppMapper;
import com.leon.aicodeheadstream.service.AppService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author leon
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
