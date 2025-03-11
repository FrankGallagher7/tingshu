package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
