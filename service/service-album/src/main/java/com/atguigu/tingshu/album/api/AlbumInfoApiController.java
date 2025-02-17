package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {


	@Autowired
	private AlbumInfoService albumInfoService;


	/**
	 * 查看当前用户专辑分页列表
	 * api/album/albumInfo/findUserAlbumPage/{page}/{limit}
	 * @param page
	 * @param limit
	 * @param albumInfoQuery
	 * @return
	 */
	@PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
	public Result<Page<AlbumListVo>> findUserAlbumList(@PathVariable Long page,
													   @PathVariable Long limit,
													   @RequestBody AlbumInfoQuery albumInfoQuery) {
		// 封装分页对象
		Page<AlbumListVo> albumListVoPage = new Page<>(page, limit);
		// 获取用户ID
		Long userId = AuthContextHolder.getUserId();
		albumInfoQuery.setUserId(userId);

		albumListVoPage = albumInfoService.findUserAlbumPage(albumListVoPage,albumInfoQuery);

		return Result.ok(albumListVoPage);
	}

	/**
	 * 新增专辑
	 * api/album/albumInfo/saveAlbumInfo
	 * @param albumInfoVo
	 * @return
	 */
	@PostMapping("/albumInfo/saveAlbumInfo")
	public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {

		// 获取用户ID
		Long userId = AuthContextHolder.getUserId();

		albumInfoService.saveAlbumInfo(albumInfoVo,userId);

		return Result.ok();
	}

}

