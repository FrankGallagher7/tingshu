package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.impl.UserDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-user", fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {

    /**
     * 获取用户声音列表付费情况
     * 判断当前用户某一页中声音列表购买情况
     * /api/user/userInfo/userIsPaidTrack/{userId}/{albumId}
     */
    @RequestMapping("/api/user/userInfo/userIsPaidTrack/{userId}/{albumId}")

    public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long userId,
                                                      @PathVariable Long albumId,
                                                      @RequestBody List<Long> needChackTrackIdList);

    /**
     * 根据用户ID查询用户信息
     * api/user/userInfo/getUserInfoVo/{userId}
     * @param userId
     * @return
     */
    @RequestMapping("/api/user/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId);

}
