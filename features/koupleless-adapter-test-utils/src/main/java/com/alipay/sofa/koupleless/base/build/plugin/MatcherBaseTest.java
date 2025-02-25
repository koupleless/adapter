package com.alipay.sofa.koupleless.base.build.plugin;

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

import com.alipay.sofa.koupleless.base.build.plugin.model.KouplelessAdapterConfig;
import com.alipay.sofa.koupleless.base.build.plugin.model.MavenDependencyAdapterMapping;
import lombok.SneakyThrows;
import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: AdapterBaseTest.java, v 0.1 2024年11月26日 11:31 立蓬 Exp $
 */
public abstract class MatcherBaseTest {
    protected KouplelessAdapterConfig           config          = loadConfig();

    // 配置仓库地址
    private static final List<RemoteRepository> REPOSITORIES    = Arrays.asList(
        new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/")
            .build());

    // 本地仓库路径（默认 ~/.m2/repository）
    private static final String                 LOCAL_REPO_PATH = System.getProperty("user.home")
                                                                  + File.separator + ".m2"
                                                                  + File.separator + "repository";

    protected RepositorySystem                  system          = newRepositorySystem();
    protected RepositorySystemSession           session         = newSession();

    public MatcherBaseTest() throws IOException {
    }

    // 解析依赖树
    protected ArtifactResult resolveArtifacts(org.eclipse.aether.artifact.Artifact artifact) throws ArtifactResolutionException {
        ArtifactRequest artifactRequest = new ArtifactRequest().setArtifact(artifact)
            .setRepositories(REPOSITORIES);
        return system.resolveArtifact(session, artifactRequest);
    }

    protected String parseArtifactVersion(MavenDependencyAdapterMapping mapping) throws Exception {
        VersionRangeRequest versionRangeRequest = new VersionRangeRequest()
            .setArtifact(
                new DefaultArtifact(String.format("%s:%s:%s", mapping.getMatcher().getGroupId(),
                    mapping.getMatcher().getArtifactId(), mapping.getMatcher().getVersionRange())))
            .setRepositories(REPOSITORIES);

        VersionRangeResult versionRangeResult = system.resolveVersionRange(session,
            versionRangeRequest);
        Version rangeHighestVersion = versionRangeResult.getHighestVersion();
        if (rangeHighestVersion == null) {
            throw new VersionRangeResolutionException(versionRangeResult, "No version found");
        }
        return rangeHighestVersion.toString();
    }

    // 初始化仓库系统
    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    // 创建仓库会话
    private RepositorySystemSession newSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(LOCAL_REPO_PATH);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }

    protected KouplelessAdapterConfig loadConfig() {
        String MAPPING_FILE = "conf/adapter-mapping.yaml";
        InputStream mappingConfigIS = this.getClass().getClassLoader()
            .getResourceAsStream(MAPPING_FILE);
        Yaml yaml = new Yaml();
        return yaml.loadAs(mappingConfigIS, KouplelessAdapterConfig.class);
    }

    protected Artifact mockArtifact(String groupId, String artifactId, String version) {
        Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getGroupId()).thenReturn(groupId);
        Mockito.when(artifact.getArtifactId()).thenReturn(artifactId);
        Mockito.when(artifact.getVersion()).thenReturn(version);
        return artifact;
    }

    protected List<Dependency> getMatcherAdaptor(Artifact artifact) throws InvalidVersionSpecificationException {
        Map<MavenDependencyAdapterMapping, Artifact> matched = config
            .matches(Collections.singleton(artifact));

        List<Dependency> dependencies = new ArrayList<>();
        for (Map.Entry<MavenDependencyAdapterMapping, Artifact> entry : matched.entrySet()) {
            MavenDependencyAdapterMapping mapping = entry.getKey();
            dependencies.add(mapping.getAdapter());
        }
        return dependencies;
    }

    public Object invokeMethod(Class clazz, Object obj, String methodName, Class[] argClasses,
                               Object... args) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, argClasses);
            declaredMethod.setAccessible(true);
            return declaredMethod.invoke(obj, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object getFieldValue(Class clazz, Object obj, String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return declaredField.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static Map<String, byte[]> getFileContentAsLines(File file, Pattern entryPattern) {
        Map<String, byte[]> result = new HashMap<>();
        try (JarInputStream jin = new JarInputStream(new FileInputStream(file))) {

            JarEntry entry = null;
            while ((entry = jin.getNextJarEntry()) != null) {
                if (!entryPattern.matcher(entry.getName()).matches() || entry.isDirectory()) {
                    continue;
                }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead = -1;
                byte[] data = new byte[1024];
                while ((nRead = jin.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                byte[] byteArray = buffer.toByteArray();
                result.put(entry.getName(), byteArray);
            }
        }
        return result;
    }
}
