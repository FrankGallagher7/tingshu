package com.atguigu.tingshu.order.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class OrderDegradeFeignClient implements OrderFeignClient {

    /**
     * 用户支付成功后，修改订单状态
     * @param orderNo 订单编号
     * @return
     */
    @Override
    public Result orderPaySuccess(String orderNo) {
        log.error("[订单服务]远程调用orderPaySuccess服务降级");
        return null;
    }

    /**
     * 查询当前用户指定订单信息
     * @param orderNo
     * @return
     */
    @Override
    public Result<OrderInfo> getOrderInfo(String orderNo) {

        log.error("[订单模块]提供远程调用getOrderInfo服务降级");
        return null;
    }
}
