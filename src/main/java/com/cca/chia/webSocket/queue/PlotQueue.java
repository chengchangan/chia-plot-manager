package com.cca.chia.webSocket.queue;

import com.cca.chia.types.plot.StartPlotParam;
import com.cca.chia.webSocket.ParamBuilder;
import com.cca.chia.webSocket.WebSocketClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 此队列调度用于控制服务器的p盘并发数
 * 通过加载配置，控制是否需要发送p盘任务到chia服务端
 *
 * @author cca
 * @version 1.0
 * @date 2021/5/24 11:23
 */
@Component
public class PlotQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlotQueue.class);

    @Autowired
    private WebSocketClientManager webSocketClientFactory;

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1,
            x -> new Thread(x, "chia.remote.webSocket.queue.PlotQueue"));
    /**
     * p 盘任务队列
     */
    private static final Map<String, LinkedBlockingQueue<StartPlotParam>> PLOT_TASK_MAP = new ConcurrentHashMap<>();

    /**
     * 当前每台服务器正在ｐ盘数
     */
    private static final Map<String, AtomicInteger> CURRENT_PLOTTING_NUM_MAP = new ConcurrentHashMap<>();

    /**
     * 配置每台服务器并发ｐ盘数,默认2.
     */
    private Map<String, Integer> concurrencyPlotConfig;


    @PostConstruct
    public void init() {
        // todo 加载系统配置的每台服务器的并发p盘数量，后期可改为动态获取目标机器是否有资源进行p盘
        concurrencyPlotConfig = new HashMap<>();
        executorService.scheduleWithFixedDelay(new PlotTaskChecker(), 5, 3, TimeUnit.SECONDS);
    }


    /**
     * 添加ｐ盘任务到队列
     */
    public void addPlotTask(StartPlotParam startPlotParam) {
        ParamBuilder.checkStartPlotParam(startPlotParam);
        LinkedBlockingQueue<StartPlotParam> serverPlotQueue = PLOT_TASK_MAP.computeIfAbsent(startPlotParam.getRemoteServerIp(), x -> new LinkedBlockingQueue<>());
        serverPlotQueue.add(startPlotParam);
    }

    /**
     * 完成p盘任务
     * 并开始下一个
     */
    public void finishedPlotAndRunNext(String remoteServerIp) {
        CURRENT_PLOTTING_NUM_MAP.get(remoteServerIp).decrementAndGet();
        this.runNextPlot(remoteServerIp);
    }

    /**
     * 发送指定服务器上的下一个p盘任务
     */
    private void runNextPlot(String remoteServerIp) {
        StartPlotParam plotParam = getNextPlot(remoteServerIp);
        if (plotParam != null) {
            String plotParamStr = ParamBuilder.buildStartPlotParam(plotParam);
            webSocketClientFactory.getClient(remoteServerIp).send(plotParamStr);
        }
    }


    /**
     * 获取对应服务器的一个ｐ盘任务
     * <p>
     * 如果当前服务器正在p盘的数量，超过可支持的p盘数量，则返回空
     *
     * @param remoteServerIp
     * @return
     */
    public StartPlotParam getNextPlot(String remoteServerIp) {
        LinkedBlockingQueue<StartPlotParam> queue = PLOT_TASK_MAP.get(remoteServerIp);

        if (queue.isEmpty()) {
            return null;
        }
        if (isCanPlot(remoteServerIp)) {
            CURRENT_PLOTTING_NUM_MAP.get(remoteServerIp).incrementAndGet();
            return queue.poll();
        }
        LOGGER.info("目标服务器：{}，当前p盘数：{}，配置并发数：{}，暂时无法p盘", remoteServerIp,
                CURRENT_PLOTTING_NUM_MAP.get(remoteServerIp).get(), concurrencyPlotConfig.getOrDefault(remoteServerIp, 2));
        return null;
    }

    /**
     * 校验服务器是否可以进行p盘
     *
     * @param remoteServerIp
     * @return
     */
    private boolean isCanPlot(String remoteServerIp) {
        // 当前服务器正在并发p盘的数量
        AtomicInteger currentPlottingNum = CURRENT_PLOTTING_NUM_MAP.computeIfAbsent(remoteServerIp, x -> new AtomicInteger());
        // 当前服务器可支持并发的p盘数
        Integer concurrentNumConfig = concurrencyPlotConfig.getOrDefault(remoteServerIp, 2);
        return concurrentNumConfig > currentPlottingNum.get();
    }


    /**
     * 监听队列收到的p盘任务，并发送给chia
     */
    private class PlotTaskChecker implements Runnable {

        @Override
        public void run() {
            PLOT_TASK_MAP.forEach((remoteServerIp, plotQueue) -> runNextPlot(remoteServerIp));
        }
    }

}
