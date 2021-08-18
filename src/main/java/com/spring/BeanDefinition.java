package com.spring;

public class BeanDefinition {
    private Class clazz;//当前bean的类型
    private String scope;//当前Bean的作用域


    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
