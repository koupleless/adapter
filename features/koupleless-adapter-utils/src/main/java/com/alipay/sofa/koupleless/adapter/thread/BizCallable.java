package com.alipay.sofa.koupleless.adapter.thread;

import java.util.concurrent.Callable;

/**
 * 1. 切换到目标 biz 的 classLoader
 * 2. 传递线程上下文
 */
public class BizCallable implements Callable {

    private Callable callable;
    private BizContext bizContext;
    /**
     * 记录创建 runnable 时的 classLoader，此时为 BizClassLoader
     */
    private ClassLoader classLoader;

    public BizCallable(Callable callable) {
        this.callable = callable;
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.bizContext = BizContextHolder.cloneBizContext();
    }

    @Override
    public Object call() throws Exception {
        BizContext oldContext = BizContextHolder.get();
        BizContextHolder.set(this.bizContext);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 这里要获取到目标 biz 的 classLoader，并切换到 bizClassLoader
            Thread.currentThread().setContextClassLoader(this.classLoader);
            return this.callable.call();
        } finally {
            BizContextHolder.set(oldContext);
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
