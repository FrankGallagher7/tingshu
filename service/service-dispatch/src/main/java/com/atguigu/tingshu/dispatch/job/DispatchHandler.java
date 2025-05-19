package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.SearchFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchHandler {

    @Autowired
    private SearchFeignClient searchFeignClient;

    /**
     * 定时执行热门专辑更新
     */
    @XxlJob("updateHotAlbumJob")
    public void updateHotAlbumJob() {
        log.info("定时执行热门专辑更新");
        searchFeignClient.updateLatelyAlbumRanking();
        XxlJobHelper.log("XXL-JOB, 定时执行热门专辑更新");
    }

    @XxlJob("firstJobHandler")
    public void firstJobHandler() {
        log.info("xxl-job项目集成测试");
        System.out.println("测试任务执行了。。。。");
    }

}