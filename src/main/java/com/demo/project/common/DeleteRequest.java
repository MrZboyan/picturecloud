package com.demo.project.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除请求 8
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    @Serial
    private static final long serialVersionUID = 1L;
}