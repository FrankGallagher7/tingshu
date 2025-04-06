package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SearchService {

    /**
     * 构建提词库
     * @param albumInfoIndex
     */
    void saveSuggetIndex(AlbumInfoIndex albumInfoIndex);


    /**
     * 上架专辑-导入索引库
     * @param albumId
     */
    void upperAlbum(Long albumId);

    /**
     * 下架专辑-删除文档
     * @param albumId
     */
    void lowerAlbum(Long albumId);

    /**
     * 专辑检索
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 查询1级分类下置顶3级分类下包含分类热门专辑
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> getTopCategory3HotAlbumList(Long category1Id);

    /**
     * 更新所有分类下排行榜-手动调用
     */
    void updateLatelyAlbumRanking();

    /**
     * 获取排行榜
     *
     * 获取指定1级分类下不同排序方式榜单列表-从Redis中获取
     * @param category1Id
     * @param dimension
     * @return
     */
    List<AlbumInfoIndex> findRankingList(String category1Id, String dimension);

    /**
     * 根据用户录入部分关键字进行自动补全
     * @param keyword
     * @return
     */
    List<String> completeSuggest(String keyword);

    /**
     * 解析建议词结果
     * @param suggestName 自定义建议名称
     * @param searchResponse ES响应结果对象
     * @return
     */
    Collection<String> parseSuggestResult(String suggestName, SearchResponse<SuggestIndex> searchResponse);
}
