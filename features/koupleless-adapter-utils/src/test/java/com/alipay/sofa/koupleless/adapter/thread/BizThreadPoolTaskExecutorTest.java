package com.alipay.sofa.koupleless.adapter.thread;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

public class BizThreadPoolTaskExecutorTest {

    @Test
    public void testBizThreadPoolTaskExecutor() {
        ClassLoader initClassLoader = Thread.currentThread().getContextClassLoader();
        BizContext oldBizContext = BizContextHolder.get();

        // get the classLoader of current biz
        ClassLoader classLoader1 = new URLClassLoader(new URL[]{}, initClassLoader);
        BizThreadPoolTaskExecutor executor = new BizThreadPoolTaskExecutor(classLoader1);
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("test-");
        executor.initialize();


        Assert.assertNull(BizContextHolder.get());

        BizContext bizContext = BizContext.builder().bizName("bizName").bizVersion("bizVersion")
                .attachment(new HashMap<String, Object>() {
                    {
                        put("key", "value");
                    }
                }).build();
        BizContextHolder.set(bizContext);
        Assert.assertEquals(bizContext, BizContextHolder.get());

        // 在非线程池任务里执行获得到的 classLoader 是当前线程的 classLoader
        Assert.assertEquals(initClassLoader, Thread.currentThread().getContextClassLoader());

        // 在线程池任务里执行获得到的 classLoader 是当前 classLoader1，线程上下文是随 BizContextHolder 的变化而变化
        // 1. classLoader
        // 2. 传递上下文
        BizContextHolder.set(bizContext);
        try {
            executor.submit(() -> {
                Assert.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());
                Assert.assertEquals(bizContext, BizContextHolder.get());
                return null;
            });

            executor.submit(() -> {
                Assert.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());
                Assert.assertEquals(bizContext, BizContextHolder.get());
            }).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Thread.currentThread().setContextClassLoader(initClassLoader);
        BizContextHolder.set(oldBizContext);
        try {
            executor.submit(() -> {
                Assert.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());
                Assert.assertEquals(oldBizContext, BizContextHolder.get());
                return null;
            });

            executor.submit(() -> {
                Assert.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());
                Assert.assertEquals(oldBizContext, BizContextHolder.get());
            }).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(initClassLoader);
            BizContextHolder.set(oldBizContext);
        }
    }
}
