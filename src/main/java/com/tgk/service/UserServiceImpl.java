package com.tgk.service;

import com.spring.*;

@Component("userService")//加了component注解的类才是Spring真正关心的，加了component就表示这个是一个bean
//@Scope("prototype")
public class UserServiceImpl implements BeanNameAware,InitializingBean, UserService {
    @Autowired
    private OrderService orderService;

    private String beanName;
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void afterPropertiesSet() throws Exception{
        System.out.println("初始化");
    }
    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public void test(){
        System.out.println(orderService);
        System.out.println(beanName);
        System.out.println(name);
    }

}
