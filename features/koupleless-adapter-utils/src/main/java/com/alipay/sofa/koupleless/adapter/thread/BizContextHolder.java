package com.alipay.sofa.koupleless.adapter.thread;

public class BizContextHolder {
    private static final ThreadLocal<BizContext> BIZ_CONTEXT = new ThreadLocal<>();

    public static void set(BizContext context) {
        BIZ_CONTEXT.set(context);
    }

    public static void clear() {
        BIZ_CONTEXT.remove();
    }

    public static BizContext get() {
        return BIZ_CONTEXT.get();
    }

    public static BizContext cloneBizContext() {
        BizContext context = get();
        if (context == null) {
            return null;
        }
        return context.cloneInstance();
    }
}
