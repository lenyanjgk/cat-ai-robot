package com.sqnugy.ai.robot.model.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewChatReqVO {

    @Schema(description = "角色ID")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @NotBlank(message = "用户消息不能为空")
    private String message;

}
