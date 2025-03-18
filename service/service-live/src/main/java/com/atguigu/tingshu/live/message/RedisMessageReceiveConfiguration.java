package com.atguigu.tingshu.live.message;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisMessageReceiveConfiguration {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(MessageListenerAdapter messageListenerAdapter , RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer() ;
        PatternTopic patternTopic = new PatternTopic("msg:list");
        redisMessageListenerContainer.addMessageListener(messageListenerAdapter , patternTopic);
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        return  redisMessageListenerContainer ;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisMessageReceive redisMessageReceive) {
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisMessageReceive , "receiveMessage");
        return messageListenerAdapter ;
    }

}
