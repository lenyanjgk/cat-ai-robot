package com.sqnugy.ai.robot.service;

/**
 * @author 吴光耀
 * @Date 2025/9/23
 * @description:
 */
public interface AudioChatService {

    String recognize(String audioFileUrl);

    String synthesize(String text);

}
