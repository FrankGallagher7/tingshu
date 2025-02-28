package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;

	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;

	@Autowired
	private BaseAttributeMapper baseAttributeMapper;


	/**
	 * 查询所有分类（1、2、3级分类）
	 * @return
	 */
	@Override
	public List<JSONObject> getCategoryList() {
		// 创建集合 一级分类，收集数据
		List<JSONObject> arrList = new ArrayList<>();

		// 查询所有分类信息
		List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
		//判断
		if (CollectionUtil.isNotEmpty(baseCategoryViewList)){

			// 分组 一级分类
			Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
			for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
				// 创建一级分类封装对象
				JSONObject obj1 = new JSONObject();

				// 获取一级分类ID
				Long category1Id = entry1.getKey();
				obj1.put("categoryId", category1Id);
				// 获取一级分类name
				List<BaseCategoryView> category2List = entry1.getValue();
				String category1Name = category2List.get(0).getCategory1Name();
				obj1.put("categoryName", category1Name);

				// 创建集合 二级分类
				List<JSONObject> array2 = new ArrayList<>();

				// 分组 二级分类
				Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
				for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
					// 创建二级分类封装对象
					JSONObject obj2 = new JSONObject();

					// 获取二级分类ID
					Long category2Id = entry2.getKey();
					obj2.put("categoryId", category2Id);

					// 获取二级分类name
					List<BaseCategoryView> category3List = entry2.getValue();
					String category2Name = category3List.get(0).getCategory2Name();
					obj2.put("categoryName", category2Name);

					// 封装三级分类集合
					List<JSONObject> array3 = category3List.stream().map(baseCategoryView -> {
						JSONObject obj3 = new JSONObject();
						obj3.put("categoryId", baseCategoryView.getCategory3Id());
						obj3.put("categoryName", baseCategoryView.getCategory3Name());

						return obj3;
					}).collect(Collectors.toList());
					// 获取三级分类
					obj2.put("categoryChild",array3);

					// 收集二级分类对象
					array2.add(obj2);
				}

				// 获取二级分类
				obj1.put("categoryChild",array2);

				// 收集一级分类对象
				arrList.add(obj1);
			}

		}
		return arrList;
	}

	/**
	 * 根据一级分类Id获取分类属性（标签）列表
	 * @param category1Id
	 * @return
	 */
	@Override
	public List<BaseAttribute> findAttribute(Long category1Id) {

		return baseAttributeMapper.SelectAttribute(category1Id);
	}

	/**
	 * 根据三级分类Id 获取到分类信息
	 * @param category3Id
	 * @return
	 */
	@Override
	public BaseCategoryView getCategoryView(Long category3Id) {

		BaseCategoryView baseCategoryView = baseCategoryViewMapper.selectById(category3Id);
		return baseCategoryView;
	}

	/**
	 * 根据一级分类Id查询三级分类列表
	 * @param category1Id
	 * @return
	 */
	@Override
	public List<BaseCategory3> getTop7BaseCategory3(Long category1Id) {

		//根据一级分类ID查询所属二级分类集合
		List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id", category1Id));

		//根据二级分类集合过滤出二级分类ID集合
		List<Long> category2IdList = baseCategory2List.stream().map(baseCategory2 -> baseCategory2.getId()).collect(Collectors.toList());

		//构建查询条件对象
		QueryWrapper<BaseCategory3> queryWrapper = new QueryWrapper<>();
		//添加二级分类集合ID
		queryWrapper.in("category2_id",category2IdList);
		//筛选可以置顶的分类
		queryWrapper.eq("is_top",1);
		//添加排序
		queryWrapper.orderByDesc("order_num");
		//只获取前七个分类
		queryWrapper.last("limit 7");

		//查询三级分类数据
		List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(queryWrapper);
		return baseCategory3List;
	}

	/**
	 * 根据一级分类id获取全部分类信息
	 * @param category1Id
	 * @return
	 */
	@Override
	public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {
		//1.根据1级分类ID查询分类视图得到一级分类列表
		LambdaQueryWrapper<BaseCategoryView> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(BaseCategoryView::getCategory1Id, category1Id);
		List<BaseCategoryView> baseCategory1List = baseCategoryViewMapper.selectList(queryWrapper);
		//2.处理一级分类对象 封装一级分类对象包含ID，分类名称
		if (CollectionUtil.isNotEmpty(baseCategory1List)) {
			//2.1 构建一级分类对象 封装ID，名称
			JSONObject jsonObject1 = new JSONObject();
			jsonObject1.put("categoryId", baseCategory1List.get(0).getCategory1Id());
			jsonObject1.put("categoryName", baseCategory1List.get(0).getCategory1Name());
			//3.处理一级分类下二级分类
			//3.1 将一级分类集合再按照二级分类ID分组得到Map Map中key:二级分类ID，Map中Value二级分类集合
			Map<Long, List<BaseCategoryView>> category2Map = baseCategory1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			//3.2 遍历Map每遍历一次Map封装二级分类JSON对象
			List<JSONObject> jsonObject2List = new ArrayList<>();
			for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
				//3.3 构建二级分类对象，封装二级分类ID及名称
				JSONObject jsonObject2 = new JSONObject();
				jsonObject2.put("categoryId", entry2.getKey());
				jsonObject2.put("categoryName", entry2.getValue().get(0).getCategory2Name());
				jsonObject2List.add(jsonObject2);
				//4.处理二级分类下三级分类
				//4.1 遍历二级分类列表，没遍历一条记录构建三级分类对象
				List<JSONObject> jsonObject3List = new ArrayList<>();
				for (BaseCategoryView baseCategoryView : entry2.getValue()) {
					//4.2 构建三级分类对象，封装三级分类ID名称
					JSONObject jsonObject3 = new JSONObject();
					jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
					jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
					jsonObject3List.add(jsonObject3);
				}
				//4.3 将三级分类集合放入二级分类对象中
				jsonObject2.put("categoryChild", jsonObject3List);
			}
			//3.3 将二级分类集合放入一级分类对象中
			jsonObject1.put("categoryChild", jsonObject2List);
			return jsonObject1;
		}
		return null;
	}
}
