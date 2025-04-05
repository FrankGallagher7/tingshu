package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessListVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

	@Autowired
	private UserListenProcessService userListenProcessService;

	/**
	 * 删除用户历史界面声音播放进度记录
	 * @param id mongodbID
	 * @return
	 */
	@GuiLogin(required = true)
	@DeleteMapping("/userListenProcess/delete/{id}")
	public Result deleteUserListenProcess(@PathVariable String id) {
		userListenProcessService.deleteUserListenProcess(id);
		return Result.ok();
	}

	/**
	 * 分页查询当前用户历史播放记录
	 * /api/user/userListenProcess/findUserPage
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiLogin(required = true)
	@GetMapping("/userListenProcess/findUserPage/{page}/{limit}")
	public Result<Page<UserListenProcessListVo>> findUserPage(@PathVariable Long page,
															  @PathVariable Long limit) {
		// 构建分页对象
		Long userId = AuthContextHolder.getUserId();
		Page<UserListenProcessListVo> userListenProcessListPage = new Page<>(page, limit);
		userListenProcessListPage = userListenProcessService.findUserPage(userListenProcessListPage, userId);
		return Result.ok(userListenProcessListPage);
	}


	/**
	 * 获取当前用户上次播放专辑声音记录--听专辑按钮
	 * @return
	 */
	@GuiLogin
	@GetMapping("/userListenProcess/getLatelyTrack")
	public Result<Map<String, Long>> getLatelyTrack() {
		Long userId = AuthContextHolder.getUserId();
		return Result.ok(userListenProcessService.getLatelyTrack(userId));
	}

	/**
	 * 更新当前用户收听声音播放进度
	 * @param userListenProcessVo
	 * @return
	 */
	@GuiLogin(required = false)
	@Operation(summary = "更新当前用户收听声音播放进度")
	@PostMapping("/userListenProcess/updateListenProcess")
	public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo){
		Long userId = AuthContextHolder.getUserId();
		if (userId != null) {
			userListenProcessService.updateListenProcess(userId, userListenProcessVo);
		}
		return Result.ok();
	}

	/**
	 * 获取当前用户收听声音播放进
	 * /api/user/userListenProcess/getTrackBreakSecond/{trackId}
	 * @param trackId
	 * @return
	 */
	@GuiLogin(required = false)
	@Operation(summary = "获取当前用户收听声音播放进度")
	@GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
	public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
		Long userId = AuthContextHolder.getUserId();
		if (userId != null) {
			BigDecimal breakSecond = userListenProcessService.getTrackBreakSecond(userId, trackId);
			return Result.ok(breakSecond);
		}
		return Result.ok();
	}
}

