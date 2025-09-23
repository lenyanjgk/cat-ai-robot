package com.sqnugy.ai.robot.service;

import com.sqnugy.ai.robot.utils.Response;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 上传文件
     * 
     * @param file
     * @return
     */
    Response<?> uploadFile(MultipartFile file);
}