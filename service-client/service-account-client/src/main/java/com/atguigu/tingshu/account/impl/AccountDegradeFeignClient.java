package com.atguigu.tingshu.account.impl;


import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class AccountDegradeFeignClient implements AccountFeignClient {

    /**
     * 用户充值，支付成功后，充值业务处理
     * @param orderNo
     * @return
     */
    @Override
    public Result rechargePaySuccess(String orderNo) {
        log.error("[账户服务]执行服务降级方法：rechargePaySuccess");
        return null;
    }

    /**
     * 根据订单号获取充值信息
     * @param orderNo
     * @return
     */
    @Override
    public Result<RechargeInfo> getRechargeInfo(String orderNo) {
        log.error("[账户服务]提供远程调用接口getRechargeInfo服务降级");
        return null;
    }

    /**
     * 检查及锁定账户金额
     * @param accountLockVo
     * @return
     */
    @Override
    public Result checkAndDeduct(AccountLockVo accountLockVo) {
        log.error("[账户服务]执行服务降级方法：checkAndDeduct");
        return null;
    }
}
