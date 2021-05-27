package com.cca.chia.webSocket;

import com.alibaba.fastjson.JSON;
import com.cca.chia.webSocket.handler.WebSocketMessageHandler;
import lombok.SneakyThrows;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * @author cca
 * @version 1.0
 * @date 2021/5/22 17:04
 */
public class CustomWebSocketClient extends WebSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomWebSocketClient.class);

    private static final ThreadLocal<Integer> RETRY_TIME_LOCAL = new ThreadLocal<>();
    /**
     * 连接失败，重试次数
     */
    private static final Integer RETRY_TIME = 3;

    WebSocketClientManager clientManager;

    public CustomWebSocketClient(WebSocketClientManager manager, URI serverUri) {
        super(serverUri);
        this.clientManager = manager;
    }

    @Override
    public void onOpen(ServerHandshake shake) {
        LOGGER.info("WebSocket onOpen，{}", JSON.toJSONString(shake));
        registerService();
    }

    @Override
    public void onClose(int arg0, String arg1, boolean arg2) {
        String host = super.getURI().getHost();
        LOGGER.info("WebSocket onClose ，目标服务器：{}，原因：{}", host, arg1);
        clientManager.waitRetryConnect(host);
    }

    @Override
    public void onError(Exception exception) {
        String host = super.getURI().getHost();
        LOGGER.info("WebSocket onError，目标服务器：{}，异常：{}", host, exception.getMessage());
        clientManager.waitRetryConnect(host);
    }

    @Override
    public void onMessage(String message) {
        String host = super.getURI().getHost();
        LOGGER.info("接收到服务端：{}，数据：{}", host, message);
        String command = JSON.parseObject(message).getString("command");
        WebSocketMessageHandler messageHandler = clientManager.getClientHandlerFactory().getMessageHandler(command);
        if (messageHandler == null) {
            LOGGER.warn("接收到不支持的websocket消息，内容：{}", message);
            return;
        }
        messageHandler.handle(host, message);
    }

    private void registerService() {
        this.send(ParamBuilder.getRegisterParam());
    }

    @SneakyThrows
    @Override
    public void send(String text) {
        String host = super.getURI().getHost();
        long startTime = System.currentTimeMillis();
        if (!isOpen()) {
            try {
                synchronized (this) {
                    while (!isOpen()) {
                        if (checkTimeout(startTime, 5, TimeUnit.SECONDS)) {
                            Integer currentRetryTime = Optional.ofNullable(RETRY_TIME_LOCAL.get()).orElse(0);
                            if (currentRetryTime >= RETRY_TIME) {
                                throw new RuntimeException("无法连接目标服务器，ip：" + host);
                            }
                            // 如果当前socket连接超时，则重置这个连接再次发送，结束当前发送
                            WebSocketClient client = clientManager.resetClientAndGet(host);
                            RETRY_TIME_LOCAL.set(currentRetryTime + 1);
                            LOGGER.info("连接超时，第：{}次，重置连接", RETRY_TIME_LOCAL.get());
                            client.send(text);
                            return;
                        }
                        LOGGER.info("等待打开连接，ip：{}", getURI().getHost());
                        this.wait(1000);
                    }
                }
            } finally {
                RETRY_TIME_LOCAL.remove();
            }
        }
        LOGGER.info("发送websocket请求，参数：{}", text);
        super.send(text);
    }

    private Boolean checkTimeout(Long startTimeStamp, long timeout, TimeUnit unit) {
        return System.currentTimeMillis() > (unit.toMillis(timeout) + startTimeStamp);
    }
}
