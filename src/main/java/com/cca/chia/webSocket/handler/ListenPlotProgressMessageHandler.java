package com.cca.chia.webSocket.handler;

import com.cca.chia.webSocket.queue.PlotQueue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * 监听p盘任务进度的处理器
 *
 * @author cca
 * @version 1.0
 * @date 2021/5/22 14:21
 */
@Component
public class ListenPlotProgressMessageHandler implements WebSocketMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenPlotProgressMessageHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 监听的事件
     */
    private static final String REGISTER_SERVICE = "register_service";
    private static final String STATE_CHANGED = "state_changed";
    private static final String LOG_CHANGED = "log_changed";

    /**
     * p盘状态
     */
    private static final String SUBMITTED = "SUBMITTED";
    private static final String REMOVING = "REMOVING";
    private static final String FINISHED = "FINISHED";

    /**
     * p盘总进度
     */
    private static final Integer FINISHED_LOG_LINES = 2626;


    @PostConstruct
    public void init() {
        MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }


    @Autowired
    private PlotQueue plotQueue;
    @Autowired(required = false)
    private RedisTemplate<Object, Object> redisTemplate;


    /**
     * 收到来自chia服务端的websocket信息
     * 使用信息中的队列名和 数据库的plotId进行关联
     * 因为在提交p盘的时候使用plotId当作队列名
     */
    @Override
    public void handle(String remoteServerIp, String message) {
        String command;
        List<LogChangeMessage> queue;
        try {
            JsonNode jsonNode = MAPPER.readTree(message);
            command = jsonNode.get("command").asText();
            String queues = jsonNode.get("data").get("queue").toString();

            JavaType javaType = MAPPER.getTypeFactory().constructCollectionType(List.class, LogChangeMessage.class);
            queue = MAPPER.readValue(queues, javaType);
        } catch (JsonProcessingException e) {
            LOGGER.error("收到chia服务端监听推送的信息，转换异常：", e);
            return;
        }

        List<HandleResult> handleResults = handleLog(command, queue);

        for (HandleResult result : handleResults) {
            if (result.isFinished) {
                String queueName = result.queueName;
                // todo 修改dbPlot 为完成
                plotQueue.finishedPlotAndRunNext(remoteServerIp);
            }
            if (result.isDelete) {
                String queueName = result.queueName;
                removeRate(queueName);
                // todo 修改dbPlot 为删除
                plotQueue.finishedPlotAndRunNext(remoteServerIp);
            }
            if (redisTemplate.opsForHash().get(getPlotCacheKey(result.queueName), "lines") == null) {
                // todo 第一次收到监听信息
                // 修改dbPlot 为正在P盘
            }
            if (result.isErrorLog) {
                //异常日志，不统计行数，后续考虑日志记录，添加监控
                continue;
            }
            // 计算比例
            calcRate(result.queueName, result.totalLine);
        }
    }

    /**
     * 计算p盘进度
     */
    private void calcRate(String queueName, Integer totalLine) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        String cacheKey = getPlotCacheKey(queueName);

        Long lines;
        if (totalLine != null) {
            lines = Long.valueOf(totalLine);
        } else {
            lines = hash.increment(cacheKey, "lines", 1);
        }
        BigDecimal rate = lines > FINISHED_LOG_LINES ? BigDecimal.ONE : new BigDecimal(lines).divide(new BigDecimal(FINISHED_LOG_LINES), 4, RoundingMode.UP);
        hash.put(cacheKey, "rate", rate.doubleValue());
        hash.put(cacheKey, "lines", lines);
    }

    private void removeRate(String queueName) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        String cacheKey = getPlotCacheKey(queueName);
        hash.delete(cacheKey);
    }

    private String getPlotCacheKey(String queueName) {
        return "plot_rate::" + queueName;
    }


    /**
     * 1、获取日志队列名（队列名就是dbPlotId）
     * 2、判断日志是否正常
     * 3、累加日志行数
     * 4、计算p盘的百分比
     * 5、监控p盘状态，修改db状态
     */
    private List<HandleResult> handleLog(String command, List<LogChangeMessage> queue) {
        List<HandleResult> results = Lists.newArrayListWithCapacity(queue.size());
        for (LogChangeMessage message : queue) {
            if (SUBMITTED.equals(message.getState())) {
                continue;
            }
            HandleResult result = new HandleResult();
            result.queueName = message.queue;
            result.size = message.size;
            result.isFinished = FINISHED.equals(message.state);
            result.isDelete = REMOVING.equals(message.state);

            if (REGISTER_SERVICE.equals(command)) {
                result.totalLine = countLogLines(message.log);
            } else if (LOG_CHANGED.equals(command)) {
                result.isErrorLog = isErrorLog(message.logNew);
            }
            results.add(result);
        }
        return results;
    }

    private Integer countLogLines(String log) {
        return log.trim().split("\\r\\n|\\r|\\n").length;
    }

    private boolean isErrorLog(String log) {
        return log.contains("Error");
    }

    @Override
    public List<String> support() {
        return Arrays.asList("register_service", "state_changed", "log_changed");
    }

    private static class HandleResult {

        public String queueName;

        public Integer totalLine;

        public boolean isFinished;

        public boolean isDelete;

        public boolean isErrorLog;

        public String size;

    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LogChangeMessage {
        private String queue;
        private String error;
        private String id;
        private String log;
        private String logNew;
        private String size;
        private String state;
    }

}
