package com.cca.chia.webSocket.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * P盘任务提交后的，反馈的信息
 *
 * @author cca
 * @version 1.0
 * @date 2021/5/22 14:18
 */
@Component
public class StartPlotMessageHandler implements WebSocketMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartPlotMessageHandler.class);

    /**
     * 消息模板：
     *  {
     *     "ack":true,
     *     "command":"start_plotting",
     *     "data":{
     *         "service_name":"chia plots create",
     *         "success":true
     *     },
     *     "destination":"ui",
     *     "origin":"daemon",
     *     "request_id":"123456"
     * }
     *
     * 说明：
     *      使用数据库主键，当作requestId，因为chia后台会把提交startPlot时的requestId原样的返回
     *
     *
     * @param message websocket消息内容
     */
    @Override
    public void handle(String remoteServerIp, String message) {
        JSONObject jsonObject = JSON.parseObject(message);
        JSONObject data = jsonObject.getJSONObject("data");

        String plotId = jsonObject.getString("request_id");
        Boolean success = data.getBoolean("success");
        // todo 同步数据库状态

    }

    @Override
    public List<String> support() {
        return Collections.singletonList("start_plotting");
    }
}
