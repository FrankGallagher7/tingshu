package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class AlbumDegradeFeignClient implements AlbumFeignClient {

    /**
     * 根据id集合批量查询声音信息
     * @return
     */
    @Override
    public Result<List<TrackInfo>> batchGetTracksByIds(Set<Long> trackIds) {
        log.error("[专辑模块]提供远程调用方法batchGetTracksByIds服务降级-->根据id集合批量查询声音信息");
        return Result.fail();
    }

    /**
     * 根据id集合批量查询专辑信息
     * @param albumIds
     * @return
     */
    @Override
    public Result<List<AlbumInfo>> batchGetAlbumsByIds(Set<Long> albumIds) {
        log.error("[专辑模块]提供远程调用方法batchGetAlbumsByIds服务降级-->根据id集合批量查询专辑信息");
        return Result.fail();
    }

    /**
     * 根据声音ID+声音数量 获取下单付费声音列表
     * 提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表
     * @param trackId    声音ID
     * @param trackCount 数量
     * @return
     */
    @Override
    public Result<List<TrackInfo>> getWaitBuyTrackInfoList(Long trackId, int trackCount) {
        log.error("[专辑模块]提供远程调用方法getWaitBuyTrackInfoList服务降级");
        return null;
    }

    /**
     * 根据id查询声音详情
     * @param id
     * @return
     */
    @Override
    public Result<TrackInfo> getTrackInfo(Long id) {
        return null;
    }

    /**
     * 查询所有一级分类列表
     * @return
     */
    @Override
    public Result<List<BaseCategory1>> getAllCategory1() {
        log.error("[专辑模块Feign调用]getAllCategory1异常");
        return null;
    }

    /**
     * 根据专辑ID获取专辑统计信息
     * @param albumId
     * @return
     */
    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("[专辑模块]提供远程调用方法getAlbumStatVo服务降级");
        return null;
    }

    /**
     * 根据一级分类Id查询三级分类列表
     * @param category1Id
     * @return
     */
    @Override
    public Result<List<BaseCategory3>> getTop7BaseCategory3(Long category1Id) {
        log.error("[专辑模块]提供远程调用getTop7BaseCategory3服务降级");
        return Result.fail();
    }

    /**
     * 根据三级分类Id 获取到分类信息
     * @param category3Id
     * @return
     */
    @Override
    public Result<BaseCategoryView> getCategoryView(Long category3Id) {
        log.error("[专辑模块]提供远程调用getCategoryView服务降级");
        return Result.fail();
    }

    /**
     * 根据ID查询专辑信息
     * @param id
     * @return
     */
    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {

        log.error("[专辑模块]提供远程调用getAlbumInfo服务降级");
        return Result.fail();
    }
}
