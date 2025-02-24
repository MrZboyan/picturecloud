package com.demo.copicloud.infrastructure.api.ImageSearch;

import com.demo.copicloud.infrastructure.api.ImageSearch.modle.ImageSearchResult;
import com.demo.copicloud.infrastructure.api.ImageSearch.sub.GetImageFirstUrlApi;
import com.demo.copicloud.infrastructure.api.ImageSearch.sub.GetImageListApi;
import com.demo.copicloud.infrastructure.api.ImageSearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl 图片地址
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

}

