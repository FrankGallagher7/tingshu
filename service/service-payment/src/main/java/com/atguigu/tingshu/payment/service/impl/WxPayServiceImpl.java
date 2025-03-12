package com.atguigu.tingshu.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
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
		payer.setOpenid("odo3j4ujPBRopdATZnxKZ3HDOLAc");
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

	/**
	 * 根据商户订单编号查询交易状态
	 * @param orderNo
	 * @return
	 */
	@Override
	public Boolean queryPayStatus(String orderNo) {
//		//1.创建查询交易请求对象
//		QueryOrderByOutTradeNoRequest queryOrderByOutTradeNoRequest = new QueryOrderByOutTradeNoRequest();
//		queryOrderByOutTradeNoRequest.setMchid(wxPayV3Config.getMerchantId());
//		queryOrderByOutTradeNoRequest.setOutTradeNo(orderNo);
//
//		//2.创建调用微信服务端业务对象
//		JsapiServiceExtension jsapiService = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
//
//		//3.调用微信查询交易状态接口
//		Transaction transaction = jsapiService.queryOrderByOutTradeNo(queryOrderByOutTradeNoRequest);
//
//		//4.解析响应结果返回交易状态--假设支付状态为已付款
//		if (transaction != null) {
//			Transaction.TradeStateEnum tradeState = transaction.getTradeState();
//			if (Transaction.TradeStateEnum.SUCCESS == tradeState) {
//				//用户支付成功
//				return true;
//			}
//		}
		//1.伪造微信交易对象
		Transaction transaction = new Transaction();
		transaction.setOutTradeNo(orderNo);
		transaction.setTransactionId("WX"+ IdUtil.getSnowflakeNextId()); //使用雪花算法生成唯一交易ID
		/**
		 * 修改本地交易记录
		 */
		paymentInfoService.updatePaymentInfoSuccess(transaction);
		return true;
	}

	/**
	 * 用户付款成功后，处理微信支付异步回调
	 * @param request
	 * @return
	 */
	@Override
	public Map<String, String> paySuccessNotify(HttpServletRequest request) {
		//1.从请求头中获取微信提交参数
		String wechatPaySerial = request.getHeader("Wechatpay-Serial");  //签名
		String nonce = request.getHeader("Wechatpay-Nonce");  //签名中的随机数
		String timestamp = request.getHeader("Wechatpay-Timestamp"); //时间戳
		String signature = request.getHeader("Wechatpay-Signature"); //签名类型

		//HTTP 请求体 body。切记使用原始报文，不要用 JSON 对象序列化后的字符串，避免验签的 body 和原文不一致。
		String requestBody = PayUtil.readData(request);
		//2.构建RequestParam请求参数对象
		RequestParam requestParam = new RequestParam.Builder()
				.serialNumber(wechatPaySerial)
				.nonce(nonce)
				.signature(signature)
				.timestamp(timestamp)
				.body(requestBody)
				.build();
		//3.// 初始化 NotificationParser 解析器对象
		NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
		//4. 调用解析器对象解析方法 验签、解密 并转换成 Transaction
		Transaction transaction = parser.parse(requestParam, Transaction.class);
		if (transaction != null) {
			//4.1 业务验证，验证付款状态以及用户实际付款金额跟商户侧金额是否一致
			if (Transaction.TradeStateEnum.SUCCESS == transaction.getTradeState()) {
				Integer payerTotal = transaction.getAmount().getPayerTotal();
				if (payerTotal.intValue() == 1) {
					//4.2 更新本地交易记录状态
//					paymentInfoService.updatePaymentInfoSuccess(transaction);
					Map<String, String> map = new HashMap<>();
					map.put("code", "SUCCESS");
					map.put("message", "SUCCESS");
					return map;
				}
			}
		}
		return null;
	}
}
