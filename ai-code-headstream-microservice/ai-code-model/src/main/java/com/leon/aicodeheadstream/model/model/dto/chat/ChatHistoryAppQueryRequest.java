package com.leon.aicodeheadstream.model.model.dto.chat;

import com.leon.aicodeheadstream.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 应用对话历史游标分页查询请求。
 *
 * <p>游标只基于创建时间（消息 id 由雪花算法生成，不作为分页依据）。</p>
 *
 * @author leon
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryAppQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 游标（已加载消息中最早的 createTime），为空表示从最新开始
     *
     * <p>支持 ISO 格式（2026-11-06T10:00:00）与空格分隔格式（2026-11-06 10:00:00）。</p>
     */
    private String cursor;

    private static final long serialVersionUID = 1L;
}
