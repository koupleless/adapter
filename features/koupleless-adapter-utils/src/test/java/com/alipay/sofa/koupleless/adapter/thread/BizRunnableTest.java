/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.koupleless.adapter.thread;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BizRunnableTest {

    @Test
    public void testBizRunnable() throws Exception {
        final ClassLoader[] recordClassLoaders = { null }; // 使用数组包装
        final BizContext[] bizContexts = { null }; // 使用数组包装

        Runnable runnable = () -> {
            recordClassLoaders[0] = Thread.currentThread().getContextClassLoader();
            bizContexts[0] = BizContextHolder.get();
            System.out.println(String.format("classLoader: %s; bizContext: %s",
                recordClassLoaders[0], bizContexts[0]));
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        // 第一次执行，会使用当前线程的 classLoader，也就是 appClassLoader,后续默认执行任务都会使用这个 classLoader
        executor.submit(runnable);
        TimeUnit.SECONDS.sleep(2);
        Assert.assertTrue(recordClassLoaders[0] == Thread.currentThread().getContextClassLoader());
        Assert.assertNull(bizContexts[0]);

        // 创建一个模拟 biz 的 BizclassLoader
        URL url = this.getClass().getResource("");
        ClassLoader bizClassLoader = new URLClassLoader(new URL[] { url });

        // 构造一个模块的 biz Context
        BizContext bizContext = BizContext.builder().bizName("bizName").bizVersion("bizVersion")
            .attachment(new HashMap<String, Object>() {
                {
                    put("key", "value");
                }
            }).build();

        // 记录老的 classLoader
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        BizContext oldBizContext = BizContextHolder.get();
        try {
            // 切换现成 classLoader 为 bizClassLoader
            Thread.currentThread().setContextClassLoader(bizClassLoader);
            BizContextHolder.set(bizContext);
            BizRunnable bizRunnable = new BizRunnable(runnable);
            executor.submit(bizRunnable);
            TimeUnit.SECONDS.sleep(2);
            Assert.assertTrue(recordClassLoaders[0] == bizClassLoader);
            Assert.assertNotNull(bizContexts[0]);
        } finally {
            // 将线程 classLoader 切换会老的 classLoader
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            BizContextHolder.set(oldBizContext);
        }

        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
