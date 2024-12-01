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

import org.apache.maven.model.Dependency;

import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: MatcherUtilsTest.java, v 0.1 2024年11月25日 19:52 立蓬 Exp $
 */
public class MatcherTest extends MatcherBaseTest {

    public MatcherTest() throws IOException {
    }

    /**
     * test for adaptor: koupleless-adapter-dubbo-2.7
     * matcher:
     * groupId: org.apache.dubbo
     * artifactId: dubbo-spring-boot-starter
     * versionRange: "[2.7.0,2.8.0)"
     * adapter:
     * artifactId: koupleless-adapter-dubbo-2.7
     */
    @Test
    public void testMatcher9() throws InvalidVersionSpecificationException {
        List<Dependency> res = getMatcherAdaptor(
            mockArtifact("org.apache.dubbo", "dubbo-spring-boot-starter", "2.7.0"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-dubbo-2.7");

        res = getMatcherAdaptor(
            mockArtifact("org.apache.dubbo", "dubbo-spring-boot-starter", "2.7.23"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-dubbo-2.7");

        res = getMatcherAdaptor(
            mockArtifact("org.apache.dubbo", "dubbo-spring-boot-starter", "3.0.0"));
        assertEquals(0, res.size());
    }

    /**
     * test for adaptor: koupleless-adapter-dubbo-2.7
     * matcher:
     * groupId: org.apache.dubbo
     * artifactId: dubbo
     * versionRange: "[2.7.0,2.8.0)"
     * adapter:
     * artifactId: koupleless-adapter-dubbo-2.7
     */
    @Test
    public void testMatcher10() throws InvalidVersionSpecificationException {
        List<Dependency> res = getMatcherAdaptor(
            mockArtifact("org.apache.dubbo", "dubbo", "2.7.0"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-dubbo-2.7");

        res = getMatcherAdaptor(mockArtifact("org.apache.dubbo", "dubbo", "2.7.23"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-dubbo-2.7");

        res = getMatcherAdaptor(mockArtifact("org.apache.dubbo", "dubbo", "3.0.0"));
        assertEquals(0, res.size());
    }
}