package com.sqnugy.ai.robot.service;

import com.sqnugy.ai.robot.domain.dos.RoleDO;

/**
 * @author 吴光耀，江国凯
 * @Date 2025/9/23
 * @description:
 */
public interface AudioChatService {

    String recognize(String audioFileUrl);


    String synthesize(String text, RoleDO roleDO);

}
