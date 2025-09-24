package com.sqnugy.ai.robot.controller;

import com.sqnugy.ai.robot.aspect.ApiOperationLog;
import com.sqnugy.ai.robot.service.FileService;
import com.sqnugy.ai.robot.utils.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@Slf4j
@Tag(name = "文件管理", description = "文件管理相关接口")
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping(value = "/upload")
    @Operation(summary = "上传文件")
    public Response<?> uploadFile(@RequestPart(value = "file") MultipartFile file) {
        return fileService.uploadFile(file);
    }

}