package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;

	@Autowired
	private UserAccountDetailMapper userAccountDetailMapper;

	@Autowired
	private RechargeInfoService rechargeInfoService;

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
		if (Objects.isNull(userAccount)) {
			return BigDecimal.ZERO;
		} else {
			BigDecimal availableAmount = userAccount.getAvailableAmount();
			if (availableAmount != null) {
				return availableAmount;
			}
		}
		return BigDecimal.ZERO;
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
				accountDeductVo.getContent(), // 内容--title
				SystemConstant.ACCOUNT_TRADE_TYPE_MINUS, //消费类型：消费
				accountDeductVo.getAmount(), //扣减金额
				accountDeductVo.getOrderNo() //订单号
		);
	}

	/**
	 * 用户充值，支付成功后，充值业务处理
	 * @param orderNo
	 */
	@Override
	public void rechargePaySuccess(String orderNo) {
		//1.根据订单编号查询充值记录 判断状态：未支付才处理
		RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfo(orderNo);
		if (rechargeInfo != null && SystemConstant.ORDER_STATUS_PAID.equals(rechargeInfo.getRechargeStatus())) { // 已付款
			return;
		}
		//2.新增余额-账户充值
		int count = userAccountMapper.add(rechargeInfo.getUserId(), rechargeInfo.getRechargeAmount());
		if (count == 0) {
			throw new GuiguException(400, "充值异常");
		}
		//3.新增账户变动日志--账户变更
		this.saveUserAccountDetail(
				rechargeInfo.getUserId(),
				"充值：" + rechargeInfo.getRechargeAmount(), // 内容title
				SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, // 变更类型：充值
				rechargeInfo.getRechargeAmount(), // 变更金额
				orderNo // 订单id
		);
		//4.修改充值记录状态：已支付
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
		rechargeInfoService.updateById(rechargeInfo);
	}

	/**
	 * 分页查询当前用户充值记录
	 * @param pageInfo
	 * @param accountTradeTypeDeposit
	 */
	@Override
	public void getUserAccountDetailPage(Page<UserAccountDetail> pageInfo, String tradeType) {
		//1.获取当前登录用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用持久层获取充值记录
		pageInfo = userAccountMapper.getUserAccountDetailPage(pageInfo, userId, tradeType);
	}

	/**
	 * 分页查询当前用户消费记录
	 * @param pageInfo
	 * @param accountTradeTypeMinus
	 */
	@Override
	public void getUserConsumePage(Page<UserAccountDetail> pageInfo, String tradeType) {
		//1.获取当前登录用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用持久层获取充值记录
		pageInfo = userAccountMapper.getUserConsumePage(pageInfo, userId, tradeType);
	}
}
