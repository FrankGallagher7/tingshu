package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;

	@Autowired
	private UserAccountDetailMapper userAccountDetailMapper;

	/**
	 * 初始化用户账户
	 * @param userId
	 */
	@Override
	public void saveUserAccount(Long userId) {

		//创建账号对象
		UserAccount userAccount = new UserAccount();
		userAccount.setUserId(userId);
		//初始化账户，赠送部分金额（首次注册赠送100点）
		userAccount.setTotalAmount(new BigDecimal("100"));
		userAccount.setAvailableAmount(new BigDecimal("100"));
		userAccount.setTotalIncomeAmount(new BigDecimal("100"));
		userAccountMapper.insert(userAccount);

		//更新账户记录
		this.saveUserAccountDetail(userId, "赠送", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, userAccount.getAvailableAmount(), null);

	}


	/**
	 * 初始化账户记录
	 * 账户明细
	 * @param userId
	 * @param title
	 * @param tradeType
	 * @param amount
	 * @param order_no
	 */
	@Override
	public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String order_no) {
		//创建用户账户明细对象
		UserAccountDetail userAccountDetail = new UserAccountDetail();
		userAccountDetail.setUserId(userId);
		userAccountDetail.setTitle(title);
		userAccountDetail.setTradeType(tradeType);
		userAccountDetail.setAmount(amount);
		userAccountDetail.setOrderNo(order_no);
		userAccountDetailMapper.insert(userAccountDetail);
	}

	/**
	 * 获取当前登录用户账户可用余额
	 * @param userId
	 * @return
	 */
	@Override
	public BigDecimal getAvailableAmount(Long userId) {

		QueryWrapper<UserAccount> wrapper = new QueryWrapper<>();
		wrapper.eq("user_id", userId);

		UserAccount userAccount = userAccountMapper.selectOne(wrapper);
		BigDecimal availableAmount = userAccount.getAvailableAmount();

		if (availableAmount != null) {
			return availableAmount;
		}
		return null;
	}

	/**
	 * 检查及扣减账户余额
	 * @param accountDeductVo
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void checkAndDeduct(AccountLockVo accountDeductVo) {

		// 扣减账户余额
		int count = userAccountMapper.checkAndDeduct(accountDeductVo.getAmount(), accountDeductVo.getUserId());

		// 判断是否扣减成功
		if (count == 0) {
			throw new GuiguException(400, "账户扣减异常");
		}
//		int i=1/0;

		// 新增账户记录
		this.saveUserAccountDetail(
				accountDeductVo.getUserId(),
				accountDeductVo.getContent(), // 锁定内容
				SystemConstant.ACCOUNT_TRADE_TYPE_MINUS, //消费
				accountDeductVo.getAmount(), //扣减金额
				accountDeductVo.getOrderNo() //订单号
		);
	}
}
