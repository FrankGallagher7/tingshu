package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 保存声音
     * @param trackInfoVo
     * @param userId
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);


    /**
     * 初始化统计信息
     * @param trackId
     * @param statType
     * @param statNum
     */
    void saveTrackState(Long trackId, String statType, int statNum);

    /**
     * 获取当前用户声音分页列表
     * @param trackInfoQuery
     * @return
     */
    Page<TrackListVo> findUserTrackPage(Page<TrackListVo>  listVoPage, TrackInfoQuery trackInfoQuery);

    /**
     * 修改声音信息
     * @param id
     * @param trackInfoVo
     */
    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);

    /**
     * 删除声音信息
     * @param id
     */
    void removeTrackInfo(Long id);

    /**
     * 用于小程序端专辑页面展示分页声音列表，动态根据用户展示声音付费标识
     * @param pageInfo
     * @param albumId
     * @param userId
     * @return
     */
    Page<AlbumTrackListVo> getAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long albumId, Long userId);

    /**
     * 获取当前用户分集购买声音列表
     * @param trackId
     * @return
     */
    List<Map<String, Object>> getUserWaitBuyTrackPayList(Long trackId);

    /**
     * 查询当前用户待购买声音列表（加用户已购买声音排除掉）
     * @param userId
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> getWaitBuyTrackInfoList(Long userId, Long trackId, int trackCount);

    /**
     * 根据id集合批量查询声音信息
     * @param trackIds
     * @return
     */
    List<TrackInfo> batchGetTracksByIds(Set<Long> trackIds);
}
