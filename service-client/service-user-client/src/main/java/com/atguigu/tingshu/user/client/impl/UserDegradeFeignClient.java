package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserDegradeFeignClient implements UserFeignClient {


    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * @param albumId
     * @return
     */
    @Override
    public Result<List<Long>> getUserPaidTrackIdList(Long albumId) {
        log.error("[用户服务]提供远程调用方法getUserPaidTrackIdList执行服务降级");
        return null;
    }

    /**
     * 判断用户是否购买过指定专辑
     * @param albumId
     * @return
     */
    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        log.error("[用户服务]提供远程调用方法isPaidAlbum执行服务降级");
        return null;
    }

    /**
     * 根据id获取VIP服务配置信息
     * @param id
     * @return
     */
    @Override
    public Result<VipServiceConfig> getVipServiceConfig(Long id) {
        log.error("[用户服务]提供远程调用方法getVipServiceConfig执行服务降级");
        return null;
    }

    /**
     * 获取用户声音列表付费情况
     * 判断当前用户某一页中声音列表购买情况
     * @param userId
     * @param albumId
     * @param needChackTrackIdList
     * @return
     */
    @Override
    public Result<Map<Long, Integer>> userIsPaidTrack(Long userId, Long albumId, List<Long> needChackTrackIdList) {
        log.error("[用户服务]提供远程调用方法userIsPaidTrack执行服务降级");
        return Result.fail();
    }

    /**
     * 根据用户ID查询用户信息
     * @param userId
     * @return
     */
    @Override
    public Result<UserInfoVo> getUserInfoVo(Long userId) {
        log.error("[用户模块]提供远程调用getUserInfo服务降级");
        return Result.fail();
    }
}
