package com.atguigu.tingshu.live.api;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
// @ServerEndpoint(value = "/api/websocket/{liveRoomId}/{token}")      // 声明当前这个controller是WebSocket一个服务端点
/**
 * 上述的链接地址上存在两个路径占位符：
 * liveRoomId： 直播间的id
 * token： 用户登录成功以后，后端给前端返回的登录令牌
 *
 * 每一次客户端和服务器端建立WebSocket的链接的时候，都会创建一个新的WebSocketApiTestController实例对象，这个对象的创建就相当于是new了一个对象，和spring容器没有关系，因此就没有
 * 执行spring bean的生命周期，因此就无法通过@Autowired从spring容器中注入RedisTemplate。
 *
 * 问题的解决方案？ 让RedisTemplate可以被该类多个实例所共享
 *
 */
@Slf4j
public class WebSocketApiTestController {

    // 创建一个Map集合对象，存储用户id和Session之间的对应关系
    private static final Map<Long  , Session>  SESSION_MAP = new ConcurrentHashMap<>() ;
    private static RedisTemplate<String , String> redisTemplate;

    @Autowired
    private void setRedisTemplate(RedisTemplate<String , String> redisTemplate) {
        this.redisTemplate = redisTemplate ;
    }

    @OnOpen  // 作用：声明当前这个方法是onOpen事件的事件处理函数
    public void onOpen(Session session , @PathParam(value = "liveRoomId") Long liveRoomId , @PathParam(value = "token") String token ) {

        // 从Redis中根据token获取用户信息
        String userLoginRedisKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token ;
        String userInfoJSON = redisTemplate.opsForValue().get(userLoginRedisKey);
        UserInfo userInfo = JSON.parseObject(userInfoJSON, UserInfo.class);
        Long userId = userInfo.getId();
        SESSION_MAP.put(userId , session) ;         // 向Map中存储存储用户id和Session之间的对应关系

        // 发送欢迎消息
        SESSION_MAP.values().forEach(s -> {
            s.getAsyncRemote().sendText("欢迎" + userInfo.getNickname() + "进入到直播间！") ;
        });

    }

    @OnClose
    public void onClose(Session session , @PathParam(value = "liveRoomId") Long liveRoomId , @PathParam(value = "token") String token) {

        // 从Redis中根据token获取用户信息
        String userLoginRedisKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token ;
        String userInfoJSON = redisTemplate.opsForValue().get(userLoginRedisKey);
        UserInfo userInfo = JSON.parseObject(userInfoJSON, UserInfo.class);
        Long userId = userInfo.getId();
        SESSION_MAP.remove(userId) ;

        // 发退出消息
        SESSION_MAP.values().forEach(s -> {
            s.getAsyncRemote().sendText(userInfo.getNickname() + "退出了直播间！") ;
        });

    }

    @OnMessage          // 声明当前方法是WebSocket的onMessage的事件处理函数
    public void onMessage(Session session , String msg) {

        // 接收到某一个客户端发送过来的消息以后，就把这个消息发给直播间所有的用户
        SESSION_MAP.values().forEach(s -> {
            s.getAsyncRemote().sendText(msg);
        });

    }

    @OnError            //声明当前方法是WebSocket的onError事件处理函数
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }

}












