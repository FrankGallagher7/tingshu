package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 根据充值订单编号查询充值记录
     * @param orderNo
     * @return
     */
    RechargeInfo getRechargeInfo(String orderNo);

    /**
     * 新增充值记录-用户充值记录
     * @param rechargeInfoVo
     * @return
     */
    Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo);
}
