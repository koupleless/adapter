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

import edu.emory.mathcs.backport.java.util.Collections;
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
     * test for adaptor: koupleless-adapter-spring-boot-logback-2.7.14
     * pattern:
     *      matcher:
     *       groupId: org.springframework.boot
     *       artifactId: spring-boot
     *       versionRange: "[2.5.1,2.7.14]"
     *     adapter:
     *       artifactId: koupleless-adapter-spring-boot-logback-2.7.14
     */
    @Test
    public void testMatcher1() throws InvalidVersionSpecificationException {
        List<Dependency> res = getMatcherAdaptor(
            mockArtifact("org.springframework.boot", "spring-boot", "2.5.0"));
        assertEquals(0, res.size());

        res = getMatcherAdaptor(mockArtifact("org.springframework.boot", "spring-boot", "2.5.1"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-spring-boot-logback-2.7.14");

        res = getMatcherAdaptor(mockArtifact("org.springframework.boot", "spring-boot", "2.6.5"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-spring-boot-logback-2.7.14");

        res = getMatcherAdaptor(mockArtifact("org.springframework.boot", "spring-boot", "2.7.14"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-spring-boot-logback-2.7.14");

        res = getMatcherAdaptor(mockArtifact("org.springframework.boot", "spring-boot", "2.7.15"));
        assertEquals(0, res.size());
    }

}