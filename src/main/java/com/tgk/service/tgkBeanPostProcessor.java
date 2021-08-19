package com.tgk.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class tgkBeanPostProcessor implements BeanPostProcessor{

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if(beanName.equals("userService")){
            System.out.println("初始化前");
            ((UserServiceImpl)bean).setName("唐国凯");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("初始化后");
        if(beanName.equals("userService")){
            Object proxyInstance = Proxy.newProxyInstance(tgkBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new
                    InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            System.out.println("先执行一些代理逻辑");
                            //使用代理的目的是 可以在真正执行UserService的方法之前执行一些代理逻辑
                            return method.invoke(bean,args);//真正执行UserService的方法
                            //return null;
                        }
                    });
            return proxyInstance;//返回一个代理实例
        }
        return bean;
    }
}
