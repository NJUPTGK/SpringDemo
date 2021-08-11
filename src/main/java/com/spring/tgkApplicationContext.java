package com.spring;

import com.sun.org.apache.xpath.internal.SourceTree;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class tgkApplicationContext {
    private Class configClass;

    private ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<>();//单例池
    private ConcurrentHashMap<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
    public tgkApplicationContext(Class configClass) {
        this.configClass = configClass;
        //解析配置类
        //ComponentScan注解-->扫描路径--->扫描---->Beandefinition-->BeanDefinitionMap
        scan(configClass);

        for(Map.Entry<String,BeanDefinition> entry:beanDefinitionMap.entrySet()){
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if(beanDefinition.getScope().equals("singleton")){
                Object bean = createBean(beanName,beanDefinition);//单例bean
                singletonObjects.put(beanName,bean);//放进单例池
            }
        }
    }


    private void scan(Class configClass) {
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value();//扫描路径 com.tgk.service
        path = path.replace(".","/");

        //扫描
        //Bootstrap---->jre/lib
        //Ext----------->jre/ext/lib
        //App--------->classpath---->
        ClassLoader classLoader = tgkApplicationContext.class.getClassLoader();//app
        URL resource = classLoader.getResource(path);//拿的是target class下的资源
        File file = new File(resource.getFile());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                String fileName = f.getAbsolutePath();
                if(fileName.endsWith(".class")){
                String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                className = className.replace("\\", ".");
                //System.out.println(className);
                    try {
                        Class<?> Clazz = classLoader.loadClass(className);
                        if (Clazz.isAnnotationPresent(Component.class)) {
                            //表示当前这个类是一个Bean
                            //解析类，判断当前bean是单例bean还是原型bean
                            //BeanDefinition

                            if (BeanPostProcessor.class.isAssignableFrom(Clazz)) {
                                BeanPostProcessor instance = (BeanPostProcessor) Clazz.getDeclaredConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }
                            Component componentAnnotation = Clazz.getDeclaredAnnotation(Component.class);
                            String beanName = componentAnnotation.value();
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(Clazz);
                            if (Clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = Clazz.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            }else {
                                beanDefinition.setScope("singleton");
                            }
                            beanDefinitionMap.put(beanName,beanDefinition);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    public Object getBean(String beanName) {
        //get
        if(beanDefinitionMap.containsKey(beanName)){
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(beanDefinition.getScope().equals("singleton")){
                Object o = singletonObjects.get(beanName);
                return o;
            }
            else {
                Object bean = createBean(beanName,beanDefinition);//创建一个bean  原型bean
                return bean;
            }
        }else {
            throw new NullPointerException();
        }
    }

    public Object createBean(String beanName,BeanDefinition beanDefinition){
        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();//反射
            // 依赖注入
            for (Field declaredField:clazz.getDeclaredFields())
            {
                if (declaredField.isAnnotationPresent(Autowired.class)){
                    //对属性进行赋值
                    Object bean = getBean(declaredField.getName());
                    declaredField.setAccessible(true);//不加这句代码会报错
                    declaredField.set(instance,bean);
                }
            }

            //Aware回调
            if(instance instanceof BeanNameAware){
                ((BeanNameAware)instance).setBeanName(beanName);//Aware回调，获取bean的名字
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            //初始化
            if(instance instanceof  InitializingBean){
                try {
                    ((InitializingBean)instance).afterPropertiesSet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }
            //BeanPostProcessor


            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}


