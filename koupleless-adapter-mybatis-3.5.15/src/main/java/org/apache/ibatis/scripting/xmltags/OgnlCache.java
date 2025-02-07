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
package org.apache.ibatis.scripting.xmltags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.ognl.Ognl;
import org.apache.ibatis.ognl.OgnlContext;
import org.apache.ibatis.ognl.OgnlException;

import org.apache.ibatis.builder.BuilderException;

import com.alipay.sofa.koupleless.adapter.AdapterUtils;

/**
 * Caches OGNL parsed expressions.
 *
 * @author Eduardo Macarron
 *
 * @see <a href='https://github.com/mybatis/old-google-code-issues/issues/342'>Issue 342</a>
 */
public final class OgnlCache {

    private static final OgnlMemberAccess                                    MEMBER_ACCESS   = new OgnlMemberAccess();
    private static final OgnlClassResolver                                   CLASS_RESOLVER  = new OgnlClassResolver();
    private static final Map<ClassLoader, ConcurrentHashMap<String, Object>> expressionCache = new ConcurrentHashMap<>();

    private OgnlCache() {
        // Prevent Instantiation of Static Class
    }

    public static Object getValue(String expression, Object root) {
        try {
            OgnlContext context = Ognl.createDefaultContext(root, MEMBER_ACCESS, CLASS_RESOLVER,
                null);
            return Ognl.getValue(parseExpression(expression), context, root);
        } catch (OgnlException e) {
            throw new BuilderException(
                "Error evaluating expression '" + expression + "'. Cause: " + e, e);
        }
    }

    private static Object parseExpression(String expression) throws OgnlException {
        ClassLoader classLoader = AdapterUtils.findClassLoader();
        ConcurrentHashMap<String, Object> innerMap = expressionCache.get(classLoader);
        Object node = null;
        if (innerMap == null) {
            innerMap = new ConcurrentHashMap<>();
            expressionCache.putIfAbsent(classLoader, innerMap);
        } else {
            node = innerMap.get(expression);
        }
        if (node == null) {
            node = Ognl.parseExpression(expression);
            innerMap.put(expression, node);
        }
        return node;
    }

    public static void clearByClassLoader(ClassLoader classLoader) throws Exception {
        expressionCache.remove(classLoader);
        CLASS_RESOLVER.clearByClassLoader(classLoader);
    }
}