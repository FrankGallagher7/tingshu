package com.atguigu.tingshu.user.api;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.login.GuiLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 小程序授权登录
     * api/user/wxLogin/wxLogin/{code}
     * @param code
     * @return
     */
    @GetMapping("/wxLogin/{code}")
    public Result<Map<String, String>> wxLogin(@PathVariable String code) {
        return Result.ok(userInfoService.wxLogin(code));
    }



    /**
     * 获取登录用户信息
     * api/user/wxLogin/getUserInfo
     * @return
     */
    @GetMapping("/getUserInfo")
    @GuiLogin(required = true)
    public Result<UserInfoVo> getUserInfo() {
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }


    /**
     * 更新用户信息
     * api/user/wxLogin/updateUser
     * @param userInfoVo
     * @return
     */
    @PostMapping("/updateUser")
    @GuiLogin(required = true)
    public Result updateUser(@RequestBody UserInfoVo userInfoVo) {

        Long userId = AuthContextHolder.getUserId();
        userInfoVo.setId(userId);
        userInfoService.updateUser(userInfoVo);

        return Result.ok();
    }

    /**
     * 创建虚拟用户用来测试--添加token
     * api/user/wxLogin/getToken/{userId}
     * @param userId
     * @return
     */
    @Operation(summary = "手动获取token")
    @GetMapping("/getToken/{userId}")
    public Result login(@PathVariable Long userId) {
        UserInfo userInfo = userInfoService.getById(userId);
        UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+token, userInfoVo, RedisConstant.USER_LOGIN_REFRESH_KEY_TIMEOUT, TimeUnit.SECONDS);
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        return Result.ok(map);
    }

}
