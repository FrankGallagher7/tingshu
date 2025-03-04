package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	//			System.out.println("openid = " + openid);
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
}
