package com.atguigu.tingshu.account;

import com.atguigu.tingshu.account.impl.AccountDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 账号模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-account", fallback = AccountDegradeFeignClient.class)
public interface AccountFeignClient {

    /**
     * 根据订单号获取充值信息
     * 根据充值订单编号查询充值记录
     * /api/account/rechargeInfo/getRechargeInfo/{orderNo}
     * @param orderNo
     * @return
     */
    @GetMapping("/api/account/rechargeInfo/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo);

    /**
     * 检查及锁定账户金额
     * 检查及扣减账户余额
     * /api/account/userAccount/checkAndLock
     * @param accountLockVo
     * @return
     */
    @PostMapping("/api/account/userAccount/checkAndLock")
    public Result checkAndDeduct(@RequestBody AccountLockVo accountLockVo);
}
