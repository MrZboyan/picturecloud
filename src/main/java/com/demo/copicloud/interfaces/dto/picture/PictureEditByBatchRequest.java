package com.demo.copicloud.interfaces.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditByBatchRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 图片 id 列表
     */
    private List<Long> pictureIds;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;

    @Serial
    private static final long serialVersionUID = 1L;
}

