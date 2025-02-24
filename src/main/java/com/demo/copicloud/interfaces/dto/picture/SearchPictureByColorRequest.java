package com.demo.copicloud.interfaces.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SearchPictureByColorRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 颜色
     */
    private String color;

    @Serial
    private static final long serialVersionUID = 1L;
}

