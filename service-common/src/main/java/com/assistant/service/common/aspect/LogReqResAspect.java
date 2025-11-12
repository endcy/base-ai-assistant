package com.assistant.service.common.aspect;

import cn.hutool.json.JSONUtil;
import com.assistant.service.common.annotation.LogReqRes;
import com.assistant.service.common.config.ApolloConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Slf4j
@Aspect // 声明为切面类
@Component // 注册为 Spring Bean
@RequiredArgsConstructor
public class LogReqResAspect {

    private final ApolloConfigService apolloConfigService;

    /**
     * 注解的方法
     *
     * @param joinPoint .
     * @param logReqRes .
     * @return .
     * @throws Throwable .
     */
    @Around("@annotation(logReqRes) || @within(logReqRes)")
    public Object logReqRes(ProceedingJoinPoint joinPoint, LogReqRes logReqRes) throws Throwable {
        String switchKey = logReqRes.value();
        boolean isSwitchOn = apolloConfigService.getConfig(switchKey);
        if (!isSwitchOn) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        Object[] args = joinPoint.getArgs();
        log.info(">>> Request method {} params: {}", methodName, JSONUtil.toJsonStr(args));
        Object result = joinPoint.proceed();
        log.info(">>> Request method {} return: {}", methodName, JSONUtil.toJsonStr(result));

        return result;
    }
}
