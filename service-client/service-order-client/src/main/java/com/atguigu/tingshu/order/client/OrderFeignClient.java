package com.atguigu.tingshu.order.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 订单模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-order", fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {

    /**
     * 用户支付成功后，修改订单状态
     * /api/order/orderInfo/orderPaySuccess/{orderNo}
     * @param orderNo 订单编号
     * @return
     */
    @GetMapping("/api/order/orderInfo/orderPaySuccess/{orderNo}")
    public Result orderPaySuccess(@PathVariable String orderNo);

    /**
     * 查询当前用户指定订单信息
     * /api/order/orderInfo/getOrderInfo/{orderNo}
     * 根据订单号获取订单相关信息
     * @param orderNo
     * @return
     */
    @GetMapping("/api/order/orderInfo/getOrderInfo/{orderNo}")
    public Result<OrderInfo> getOrderInfo(@PathVariable("orderNo") String orderNo);


}
