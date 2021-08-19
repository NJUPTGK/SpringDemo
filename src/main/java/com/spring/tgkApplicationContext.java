package com.spring;

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

    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();//单例池
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public tgkApplicationContext(Class configClass) {
        this.configClass = configClass;
        //解析配置类
        //ComponentScan注解-->扫描路径--->扫描---->BeanDefinition-->BeanDefinitionMap
        scan(configClass);
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {//ConcurrentHashMap会按首字母的顺序进行排序，
            // 有的时候会有bug，会有循环依赖的问题，这个bug已经在getBean方法里修复好
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);//单例bean
                singletonObjects.put(beanName, bean);//放进单例池
            }
        }
    }


    private void scan(Class configClass) {
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value();//扫描路径 com.tgk.service  path的值就是com.tgk.service
        path = path.replace(".", "/");

        //扫描
        //Bootstrap---->jre/lib
        //Ext----------->jre/ext/lib
        //App--------->classpath---->
        ClassLoader classLoader = tgkApplicationContext.class.getClassLoader();//app   类加载器
        URL resource = classLoader.getResource(path);//拿的是target class下的资源
        //System.out.println(resource.getFile());//打印的是/D:/Users/11136813/IdeaProjects/Springdemo/target/classes/com/tgk/service
        File file = new File(resource.getFile());
        //System.out.println(file.getAbsolutePath());//打印的是D:\Users\11136813\IdeaProjects\Springdemo\target\classes\com\tgk\service
        if (file.isDirectory()) {//测试被抽象路径名所表示的文件是不是路径
            File[] files = file.listFiles();//返回路径下所有文件的路径的数组
            for (File f : files) {
                String fileName = f.getAbsolutePath();
                //System.out.println(fileName);//会打印出类似这样的路径：D:\Users\11136813\IdeaProjects\Springdemo\target\classes\com\tgk\service\UserServiceImpl.class
                if (fileName.endsWith(".class")) {
                    String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    //System.out.println(className);//com\tgk\service\OrderService
                    className = className.replace("\\", ".");//用.代替\\
                    //System.out.println(className);//com.tgk.service.OrderService
                    try {
                        Class<?> Clazz = classLoader.loadClass(className);
                        if (Clazz.isAnnotationPresent(Component.class)) {//判断类上是不是有@Component注解
                            //表示当前这个类是一个Bean
                            //解析类，判断当前bean是单例bean还是原型bean
                            //BeanDefinition

                            if (BeanPostProcessor.class.isAssignableFrom(Clazz)) {//判断BeanPostProcessor是不是clazz的父类
                                BeanPostProcessor instance = (BeanPostProcessor) Clazz.getDeclaredConstructor().newInstance();//实例化
                                beanPostProcessorList.add(instance);//存到列表里
                            }

                            Component componentAnnotation = Clazz.getDeclaredAnnotation(Component.class);//把Component注解拿出来
                            String beanName = componentAnnotation.value();//当前这个类所对应的bean的名字
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(Clazz);//设置bean的类型
                            if (Clazz.isAnnotationPresent(Scope.class)) {//如果有Scope注解
                                Scope scopeAnnotation = Clazz.getDeclaredAnnotation(Scope.class);//拿出Scope注解
                                beanDefinition.setScope(scopeAnnotation.value());//设置bean的作用域
                            } else {
                                beanDefinition.setScope("singleton");//如果没有Scope，就设置为单例bean
                            }
                            beanDefinitionMap.put(beanName, beanDefinition);//放到map里
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
        if (beanDefinitionMap.containsKey(beanName)) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")) {
                Object o = singletonObjects.get(beanName);
                if (o == null){//解决了循环依赖的问题
                    o = createBean(beanName,beanDefinition);
                    singletonObjects.put(beanName,o);

                }
                return o;
            } else {
                Object bean = createBean(beanName, beanDefinition);//创建一个bean  原型bean
                return bean;
            }
        } else {
            throw new NullPointerException();
        }
    }

    public Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();//通过构造方法反射得到一个bean对象
            // 依赖注入
            for (Field declaredField : clazz.getDeclaredFields()) {//把所有的属性拿出来
                if (declaredField.isAnnotationPresent(Autowired.class)) {//这里有可能会有循环依赖的问题
                    //对bean有@Autowired的属性进行赋值
                    Object bean = getBean(declaredField.getName());//通过属性的名字来拿到一个bean
                    declaredField.setAccessible(true);//不加这句代码会报错
                    declaredField.set(instance, bean);//给instance赋值
                }
            }

            //Aware回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);//Aware回调，获取bean的名字
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }//执行bean初始化之前的操作

            /*
            isAssignableFrom()方法是从类继承的角度去判断，instanceof关键字是从实例继承的角度去判断。
            isAssignableFrom()方法是判断是否为某个类的父类，instanceof关键字是判断是否某个类的子类。
            使用方法：
            父类.class.isAssignableFrom(子类.class)
            子类实例 instanceof 父类类型
             */
            //初始化
            if (instance instanceof InitializingBean) {
                try {
                    ((InitializingBean) instance).afterPropertiesSet();//执行初始化操作
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }//执行bean初始化之后的操作


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


