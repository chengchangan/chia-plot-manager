package com.cca.chia.types.plot;

import lombok.Data;

/**
 * @author cca
 * @version 1.0
 * @date 2021/5/22 15:04
 */
@Data
public class StartPlotParam {

    /**
     * 远程服务器IP(chia客户端所在机器ip)
     */
    private String remoteServerIp;
    /**
     * 使用数据库的plotId 当作p盘任务的 队列名 和 requestId
     */
    private String dbPlotId;
    /**
     * P图数量.（仅用于前段传参，后端拆分任务）
     */
    private Integer plotCount = 1;
    /**
     * P盘规格
     */
    private Integer plotSize;
    /**
     * 延迟时间
     */
    private Integer delay = 0;
    /**
     * 是否并发.(暂时无用)
     */
    private Boolean parallel = false;
    /**
     * 临时文件存放目录
     */
    private String tempDir;
    /**
     * 临时文件二级存放目录
     */
    private String tempDir2Level;
    /**
     * 最终文件存放目录
     */
    private String finalDir;
    /**
     * 最大使用内存
     */
    private Integer maxRam = 2048;
    /**
     * 桶的数量
     */
    private Integer buckets = 128;
    /**
     * 线程数
     */
    private Integer threadNum = 2;
    /**
     * 登陆的指纹
     */
    private String fingerPrint;
    /**
     * 开垦时禁用位域
     */
    private Boolean disableBitfieldPlotting = false;
    /**
     * 排除最终目录，P完不直接使用solo 挖矿
     */
    private Boolean excludeFinalDir = false;

}
