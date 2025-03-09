package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {


    int checkAndDeduct(@Param("amount") BigDecimal amount,@Param("userId") Long userId);
}
