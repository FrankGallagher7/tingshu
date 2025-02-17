package com.atguigu.tingshu.album.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    /**
     * 上传声音
     * @param file
     * @return
     */
    Map<String, String> uploadTrack(MultipartFile file);
}
