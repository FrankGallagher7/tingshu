package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    /**
     * 上架专辑-导入索引库
     *
     * @param albumId
     */
    @Override
    public void upperAlbum(Long albumId) {


        //1.构建索引库文档对象
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        //2.封装专辑及专辑标签属性-远程调用专辑服务获取专辑信息（包含专辑标签列表）
        //2.1 处理专辑基本信息 不依赖其他任务，当前任务得有返回值
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑不存在，专辑ID{}", albumId);
            BeanUtil.copyProperties(albumInfo, albumInfoIndex);
            //2.2 处理专辑标签列表 A
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)) {
                List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.stream().map(albumAttributeValue -> {
                    //将专辑标签集合泛型从AlbumAttributeValue转为AttributeValueIndex
                    AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                    attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
                    attributeValueIndex.setValueId(albumAttributeValue.getValueId());
                    return attributeValueIndex;
                }).collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            }
            return albumInfo;
        }, threadPoolExecutor);

        //3.封装分类信息-远程调用专辑服务获取分类视图对象 依赖专辑异步任务，当前任务不需要返回值
        CompletableFuture<Void> basecategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "分类不存在，分类ID：{}", albumInfo.getCategory3Id());
            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        }, threadPoolExecutor);

        //4.封装主播名称-远程调用用户服务获取用户信息
        CompletableFuture<Void> userInfoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "主播信息不存在，主播ID：{}", albumInfo.getUserId());
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        }, threadPoolExecutor);

        //5.TODO 封装统计信息，采用产生随机值 以及专辑热度
        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {
            //5.1 随机为专辑产生播放量，订阅量，购买量，评论量
            int num1 = RandomUtil.randomInt(1000, 2000);
            int num2 = RandomUtil.randomInt(500, 1000);
            int num3 = RandomUtil.randomInt(200, 400);
            int num4 = RandomUtil.randomInt(100, 200);
            albumInfoIndex.setPlayStatNum(num1);
            albumInfoIndex.setSubscribeStatNum(num2);
            albumInfoIndex.setBuyStatNum(num3);
            albumInfoIndex.setCommentStatNum(num4);

            //5.2 基于统计值计算出专辑得分 为不同统计类型设置不同权重
            BigDecimal bigDecimal1 = new BigDecimal(num4).multiply(new BigDecimal("0.4"));
            BigDecimal bigDecimal2 = new BigDecimal(num3).multiply(new BigDecimal("0.3"));
            BigDecimal bigDecimal3 = new BigDecimal(num2).multiply(new BigDecimal("0.2"));
            BigDecimal bigDecimal4 = new BigDecimal(num1).multiply(new BigDecimal("0.1"));
            BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
            albumInfoIndex.setHotScore(hotScore.doubleValue());
        }, threadPoolExecutor);


        //6.组合异步任务对象-需求以上四个异步任务必须全部执行完毕，主线程继续
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                statCompletableFuture,
                basecategoryViewCompletableFuture,
                userInfoCompletableFuture
        ).join();

        //7.将索引库文档对象存入索引库
        albumInfoIndexRepository.save(albumInfoIndex);
//
//        //创建封装数据实体
//        //根据专辑ID查询专辑信息
//        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
//        //判断
//        Assert.notNull(albumInfo, "专辑信息不存在，专辑ID：{}", albumId);
//        //封装专辑数据
//        AlbumInfoIndex albumInfoIndex = BeanUtil.copyProperties(albumInfo, AlbumInfoIndex.class);
//        //封装数据
//        //设置上架时间
//        albumInfoIndex.setCreateTime(new Date());
//
//        //封装专辑属性信息
//        List<AlbumAttributeValue> attributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
//        if (CollectionUtil.isNotEmpty(attributeValueVoList)) {
//            //数据转换
//            List<AttributeValueIndex> attributeValueIndexList = attributeValueVoList.stream().map(albumAttributeValue -> {
//
//                return BeanUtil.copyProperties(albumAttributeValue, AttributeValueIndex.class);
//            }).collect(Collectors.toList());
//
//            // 设置专辑属性集合
//            albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
//        }
//
//        // 设置主播名称
//        UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
//        // 判断
//        Assert.notNull(userInfoVo, "主播信息不存在，主播ID：{}", albumInfo.getUserId());
//        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
//
//
//        //设置三级分类
//        Result<BaseCategoryView> baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id());
//        // 判断
//        Assert.notNull(baseCategoryView, "分页查询异常，分类ID：{}", albumInfo.getCategory3Id());
//
//        // 设置一二级ID
//        albumInfoIndex.setCategory1Id(baseCategoryView.getData().getCategory1Id());
//        albumInfoIndex.setCategory2Id(baseCategoryView.getData().getCategory2Id());
//
//        //5.TODO 封装统计信息，采用产生随机值 以及专辑热度
//        //5.1 随机为专辑产生播放量，订阅量，购买量，评论量 、
//        int num1 = RandomUtil.randomInt(1000, 2000);
//        int num2 = RandomUtil.randomInt(500, 1000);
//        int num3 = RandomUtil.randomInt(200, 400);
//        int num4 = RandomUtil.randomInt(100, 200);
//        //播放量
//        albumInfoIndex.setPlayStatNum(num1);
//        //订阅
//        albumInfoIndex.setSubscribeStatNum(num2);
//        //购买量
//        albumInfoIndex.setBuyStatNum(num3);
//        //评论数
//        albumInfoIndex.setCommentStatNum(num4);
//
//
//        //5.2 基于统计值计算出专辑得分 为不同统计类型设置不同权重
//        BigDecimal bigDecimal1 = new BigDecimal(num4).multiply(new BigDecimal("0.4"));
//        BigDecimal bigDecimal2 = new BigDecimal(num3).multiply(new BigDecimal("0.3"));
//        BigDecimal bigDecimal3 = new BigDecimal(num2).multiply(new BigDecimal("0.2"));
//        BigDecimal bigDecimal4 = new BigDecimal(num1).multiply(new BigDecimal("0.1"));
//        BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
//        albumInfoIndex.setHotScore(hotScore.doubleValue());
//
//        //6.将索引库文档对象存入索引库
//        albumInfoIndexRepository.save(albumInfoIndex);

    }

    /**
     * 下架专辑-删除文档
     * @param albumId
     */
//    @Override
//    public void lowerAlbum(Long albumId) {
//
//        albumInfoIndexRepository.deleteById(albumId);
//    }
}
