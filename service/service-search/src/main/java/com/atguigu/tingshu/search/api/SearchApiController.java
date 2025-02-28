package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;


    /**
     * 专辑检索
     * /api/search/albumInfo
     * @param albumIndexQuery
     * @return
     */
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery) {

        AlbumSearchResponseVo albumSearchResponseVo = searchService.search(albumIndexQuery);

        return Result.ok(albumSearchResponseVo);
    }


    /**
     * 上架专辑-导入索引库
     * /api/search/albumInfo/upperAlbum/{albumId}
     * @param albumId
     * @return
     */
    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId) {
        searchService.upperAlbum(albumId);
        return Result.ok();
    }


    /**
     * 下架专辑-删除文档
     * /api/search/albumInfo/lowerAlbum/{albumId}
     * @param albumId
     * @return
     */
    @GetMapping("albumInfo/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId) {

        searchService.lowerAlbum(albumId);

        return Result.ok();
    }


}

