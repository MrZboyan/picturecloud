package com.demo.copicloud.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.infrastructure.common.DeleteRequest;
import com.demo.copicloud.interfaces.dto.user.*;
import com.demo.copicloud.interfaces.vo.user.LoginUserVO;
import com.demo.copicloud.interfaces.vo.user.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Set;

/**
 * 用户服务
 */
public interface UserApplicationService {

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletResponse response, HttpServletRequest request);

    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 添加用户
     */
    User addUser(User user, HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 更新用户信息
     *
     * @param userUpdateMyRequest 修改用户信息
     */
    boolean updateMyUser(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request);

    /**
     * 修改密码
     *
     * @param userUpdatePasswordRequest 请求
     */
    boolean updatePassword(UserUpdatePasswordRequest userUpdatePasswordRequest,HttpServletRequest request);

    User getUserById(long id);

    UserVO getUserVOById(long id);

    boolean deleteUser(DeleteRequest deleteRequest);

    boolean updateUser(User user);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    Page<User> userPage(UserQueryRequest userQueryRequest, QueryWrapper<User> queryWrapper);

}
