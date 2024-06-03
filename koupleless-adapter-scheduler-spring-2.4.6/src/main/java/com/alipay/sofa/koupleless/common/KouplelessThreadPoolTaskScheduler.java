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
package com.alipay.sofa.koupleless.common;

import com.alipay.sofa.koupleless.common.util.ClassUtil;
import com.alipay.sofa.koupleless.plugin.concurrent.KouplelessScheduledExecutorService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: KouplelessThreadPoolTaskScheduler.java, v 0.1 2024年05月13日 23:33 立蓬 Exp $
 */
public class KouplelessThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {

    @Override
    protected ExecutorService initializeExecutor(ThreadFactory threadFactory,
                                                 RejectedExecutionHandler rejectedExecutionHandler) {

        ScheduledExecutorService scheduledExecutor = (ScheduledExecutorService) super.initializeExecutor(
            threadFactory, rejectedExecutionHandler);
        ScheduledExecutorService executor = new KouplelessScheduledExecutorService(
            scheduledExecutor);
        ClassUtil.setField("scheduledExecutor", this, executor);
        return executor;
    }
}