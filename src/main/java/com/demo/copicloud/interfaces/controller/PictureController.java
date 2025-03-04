package com.demo.copicloud.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.copicloud.application.service.PictureApplicationService;
import com.demo.copicloud.application.service.SpaceApplicationService;
import com.demo.copicloud.application.service.UserApplicationService;
import com.demo.copicloud.domain.picture.entity.Picture;
import com.demo.copicloud.domain.picture.valueobject.PictureReviewStatusEnum;
import com.demo.copicloud.domain.space.entity.Space;
import com.demo.copicloud.domain.user.constant.UserConstant;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.infrastructure.annotation.AuthCheck;
import com.demo.copicloud.infrastructure.annotation.SaSpaceCheckPermission;
import com.demo.copicloud.infrastructure.api.AliyunAi.AliYunAiApi;
import com.demo.copicloud.infrastructure.api.AliyunAi.modle.CreateOutPaintingTaskResponse;
import com.demo.copicloud.infrastructure.api.AliyunAi.modle.GetOutPaintingTaskResponse;
import com.demo.copicloud.infrastructure.api.ImageSearch.modle.ImageSearchResult;
import com.demo.copicloud.infrastructure.common.BaseResponse;
import com.demo.copicloud.infrastructure.common.DeleteRequest;
import com.demo.copicloud.infrastructure.common.ResultUtils;
import com.demo.copicloud.infrastructure.exception.BusinessException;
import com.demo.copicloud.infrastructure.exception.ErrorCode;
import com.demo.copicloud.infrastructure.manager.auth.SpaceUserAuthManager;
import com.demo.copicloud.infrastructure.manager.auth.model.StpKit;
import com.demo.copicloud.infrastructure.manager.auth.model.SpaceUserPermissionConstant;
import com.demo.copicloud.infrastructure.utils.ThrowUtils;
import com.demo.copicloud.interfaces.assembler.PictureAssembler;
import com.demo.copicloud.interfaces.dto.picture.*;
import com.demo.copicloud.interfaces.vo.picture.PictureTagCategory;
import com.demo.copicloud.interfaces.vo.picture.PictureVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/picture")
@Tag(name = "图片业务接口")
public class PictureController {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureApplicationService pictureApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 图片标签
     */
    @Operation(summary = "获取图片标签")
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意", "宠物");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报", "壁纸", "头像");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 上传图片（本地文件） 添加
     */
    @Operation(summary = "上传图片（本地文件）")
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile file, PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        User loginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(file, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量上传图片（本地文件） 添加
     */
    @Operation(summary = "批量上传图片（本地文件）")
    @PostMapping("/upload/pictures")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Integer> uploadPictures(MultipartFile[] file, PictureUploadRequest pictureUploadRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        User loginUser = userApplicationService.getLoginUser(request);
        // 计数器
        Integer count = 0;
        // 获取所有文件依次上传
        for (MultipartFile multipartFile : file) {
            pictureApplicationService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
            count++;
        }
        return ResultUtils.success(count);
    }

    /**
     * 上传图片（Url） 添加
     */
    @Operation(summary = "上传图片（Url）")
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量抓取图片并上传
     */
    @Operation(summary = "批量抓取图片并上传")
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        int uploadCount = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 删除图片
     */
    @Operation(summary = "删除图片")
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.deletePicture(deleteRequest.getId(),loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @Operation(summary = "更新图片")
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture pictureEntity = PictureAssembler.toPictureEntity(pictureUpdateRequest);
        pictureApplicationService.updatePicture(pictureEntity,request);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片信息（仅管理员可用）
     */
    @Operation(summary = "根据 id 获取图片信息")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片VO（封装类）
     */
    @Operation(summary = "根据 id 获取图片VO")
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间的图片，需要校验权限
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureApplicationService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }


    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @Operation(summary = "分页获取图片列表")
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Picture> picturePage = pictureApplicationService.getPicturePage(pictureQueryRequest,
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @Operation(summary = "分页获取图片列表VO")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 30, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(spaceId, SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "没有空间权限");
        }
        // 查询数据库
        Page<Picture> picturePage = pictureApplicationService.getPicturePage(pictureQueryRequest,
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureApplicationService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表（封装类 + 缓存）
     */
    @Operation(summary = "分页获取图片列表VO（缓存）")
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 分页获取图片列表（封装类 + 缓存）
        Page<PictureVO> pictureVOPage = pictureApplicationService.listPictureVOByPageWithCache(pictureQueryRequest, request);
        // 返回结果
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @Operation(summary = "编辑图片")
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.editPicture(pictureEditRequest,loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 审核图片
     *
     * @param pictureReviewRequest 审核请求
     */
    @Operation(summary = "审核图片")
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 以图搜图
     *
     * @param searchRequest 搜图请求
     */
    @Operation(summary = "以图搜图")
    @PostMapping("/picture_search")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<ImageSearchResult>> searchImage(@RequestBody SearchPictureByPictureRequest searchRequest) {
        ThrowUtils.throwIf(searchRequest == null, ErrorCode.PARAMS_ERROR);
        List<ImageSearchResult> imageSearchResults = pictureApplicationService.searchPicture(searchRequest);
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 颜色搜图
     */
    @Operation(summary = "颜色搜图")
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchImageByColor(@RequestBody SearchPictureByColorRequest searchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<PictureVO> pictureVOS = pictureApplicationService.searchPictureByColor(searchRequest, loginUser);
        return ResultUtils.success(pictureVOS);
    }

    /**
     * 批量编辑图片
     */
    @Operation(summary = "批量编辑图片")
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> batchEditPicture(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * AI 扩图任务
     */
    @Operation(summary = "AI 扩图任务")
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest taskRequest,
                                                                                    HttpServletRequest request) {
        // 参数校验
        if (taskRequest == null || taskRequest.getPictureId() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        CreateOutPaintingTaskResponse taskResponse = pictureApplicationService.createPictureOutPaintingTask(taskRequest, loginUser);
        return ResultUtils.success(taskResponse);
    }

    /**
     * 获取 AI 扩图结果
     */
    @Operation(summary = "获取 AI 扩图结果")
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId){
        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR,"任务 id 不能为空！");
        GetOutPaintingTaskResponse taskResponse = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(taskResponse);
    }

}
