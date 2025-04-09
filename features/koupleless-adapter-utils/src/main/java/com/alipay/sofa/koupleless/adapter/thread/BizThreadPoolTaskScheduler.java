package com.alipay.sofa.koupleless.adapter.thread;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 *
 * 具备绑定 bizClassLoader、传递线程上下文、自动关闭功能的线程池
 * @author youji.zzl youji.zzl@antfin.com
 * @since 2025/4/8 20:37
 *
 */

public class BizThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {

    private ClassLoader classLoader;

    public BizThreadPoolTaskScheduler(ClassLoader classLoader) {
        super();
        this.classLoader = classLoader;
    }

    @Override
    protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory,
                                                      RejectedExecutionHandler rejectedExecutionHandler) {
        ScheduledExecutorService executorService = super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
        ScheduledThreadPoolExecutor threadPoolExecutor = (ScheduledThreadPoolExecutor) executorService;
        return new BizScheduledThreadPoolExecutor(poolSize, classLoader, threadPoolExecutor);
    }

    class BizScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

        private final ScheduledThreadPoolExecutor delegate;
        private final ClassLoader classLoader;

        private Method decorateRunnableTask;
        private Method decorateCallableTask;

        public BizScheduledThreadPoolExecutor(int corePoolSize, ClassLoader classLoader,
                                              ScheduledThreadPoolExecutor delegate) {
            super(corePoolSize);
            this.delegate = delegate;
            this.classLoader = classLoader;
            try {
                this.decorateRunnableTask = ScheduledThreadPoolExecutor.class.getDeclaredMethod(
                        "decorateTask", Runnable.class, RunnableScheduledFuture.class);
                this.decorateRunnableTask.setAccessible(true);
                this.decorateCallableTask = ScheduledThreadPoolExecutor.class.getDeclaredMethod(
                        "decorateTask", Callable.class, RunnableScheduledFuture.class);
                this.decorateCallableTask.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable,
                                                              RunnableScheduledFuture<V> task) {
            try {
                return (RunnableScheduledFuture<V>) this.decorateCallableTask.invoke(this.delegate, new BizRunnable(runnable, this.classLoader), task);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable,
                                                              RunnableScheduledFuture<V> task) {
            try {
                return (RunnableScheduledFuture<V>) this.decorateRunnableTask.invoke(this.delegate, new BizCallable(callable, this.classLoader), task);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
