package com.demo.copicloud.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.copicloud.domain.space.entity.SpaceUser;
import com.demo.copicloud.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import com.demo.copicloud.interfaces.dto.space.spaceuser.SpaceUserQueryRequest;
import com.demo.copicloud.interfaces.vo.space.SpaceUserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author Zangdibo
* description 针对表【space_user(空间用户关联)】的数据库操作Service
* createDate 2025-02-05 10:49:48
*/
public interface SpaceUserDomainService {

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest 添加空间成员请求
     * @return 返回空间 id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 空间成员校验
     *
     * @param spaceUser 空间信息
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员信息 视图
     *
     * @param spaceUser 空间成员信息
     * @param request 请求
     * @return 空间成员信息 视图
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员信息 视图 列表
     *
     * @param spaceUserList 空间成员信息 列表
     * @return 空间成员信息 视图 列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest 空间信息查询请求
     * @return 空间信息
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);


    /**
     * 保存空间成员
     */
    boolean save(SpaceUser spaceUser);

    SpaceUser getById(long id);

    boolean removeById(long id);

    SpaceUser getOne(QueryWrapper<SpaceUser> queryWrapper);

    List<SpaceUser> list(QueryWrapper<SpaceUser> queryWrapper);

    boolean updateById(SpaceUser spaceUser);

}
