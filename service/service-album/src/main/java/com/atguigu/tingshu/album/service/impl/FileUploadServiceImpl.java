package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinioClientConfig;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.*;
import io.minio.errors.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @Override
    public String fileUpload(MultipartFile file) {
        String url = null;

        try {
            // 校验是否为图片类型是否为图片，借助IMageIO读取图片。
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new GuiguException(400, "图片格式非法");
            }

            // 创建Minio客户端-MinioClientConfig

            // 判断存储桶是否存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            if (!found) {
                //如果不存在，创建一个
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            } else {
                System.out.println("Bucket '"+minioConstantProperties.getBucketName()+"' already exists.");
            }

            // 随机生成文件名
            // 创建文件夹名称
            String folderName = "/"+ DateUtil.today()+"/";
            // 创建文件名称
            String fileName = IdUtil.randomUUID();
            // 后缀名
            String extName = FileUtil.extName(file.getOriginalFilename());

            fileName=folderName+fileName+"."+extName;

            // 实现文件上传
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(minioConstantProperties.getBucketName()).object(fileName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            //构建返回路径
            url = minioConstantProperties.getEndpointUrl()+"/"+minioConstantProperties.getBucketName()+fileName;
            System.out.println("url = " + url);
        } catch (Exception e) {
            log.error("文件上传失败！message{}",e);
            throw new RuntimeException(e);
        }

        return url;
    }
}
