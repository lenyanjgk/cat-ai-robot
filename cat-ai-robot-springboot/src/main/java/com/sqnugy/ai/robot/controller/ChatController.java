package com.sqnugy.ai.robot.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.sqnugy.ai.robot.advisor.CustomChatMemoryAdvisor;
import com.sqnugy.ai.robot.advisor.CustomStreamLoggerAndMessage2DBAdvisor;
import com.sqnugy.ai.robot.aspect.ApiOperationLog;
import com.sqnugy.ai.robot.domain.dos.ChatDO;
import com.sqnugy.ai.robot.domain.dos.ChatMessageDO;
import com.sqnugy.ai.robot.domain.dos.RoleDO;
import com.sqnugy.ai.robot.domain.mapper.ChatMapper;
import com.sqnugy.ai.robot.domain.mapper.ChatMessageMapper;
import com.sqnugy.ai.robot.domain.mapper.RoleMapper;
import com.sqnugy.ai.robot.model.vo.chat.*;
import com.sqnugy.ai.robot.service.AudioChatService;
import com.sqnugy.ai.robot.service.ChatService;
import com.sqnugy.ai.robot.utils.PageResponse;
import com.sqnugy.ai.robot.utils.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


@RestController
@RequestMapping("/chat")
@Slf4j
@Tag(name = "对话管理")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AudioChatService audioChatService;

    @Resource
    private ChatMapper chatMapper;

    @Resource
    private RoleMapper roleMapper;

    @PostMapping("/new")
    @ApiOperationLog(description = "新建对话")
    @Operation(description = "新建会话")
    public Response<?> newChat(@RequestBody @Validated NewChatReqVO newChatReqVO) {
        return chatService.newChat(newChatReqVO);
    }

    /**
     * 流式对话
     *
     * @return
     */
    @PostMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "流式对话")
    @Operation(description = "流式对话")
    public Flux<AIResponse> chat(@RequestBody @Validated AiChatReqVO aiChatReqVO) {
        // 用户消息
        String userMessage = aiChatReqVO.getMessage();

        // 模型名称
        String modelName = aiChatReqVO.getModelName();

        // 温度值
        Double temperature = aiChatReqVO.getTemperature();

        // 构建 ChatModel
        ChatModel chatModel = OpenAiChatModel.builder().openAiApi(OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build()).build();

        // 动态设置调用的模型名称、温度值
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel).prompt().options(OpenAiChatOptions.builder().model(modelName).temperature(temperature).build()).user(userMessage); // 用户提示词

        // Advisor 集合
        List<Advisor> advisors = Lists.newArrayList();

        // 是否开启了联网搜索
        // 此处废弃联网, 前端固定为 false
//        if (networkSearch) {
//            advisors.add(new NetworkSearchAdvisor(searXNGService, searchResultContentFetcherService));
//        } else {
//            // 添加自定义对话记忆 Advisor（以最新的 50 条消息作为记忆）
//            advisors.add(new CustomChatMemoryAdvisor(chatMessageMapper, aiChatReqVO, 50));
//        }

        advisors.add(new CustomChatMemoryAdvisor(roleMapper, chatMapper, chatMessageMapper, aiChatReqVO, 50));

        // 添加自定义打印流式对话日志 Advisor
        advisors.add(new CustomStreamLoggerAndMessage2DBAdvisor(chatMessageMapper, aiChatReqVO, transactionTemplate));

        // 应用 Advisor 集合
        chatClientRequestSpec.advisors(advisors);

        // 流式输出
        return chatClientRequestSpec.stream().content().mapNotNull(text -> AIResponse.builder().v(text).build()); // 构建返参 AIResponse

    }

    @PostMapping("/voice-chat")
    @ApiOperationLog(description = "语音对话")
    @Operation(description = "语音对话")
    public Response<?> voiceChat(@RequestBody @Validated AudioChatReqVO audioChatReqVO) {

        String chatId = audioChatReqVO.getChatId();

        // 1️⃣ 获取会话信息
        ChatDO chatDO = chatMapper.selectByUuid(chatId);
        if (Objects.isNull(chatDO)) {
            return Response.fail("会话不存在");
        }

        // 音频文件地址
        String audioFileUrl = audioChatReqVO.getAudioFileUrl();
        // 模型名称
        String modelName = audioChatReqVO.getModelName();
        // 温度值
        Double temperature = audioChatReqVO.getTemperature();

        // 2️⃣ 用户语音转文本
        String userMessage = audioChatService.recognize(audioFileUrl);

        // 3️⃣ 构建上下文
        List<Message> messageList = Lists.newArrayList();
        RoleDO roleDO = roleMapper.selectById(chatDO.getRoleId());


        // 3.1 systemPrompt
        if (roleDO.getSystemPrompt() != null) {
            messageList.add(new SystemMessage(roleDO.getSystemPrompt()));
        }

        // 3.2 最近历史消息
        List<ChatMessageDO> messages = chatMessageMapper.selectList(Wrappers.<ChatMessageDO>lambdaQuery().eq(ChatMessageDO::getChatUuid, chatId).orderByDesc(ChatMessageDO::getCreateTime).last("LIMIT 50"));

        messages.stream().sorted(Comparator.comparing(ChatMessageDO::getCreateTime)) // 升序
                .forEach(msg -> {
                    if (Objects.equals(msg.getRole(), MessageType.USER.getValue())) {
                        messageList.add(new UserMessage(msg.getContent()));
                    } else if (Objects.equals(msg.getRole(), MessageType.ASSISTANT.getValue())) {
                        messageList.add(new AssistantMessage(msg.getContent()));
                    }
                });

        // 3.3 当前用户 ASR 消息
        messageList.add(new UserMessage(userMessage));

        // 4️⃣ 调用 AI 模型
        ChatModel chatModel = OpenAiChatModel.builder().openAiApi(OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build()).build();

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel).prompt().options(OpenAiChatOptions.builder().model(modelName).temperature(temperature).build()).messages(messageList); // 直接传上下文

        String reply = chatClientRequestSpec.call().content();
        String cleanedReplyText = reply.replaceAll("\\（.*?\\）|\\(.*?\\)", "");

        // 5️⃣ 文本转语音
        String replyAudioUrl = audioChatService.synthesize(cleanedReplyText, roleDO);

        // 5.2 持久化用户 ASR 文本与 AI 回复（同一事务）
        transactionTemplate.execute(status -> {
            try {
                // 用户 ASR 文本
                chatMessageMapper.insert(ChatMessageDO.builder().chatUuid(chatId).audioUrl(audioFileUrl).content(userMessage).role(MessageType.USER.getValue()).createTime(LocalDateTime.now()).build());

                // AI 回复
                chatMessageMapper.insert(ChatMessageDO.builder().chatUuid(chatId).audioUrl(replyAudioUrl).content(reply).role(MessageType.ASSISTANT.getValue()).createTime(LocalDateTime.now()).build());

                return true;
            } catch (Exception ex) {
                status.setRollbackOnly();
                log.error("保存语音对话消息失败", ex);
            }
            return false;
        });

        // 6️⃣ 返回
        VoiceChatRspVO rspVO = VoiceChatRspVO.builder().replyText(reply).replyAudioUrl(replyAudioUrl).build();

        return Response.success(rspVO);
    }

    @PostMapping("/list")
    @ApiOperationLog(description = "查询历史对话")
    @Operation(summary = "查询历史对话")
    public PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(@RequestBody @Validated FindChatHistoryPageListReqVO findChatHistoryPageListReqVO) {
        return chatService.findChatHistoryPageList(findChatHistoryPageListReqVO);
    }

    @PostMapping("/message/list")
    @ApiOperationLog(description = "查询对话历史消息")
    @Operation(summary = "查询对话历史消息")
    public PageResponse<FindChatHistoryMessagePageListRspVO> findChatMessagePageList(@RequestBody @Validated FindChatHistoryMessagePageListReqVO findChatHistoryMessagePageListReqVO) {
        return chatService.findChatHistoryMessagePageList(findChatHistoryMessagePageListReqVO);
    }

    @PostMapping("/summary/rename")
    @ApiOperationLog(description = "重命名对话摘要")
    @Operation(summary = "重命名对话摘要")
    public Response<?> renameChatSummary(@RequestBody @Validated RenameChatReqVO renameChatReqVO) {
        return chatService.renameChatSummary(renameChatReqVO);
    }

    @PostMapping("/delete")
    @ApiOperationLog(description = "删除对话")
    @Operation(summary = "删除对话")
    public Response<?> deleteChat(@RequestBody @Validated DeleteChatReqVO deleteChatReqVO) {
        return chatService.deleteChat(deleteChatReqVO);
    }

    @PostMapping("/role/info")
    @ApiOperationLog(description = "根据对话ID获取角色信息")
    @Operation(summary = "根据对话ID获取角色信息")
    public Response<RoleDO> getChatRoleInfo(@RequestParam("chatId") String chatId) {
        try {
            // 1. 根据chatId查询对话信息
            ChatDO chatDO = chatMapper.selectByUuid(chatId);
            if (Objects.isNull(chatDO)) {
                return Response.fail("对话不存在");
            }

            // 2. 根据roleId查询角色信息
            RoleDO roleDO = roleMapper.selectById(chatDO.getRoleId());
            if (Objects.isNull(roleDO)) {
                return Response.fail("角色不存在");
            }

            return Response.success(roleDO);
        } catch (Exception e) {
            log.error("获取对话角色信息失败", e);
            return Response.fail("获取角色信息失败");
        }
    }


}