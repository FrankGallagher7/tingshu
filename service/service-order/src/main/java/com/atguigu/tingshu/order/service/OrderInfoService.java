package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单结算页面数据汇总（VIP会员、专辑、声音）
     * @param userId
     * @param tradeVo
     * @return
     */
    OrderInfoVo tradeOrderData(Long userId, TradeVo tradeVo);

    /**
     * 提交订单
     * @param orderInfoVo
     * @param userId
     * @return
     */
    Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId);

    /**
     * 保存订单相关信息
     * @param orderInfoVo
     * @param userId
     * @return
     */
    OrderInfo saveOrderInfo(OrderInfoVo orderInfoVo, Long userId);
}
