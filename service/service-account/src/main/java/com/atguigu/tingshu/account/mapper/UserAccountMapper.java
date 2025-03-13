package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 检查及扣减账户余额
     * @param amount
     * @param userId
     * @return
     */
    int checkAndDeduct(@Param("amount") BigDecimal amount,@Param("userId") Long userId);

    /**
     * 账户充值
     * @param userId
     * @param rechargeAmount
     * @return
     */
    int add(@Param("userId") Long userId, @Param("amount") BigDecimal rechargeAmount);

    /**
     * 分页查询当前用户充值、消费记录
     * @param pageInfo
     * @param userId
     * @param tradeType
     * @return
     */
    Page<UserAccountDetail> getUserAccountDetailPage(Page<UserAccountDetail> pageInfo,@Param("userId") Long userId,@Param("tradeType") String tradeType);

    /**
     * 分页查询当前用户消费记录
     * @param pageInfo
     * @param userId
     * @param tradeType
     * @return
     */
    Page<UserAccountDetail> getUserConsumePage(Page<UserAccountDetail> pageInfo, Long userId, String tradeType);
}
