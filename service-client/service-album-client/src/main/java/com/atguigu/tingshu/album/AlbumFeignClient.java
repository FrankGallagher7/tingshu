package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
