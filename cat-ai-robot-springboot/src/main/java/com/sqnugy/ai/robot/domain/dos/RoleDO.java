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
 * @Author: sqnugy
 * @Date: 2025/8/11 11:32
 * @Version: v1.0.0
 * @Description: 角色 DO 实体类
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_role")
public class RoleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String introduction;

    private String systemPrompt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
