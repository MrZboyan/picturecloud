package com.demo.copicloud.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.copicloud.application.service.SpaceUserApplicationService;
import com.demo.copicloud.domain.space.entity.SpaceUser;
import com.demo.copicloud.domain.space.service.SpaceUserDomainService;
import com.demo.copicloud.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import com.demo.copicloud.interfaces.dto.space.spaceuser.SpaceUserQueryRequest;
import com.demo.copicloud.interfaces.vo.space.SpaceUserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Zangdibo
 * description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * createDate 2025-02-05 10:49:48
 */
@Service
public class SpaceUserApplicationServiceImpl implements SpaceUserApplicationService {

    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest 添加空间成员请求
     * @return 返回空间 id
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        return spaceUserDomainService.addSpaceUser(spaceUserAddRequest);
    }

    /**
     * 空间成员校验
     *
     * @param spaceUser 空间信息
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        spaceUserDomainService.validSpaceUser(spaceUser, add);
    }

    /**
     * 获取空间成员信息 视图
     *
     * @param spaceUser 空间成员信息
     * @param request 请求
     * @return 空间成员信息 视图
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        return spaceUserDomainService.getSpaceUserVO(spaceUser, request);
    }

    /**
     * 获取空间成员信息 视图 列表
     *
     * @param spaceUserList 空间成员信息 列表
     * @return 空间成员信息 视图 列表
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        return spaceUserDomainService.getSpaceUserVOList(spaceUserList);
    }

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest 空间信息查询请求
     * @return 空间信息
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        return spaceUserDomainService.getQueryWrapper(spaceUserQueryRequest);
    }

    /**
     * 保存空间成员
     */
    @Override
    public boolean save(SpaceUser spaceUser) {
        return spaceUserDomainService.save(spaceUser);
    }

    @Override
    public SpaceUser getById(long id) {
        return spaceUserDomainService.getById(id);
    }

    @Override
    public boolean removeById(long id) {
        return spaceUserDomainService.removeById(id);
    }

    @Override
    public SpaceUser getOne(QueryWrapper<SpaceUser> queryWrapper) {
        return spaceUserDomainService.getOne(queryWrapper);
    }

    @Override
    public List<SpaceUser> list(QueryWrapper<SpaceUser> queryWrapper) {
        return spaceUserDomainService.list(queryWrapper);
    }

    @Override
    public boolean updateById(SpaceUser spaceUser) {
        return spaceUserDomainService.updateById(spaceUser);
    }

}




