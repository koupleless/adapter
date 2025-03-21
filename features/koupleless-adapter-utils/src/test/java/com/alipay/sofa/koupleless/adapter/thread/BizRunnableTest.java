package com.alipay.sofa.koupleless.adapter.thread;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BizRunnableTest {

    @Test
    public void testBizRunnable() throws Exception {
        final ClassLoader[] recordClassLoader = {new URLClassLoader(new URL[]{})}; // 使用数组包装

        Runnable runnable = () -> {
            recordClassLoader[0] = Thread.currentThread().getContextClassLoader();
            System.out.println(String.format("classLoader: %s", recordClassLoader[0]));
            System.out.println("Hello, World!");
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        // 第一次执行，会使用当前线程的 classLoader，也就是 appClassLoader,后续默认执行任务都会使用这个 classLoader
        executor.submit(runnable);
        TimeUnit.SECONDS.sleep(2);
        Assert.assertTrue(recordClassLoader[0] == Thread.currentThread().getContextClassLoader());

        // 创建一个模拟 biz 的 BizclassLoader
        URL url = this.getClass().getResource("");
        ClassLoader bizClassLoader = new URLClassLoader(new URL[]{url});

        // 记录老的 classLoader
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 切换现成 classLoader 为 bizClassLoader
            Thread.currentThread().setContextClassLoader(bizClassLoader);
            BizRunnable bizRunnable = new BizRunnable(runnable);
            executor.submit(bizRunnable);
            TimeUnit.SECONDS.sleep(2);
            Assert.assertTrue(recordClassLoader[0] == bizClassLoader);
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
