package com.alipay.sofa.koupleless.adapter.thread;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BizCallableTest {

    @Test
    public void testBizRunnable() throws Exception {
        Callable callable = () -> {
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            System.out.println(String.format("classLoader: %s", currentClassLoader));
            System.out.println("Hello, World!");
            return currentClassLoader;
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        // 第一次执行，会使用当前线程的 classLoader，也就是 appClassLoader,后续默认执行任务都会使用这个 classLoader
        Future<ClassLoader> future = executor.submit(callable);
        Assert.assertTrue(future.get() == Thread.currentThread().getContextClassLoader());

        // 创建一个模拟 biz 的 BizclassLoader
        URL url = this.getClass().getResource("");
        ClassLoader bizClassLoader = new URLClassLoader(new URL[]{url});

        // 记录老的 classLoader
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 切换现成 classLoader 为 bizClassLoader
            Thread.currentThread().setContextClassLoader(bizClassLoader);
            BizCallable bizCallable = new BizCallable(callable);
            Future<ClassLoader> future1 = executor.submit(bizCallable);
            Assert.assertTrue(future1.get() == bizClassLoader);
        } finally {
            // 将线程 classLoader 切换会老的 classLoader
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
