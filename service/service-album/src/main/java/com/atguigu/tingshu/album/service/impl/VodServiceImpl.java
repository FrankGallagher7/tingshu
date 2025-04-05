package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.common.StringUtil;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
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

    /**
     * 查询云点播声音信息
     * @param mediaFileId
     * @return
     */
    @Override
    public TrackMediaInfoVo getTrackMediainfo(String mediaFileId) {

        // 创建封装对象
        TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());

            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            // 封装声音唯一标识
            String[] fileIds1 = {mediaFileId};
            // 设置声音id请求参数
            req.setFileIds(fileIds1);

            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);
            // 输出json格式的字符串回包
//            System.out.println(AbstractModel.toJsonString(resp));
            // 获取声音详情数据，封装
            if (resp != null) {

                // 获取媒体信息集合
                MediaInfo[] mediaInfoSet = resp.getMediaInfoSet();
                if (mediaInfoSet != null && mediaInfoSet.length>0) {
                    // 获取媒体信息对象
                    MediaInfo mediaInfo = mediaInfoSet[0];
                    // 获取声音类型
                    String type = mediaInfo.getBasicInfo().getType();
                    // 获取声音大小
                    Long size = mediaInfo.getMetaData().getSize();
                    // 获取声音时长
                    Float duration = mediaInfo.getMetaData().getDuration();

                    trackMediaInfoVo.setType(type);
                    trackMediaInfoVo.setSize(size);
                    trackMediaInfoVo.setDuration(duration);
                }
            }
        } catch (Exception e) {
            log.error("[专辑服务]获取点播平台文件：{}，详情异常：{}", mediaFileId, e);
            throw new GuiguException(400,"获取云点播声音失败"+e.getMessage());
        }
        return trackMediaInfoVo;
    }

    /**
     * 删除云点播旧的声音
     * @param beforeMediaFileId
     */
    @Override
    public void deleteTrack(String beforeMediaFileId) {
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());

            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(beforeMediaFileId);
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            DeleteMediaResponse resp = client.DeleteMedia(req);
            // 输出json格式的字符串回包
//            System.out.println(AbstractModel.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.info("[专辑服务]删除云点播声音失败：{}，异常信息：{}", beforeMediaFileId, e);
            throw new GuiguException(400,"删除云点播声音失败"+e.getMessage());
        }
    }
}
