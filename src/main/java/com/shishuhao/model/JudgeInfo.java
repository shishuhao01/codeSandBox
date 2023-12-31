package com.shishuhao.model;

import lombok.Data;

/**
 * 题目配置
 */
@Data
public class JudgeInfo {
    /**
     * 执行信息
     */
    private String message;

    /**
     * 内存消耗 kB
     */
    private Long memoryLimit;

    /**
     * 消耗时间 kB
     */
    private Long time;

}
