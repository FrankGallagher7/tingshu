package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;


	/**
	 * 订单确认
	 * 订单结算页面数据汇总（VIP会员、专辑、声音）
	 * /api/order/orderInfo/trade
	 * @return
	 */
	@Operation(summary = "订单结算页面数据汇总（VIP会员、专辑、声音）")
	@PostMapping("/orderInfo/trade")
	@GuiLogin
	public Result<OrderInfoVo>  tradeOrderData(@RequestBody TradeVo tradeVo) {
		Long userId = AuthContextHolder.getUserId();
		OrderInfoVo orderInfoVo = orderInfoService.tradeOrderData(userId, tradeVo);
		return Result.ok(orderInfoVo);
	}

}

