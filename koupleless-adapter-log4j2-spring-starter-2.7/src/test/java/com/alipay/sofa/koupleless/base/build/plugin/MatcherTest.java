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

import com.alipay.sofa.koupleless.base.build.plugin.model.KouplelessAdapterConfig;
import com.alipay.sofa.koupleless.base.build.plugin.model.MavenDependencyAdapterMapping;
import org.apache.maven.model.Dependency;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: MatcherUtilsTest.java, v 0.1 2024年11月25日 19:52 立蓬 Exp $
 */
public class MatcherTest extends MatcherBaseTest {

    public MatcherTest() throws IOException {
    }

    /**
     * test for adaptor: koupleless-adapter-log4j2-spring-starter-2.7
     *     matcher:
     *       groupId: org.springframework.boot
     *       artifactId: spring-boot-starter-log4j2
     *       versionRange: "[2.7.0,3.0.0)"
     *     adapter:
     *       artifactId:koupleless-adapter-log4j2-spring-starter-2.7
     */
    @Test
    public void testMatcher17() throws InvalidVersionSpecificationException {
        List<Dependency> res = getMatcherAdaptor(
            mockArtifact("org.springframework.boot", "spring-boot-starter-log4j2", "2.7.0"));
        assertEquals(1, res.size());
        assertEquals(res.get(0).getArtifactId(), "koupleless-adapter-log4j2-spring-starter-2.7");

        res = getMatcherAdaptor(
            mockArtifact("org.springframework.boot", "spring-boot-starter-log4j2", "3.0.0"));
        assertEquals(0, res.size());
    }

    @Test
    public void testSourceToPatch() throws Exception {
        // 1. scan all java files
        String testRootPath = this.getClass().getClassLoader().getResource("").getFile();
        String srcRootPath = Paths.get(testRootPath, "..", "..", "src", "main", "java")
            .toAbsolutePath().toString();

        List<Path> javaFiles = Files.walk(Paths.get(srcRootPath)).filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());

        Map<String, File> javaFileMap = new java.util.HashMap<>();
        for (Path javaFile : javaFiles) {
            String javaPackageAndName = javaFile.toString().replaceFirst(srcRootPath + "/", "");
            javaFileMap.put(javaPackageAndName, javaFile.toFile());
        }

        // 2. get all source from the adapter
        List<Artifact> artifacts = parseMatcherVersion(config);
        for (Artifact artifact : artifacts) {
            File sourceJarFile = artifact.getFile();
            Map<String, byte[]> entryToContent = getFileContentAsLines(sourceJarFile,
                Pattern.compile("(.*\\.java$)"));
        }
    }

    private List<Artifact> parseMatcherVersion(KouplelessAdapterConfig config) throws Exception {
        List<Artifact> artifacts = new ArrayList<>();
        for (MavenDependencyAdapterMapping mapping : config.getAdapterMappings()) {
            String version = parseArtifactVersion(mapping);
            Artifact artifact = new DefaultArtifact(mapping.getMatcher().getGroupId(),
                mapping.getMatcher().getArtifactId(), "sources", "jar", version);

            artifacts.add(resolveArtifacts(artifact).getArtifact());
        }
        return artifacts;
    }
}
