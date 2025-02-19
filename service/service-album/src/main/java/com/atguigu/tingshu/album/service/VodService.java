package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    /**
     * 上传声音
     * @param file
     * @return
     */
    Map<String, String> uploadTrack(MultipartFile file);

    /**
     * 查询云点播声音信息
     * @param mediaFileId
     * @return
     */
    TrackMediaInfoVo getTrackMediainfo(String mediaFileId);

    /**
     * 删除云点播旧的声音
     * @param beforeMediaFileId
     */
    void deleteTrack(String beforeMediaFileId);
}
