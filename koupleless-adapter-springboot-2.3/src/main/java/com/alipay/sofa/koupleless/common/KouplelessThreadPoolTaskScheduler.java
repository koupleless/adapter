/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2024 All Rights Reserved.
 */
package com.alipay.sofa.koupleless.common;

import com.alipay.sofa.koupleless.common.util.ClassUtil;
import com.alipay.sofa.koupleless.common.util.KouplelessScheduledExecutorServiceAdaptor;
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
    protected ExecutorService initializeExecutor(
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

        ScheduledExecutorService scheduledExecutor = (ScheduledExecutorService) super.initializeExecutor(threadFactory, rejectedExecutionHandler);
        ScheduledExecutorService executor = new KouplelessScheduledExecutorServiceAdaptor(scheduledExecutor);
        ClassUtil.setField("scheduledExecutor", this, executor);
        return executor;
    }
}