package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import com.alibaba.nacos.common.utils.StringUtils;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	private VodService vodService;

	@Autowired
	private TrackStatMapper trackStatMapper;

	@Autowired
	private AlbumFeignClient albumFeignClient;

	@Autowired
	private UserFeignClient userFeignClient;

	/**
	 * 保存声音
	 *track_info :声音表
	 *track_stat :初始化统计信息
	 *album_info :专辑包含总数
	 * @param trackInfoVo
	 * @param userId
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {

		// 保存声音信息

		TrackInfo trackInfo = new TrackInfo();
		BeanUtil.copyProperties(trackInfoVo, trackInfo);
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
		// 设置排序字段
		trackInfo.setOrderNum(albumInfo.getIncludeTrackCount()+1);
		trackInfo.setUserId(userId);

		// 查询云点播声音的时长 大小 类型
		TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackMediainfo(trackInfo.getMediaFileId());
		trackInfo.setMediaDuration(new BigDecimal(trackMediaInfoVo.getDuration()));
		trackInfo.setMediaSize(trackMediaInfoVo.getSize());
		trackInfo.setMediaType(trackMediaInfoVo.getType());
		// 设置声音来源
		trackInfo.setSource(SystemConstant.TRACK_SOURCE_USER);
		// 设置状态
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
		// 保存声音
		trackInfoMapper.insert(trackInfo);


		// 更新专辑声音总数

		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount()+1);
		albumInfoMapper.updateById(albumInfo);

		// 初始化统计信息

		this.saveTrackState(trackInfo.getId(),SystemConstant.TRACK_STAT_PLAY,0);
		this.saveTrackState(trackInfo.getId(),SystemConstant.TRACK_STAT_COLLECT,0);
		this.saveTrackState(trackInfo.getId(),SystemConstant.TRACK_STAT_PRAISE,0);
		this.saveTrackState(trackInfo.getId(),SystemConstant.TRACK_STAT_COMMENT,0);
	}

	/**
	 * 初始化统计信息
	 * @param id
	 * @param trackStatPlay
	 * @param i
	 */
	@Override
	public void saveTrackState(Long trackId, String statType, int statNum) {

		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(statType);
		trackStat.setStatNum(statNum);

		trackStatMapper.insert(trackStat);

	}

	/**
	 * 获取当前用户声音分页列表
	 * @param trackInfoQuery
	 * @return
	 */
	@Override
	public Page<TrackListVo> findUserTrackPage(Page<TrackListVo>  listVoPage, TrackInfoQuery trackInfoQuery) {


		return trackInfoMapper.selectUserTrackPage(listVoPage,trackInfoQuery);
		
	}

	/**
	 * 修改声音信息
	 * @param id
	 * @param trackInfoVo
	 */
	@Override
	public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {

		// 根据id查询数据声音信息
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		// 获取修改之前的声音id
		String beforeMediaFileId = trackInfo.getMediaFileId();
		// 获取修改之后的声音id
		String afterMediaFileId = trackInfoVo.getMediaFileId();
		// 整合修改数据
		BeanUtils.copyProperties(trackInfoVo,trackInfo);

		// 判断声音是否更改-删除云点播旧的声音
		if (!StringUtils.equals(beforeMediaFileId,afterMediaFileId)){
			// 删除云点播旧的声音
			vodService.deleteTrack(beforeMediaFileId);

			// 设置新的云点播声音详情 大小，时长，类型
			if (StringUtils.isNotBlank(afterMediaFileId)) {
				TrackMediaInfoVo trackMediainfo = vodService.getTrackMediainfo(afterMediaFileId);
				trackInfo.setMediaType(trackMediainfo.getType());
				trackInfo.setMediaSize(trackMediainfo.getSize());
				trackInfo.setMediaDuration(new BigDecimal(trackMediainfo.getDuration()));
			}
		}

		// 执行修改
		trackInfoMapper.updateById(trackInfo);
	}

	/**
	 * 删除声音信息
	 * @param id
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeTrackInfo(Long id) {

		// 根据声音id,查询专辑id
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		Long albumId = trackInfo.getAlbumId();
		Integer orderNum = trackInfo.getOrderNum();
		String mediaFileId = trackInfo.getMediaFileId();
		// 删除声音
		trackInfoMapper.deleteById(id);

		// 更新声音排序
		trackInfoMapper.updateOrderNum(albumId,orderNum);

		// 获取专辑对象
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		//更新专辑包含总数
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount()-1);
		albumInfo.setUpdateTime(new Date());
		albumInfoMapper.updateById(albumInfo);

		// 删除声音统计信息
		trackStatMapper.delete(new QueryWrapper<TrackStat>().eq("track_id",id));

		// 删除云点播声音
		vodService.deleteTrack(mediaFileId);
	}

	/**
	 * 用于小程序端专辑页面展示分页声音列表，动态根据用户展示声音付费标识
	 * @param pageInfo
	 * @param albumId
	 * @param userId
	 * @return
	 */
	@Override
	public Page<AlbumTrackListVo> getAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long albumId, Long userId) {

		Page<AlbumTrackListVo> albumTrackPage = trackInfoMapper.selectAlbumTrackPage(pageInfo,albumId,userId);

		// 获取专辑类型 免费 VIP免费 付费
		AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
		Assert.notNull(albumInfo,"查询专辑：{}出现异常",albumId);
		String payType = albumInfo.getPayType();


		// 判断是否登录
		if(userId == null) { //未登录
			// 判断在未登录的情况下，是VIP免费还是付费
			if (payType.equals(SystemConstant.ALBUM_PAY_TYPE_VIPFREE) || payType.equals(SystemConstant.ALBUM_PAY_TYPE_REQUIRE)) {
				// 拿到声音列表
				List<AlbumTrackListVo> albumTrackListVos = albumTrackPage.getRecords();
				// 过滤前五集试听
				List<AlbumTrackListVo> albumTrackListVo = albumTrackListVos.stream().filter(new Predicate<AlbumTrackListVo>() {
					@Override
					public boolean test(AlbumTrackListVo albumTrackListVo) {
						// 返回true过滤，返回false保留
						return albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree();
					}
				}).collect(Collectors.toList());

				// 将剩余的声音设置为付费标识
				for (AlbumTrackListVo trackListVo : albumTrackListVo) {
					trackListVo.setIsShowPaidMark(true);
				}
			}

		}else{ //已登录
			// 设置变量用于后续处理声音是否购买
			Boolean isNeedCheckPayStatus = false;
			// 根据用户id查询用户信息
			UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
			Assert.notNull(userInfoVo,"查询用户信息id：{}出现异常",userId);
			// 获取用户VIP状态
			Integer isVip = userInfoVo.getIsVip();
			// 获取用户的会员到期时间
			Date vipExpireTime = userInfoVo.getVipExpireTime();

			// VIP免费
			if (payType.equals(SystemConstant.ALBUM_PAY_TYPE_VIPFREE)) {

				// 普通用户
				if (isVip.intValue() == 0) {
					isNeedCheckPayStatus = true;
				}
				// 过期VIP用户==普通用户
				if (isVip.intValue() == 1 && new Date().after(vipExpireTime)) {
					isNeedCheckPayStatus = true;
				}
			}

			// 付费
			if (payType.equals(SystemConstant.ALBUM_PAY_TYPE_REQUIRE)) {
				isNeedCheckPayStatus = true;
			}


			if (isNeedCheckPayStatus) {
				//获取待验证的声音列表（前五集除外）
				List<AlbumTrackListVo> needChackTrackList = albumTrackPage.getRecords().stream().filter(albumTrackListVo -> {
					return albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree();
				}).collect(Collectors.toList());
				// 获取过滤后的声音id列表
				List<Long> needChackTrackIdList = needChackTrackList.stream().map(albumTrackListVo -> albumTrackListVo.getTrackId()).collect(Collectors.toList());

				// 进行下一步处理--查询是否购买过专辑或者声音
				Map<Long, Integer> resultMap = userFeignClient.userIsPaidTrack(userId, albumId, needChackTrackIdList).getData();

				for (AlbumTrackListVo albumTrackListVo : needChackTrackList) {

					// 根据指定的声音id，获取结果
					Integer result = resultMap.get(albumTrackListVo.getTrackId());

					// 判断 结果为0，表示未购买，设置为付费标识
					if(result.intValue() == 0) {
						albumTrackListVo.setIsShowPaidMark(true);
					}
				}
			}
		}
		return albumTrackPage;
	}
}
