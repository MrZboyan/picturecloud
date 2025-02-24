package com.demo.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.copicloud.infrastructure.api.AliyunAi.modle.CreateOutPaintingTaskResponse;
import com.demo.copicloud.infrastructure.api.ImageSearch.modle.ImageSearchResult;
import com.demo.project.model.dto.picture.*;
import com.demo.project.model.entity.Picture;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.project.model.vo.PictureVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author Zangdibo
* description 针对表【picture(图片)】的数据库操作Service
* createDate 2024-12-16 15:42:24
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource 数据源
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return 图片信息 视图
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取图片信息 视图
     *
     * @param picture 图片信息
     * @param request 请求
     * @return 图片信息 视图
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片信息 视图 分页
     *
     * @param picturePage 图片信息 分页
     * @param request 请求
     * @return 图片信息 视图 分页
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片校验
     *
     * @param picture 图片信息
     */
    void validPicture(Picture picture);

    /**
     * 获取查询条件
     *
     * @param pictureQueryRequest 图片信息查询请求
     * @return 图片信息
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser 登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture 图片信息
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量抓取请求
     * @param loginUser 当前登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清理图片
     *
     * @param oldPicture 旧图片
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 编辑图片请求
     * @param loginUser 当前登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 删除图片
     *
     * @param pictureId 删除的图片 id
     * @param loginUser 登录用户
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 根据图片搜索图片
     *
     * @param searchRequest 搜索请求
     * @return 搜索结果
     */
    List<ImageSearchResult> searchPicture(SearchPictureByPictureRequest searchRequest);

    /**
     * 根据颜色搜索图片
     *
     * @param searchRequest 搜索请求
     * @param loginUser 登录用户
     * @return 搜索结果
     */
    List<PictureVO> searchPictureByColor(SearchPictureByColorRequest searchRequest, User loginUser);

    /**
     * 根据图片 id 批量修改标签和分类信息
     *
     * @param pictureEditByBatchRequest 批量修改请求
     * @param loginUser 当前登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建扩图任务请求
     * @param loginUser 当前登录用户
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    /**
     * 获取图片信息（缓存）
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 图片信息
     */
    Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);
}
