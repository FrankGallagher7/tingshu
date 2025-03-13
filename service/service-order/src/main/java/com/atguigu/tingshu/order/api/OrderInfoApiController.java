package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;

	/**
	 * 用户支付成功后，修改订单状态
	 * /api/order/orderInfo/orderPaySuccess/{orderNo}
	 * @param orderNo 订单编号
	 * @return
	 */
	@Operation(summary = "用户支付成功后，修改订单状态")
	@GetMapping("/orderInfo/orderPaySuccess/{orderNo}")
	public Result orderPaySuccess(@PathVariable String orderNo){
		orderInfoService.orderPaySuccess(orderNo);
		return Result.ok();
	}
	/**
	 * 分页获取当前用户订单列表
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiLogin
	@Operation(summary = "分页获取当前用户订单列表")
	@GetMapping("/orderInfo/findUserPage/{page}/{limit}")
	public Result<Page<OrderInfo>> getUserOrderByPage(@PathVariable int page, @PathVariable int limit){
		Long userId = AuthContextHolder.getUserId();
		Page<OrderInfo> pageInfo = new Page<>(page, limit);
		pageInfo = orderInfoService.getUserOrderByPage(pageInfo, userId);
		return Result.ok(pageInfo);
	}

	/**
	 * 查询当前用户指定订单信息
	 * /api/order/orderInfo/getOrderInfo/{orderNo}
	 * 根据订单号获取订单相关信息
	 * @param orderNo
	 * @return
	 */
	@GuiLogin
	@GetMapping("/orderInfo/getOrderInfo/{orderNo}")
	public Result<OrderInfo> getOrderInfo(@PathVariable("orderNo") String orderNo) {
		Long userId = AuthContextHolder.getUserId();
		OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo);
		return Result.ok(orderInfo);
	}



	/**
	 * 提交订单
	 * /api/order/orderInfo/submitOrder
	 * @param orderInfoVo
	 * @return
	 */
	@PostMapping("/orderInfo/submitOrder")
	@Operation(summary = "提交订单，可能包含余额支付")
	@GuiLogin
	public Result<Map<String,String>> submitOrder(@RequestBody OrderInfoVo orderInfoVo) {

		// 获取用户id
		Long userId = AuthContextHolder.getUserId();

		// 保存订单
		Map<String, String> resultMap = orderInfoService.submitOrder(orderInfoVo, userId);
		return Result.ok(resultMap);
	}

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

