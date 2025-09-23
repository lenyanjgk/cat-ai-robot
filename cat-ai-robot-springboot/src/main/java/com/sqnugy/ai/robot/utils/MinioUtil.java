package com.sqnugy.ai.robot.utils;

import com.sqnugy.ai.robot.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Component
@Slf4j
public class MinioUtil {

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private MinioClient minioClient;

    /**
     * 上传文件
     * @param file
     * @return
     * @throws Exception
     */
    public String uploadFile(MultipartFile file) throws Exception {
        // 判断文件是否为空
        if (file == null || file.getSize() == 0) {
            log.error("==> 上传文件异常：文件大小为空 ...");
            throw new RuntimeException("文件大小不能为空");
        }

        // 文件的原始名称
        String originalFileName = file.getOriginalFilename();
        // 文件的 Content-Type
        String contentType = file.getContentType();

        // 生成存储对象的名称（将 UUID 字符串中的 - 替换成空字符串）
        String key = UUID.randomUUID().toString().replace("-", "");
        // 获取文件的后缀，如 .jpg
        String suffix = originalFileName.substring(originalFileName.lastIndexOf("."));

        // 拼接上文件后缀，即为要存储的文件名
        String objectName = String.format("%s%s", key, suffix);

        log.info("==> 开始上传文件至 Minio, ObjectName: {}", objectName);
        
        // 上传文件至 Minio
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(contentType)
                .build());

        // 返回文件的访问链接
        String url = String.format("%s/%s/%s", minioProperties.getEndpoint(), minioProperties.getBucketName(), objectName);
        log.info("==> 上传文件至 Minio 成功，访问路径: {}", url);
        return url;
    }

    /**
     * 上传字节流为文件（直接提供后缀）
     *
     * @param bytes      文件字节数组
     * @param suffix     文件后缀，例如 ".png"、".jpg"
     * @param contentType 文件类型，例如 "image/png"
     * @return 文件访问 URL
     * @throws Exception
     */
    public String uploadBytes(byte[] bytes, String suffix, String contentType) throws Exception {
        if (bytes == null || bytes.length == 0) {
            log.error("==> 上传字节流异常：字节数组为空 ...");
            throw new RuntimeException("文件内容不能为空");
        }

        // 生成存储对象名称
        String key = UUID.randomUUID().toString().replace("-", "");
        if (suffix == null) {
            suffix = "";
        } else if (!suffix.startsWith(".")) {
            suffix = "." + suffix;
        }

        String objectName = key + suffix;

        log.info("==> 开始上传字节流至 Minio, ObjectName: {}", objectName);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .stream(inputStream, bytes.length, -1)
                    .contentType(contentType)
                    .build());
        }

        String url = String.format("%s/%s/%s", minioProperties.getEndpoint(), minioProperties.getBucketName(), objectName);
        log.info("==> 上传字节流至 Minio 成功，访问路径: {}", url);
        return url;
    }

}
