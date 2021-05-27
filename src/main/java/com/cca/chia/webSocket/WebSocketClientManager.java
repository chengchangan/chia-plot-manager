package com.cca.chia.webSocket;

import com.cca.chia.util.KeyStoreLoader;
import org.java_websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author cca
 * @version 1.0
 * @date 2021/5/22 10:26
 */
@Component
public class WebSocketClientManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClientManager.class);

    @Autowired
    private WebSocketClientHandlerFactory clientHandlerFactory;

    private static final ConcurrentHashMap<String, WebSocketClient> CLIENT_MAP = new ConcurrentHashMap<>();
    /**
     * 目标服务器宕机，延迟定时重连
     */
    private static final Set<String> WAIT_RETRY_CONNECTION_SERVER_IP = Collections.synchronizedSet(new HashSet<>());

    private static final ScheduledExecutorService RETRY_CONNECT_EXECUTOR = Executors.newScheduledThreadPool(1,
            x -> new Thread(x, "baas.chia.remote.webSocket.WebSocketClientManager"));


    @PostConstruct
    public void init() {
        RETRY_CONNECT_EXECUTOR.scheduleWithFixedDelay(new RetryConnector(), 10, 30, TimeUnit.MINUTES);
    }

    /**
     * 获取指定服务器websocket客户端
     *
     * @param ip 指定服务器IP
     * @return
     */
    public WebSocketClient getClient(String ip) {
        return CLIENT_MAP.computeIfAbsent(ip, this::createWebSocketClient);
    }

    /**
     * 失败重连
     *
     * @param ip 需要连接的服务器Ip
     */
    protected WebSocketClient resetClientAndGet(String ip) {
        WebSocketClient webSocketClient = CLIENT_MAP.get(ip);
        if (webSocketClient.isOpen()) {
            return webSocketClient;
        }
        synchronized (ip.intern()) {
            webSocketClient = CLIENT_MAP.get(ip);
            if (webSocketClient.isOpen()) {
                return webSocketClient;
            }
            webSocketClient = this.createWebSocketClient(ip);
            CLIENT_MAP.put(ip, webSocketClient);
        }
        return webSocketClient;
    }

    /**
     * 获取一个websocketMessageHandlerFactory 工厂
     */
    protected WebSocketClientHandlerFactory getClientHandlerFactory() {
        return clientHandlerFactory;
    }

    protected void waitRetryConnect(String remoteServerIp) {
        WAIT_RETRY_CONNECTION_SERVER_IP.add(remoteServerIp);
    }


    private WebSocketClient createWebSocketClient(String ip) {
        try {
            WebSocketClient webSocketClient = new CustomWebSocketClient(this, new URI("wss://" + ip + ":55400"));
            // 使用ssl
            SSLSocketFactory factory = useSsl(ip);
            webSocketClient.setSocket(factory.createSocket());
            webSocketClient.connect();
            return webSocketClient;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("webSocketClient creat failed，reason：" + e.getMessage());
        }
    }

    private static SSLSocketFactory useSsl(String ip) throws Exception {
        // todo ： 获取证书的路径待确定
        String keypassWord = "123456";
//        String keyPath = ip + "_private_daemon.p12";
        String keyPath = "private_daemon.p12";

        KeyStore keyStore;
        try (InputStream in = WebSocketClientManager.class.getClassLoader().getResourceAsStream(keyPath)) {
            if (in == null) {
                throw new IllegalArgumentException("无法找到证书");
            }
            keyStore = KeyStoreLoader.load(in, "PKCS12", keypassWord);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keypassWord.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }


    /**
     * 目标服务挂掉，延迟定时重连
     */
    private class RetryConnector implements Runnable {

        @Override
        public void run() {
            Iterator<String> iterator = WAIT_RETRY_CONNECTION_SERVER_IP.iterator();
            while (iterator.hasNext()) {
                String remoteServerIp = iterator.next();
                iterator.remove();
                LOGGER.info("服务器：{}，连接断开，开始重连", remoteServerIp);
                resetClientAndGet(remoteServerIp);
            }
        }
    }

}
