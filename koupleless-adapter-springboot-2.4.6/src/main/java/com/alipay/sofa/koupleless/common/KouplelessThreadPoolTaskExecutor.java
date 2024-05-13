package com.alipay.sofa.koupleless.common;


import com.alipay.sofa.koupleless.common.util.ClassUtil;
import com.alipay.sofa.koupleless.common.util.KouplelessExecutorServiceAdaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: KouplelessThreadPoolTaskExecutor.java, v 0.1 2024年05月13日 20:51 立蓬 Exp $
 */
public class KouplelessThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    @Override
    protected ExecutorService initializeExecutor(
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

        ExecutorService executorService = super.initializeExecutor(threadFactory, rejectedExecutionHandler);
        KouplelessExecutorServiceAdaptor executor = new KouplelessExecutorServiceAdaptor(executorService);
        ClassUtil.setField("threadPoolExecutor", this, executor);
        return executor;
    }
}