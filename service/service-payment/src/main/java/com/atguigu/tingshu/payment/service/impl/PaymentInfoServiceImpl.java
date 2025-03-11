package com.atguigu.tingshu.payment.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {


    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private AccountFeignClient accountFeignClient;


    /**
     * 保存本次交易记录
     * @param paymentType   付类型  支付类型：1301-订单 1302-充值
     * @param orderNo       订单编号
     * @param userId
     * @return
     */
    @Override
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId) {

        //1.根据订单编号查询本地交易记录 如果存在则返回即可
        LambdaQueryWrapper<PaymentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfoDB = getOne(queryWrapper);
        if (paymentInfoDB != null) {
            // 如果找到匹配的记录，则返回这条记录
            return paymentInfoDB;
        }
        //2.构建本地交易记录对象
        paymentInfoDB = new PaymentInfo();
        //2.1 基本属性赋值 用户ID、交易类型、订单编号、付款方式（微信）、交易状态：未支付(1401)
        paymentInfoDB.setUserId(userId);
        paymentInfoDB.setOrderNo(orderNo);
        paymentInfoDB.setPaymentType(paymentType);
        paymentInfoDB.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        paymentInfoDB.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
        //2.2 封装交易内容及金额（远程调用订单服务或者账户服务获取）
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) { // 订单
            //远程调用订单服务获取订单金额及购买项目（VIP会员，专辑、声音）
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            Assert.notNull(orderInfo, "订单不存在！");
            if (!SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())) { // 未支付状态
                throw new GuiguException(211, "订单状态错误！");
            }
            paymentInfoDB.setAmount(orderInfo.getOrderAmount());
            paymentInfoDB.setContent(orderInfo.getOrderTitle());
            // 充值
        } else if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) { // 会员充值
            //远程调用账户服务获取充值金额
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfo(orderNo).getData();
            Assert.notNull(rechargeInfo, "充值记录不存在！");
            if(!SystemConstant.ORDER_STATUS_UNPAID.equals(rechargeInfo.getRechargeStatus())){  // 未支付状态
                throw new GuiguException(211, "充值订单状态错误！");
            }
            paymentInfoDB.setAmount(rechargeInfo.getRechargeAmount());
            paymentInfoDB.setContent("充值" + rechargeInfo.getRechargeAmount());
        }
        //3.保存本地交易记录且返回
        this.save(paymentInfoDB);
        return paymentInfoDB;
    }
}
