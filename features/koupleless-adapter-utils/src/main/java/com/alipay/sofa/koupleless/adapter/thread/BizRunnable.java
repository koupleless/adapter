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

/**
 * 1. 切换到目标 biz 的 classLoader
 * 2. 传递线程上下文
 */
public class BizRunnable implements Runnable {

    private Runnable    runnable;
    private BizContext  bizContext;
    /**
     * 记录创建 runnable 时的 classLoader，此时为 BizClassLoader
     */
    private ClassLoader classLoader;

    public BizRunnable(Runnable runnable) {
        this.runnable = runnable;
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.bizContext = BizContextHolder.cloneBizContext();
    }

    @Override
    public void run() {
        BizContext oldContext = BizContextHolder.get();
        BizContextHolder.set(this.bizContext);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 这里要获取到目标 biz 的 classLoader，并切换到 bizClassLoader
            Thread.currentThread().setContextClassLoader(this.classLoader);
            runnable.run();
        } finally {
            BizContextHolder.set(oldContext);
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
