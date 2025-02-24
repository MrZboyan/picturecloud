package com.demo.copicloud.interfaces.dto.space.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 值
     */
    private int value;

    /**
     * 对应的等级
     */
    private String text;

    /**
     * 最大记录数
     */
    private long maxCount;

    /**
     * 最大容量
     */
    private long maxSize;
}


