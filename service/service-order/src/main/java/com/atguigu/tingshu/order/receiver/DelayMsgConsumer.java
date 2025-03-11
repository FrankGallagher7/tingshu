package com.atguigu.tingshu.order.receiver;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
/**
 * 监听器-延迟订单
 */
public class DelayMsgConsumer {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderInfoService orderInfoService;
    @PostConstruct
    public void orderCancal() {
        log.info("开启线程监听延迟消息：");
        //1.创建阻塞队列（当队列内元素超过上限，继续队列发送消息，进入阻塞状态/当队列中元素为空，继续拉取消息，进入阻塞状态）
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(KafkaConstant.QUEUE_ORDER_CANCEL);
        //2.开启线程监听阻塞队列中消息 只需要单一核心线程线程池对象即可
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(()->{
            while (true) {
                String take = null;
                try {
                    take = blockingQueue.take();
                } catch (InterruptedException e) {
                    log.error("消费者异常，订单号：{}",take);
                    e.printStackTrace();
                }
                if (StringUtils.isNotBlank(take)) {
                    log.info("监听到延迟关单消息：{}", take);
                    //查询订单状态，关闭订单
                    orderInfoService.orderCanncal(Long.valueOf(take));
                }
            }
        });
    }
}
