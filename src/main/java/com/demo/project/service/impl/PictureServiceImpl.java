package com.demo.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.project.api.AliyunAi.AliYunAiApi;
import com.demo.project.api.AliyunAi.modle.CreateOutPaintingTaskRequest;
import com.demo.project.api.AliyunAi.modle.CreateOutPaintingTaskResponse;
import com.demo.project.api.ImageSearch.ImageSearchApiFacade;
import com.demo.project.api.ImageSearch.modle.ImageSearchResult;
import com.demo.project.constant.CommonConstant;
import com.demo.project.exception.BusinessException;
import com.demo.project.exception.ErrorCode;
import com.demo.project.manager.upload.CosManager;
import com.demo.project.manager.upload.FilePictureUpload;
import com.demo.project.manager.upload.PictureUploadTemplate;
import com.demo.project.manager.upload.UrlPictureUpload;
import com.demo.project.mapper.PictureMapper;
import com.demo.project.model.dto.file.UploadPictureResult;
import com.demo.project.model.dto.picture.*;
import com.demo.project.model.entity.Picture;
import com.demo.project.model.entity.Space;
import com.demo.project.model.entity.User;
import com.demo.project.model.enums.PictureReviewStatusEnum;
import com.demo.project.model.vo.PictureVO;
import com.demo.project.model.vo.UserVO;
import com.demo.project.service.PictureService;
import com.demo.project.service.SpaceService;
import com.demo.project.service.UserService;
import com.demo.project.utils.ColorSimilarityUtil;
import com.demo.project.utils.ThrowUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Zangdibo
 * description 针对表【picture(图片)】的数据库操作Service实现
 * createDate 2024-12-16 15:42:24
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder()
                    .initialCapacity(1024) // 初始大小
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    /**
     * 上传图片 本地文件
     *
     * @param inputSource          数据源
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser            登录用户
     * @return 图片信息
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 1.校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 空间权限校验
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 2.判断业务类型是 更新 还是 新增图片
        // 获取 id （可以为空 表示新增）
        Long pictureId = pictureUploadRequest.getId();
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            // 通过图片的id 对数据库进行查询 看是否存在
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 判断是否为当前用户或管理员上传的图片不是则抛异常
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限");
            }
            // 校验空间是否一致
            // 没传spaceId 则复用原有图片的 spaceId
            if (spaceId == null) {
                // 并且原空间 id 不为空 则复用
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了spaceId 必须和原来图片的空间id一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
            // 若校验都无问题 则视为更新操作 如果是更新则需要删除原缩略图
            this.clearPictureFile(oldPicture);
            System.out.println("开始执行清理缩略图操作");
        }
        // 3.上传图片 根据用户 id 规划上传路径
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 类型区分上传方式
        // 根据输入源类型，选择不同的上传模板 默认是 filePictureUpload 类型
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        // 如果输入源是字符串类型的实例 而非文件类型 则使用 urlPictureUpload 类型
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        // 使用选择的模板上传图片
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息 并返回一个 picture 对象
        Picture picture = this.getPicture(loginUser, uploadPictureResult, pictureId);
        // 填充额外属性
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setSpaceId(spaceId);
        String picName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        // 5. 上传成功则返回图片信息
        return PictureVO.objToVo(picture);
    }

    /**
     * 上传图片 批量抓取并上传
     *
     * @param pictureUploadByBatchRequest 图片抓取请求
     * @param loginUser                   登录用户
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 获取搜索关键字，即用户请求的图片搜索文本
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 从请求中获取图片名称前缀
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        // 获取上传的图片数量，最大支持30张
        Integer count = pictureUploadByBatchRequest.getCount();
        // 如果请求的数量大于30，则抛出参数错误异常
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");

        // 格式化要抓取的图片地址，构建 Bing 图片搜索 URL
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);

        // 初始化 Jsoup Document 对象，用于抓取网页内容
        Document document;
        try {
            // 通过 Jsoup 连接并抓取网页内容
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            // 如果抓取页面失败，则记录错误日志并抛出业务异常
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        // 获取包含图片的 HTML 元素（根据 CSS 类名 dgControl 获取）
        Element div = document.getElementsByClass("dgControl").first();

        // 如果未找到该元素，则抛出获取元素失败的异常
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }

        // 获取所有图片元素（根据 class 名称 mimg 获取）
        Elements imgElementList = div.select("img.mimg");

        int uploadCount = 0; // 记录已上传的图片数量

        // 遍历图片元素列表，依次上传
        for (Element imgElement : imgElementList) {
            // 获取图片的 src 属性，即图片的 URL
            String fileUrl = imgElement.attr("src");

            // 如果图片 URL 为空，则跳过当前图片
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }

            // 处理图片上传地址，去掉 URL 中的查询参数（即 "?" 后面的部分）
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            // 创建图片上传请求对象
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            try {
                // 调用 uploadPicture 方法上传图片
                if (StrUtil.isNotBlank(namePrefix)) {
                    // 设置图片名称，序号连续递增
                    pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
                }
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++; // 上传成功则增加上传计数
            } catch (Exception e) {
                // 如果上传失败，记录错误并跳过当前图片
                log.error("图片上传失败", e);
                continue;
            }

            // 如果已上传的图片数量达到用户指定的数量，则停止上传
            if (uploadCount >= count) {
                break;
            }
        }

        // 返回实际上传的图片数量
        return uploadCount;
    }

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 编辑图片请求
     * @param loginUser          当前登录用户
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 删除图片
     *
     * @param pictureId 删除的图片 id
     * @param loginUser 登录用户
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        // 根据id查询数据库 判断是否存在 存在则是旧图片
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除 如果不是当前用户上传或创建的 或不是管理员 则无权限删除 不可操作
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // this.checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    /**
     * 根据图片搜索图片
     *
     * @param searchRequest 搜索请求
     * @return 搜索结果
     */
    @Override
    public List<ImageSearchResult> searchPicture(SearchPictureByPictureRequest searchRequest) {
        Long pictureId = searchRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return ImageSearchApiFacade.searchImage(picture.getThumbnailUrl());
    }

    /**
     * 颜色搜图
     *
     * @param searchRequest 搜索请求
     * @param loginUser     登录用户
     */
    @Override
    public List<PictureVO> searchPictureByColor(SearchPictureByColorRequest searchRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(searchRequest.getColor()) ||
                searchRequest.getSpaceId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        String color = searchRequest.getColor();
        Long spaceId = searchRequest.getSpaceId();
        // 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 判断当前用户是否为空间管理员
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        // 首先需要查询出空间内所有 picColor 不为空的字段 并返回一个图片的集合
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 若集合为空则直接返回
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 遍历集合 获取所有图片的 picColor 字段 然后与当前的图片颜色进行比对
        List<Picture> pictures = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    // 计算相似度
                    String picColor = picture.getPicColor();
                    if (StrUtil.isBlank(picColor)) {
                        return Double.MAX_VALUE;
                    }
                    // 越大越相似 负为降序排列
                    return -ColorSimilarityUtil.calculateColorSimilarity(color, picColor);
                }))
                .limit(2)
                .toList();
        // 脱敏后进行返回
        return pictures.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    /**
     * 根据图片 id 批量修改标签和分类信息
     *
     * @param pictureEditByBatchRequest 批量修改请求
     * @param loginUser                 当前登录用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务注解
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 校验参数
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        List<Long> pictureIds = pictureEditByBatchRequest.getPictureIds();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(spaceId == null && CollUtil.isEmpty(pictureIds), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 判断当前用户是否为空间管理员
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        // 查询空间中指定 id 列表的字段信息
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId) // 仅查询 id 和 space_id 字段
                .eq(Picture::getSpaceId, spaceId)           // WHERE space_id = #{spaceId}
                .in(Picture::getId, pictureIds)             // AND id IN (pictureIds集合)
                .list();                                    // 执行查询并返回结果列表

        if (pictureList.isEmpty()){
            return;
        }
        // 执行批量修改
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureNameRule(pictureList, nameRule);
        // 执行批量更新操作
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "批量修改失败");
    }

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建扩图任务请求
     * @param loginUser                           当前登录用户
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                                      User loginUser) {
        // 参数校验 获取 id 去数据库查询
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 权限校验
        // checkPictureAuth(loginUser, picture);
        // 构造请求参数 将 扩图请求中的 input 设置为图片的 url
        // 然后将 createPictureOutPaintingTaskRequest 复制到 taskRequest
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtils.copyProperties(createPictureOutPaintingTaskRequest,taskRequest);
        // 调用 Api 发送扩图任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

    /**
     * 获取图片信息（缓存）
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 图片信息
     */
    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 获取分页参数
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 限制爬虫，防止超大查询
        ThrowUtils.throwIf(size > 30, ErrorCode.PARAMS_ERROR);

        // 普通用户只能查看已审核数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 生成缓存 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "CoPicCloud:listPictureVOByPage:" + hashKey;

        // 查询本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            return JSONUtil.toBean(cachedValue, Page.class, true);
        }

        // 查询 Redis 缓存
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        cachedValue = valueOps.get(cacheKey);
        if (cachedValue != null) {
            LOCAL_CACHE.put(cacheKey, cachedValue); // 更新本地缓存
            return JSONUtil.toBean(cachedValue, Page.class, true);
        }

        // 缓存未命中，查询数据库
        Page<Picture> picturePage = this.page(new Page<>(current, size),
                getQueryWrapper(pictureQueryRequest));

        // 处理数据并转换为 VO
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);

        // 存入本地缓存 & Redis，5-10 分钟随机过期，防止缓存雪崩
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(cacheKey, cacheValue);
        valueOps.set(cacheKey, cacheValue, 300 + RandomUtil.randomInt(0, 300), TimeUnit.SECONDS);

        return pictureVOPage;
    }

    /**
     * 填充图片命名规则 格式：图片{序号}
     *
     * @param pictureList 图片列表
     * @param nameRule   命名规则
     */
    private void fillPictureNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String replaceName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(replaceName);
            }
        } catch (Exception e) {
            log.info("命名规则格式错误:{}", nameRule);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "命名规则格式错误");
        }
    }

    /**
     * 上传图片 - 构造信息
     *
     * @param loginUser           当前登录用户
     * @param uploadPictureResult 已上传的图片信息
     * @param pictureId           图片 id
     * @return 图片实体类
     */
    @NotNull
    public Picture getPicture(User loginUser, UploadPictureResult uploadPictureResult, Long pictureId) {
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());

        // 执行审核参数填充
        this.fillReviewParams(picture, loginUser);
        // 4.保存图片信息到数据库
        // 如果id不为空表示更新
        if (pictureId != null) {
            // 如果是更新需要补充 id 和 编辑时间
            picture.setId(pictureId);
            picture.setUpdateTime(new Date());
        }
        return picture;
    }

    /**
     * 获取图片信息 视图
     *
     * @param picture 图片信息
     * @param request 请求
     * @return 图片信息 视图
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片信息 视图 分页
     *
     * @param picturePage 图片信息 分页
     * @param request     请求
     * @return 图片信息 视图 分页
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 1. 获取图片分页的信息
        // 从传入的图片分页对象中获取当前页的图片记录列表
        List<Picture> pictureList = picturePage.getRecords();

        // 2. 构造分页对象并转换为 VO 类
        // 使用传入的分页参数（当前页、每页大小、总记录数）创建一个新的分页对象，用于返回 PictureVO 类型的数据
        Page<PictureVO> pictureVOPage =
                new Page<>(picturePage.getCurrent(),
                        picturePage.getSize(),
                        picturePage.getTotal());

        // 3. 如果图片记录为空则直接返回空的 VO 分页对象
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        // 4. 将图片记录列表中的每个 Picture 对象转换为 PictureVO 对象
        // 调用 PictureVO 的静态方法 objToVo 进行转换
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());

        // 5. 关联查询用户信息
        // 从图片列表中提取出所有用户的 ID，放入一个 Set 集合（确保去重）
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());

        // 6. 根据用户 ID 集合批量查询用户信息
        // 将查询结果转换为 Map，键为用户 ID，值为对应的用户对象列表（通常是单个对象，但用 List 表示为了通用性）
        Map<Long, List<User>> userIdUserListMap =
                userService.listByIds(userIdSet)
                        .stream()
                        .collect(Collectors.groupingBy(User::getId));

        // 7. 填充用户信息到 PictureVO 对象
        // 遍历转换后的 PictureVO 列表，为每个 PictureVO 填充对应的用户信息
        pictureVOList.forEach(pictureVO -> {
            // 获取当前图片的用户 ID
            Long userId = pictureVO.getUserId();
            User user = null;

            // 如果用户 ID 存在于用户信息 Map 中，取出对应的用户对象（第一个）
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }

            // 调用 userService 的方法将 User 转换为 UserVO 并设置到 PictureVO 对象中
            pictureVO.setUser(userService.getUserVO(user));
        });

        // 8. 将转换后的 PictureVO 列表设置为分页对象的记录数据
        pictureVOPage.setRecords(pictureVOList);

        // 9. 返回包含 PictureVO 数据的分页对象
        return pictureVOPage;
    }

    /**
     * 校验图片信息
     *
     * @param picture 图片信息
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param pictureQueryRequest 图片信息查询请求
     * @return 图片信息
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();


        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name LIKE '%searchText%' OR introduction LIKE '%searchText%')
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        // 精确查询
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // 是否需要查询 spaceId为空的字段 如果需要 就查询 spaceId 列
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // 模糊查询
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);

        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                // and (tag like "%\"Java\"%" and like "%\"Python\"%")
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(
                StrUtil.isNotEmpty(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);

        return queryWrapper;
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        // 此处可以将 审核状态转为枚举类中的状态
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            log.info("传入参数错误，id:{}, reviewStatus: {}, reviewMessage: {}", id, reviewStatus, reviewMessage);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // 2. 判断图片是否存在
        Picture oldPicture = getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态重复！");
        }
        // 4. 更新数据库
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewMessage(reviewMessage);
        updatePicture.setReviewTime(new Date());

        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核参数
     *
     * @param picture   图片信息
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 首先判断当前用户是否为管理员
        if (userService.isAdmin(loginUser)) {
            // 是管理员自动过审 填充参数
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 不是管理员，则将状态设置为默认值
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 清理图片
     *
     * @param oldPicture 旧图片
     */
    @Async // 使方法异步的执行
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该记录是否被多条记录给使用
        String oldPictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, oldPictureUrl)
                .count();
        if (count > 1) {
            return;
        }
        // 删除图片 在对象的访问域名 bucket.cos.ap-guangzhou.com/doc/picture.jpg 中，
        // 对象键为 doc/picture.jpg 此处传入的 key 的值就是对象键
        // 清理 webp 文件
        String objKey = this.checkUrl(oldPicture.getUrl());
        cosManager.deleteObject(objKey);
        // 清理缩略图
        objKey = this.checkUrl(oldPicture.getThumbnailUrl());
        if (StrUtil.isNotBlank(objKey)) {
            cosManager.deleteObject(objKey);
        }
    }

    /**
     * 处理对象存储中的 url
     * 使其只包含路径 而非绝对地址
     *
     * @param url 绝对地址
     * @return 路径部分
     */
    public String checkUrl(String url) {
        URI uri = URI.create(url);
        String result = uri.getPath(); // 获取路径部分

        // 去掉开头的 "/"
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

}




