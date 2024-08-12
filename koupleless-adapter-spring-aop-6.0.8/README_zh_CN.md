## 适配springboot aop 问题合集issues

### 遇到问题
模块启动在 JdkDynamicAopProxy.getProxy 报错 xxx referenced from a method is not visible from class loader

### 问题原因
在特定几个 spring-aop 版本里，getProxy 的实现存在 bug，这里即使传入 BizClassLoader，也会因为 BizClassLoader 没有 parent 而强制切换 classLoader 成了 基座 ClassLoader

```java
public Object getProxy(@Nullable ClassLoader classLoader) {
    if (logger.isTraceEnabled()) {
        logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
    }

    if (classLoader == null || classLoader.getParent() == null) {
        classLoader = this.getClass().getClassLoader();
    }

    return Proxy.newProxyInstance(classLoader, this.proxiedInterfaces, this);
}
```

### 版本范围 
springboot 2.7.11, 2.7.12, 3.0.6, 3.0.7, 3.1.0
spring-aop 5.3.27, 6.0.8, 6.0.9

### 解决方法
更换到其他版本
