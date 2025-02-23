package com.demo.project.model.dto.space.analyze;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private Boolean queryPublic;

    /**
     * 全空间分析
     */
    private Boolean queryAll;

    @Serial
    private static final long serialVersionUID = 1L;
}


