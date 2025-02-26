package com.atguigu.tingshu.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaService {

    /**
     * 发送者
     */
    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);

    @Autowired
    private KafkaTemplate kafkaTemplate;


    /**
     * 发送消息，没有指定key
     * @param topic
     * @param data
     */
    public void sendMessage(String topic, String data){
        this.sendMessage(topic,null,data);
    }

    /**
     * 发送消息，指定key
     * @param topic
     * @param key
     * @param data
     */
    public void sendMessage(String topic, String key, String data){

        CompletableFuture completableFuture = kafkaTemplate.send(topic, key, data);
        //获取发送消息结果
        completableFuture.completeAsync(() -> {
            logger.info("[生产者]发送消息成功,话题：{}，key：{}，消息：{}", topic, key, data);
            return "success";
        }).exceptionallyAsync(e -> {
            logger.error("[生产者]发送消息失败,话题：{}，key：{}，消息：{}，异常原因：{}", topic, key, data, e);
            return "failure";
        });
    }

}
