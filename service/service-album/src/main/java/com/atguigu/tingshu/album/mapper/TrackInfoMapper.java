package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 获取声音统计信息
     * @param trackId
     * @return
     */
    @Select("select\n" +
            "    track_id,\n" +
            "    max(if(stat_type='0701', stat_num, 0)) playStatNum,\n" +
            "    max(if(stat_type='0702', stat_num, 0)) collectStatNum,\n" +
            "    max(if(stat_type='0703', stat_num, 0)) praiseStatNum,\n" +
            "    max(if(stat_type='0704', stat_num, 0)) commentStatNum\n" +
            "    from track_stat where track_id = #{trackId} and is_deleted=0\n" +
            "group by track_id")
    TrackStatVo getTrackStatVo(@Param("trackId") Long trackId);
}
