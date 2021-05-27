package com.cca.chia.types.plot;

import lombok.Data;

/**
 * @author cca
 * @version 1.0
 * @date 2021/5/22 15:04
 */
@Data
public class StopPlotParam {

    /**
     * 远程服务器Ip(chia客户端所在机器ip)
     */
    private String remoteServerIp;

    private String plotId;

}
