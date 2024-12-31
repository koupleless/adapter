## 适配合并部署时，模块的资源目录无法正确的加载
https://github.com/sofastack/sofa-ark/issues/1048

## [StaticResourceJars.java](src/main/java/org/springframework/boot/web/servlet/server/StaticResourceJars.java)
### 遇到问题
模块所定义的jsp无法被访问，debug查看是没有被加载进去
### 问题原因
设置模块的StandardContext的资源目录时，拿到的是tomcat的ClassLoader，通过url属性拿不到正确的模块资源目录
### 改动点
重写覆盖掉springboot的org.springframework.boot.web.servlet.server.StaticResourceJars类，将getClass().getClassLoader()改成Thread.currentThread().getContextClassLoader()，获取当前线程的classLoader。再通过classLoader.getParent()拿到BizClassLoader，从而拿到模块对应的资源目录。