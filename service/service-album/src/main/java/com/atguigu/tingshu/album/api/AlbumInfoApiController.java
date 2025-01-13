package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {


	@Autowired
	private AlbumInfoService albumInfoService;

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

