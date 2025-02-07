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
package org.apache.ibatis.ognl;

import java.util.concurrent.ConcurrentHashMap;

import static com.alipay.sofa.koupleless.adapter.AdapterUtils.findClassLoader;

/**
 * Default class resolution.  Uses Class.forName() to look up classes by name.
 * It also looks in the "java.lang" package if the class named does not give
 * a package specifier, allowing easier usage of these classes.
 */
public class DefaultClassResolver implements ClassResolver {

    // patch begin
    private final ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Class<?>>> classes = new ConcurrentHashMap<>(
        23);
    // patch end

    public DefaultClassResolver() {
        super();
    }

    public <T> Class<T> classForName(String className,
                                     OgnlContext context) throws ClassNotFoundException {
        // patch begin
        ClassLoader classLoader = findClassLoader();
        ConcurrentHashMap<String, Class<?>> innerMap = classes.computeIfAbsent(classLoader,
            cl -> new ConcurrentHashMap<>(101));
        Class<?> result = innerMap.get(className);
        // patch end
        if (result != null) {
            return (Class<T>) result;
        }
        try {
            result = toClassForName(className);
        } catch (ClassNotFoundException e) {
            if (className.indexOf('.') > -1) {
                throw e;
            }
            // The class was not in the default package.
            // Try prepending 'java.lang.'.
            try {
                result = toClassForName("java.lang." + className);
            } catch (ClassNotFoundException e2) {
                // Report the specified class name as-is.
                throw e;
            }
        }
        // patch begin
        innerMap.putIfAbsent(className, result);
        // patch end
        return (Class<T>) result;
    }

    protected Class<?> toClassForName(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    // patch begin
    public void clearByClassLoader(ClassLoader classLoader) {
        classes.remove(classLoader);
    }
    // patch end
}
