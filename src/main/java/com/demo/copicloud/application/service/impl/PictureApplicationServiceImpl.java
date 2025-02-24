package com.demo.copicloud.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.copicloud.application.service.PictureApplicationService;
import com.demo.copicloud.application.service.UserApplicationService;
import com.demo.copicloud.domain.picture.entity.Picture;
import com.demo.copicloud.domain.picture.service.PictureDomainService;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.infrastructure.api.AliyunAi.modle.CreateOutPaintingTaskResponse;
import com.demo.copicloud.infrastructure.api.ImageSearch.modle.ImageSearchResult;
import com.demo.copicloud.interfaces.dto.picture.*;
import com.demo.copicloud.interfaces.vo.picture.PictureVO;
import com.demo.copicloud.interfaces.vo.user.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Zangdibo
 * description 针对表【picture(图片)】的数据库操作Service实现
 * createDate 2024-12-16 15:42:24
 */
@Slf4j
@Service
public class PictureApplicationServiceImpl implements PictureApplicationService {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureDomainService pictureDomainService;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        return pictureDomainService.uploadPicture(inputSource, pictureUploadRequest, loginUser);
    }

    @Override
    public void validPicture(Picture picture) {
        pictureDomainService.validPicture(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    /**
     * 获取图片 VO
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        return pictureDomainService.getPictureVOPage(picturePage, request);
    }

    /**
     * 更新图片
     */
    @Override
    public void updatePicture(Picture picture, HttpServletRequest request) {
        pictureDomainService.updatePicture(picture, request);
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        pictureDomainService.doPictureReview(pictureReviewRequest, loginUser);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        pictureDomainService.fillReviewParams(picture, loginUser);
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        return pictureDomainService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
    }

    @Override
    public void clearPictureFile(Picture oldPicture) {
        pictureDomainService.clearPictureFile(oldPicture);
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        pictureDomainService.deletePicture(pictureId, loginUser);
    }

    /**
     * 根据图片搜索图片
     *
     * @param searchRequest 搜索请求
     * @return 搜索结果
     */
    @Override
    public List<ImageSearchResult> searchPicture(SearchPictureByPictureRequest searchRequest) {
        return pictureDomainService.searchPicture(searchRequest);
    }

    /**
     * 根据颜色搜索图片
     *
     * @param searchRequest 搜索请求
     * @param loginUser     登录用户
     * @return 搜索结果
     */
    @Override
    public List<PictureVO> searchPictureByColor(SearchPictureByColorRequest searchRequest, User loginUser) {
        return pictureDomainService.searchPictureByColor(searchRequest,loginUser);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        pictureDomainService.editPicture(pictureEditRequest, loginUser);
    }

    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        pictureDomainService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        return pictureDomainService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    }

    /**
     * 获取图片信息（缓存）
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 图片信息
     */
    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        return pictureDomainService.listPictureVOByPageWithCache(pictureQueryRequest, request);
    }

    /**
     * 根据 id 获取图片信息
     */
    @Override
    public Picture getById(long id) {
        return pictureDomainService.getById(id);
    }

    /**
     * 获取图片分页
     */
    @Override
    public Page<Picture> getPicturePage(PictureQueryRequest pictureQueryRequest, QueryWrapper<Picture> queryWrapper) {
        return pictureDomainService.getPicturePage(pictureQueryRequest, queryWrapper);
    }
}




