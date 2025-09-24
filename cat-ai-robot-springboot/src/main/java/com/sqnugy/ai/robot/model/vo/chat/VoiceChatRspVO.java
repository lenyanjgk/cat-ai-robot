package com.sqnugy.ai.robot.model.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 吴光耀
 * @Date 2025/9/24
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VoiceChatRspVO {

    private String replyText;

    private String replyAudioUrl;
}
