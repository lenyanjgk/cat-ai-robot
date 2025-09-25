package com.sqnugy.ai.robot.model.vo.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author: 吴光耀
 * @url: www.sqnugy.top
 * @date: 2026-09-25 14:07
 * @description: 删除对话
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeleteChatReqVO {

    @NotBlank(message = "对话 UUID 不能为空")
    private String uuid;

}
