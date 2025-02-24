package com.demo.project.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.project.constant.CommonConstant;
import com.demo.copicloud.infrastructure.exception.BusinessException;
import com.demo.copicloud.infrastructure.exception.ErrorCode;
import com.demo.project.manager.sharing.DynamicShardingManager;
import com.demo.copicloud.infrastructure.mapper.SpaceMapper;
import com.demo.project.model.dto.space.SpaceAddRequest;
import com.demo.project.model.dto.space.SpaceQueryRequest;
import com.demo.project.model.entity.Space;
import com.demo.project.model.entity.SpaceUser;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.project.model.enums.SpaceLevelEnum;
import com.demo.project.model.enums.SpaceRoleEnum;
import com.demo.project.model.enums.SpaceTypeEnum;
import com.demo.project.model.vo.SpaceVO;
import com.demo.copicloud.interfaces.vo.user.UserVO;
import com.demo.project.service.SpaceService;
import com.demo.project.service.SpaceUserService;
import com.demo.copicloud.application.service.UserApplicationService;
import com.demo.copicloud.infrastructure.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Zangdibo
 * description 针对表【space(空间)】的数据库操作 Service
 * createDate 2025-01-13 16:17:46
 */
@Slf4j
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    @Lazy
    private DynamicShardingManager dynamicShardingManager;

    /**
     * 添加空间
     *
     * @param spaceAddRequest 创建空间请求
     * @param loginUser       当前登录用户
     * @return 返回空间 id
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1.填充参数默认值
        // DTO转实体类
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 名称为空则设置默认名称
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName(loginUser.getUserName() + "的空间");
        }
        // 等级为空则设置默认等级
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 类型为空则设置默认等级
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 2.默认参数
        this.validSpace(space, true);
        // 3.权限校验 非管理员只能创建普通级别的空间
        Long loginUserId = loginUser.getId();
        space.setUserId(loginUserId);
        // 当用户创建的空间等级和默认的等级不符 并且不是管理员
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 4.控制同一用户只能创建一个私有空间 使用 Redisson 分布式锁管理
        // 使用 RedissonClient 实现分布式锁
        RLock lock = redissonClient.getLock("lock:user:space:" + loginUserId);
        // 尝试获取锁并设置超时时间
        try {
            // 等待 3 秒，持有锁 15 秒
            boolean isLocked = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (isLocked) {
                log.info("获取分布式锁成功: {}", lock.getName());
            } else {
                throw new IllegalStateException("获取分布式锁失败");
            }
            // 使用 Spring 提供的事务模板执行事务逻辑
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 1. 判断是否已有空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, loginUserId) // 查询条件：用户ID匹配
                        .eq(Space::getSpaceType, space.getSpaceType()) // 查询条件：空间类型匹配
                        .exists(); // 检查是否存在记录 大部分场景下较 count 函数的性能更优
                // 2. 如果已存在空间，则抛出异常
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个");
                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                // 如果是团队空间，关联新增团队成员记录
                if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(loginUserId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                dynamicShardingManager.createSpacePictureTable(space);
                // 返回新写入的数据 id
                return space.getId();
            });
            // 返回新创建的空间 ID，如果事务失败则返回 -1L
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复线程中断状态
            throw new IllegalStateException("线程被中断", e);
        } finally {
            // 确保锁最终释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 根据空间级别自动填充空间对象中的参数
     *
     * @param space 空间实体类
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        // 不为空则填充
        if (spaceLevelEnum != null) {
            // 最大容量
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            // 最大记录数
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 空间校验
     *
     * @param space 空间信息
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        // 将空间级别转换为枚举值
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 创建时校验 如果是创建空间
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空！");
            ThrowUtils.throwIf(ObjUtil.isEmpty(spaceType), ErrorCode.PARAMS_ERROR, "空间级别有误！");
            ThrowUtils.throwIf(ObjUtil.isEmpty(spaceType), ErrorCode.PARAMS_ERROR, "空间类型有误！");
        }

        // 修改数据时 有参数则校验
        // 如果名称不为空且大于30
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 如果等级不为空 但不在枚举范围内
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 如果类型不为空 但不在枚举范围内
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }
    }

    /**
     * 获取空间信息 视图
     *
     * @param space   空间信息
     * @param request 请求
     * @return 空间信息 视图
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间信息 视图 分页
     *
     * @param spacePage 空间信息 分页
     * @param request   请求
     * @return 空间信息 视图 分页
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 1. 获取空间分页的信息
        // 从传入的空间分页对象中获取当前页的空间记录列表
        List<Space> spaceList = spacePage.getRecords();

        // 2. 构造分页对象并转换为 VO 类
        // 使用传入的分页参数（当前页、每页大小、总记录数）创建一个新的分页对象，用于返回 SpaceVO 类型的数据
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(),
                spacePage.getSize(),
                spacePage.getTotal());

        // 3. 如果空间记录为空则直接返回空的 VO 分页对象
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        // 4. 将空间记录列表中的每个 Space 对象转换为 SpaceVO 对象
        // 调用 SpaceVO 的静态方法 objToVo 进行转换
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());

        // 5. 关联查询用户信息
        // 从空间列表中提取出所有用户的 ID，放入一个 Set 集合（确保去重）
        Set<Long> userIdSet = spaceList.stream()
                .map(Space::getUserId)
                .collect(Collectors.toSet());

        // 6. 根据用户 ID 集合批量查询用户信息
        // 将查询结果转换为 Map，键为用户 ID，值为对应的用户对象列表（通常是单个对象，但用 List 表示为了通用性）
        Map<Long, List<User>> userIdUserListMap =
                userApplicationService.listByIds(userIdSet)
                        .stream()
                        .collect(Collectors.groupingBy(User::getId));

        // 7. 填充用户信息到 SpaceVO 对象
        // 遍历转换后的 SpaceVO 列表，为每个 SpaceVO 填充对应的用户信息
        spaceVOList.forEach(spaceVO -> {
            // 获取当前空间的用户 ID
            Long userId = spaceVO.getUserId();
            User user = null;

            // 如果用户 ID 存在于用户信息 Map 中，取出对应的用户对象（第一个）
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }

            // 调用 userService 的方法将 User 转换为 UserVO 并设置到 SpaceVO 对象中
            spaceVO.setUser(userApplicationService.getUserVO(user));
        });

        // 8. 将转换后的 SpaceVO 列表设置为分页对象的记录数据
        spaceVOPage.setRecords(spaceVOList);

        // 9. 返回包含 SpaceVO 数据的分页对象
        return spaceVOPage;
    }

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest 空间信息查询请求
     * @return 空间信息
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        // 排序字段
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        // 具体查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);

        return queryWrapper;
    }

    /**
     * 校验用户是否有空间权限
     *
     * @param loginUser 当前登录用户
     * @param space     空间信息
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可编辑 如果既不是空间创建者 又不是管理员 则抛异常
        if (!(space.getUserId().equals(loginUser.getId()) && loginUser.isAdmin())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

}




