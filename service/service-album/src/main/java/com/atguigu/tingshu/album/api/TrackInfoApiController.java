package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;

	@Autowired
	private VodService vodService;

	/**
	 * 根据id集合批量查询声音信息
	 * @return
	 */
	@GetMapping("/albumList")
	public Result<List<TrackInfo>> batchGetTracksByIds(@RequestParam("trackIds") Set<Long> trackIds) {
		List<TrackInfo> trackInfoList = trackInfoService.batchGetTracksByIds(trackIds);
		return Result.ok(trackInfoList);
	}


	/**
	 * 根据声音ID+声音数量 获取下单付费声音列表
	 * 提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表
	 * /api/album/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}
	 * @param trackId    声音ID
	 * @param trackCount 数量
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表")
	@GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
	public Result<List<TrackInfo>> getWaitBuyTrackInfoList(@PathVariable Long trackId, @PathVariable int trackCount) {
		Long userId = AuthContextHolder.getUserId();
		List<TrackInfo> trackInfoList = trackInfoService.getWaitBuyTrackInfoList(userId, trackId, trackCount);
		return Result.ok(trackInfoList);
	}

	/**
	 * 获取当前用户分集购买声音列表
	 *
	 * /api/album/trackInfo/findUserTrackPaidList/{trackId}
	 * Map<String, Object> map = new HashMap<>();
	 * map.put("name","本集"); // 显示文本
	 * map.put("price",albumInfo.getPrice()); // 专辑声音对应的价格
	 * map.put("trackCount",1); // 记录购买集数
	 * list.add(map);
	 * @param trackId 声音ID
	 * @return [{name:"本集", price:0.2, trackCount:1},{name:"后10集", price:2, trackCount:10},...,{name:"全集", price:*, trackCount:*}]
	 */
	@GuiLogin
	@Operation(summary = "获取当前用户分集购买声音列表")
	@GetMapping("/trackInfo/findUserTrackPaidList/{trackId}")
	public Result<List<Map<String, Object>>> getUserWaitBuyTrackPayList(@PathVariable Long trackId) {
		Long userId = AuthContextHolder.getUserId();
		List<Map<String, Object>> list = trackInfoService.getUserWaitBuyTrackPayList(trackId);
		return Result.ok(list);
	}


	/**
	 * 查询专辑声音分页列表-动态根据用户展示声音付费标识
	 *
	 * 用于小程序端专辑页面展示分页声音列表，动态根据用户展示声音付费标识
	 * /api/album/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}
	 * @param albumId
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiLogin(required = false)
	@Operation(summary = "用于小程序端专辑页面展示分页声音列表，动态根据用户展示声音付费标识")
	@GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
	public Result<Page<AlbumTrackListVo>> getAlbumTrackPage(@PathVariable Long albumId, @PathVariable Integer page, @PathVariable Integer limit) {
		//1.获取用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.封装分页对象
		Page<AlbumTrackListVo> pageInfo = new Page<>(page, limit);
		//3.调用业务层封装分页对象
		pageInfo = trackInfoService.getAlbumTrackPage(pageInfo, albumId, userId);
		return Result.ok(pageInfo);
	}


	/**
	 * 删除声音信息
	 * api/album/trackInfo/removeTrackInfo/{id}
	 * @param id
	 * @return
	 */
	@DeleteMapping("/trackInfo/removeTrackInfo/{id}")
	public Result removeTrackInfo(@PathVariable Long id) {

		trackInfoService.removeTrackInfo(id);

		return Result.ok();
	}

	/**
	 * 修改声音信息
	 * api/album/trackInfo/updateTrackInfo/{id}
	 * @param id
	 * @param trackInfo
	 * @return
	 */
	@PutMapping("/trackInfo/updateTrackInfo/{id}")
	public Result updateTrackInfo(@PathVariable Long id,
								  @RequestBody TrackInfoVo trackInfoVo) {

		trackInfoService.updateTrackInfo(id, trackInfoVo);

		return Result.ok();
	}


	/**
	 * 查询声音信息
	 * api/album/trackInfo/getTrackInfo/{id}
	 * @param id
	 * @return
	 */
	@GetMapping("/trackInfo/getTrackInfo/{id}")
	public Result<TrackInfo> getTrackInfo(@PathVariable Long id) {
		return Result.ok(trackInfoService.getById(id));
	}


	/**
	 * 获取当前用户声音分页列表
	 *
	 * /api/album/trackInfo/findUserTrackPage/{page}/{limit}
	 *
	 * @param page
	 * @param limit
	 * @param trackInfoQuery
	 * @return
	 */
	@PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
	@GuiLogin
	public Result<Page<TrackListVo>> findUserTrackPage(@PathVariable Long page,
													   @PathVariable Long limit,
													   @RequestBody TrackInfoQuery trackInfoQuery) {
		// 封装分页对象
		Page<TrackListVo>  listVoPage = new Page<>(page, limit);
		// 封装用户ID
		Long userId = AuthContextHolder.getUserId();
		trackInfoQuery.setUserId(userId);
		// 调用service方法
		listVoPage = trackInfoService.findUserTrackPage(listVoPage, trackInfoQuery);

		return Result.ok(listVoPage);
	}



	/**
	 * 保存声音
	 * api/album/trackInfo/saveTrackInfo
	 * @param trackInfoVo
	 * @return
	 */
	@PostMapping("/trackInfo/saveTrackInfo")
	@GuiLogin
	public Result saveTrackInfo (@RequestBody TrackInfoVo trackInfoVo) {

		// 获取用户id
		Long userId = AuthContextHolder.getUserId();

		trackInfoService.saveTrackInfo(trackInfoVo, userId);

		return Result.ok();
	}
	/**
	 * 上传声音
	 * api/album/trackInfo/uploadTrack
	 * @param file
	 * @return
	 */
	@PostMapping("/trackInfo/uploadTrack")
	public Result<Map<String, String>> uploadTrack(MultipartFile file) {
		Map<String, String> resultMap = vodService.uploadTrack(file);
		return Result.ok(resultMap);
	}

}

