package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.common.StringUtil;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private VodUploadClient vodUploadClient;

    /**
     * 上传声音
     * @param file
     * @return
     */
    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {

        Map<String, String> resultMap = new HashMap<>();
        try {

            // 保存数据到本地，返回本地存储地址
            String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
            if (StringUtil.isEmpty(tempPath)) {
                throw new GuiguException(400,"上传失败，当前声音为空");
            }


            // 创建客户端对象
            // 创建请求对象，设置媒体本地上传路径。
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(tempPath);
            // 调用上传方法，传入接入点地域及上传请求。
            VodUploadResponse response = vodUploadClient.upload(vodConstantProperties.getRegion(), request);
            log.info("Upload FileId = {}", response.getFileId());
            // 获取上传后的声音id和声音播放地址
            String fileId = response.getFileId();
            String mediaUrl = response.getMediaUrl();

            resultMap.put("mediaFileId", fileId);
            resultMap.put("mediaUrl", mediaUrl);

            log.info("[专辑服务]上传音频文件到点播平台：mediaFileId：{}，mediaUrl：{}", fileId, mediaUrl);
        } catch (Exception e) {
            log.info("[专辑服务]上传音频文件到点播平台异常：文件：{}，错误信息：{}",file, e);
            throw new RuntimeException(e);
        }
        return resultMap;
    }
}
