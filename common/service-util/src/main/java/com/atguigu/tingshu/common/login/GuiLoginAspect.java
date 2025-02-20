package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class GuiLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..))&& @annotation(guiLogin)")
    public Object doConcurrentOperation(ProceedingJoinPoint joinPoint, GuiLogin guiLogin) throws Throwable {
        int numAttempts = 0;
        // 定义返回结果
        Object object = new Object();

        // 通过请求上下文对象，获取请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 将请求属性转换为Servlet请求属性
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        // 获取请求对象
        HttpServletRequest request = servletRequestAttributes.getRequest();

        // 获取Token
        String token = request.getHeader("token");

        // 定义用户登录存储key
        String loginKey =  RedisConstant.USER_LOGIN_KEY_PREFIX+token;
        // 尝试从redis中获取用户信息
        UserInfoVo userInfo = (UserInfoVo) redisTemplate.opsForValue().get(loginKey);
        // 认证且拦截
        if (guiLogin.required() && userInfo == null){
            // 返回状态码208，前端会跳转到登录页面
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 存储用户id
        if (userInfo!=null){
            // 设置userId，业务中随时获取
            AuthContextHolder.setUserId(userInfo.getId());
        }

        // 执行目标方法
        object = joinPoint.proceed();

        // 清除线程变量中userId,避兔0OM,防止用户信息泄露
        AuthContextHolder.removeUserId();


        return object;
    }
    }
