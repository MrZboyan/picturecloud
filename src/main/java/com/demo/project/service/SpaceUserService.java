package com.demo.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.project.model.dto.spaceuser.SpaceUserAddRequest;
import com.demo.project.model.dto.spaceuser.SpaceUserQueryRequest;
import com.demo.project.model.entity.SpaceUser;
import com.demo.project.model.vo.SpaceUserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author Zangdibo
* description 针对表【space_user(空间用户关联)】的数据库操作Service
* createDate 2025-02-05 10:49:48
*/
public interface SpaceUserService extends IService<SpaceUser> {

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


}
