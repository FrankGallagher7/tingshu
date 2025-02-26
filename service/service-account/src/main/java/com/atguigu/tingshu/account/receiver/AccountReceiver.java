package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 监听器
     * 接收消息
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void userRegister(ConsumerRecord<String, String> record) {

        // 获取消息的值
        String value = record.value();
        //判断
        if (StringUtils.isNotEmpty(value)){
            Long userId = Long.valueOf(value);
            //初始化用户账户
            userAccountService.saveUserAccount(userId);
            log.info("[账户服务]监听用户注册成功消息：{}", value);
        }

    }
}
