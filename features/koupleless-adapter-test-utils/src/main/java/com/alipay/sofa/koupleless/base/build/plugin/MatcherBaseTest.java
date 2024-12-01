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
import org.apache.maven.model.Dependency;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: AdapterBaseTest.java, v 0.1 2024年11月26日 11:31 立蓬 Exp $
 */
public abstract class MatcherBaseTest {
    protected KouplelessBaseBuildPrePackageMojo               mojo            = new KouplelessBaseBuildPrePackageMojo();

    protected final Collection<MavenDependencyAdapterMapping> adapterMappings = loadAdapterMappings();

    public MatcherBaseTest() throws IOException {
    }

    private Collection<MavenDependencyAdapterMapping> loadAdapterMappings() throws IOException {
        String MAPPING_FILE = "conf/adapter-mapping.yaml";
        InputStream mappingConfigIS = this.getClass().getClassLoader()
            .getResourceAsStream(MAPPING_FILE);
        Yaml yaml = new Yaml();
        KouplelessAdapterConfig config = yaml.loadAs(mappingConfigIS,
            KouplelessAdapterConfig.class);
        return CollectionUtils.emptyIfNull(config.getAdapterMappings());
    }

    protected Artifact mockArtifact(String groupId, String artifactId, String version) {
        Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getGroupId()).thenReturn(groupId);
        Mockito.when(artifact.getArtifactId()).thenReturn(artifactId);
        Mockito.when(artifact.getVersion()).thenReturn(version);
        return artifact;
    }

    protected List<Dependency> getMatcherAdaptor(Artifact artifact) throws InvalidVersionSpecificationException {
        return mojo.getDependenciesByMatching(Collections.singleton(artifact), adapterMappings);

    }
}