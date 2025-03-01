package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 获取当前用户声音分页列表
     * @param listVoPage
     * @param trackInfoQuery
     * @return
     */
    Page<TrackListVo> selectUserTrackPage(Page<TrackListVo> listVoPage, @Param("vo") TrackInfoQuery trackInfoQuery);

    /**
     * 更新声音排序
     * @param albumId
     * @param orderNum
     */
    void updateOrderNum(@Param("albumId") Long albumId, @Param("orderNum") Integer orderNum);

    /**
     * 用于小程序端专辑页面展示分页声音列表，动态根据用户展示声音付费标识
     * @param pageInfo
     * @param albumId
     * @param userId
     * @return
     */
    Page<AlbumTrackListVo> selectAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, @Param("albumId") Long albumId, Long userId);
}
