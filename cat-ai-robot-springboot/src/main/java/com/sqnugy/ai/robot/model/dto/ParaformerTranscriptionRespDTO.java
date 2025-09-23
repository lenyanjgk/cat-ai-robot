package com.sqnugy.ai.robot.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParaformerTranscriptionRespDTO {

    @JsonProperty("file_url")
    private String fileUrl;

    private Properties properties;

    private List<Transcript> transcripts;

    @Data
    public static class Properties {

        @JsonProperty("audio_format")
        private String audioFormat;

        private List<Integer> channels;

        @JsonProperty("original_sampling_rate")
        private int originalSamplingRate;

        @JsonProperty("original_duration_in_milliseconds")
        private int originalDurationInMilliseconds;
    }

    @Data
    public static class Transcript {

        @JsonProperty("channel_id")
        private int channelId;

        @JsonProperty("content_duration_in_milliseconds")
        private int contentDurationInMilliseconds;

        private String text;

        private List<Sentence> sentences;
    }

    @Data
    public static class Sentence {

        @JsonProperty("begin_time")
        private int beginTime;

        @JsonProperty("end_time")
        private int endTime;

        private String text;

        private List<Word> words;
    }

    @Data
    public static class Word {

        private int beginTime;

        private int endTime;

        private String text;

        private String punctuation;
    }
}