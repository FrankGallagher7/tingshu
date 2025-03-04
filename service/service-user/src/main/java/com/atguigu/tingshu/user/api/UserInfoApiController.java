package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

