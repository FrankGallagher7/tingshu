package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * 查询所有分类（1、2、3级分类）
     * @return
     */
    List<JSONObject> getCategoryList();

    /**
     * 根据一级分类Id获取分类属性（标签）列表
     * @param category1Id
     * @return
     */
    List<BaseAttribute> findAttribute(Long category1Id);

    /**
     * 根据三级分类Id 获取到分类信息
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryView(Long category3Id);

    /**
     * 根据一级分类Id查询三级分类列表
     * @param category1Id
     * @return
     */
    List<BaseCategory3> getTop7BaseCategory3(Long category1Id);

    /**
     * 根据一级分类id获取全部分类信息
     * @param category1Id
     * @return
     */
    JSONObject getBaseCategoryListByCategory1Id(Long category1Id);
}
