package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 专辑上架监听器
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void albumUpper(ConsumerRecord<String, String> record) {

        // 获取消息内容
        String value = record.value();
        // 判断
        if (StringUtils.isNotEmpty(value)) {
            log.info("[搜索服务]收到专辑上架消息：{}", record);
            searchService.upperAlbum(Long.valueOf(value));

        }

    }

    /**
     * 专辑下架监听器
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void albumLower(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.isNotBlank(value)) {

            log.info("[搜索服务]监听到专辑下架消息：{}", value);
            searchService.lowerAlbum(Long.valueOf(value));

        }
    }
}
