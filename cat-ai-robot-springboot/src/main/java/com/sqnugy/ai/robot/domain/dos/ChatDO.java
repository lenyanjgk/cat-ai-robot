package com.sqnugy.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: sqnugy,江国凯
 * @Date: 2025/9/24 11:32
 * @Version: v1.0.0
 * @Description: 对话 DO 实体类
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_chat")
public class ChatDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private String uuid;

    private String summary;

    private String systemPrompt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
