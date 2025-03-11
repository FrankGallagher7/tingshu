package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

	@Autowired
	private PaymentInfoService paymentInfoService;

	@Autowired
	private RSAAutoCertificateConfig rsaAutoCertificateConfig;

	@Autowired
	private WxPayV3Config wxPayV3Config;

	/**
	 * 获取微信小程序拉起本地微信支付所需要参数
	 * @param paymentType
	 * @param orderNo
	 * @return
	 */
	@Override
	public Map<String, Object> getWxPrePayParams(String paymentType, String orderNo) {
		Long userId = AuthContextHolder.getUserId();
		//1.将本次交易记录保存到本地交易表
		PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo, userId);

		// 对接微信支付接口
		// 构建service
		JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
		// request.setXxx(val)设置所需参数，具体参数可见Request定义
		PrepayRequest request = new PrepayRequest();
		Amount amount = new Amount();
		// 一次支付单位分---开发接口暂时编码1分 实际应该从paymentInfo对象中获取
		amount.setTotal(1);
		request.setAmount(amount);
		request.setAppid(wxPayV3Config.getAppid());
		request.setMchid(wxPayV3Config.getMerchantId());
		request.setDescription(paymentInfo.getContent());
		request.setOutTradeNo(paymentInfo.getOrderNo());
		// 异步回调，微信处理成功后，调用当前设置的异步回调地址，处理后续业务
		request.setNotifyUrl(wxPayV3Config.getNotifyUrl());

		// 开发阶段必选设置付款人（真正付款只有应用开发者列表中微信账户才有权限）
		// 设置开发者信息
		Payer payer = new Payer();
		payer.setOpenid("oP4gf7Z5arWWhu2JllhLzws54bTo");
		request.setPayer(payer);
		// 调用下单方法，得到应答
		PrepayWithRequestPaymentResponse paymentResponse = service.prepayWithRequestPayment(request);
		// 使用微信扫描 code_url 对应的二维码，即可体验Native支付
		// 获取对应信息
		//调用下单方法（小程序所需参数），得到应答
			// 创建对象数组封装
			Map<String, Object> mapResult = new HashMap<>();
			mapResult.put("timeStamp",paymentResponse.getTimeStamp());  // 时间戳
			mapResult.put("package",paymentResponse.getPackageVal());  // 订单详情扩展字符串
			mapResult.put("paySign",paymentResponse.getPaySign());  // 签名
			mapResult.put("signType",paymentResponse.getSignType());  // 签名方式
			mapResult.put("nonceStr",paymentResponse.getNonceStr());  // 随机字符串
			return mapResult;
	}
}
