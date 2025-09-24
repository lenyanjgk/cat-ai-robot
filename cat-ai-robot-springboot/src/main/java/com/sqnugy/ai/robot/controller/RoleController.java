package com.sqnugy.ai.robot.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqnugy.ai.robot.aspect.ApiOperationLog;
import com.sqnugy.ai.robot.domain.dos.RoleDO;
import com.sqnugy.ai.robot.domain.mapper.RoleMapper;
import com.sqnugy.ai.robot.model.common.BasePageQuery;
import com.sqnugy.ai.robot.utils.PageResponse;
import com.sqnugy.ai.robot.utils.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author 江国凯
 * @Date 2025/9/24
 * @description: 角色
 */
@RestController
@RequestMapping("/role")
@Tag(name = "角色管理")
@Slf4j
public class RoleController {

    @Resource
    private RoleMapper roleMapper;

    @PostMapping("/page")
    @ApiOperationLog(description = "分页查询角色")
    @Operation(description = "分页查询角色")
    public PageResponse<RoleDO> page(@RequestBody @Validated BasePageQuery query) {
        Page<RoleDO> page = new Page<>(query.getCurrent(), query.getSize());
        Page<RoleDO> result = roleMapper.selectPage(page,
                Wrappers.<RoleDO>lambdaQuery().orderByDesc(RoleDO::getCreateTime));
        List<RoleDO> records = result.getRecords();
        return PageResponse.success(result, records);
    }

    @PostMapping("/get")
    @ApiOperationLog(description = "根据ID查询角色")
    @Operation(description = "根据ID查询角色")
    public Response<RoleDO> get(@RequestParam("id") Long id) {
        RoleDO role = roleMapper.selectById(id);
        return Response.success(role);
    }

    @PostMapping("/create")
    @ApiOperationLog(description = "创建角色")
    @Operation(description = "创建角色")
    public Response<?> create(@RequestBody @Validated RoleDO role) {
        role.setId(null);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return Response.success();
    }

    @PostMapping("/update")
    @ApiOperationLog(description = "更新角色")
    @Operation(description = "更新角色")
    public Response<?> update(@RequestBody @Validated RoleDO role) {
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
        return Response.success();
    }

    @PostMapping("/delete")
    @ApiOperationLog(description = "删除角色")
    @Operation(description = "删除角色")
    public Response<?> delete(@RequestParam("id") Long id) {
        roleMapper.deleteById(id);
        return Response.success();
    }
}
