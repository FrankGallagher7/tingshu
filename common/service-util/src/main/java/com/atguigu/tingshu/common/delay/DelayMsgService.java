package com.atguigu.tingshu.common.delay;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DelayMsgService {

    @Autowired
    private RedissonClient redissonClient;
    /**
     * 基于Redisson（Redis）实现延迟消息
     *
     * @param data      数据
     * @param queueName 延迟队列名称
     * @param ttl       延迟时间：单位s
     */
    public void sendDelayMessage(String queueName, String data, int ttl) {
        try {
            //7.1 创建阻塞队列-创建阻塞对象
            RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(queueName);
            //7.2 基于阻塞队列创建延迟队列
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
            //7.3 发送延迟消息 测试阶段：设置为30s
            delayedQueue.offer(data, ttl, TimeUnit.SECONDS);
            log.info("发送延迟消息成功：{}", data);
        } catch (Exception e) {
            log.error("[延迟消息]发送异常：{}", data);
            throw new RuntimeException(e);
        }
    }

}
