package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;

	@Autowired
	private WxMaService wxMaService;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private KafkaService kafkaService;

	@Autowired
	private UserPaidAlbumMapper userPaidAlbumMapper;

	@Autowired
	private UserPaidTrackMapper userPaidTrackMapper;

	@Autowired
	private AlbumFeignClient AlbumFeignClient;

	@Autowired
	private UserVipServiceMapper userVipServiceMapper;

	@Autowired
	private UserFeignClient userFeignClient;

	@Autowired
	private VipServiceConfigMapper vipServiceConfigMapper;

    @Autowired
    private AlbumFeignClient albumFeignClient;
	@Qualifier("com.atguigu.tingshu.album.AlbumFeignClient")

	/**
	 * 小程序授权登录
	 * @param code
	 * @return
	 */
	@Override
	public Map<String, String> wxLogin(String code) {

		// 创建封装响应对象
		Map<String, String> result = null;
		try {
			result = new HashMap<>();

			// 调用微信认证接口GET https://api.weixin.qq.com/sns/jscode2session
			WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);

			// 判断
			if (sessionInfo!=null) {
				// 获取openId
				String openid = sessionInfo.getOpenid();
				// 构建查询对象
				LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
				queryWrapper.eq(UserInfo::getWxOpenId, openid);
				//根据openId查询用户信息
				UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
				//判断是否注册
				if (userInfo==null){//注册
					//1.初始化用户信息
					userInfo = new UserInfo();
					userInfo.setWxOpenId(openid);
					userInfo.setNickname("听友"+ IdUtil.fastUUID());
					userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
					userInfo.setIsVip(0);
					//2.保存用户信息
					userInfoMapper.insert(userInfo);
					//3.初始化用户账户信息
					kafkaService.sendMessage(KafkaConstant.QUEUE_USER_REGISTER,userInfo.getId().toString());
				}

				//1.生成token
				String token = IdUtil.getSnowflakeNextIdStr();
				//2.定义存储key
				String loginKey =  RedisConstant.USER_LOGIN_KEY_PREFIX+token;
				//基于安全信息控制，UserInfoVo
				UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
				//3.存储用户信息到redis
				redisTemplate.opsForValue().set(loginKey,userInfoVo,RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

				//封装存储
				result.put("token",token);

				log.info("[用户服务]微信登陆成功token：{}",token);
			}
		} catch (WxErrorException e) {
			log.error("[用户服务]微信登陆异常：{}",e);
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * 获取登录用户信息
	 * @return
	 */
	@Override
	public UserInfoVo getUserInfo( Long id) {

		UserInfo userInfo = userInfoMapper.selectById(id);
		return BeanUtil.copyProperties(userInfo, UserInfoVo.class);
	}

	/**
	 * 更新用户信息
	 * @param userInfoVo
	 */
	@Override
	public void updateUser(UserInfoVo userInfoVo) {
		UserInfo userInfo = BeanUtil.copyProperties(userInfoVo, UserInfo.class);

		userInfoMapper.updateById(userInfo);

	}

	/**
	 * 判断当前用户某一页中声音列表购买情况
	 * @param userId
	 * @param albumId
	 * @param needChackTrackIdList
	 * @return
	 */
	@Override
	public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needChackTrackIdList) {

		// 创建Map封装结果
		Map<Long, Integer> resultMap = new HashMap<>();

		// 根据专辑ID查询用户购买的专辑
//		select * from user_paid_album where album_id = #{albumId} and user_id = #{userId}
		QueryWrapper<UserPaidAlbum> userPaidAlbumQueryWrapper = new QueryWrapper<>();
		userPaidAlbumQueryWrapper.eq("album_id", albumId);
		userPaidAlbumQueryWrapper.eq("user_id", userId);
		Long count = userPaidAlbumMapper.selectCount(userPaidAlbumQueryWrapper);

		// 购买了专辑，直接将所有声音ID列表设置为1，表示已购买
		if (count.intValue() > 0) {


			for (Long trackId : needChackTrackIdList) {
				resultMap.put(trackId, 1);
			}
			return resultMap;
		}

		// 根据声音列表查询用户是否已购买该声音
		// select * from user_paid_track where track_id in (1, 2, 3, 4) and user_id = #{userId}
		QueryWrapper<UserPaidTrack> userPaidTrackQueryWrapper = new QueryWrapper<>();
		userPaidTrackQueryWrapper.eq("user_id", userId);
		userPaidTrackQueryWrapper.in("track_id", needChackTrackIdList);
		List<UserPaidTrack> userPaidTracks = userPaidTrackMapper.selectList(userPaidTrackQueryWrapper);
		// 没有查询到，当前用户对于专辑和声音没有购买关系，设置为0
		if (CollectionUtils.isEmpty(userPaidTracks)) {

			for (Long trackId : needChackTrackIdList) {
				resultMap.put(trackId, 0);
			}
			return resultMap;
		}

		// 获取用户购买声音列表ID集合
		List<Long> userPaidTrackIdList = userPaidTracks.stream().map(userPaidTrack -> userPaidTrack.getTrackId()).collect(Collectors.toList());

		// 查询到结果，判断那些声音购买了，那些没有购买，购买的设置为1，没有购买的设置为0
		for(Long trackId : needChackTrackIdList) {

			if (userPaidTrackIdList.contains(trackId)) {
				resultMap.put(trackId, 1);
			} else {
				resultMap.put(trackId, 0);
			}
		}
		return resultMap;
	}

	/**
	 * 验证当前用户是否购买过专辑
	 * 提供给订单服务调用，验证当前用户是否购买过专辑
	 * @param albumId
	 * @return
	 */
	@Override
	public Boolean isPaidAlbum(Long albumId) {

		Long userId = AuthContextHolder.getUserId();
		// select * from user_paid_album where album_id = #{albumId} and user_id = #{userId}
		QueryWrapper<UserPaidAlbum> userPaidAlbumQueryWrapper = new QueryWrapper<>();
		userPaidAlbumQueryWrapper.eq("album_id", albumId);
		userPaidAlbumQueryWrapper.eq("user_id", userId);
		Long count = userPaidAlbumMapper.selectCount(userPaidAlbumQueryWrapper);
		if (count.intValue() > 0) {
			return true;
		}
		return false;
	}

	/**
	 * 根据专辑id+用户ID获取用户已购买声音id列表
	 * @param userId
	 * @param albumId
	 * @return
	 */
	@Override
	public List<Long> getUserPaidTrackIdList(Long userId, Long albumId) {

		LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UserPaidTrack::getUserId, userId);
		wrapper.eq(UserPaidTrack::getAlbumId, albumId);
		List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(wrapper);
		if (CollectionUtil.isNotEmpty(userPaidTrackList)) {
			// 获取以购买声音id集合
			List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(userPaidTrack -> userPaidTrack.getTrackId()).collect(Collectors.toList());
			return userPaidTrackIdList;
		}
		return null;
	}

	/**
	 * 处理用户购买记录（虚拟物品发货）
	 * 处理不同购买项：VIP会员，专辑、声音
	 * @param userPaidRecordVo
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
//1.判断购买项目类型-处理专辑
		if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(userPaidRecordVo.getItemType())) { //专辑
			//1.1 根据订单编号查询专辑购买记录--判断是否已经更新过
			LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
			userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo());
			Long count = userPaidAlbumMapper.selectCount(userPaidAlbumLambdaQueryWrapper);
			if (count > 0) {
				return;
			}
			//1.2 查询到专辑购买记录为空则新增购买记录
			UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
			userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
			userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
			userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
			userPaidAlbumMapper.insert(userPaidAlbum);
		} else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(userPaidRecordVo.getItemType())) { // 声音
			//2.判断购买项目类型-处理声音
			//2.1 根据订单编号查询声音购买记录--判断是否已经更新过
			LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
			userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo());
			Long count = userPaidTrackMapper.selectCount(userPaidTrackLambdaQueryWrapper);
			if (count > 0) {
				return;
			}
			//2.2 查询到声音购买记录为空则新增购买记录（循环批量新增）
			//2.2.1 远程调用专辑服务-根据声音ID查询声音对象-获取声音所属专辑ID
			TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
			Long albumId = trackInfo.getAlbumId();
			//2.2.2 遍历购买项目ID集合批量新增声音购买记录
			userPaidRecordVo.getItemIdList().forEach(trackId -> {
				UserPaidTrack userPaidTrack = new UserPaidTrack();
				userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
				userPaidTrack.setUserId(userPaidRecordVo.getUserId());
				userPaidTrack.setAlbumId(albumId);
				userPaidTrack.setTrackId(trackId);
				userPaidTrackMapper.insert(userPaidTrack);
			});
		} else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(userPaidRecordVo.getItemType())) { //  VIP
			//3.判断购买项目类型-处理VIP会员-允许多次购买
			//3.1 新增VIP购买记录
			UserVipService userVipService = new UserVipService();
			//3.1.1 根据VIP套餐ID查询套餐信息-得到VIP会员服务月数
			// 套餐id
			Long vipConfigId = userPaidRecordVo.getItemIdList().get(0);
			// 套餐
			VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipConfigId);
			// 服务月数（vip时长）
			Integer serviceMonth = vipServiceConfig.getServiceMonth();
			//3.1.2 获取用户身份，如果是VIP会员，则续费
			UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
			Integer isVip = userInfo.getIsVip();
			if (isVip.intValue() == 1 && userInfo.getVipExpireTime().after(new Date())) {
				//如果是VIP会员，则续费--本次服务开始时间=VIP结束时间
				userVipService.setStartTime(userInfo.getVipExpireTime());
				//续费会员过期时间=现有会员过期时间+套餐服务月数
				userVipService.setExpireTime(DateUtil.offsetMonth(userInfo.getVipExpireTime(), serviceMonth));
			} else {
				//3.1.3 获取用户身份，如果是普通用户，则新开
				userVipService.setStartTime(new Date());
				//续费会员过期时间=现有会员过期时间+套餐服务月数
				userVipService.setExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
			}
			//3.1.4 构建VIP购买记录对象保存
			userVipService.setUserId(userPaidRecordVo.getUserId());
			userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
			userVipServiceMapper.insert(userVipService);

			//3.2 更新用户表中VIP状态及会员过期时间
			userInfo.setIsVip(1);
			userInfo.setVipExpireTime(userVipService.getExpireTime());
			userInfoMapper.updateById(userInfo);
		}
	}
}
