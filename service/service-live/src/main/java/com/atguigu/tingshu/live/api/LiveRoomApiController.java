package com.atguigu.tingshu.live.api;

import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.live.service.LiveRoomService;
import com.atguigu.tingshu.model.live.LiveRoom;
import com.atguigu.tingshu.vo.live.LiveRoomVo;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/live/liveRoom")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveRoomApiController {

	@Autowired
	private LiveRoomService liveRoomService;


	/**
	 * 获取当前直播列表-用户获取所有直播列表
	 * /api/live/liveRoom/findLiveList
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "获取当前直播列表")
	@GetMapping("findLiveList")
	public Result<List<LiveRoom>> findLiveList() {
		return Result.ok(liveRoomService.findLiveList());
	}

	/**
	 * 根据id获取直播间-获取自己直播间
	 * /api/live/liveRoom/getById/{id}
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "根据id获取信息")
	@GetMapping("getById/{id}")
	public Result<LiveRoom> getById(@PathVariable Long id) {
		return Result.ok(liveRoomService.getById(id));
	}


	/**
	 * 创建直播-提交直播表单
	 * /api/live/liveRoom/saveLiveRoom
	 * @param liveRoomVo
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "创建直播")
	@PostMapping("saveLiveRoom")
	public Result<LiveRoom> saveLiveRoom(@RequestBody @Validated LiveRoomVo liveRoomVo) {
		return Result.ok(liveRoomService.saveLiveRoom(liveRoomVo, AuthContextHolder.getUserId()));
	}

	/**
	 * 获取用户当前正在直播的信息
	 * /api/live/liveRoom/getCurrentLive
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "获取当前用户直播")
	@GetMapping("getCurrentLive")
	public Result<LiveRoom> getCurrentLive() {
		return Result.ok(liveRoomService.getCurrentLive(AuthContextHolder.getUserId()));
	}
}

