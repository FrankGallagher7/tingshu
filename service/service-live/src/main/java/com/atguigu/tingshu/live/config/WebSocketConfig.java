package com.atguigu.tingshu.live.config;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {

    /**
     * 配置WebSocket服务器的端点导出器，作用：把webSocket服务的端点进行导出，供webSocket的客户端进行连接
     * 端点：连接地址
     * WebSocket配置类：
     *  开启websocket配置，通过这个配置 spring boot 才能去扫描到 websocket 的注解
     * WebSocket常用注解：
     *  @ServerEndpoint
     * 通过这个 spring boot 就可以知道你暴露出去的 webservice 应用的路径，有点类似我们经常用的@RequestMapping(Http)。
     * 比如你的启动端口是8080，而这个注解的值是api，那我们就可以通过 ws://127.0.0.1:8080/api 来连接你的应用
     * @OnOpen
     * 当 websocket 建立连接成功后会触发这个注解修饰的方法，注意它有一个 Session 参数
     * @OnClose
     * 当 websocket 建立的连接断开后会触发这个注解修饰的方法，注意它有一个 Session 参数
     * @OnMessage
     * 当客户端发送消息到服务端时，会触发这个注解修改的方法，它有一个 String 入参表明客户端传入的值
     * @OnError
     * 当 websocket 建立连接时出现异常会触发这个注解修饰的方法，注意它有一个 Session 参数
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter() ;
    }

}
