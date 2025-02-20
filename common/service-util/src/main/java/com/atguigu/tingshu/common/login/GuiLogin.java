package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

/**
 * 自定义注解-登录拦截
 */
@Target({ ElementType.METHOD}) // 方法级别
@Retention(RetentionPolicy.RUNTIME)
@Inherited // 子父继承
@Documented
public @interface GuiLogin {

    /**
     * 是否需要拦截
     * true：认证+拦截
     * false：认证+不拦截
     * @return
     */
    boolean required() default true;
}
