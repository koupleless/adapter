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

import com.alipay.sofa.koupleless.adapter.AdapterUtils;
import com.alipay.sofa.koupleless.base.build.plugin.MatcherBaseTest;
import org.apache.ibatis.ognl.ASTStaticField;
import org.apache.ibatis.ognl.DefaultClassResolver;
import org.apache.ibatis.ognl.OgnlContext;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.currentThread;
import static org.apache.ibatis.scripting.xmltags.OgnlCache.clearByClassLoader;
import static org.junit.Assert.assertEquals;

/**
 * @author lylingzhen@github.com
 * @version MybatisTest.java, v 0.1 2025年01月15日 上午11:39 lylingzhen
 */
public class MybatisTest extends MatcherBaseTest {

    @Before
    public void setUp() {
        currentThread().setContextClassLoader(null);
        ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Object>> expressionCache = (ConcurrentHashMap) getFieldValue(
            OgnlCache.class, null, "expressionCache");
        expressionCache.clear();
    }

    @After
    public void tearDown() {
        currentThread().setContextClassLoader(null);
    }

    public MybatisTest() throws IOException {
    }

    /**
     * test for adaptor: koupleless-adapter-mybatis
     * pattern:
     * matcher:
     * groupId: org.mybatis
     * artifactId: mybatis
     * versionRange: "[3.5.15,)"
     * adapter:
     * artifactId: koupleless-adapter-mybatis-3.5.15
     * groupId: com.alipay.sofa.koupleless
     */
    @Test
    public void testMatcher() throws InvalidVersionSpecificationException {
        List<Dependency> res = getMatcherAdaptor(mockArtifact("org.mybatis", "mybatis", "3.5.15"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-mybatis-3.5.15");

        res = getMatcherAdaptor(mockArtifact("org.mybatis", "mybatis", "3.6.1"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-mybatis-3.5.15");

        res = getMatcherAdaptor(mockArtifact("org.mybatis", "mybatis", "3.4.9"));
        assertEquals(0, res.size());
    }

    public static final String TEST_STATIC_VALUE_1 = "STATIC_VALUE_1";

    @Test
    public void testParseExpression() {

        ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Object>> expressionCache = (ConcurrentHashMap) getFieldValue(
            OgnlCache.class, null, "expressionCache");
        assertEquals(0, expressionCache.size());

        currentThread().setContextClassLoader(this.getClass().getClassLoader().getParent());
        assertEquals(ASTStaticField.class,
            invokeMethod(OgnlCache.class, null, "parseExpression", new Class[] { String.class },
                "@java.util.ConcurrentHashMap@DEFAULT_CAPACITY").getClass());

        currentThread().setContextClassLoader(null);
        assertEquals(ASTStaticField.class,
            invokeMethod(OgnlCache.class, null, "parseExpression", new Class[] { String.class },
                "@java.util.ConcurrentHashMap@DEFAULT_CAPACITY").getClass());
        assertEquals(ASTStaticField.class,
            invokeMethod(OgnlCache.class, null, "parseExpression", new Class[] { String.class },
                "@org.apache.ibatis.scripting.xmltags.OgnlCacheTest@TEST_STATIC_VALUE_1")
                    .getClass());
        assertEquals(ASTStaticField.class,
            invokeMethod(OgnlCache.class, null, "parseExpression", new Class[] { String.class },
                "@org.apache.ibatis.scripting.xmltags.OgnlCacheTest@TEST_STATIC_VALUE_1")
                    .getClass());

        assertEquals(2, expressionCache.size());
        assertEquals(1, expressionCache.get(this.getClass().getClassLoader().getParent()).size());
        assertEquals(2, expressionCache.get(this.getClass().getClassLoader()).size());
    }

    @Test
    public void testOgnlCacheClearByClassLoader() throws Exception {

        ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Object>> expressionCache = (ConcurrentHashMap) getFieldValue(
            OgnlCache.class, null, "expressionCache");
        currentThread().setContextClassLoader(this.getClass().getClassLoader().getParent());
        assertEquals(0, expressionCache.size());
        invokeMethod(OgnlCache.class, null, "parseExpression", new Class[] { String.class },
            "@java.util.ConcurrentHashMap@DEFAULT_CAPACITY");
        assertEquals(1, expressionCache.size());

        currentThread().setContextClassLoader(this.getClass().getClassLoader());
        invokeMethod(OgnlCache.class, null, "parseExpression", new Class[] { String.class },
            "@java.util.ConcurrentHashMap@DEFAULT_CAPACITY");
        assertEquals(2, expressionCache.size());

        clearByClassLoader(this.getClass().getClassLoader());
        assertEquals(1, expressionCache.size());
        assertEquals(1, expressionCache.get(this.getClass().getClassLoader().getParent()).size());
    }

    @Test
    public void testGetValue() {
        assertEquals("STATIC_VALUE_1", OgnlCache.getValue(
            "@org.apache.ibatis.scripting.xmltags.MybatisTest@TEST_STATIC_VALUE_1", null));
    }

    @Test
    public void testClassForName() throws Exception {

        DefaultClassResolver defaultClassResolver = new DefaultClassResolver();
        ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Class<?>>> classes = (ConcurrentHashMap) getFieldValue(
            DefaultClassResolver.class, defaultClassResolver, "classes");

        currentThread()
            .setContextClassLoader(DefaultClassResolver.class.getClassLoader().getParent());
        assertEquals(String.class, defaultClassResolver.classForName(String.class.getName(), null));
        assertEquals(1, classes.size());

        currentThread().setContextClassLoader(null);
        assertEquals(DefaultClassResolver.class,
            defaultClassResolver.classForName(DefaultClassResolver.class.getName(), null));
        assertEquals(2, classes.size());

        assertEquals(OgnlContext.class,
            defaultClassResolver.classForName(OgnlContext.class.getName(), null));
        assertEquals(OgnlContext.class,
            defaultClassResolver.classForName(OgnlContext.class.getName(), null));
        assertEquals(AdapterUtils.class,
            defaultClassResolver.classForName(AdapterUtils.class.getName(), null));
        assertEquals(2, classes.size());
        assertEquals(3, classes.get(DefaultClassResolver.class.getClassLoader()).size());
    }

    @Test
    public void testDefaultClassResolverClearByClassLoader() throws Exception {

        DefaultClassResolver defaultClassResolver = new DefaultClassResolver();
        ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Class<?>>> classes = (ConcurrentHashMap) getFieldValue(
            DefaultClassResolver.class, defaultClassResolver, "classes");

        currentThread().setContextClassLoader(this.getClass().getClassLoader());
        assertEquals(0, classes.size());
        assertEquals(String.class, defaultClassResolver.classForName(String.class.getName(), null));
        assertEquals(1, classes.size());

        currentThread().setContextClassLoader(this.getClass().getClassLoader().getParent());
        assertEquals(String.class, defaultClassResolver.classForName(String.class.getName(), null));
        assertEquals(2, classes.size());
        defaultClassResolver.clearByClassLoader(MybatisTest.class.getClassLoader());
        assertEquals(1, classes.size());
    }
}