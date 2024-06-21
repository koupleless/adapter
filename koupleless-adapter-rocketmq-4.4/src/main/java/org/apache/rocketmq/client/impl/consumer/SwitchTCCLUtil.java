package org.apache.rocketmq.client.impl.consumer;

/**
 * @author yunfei.jyf
 * @date 2024/6/21
 */
public class SwitchTCCLUtil {
    public static void doSwitchTCCLAndRun(ClassLoader runWithClassLoader, Runnable runnable) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(runWithClassLoader);
            runnable.run();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
