package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 根据专辑ID查询专辑详情相关数据
     * @param albumId
     * @return
     */
    @Override
    public Map<String, Object> getItemInfo(Long albumId) {
        //1.创建响应结果Map对象 HashMap在多线程环境下并发读写线程不安全：导致key覆盖；导致死循环
        //采用线程安全:ConcurrentHashMap
        Map<String, Object> mapResult = new ConcurrentHashMap<>();
        //2.远程调用专辑服务获取专辑基本信息-封装albumInfo属性
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑：{}不存在", albumId);
            mapResult.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);

        //3.远程调用专辑服务获取专辑统计信息-封装albumStatVo属性
        CompletableFuture<Void> albumStatCompletableFuture = CompletableFuture.runAsync(() -> {
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "专辑统计信息：{}不存在", albumId);
            mapResult.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);

        //4.远程调用专辑服务获取专辑分类信息-封装baseCategoryView属性
        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            BaseCategoryView categoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(categoryView, "分类：{}不存在", albumInfo.getCategory3Id());
            mapResult.put("baseCategoryView", categoryView);
        }, threadPoolExecutor);

        //5.远程调用用户服务获取主播信息-封装announcer属性
        CompletableFuture<Void> announcerCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "用户：{}不存在", albumInfo.getUserId());
            mapResult.put("announcer", userInfoVo);
        }, threadPoolExecutor);

        //6.组合异步任务，阻塞等待所有异步任务执行完毕
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                albumStatCompletableFuture,
                baseCategoryViewCompletableFuture,
                announcerCompletableFuture
        ).join();

        return mapResult;
    }
}
