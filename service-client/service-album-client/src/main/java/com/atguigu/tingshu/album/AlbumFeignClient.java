package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 专辑模块远程调用Feign接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-album", fallback = AlbumDegradeFeignClient.class)
public interface AlbumFeignClient {

    /**
     * 根据id集合批量查询声音信息
     * @return
     */
    @GetMapping("/api/album/albumList")
    Result<List<TrackInfo>> batchGetTracksByIds(@RequestParam("trackIds") Set<Long> trackIds);

    /**
     * 根据id集合批量查询专辑信息
     * @return
     */
    @GetMapping("/api/album")
    Result<List<AlbumInfo>> batchGetAlbumsByIds(@RequestParam("albumIds") Set<Long> albumIds);

    /**
     * 根据声音ID+声音数量 获取下单付费声音列表
     * 提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表
     * /api/album/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}
     * @param trackId    声音ID
     * @param trackCount 数量
     * @return
     */
    @GetMapping("/api/album/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> getWaitBuyTrackInfoList(@PathVariable Long trackId, @PathVariable int trackCount);

    /**
     * 查询声音信息
     * 根据id查询声音详情
     * /api/album/trackInfo/getTrackInfo/{id}
     * @param id
     * @return
     */
    @GetMapping("/api/album/trackInfo/getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id);

    /**
     * 查询所有一级分类列表
     * /api/album/category/findAllCategory1
     * @return
     */
    @GetMapping("/api/album/category/findAllCategory1")
    public Result<List<BaseCategory1>> getAllCategory1();

    /**
     * 根据专辑ID获取专辑统计信息
     * /api/album/albumInfo/getAlbumStatVo/{albumId}
     * @param albumId
     * @return
     */
    @GetMapping("/api/album/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId);

    /**
     * 根据一级分类Id查询三级分类列表
     * /api/album/category/findTopBaseCategory3/{category1Id}
     * @param category1Id
     * @return
     */
    @GetMapping("/api/album/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> getTop7BaseCategory3(@PathVariable Long category1Id);

    /**
     * 根据三级分类Id 获取到分类信息
     * api/album/category/getCategoryView/{category3Id}
     * @param category3Id
     * @return
     */
    @GetMapping("/api/album/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id);

    /**
     * 根据ID查询专辑信息
     * @param id
     * @return
     */
    @GetMapping("/api/album/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id);

}
