package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;
    /**
     * 微信下单
     * 获取微信小程序拉起本地微信支付所需要参数
     * /api/payment/wxPay/createJsapi/{paymentType}/{orderNo}
     * @param paymentType 支付类型：支付类型：1301-订单 1302-充值
     * @param orderNo     订单编号
     * @return 小程序拉调用wx.requestPayment(Object object)需要参数
     */
    @GuiLogin
    @Operation(summary = "获取微信小程序拉起本地微信支付所需要参数")
    @PostMapping("/wxPay/createJsapi/{paymentType}/{orderNo}")
    public Result<Map<String, Object>> getWxPrePayParams(@PathVariable String paymentType, @PathVariable String orderNo) {
        Map<String, Object> mapResult = wxPayService.getWxPrePayParams(paymentType, orderNo);
        return Result.ok(mapResult);
    }

}
