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
     * test for adaptor: koupleless-adapter-logback
     * pattern:
     *     matcher:
     *       groupId: ch.qos.logback
     *       artifactId: logback-classic
     *       versionRange: "[1.4.12,)"
     *     adapter:
     *       artifactId: koupleless-adapter-logback-core-1.4.12
     *       groupId: com.alipay.sofa.koupleless
     */
    @Test
    public void testMatcher() throws InvalidVersionSpecificationException {
        List<Dependency> res = getMatcherAdaptor(
            mockArtifact("ch.qos.logback", "logback-core", "1.4.12"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-logback-core-1.4.12");

        res = getMatcherAdaptor(mockArtifact("ch.qos.logback", "logback-core", "1.5.12"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-logback-core-1.4.12");

        res = getMatcherAdaptor(mockArtifact("ch.qos.logback", "logback-core", "1.4.11"));
        assertEquals(0, res.size());
    }
}
