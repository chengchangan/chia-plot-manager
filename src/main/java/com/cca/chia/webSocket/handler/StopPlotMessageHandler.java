package com.cca.chia.webSocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 停止批判操作后的，反馈信息
 *
 * @author cca
 * @version 1.0
 * @date 2021/5/22 14:17
 */
@Component
public class StopPlotMessageHandler implements WebSocketMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopPlotMessageHandler.class);


    @Override
    public void handle(String remoteServerIp, String message) {
        // todo chia 服务端有bug，暂时没有返回停止p盘的反馈
    }

    @Override
    public List<String> support() {
        return Collections.singletonList("stop_plotting");
    }
}
