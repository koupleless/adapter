package com.alipay.sofa.koupleless.adapter.thread;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

@Slf4j
public class BizThreadPoolTaskSchedulerTest {

    @Test
    public void testBizThreadPoolTaskScheduler() {
        ClassLoader initClassLoader = Thread.currentThread().getContextClassLoader();
        BizContext oldBizContext = BizContextHolder.get();

        // get the classLoader of current biz
        ClassLoader classLoader1 = new URLClassLoader(new URL[]{}, initClassLoader);
        BizThreadPoolTaskScheduler scheduler = new BizThreadPoolTaskScheduler(classLoader1);
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("test-");
        scheduler.initialize();

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
            scheduler.submit(() -> {
                System.out.println(String.format("thread %s running.", Thread.currentThread().getName()));
                Assert.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());
                Assert.assertEquals(bizContext, BizContextHolder.get());
                return null;
            }).get();
            Assert.assertEquals(bizContext, BizContextHolder.get());

            scheduler.submit(() -> {
                System.out.println(String.format("thread %s running.", Thread.currentThread().getName()));
                Assert.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());
                Assert.assertEquals(bizContext, BizContextHolder.get());
            }).get();
            Assert.assertEquals(bizContext, BizContextHolder.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Thread.currentThread().setContextClassLoader(initClassLoader);
        BizContextHolder.set(oldBizContext);
        Assert.assertEquals(oldBizContext, BizContextHolder.get());
        try {
            scheduler.submit(() -> {
                System.out.println(String.format("thread %s running.", Thread.currentThread().getName()));
                Assert.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());
                Assert.assertEquals(oldBizContext, BizContextHolder.get());
                return null;
            }).get();
            Assert.assertEquals(oldBizContext, BizContextHolder.get());

            scheduler.submit(() -> {
                System.out.println(String.format("thread %s running.", Thread.currentThread().getName()));
                Assert.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());
                Assert.assertEquals(oldBizContext, BizContextHolder.get());
            }).get();
            Assert.assertEquals(oldBizContext, BizContextHolder.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(initClassLoader);
            BizContextHolder.set(oldBizContext);
        }
    }
}
