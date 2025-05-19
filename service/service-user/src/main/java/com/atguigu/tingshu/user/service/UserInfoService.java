package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserLogin;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 小程序授权登录
     * @param code
     * @return
     */
    Map<String, String> wxLogin(String code);

    /**
     * 获取登录用户信息
     * @return
     */
    UserInfoVo getUserInfo( Long id);

    /**
     * 更新用户信息
     * @param userInfoVo
     */
    void updateUser(UserInfoVo userInfoVo);

    /**
     * 判断当前用户某一页中声音列表购买情况
     * @param userId
     * @param albumId
     * @param needChackTrackIdList
     * @return
     */
    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needChackTrackIdList);

    /**
     * 提供给订单服务调用，验证当前用户是否购买过专辑
     * @param albumId
     * @return
     */
    Boolean isPaidAlbum(Long albumId);

    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * @param userId
     * @param albumId
     * @return
     */
    List<Long> getUserPaidTrackIdList(Long userId, Long albumId);

    /**
     * 处理用户购买记录（虚拟物品发货）
     * @param userPaidRecordVo
     */
    void savePaidRecord(UserPaidRecordVo userPaidRecordVo);

    /**
     * 用户账户密码登录
     * @return
     */
    Map<String, String> login(UserInfo userInfo);
}
