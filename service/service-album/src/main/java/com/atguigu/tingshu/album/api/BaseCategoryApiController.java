package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;

	/**
	 * 查询所有分类（1、2、3级分类）
	 * /api/album/category/getBaseCategoryList
	 * @return
	 */
	@GetMapping("/category/getBaseCategoryList")
	public Result<List<JSONObject>> getCategoryList() {

		List<JSONObject> categoryList = baseCategoryService.getCategoryList();

		return Result.ok(categoryList);
	}


	/**
	 * 根据一级分类Id获取分类属性（标签）列表
	 * api/album/category/findAttribute/{category1Id}
	 * @param category1Id
	 * @return
	 */
	@GetMapping("/category/findAttribute/{category1Id}")
	public Result<List<BaseAttribute>> findAttribute(@PathVariable Long category1Id) {

		List<BaseAttribute> attributeList = baseCategoryService.findAttribute(category1Id);

		return Result.ok(attributeList);
	}
}

