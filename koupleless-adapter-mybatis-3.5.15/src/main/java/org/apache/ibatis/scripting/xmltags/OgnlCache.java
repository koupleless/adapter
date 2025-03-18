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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.ognl.Ognl;
import org.apache.ibatis.ognl.OgnlContext;
import org.apache.ibatis.ognl.OgnlException;

import org.apache.ibatis.builder.BuilderException;

import static com.alipay.sofa.koupleless.adapter.AdapterUtils.getClassLoader;

/**
 * Caches OGNL parsed expressions.
 *
 * @author Eduardo Macarron
 *
 * @see <a href='https://github.com/mybatis/old-google-code-issues/issues/342'>Issue 342</a>
 */
public final class OgnlCache {

    private static final OgnlMemberAccess                                                  MEMBER_ACCESS   = new OgnlMemberAccess();
    private static final OgnlClassResolver                                                 CLASS_RESOLVER  = new OgnlClassResolver();
    // patch begin
    private static final ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Object>> expressionCache = new ConcurrentHashMap<>();
    // patch end

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
        // patch begin
        ClassLoader classLoader = getClassLoader();
        ConcurrentHashMap<String, Object> innerMap = expressionCache.computeIfAbsent(classLoader,
            cl -> new ConcurrentHashMap<>());
        Object node = innerMap.get(expression);
        // patch end
        if (node == null) {
            node = Ognl.parseExpression(expression);
            // patch begin
            innerMap.put(expression, node);
            // patch end
        }
        return node;
    }

    // patch begin
    public static void clearByClassLoader(ClassLoader classLoader) throws Exception {
        expressionCache.remove(classLoader);
        CLASS_RESOLVER.clearByClassLoader(classLoader);
    }
    // patch end
}
