package com.tgk;
import com.spring.tgkApplicationContext;
import com.tgk.service.UserService;
import com.tgk.service.UserServiceImpl;

public class Test {
    public static void main(String[] args) {
        tgkApplicationContext applicationContext = new tgkApplicationContext(Appconfig.class);
        UserService userService = (UserService) applicationContext.getBean("userService");
        userService.test();

    }
}
