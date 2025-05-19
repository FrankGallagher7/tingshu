package com.atguigu.tingshu.impl;
import com.atguigu.tingshu.SearchFeignClient;
import com.atguigu.tingshu.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchDegradeFeignClient implements SearchFeignClient {
    /**
     * 更新所有分类下排行榜
     * @return
     */
    @Override
    public Result updateLatelyAlbumRanking() {
        log.error("[搜索服务]远程调用updateLatelyAlbumRanking服务降级");
        return null;
    }
}
