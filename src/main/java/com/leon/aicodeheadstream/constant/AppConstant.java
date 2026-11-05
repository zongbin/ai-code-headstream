package com.leon.aicodeheadstream.constant;

public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 置顶应用的优先级（置顶通过把优先级标记为该值实现，与精选互斥）
     */
    Integer TOP_APP_PRIORITY = 999;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用可见状态：私有（只有自己可见）
     */
    Integer APP_VISIBLE_PRIVATE = 0;

    /**
     * 应用可见状态：公开（所有人可见，展示在案例广场）
     */
    Integer APP_VISIBLE_PUBLIC = 1;

    /**
     * 用户分页查询每页最大数量
     */
    int MAX_PAGE_SIZE = 20;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";

}
