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
package com.alipay.sofa.koupleless.base.build.plugin;

import com.alipay.sofa.koupleless.adapter.AdapterUtils;
import ognl.DefaultClassResolver;
import ognl.OgnlContext;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.currentThread;
import static org.junit.Assert.assertEquals;

/**
 * @author lylingzhen@github.com
 * @version OgnlTest.java, v 0.1 2025年01月15日 上午11:39 lylingzhen
 */
public class OgnlTest extends MatcherBaseTest {

    @Before
    public void setUp() {
        currentThread().setContextClassLoader(null);
    }

    @After
    public void tearDown() {
        currentThread().setContextClassLoader(null);
    }

    public OgnlTest() throws IOException {
    }

    /**
     * test for adaptor: koupleless-adapter-ognl
     * pattern:
     * matcher:
     * groupId: ognl
     * artifactId: ognl
     * versionRange: "[3.4.0,)"
     * adapter:
     * artifactId: koupleless-adapter-ognl-3.4.0
     * groupId: com.alipay.sofa.koupleless
     */
    @Test
    public void testMatcher() throws InvalidVersionSpecificationException {

        List<Dependency> res = getMatcherAdaptor(mockArtifact("ognl", "ognl", "3.4.0"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-ognl-3.4.0");

        res = getMatcherAdaptor(mockArtifact("ognl", "ognl", "3.5.0"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-ognl-3.4.0");

        res = getMatcherAdaptor(mockArtifact("ognl", "ognl", "3.3.0"));
        assertEquals(0, res.size());
    }

    @Test
    public void testClassForName() throws Exception {

        DefaultClassResolver defaultClassResolver = new DefaultClassResolver();
        ConcurrentHashMap<ClassLoader, Map<String, Class<?>>> classes = (ConcurrentHashMap) getFieldValue(
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
    public void testClearByClassLoader() throws Exception {

        DefaultClassResolver defaultClassResolver = new DefaultClassResolver();
        ConcurrentHashMap<ClassLoader, Map<String, Class<?>>> classes = (ConcurrentHashMap) getFieldValue(
            DefaultClassResolver.class, defaultClassResolver, "classes");

        currentThread().setContextClassLoader(this.getClass().getClassLoader());
        assertEquals(0, classes.size());
        assertEquals(String.class, defaultClassResolver.classForName(String.class.getName(), null));
        assertEquals(1, classes.size());

        currentThread().setContextClassLoader(this.getClass().getClassLoader().getParent());
        assertEquals(String.class, defaultClassResolver.classForName(String.class.getName(), null));
        assertEquals(2, classes.size());
        defaultClassResolver.clearByClassLoader(OgnlTest.class.getClassLoader());
        assertEquals(1, classes.size());
    }
}
