package com.demo.project.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片id （用于判断是更新还是删除）
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 图片名称
     */
    private String picName;

    @Serial
    private static final long serialVersionUID = 1L;
}
