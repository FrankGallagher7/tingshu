package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;

public interface SearchService {


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
}
