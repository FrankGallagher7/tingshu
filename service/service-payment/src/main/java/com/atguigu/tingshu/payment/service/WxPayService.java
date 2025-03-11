package com.atguigu.tingshu.payment.service;

import java.util.Map;

public interface WxPayService {

    /**
     * 获取微信小程序拉起本地微信支付所需要参数
     * @param paymentType
     * @param orderNo
     * @return
     */
    Map<String, Object> getWxPrePayParams(String paymentType, String orderNo);
}
