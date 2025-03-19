package com.atguigu.tingshu.live.api;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.live.util.WebSocketLocalContainerUtil;
import com.atguigu.tingshu.model.live.FromUser;
import com.atguigu.tingshu.model.live.SocketMsg;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


/**
 * 缺少的点：
 * 1、没有对聊天内容进行审核(需要进行机审  ------> 使用第三方的内容审核服务进行审核【阿里云内容审核服务，华为云内容审核服务】)
 * 2、可用对用户进行禁言
 * 3、没有把用户聊天信息存储到服务端
 * 4、直播带货
 * 5、直播打赏
 * 6、直播点赞
 * 7、直播加关注
 *
 * 直播模块所涉及到的数据库表：live_room , live_tag , mongodb（记录及时通讯消息）
 *
 * Redis的常见功能：
 * 1、做数据缓存
 * 2、做分布式锁
 * 3、使用Redis实现布隆过滤器
 * 4、使用Redis做延迟队列
 * 5、使用Redis做消息队列（发布订阅模式）
 *
 */

/**
 *      *  @ServerEndpoint
 *      * 通过这个 spring boot 就可以知道你暴露出去的 webservice 应用的路径，有点类似我们经常用的@RequestMapping(Http)。
 *      * 比如你的启动端口是8080，而这个注解的值是api，那我们就可以通过 ws://127.0.0.1:8080/api 来连接你的应用
 *      liveRoomId--房间号
 *      token--userId--token
 *
 */
@Component
@ServerEndpoint(value = "/api/websocket/{liveRoomId}/{token}")      // 声明当前这个controller是WebSocket一个服务端点
@Slf4j
@Tag(name = "直播间即时通讯接口管理")
public class WebSocketApiController {

    private static RedisTemplate redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate redisTemplate) {
        WebSocketApiController.redisTemplate = redisTemplate;
    }

    /**
     * 连接建立成功调用的方法
     * @param session
     * @param liveRoomId
     * @param token
     */
    // 建立连接成功后会触发这个注解修饰的方法，注意它有一个 Session 参数
    @OnOpen  // 作用：声明当前这个方法是onOpen事件的事件处理函数
    public void onOpen(Session session , @PathParam(value = "liveRoomId") Long liveRoomId , @PathParam(value = "token") String token ) {

        log.info("连接直播间成功 liveRoomId: {}, token: {}",liveRoomId,token);

        //从redis获取用户基本信息，登录时保存到了redis
        UserInfoVo userInfo = (UserInfoVo)redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX+token);

        Long userId = userInfo.getId();
        //构建即时通讯用户基本信息
        FromUser fromUser = new FromUser();
        fromUser.setUserId(userInfo.getId());
        fromUser.setNickname(userInfo.getNickname());
        fromUser.setAvatarUrl(userInfo.getAvatarUrl());

        //保存用户与直播间关联关系到容器
        WebSocketLocalContainerUtil.addSession(userId, session); //添加用户Id 与 会话的关系
        WebSocketLocalContainerUtil.addFromUser(userId, fromUser); //添加用户Id 与用户的信息
        WebSocketLocalContainerUtil.addUserIdToLiveRoom(liveRoomId, userId); //播间id与用户id列表的对应关系容器

        // 构建发送消息对象
        SocketMsg socketMsg = WebSocketLocalContainerUtil.buildSocketMsg(liveRoomId, fromUser, SocketMsg.MsgTypeEnum.JOIN_CHAT, "欢迎" + userInfo.getNickname() + "进入到直播间");
        // 调用发送消息
         WebSocketLocalContainerUtil.sendMsg(socketMsg);
//        redisTemplate.convertAndSend("msg:list" , JSON.toJSONString(socketMsg)) ;       // 把消息的发送给redis的msg:list通道
//
    }

/**
 * 连接关闭调用的方法
 * @param session
 * @param liveRoomId
 * @param token
 */
// 建立的连接断开后会触发这个注解修饰的方法，注意它有一个 Session 参数
    @OnClose
    public void onClose(Session session , @PathParam(value = "liveRoomId") Long liveRoomId , @PathParam(value = "token") String token) {

        log.info("断开直播连接 liveRoomId, token: {}", liveRoomId, token);

        //获取用户信息
        UserInfoVo userInfo = (UserInfoVo) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        Long userId = userInfo.getId();

        //构建消息：退出直播间
        FromUser fromUser = WebSocketLocalContainerUtil.getFromUser(userId); // 获取用户信息
        SocketMsg socketMsg = WebSocketLocalContainerUtil.buildSocketMsg(liveRoomId, fromUser, SocketMsg.MsgTypeEnum.CLOSE_SOCKET, fromUser.getNickname()+"离开直播间");

        //删除容器中的关联关系
        WebSocketLocalContainerUtil.removeSession(userId); //删除会话
        WebSocketLocalContainerUtil.removeFromUser(userId); //删除用户信息
        WebSocketLocalContainerUtil.removeUserIdToLiveRoom(liveRoomId, userId); //删除该房间的用户

        // 发送消息
        WebSocketLocalContainerUtil.sendMsg(socketMsg);
//        redisTemplate.convertAndSend("msg:list" , JSON.toJSONString(socketMsg)) ;       // 把消息的发送给redis的msg:list通道
        //客户端发送消息到服务端时，会触发这个注解修改的方法，它有一个 String 入参表明客户端传入的值
    }


    /**
     * 收到客户端消息后调用的方法
     *
     * @param msg 客户端发送过来的消息
     */
    @OnMessage          // 声明当前方法是WebSocket的onMessage的事件处理函数
    public void onMessage(Session session , String msg) {
        log.info("来自客户端的消息：{}", msg);
         SocketMsg socketMsg = JSON.parseObject(msg, SocketMsg.class);
//         把发送的消息存储起来  -----> MongoDB
         WebSocketLocalContainerUtil.sendMsg(socketMsg);
//        redisTemplate.convertAndSend("msg:list" , msg) ;       // 把消息的发送给redis的msg:list通道
    }

    //当 websocket 建立连接时出现异常会触发这个注解修饰的方法，注意它有一个 Session 参数
    @OnError            //声明当前方法是WebSocket的onError事件处理函数
    public void onError(Session session, Throwable throwable) {
        log.error("发生错误");
//        throwable.printStackTrace();
    }
}
