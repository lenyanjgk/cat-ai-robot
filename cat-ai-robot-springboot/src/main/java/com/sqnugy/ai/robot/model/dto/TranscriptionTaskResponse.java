package com.sqnugy.ai.robot.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class TranscriptionTaskResponse {

    @SerializedName("task_id")
    private String taskId;

    @SerializedName("task_status")
    private String taskStatus;

    @SerializedName("submit_time")
    private String submitTime;

    @SerializedName("scheduled_time")
    private String scheduledTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("results")
    private List<ResultItem> results;

    @SerializedName("task_metrics")
    private TaskMetrics taskMetrics;

    @Data
    public static class ResultItem {
        @SerializedName("file_url")
        private String fileUrl;

        @SerializedName("transcription_url")
        private String transcriptionUrl;

        @SerializedName("subtask_status")
        private String subtaskStatus;
    }

    @Data
    public static class TaskMetrics {
        @SerializedName("TOTAL")
        private int total;

        @SerializedName("SUCCEEDED")
        private int succeeded;

        @SerializedName("FAILED")
        private int failed;
    }
}