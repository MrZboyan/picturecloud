package com.demo.copicloud.infrastructure.aop;

import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.infrastructure.aop.auth.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 请求响应日志 AOP
 **/
@Aspect
@Component
@Slf4j
public class LogInterceptor {
    @Around("execution(* com..interfaces.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 开始计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 获取请求上下文
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // 从 session 或 token 中获取用户信息
        User loginUser = AuthUtils.checkUser(request);
        // 构造日志参数
        Long userId = loginUser.getId();
        String requestId = UUID.randomUUID().toString();
        String url = request.getRequestURI();
        String reqParam = "[" + StringUtils.join(point.getArgs(), ", ") + "]";
        // 输出请求日志
        log.info("请求开始-- 请求ID: {}, 用户ID: {}, 请求路径: {}, 请求IP: {}, 请求参数: {}",
                requestId, userId != null ? userId : "游客", url, request.getRemoteHost(), reqParam);
        // 执行后续逻辑 停止计时
        Object result = point.proceed();
        stopWatch.stop();
        // 输出响应日志
        log.info("请求结束-- ID: {}, 耗时: {}ms", requestId, stopWatch.getTotalTimeMillis());
        // 返回结果
        return result;
    }
}

