package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.delay.DelayMsgService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderDerateMapper orderDerateMapper;

    @Autowired
    private AccountFeignClient accountFeignClient;

    @Autowired
    private DelayMsgService delayMsgService;


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
            //4. 处理订单确认页数据-选择声音
            //4.1 远程调用"专辑服务"-获取待购买声音列表  查询指定列：封面图片、声音名称、声音ID、所属专辑ID
            List<TrackInfo> waitBuyTrackInfoList = albumFeignClient.getWaitBuyTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount()).getData();
            if(CollectionUtil.isEmpty(waitBuyTrackInfoList)){
                throw new GuiguException(400, "无符合要求声音");
            }
            //4.2 远程调用"专辑服务"获取专辑信息（得到单集价格）
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(waitBuyTrackInfoList.get(0).getAlbumId()).getData();
            BigDecimal price = albumInfo.getPrice();

            //4.3 计算价格 数量*单价 声音没有折扣
            // 原始金额
            originalAmount = price.multiply(new BigDecimal(waitBuyTrackInfoList.size()));
            // 订单金额
            orderAmount = originalAmount;

            //4.4 遍历待购买声音列表封装订单明细列表
            orderDetailVoList= waitBuyTrackInfoList.stream()
                    .map(trackInfo -> {
                        OrderDetailVo orderDetailVo = new OrderDetailVo();
                        orderDetailVo.setItemId(trackInfo.getId());
                        orderDetailVo.setItemName(trackInfo.getTrackTitle());
                        orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                        orderDetailVo.setItemPrice(price);
                        return orderDetailVo;
                    }).collect(Collectors.toList());
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

    /**
     * 提交订单
     * @param orderInfoVo
     * @param userId
     * @return
     */
    @GlobalTransactional(rollbackFor = Exception.class)
    @Override
    public Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId) {
        //1.业务校验-验证流水号-解决订单重复提交问题
        String tradeNoKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        //1.1 构建验证流水号lua脚本
        String scriptText = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";

        //2.2 执行脚本，如果脚本返回结果为false 抛出异常即可
        // 定义lua脚本对象
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(scriptText, Boolean.class);

        boolean flag = (boolean) redisTemplate.execute(redisScript, Arrays.asList(tradeNoKey), orderInfoVo.getTradeNo());
        if (!flag) {
            throw new GuiguException(400, "订单重复提交");
        }
        //2.验证签名-解决用户篡改订单中数据
        //2.1 将提交订单VO参数转为Map 加签并未加入"payWay" ,手动将提交参数Map中payWay移除掉
        // 用于将一个 Java Bean 对象转换为 Map<String, Object>。它的作用是将对象的属性名作为 Map 的键，属性值作为 Map 的值。
        Map<String, Object> mapParams = BeanUtil.beanToMap(orderInfoVo);
        mapParams.remove("payWay");
        //2.2 调用签名工具类进行验签
        SignHelper.checkSign(mapParams);
        //3.保存订单及订单明细、优惠明细
        OrderInfo orderInfo = this.saveOrderInfo(orderInfoVo, userId);
        //4.处理余额付款 支付方式：1103 余额支付
    if (SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay())) {

        // 4.1 余额支付-远程调用账户服务扣减账户余额
        AccountLockVo accountDeductVo = new AccountLockVo();
        accountDeductVo.setOrderNo(orderInfo.getOrderNo()); //订单号
        accountDeductVo.setUserId(userId); //用户id
        accountDeductVo.setAmount(orderInfo.getOrderAmount()); //优惠后的金额
        accountDeductVo.setContent(orderInfo.getOrderTitle()); //标题-账户明细-付费项目

        //扣减成功虚拟发货-远程调用账户服务更新账户
        Result deductResult = accountFeignClient.checkAndDeduct(accountDeductVo);
        if (200 != deductResult.getCode()) {
            //扣减余额失败：全局事务都需要回滚
            throw new GuiguException(ResultCodeEnum.ACCOUNT_LESS);
        }

        // 4.2  虚拟物品发货-远程调用用户服务新增购买记录  声音 专辑 VIP
        UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
        userPaidRecordVo.setOrderNo(orderInfo.getOrderNo()); //订单号
        userPaidRecordVo.setUserId(userId);  //用户id
        userPaidRecordVo.setItemType(orderInfo.getItemType()); //付款项目类型
        List<Long> itemIdList = orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList()); // 物品id
        userPaidRecordVo.setItemIdList(itemIdList); //物品id(声音 专辑 VIP)

        //虚拟物品发货-远程调用用户服务新增购买记录
        Result paidRecordResult = userFeignClient.savePaidRecord(userPaidRecordVo);
        if (200 != paidRecordResult.getCode()) {
            //新增购买记录失败：全局事务都需要回滚
            throw new GuiguException(211, "新增购买记录异常");
        }
        // 4.3 订单状态：已支付
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID); //订单已支付
        orderInfo.setUpdateTime(new Date()); //支付成功时间
        orderInfoMapper.updateById(orderInfo);
    }
        //7.发送延迟消息-完成订单延迟关单
        delayMsgService.sendDelayMessage(
                KafkaConstant.QUEUE_ORDER_CANCEL,
                orderInfo.getId().toString(),
                15); // 测试30秒
        //5.响应提交成功订单编号
        Map<String, String> mapResult = new HashMap<>();
        mapResult.put("orderNo", orderInfo.getOrderNo());
        return mapResult;
    }

    /**
     * 保存订单及订单商品明细优惠明细
     * @param orderInfoVo
     * @param userId
     * @return
     */
    @Override
    public OrderInfo saveOrderInfo(OrderInfoVo orderInfoVo, Long userId) {
        //1.保存订单
        //1.1 通过拷贝将订单VO中信息拷贝到订单PO对象中
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
        //1.2 设置用户ID
        orderInfo.setUserId(userId);
        //1.3 为订单设置初始付款状态：未支付
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        //1.4 生成全局唯一订单编号 形式：当日日期+雪花算法
        String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        orderInfo.setOrderNo(orderNo);
        //1.5 订单标题
        String itemName = orderInfoVo.getOrderDetailVoList().get(0).getItemName();
        orderInfo.setOrderTitle(itemName);
        //1.6 保存订单
        orderInfoMapper.insert(orderInfo);
        Long orderId = orderInfo.getId();

        //2.保存订单商品明细
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollectionUtil.isNotEmpty(orderDetailVoList)) {
            orderDetailVoList.forEach(orderDetailVo -> {
                OrderDetail orderDetail = BeanUtil.copyProperties(orderDetailVo, OrderDetail.class);
                //关联订单ID
                orderDetail.setOrderId(orderId);
                orderDetailMapper.insert(orderDetail);
            });
        }

        //3.保存订单优惠明细
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollectionUtil.isNotEmpty(orderDerateVoList)) {
            orderDerateVoList.forEach(orderDetailVo -> {
                OrderDerate orderDerate = BeanUtil.copyProperties(orderDetailVo, OrderDerate.class);
                //关联订单ID
                orderDerate.setOrderId(orderId);
                orderDerateMapper.insert(orderDerate);
            });
        }
        //4.返回订单对象
        return orderInfo;
    }

    /**
     * 查询当前用户指定订单信息-根据订单号
     * @param userId
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(Long userId, String orderNo) {
        //1.根据订单编号查询订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if(orderInfo!=null){
            //2.跟订单ID查询订单明细列表
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, orderInfo.getId());
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailLambdaQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);

            //3.跟订单ID查询订单优惠列表
            LambdaQueryWrapper<OrderDerate> orderDerateLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDerateLambdaQueryWrapper.eq(OrderDerate::getOrderId, orderInfo.getId());
            List<OrderDerate> orderDerateList = orderDerateMapper.selectList(orderDerateLambdaQueryWrapper);
            orderInfo.setOrderDerateList(orderDerateList);

            //4.设置订单支付状态及支付方式：中文
            orderInfo.setOrderStatusName(this.getOrderStatusName(orderInfo.getOrderStatus()));
            orderInfo.setPayWayName(this.getPayWayName(orderInfo.getPayWay()));
            return orderInfo;
        }
        return null;
    }

    /**
     * 分页获取当前用户订单列表
     * @param pageInfo
     * @param userId
     * @return
     */
    @Override
    public Page<OrderInfo> getUserOrderByPage(Page<OrderInfo> pageInfo, Long userId) {
        //1.调用持久层获取订单分页列表
        pageInfo = orderInfoMapper.getUserOrderByPage1(pageInfo, userId);
        //2.遍历处理订单状态、订单付费方式中文
        pageInfo.getRecords().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(getOrderStatusName(orderInfo.getOrderStatus())); // 订单状态
            orderInfo.setPayWayName(getPayWayName(orderInfo.getPayWay())); // 支付方式
        });
        return pageInfo;
    }

    /**
     * 延迟取消订单取消订单
     * @param aLong
     */
    @Override
    public void orderCanncal(Long valueOf) {
        //1.根据订单ID查询订单状态
        OrderInfo orderInfo = orderInfoMapper.selectById(valueOf);
        if (orderInfo != null && SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())) {
            //2.如果订单为未支付，说明超时未付款-修改为关闭
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
            orderInfoMapper.updateById(orderInfo);
        }
    }

    // 处理订单状态
    private String getOrderStatusName(String orderStatus) {
        if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus)) {
            return "未支付";
        } else if (SystemConstant.ORDER_STATUS_PAID.equals(orderStatus)) {
            return "已支付";
        } else if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderStatus)) {
            return "取消";
        }
        return null;
    }

    /**
     * 根据支付方式编号得到支付名称
     *
     * @param payWay
     * @return
     */
    private String getPayWayName(String payWay) {
        if (SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay)) {
            return "微信";
        } else if (SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)) {
            return "余额";
        } else if (SystemConstant.ORDER_PAY_WAY_ALIPAY.equals(payWay)) {
            return "支付宝";
        }
        return "";
    }
    }

