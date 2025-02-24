package com.demo.copicloud.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.copicloud.application.service.UserApplicationService;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.domain.user.service.UserDomainService;
import com.demo.copicloud.infrastructure.common.DeleteRequest;
import com.demo.copicloud.infrastructure.exception.BusinessException;
import com.demo.copicloud.infrastructure.exception.ErrorCode;
import com.demo.copicloud.infrastructure.utils.ThrowUtils;
import com.demo.copicloud.interfaces.dto.user.*;
import com.demo.copicloud.interfaces.vo.user.LoginUserVO;
import com.demo.copicloud.interfaces.vo.user.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 用户服务实现
 */
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    /**
     * 用户注册
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 参数校验
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 校验
        User.validUserRegister(userAccount, userPassword, checkPassword);
        // 执行
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);
    }

    /**
     * 登录用户视图
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletResponse response, HttpServletRequest request) {
        // 参数校验 清理旧会话 看是否有session 如果有则清空 没有则继续验证登录逻辑
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            request.getSession().invalidate();
        }
        // 校验账号密码
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 校验
        User.validUserLogin(userAccount, userPassword);
        return userDomainService.userLogin(userAccount, userPassword,response, request);
    }

    /**
     * 获取当前登录用户
     *
     * @param request session
     * @return 用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    /**
     * 获取登录用户 视图
     *
     * @param user 用户
     * @return 登录用户脱敏视图
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return userDomainService.getLoginUserVO(user);
    }

    /**
     * 获取用户 视图
     *
     * @param user 用户
     * @return 用户脱敏视图
     */
    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    /**
     * 获取当前登录用户（允许未登录）
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        return userDomainService.getLoginUserPermitNull(request);
    }

    /**
     * 用户注销
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        return userDomainService.userLogout(request);
    }

    /**
     * 添加用户
     */
    @Override
    public User addUser(User user, HttpServletRequest request) {
        return userDomainService.addUser(user,request);
    }

    /**
     * 获取用户列表 视图
     *
     * @param userList 用户列表
     */
    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        return userDomainService.getUserVO(userList);
    }

    /**
     * 获取查询条件
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest 修改信息
     * @param request             当前登录用户
     */
    @Override
    public boolean updateMyUser(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request) {
        return userDomainService.updateMyUser(userUpdateMyRequest, request);
    }

    /**
     * 修改密码
     *
     * @param userUpdatePasswordRequest 修改请求
     * @return 是否成功
     */
    @Override
    public boolean updatePassword(UserUpdatePasswordRequest userUpdatePasswordRequest, HttpServletRequest request) {
        return userDomainService.updatePassword(userUpdatePasswordRequest, request);
    }

    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public boolean updateUser(User user) {
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, size),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userDomainService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }

    /**
     * 分页获取用户信息
     */
    @Override
    public Page<User> userPage(UserQueryRequest userQueryRequest, QueryWrapper<User> queryWrapper) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        return userDomainService.page(new Page<>(current, size), queryWrapper);
    }

}
