package com.demo.copicloud.interfaces.dto.picture;

import com.demo.copicloud.infrastructure.api.AliyunAi.modle.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    @Serial
    private static final long serialVersionUID = 1L;
}

