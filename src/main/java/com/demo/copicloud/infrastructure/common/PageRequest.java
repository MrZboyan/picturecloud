package com.demo.copicloud.infrastructure.common;

import com.demo.copicloud.infrastructure.constant.CommonConstant;
import lombok.Data;

/**
 * 分页请求 7
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）确保最近的数据在最上方 可以根据自己的需求进行更改
     */
    private String sortOrder = CommonConstant.SORT_ORDER_DESC;
}
