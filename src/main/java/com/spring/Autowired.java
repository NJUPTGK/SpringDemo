package com.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)//运行时生效
@Target({ElementType.METHOD,ElementType.FIELD})//写在属性和方法上面
public @interface Autowired {

}