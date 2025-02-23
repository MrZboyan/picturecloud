package com.demo.project.aop.auth;

import com.demo.project.annotation.AuthCheck;
import com.demo.project.exception.BusinessException;
import com.demo.project.exception.ErrorCode;
import com.demo.project.model.entity.User;
import com.demo.project.model.enums.UserRoleEnum;
import com.demo.project.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验 AOP
 * 切面类
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 获取注解中的必须角色
        String mustRole = authCheck.mustRole();
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (mustRoleEnum == null) {
            // 无需角色校验，直接放行
            return joinPoint.proceed();
        }
        // 2. 获取当前用户
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        User loginUser = userService.getLoginUser(request);
        // 3. 解析用户角色
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null || UserRoleEnum.BAN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户角色无效或已被封禁");
        }
        // 4. 角色校验
        if (mustRoleEnum == UserRoleEnum.ADMIN && userRoleEnum != UserRoleEnum.ADMIN) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }
        // 5. 放行请求
        return joinPoint.proceed();
    }
}

