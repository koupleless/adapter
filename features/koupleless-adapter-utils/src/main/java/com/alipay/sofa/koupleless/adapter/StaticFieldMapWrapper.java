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
package com.alipay.sofa.koupleless.adapter;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 静态字段使用Map适配的包装类
 *
 * @author yunfei.jyf
 * @date 2024/8/2
 */
public class StaticFieldMapWrapper<T> {
    /**
     * 能力包可能并发启动，设置静态字段时可能存在多线程线程安全问题，所以使用线程安全的ConcurrentHashMap
     */
    private final ConcurrentHashMap<ClassLoader, T> classLoaderTMap = new ConcurrentHashMap<>();

    /**
     * 对于一些Map类型的static字段，很可能是在字段声明的时候就直接赋值了一个空Map，但实际每个Classloader需要对应一个不同的Map，
     * 把Map赋值这个动作后置到getOrPutDefault时也就是用这个Map的时候才创建Map并和自己的Classloader绑定
     * </p>
     * 获取default值的方法
     */
    private Supplier<T>                             getDefaultMethod;

    public StaticFieldMapWrapper() {
    }

    public StaticFieldMapWrapper(Supplier<T> getDefaultMethod) {
        this.getDefaultMethod = getDefaultMethod;
    }

    public StaticFieldMapWrapper(T t) {
        put(t);
    }

    public void put(T t) {
        if (Objects.nonNull(Thread.currentThread().getContextClassLoader()) && Objects.nonNull(t)) {
            classLoaderTMap.put(Thread.currentThread().getContextClassLoader(), t);
        }
    }

    public void put(ClassLoader classLoader, T t) {
        classLoaderTMap.put(classLoader, t);
    }

    public T get() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (Objects.isNull(contextClassLoader)) {
            return null;
        }
        T t = classLoaderTMap.get(contextClassLoader);
        if (t == null) {
            // 可能是tomcat的Classloader需要从parent取出BizClassLoader
            ClassLoader parent = contextClassLoader.getParent();
            if (parent != null) {
                t = classLoaderTMap.get(parent);
            }
        }
        return t;
    }

    public T get(ClassLoader classLoader) {
        T t = classLoaderTMap.get(classLoader);
        if (t == null) {
            // 可能是tomcat的Classloader需要从parent取出BizClassLoader
            ClassLoader parent = classLoader.getParent();
            if (parent != null) {
                t = classLoaderTMap.get(parent);
            }
        }
        return t;
    }

    /**
     * 获取
     * 获取为空时使用getDefaultMethod获取default值
     * 将default值put
     * 再次获取
     */
    public synchronized T getOrPutDefault() {
        T t = get();
        if (t == null && getDefaultMethod != null) {
            put(getDefaultMethod.get());
        }
        return get();
    }
}
