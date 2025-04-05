package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessListVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.sound.midi.Track;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AlbumFeignClient albumFeignClient;




	/**
	 * 获取当前用户收听声音播放进度
	 * @param userId  用户ID
	 * @param trackId 声音ID
	 * @return
	 */
	@Override
	public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
		//1.构建查询条件
		Query query = new Query();
		//添加条件
		query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId));
		// 防止暂停后前端发送多个相同的进度
		// 按更新时间倒序排序，留一个
		query.with(Sort.by(Sort.Direction.DESC,"updateTime"));
		query.limit(1);
		//2.执行查询播放进度--动态分表100%
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		if (userListenProcess != null) {
			return userListenProcess.getBreakSecond();
		}
		// 从头开始
		return new BigDecimal("0.00");
	}

	/**
	 * 更新当前用户收听声音播放进度
	 * @param userId
	 * @param userListenProcessVo
	 */
	@Override
	public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
		//1.构建查询条件
		Query query = new Query();
		//添加条件
		query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
		// 防止暂停后前端发送多个相同的进度
		// 按更新时间倒序排序，留一个
		query.with(Sort.by(Sort.Direction.DESC,"updateTime"));
		query.limit(1);
		//执行查询播放进度--动态分表用户id100取余
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));

		if (userListenProcess == null) {
			//2.如果播放进度不存在-新增播放进度
			userListenProcess = new UserListenProcess();
			userListenProcess.setUserId(userId);
			userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
			userListenProcess.setTrackId(userListenProcessVo.getTrackId());
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setIsShow(1);
			userListenProcess.setCreateTime(new Date());
			userListenProcess.setUpdateTime(new Date());
		} else {
			//3.如果播放进度存在-更新进度
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setUpdateTime(new Date());
		}
		mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
	}

	/**
	 * 获取当前用户上次播放专辑声音记录--听专辑按钮
	 * @param userId
	 * @return
	 */
	@Override
	public Map<String, Long> getLatelyTrack(Long userId) {
		//根据用户ID查询播放进度集合，按照更新时间倒序，获取第一条记录
		// where userId=? order by desc update_time limit 1
		//1.构建查询条件对象
		Query query = new Query();
		//1.1 封装用户ID查询条件
		query.addCriteria(Criteria.where("userId").is(userId));
		//1.2 按照更新时间排序
		query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
		//1.3 只获取第一条记录
		query.limit(1);
		//2.执行查询
		UserListenProcess listenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		if (listenProcess != null) {
			//封装响应结果
			Map<String, Long> mapResult = new HashMap<>();
			mapResult.put("albumId", listenProcess.getAlbumId());
			mapResult.put("trackId", listenProcess.getTrackId());
			return mapResult;
		}
		return null;
	}

	/**
	 * 分页查询当前用户历史播放记录
	 * @param userListenProcessPage
	 * @param userId
	 * @return
	 */
	@Override
	public Page<UserListenProcessListVo> findUserPage(Page<UserListenProcessListVo> userListenProcessListPage, Long userId) {
		// 创建查询条件，按用户ID过滤，并按更新时间降序排序
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId));
		query.with(Sort.by(Sort.Direction.DESC, "updateTime"));

		// 获取集合名称
		String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);

		// 计算总记录数
		long total = mongoTemplate.count(query, UserListenProcess.class, collectionName);
		userListenProcessListPage.setTotal(total);

		// 如果没有数据，直接返回空列表
		if (total == 0) {
			userListenProcessListPage.setRecords(Collections.emptyList());
			return userListenProcessListPage;
		}

		// 计算总页数
		int size = (int) userListenProcessListPage.getSize();
		int pages = (int) (total / size);
		if (total % size != 0) {
			pages++;
		}
		userListenProcessListPage.setPages(pages);

		// 调整当前页，确保不超过总页数
		int current = (int) userListenProcessListPage.getCurrent();
		if (current > pages) {
			current = pages;
			userListenProcessListPage.setCurrent(current);
		}

		// 设置分页参数（skip和limit）
		query.skip((current - 1) * (long) size).limit(size);

		// 执行分页查询
		List<UserListenProcessListVo> records = mongoTemplate.find(query, UserListenProcessListVo.class, collectionName);

		// 1. 提取所有 albumId 和 trackId（去重）
		Set<Long> albumIds = records.stream()
				.map(UserListenProcessListVo::getAlbumId)
				.collect(Collectors.toSet());

		Set<Long> trackIds = records.stream()
				.map(UserListenProcessListVo::getTrackId)
				.collect(Collectors.toSet());

//		Result<List<TrackInfo>> listResult = albumFeignClient.batchGetTracksByIds(trackIds);
//		List<TrackInfo> data = albumFeignClient.batchGetTracksByIds(trackIds).getData();

		// 2. 批量查询专辑信息--远程调用
		Map<Long, AlbumInfo> albumMap = albumFeignClient.batchGetAlbumsByIds(albumIds).getData()
				.stream()
				.collect(Collectors.toMap(AlbumInfo::getId, album -> album));
		// 3. 批量查询音频信息--远程调用
		Map<Long, TrackInfo> trackMap = albumFeignClient.batchGetTracksByIds(trackIds).getData()
				.stream()
				.collect(Collectors.toMap(TrackInfo::getId, track -> track));

		// 填充mongodb里没有的数据字段
		records.forEach(vo -> {
			// 填充专辑信息
			AlbumInfo albumInfo = albumMap.get(vo.getAlbumId());
			if (albumInfo != null) {
				vo.setAlbumTitle(albumInfo.getAlbumTitle());
				vo.setCoverUrl(albumInfo.getCoverUrl());
			}

			// 填充声音字段
			TrackInfo trackInfo = trackMap.get(vo.getTrackId());
			if (trackInfo != null) {
				vo.setTrackTitle(trackInfo.getTrackTitle());
				vo.setMediaDuration(this.getTrackBreakSecond(userId,trackInfo.getId()));
			}
		});
		userListenProcessListPage.setRecords(records);

		return userListenProcessListPage;
	}

	/**
	 * 删除用户历史界面声音播放进度记录
	 * @param id
	 */
	@Override
	public void deleteUserListenProcess(String id) {
		// 用户id
		Long userId = AuthContextHolder.getUserId();
		// 删除记录
		mongoTemplate.remove(Query.query(Criteria.where("_id").is(id)),
				MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
	}
}
