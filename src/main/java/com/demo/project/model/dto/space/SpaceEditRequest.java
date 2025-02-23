package com.demo.project.model.dto.space;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    @Serial
    private static final long serialVersionUID = 1L;
}

