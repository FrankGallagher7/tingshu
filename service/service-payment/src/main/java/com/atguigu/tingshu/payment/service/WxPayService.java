package com.atguigu.tingshu.payment.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {

    /**
     * 获取微信小程序拉起本地微信支付所需要参数
     * @param paymentType
     * @param orderNo
     * @return
     */
    Map<String, Object> getWxPrePayParams(String paymentType, String orderNo);

    /***
     * 根据商户订单编号查询交易状态
     * @param orderNo
     * @return
     */
    Boolean queryPayStatus(String orderNo);

    /**
     * 用户付款成功后，处理微信支付异步回调
     * @param request
     * @return
     */
    Map<String, String> paySuccessNotify(HttpServletRequest request);
}
