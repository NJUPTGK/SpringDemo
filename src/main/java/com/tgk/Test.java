package com.tgk;
import com.spring.tgkApplicationContext;
import com.tgk.service.YService;

public class Test {
    public static void main(String[] args) {
        tgkApplicationContext applicationContext = new tgkApplicationContext(Appconfig.class);
        YService yservice = (YService) applicationContext.getBean("yService");
        yservice.test();

    }
}
