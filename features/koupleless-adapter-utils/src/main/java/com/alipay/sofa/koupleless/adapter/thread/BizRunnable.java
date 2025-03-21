package com.alipay.sofa.koupleless.adapter.thread;

/**
 * 1. 切换到目标 biz 的 classLoader
 * 2. 传递线程上下文
 */
public class BizRunnable implements Runnable {

    private Runnable runnable;
    private BizContext bizContext;
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
