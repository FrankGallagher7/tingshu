package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.atguigu.tingshu.vo.account.AccountLockVo;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;





	/**
	 * 检查及锁定账户金额
	 * 检查及扣减账户余额
	 * /api/account/userAccount/checkAndLock
	 * @param accountLockVo
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "检查及扣减账户余额")
	@PostMapping("/userAccount/checkAndLock")
	public Result checkAndDeduct(@RequestBody AccountLockVo accountLockVo) {
		//1.从ThreadLocal中获取用户ID
		Long userId = AuthContextHolder.getUserId();
		if (userId != null) {
			accountLockVo.setUserId(userId);
		}
		//2.调用业务逻辑完成账户余额扣款
		userAccountService.checkAndDeduct(accountLockVo);
		return Result.ok();
	}



	/**
	 * 获取账户可用余额
	 * 获取当前登录用户账户可用余额
	 * /api/account/userAccount/getAvailableAmount
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

