package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    /**
     * 查询指定一级分类下热门排行专辑
     * /api/search/albumInfo/channel/{category1Id}
     * @param category1Id
     * @return
     */
    @Operation(summary = "查询1级分类下置顶3级分类下包含分类热门专辑")
    @GetMapping("/albumInfo/channel/{category1Id}")
    public Result<List<Map<String, Object>>> getTopCategory3HotAlbumList(@PathVariable Long category1Id) {

        List<Map<String, Object>> list = searchService.getTopCategory3HotAlbumList(category1Id);

        return Result.ok(list);
    }


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

