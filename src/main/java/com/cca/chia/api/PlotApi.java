package com.cca.chia.api;

import com.cca.chia.types.plot.StartPlotParam;
import com.cca.chia.types.plot.StopPlotParam;
import com.cca.chia.webSocket.ParamBuilder;
import com.cca.chia.webSocket.WebSocketClientManager;
import com.cca.chia.webSocket.queue.PlotQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.UUID;

/**
 * @author cca
 * @version 1.0
 * @date 2021/5/22 15:05
 */
@Component
public class PlotApi {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebSocketClientManager webSocketClientFactory;
    @Autowired
    private PlotQueue plotQueue;

    @PostConstruct
    public void init() {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    /**
     * 提交P盘任务
     */
    public void startPlot(StartPlotParam startPlotParam) {
        plotQueue.addPlotTask(startPlotParam);
    }

    /**
     * 停止p盘
     */
    public void stopPlot(StopPlotParam stopPlotParam) {
        String remoteServerIp = stopPlotParam.getRemoteServerIp();
        if (StringUtils.isBlank(remoteServerIp)) {
            throw new IllegalArgumentException("远程服务IP不能为空");
        }
        String plotId = stopPlotParam.getPlotId();
        if (StringUtils.isBlank(plotId)) {
            throw new IllegalArgumentException("停止P盘的id不能为空");
        }
        ObjectNode param = objectMapper.createObjectNode();
        param.put("command", "stop_plotting");
        param.put("ack", "false");
        param.put("request_id", UUID.randomUUID().toString());
        param.put("destination", "daemon");
        param.put("origin", "ui");
        ObjectNode data = objectMapper.createObjectNode();
        data.put("id", plotId);
        param.set("data", data);
        WebSocketClient socketClient = webSocketClientFactory.getClient(remoteServerIp);
        socketClient.send(param.toString());
    }

    /**
     * 监听P盘进度
     */
    public void listenPlotProgress(String remoteServerIp) {
        if (StringUtils.isBlank(remoteServerIp)) {
            throw new IllegalArgumentException("远程服务IP不能为空");
        }
        WebSocketClient socketClient = webSocketClientFactory.getClient(remoteServerIp);
        socketClient.send(ParamBuilder.getRegisterParam());
    }


}
