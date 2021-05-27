package com.cca.chia.webSocket;

import com.cca.chia.types.plot.StartPlotParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * @author cca
 * @version 1.0
 * @date 2021/5/24 15:21
 */
public class ParamBuilder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    public static void checkStartPlotParam(StartPlotParam startPlotParam) {
        Integer plotSize = startPlotParam.getPlotSize();
        if (plotSize == null) {
            throw new IllegalArgumentException("P盘规格不能为空");
        }
        String tempDir = startPlotParam.getTempDir();
        if (StringUtils.isBlank(tempDir)) {
            throw new IllegalArgumentException("P盘临时文件目录不能为空");
        }

        String finalDir = startPlotParam.getFinalDir();
        if (StringUtils.isBlank(finalDir)) {
            throw new IllegalArgumentException("P盘最终文件目录不能为空");
        }
        String fingerPrint = startPlotParam.getFingerPrint();
        if (StringUtils.isBlank(fingerPrint)) {
            throw new IllegalArgumentException("公共指纹不能为空");
        }
    }

    public static String buildStartPlotParam(StartPlotParam startPlotParam) {
        checkStartPlotParam(startPlotParam);
        String queueName = startPlotParam.getDbPlotId();

        Integer plotSize = startPlotParam.getPlotSize();
        String tempDir = startPlotParam.getTempDir();
        String finalDir = startPlotParam.getFinalDir();
        String fingerPrint = startPlotParam.getFingerPrint();
        String tempDir2Level = startPlotParam.getTempDir2Level();
        if (StringUtils.isBlank(tempDir2Level)) {
            tempDir2Level = tempDir;
        }
        Integer delay = startPlotParam.getDelay() * 60;
        Integer maxRam = startPlotParam.getMaxRam();
        Integer buckets = startPlotParam.getBuckets();
        Integer threadNum = startPlotParam.getThreadNum();
        Boolean disableBitfieldPlotting = startPlotParam.getDisableBitfieldPlotting();
        Boolean excludeFinalDir = startPlotParam.getExcludeFinalDir();

        ObjectNode param = OBJECT_MAPPER.createObjectNode();
        param.put("command", "start_plotting");
        param.put("ack", "false");
        param.put("request_id", startPlotParam.getDbPlotId());
        param.put("destination", "daemon");
        param.put("origin", "ui");
        ObjectNode data = OBJECT_MAPPER.createObjectNode();
        data.put("service", "chia plots create");
        data.put("queue", queueName);
        data.put("k", plotSize);
        data.put("n", 1);
        data.put("t", tempDir);
        data.put("t2", tempDir2Level);
        data.put("d", finalDir);
        data.put("b", maxRam);
        data.put("u", buckets);
        data.put("r", threadNum);
        data.put("a", fingerPrint);
        data.put("e", disableBitfieldPlotting);
        data.put("x", excludeFinalDir);
        data.put("delay", delay);
        data.put("parallel", false);
        data.put("overrideK", false);
        param.set("data", data);
        return param.toString();
    }

    public static String getRegisterParam() {
        ObjectNode param = OBJECT_MAPPER.createObjectNode();
        param.put("command", "register_service");
        param.put("ack", false);
        param.put("request_id", UUID.randomUUID().toString());
        param.put("destination", "daemon");
        param.put("origin", "my_ui");
        ObjectNode data = OBJECT_MAPPER.createObjectNode();
        data.put("service", "chia plots create");
        param.set("data", data);
        return param.toString();
    }


}
