package com.cca.chia.controller;

import com.cca.chia.api.PlotApi;
import com.cca.chia.types.plot.StartPlotParam;
import com.cca.chia.types.plot.StopPlotParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author cca
 * @version 1.0
 * @date 2021/5/25 16:24
 */
@RestController
@RequestMapping("/chia/plot")
public class PlotTestController {

    @Autowired
    private PlotApi plotApi;

    @PostMapping("/register")
    public void register(@RequestBody StopPlotParam stopPlotParam) {
        plotApi.listenPlotProgress(stopPlotParam.getRemoteServerIp());
    }

    @PostMapping("/start")
    public void start(@RequestBody StartPlotParam startPlotParam) {
        plotApi.startPlot(startPlotParam);
    }

    @PostMapping("/stop")
    public void stop(@RequestBody StopPlotParam stopPlotParam) {
        plotApi.stopPlot(stopPlotParam);
    }


}
