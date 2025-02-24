package com.demo.copicloud.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.copicloud.application.service.SpaceApplicationService;
import com.demo.copicloud.domain.space.entity.Space;
import com.demo.copicloud.domain.space.service.SpaceDomainService;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.infrastructure.common.DeleteRequest;
import com.demo.copicloud.interfaces.dto.space.space.SpaceAddRequest;
import com.demo.copicloud.interfaces.dto.space.space.SpaceEditRequest;
import com.demo.copicloud.interfaces.dto.space.space.SpaceQueryRequest;
import com.demo.copicloud.interfaces.vo.space.SpaceVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author Zangdibo
 * description 针对表【space(空间)】的数据库操作 Service
 * createDate 2025-01-13 16:17:46
 */
@Slf4j
@Service
public class SpaceApplicationServiceImpl implements SpaceApplicationService {

    @Resource
    @Lazy
    private SpaceDomainService spaceDomainService;

    /**
     * 添加空间
     *
     * @param spaceAddRequest 创建空间请求
     * @param loginUser       当前登录用户
     * @return 返回空间 id
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        return spaceDomainService.addSpace(spaceAddRequest, loginUser);
    }

    /**
     * 根据空间级别自动填充空间对象中的参数
     *
     * @param space 空间实体类
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    /**
     * 空间校验
     *
     * @param space 空间信息
     */
    @Override
    public void validSpace(Space space, boolean add) {
        spaceDomainService.validSpace(space, add);
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
        return spaceDomainService.getSpaceVO(space, request);
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
        return spaceDomainService.getSpaceVOPage(spacePage, request);
    }

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest 空间信息查询请求
     * @return 空间信息
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    /**
     * 校验用户是否有空间权限
     *
     * @param loginUser 当前登录用户
     * @param space     空间信息
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        spaceDomainService.checkSpaceAuth(loginUser, space);
    }

    /**
     * 根据空间id获取空间信息
     */
    @Override
    public Space getById(Long spaceId) {
        return spaceDomainService.getById(spaceId);
    }

    /**
     * 根据空间 id 集合获取空间信息
     */
    @Override
    public List<Space> listByIds(Set<Long> spaceIds) {
        return spaceDomainService.listByIds(spaceIds);
    }

    /**
     * 删除空间
     */
    @Override
    public boolean removeById(long id) {
        return spaceDomainService.removeById(id);
    }

    /**
     * 更新空间
     */
    @Override
    public boolean updateById(Space space) {
        return spaceDomainService.updateById(space);
    }

    /**
     * 分页查询空间信息
     */
    @Override
    public Page<Space> page(SpaceQueryRequest spaceQueryRequest, QueryWrapper<Space> queryWrapper) {
        return spaceDomainService.page(spaceQueryRequest, queryWrapper);
    }

    /**
     * 编辑空间信息
     */
    @Override
    public void editSpace(SpaceEditRequest spaceEditRequest, User loginUser) {
        spaceDomainService.editSpace(spaceEditRequest, loginUser);
    }

    /**
     * 删除空间信息
     */
    @Override
    public void deleteSpace(DeleteRequest deleteRequest, User loginUser) {
        spaceDomainService.deleteSpace(deleteRequest, loginUser);
    }

    /**
     * 更新空间信息
     */
    @Override
    public void updateSpace(Space spaceEntity) {
        spaceDomainService.updateSpace(spaceEntity);
    }

    /**
     * 获取空间列表篇
     */
    @Override
    public List<Space> list(QueryWrapper<Space> queryWrapper) {
        return spaceDomainService.list(queryWrapper);
    }

}




