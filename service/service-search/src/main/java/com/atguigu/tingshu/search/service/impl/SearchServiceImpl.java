package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    private static final String INDEX_NAME = "albuminfo";
    private static final String SUGGEST_INDEX_NAME = "suggestinfo";

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private RedisTemplate redisTemplate;


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

        // 上架成功建立索引库
        this.saveSuggetIndex(albumInfoIndex);

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
//
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

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;
    /**
     * 构建提词库
     * @param albumInfoIndex
     */
    @Override
    public void saveSuggetIndex(AlbumInfoIndex albumInfoIndex) {
        //1.将专辑标题内容作为提词原始记录存入提词库
        SuggestIndex suggestIndex = new SuggestIndex();
        //1.1 封装提词记录主键 - 跟专辑文档主键一致
        suggestIndex.setId(albumInfoIndex.getId().toString());
        //1.2 封装提词原始内容 给用户展示提词内容（专辑名称）
        String albumTitle = albumInfoIndex.getAlbumTitle();
        suggestIndex.setTitle(albumTitle);
        //1.3 用于提词字段：汉字提词
        suggestIndex.setKeyword(new Completion(new String[]{suggestIndex.getTitle()}));
        //1.4 用于提词字段：拼音提词 将中文转为拼音 采用""
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinyinUtil.getPinyin(albumTitle, "")}));
        //1.4 用于提词字段：首字母提词 将中文转为拼音首字母 采用""分割
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinyinUtil.getFirstLetter(albumTitle, "")}));
        //2.执行保存
        suggestIndexRepository.save(suggestIndex);
    }

    /**
     * 下架专辑-删除文档
     * @param albumId
     */
    @Override
    public void lowerAlbum(Long albumId) {

        albumInfoIndexRepository.deleteById(albumId);
    }

    /**
     * 专辑检索
     * @param albumIndexQuery
     * @return
     */
    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {

        // 构建请求对象
        SearchRequest searchRequest = this.buildDSL(albumIndexQuery);

        //一、构建检索请求对象SearchReqeust对象
        System.err.println("本次检索DSL：复制到Kibana中验证");
        System.err.println(searchRequest.toString());

        // 执行查询
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 转换查询条件

        AlbumSearchResponseVo albumSearchResponseVo = this.parseResult(searchResponse, albumIndexQuery);

        return albumSearchResponseVo;
    }

    /**
     * 查询1级分类下置顶3级分类下包含分类热门专辑
     * @param category1Id
     * @return
     */
    @Override
    public List<Map<String, Object>> getTopCategory3HotAlbumList(Long category1Id) {

        try {
            //1.根据1级分类ID远程调用专辑服务获取置顶前7个三级分类集合
            //1.1 远程调用专辑服务获取置顶三级分类集合
            List<BaseCategory3> baseCategory3List = albumFeignClient.getTop7BaseCategory3(category1Id).getData();
            Assert.notNull(baseCategory3List, "一级分类{}未包含置顶三级分类", category1Id);
            //1.2 获取所有置顶三级分类ID集合
            List<Long> baseCategory3IdList = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
            //1.3 将三级分类集合转为Map<三级分类ID，三级分类对象> 方便解析结果封装三级分类对象
            //对BaseCategory3集合处理，转为Map Map中Key:ID，Map中val:三级分类对象BaseCategory3
            Map<Long, BaseCategory3> category3Map = baseCategory3List.stream()
                    .collect(Collectors.toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));
            //1.4 将置顶三级分类ID转为FieldValue类型
            List<FieldValue> fieldValueList = baseCategory3IdList.stream()
                    .map(baseCategory3Id -> FieldValue.of(baseCategory3Id))
                    .collect(Collectors.toList());
            //2.检索ES获取置顶三级分类（7个）不同置顶三级分类下热度前6个的专辑列表
            //2.1 采用ES检索方法，通过lambda构建请求参数：query,size,aggs
            SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(
                    s -> s.index(INDEX_NAME)
                            .query(q -> q.terms(t -> t.field("category3Id").terms(tf -> tf.value(fieldValueList))))
                            .size(0)
                            .aggregations("category3Agg", a -> a.terms(
                                    t -> t.field("category3Id").size(10)
                            ).aggregations("top6Agg", a1 -> a1.topHits(t -> t.size(6).sort(sort -> sort.field(f -> f.field("hotScore").order(SortOrder.Desc)))))),
                    AlbumInfoIndex.class);
            //3.解析ES响应聚合
            System.out.println(searchResponse);
            //3.1 获取三级分类聚合结果对象
            Aggregate category3Agg = searchResponse.aggregations().get("category3Agg");
            //3.2 获取三级分类聚合“桶”集合 由于三级分类ID字段类型为Long调用lterms方法
            Buckets<LongTermsBucket> buckets = category3Agg.lterms().buckets();
            List<LongTermsBucket> bucketList = buckets.array();
            if (CollectionUtil.isNotEmpty(bucketList)) {
                //3.3 遍历“桶”集合，每遍历一个“桶”处理某个置顶三级分类热门专辑
                List<Map<String, Object>> listMap = bucketList.stream().map(bucket -> {
                    Map<String, Object> map = new HashMap<>();
                    //3.3.1 处理热门专辑->分类
                    long category3Id = bucket.key();
                    BaseCategory3 baseCategory3 = category3Map.get(category3Id);
                    map.put("baseCategory3", baseCategory3);
                    //3.3.2 处理热门专辑->专辑列表
                    //3.3.2.1 继续下钻获取子聚合得到当前分类下热门专辑
                    Aggregate top6Agg = bucket.aggregations().get("top6Agg");
                    List<Hit<JsonData>> hits = top6Agg.topHits().hits().hits();
                    if (CollectionUtil.isNotEmpty(hits)) {
                        List<AlbumInfoIndex> hotAlbumList = hits.stream().map(hit -> {
                            //获取专辑JSON对象类型
                            JsonData source = hit.source();
                            return JSON.parseObject(source.toString(), AlbumInfoIndex.class);
                        }).collect(Collectors.toList());
                        map.put("list", hotAlbumList);
                    }
                    return map;
                }).collect(Collectors.toList());
                return listMap;
            }
        } catch (Exception e) {
            log.error("[检索服务]首页热门专辑异常：{}", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * 更新所有分类下排行榜-手动调用
     */
    @Override
//    @SneakyThrows
    public void updateLatelyAlbumRanking() {

        try {
            // 获取所有一级分类
            List<BaseCategory1> category1List =  albumFeignClient.getAllCategory1().getData();
            Assert.notNull(category1List, "查询所有一级分类异常");
            // 处理集合
            List<Long> baseCategory1List = category1List.stream().map(baseCategory1 -> baseCategory1.getId()).collect(Collectors.toList());
            // 遍历集合  循环嵌套查询
            for (Long category1Id : baseCategory1List) {
                // 设置查询的5个维度
                String[] rankingDimensionArray = new String[]{"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};

                // 循环维度
                for (String rankingDimension : rankingDimensionArray) {

                    // 构建DSL语句
                    SearchResponse<AlbumInfoIndex> searchResponses = elasticsearchClient.search(s ->
                            s.index(INDEX_NAME)
                                    .query(q -> q.term(t -> t.field("category1Id").value(category1Id)))
                                    .sort(sort -> sort.field(f -> f.field(rankingDimension).order(SortOrder.Desc)))
                                    .size(20)
                            , AlbumInfoIndex.class);

                    // 解析es查询结果
                    List<Hit<AlbumInfoIndex>> hits = searchResponses.hits().hits();
                    // 处理获取的集合数据
                    List<AlbumInfoIndex> albumInfoIndexList = hits.stream().map(hit -> hit.source()).collect(Collectors.toList());

                    // 定义存储key
                    String key = RedisConstant.RANKING_KEY_PREFIX + category1Id;

                    //存储
                    redisTemplate.opsForHash().put(key, rankingDimension, albumInfoIndexList);
                }
            }
        } catch (Exception e) {
            log.error("[搜索服务]更新排行榜异常：{}", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * 获取排行榜
     *
     * 获取指定1级分类下不同排序方式榜单列表-从Redis中获取
     * @param category1Id
     * @param dimension
     * @return
     */
    @Override
    public List<AlbumInfoIndex> findRankingList(String category1Id, String dimension) {

        // 定义存储排行榜的key
        String key = RedisConstant.RANKING_KEY_PREFIX + category1Id;
        // 判断
        Boolean flag = redisTemplate.opsForHash().hasKey(key, dimension);
        if (flag) { // 有key
            List<AlbumInfoIndex> list = (List<AlbumInfoIndex>) redisTemplate.opsForHash().get(key, dimension);
            return list;
        }

        return null;
    }

    /**
     * 根据用户录入部分关键字进行自动补全
     * @param keyword
     * @return
     */
    @Override
    public List<String> completeSuggest(String keyword) {
        try {
            //1.根据用户录入关键字进行建议提词请求发起-DSL
            SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search(
                    s -> s.index(SUGGEST_INDEX_NAME)
                            .suggest(s1 -> s1.suggesters("mySuggestKeyword", fs -> fs.prefix(keyword).completion(c -> c.field("keyword").size(10).skipDuplicates(true)))
                                    .suggesters("mySuggestPinyin", fs -> fs.prefix(keyword).completion(c -> c.field("keywordPinyin").size(10).skipDuplicates(true)))
                                    .suggesters("mySuggestSequence", fs -> fs.prefix(keyword).completion(c -> c.field("keywordSequence").size(10).skipDuplicates(true)))
                            )
                    , SuggestIndex.class
            );
            //2.解析建议词响应结果，将结果进行去重-LinkedHashSet-保证有序
            Set<String> hashSet = new LinkedHashSet<>();
            hashSet.addAll(this.parseSuggestResult("mySuggestKeyword", searchResponse));
            hashSet.addAll(this.parseSuggestResult("mySuggestPinyin", searchResponse));
            hashSet.addAll(this.parseSuggestResult("mySuggestSequence", searchResponse));
            // 如果建议词记录数大于等于10，返回前10个建议词
            if (hashSet.size() >= 10) {
                return new ArrayList<>(hashSet).subList(0, 10);
            }
            //3.如果建议词记录数小于10，采用全文查询专辑索引库尝试补全-DSL-模糊匹配
            SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(
                    s -> s.index(INDEX_NAME).query(q -> q.match(m -> m.field("albumTitle").query(keyword))),
                    AlbumInfoIndex.class

            );
            //解析检索结果，将结果放入HashSet
            List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
            if (CollectionUtil.isNotEmpty(hits)) {
                for (Hit<AlbumInfoIndex> hit : hits) {
                    AlbumInfoIndex albumInfoIndex = hit.source();
                    hashSet.add(albumInfoIndex.getAlbumTitle());
                    if (hashSet.size() >= 10) {
                        break;
                    }
                }
            }
            //4.最多返回10个自动补全提示词
            return new ArrayList<>(hashSet);
        } catch (Exception e) {
            log.error("[搜索服务]建议词自动补全异常：{}", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 解析建议词结果
     *
     * @param suggestName    自定义建议名称
     * @param searchResponse ES响应结果对象
     * @return
     */
    @Override
    public Collection<String> parseSuggestResult(String suggestName, SearchResponse<SuggestIndex> searchResponse) {
        //1.获取指定自定义建议词名称获取建议结果
        List<Suggestion<SuggestIndex>> suggestionList = searchResponse.suggest().get(suggestName);
        //2.获取建议自动补全对象
        List<String> list = new ArrayList<>();
        suggestionList.forEach(suggestIndexSuggestion -> {
            //3.获取options中自动补全结果
            for (CompletionSuggestOption<SuggestIndex> suggestOption : suggestIndexSuggestion.completion().options()) {
                SuggestIndex suggestIndex = suggestOption.source();
                list.add(suggestIndex.getTitle());
            }
        });
        return list;
    }

    /**
     * 解析结果集，转换返回值对象类型
     * @param searchResponse
     * @return
     */
    private AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery queryVo) {
        //1.构建响应VO对象
        AlbumSearchResponseVo vo = new AlbumSearchResponseVo();
        //2.封装分页信息（总记录数、总页数、页码、页大小）
        //当前页
        vo.setPageNo(queryVo.getPageNo());
        //每页条数
        Integer pageSize = queryVo.getPageSize();
        vo.setPageSize(pageSize);
        //1.1 从ES响应结果中得到总记录数
        long total = searchResponse.hits().total().value();
        vo.setTotal(total);
        //1.2 动态计算总页数
        long totalPages = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
        vo.setTotalPages(totalPages);
        //3.封装检索到业务数据（专辑搜索Vo集合）
        List<Hit<AlbumInfoIndex>> hitList = searchResponse.hits().hits();
        if (CollectionUtil.isNotEmpty(hitList)) {
            List<AlbumInfoIndexVo> infoIndexVoList = hitList.stream().map(hit -> {
                //将获取到的文档对象AlbumInfoIndex类型转为AlbumInfoIndexVo类型
                AlbumInfoIndexVo albumInfoIndexVo = BeanUtil.copyProperties(hit.source(), AlbumInfoIndexVo.class);
                //处理高亮片段
                Map<String, List<String>> highlightMap = hit.highlight();
                if(CollectionUtil.isNotEmpty(highlightMap) && highlightMap.containsKey("albumTitle")){
                    String highlightAlbumTitle = highlightMap.get("albumTitle").get(0);
                    albumInfoIndexVo.setAlbumTitle(highlightAlbumTitle);
                }
                return albumInfoIndexVo;
            }).collect(Collectors.toList());
            //封装集合到结果集中
            vo.setList(infoIndexVoList);
        }
        //4.返回自定义VO对象
        return vo;
    }

    /**
     * 构建DSL数据，返回请求对象
     * @param albumIndexQuery
     * @return
     */
    private SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {
        //1.创建检索请求构建器对象-封装检索索引库 及 所有检索DSL语句
        SearchRequest.Builder builder = new SearchRequest.Builder();
        // 指定查询索引
        builder.index(INDEX_NAME);

        //2.设置请求体参数"query",处理查询条件（关键字、分类、标签）
        //2.1 创建最外层bool组合条件对象
        BoolQuery.Builder allBoolQueryBuilder = new BoolQuery.Builder();

        //2.2 处理关键字查询条件 采用must必须满足，包含bool组合三个子条件，三个子条件或者关系
        String keyword = albumIndexQuery.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            BoolQuery.Builder keyWordBoolQueryBuilder = new BoolQuery.Builder();
            //2.2.1 should 设置匹配查询专辑标题
            keyWordBoolQueryBuilder.should(s -> s.match(m -> m.field("albumTitle").query(keyword)));
            //2.2.2 should 设置匹配查询专辑简介
            keyWordBoolQueryBuilder.should(s -> s.match(m -> m.field("albumIntro").query(keyword)));
            //2.2.3 should 设置精确查询作者名称
            keyWordBoolQueryBuilder.should(s -> s.term(t -> t.field("announcerName").value(keyword)));
            //添加条件到最外出的booL对象
            allBoolQueryBuilder.must(keyWordBoolQueryBuilder.build()._toQuery());
        }
        //2.3 处理分类ID查询条件
        // 三级分类
        if (albumIndexQuery.getCategory1Id() != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(albumIndexQuery.getCategory1Id())));
        }
        if (albumIndexQuery.getCategory2Id() != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
        }
        if (albumIndexQuery.getCategory3Id() != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
        }

        //2.4 处理标签查询条件(可能有多个)
        // 专辑属性设置---nested类型
        List<String> attributeList = albumIndexQuery.getAttributeList();
        //2.4.1 判断是否提交标签过滤条件
        if (CollectionUtil.isNotEmpty(attributeList)) {
            //2.4.2 每遍历一个标签，设置Nested查询
            for (String attribute : attributeList) {
                // 截取数据
                String[] split = attribute.split(":");
                if (split != null && split.length == 2) {
                    //2.4.3 在当前Nested查询中包含bool组合条件查询  - 采用传统方式
               /* NestedQuery.Builder nestedQueryBuilder = new NestedQuery.Builder();
                nestedQueryBuilder.path("attributeValueIndexList");

                BoolQuery.Builder nestedBoolQueryBuilder = new BoolQuery.Builder();
                //2.4.4 每个bool查询条件must精确查询标签ID
                nestedBoolQueryBuilder.must(m->m.term(t->t.field("attributeValueIndexList.attributeId").value(split[0])));
                //2.4.5 每个bool查询条件must精确查询标签值ID
                nestedBoolQueryBuilder.must(m->m.term(t->t.field("attributeValueIndexList.valueId").value(split[1])));
                nestedQueryBuilder.query(nestedBoolQueryBuilder.build()._toQuery());
                //2.4.6 将构建好Nested查询封装到最外层bool查询filter中
                allBoolQueryBuilder.filter(nestedQueryBuilder.build()._toQuery());*/
                    //2.4.7 采用Lambda表达式简化
                    allBoolQueryBuilder.filter(
                            f -> f.nested(n -> n.path("attributeValueIndexList")
                                    .query(q -> q.bool(
                                            b -> b.must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0])))
                                                    .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(split[1])))
                                    ))
                            ));
                }
            }
        }

        //2.5 将最外层bool组合条件对象设置到请求体参数"query"中
        builder.query(allBoolQueryBuilder.build()._toQuery());

        //3.设置请求体参数"from","size" 处理分页
        // 分页
        int from = (albumIndexQuery.getPageNo() - 1) * albumIndexQuery.getPageSize();
        builder.from(from).size(albumIndexQuery.getPageSize());

        //4.设置请求体参数"sort" 处理排序（动态 综合、播放量、发布时间）
        //4.1 判断参数排序是否提交 提交形式： 排序字段（1：综合 2：播放量 3：发布时间）:排序方式
        String order = albumIndexQuery.getOrder();
        if (StringUtils.isNotBlank(order)) {
            //4.2 按照冒号对查询条件进行分割得到数组
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                //4.3 判断得到排序字段
                String orderField = ""; // 排序方式split[1] ? SortOrder.Asc : SortOrder.Desc
                switch (split[0]) {
                    case "1":
                        orderField = "hotScore";
                        break;
                    case "2":
                        orderField = "playStatNum";
                        break;
                    case "3":
                        orderField = "createTime";
                        break;
                }
                //4.4 设置排序
                String finalOrderField = orderField;
                builder.sort(s -> s.field(f -> f.field(finalOrderField).order("asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc)));
            }
        }

        //5.设置请求体参数"highlight" 处理高亮，前提：用户录入关键字
        if (StringUtils.isNotBlank(keyword)) {
            builder.highlight(h -> h.fields("albumTitle", hf -> hf.preTags("<font style='color:red'>").postTags("</font>")));
        }

        //6.设置请求体参数"_source" 处理字段指定
        // 过滤结果
        builder.source(s -> s.filter(f -> f.excludes("category1Id",
                "category2Id",
                "category3Id",
                "attributeValueIndexList.attributeId",
                "attributeValueIndexList.valueId")));

        //7.调用构建器builder返回检索请求对象
        return builder.build();
    }
}
