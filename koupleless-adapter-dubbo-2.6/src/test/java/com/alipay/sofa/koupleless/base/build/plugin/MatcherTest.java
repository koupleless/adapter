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
     * test for adaptor: koupleless-adapter-dubbo-2.6
     *     matcher:
     *       groupId: com.alibaba
     *       artifactId: dubbo-dependencies-bom
     *       versionRange: "[2.6.1,2.7.0)"
     *     adapter:
     *       artifactId: koupleless-adapter-dubbo-2.6
     */
    @Test
    public void testMatcher7() throws InvalidVersionSpecificationException {
        List<Dependency> res = getMatcherAdaptor(
            mockArtifact("com.alibaba", "dubbo-dependencies-bom", "2.6.1"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-dubbo-2.6");

        res = getMatcherAdaptor(mockArtifact("com.alibaba", "dubbo-dependencies-bom", "2.6.12"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-dubbo-2.6");

        res = getMatcherAdaptor(mockArtifact("com.alibaba", "dubbo-dependencies-bom", "2.7.0"));
        assertEquals(0, res.size());
    }

    /**
     * test for adaptor: koupleless-adapter-dubbo-2.6
     *     matcher:
     *       groupId: com.alibaba
     *       artifactId: dubbo
     *       versionRange: "[2.6.0,2.7.0)"
     *     adapter:
     *       artifactId: koupleless-adapter-dubbo-2.6
     */
    @Test
    public void testMatcher8() throws InvalidVersionSpecificationException {
        List<Dependency> res = getMatcherAdaptor(mockArtifact("com.alibaba", "dubbo", "2.6.0"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-dubbo-2.6");

        res = getMatcherAdaptor(mockArtifact("com.alibaba", "dubbo-dependencies-bom", "2.6.12"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-dubbo-2.6");

        res = getMatcherAdaptor(mockArtifact("com.alibaba", "dubbo-dependencies-bom", "2.8.4"));
        assertEquals(0, res.size());
    }

}