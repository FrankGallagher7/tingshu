package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;


	//api/account/userAccount/getAvailableAmount

	/**
	 * 获取账户可用余额
	 * 获取当前登录用户账户可用余额
	 * @return
	 */
	@Operation(summary = "获取当前登录用户账户可用余额")
	@GuiLogin
	@GetMapping("/userAccount/getAvailableAmount")
	public Result<BigDecimal> getAvailableAmount() {
		// 获取用户id
		Long userId = AuthContextHolder.getUserId();

		// 获取当前用户的可用余额
		BigDecimal availableAmount = userAccountService.getAvailableAmount(userId);


		return Result.ok(availableAmount);
	}

}

