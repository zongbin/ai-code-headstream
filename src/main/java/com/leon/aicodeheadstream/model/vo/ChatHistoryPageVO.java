package com.leon.aicodeheadstream.model.vo;

import com.mybatisflex.core.paginate.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话历史游标分页结果。
 *
 * <p>类似聊天软件的消息加载机制：首次加载最新的一批消息，
 * 需要更多历史记录时带上 {@code nextCursor} 向前加载更早的消息。</p>
 *
 * @author leon
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ChatHistoryPageVO extends Page<ChatHistoryVO> {

    /**
     * 是否还有更早的历史消息
     */
    private Boolean hasMore;

    /**
     * 下一次向前加载的游标（本次结果中最早的 createTime），没有更多历史时为 null
     */
    private LocalDateTime nextCursor;

    public ChatHistoryPageVO(Number pageNumber, Number pageSize) {
        super(pageNumber, pageSize);
    }

    private static final long serialVersionUID = 1L;
}
