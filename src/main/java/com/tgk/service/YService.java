package com.tgk.service;

import com.spring.Autowired;
import com.spring.Component;
import com.spring.Scope;


@Component("yService")
@Scope("singleton")
public class YService {
    @Autowired
    OrderService orderService;

    public void test(){
        System.out.println(orderService);
    }
}
