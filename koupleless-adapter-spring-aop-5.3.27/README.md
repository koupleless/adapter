# Springboot AOP Issue Collection
### Issue Encountered
Module startup in JdkDynamicAopProxy.getProxy reports an error: xxx referenced from a method is not visible from class loader
### Issue Cause
In certain spring-aop versions, there is a bug in the implementation of getProxy. Even if a BizClassLoader is passed in, it will forcibly switch the classLoader to the base ClassLoader because the BizClassLoader does not have a parent.
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
In ConfigFileApplicationListener, SpringFactoriesLoader.loadFactories uses the classLoader of the current class to scan for resource files, causing it to only scan the base resource files.
### Version Range
springboot 2.7.11, 2.7.12, 3.0.6, 3.0.7, 3.1.0
spring-aop 5.3.27, 6.0.8, 6.0.9
### Solution
Switch to a different version.
