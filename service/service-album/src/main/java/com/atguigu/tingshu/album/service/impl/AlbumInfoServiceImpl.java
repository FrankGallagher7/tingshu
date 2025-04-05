package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	private AlbumAttributeValueMapper albumAttributeValueMapper;

	@Autowired
	private AlbumStatMapper albumStatMapper;

	@Autowired
	private KafkaService kafkaService;

	/**
	 * 新增专辑
	 * @param albumInfoVo
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
		// 拷贝数据创建保存对象（保存用户id）
		AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
		albumInfo.setUserId(userId); // 设置用户id
		albumInfo.setTracksForFree(5); // 设置免费试听集数
		albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS); // 设置审核通过
		albumInfoMapper.insert(albumInfo); // 保存album_info
		List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList(); // 保存album_attribute_value

		// 判断有无属性
		if (CollectionUtil.isEmpty(albumAttributeValueVoList)){
			throw new GuiguException(400,"专辑信息不完整，没有属性信息");
		}
		for (AlbumAttributeValue albumAttributeValue : albumAttributeValueVoList) {
			albumAttributeValue.setAlbumId(albumInfo.getId());
			albumAttributeValueMapper.insert(albumAttributeValue);
		}

		// 保存album_stat
		this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_PLAY, 0);
		this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_SUBSCRIBE, 0);
		this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_BUY, 0);
		this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_COMMENT, 0);

		// 上架
		kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, albumInfo.getId().toString());
	}

	/**
	 * 查看当前用户专辑分页列表
	 * @param albumListVoPage
	 * @param albumInfoQuery
	 */
	@Override
	public Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery) {
		return albumInfoMapper.selectUserAlbumPage(albumListVoPage, albumInfoQuery);
	}

	/**
	 * 根据ID删除专辑
	 * @param id
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void removeAlbumInfo(Long id) {

		// 判断专辑下是否有音轨
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
		if (albumInfo.getIncludeTrackCount() > 0) {
			throw new GuiguException(400,"专辑下有音轨，不能删除");
		}

		// 删除album_info
		albumInfoMapper.deleteById(id);

		// 删除album_attribute_value专辑属性
		QueryWrapper<AlbumAttributeValue> attributeValueQueryWrapper = new QueryWrapper<>();
		attributeValueQueryWrapper.eq("album_id",id);
		albumAttributeValueMapper.delete(attributeValueQueryWrapper);

		// 删除album_stat专辑统计
		QueryWrapper<AlbumStat> albumStatQueryWrapper = new QueryWrapper<>();
		albumStatQueryWrapper.eq("album_id",id);
		albumStatMapper.delete(albumStatQueryWrapper);

		// 下架
		kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER, id.toString());
	}

	/**
	 * 根据ID查询专辑信息
	 * @param id
	 * @return
	 */
	@Override
	public AlbumInfo getAlbumInfo(Long id) {

		// 查询专辑信息
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
		// 查询专辑属性信息
		LambdaQueryWrapper<AlbumAttributeValue> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AlbumAttributeValue::getAlbumId,id);
		List<AlbumAttributeValue> attributeValueList = albumAttributeValueMapper.selectList(queryWrapper);

		// 封装数据
		if (albumInfo != null) {
			albumInfo.setAlbumAttributeValueVoList(attributeValueList);
		}

		return albumInfo;
	}

	/**
	 * 修改专辑
	 * @param albumInfoVo
	 * @param id
	 */
	@Override
	public void updateAlbumInfo(AlbumInfoVo albumInfoVo, Long id) {
		AlbumInfo albumInfo = new AlbumInfo();
		BeanUtil.copyProperties(albumInfoVo, albumInfo);
		albumInfo.setId(id);
		albumInfo.setUpdateTime( new Date());
		albumInfoMapper.updateById(albumInfo);

		// 删除属性数据
		QueryWrapper<AlbumAttributeValue> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("album_id",id);
		albumAttributeValueMapper.delete( queryWrapper);

		// 添加属性数据
		// 保存album_attribute_value
		List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();

		// 判断有无属性
		if (CollectionUtil.isEmpty(albumAttributeValueVoList)){

			throw new GuiguException(400,"专辑信息不完整，没有属性信息");
		}
		for (AlbumAttributeValue albumAttributeValue : albumAttributeValueVoList) {
			albumAttributeValue.setAlbumId(albumInfo.getId());
			albumAttributeValueMapper.insert(albumAttributeValue);
		}

		if ("1".equals(albumInfo.getIsOpen())) {
			//上架
			kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, albumInfo.getId().toString());
		}else {
			//下架
			kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER, albumInfo.getId().toString());
		}
	}

	/**
	 * 获取当前用户全部专辑列表
	 * @param userId
	 * @return
	 */
	@Override
	public List<AlbumInfo> findUserAllAlbumList(Long userId) {
		// select id,album_title from album_info where user_id = 1 order by id desc limit 20;
		QueryWrapper<AlbumInfo> queryWrapper = new QueryWrapper<>();
		// 条件
		queryWrapper.eq("user_id",userId);
		// 排序
		queryWrapper.orderByDesc("id");
		// 过滤返回字段
		queryWrapper.select("id","album_title","status");
		// 选择返回条数
		queryWrapper.last(" limit 20 ");

		List<AlbumInfo> albumInfoList = albumInfoMapper.selectList(queryWrapper);
		return albumInfoList;
	}

	/**
	 * 根据专辑ID获取专辑统计信息
	 * @param albumId
	 * @return
	 */
	@Override
	public AlbumStatVo getAlbumStatVo(Long albumId) {

		return albumInfoMapper.getAlbumStatVo(albumId);
	}

	/**
	 * 根据id集合批量查询专辑信息
	 * @return
	 */
	@Override
	public List<AlbumInfo> batchGetAlbumsByIds(Set<Long> albumIds) {
		if (albumIds == null || albumIds.isEmpty()) {
			return Collections.emptyList();
		}
		return albumInfoMapper.selectBatchIds(albumIds);
	}

	/**
	 * 保存专辑统计信息
	 * @param albumId
	 * @param statType
	 * @param statNum
	 */
	private void saveAlbumStat(Long albumId, String statType, int statNum) {
		// 封装统计对象
		AlbumStat albumStat = new AlbumStat();
		albumStat.setAlbumId(albumId);
		albumStat.setStatType(statType);
		albumStat.setStatNum(statNum);

		albumStatMapper.insert(albumStat);
	}
}
