package com.demo.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.project.model.dto.space.SpaceAddRequest;
import com.demo.project.model.dto.space.SpaceQueryRequest;
import com.demo.project.model.entity.Space;
import com.demo.project.model.entity.User;
import com.demo.project.model.vo.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author Zangdibo
* description 针对表【space(空间)】的数据库操作Service
* createDate 2025-01-13 16:17:46
*/
public interface SpaceService extends IService<Space> {

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

}
