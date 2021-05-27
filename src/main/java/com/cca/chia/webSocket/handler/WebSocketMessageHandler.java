package com.cca.chia.webSocket.handler;

import java.util.List;

/**
 * @author cca
 * @version 1.0
 * @date 2021/5/22 14:12
 */
public interface WebSocketMessageHandler {

    /**
     * websocket 消息处理器
     *
     * @param message websocket消息内容
     * @param remoteServerIp 消息来自哪个服务器
     */
    void handle(String remoteServerIp, String message);

    /**
     * 支持的命令
     *
     * @return 返回支持的操作
     */
    List<String> support();


}
