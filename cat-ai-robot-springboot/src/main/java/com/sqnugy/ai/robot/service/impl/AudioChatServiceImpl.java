package com.sqnugy.ai.robot.service.impl;

import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.google.gson.Gson;
import com.sqnugy.ai.robot.model.dto.ParaformerTranscriptionRespDTO;
import com.sqnugy.ai.robot.model.dto.TranscriptionTaskResponse;
import com.sqnugy.ai.robot.service.AudioChatService;
import com.sqnugy.ai.robot.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 吴光耀，江国凯
 * @Date 2025/9/23
 * @description:
 */
@Slf4j
@Service
public class AudioChatServiceImpl implements AudioChatService {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Resource
    private OkHttpClient okHttpClient;

    @Resource
    private Gson gson;

    @Resource
    private MinioUtil minioUtil;

    public String recognize(String audioFileUrl) {

        List<String> audioFileUrls = new ArrayList<>();
        audioFileUrls.add(audioFileUrl);

        // 1️⃣ 语音转文本
        TranscriptionParam param =
                TranscriptionParam.builder()
                        .model("paraformer-v2")
                        // “language_hints”只支持paraformer-v2模型
                        .parameter("language_hints", new String[]{"zh", "en"})
                        .fileUrls(audioFileUrls)
                        .build();


        try {
            Transcription transcription = new Transcription();
            // 提交转写请求
            TranscriptionResult result = transcription.asyncCall(param);
            System.out.println("RequestId: " + result.getRequestId());

            // 阻塞等待任务完成
            result = transcription.wait(TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));

            // ---------------- 第一步：解析 taskResponse ----------------
            String outputJson = gson.toJson(result.getOutput()); // 直接用 Gson
            System.out.println("Output JSON: " + outputJson);

            TranscriptionTaskResponse taskResponse = gson.fromJson(outputJson, TranscriptionTaskResponse.class);

            if (taskResponse.getResults() == null || taskResponse.getResults().isEmpty()) {
                throw new RuntimeException("results 为空");
            }

            String transcriptionUrl = taskResponse.getResults().get(0).getTranscriptionUrl();
            if (transcriptionUrl == null) {
                throw new RuntimeException("transcriptionUrl 为空");
            }

            // ---------------- 第二步：用 OkHttp 请求转写详情 ----------------
            HttpUrl httpUrl = HttpUrl.parse(transcriptionUrl).newBuilder()
                    .addQueryParameter("format", "json")
                    .build();

            Request request = new Request.Builder()
                    .url(httpUrl)
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String detailJson = response.body().string();

                    // 第三步：映射成 DTO
                    ParaformerTranscriptionRespDTO detail = gson.fromJson(detailJson, ParaformerTranscriptionRespDTO.class);

                    String text = detail.getTranscripts().get(0).getText();
                    System.out.println("转写结果: " + text);
                    return text;
                } else {
                    throw new RuntimeException("获取转写详情失败: " + response.code());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===== 1️⃣ ASR 语音转文本 =====
    public String recognize(byte[] audioBytes, String format) {
        // 这里假设调用阿里百炼 HTTP API
        // 可以用 RestTemplate 或 HttpClient 发送 multipart/form-data
        // audioBytes -> 音频文件
        // format -> wav / mp3
        String asrResult;

        // 示例（伪代码）：
        // HttpPost request = new HttpPost(aliyunAsrUrl);
        // request.addHeader("Authorization", "APPCODE " + apiKey);
        // MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        // builder.addBinaryBody("audio", audioBytes, ContentType.DEFAULT_BINARY, "file." + format);
        // request.setEntity(builder.build());
        // HttpResponse response = httpClient.execute(request);
        // asrResult = EntityUtils.toString(response.getEntity());

        // 这里直接返回伪结果，实际需要解析 JSON
        asrResult = "模拟识别文本";
        return asrResult;
    }

    // ===== 2️⃣ 调用 AI 模型生成文本 =====
//    public ChatClient.ChatClientRequestSpec options(OpenAiChatOptions options) {
//        // 构建 ChatModel
//        ChatModel chatModel = OpenAiChatModel.builder()
//                .openAiApi(OpenAiApi.builder()
//                        .baseUrl("https://api.openai.com") // 或你的 baseUrl
//                        .apiKey("你的API_KEY")
//                        .build())
//                .build();
//
//        // 返回 ChatClientRequestSpec，用于链式调用 user() 和 execute()
//        return ChatClient.create(chatModel).prompt().options(options);
//    }


    public String synthesize(String text, String model, String voice) {
        try {
            // cosyvoice-v2 模型音色地址 https://help.aliyun.com/zh/model-studio/text-to-speech#3a8c7759a4yyx
            // 1️⃣ 构建语音合成相关参数
            SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .voice(voice)
                    .build();

            // 2️⃣ 同步调用语音合成大模型，获取 ByteBuffer
            SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
            ByteBuffer audioBuffer = synthesizer.call(text);

            // 3️⃣ 转成 byte[]
            byte[] audioBytes = new byte[audioBuffer.remaining()];
            audioBuffer.get(audioBytes);

            String url = minioUtil.uploadBytes(audioBytes, "wav", "audio/wav");

            // 5️⃣ 返回访问 URL
            return url;

        } catch (Exception e) {
            log.error("语音合成或上传失败", e);
            throw new RuntimeException("语音合成失败");
        }
    }


}
