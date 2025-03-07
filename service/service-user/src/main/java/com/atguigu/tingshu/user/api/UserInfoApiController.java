package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;

	/**
	 * 根据专辑id+用户ID获取用户已购买声音id列表
	 * /api/user/userInfo/findUserPaidTrackList/{albumId}
	 * 提供给专辑服务调用，获取当前用户已购声音集合
	 * @param albumId
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "提供给专辑服务调用，获取当前用户已购声音集合")
	@GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
	public Result<List<Long>> getUserPaidTrackIdList(@PathVariable Long albumId) {
		Long userId = AuthContextHolder.getUserId();
		List<Long> userPaidTrackIdList = userInfoService.getUserPaidTrackIdList(userId, albumId);
		return Result.ok(userPaidTrackIdList);
	}

	/**
	 * 判断用户是否购买过指定专辑
	 * 提供给订单服务调用，验证当前用户是否购买过专辑
	 * /api/user/userInfo/isPaidAlbum/{albumId}
	 * @param albumId
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "提供给订单服务调用，验证当前用户是否购买过专辑")
	@GetMapping("/userInfo/isPaidAlbum/{albumId}")
	public Result<Boolean> isPaidAlbum(@PathVariable Long albumId){
		Boolean isBuy = userInfoService.isPaidAlbum(albumId);
		return Result.ok(isBuy);
	}

	/**
	 * 获取用户声音列表付费情况
	 * /api/user/userInfo/userIsPaidTrack/{userId}/{albumId}
	 */
	@Operation(summary = "判断当前用户某一页中声音列表购买情况")
	@RequestMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")

	public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long userId,
													  @PathVariable Long albumId,
													  @RequestBody List<Long>needChackTrackIdList) {

		Map<Long, Integer> mapResult = userInfoService.userIsPaidTrack(userId, albumId, needChackTrackIdList);
		return Result.ok(mapResult);
	}



	/**
	 * 根据用户ID查询用户信息
	 * api/user/userInfo/getUserInfoVo/{userId}
	 * @param userId
	 * @return
	 */
	@RequestMapping("/userInfo/getUserInfoVo/{userId}")
	public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId) {
		UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
		return Result.ok(userInfoVo);
	}


}

