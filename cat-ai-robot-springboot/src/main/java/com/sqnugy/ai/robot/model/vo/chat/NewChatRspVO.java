package com.sqnugy.ai.robot.model.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author: sqnugy
 * @url: www.sqnugy.top
 * @date: 2023-09-15 14:07
 * @description: 新建对话
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewChatRspVO {

    /**
     * 对话 UUID
     */
    private String uuid;

    private String roleName;

    /**
     * 摘要
     */
    private String summary;


}
