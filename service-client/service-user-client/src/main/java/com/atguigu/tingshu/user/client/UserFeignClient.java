package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.impl.UserDegradeFeignClient;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
     * 接口登录/未登录均可调用（微信支付成功后，需要在异步回调（没有Token），调用该方法处理购买记录）
     * 处理用户购买记录（虚拟物品发货）
     *  /api/user/userInfo/savePaidRecord
     * @param userPaidRecordVo
     * @return
     */
    @PostMapping("/api/user/userInfo/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo);


    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * /api/user/userInfo/findUserPaidTrackList/{albumId}
     * 提供给专辑服务调用，获取当前用户已购声音集合
     * @param albumId
     * @return
     */
    @GetMapping("/api/user/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> getUserPaidTrackIdList(@PathVariable Long albumId);

    /**
     * 判断用户是否购买过指定专辑
     * 提供给订单服务调用，验证当前用户是否购买过专辑
     * /api/user/userInfo/isPaidAlbum/{albumId}
     * @param albumId
     * @return
     */
    @GetMapping("/api/user/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId);

    /**
     * 根据id获取VIP服务配置信息
     * 据套餐ID查询套餐信息
     * /api/user/vipServiceConfig/getVipServiceConfig/{id}
     * @param id
     * @return
     */
    @GetMapping("/api/user/vipServiceConfig/getVipServiceConfig/{id}")
    public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id);

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
