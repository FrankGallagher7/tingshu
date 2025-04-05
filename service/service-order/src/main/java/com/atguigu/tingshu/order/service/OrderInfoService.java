package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 查询当前用户指定订单信息-根据订单号
     * @param
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfo(String orderNo);

    /**
     * 分页获取当前用户订单列表
     * @param pageInfo
     * @param userId
     * @return
     */
    Page<OrderInfo> getUserOrderByPage(Page<OrderInfo> pageInfo, Long userId);

    /**
     * 取消订单
     * @param aLong
     */
    void orderCanncal(Long aLong);

    /**
     * 用户支付成功后，修改订单状态(远程调用用户服务-新增用户购买记录)
     * @param orderNo
     */
    void orderPaySuccess(String orderNo);
}
