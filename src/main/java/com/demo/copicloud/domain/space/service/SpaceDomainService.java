package com.demo.copicloud.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.copicloud.domain.space.entity.Space;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.infrastructure.common.DeleteRequest;
import com.demo.copicloud.interfaces.dto.space.space.SpaceAddRequest;
import com.demo.copicloud.interfaces.dto.space.space.SpaceEditRequest;
import com.demo.copicloud.interfaces.dto.space.space.SpaceQueryRequest;
import com.demo.copicloud.interfaces.vo.space.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;

/**
* @author Zangdibo
* description 针对表【space(空间)】的数据库操作Service
* createDate 2025-01-13 16:17:46
*/
public interface SpaceDomainService {

    /**
     * 添加空间
     *
     * @param spaceAddRequest 创建空间请求
     * @param loginUser 当前登录用户
     * @return 返回空间 id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 获取空间信息 视图
     *
     * @param space 空间信息
     * @param request 请求
     * @return 空间信息 视图
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间信息 视图 分页
     *
     * @param spacePage 空间信息 分页
     * @param request 请求
     * @return 空间信息 视图 分页
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 空间校验
     *
     * @param space 空间信息
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest 空间信息查询请求
     * @return 空间信息
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别自动填充空间对象中的参数
     *
     * @param space 空间实体类
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验用户是否有空间权限
     *
     * @param loginUser 当前登录用户
     * @param space 空间信息
     */
    void checkSpaceAuth(User loginUser, Space space);

    /**
     * 根据空间 id 获取空间信息
     */
    Space getById(Long spaceId);

    /**
     * 根据空间 id 获取空间信息
     */
    List<Space> listByIds(Set<Long> spaceIds);

    /**
     * 删除空间
     */
    boolean removeById(long id);

    /**
     * 更新空间
     */
    boolean updateById(Space space);

    /**
     * 分页查询空间
     */
    Page<Space> page(SpaceQueryRequest spaceQueryRequest, QueryWrapper<Space> queryWrapper);

    /**
     * 编辑空间
     */
    void editSpace(SpaceEditRequest spaceEditRequest, User loginUser);

    /**
     * 删除空间
     */
    void deleteSpace(DeleteRequest deleteRequest, User loginUser);

    /**
     * 更新空间
     */
    void updateSpace(Space spaceEntity);

    /**
     * 查询空间列表
     */
    List<Space> list(QueryWrapper<Space> queryWrapper);
}
