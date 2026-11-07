package com.leon.aicodeheadstream.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leon.aicodeheadstream.constant.UserConstant;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.exception.ThrowUtils;
import com.leon.aicodeheadstream.mapper.AppMapper;
import com.leon.aicodeheadstream.mapper.ChatHistoryMapper;
import com.leon.aicodeheadstream.model.dto.chat.ChatHistoryAppQueryRequest;
import com.leon.aicodeheadstream.model.dto.chat.ChatHistoryQueryRequest;
import com.leon.aicodeheadstream.model.entity.App;
import com.leon.aicodeheadstream.model.entity.ChatHistory;
import com.leon.aicodeheadstream.model.entity.User;
import com.leon.aicodeheadstream.model.enums.ChatHistoryMessageTypeEnum;
import com.leon.aicodeheadstream.model.vo.ChatHistoryPageVO;
import com.leon.aicodeheadstream.model.vo.ChatHistoryVO;
import com.leon.aicodeheadstream.service.ChatHistoryService;
import com.leon.aicodeheadstream.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  对话历史服务层实现。
 *
 * @author leon
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    /**
     * 默认每页消息条数
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 每页消息最大条数
     */
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * 允许排序的字段白名单（防止非法字段拼接进 SQL）
     */
    private static final Set<String> SORTABLE_FIELDS = Set.of("createTime", "updateTime", "id");

    @Resource
    private UserService userService;

    /**
     * 应用 Mapper（直接使用 Mapper 而非 AppService，避免与 AppService 形成循环依赖）
     */
    @Resource
    private AppMapper appMapper;

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                }
                loadedCount++;
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }


    @Override
    public void addChatMessage(Long appId , String message, ChatHistoryMessageTypeEnum chatHistoryMessageTypeEnum, Long userId){
        if (appId == null || appId <= 0 || userId == null || userId <= 0) {
            log.warn("对话历史参数不完整，放弃保存：appId={}，userId={}", appId, userId);
            return;
        }
        if (StrUtil.isBlank(message)) {
            return;
        }
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setAppId(appId);
        chatHistory.setUserId(userId);
        chatHistory.setMessage(message);
        chatHistory.setMessageType(chatHistoryMessageTypeEnum.getValue());
        chatHistory.setCreateTime(LocalDateTime.now());
        chatHistory.setUpdateTime(LocalDateTime.now());
        try {
            this.save(chatHistory);
        } catch (Exception e) {
            // 保存对话历史失败不影响主流程
            log.error("保存对话历史失败：appId={}，messageType={}，原因：{}", appId, chatHistoryMessageTypeEnum.getValue(), e.getMessage(), e);
        }
    }

    @Override
    public ChatHistoryPageVO getAppChatHistory(ChatHistoryAppQueryRequest chatHistoryAppQueryRequest, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(chatHistoryAppQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long appId = chatHistoryAppQueryRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不能为空");
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        int limit = getPageLimit(chatHistoryAppQueryRequest.getPageSize());
        // 2. 查询应用信息并校验权限：只有应用创建者和管理员可以查看对话历史
        App app = appMapper.selectOneById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isOwner = Objects.equals(app.getUserId(), loginUser.getId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        }
        // 3. 游标分页：只基于 createTime 取最新的消息，createTime 早于游标的部分为更早的历史记录
        //    （消息 id 为雪花算法生成，不作为分页依据）
        QueryWrapper queryWrapper = getAppQueryWrapper(chatHistoryAppQueryRequest);
        List<ChatHistory> chatHistoryList = this.list(queryWrapper);
        // 4. 多取一条用于判断是否还有更早的历史记录
        boolean hasMore = chatHistoryList.size() > limit;
        if (hasMore) {
            chatHistoryList = chatHistoryList.subList(0, limit);
        }
        // 5. 转换为按时间正序（最早的消息在前），方便前端直接展示
        List<ChatHistory> orderedList = new ArrayList<>(chatHistoryList);
        Collections.reverse(orderedList);
        ChatHistoryPageVO chatHistoryPageVO = new ChatHistoryPageVO(1, limit);
        chatHistoryPageVO.setHasMore(hasMore);
        // 游标分页只返回当前批消息，总数未知，总条数以当前页条数为准
        chatHistoryPageVO.setTotalRow(orderedList.size());
        // 应用名称允许为空，用 HashMap 承接，避免 Map.of 遇到 null 值报错
        Map<Long, String> appNameMap = new HashMap<>();
        appNameMap.put(app.getId(), app.getAppName());
        chatHistoryPageVO.setRecords(getChatHistoryVOList(orderedList, appNameMap));
        // 6. 下一次向前加载的游标为本次结果中最早的 createTime，没有更多历史时为 null
        if (hasMore && CollUtil.isNotEmpty(orderedList)) {
            chatHistoryPageVO.setNextCursor(orderedList.getFirst().getCreateTime());
        }
        return chatHistoryPageVO;
    }

    /**
     * 获取应用对话历史的查询条件
     *
     * <p>游标只基于 createTime：取严格早于游标时间的历史消息；
     * 排序以 createTime 为主，id 仅用于同一时刻多条记录时保证顺序稳定。</p>
     *
     * @param chatHistoryAppQueryRequest 查询请求
     * @return 查询条件
     */
    @Override
    public QueryWrapper getAppQueryWrapper(ChatHistoryAppQueryRequest chatHistoryAppQueryRequest) {
        if (chatHistoryAppQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        int limit = getPageLimit(chatHistoryAppQueryRequest.getPageSize());
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", chatHistoryAppQueryRequest.getAppId());
        LocalDateTime cursor = parseCursor(chatHistoryAppQueryRequest.getCursor());
        if (cursor != null) {
            queryWrapper.lt("createTime", cursor);
        }
        queryWrapper.orderBy("createTime", false)
                .orderBy("id", false)
                // 多取一条用于判断是否还有更早的历史记录
                .limit(limit + 1);
        return queryWrapper;
    }

    /**
     * 解析游标时间，支持 ISO 格式与空格分隔格式
     *
     * @param cursor 游标字符串，为空表示从最新开始
     * @return 游标对应的创建时间
     */
    private static LocalDateTime parseCursor(String cursor) {
        if (StrUtil.isBlank(cursor)) {
            return null;
        }
        String text = cursor.trim();
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ex) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "游标格式不正确");
            }
        }
    }

    /**
     * 计算每页条数
     *
     * @param pageSize 请求的每页数量
     * @return 实际每页条数
     */
    private static int getPageLimit(int pageSize) {
        return pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        if (chatHistoryQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String messageType = chatHistoryQueryRequest.getMessageType();
        // 消息类型校验
        if (StrUtil.isNotBlank(messageType)) {
            ThrowUtils.throwIf(!ChatHistoryMessageTypeEnum.isValid(messageType), ErrorCode.PARAMS_ERROR, "消息类型无效");
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", chatHistoryQueryRequest.getAppId())
                .eq("messageType", StrUtil.isBlank(messageType) ? null : messageType);
        // 按用户账号筛选：先查账号对应的用户，再按用户 id 过滤
        // （账号不存在时用不可能匹配的 id，保证查不到任何记录，而不是忽略条件查出全部）
        String userAccount = chatHistoryQueryRequest.getUserAccount();
        if (StrUtil.isNotBlank(userAccount)) {
            User user = userService.getOne(QueryWrapper.create().eq("userAccount", userAccount).limit(1));
            queryWrapper.eq("userId", user == null ? -1L : user.getId());
        }
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        if (StrUtil.isNotBlank(sortField) && SORTABLE_FIELDS.contains(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
            // 次要排序，保证分页结果稳定
            queryWrapper.orderBy("id", false);
        } else {
            // 默认按创建时间降序，便于内容监管
            queryWrapper.orderBy("createTime", false)
                    .orderBy("id", false);
        }
        return queryWrapper;
    }

    @Override
    public boolean removeByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        boolean result = this.remove(queryWrapper);
        log.info("已删除应用 {} 的对话历史", appId);
        return result;
    }

    /**
     * 转换为对话历史视图列表，并补齐应用名称与用户昵称
     *
     * @param chatHistoryList 对话历史列表
     * @return 对话历史视图列表
     */
    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        return getChatHistoryVOList(chatHistoryList, null);
    }

    /**
     * 转换为对话历史视图列表，并补齐应用名称与用户昵称
     *
     * @param chatHistoryList 对话历史列表
     * @param appNameMap      已知的应用名称（key 为应用id），为空时自行查询
     * @return 对话历史视图列表
     */
    private List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList, Map<Long, String> appNameMap) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }
        // 1. 查询应用名称
        Map<Long, String> finalAppNameMap = appNameMap == null ? getAppNameMap(chatHistoryList) : appNameMap;
        // 2. 查询用户账号与昵称
        Map<Long, String> userAccountMap = getUserAccountMap(chatHistoryList);
        Map<Long, String> userNameMap = getUserNameMap(chatHistoryList);
        // 3. 转换
        return chatHistoryList.stream()
                .map(chatHistory -> {
                    ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
                    BeanUtil.copyProperties(chatHistory, chatHistoryVO);
                    // 消息类型描述
                    ChatHistoryMessageTypeEnum chatHistoryMessageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(chatHistory.getMessageType());
                    chatHistoryVO.setMessageTypeText(chatHistoryMessageTypeEnum == null ? chatHistory.getMessageType() : chatHistoryMessageTypeEnum.getText());
                    // 应用名称、用户账号与昵称
                    chatHistoryVO.setAppName(finalAppNameMap.get(chatHistory.getAppId()));
                    chatHistoryVO.setUserAccount(userAccountMap.get(chatHistory.getUserId()));
                    chatHistoryVO.setUserName(userNameMap.get(chatHistory.getUserId()));
                    return chatHistoryVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询对话历史涉及的应用名称
     *
     * @param chatHistoryList 对话历史列表
     * @return 应用id与应用名称的映射
     */
    private Map<Long, String> getAppNameMap(List<ChatHistory> chatHistoryList) {
        Set<Long> appIds = chatHistoryList.stream()
                .map(ChatHistory::getAppId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(appIds)) {
            return Map.of();
        }
        QueryWrapper queryWrapper = QueryWrapper.create().in("id", appIds);
        // 应用名称允许为空，用 HashMap 承接，避免 Collectors.toMap 遇到 null 值报错
        Map<Long, String> appNameMap = new HashMap<>();
        appMapper.selectListByQuery(queryWrapper).forEach(app -> appNameMap.put(app.getId(), app.getAppName()));
        return appNameMap;
    }

    /**
     * 查询对话历史涉及的用户账号
     *
     * @param chatHistoryList 对话历史列表
     * @return 用户id与用户账号的映射
     */
    private Map<Long, String> getUserAccountMap(List<ChatHistory> chatHistoryList) {
        Set<Long> userIds = chatHistoryList.stream()
                .map(ChatHistory::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(userIds)) {
            return Map.of();
        }
        // 用户账号允许为空，用 HashMap 承接，避免 Collectors.toMap 遇到 null 值报错
        Map<Long, String> userAccountMap = new HashMap<>();
        userService.listByIds(userIds).forEach(user -> userAccountMap.put(user.getId(), user.getUserAccount()));
        return userAccountMap;
    }

    /**
     * 查询对话历史涉及的用户昵称
     *
     * @param chatHistoryList 对话历史列表
     * @return 用户id与用户昵称的映射
     */
    private Map<Long, String> getUserNameMap(List<ChatHistory> chatHistoryList) {
        Set<Long> userIds = chatHistoryList.stream()
                .map(ChatHistory::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(userIds)) {
            return Map.of();
        }
        // 用户昵称允许为空，用 HashMap 承接，避免 Collectors.toMap 遇到 null 值报错
        Map<Long, String> userNameMap = new HashMap<>();
        userService.listByIds(userIds).forEach(user -> userNameMap.put(user.getId(), user.getUserName()));
        return userNameMap;
    }
}
