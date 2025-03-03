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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.junit.Assert;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: AdapterBaseTest.java, v 0.1 2024年11月26日 11:31 立蓬 Exp $
 */
public abstract class MatcherBaseTest {
    protected KouplelessAdapterConfig           config          = loadConfig();

    // 配置仓库地址
    private static final List<RemoteRepository> REPOSITORIES    = Arrays.asList(
        new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/")
            .build(),
        new RemoteRepository.Builder("aliyun", "default",
            "https://maven.aliyun.com/repository/public/").build());

    // 本地仓库路径（默认 ~/.m2/repository）
    private static final String                 LOCAL_REPO_PATH = System.getProperty("user.home")
                                                                  + File.separator + ".m2"
                                                                  + File.separator + "repository";

    protected RepositorySystem                  system          = newRepositorySystem();
    protected RepositorySystemSession           session         = newSession();

    public MatcherBaseTest() throws IOException {
    }

    protected void checkInvalidSources() throws Exception {
        // 1. scan all java files from this adapter
        String testRootPath = this.getClass().getClassLoader().getResource("").getFile();
        String srcRootPath = Paths.get(testRootPath, "..", "..", "src", "main", "java")
            .toAbsolutePath().toString();
        List<AdapterPatch> adapterPatchs = convertAllJavaFiles(srcRootPath);

        // 2. get all source from the maven central
        List<org.eclipse.aether.artifact.Artifact> artifacts = downloadAllSources(config);
        Assert.assertTrue("sources not found", CollectionUtils.isNotEmpty(artifacts));

        for (AdapterPatch adapterPatch : adapterPatchs) {
            String javaPackageAndName = adapterPatch.getSubPath() + "/"
                                        + adapterPatch.getFileName();
            for (org.eclipse.aether.artifact.Artifact artifact : artifacts) {
                File sourceJarFile = artifact.getFile();
                Map<String, byte[]> entryToContent = getFileContentAsLines(sourceJarFile,
                    Pattern.compile("(.*\\.java$)"));
                if (entryToContent.containsKey(javaPackageAndName)) {
                    byte[] sourceContent = entryToContent.get(javaPackageAndName);
                    List<String> sourceLines = edu.emory.mathcs.backport.java.util.Arrays
                        .asList(new String(sourceContent).split("\\r?\\n"));
                    adapterPatch.setSourceLines(sourceLines);
                    break;
                }
            }
        }

        Path gitRoot = Files.createTempDirectory("koupleless-adapter");
        Git git = Git.init().setDirectory(gitRoot.toFile()).call();
        // 3. generate the patch files, 必须串行执行
        for (AdapterPatch adapterPatch : adapterPatchs) {
            List<String> adaptedLines = adapterPatch.getAdaptedLines();
            List<String> sourceLines = adapterPatch.getSourceLines();

            if (CollectionUtils.isEmpty(sourceLines) || CollectionUtils.isEmpty(adaptedLines)) {
                continue;
            }
            List<String> patchContents = generateGitPatch(git, adapterPatch);
            adapterPatch.setPatchLines(patchContents);
        }

        // 4. check the patch files works for the source files in all versions
        for (AdapterPatch adapterPatch : adapterPatchs) {
            String javaPackageAndName = adapterPatch.getSubPath() + "/"
                                        + adapterPatch.getFileName();

            List<String> invalidSources = new ArrayList<>();
            for (org.eclipse.aether.artifact.Artifact artifact : artifacts) {
                File sourceJarFile = artifact.getFile();
                Map<String, byte[]> entryToContent = getFileContentAsLines(sourceJarFile,
                    Pattern.compile("(.*\\.java$)"));
                if (entryToContent.containsKey(javaPackageAndName)) {
                    byte[] sourceContent = entryToContent.get(javaPackageAndName);
                    List<String> sourceLines = Arrays
                        .asList(new String(sourceContent).split("\\r?\\n"));
                    adapterPatch.setSourceLines(sourceLines);
                    try {
                        checkGitPathValid(git, adapterPatch);
                    } catch (GitAPIException e) {
                        invalidSources.add(String.format("%s:%s:%s:%s: patch failed.",
                            artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                            adapterPatch.getFileName()));
                    } catch (RuntimeException e) {
                        invalidSources
                            .add(String.format("%s:%s:%s:%s: contains unexpected diffs %s.",
                                artifact.getGroupId(), artifact.getArtifactId(),
                                artifact.getVersion(), adapterPatch.getFileName(), e.getMessage()));
                    } finally {
                        git.reset()
                            .addPath(adapterPatch.getSubPath() + "/" + adapterPatch.getFileName())
                            .call();
                        git.checkout()
                            .addPath(adapterPatch.getSubPath() + "/" + adapterPatch.getFileName())
                            .call();
                    }
                }
            }

            Assert.assertTrue(
                String.format("this adapter invalid for \n%s,\n please create a new adapter.",
                    String.join("\n", invalidSources)),
                invalidSources.isEmpty());
        }
    }

    private List<AdapterPatch> convertAllJavaFiles(String srcRootPath) throws Exception {
        List<Path> javaFiles = Files.walk(Paths.get(srcRootPath)).filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());

        // get all adapted sources from maven
        List<AdapterPatch> adapterPatchs = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            AdapterPatch adapterPatch = new AdapterPatch();
            adapterPatch.setFileName(javaFile.getFileName().toString());
            adapterPatch
                .setSubPath(javaFile.getParent().toString().replaceFirst(srcRootPath + "/", ""));
            adapterPatch.setRootPath(srcRootPath);
            adapterPatch.setAdaptedLines(Files.readAllLines(javaFile));
            adapterPatchs.add(adapterPatch);
        }
        return adapterPatchs;
    }

    /**
     * 检查 git apply 是否有效，先拷贝 source 源码文件，然后应用 patch，然后检查是否有 diff
     * @param git
     * @param adapterPatch
     * @throws Exception
     */
    private void checkGitPathValid(Git git, AdapterPatch adapterPatch) throws Exception {
        Path gitRoot = git.getRepository().getDirectory().getParentFile().toPath();
        List<String> patchLines = adapterPatch.getPatchLines();
        Path filePath = Paths.get(gitRoot.toString(), adapterPatch.getSubPath(),
            adapterPatch.getFileName());
        Files.write(filePath, adapterPatch.getSourceLines());
        ByteArrayInputStream patchStream = new ByteArrayInputStream(
            patchLines.stream().collect(Collectors.joining("\n")).getBytes());
        git.apply().setPatch(patchStream).call();
        git.reset().addPath(adapterPatch.getSubPath() + "/" + adapterPatch.getFileName()).call();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DiffFormatter formatter = new DiffFormatter(outputStream);
        formatter.setRepository(git.getRepository());
        formatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);

        // 获取 work 空间的内容
        FileTreeIterator workTreeIterator = new FileTreeIterator(git.getRepository());
        // 获取 head 的内容
        CanonicalTreeParser headTreeParser = new CanonicalTreeParser(null,
            git.getRepository().newObjectReader(), git.getRepository().resolve("HEAD^{tree}"));
        List<DiffEntry> diffEntries = formatter.scan(headTreeParser, workTreeIterator);
        for (DiffEntry diffEntry : diffEntries) {
            formatter.format(diffEntry);
            String diffContent = outputStream.toString();
            List<String> diffLines = Arrays.asList(diffContent.split("\\r?\\n"));
            outputStream.reset();
            if (diffLines.size() > 4) {
                throw new RuntimeException("contains unexpected diffs: " + diffContent);
            }
        }
    }

    private List<String> generateGitPatch(Git git, AdapterPatch adapterPatch) throws Exception {
        // 1. 打开 /tmp 目录里临时目录
        // 2. 拷贝 source 进去，然后 git init
        Path gitRoot = git.getRepository().getDirectory().getParentFile().toPath();
        Path sourcePath = Paths.get(gitRoot.toString(), adapterPatch.getSubPath());
        Files.createDirectories(sourcePath);
        Files.write(sourcePath.resolve(adapterPatch.getFileName()), adapterPatch.getSourceLines());
        git.add().addFilepattern(".").call();
        git.commit().setMessage("add source").call();
        Files.write(sourcePath.resolve(adapterPatch.getFileName()), adapterPatch.getAdaptedLines());
        git.add().addFilepattern(".").call();
        git.commit().setMessage("add patched source").call();

        Iterator<RevCommit> commits = git.log().call().iterator();
        RevCommit newCommit = commits.next();
        RevCommit oldCommit = commits.next();

        ByteArrayOutputStream diffOutputStream = new ByteArrayOutputStream();
        DiffFormatter formatter = new DiffFormatter(diffOutputStream);
        formatter.setRepository(git.getRepository());
        formatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
        List<DiffEntry> diffEntries = formatter.scan(oldCommit.getTree(), newCommit.getTree());
        for (DiffEntry diffEntry : diffEntries) {
            formatter.format(diffEntry);
        }

        Path targetPath = Paths.get(adapterPatch.getRootPath(), "../resources",
            adapterPatch.getSubPath(), adapterPatch.getFileName() + ".patch");
        Files.createDirectories(targetPath.getParent());
        byte[] bytePatchContent = diffOutputStream.toByteArray();
        List<String> patchContent = edu.emory.mathcs.backport.java.util.Arrays
            .asList(new String(bytePatchContent).split("\\r?\\n"));
        Files.write(targetPath, bytePatchContent);
        return patchContent;
    }

    private List<org.eclipse.aether.artifact.Artifact> downloadAllSources(KouplelessAdapterConfig config) throws Exception {
        List<org.eclipse.aether.artifact.Artifact> artifacts = new ArrayList<>();
        for (MavenDependencyAdapterMapping mapping : config.getAdapterMappings()) {
            List<String> versions = parseArtifactVersion(mapping);
            for (String version : versions) {
                org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(
                    mapping.getMatcher().getGroupId(), mapping.getMatcher().getArtifactId(),
                    "sources", "jar", version);

                artifacts.add(resolveArtifacts(artifact).getArtifact());
            }
        }
        return artifacts;
    }

    // 解析依赖树
    protected ArtifactResult resolveArtifacts(org.eclipse.aether.artifact.Artifact artifact) throws ArtifactResolutionException {
        ArtifactRequest artifactRequest = new ArtifactRequest().setArtifact(artifact)
            .setRepositories(REPOSITORIES);
        return system.resolveArtifact(session, artifactRequest);
    }

    protected List<String> parseArtifactVersion(MavenDependencyAdapterMapping mapping) throws Exception {
        VersionRangeRequest versionRangeRequest = new VersionRangeRequest()
            .setArtifact(
                new DefaultArtifact(String.format("%s:%s:%s", mapping.getMatcher().getGroupId(),
                    mapping.getMatcher().getArtifactId(), mapping.getMatcher().getVersionRange())))
            .setRepositories(REPOSITORIES);

        VersionRangeResult versionRangeResult = system.resolveVersionRange(session,
            versionRangeRequest);
        return versionRangeResult.getVersions().stream().map(Version::toString)
            .collect(Collectors.toList());
    }

    // 初始化仓库系统
    private RepositorySystem newRepositorySystem() {
        RepositorySystemSupplier supplier = new RepositorySystemSupplier();
        return supplier.get();
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
        Assert.assertNotNull(String.format("config %s not found.", MAPPING_FILE), mappingConfigIS);
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
