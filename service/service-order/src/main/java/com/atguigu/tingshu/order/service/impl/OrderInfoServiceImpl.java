package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AlbumFeignClient albumFeignClient;


    /**
     * 订单结算页面数据汇总（VIP会员、专辑、声音）
     * @param userId
     * @param tradeVo
     * @return
     */
    @Override
    public OrderInfoVo tradeOrderData(Long userId, TradeVo tradeVo) {


        //1.创建订单确认页数据VO对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //1.1 订单信息VO封装-购买项目类型
        orderInfoVo.setItemType(tradeVo.getItemType());
        //1.1.声明订单中相关价格-初始值设置为"0.00"，在后续业务中赋值即可
        // 订单原始金额
        BigDecimal originalAmount = new BigDecimal("0.00");
        // 减免总金额
        BigDecimal derateAmount = new BigDecimal("0.00");
        // 订单总金额
        BigDecimal orderAmount = new BigDecimal("0.00");

        //1.2 声明初始化订单明细列表及订单优惠明细列表，在后续业务中向集合中新增对象
        // 订单详情集合
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        // 订单减免明细
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();

        //2.处理订单确认页数据-选择VIP会员
        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(tradeVo.getItemType())) {
            //2.1 远程调用“用户服务”获取套餐详情
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
            Assert.notNull(vipServiceConfig, "VIP套餐：{}不存在", tradeVo.getItemId());
            //2.2 封装订单中VIP会员价格 原价=减免价+订单价
            // 原价
            originalAmount = vipServiceConfig.getPrice();
            // 优惠后的价格
            orderAmount = vipServiceConfig.getDiscountPrice();
            // 计算优惠后的价格
            derateAmount = originalAmount.subtract(orderAmount);
            //2.3 封装订单中商品明细列表
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVoList.add(orderDetailVo);

            //2.4 封装订单中优惠明细列表
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("VIP限时优惠：" + derateAmount);
            orderDerateVoList.add(orderDerateVo);

            // 专辑结算
        } else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(tradeVo.getItemType())) {
            //3.处理订单确认页数据-选择专辑
            //3.1 远程调用"用户服务"-判断当前用户是否重复购买专辑
            Boolean isBuy = userFeignClient.isPaidAlbum(tradeVo.getItemId()).getData();
            if (isBuy) {
                throw new GuiguException(400, "当前用户已购买该专辑！");
            }
            //3.2 远程调用"用户服务"-获取当前用户信息（得到身份）
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            Assert.notNull(userInfoVo, "用户{}: 信息为空！", userId);
            Integer isVip = userInfoVo.getIsVip();
            //3.3 远程调用"专辑服务"-获取欲购买专辑信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(tradeVo.getItemId()).getData();
            Assert.notNull(albumInfo, "专辑{}: 信息为空", tradeVo.getItemId());
            //3.4 计算当前用户购买专辑价格
            // 原价
            originalAmount = albumInfo.getPrice();
            // 订单总金额
            orderAmount = originalAmount;
            //3.4.1 判断专辑是否有普通用户折扣
            BigDecimal discount = albumInfo.getDiscount();
            if (discount.intValue() != -1) {
                if (isVip.intValue() == 0) { //普通用户
                    //普通用户折扣(从0.1-9.9)：原价*折扣   100*8/10 = 80 注意：保留小数位+四舍五入
                    orderAmount = originalAmount.multiply(discount)
                            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
                if (isVip.intValue() == 1 && new Date().after(userInfoVo.getVipExpireTime())) { // VIP超时
                    //普通用户
                    orderAmount = originalAmount.multiply(discount)
                            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
            }
            //3.4.2 判断专辑是否有VIP用户折扣
            BigDecimal vipDiscount = albumInfo.getVipDiscount();
            if (vipDiscount.intValue() != -1) {
                if (isVip.intValue() == 1 && new Date().before(userInfoVo.getVipExpireTime())) {
                    //VIP会员用户
                    orderAmount = originalAmount.multiply(vipDiscount)
                            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
            }
            derateAmount = originalAmount.subtract(orderAmount);
            //3.5 封装订单中商品明细列表（专辑）
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);

            //3.6 封装订单中优惠明细列表
            if (derateAmount.doubleValue() > 0) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("专辑优惠："+derateAmount);
                orderDerateVoList.add(orderDerateVo);
            }

            // 声音结算
        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(tradeVo.getItemType())) {
            //4. TODO 处理订单确认页数据-选择声音

        }

        //5.所有订单确认都需要属性
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);

        //5.1 本次结算流水号-防止重复提交
        //5.1.1 构建当前用户本次订单流水号Key
        String tradeNoKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        //5.1.2 生成本次订单流水号
        String tradeNo = IdUtil.fastSimpleUUID();
        //5.1.3 将流水号存入Redis
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo, 5, TimeUnit.MINUTES);
        //5.1.4 封装订单VO中流水号
        orderInfoVo.setTradeNo(tradeNo);

        //5.2 本次结算时间戳--后续判断签名有限时间
        orderInfoVo.setTimestamp(DateUtil.current());
        //5.3 本次结算签名--防止数据篡改
        //5.3.1 将订单VO转为Map-将VO中支付方式null值去掉
        Map<String, Object> paramsMap = BeanUtil.beanToMap(orderInfoVo, false, true);
        //5.3.2 调用签名API对现有订单所有数据进行签名
        String sign = SignHelper.getSign(paramsMap);
        //设置签名
        orderInfoVo.setSign(sign);

        //6.返回订单确认页数据VO对象
        return orderInfoVo;

    }
}
