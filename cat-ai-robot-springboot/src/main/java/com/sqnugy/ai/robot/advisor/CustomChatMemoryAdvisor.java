package com.sqnugy.ai.robot.advisor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.sqnugy.ai.robot.domain.dos.ChatDO;
import com.sqnugy.ai.robot.domain.dos.ChatMessageDO;
import com.sqnugy.ai.robot.domain.dos.RoleDO;
import com.sqnugy.ai.robot.domain.mapper.ChatMapper;
import com.sqnugy.ai.robot.domain.mapper.ChatMessageMapper;
import com.sqnugy.ai.robot.domain.mapper.RoleMapper;
import com.sqnugy.ai.robot.model.vo.chat.AiChatReqVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.*;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @Author: sqnugy
 * @Date: 2025/5/26 16:36
 * @Version: v1.0.0
 * @Description: 自定义对话记忆 Advisor
 **/
@Slf4j
public class CustomChatMemoryAdvisor implements StreamAdvisor {

    private final RoleMapper roleMapper;

    private final ChatMapper chatMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final AiChatReqVO aiChatReqVO;

    private final int limit;

    public CustomChatMemoryAdvisor(RoleMapper roleMapper, ChatMapper chatMapper, ChatMessageMapper chatMessageMapper, AiChatReqVO aiChatReqVO, int limit) {
        this.roleMapper = roleMapper;
        this.chatMapper = chatMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.aiChatReqVO = aiChatReqVO;
        this.limit = limit;
    }

    @Override
    public int getOrder() {
        return 2; // order 值越小，越先执行
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.info("## 自定义聊天记忆 Advisor...");

        // 对话 UUID
        String chatUuid = aiChatReqVO.getChatId();

        // 查询数据库拉取最新的聊天消息
        List<ChatMessageDO> messages = chatMessageMapper.selectList(Wrappers.<ChatMessageDO>lambdaQuery()
                .eq(ChatMessageDO::getChatUuid, chatUuid) // 查询指定对话 UUID 下的聊天记录
                .orderByDesc(ChatMessageDO::getCreateTime) // 查询最新的消息
                .last(String.format("LIMIT %d", limit))); // 仅查询 LIMIT 条

        // 按发布时间升序排列
        List<ChatMessageDO> sortedMessages = messages.stream()
                 .sorted(Comparator.comparing(ChatMessageDO::getCreateTime)) // 升序排列
                 .toList();

        // 所有消息
        List<Message> messageList = Lists.newArrayList();

        // 1️⃣ systemPrompt（角色提示词）
        ChatDO chatDO = chatMapper.selectByUuid(chatUuid);
        RoleDO roleDO = roleMapper.selectById(chatDO.getRoleId());
        messageList.add(new SystemMessage(roleDO.getSystemPrompt()));

        // 2️⃣ 历史消息
        for (ChatMessageDO chatMessageDO : sortedMessages) {
            String type = chatMessageDO.getRole();
            if (Objects.equals(type, MessageType.USER.getValue())) {
                messageList.add(new UserMessage(chatMessageDO.getContent()));
            } else if (Objects.equals(type, MessageType.ASSISTANT.getValue())) {
                messageList.add(new AssistantMessage(chatMessageDO.getContent()));
            }
        }

        // 3️⃣ 当前用户消息（来自 request）
        messageList.addAll(chatClientRequest.prompt().getInstructions());

        // 构建一个新的 ChatClientRequest 请求对象
        ChatClientRequest processedChatClientRequest = chatClientRequest
                .mutate()
                .prompt(chatClientRequest.prompt().mutate().messages(messageList).build())
                .build();

        return streamAdvisorChain.nextStream(processedChatClientRequest);
    }
}
