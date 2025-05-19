package com.atguigu.tingshu;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "service-search", fallback = SearchDegradeFeignClient.class)
public interface SearchFeignClient {

    /**
     * 更新所有分类下排行榜-手动调用
     *
     *  为定时更新首页排行榜提供调用接口
     * /api/search/albumInfo/updateLatelyAlbumRanking
     * @return
     */
    @GetMapping("/api/search/albumInfo/updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking();
}
