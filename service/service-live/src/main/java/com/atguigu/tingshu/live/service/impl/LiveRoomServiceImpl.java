package com.atguigu.tingshu.live.service.impl;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.live.mapper.LiveRoomMapper;
import com.atguigu.tingshu.live.service.LiveRoomService;
import com.atguigu.tingshu.live.util.LiveAutoAddressUtil;
import com.atguigu.tingshu.model.live.LiveRoom;
import com.atguigu.tingshu.vo.live.LiveRoomVo;
import com.atguigu.tingshu.vo.live.TencentLiveAddressVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveRoomServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveRoomService {

	@Autowired
	private LiveRoomMapper liveRoomMapper;

	/**
	 * 获取用户当前正在直播的信息-无直播-创建直播
	 * @param userId
	 * @return
	 */
	@Override
	public LiveRoom getCurrentLive(Long userId) {
		// select * from live_room where uer+id = ? and expire_time > now();
		LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(LiveRoom::getUserId,userId);
		wrapper.ge(LiveRoom::getExpireTime, new Date());
		LiveRoom liveRoom = liveRoomMapper.selectOne(wrapper);
		return liveRoom;
	}

	/**
	 * 创建直播-提交直播表单
	 * @param liveRoomVo
	 * @param userId
	 * @return
	 */
	@Override
	public LiveRoom saveLiveRoom(LiveRoomVo liveRoomVo, Long userId) {
		// 判断是否有直播间
		LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(LiveRoom::getUserId,userId);
		wrapper.gt(LiveRoom::getExpireTime,new Date());
		Long count = liveRoomMapper.selectCount(wrapper);
		// 有直播间抛出异常
		if(count > 0) {
			throw new GuiguException(ResultCodeEnum.EXIST_NO_EXPIRE_LIVE);
		}
		// 创建直播间
		LiveRoom liveRoom = new LiveRoom();
		BeanUtils.copyProperties(liveRoomVo, liveRoom);
		liveRoom.setUserId(userId);
		liveRoom.setAppName("live");
		liveRoom.setStreamName("test"+userId);
		liveRoomMapper.insert(liveRoom);

		// 使用工具类获得推流地址与拉流地址
		long txTime = liveRoom.getExpireTime().getTime() / 1000;  // 获得直播过期时间(前端传递)
		TencentLiveAddressVo addressUrl = LiveAutoAddressUtil.getAddressUrl(liveRoom.getStreamName(), txTime);
		// 推流地址
		liveRoom.setPushUrl(addressUrl.getPushWebRtcUrl());
		// 拉流地址
		liveRoom.setPlayUrl(addressUrl.getPullWebRtcUrl());
		liveRoomMapper.updateById(liveRoom);
		return liveRoom;
	}


	/**
	 * 获取当前直播列表-用户获取所有直播列表
	 * @return
	 */
	@Override
	public List<LiveRoom> findLiveList() {
		// 获取所有没有过期的直播间(直播间时间 > 当前时间)
		LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
		wrapper.gt(LiveRoom::getExpireTime,new Date());
		List<LiveRoom> liveRooms = liveRoomMapper.selectList(wrapper);
		return liveRooms;
	}
}
