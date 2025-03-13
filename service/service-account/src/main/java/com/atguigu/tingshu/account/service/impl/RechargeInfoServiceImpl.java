package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

	/**
	 * 根据充值订单编号查询充值记录
	 * @param orderNo
	 * @return
	 */
	@Override
	public RechargeInfo getRechargeInfo(String orderNo) {
		LambdaQueryWrapper<RechargeInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RechargeInfo::getOrderNo, orderNo);

		return rechargeInfoMapper.selectOne(wrapper);
	}

	/**
	 * 新增充值记录-用户充值记录
	 * @param rechargeInfoVo
	 * @return
	 */
	@Override
	public Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo) {
		//1.构建充值记录对象
		Long userId = AuthContextHolder.getUserId();
		RechargeInfo rechargeInfo = new RechargeInfo();
		rechargeInfo.setUserId(userId); // 用户id
		//1.1 生成本次充值记录订单编号：CZ+日期+雪花算法ID
		String orderNo = "CZ" + DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
		rechargeInfo.setOrderNo(orderNo);
		//1.2 充值记录状态：未支付
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
		rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount()); // 充值金额
		rechargeInfo.setPayWay(rechargeInfoVo.getPayWay()); // 充值方式

		//2.保存充值记录
		rechargeInfoMapper.insert(rechargeInfo);
		// 返回数据给前端
		Map<String, String> mapResult = new HashMap<>();
		mapResult.put("orderNo", orderNo);
		return mapResult;
	}
}
