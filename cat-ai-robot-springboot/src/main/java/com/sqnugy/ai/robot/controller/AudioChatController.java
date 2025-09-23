package com.sqnugy.ai.robot.controller;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.sqnugy.ai.robot.domain.mapper.ChatMessageMapper;
import com.sqnugy.ai.robot.model.vo.chat.AudioChatReqVO;
import com.sqnugy.ai.robot.service.AudioChatService;
import com.sqnugy.ai.robot.service.ChatService;
import com.sqnugy.ai.robot.service.SearXNGService;
import com.sqnugy.ai.robot.service.SearchResultContentFetcherService;
import com.sqnugy.ai.robot.utils.MinioUtil;
import com.sqnugy.ai.robot.utils.Response;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import com.sqnugy.ai.robot.utils.Response;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.audio.speech.Speech;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @Author: sqnugy
 * @Date: 2025/5/22 12:25
 * @Version: v1.0.0
 * @Description: 对话
 **/
@RestController
@RequestMapping("/audio/chat")
@Tag(name = "对话")
@Slf4j
public class AudioChatController {

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
    private SearXNGService searXNGService;
    @Resource
    private SearchResultContentFetcherService searchResultContentFetcherService;

    @Resource
    private AudioChatService audioChatService;

    @Resource
    private OkHttpClient okHttpClient;

    @Resource
    private MinioUtil minioUtil;


    @PostMapping("/voice-chat")
    public Response<String> voiceChat(@RequestBody @Validated AudioChatReqVO audioChatReqVO) {

        // 音频文件地址
        String audioFileUrl = audioChatReqVO.getAudioFileUrl();
        // 模型名称
        String modelName = audioChatReqVO.getModelName();
        // 温度值
        Double temperature = audioChatReqVO.getTemperature();

        // 1️⃣ 语音文件转文本
        String userMessage = audioChatService.recognize(audioFileUrl);

        // 2️⃣ 调用 AI 模型生成响应
        // 构建 ChatModel
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();


        // 动态设置调用的模型名称、温度值
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(temperature)
                        .build())
                .user(userMessage); // 用户提示词


        // 调用模型获取响应
        ChatClient.CallResponseSpec responseSpec = chatClientRequestSpec.call();

        // 获取文本
        String reply = responseSpec.content();

        String replyAudioUrl = audioChatService.synthesize(reply);

        // 4️⃣ 返回音频
        return Response.success(replyAudioUrl);
    }

//    @PostMapping(value = "/voice-completion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mpeg")
//    @ApiOperationLog(description = "语音流式对话")
//    public Flux<DataBuffer> voiceChat(@RequestPart("file") FilePart audioFile,
//                                      @RequestPart("model") String modelName,
//                                      @RequestPart(value = "temperature", required = false) Double temperature,
//                                      @RequestPart(value = "networkSearch", required = false) Boolean networkSearch) {
//
//        // 创建转写请求参数
//        TranscriptionParam param =
//                TranscriptionParam.builder()
//                        // 若没有将API Key配置到环境变量中，需将apiKey替换为自己的API Key
//                        //.apiKey("apikey")
//                        .model("paraformer-v2")
//                        // “language_hints”只支持paraformer-v2模型
//                        .parameter("language_hints", new String[]{"zh", "en"})
//                        .fileUrls(
//                                Arrays.asList(
//                                        "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav",
//                                        "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_male2.wav"))
//                        .build();
//        try {
//            Transcription transcription = new Transcription();
//            // 提交转写请求
//            TranscriptionResult result = transcription.asyncCall(param);
//            System.out.println("RequestId: " + result.getRequestId());
//            // 阻塞等待任务完成并获取结果
//            result = transcription.wait(
//                    TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
//            // 打印结果
//            System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result.getOutput()));
//        } catch (Exception e) {
//            System.out.println("error: " + e);
//        }
//        System.exit(0);
//
//        // 1. 语音识别（ASR） → 得到用户文本
//        return Mono.fromCallable(() -> speechToTextService.recognize(audioFile))
//                .subscribeOn(Schedulers.boundedElastic()) // 异步执行耗时操作
//                .flatMapMany(userText -> {
//                    log.info("语音识别结果: {}", userText);
//
//                    // 2. 构造 AiChatReqVO，复用你现有的 chat 逻辑
//                    AiChatReqVO aiChatReqVO = new AiChatReqVO();
//                    aiChatReqVO.setMessage(userText);
//                    aiChatReqVO.setModelName(modelName);
//                    aiChatReqVO.setTemperature(temperature);
//                    aiChatReqVO.setNetworkSearch(networkSearch != null && networkSearch);
//
//                    // 3. 调用你现有的 chat 方法，获取 AI 回复的文本流（Flux<String>）
//                    Flux<String> aiTextStream = chat(aiChatReqVO)
//                            .map(AIResponse::getV) // 提取文本内容
//                            .collectList()         // 等待完整回复（TTS 通常需要完整文本）
//                            .flatMapMany(fullTextList -> {
//                                String fullText = String.join("", fullTextList);
//                                log.info("AI完整回复: {}", fullText);
//
//                                // 4. 文本转语音（TTS）→ 返回音频流 Flux<DataBuffer>
//                                return textToSpeechService.synthesize(fullText);
//                            });
//
//                    return aiTextStream;
//                });
//    }




}