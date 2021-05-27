package com.cca.chia.webSocket;

import com.cca.chia.webSocket.handler.WebSocketMessageHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * @author cca
 * @version 1.0
 * @date 2021/5/22 14:08
 */
@Component
public class WebSocketClientHandlerFactory {

    private static final HashMap<String, WebSocketMessageHandler> WEB_SOCKET_MESSAGE_HANDLER_MAP = new HashMap<>();

    public WebSocketClientHandlerFactory(List<WebSocketMessageHandler> handlers) {
        for (WebSocketMessageHandler handler : handlers) {
            for (String support : handler.support()) {
                WEB_SOCKET_MESSAGE_HANDLER_MAP.put(support, handler);
            }
        }
    }

    public WebSocketMessageHandler getMessageHandler(String command) {
        return WEB_SOCKET_MESSAGE_HANDLER_MAP.get(command);
    }


}
