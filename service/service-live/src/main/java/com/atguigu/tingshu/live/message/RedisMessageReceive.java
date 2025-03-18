package com.atguigu.tingshu.live.message;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.live.util.WebSocketLocalContainerUtil;
import com.atguigu.tingshu.model.live.SocketMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 订阅者
 */
@Component
@Slf4j
public class RedisMessageReceive {

    public void receiveMessage(String message) {

        // 获取到消息以后，需要将消息发送给当前这个微服务实例中的所有session
        if(!StringUtils.isEmpty(message)) {

            SocketMsg socketMsg = JSON.parseObject(message, SocketMsg.class);
            String msgType = socketMsg.getMsgType();
            if(!"0".equals(msgType)) {      // 该消息不属于心跳检查消息
                WebSocketLocalContainerUtil.sendMsg(socketMsg);
                log.info("消息发送给了当前微服务实例的所有的客户端session...");
            }

        }

    }

}
