package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class RechargeInfoApiController {

	@Autowired
	private RechargeInfoService rechargeInfoService;

	@Autowired
	private UserAccountService userAccountService;

	/**
	 * 用户充值，支付成功后，充值业务处理
	 * /api/account/rechargeInfo/rechargePaySuccess/{orderNo}
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "用户充值，支付成功后，充值业务处理")
	@GetMapping("/rechargeInfo/rechargePaySuccess/{orderNo}")
	public Result rechargePaySuccess(@PathVariable String orderNo){
		 userAccountService.rechargePaySuccess(orderNo);
		return Result.ok();
	}

	/**
	 * 新增充值记录-用户充值记录
	 * /api/account/rechargeInfo/submitRecharge
	 * @param rechargeInfoVo
	 * @return {orderNo:"充值交易订单编号"}
	 */
	@Operation(summary = "新增充值记录")
	@GuiLogin
	@PostMapping("/rechargeInfo/submitRecharge")
	public Result<Map<String,String>> submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo){
		Map<String, String> mapResult = rechargeInfoService.submitRecharge(rechargeInfoVo);
		return Result.ok(mapResult);
	}

	/**
	 * 根据订单号获取充值信息
	 * 根据充值订单编号查询充值记录
	 * /api/account/rechargeInfo/getRechargeInfo/{orderNo}
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "根据充值订单编号查询充值记录")
	@GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
	public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo){
		RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfo(orderNo);
		return Result.ok(rechargeInfo);
	}


}

