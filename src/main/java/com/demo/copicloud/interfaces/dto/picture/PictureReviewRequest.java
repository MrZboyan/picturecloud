package com.demo.copicloud.interfaces.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员审核包装类
 */
@Data
public class PictureReviewRequest implements Serializable {

    /**
     * 当前被审核的图片 id
     */
    private Long id;

    /**
     * 状态：0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    @Serial
    private static final long serialVersionUID = 1L;
}
