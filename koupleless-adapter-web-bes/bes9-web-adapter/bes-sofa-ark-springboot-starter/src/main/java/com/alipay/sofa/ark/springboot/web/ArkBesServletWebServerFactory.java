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
package com.alipay.sofa.ark.springboot.web;

import com.alipay.sofa.ark.common.util.AssertUtils;
import com.alipay.sofa.ark.spi.model.Biz;
import com.alipay.sofa.ark.spi.service.ArkInject;
import com.alipay.sofa.ark.spi.service.biz.BizManagerService;
import com.alipay.sofa.ark.spi.web.EmbeddedServerService;
import com.bes.enterprise.springboot.embedded.BesServletWebServerFactory;
import com.bes.enterprise.web.BESConnector;
import com.bes.enterprise.web.Embedded;
import com.bes.enterprise.web.util.scan.StandardJarScanFilter;
import com.bes.enterprise.webtier.*;
import com.bes.enterprise.webtier.connector.Connector;
import com.bes.enterprise.webtier.core.DefaultContext;
import com.bes.enterprise.webtier.loader.WebappLoader;
import com.bes.enterprise.webtier.webresources.StandardRoot;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContainerInitializer;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.alipay.sofa.ark.spi.constant.Constants.ROOT_WEB_CONTEXT_PATH;

/**
 * @author Phillip Webb
 * @author Dave Syer
 * @author Brock Mills
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoffer Sawicki
 * @author qilong.zql
 * @since 0.6.0
 */
public class ArkBesServletWebServerFactory extends BesServletWebServerFactory {

    private static final Charset            DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @ArkInject
    private EmbeddedServerService<Embedded> embeddedServerService;

    @ArkInject
    private BizManagerService               bizManagerService;

    private File                            baseDirectory;

    private String                          protocol        = DEFAULT_PROTOCOL;

    private int                             backgroundProcessorDelay;

    private final ApplicationPid pid;

    public ArkBesServletWebServerFactory() {
        super();
        this.pid = new ApplicationPid();
    }

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        if (embeddedServerService == null && ArkClient.getInjectionService() != null) {
            // 非应用上下文 (例如: Spring Management Context) 没有经历 Start 生命周期, 不会被注入 ArkServiceInjectProcessor,
            // 因此 @ArkInject 没有被处理, 需要手动处理
            ArkClient.getInjectionService().inject(this);
        }
        if (embeddedServerService == null) {
            return super.getWebServer(initializers);
        } else if (embeddedServerService.getEmbedServer(getPort()) == null) {
            embeddedServerService.putEmbedServer(getPort(), initEmbedServer());
        }
        Embedded embedded = (Embedded) embeddedServerService.getEmbedServer(getPort());
        prepareContext(embedded.getHost(), initializers);
        return getWebServer(embedded);
    }

    @Override
    public String getContextPath() {
        String contextPath = super.getContextPath();
        if (bizManagerService == null) {
            return contextPath;
        }
        Biz biz = bizManagerService.getBizByClassLoader(Thread.currentThread()
                .getContextClassLoader());
        if (!StringUtils.isEmpty(contextPath)) {
            return contextPath;
        } else if (biz != null) {
            if (StringUtils.isEmpty(biz.getWebContextPath())) {
                return ROOT_WEB_CONTEXT_PATH;
            }
            return biz.getWebContextPath();
        } else {
            return ROOT_WEB_CONTEXT_PATH;
        }
    }

    private Embedded initEmbedServer() {
        Embedded embedded = new Embedded();
        File baseDir = this.initBaseDir();
        embedded.setBaseDir(baseDir.getAbsolutePath());
        customizeConnector(embedded);
        this.configureEngine(embedded.getEngine());
        embedded.getHost().setAutoDeploy(false);
        for (Connector additionalConnector : getAdditionalBesConnectors()) {
            embedded.getService().addConnector(additionalConnector);
        }
        this.customizeVirtualServer(embedded.getHost());
        return embedded;
    }

    private File initBaseDir() {
        File baseDir = null;
        String installRoot = System.getProperty("com.bes.installRoot");
        if (installRoot != null) {
            baseDir = new File(installRoot);
        }

        if (baseDir == null && this.baseDirectory != null) {
            baseDir = this.baseDirectory;
        }

        if (baseDir == null) {
            baseDir = this.getDefaultBaseDirectory();
            File licenseDir = new File(baseDir, "license");
            if (!licenseDir.exists() && !licenseDir.mkdir()) {
                throw new IllegalStateException(String.format(
                        "Failed to create license directory %s.", licenseDir.getAbsolutePath()));
            }
        }

        System.setProperty("com.bes.installRoot", baseDir.getAbsolutePath());
        System.setProperty("com.bes.instanceRoot", baseDir.getAbsolutePath());
        return baseDir;
    }

    private File getDefaultBaseDirectory() {
        String userDir = System.getProperty("user.dir");
        String fileName = "bes." + this.getPort() + "." + this.pid;
        File baseDir;
        if (StringUtils.hasLength(userDir)) {
            baseDir = new File(userDir + File.separator + "appserver", fileName);
        } else {
            ApplicationHome applicationHome = new ApplicationHome(this.getClass());
            baseDir = new File(applicationHome.getDir() + File.separator + "appserver", fileName);
        }

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IllegalStateException(String.format("Failed to create base directory %s.",
                    baseDir.getAbsolutePath()));
        } else {
            return baseDir;
        }
    }

    @Override
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    /**
     * The Tomcat protocol to use when create the {@link Connector}.
     * @param protocol the protocol
     * @see Connector#Connector(String)
     */
    @Override
    public void setProtocol(String protocol) {
        AssertUtils.isFalse(StringUtils.isEmpty(protocol), "Protocol must not be empty");
        this.protocol = protocol;
    }

    @Override
    public void setBackgroundProcessorDelay(int delay) {
        this.backgroundProcessorDelay = delay;
    }

    private void configureEngine(Engine engine) {
        engine.setBackgroundProcessorDelay(this.backgroundProcessorDelay);
        for (Valve valve : getEngineValves()) {
            engine.getPipeline().addValve(valve);
        }
    }

    @Override
    protected void postProcessContext(Context context) {
        ((WebappLoader) context.getLoader())
                .setLoaderClass("com.alipay.sofa.ark.web.embed.bes.ArkBesEmbeddedWebappClassLoader");
    }

    @Override
    protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
        if (host.getState() == LifecycleState.NEW) {
            super.prepareContext(host, initializers);
        } else {
            File documentRoot = getValidDocumentRoot();
            DefaultContext context = new DefaultContext();
            if (documentRoot != null) {
                context.setResources(new StandardRoot(context));
            }
            context.setName(getContextPath());
            context.setDisplayName(getDisplayName());
            context.setPath(getContextPath());
            File docBase = (documentRoot != null) ? documentRoot : createTempDir("bes-docbase");
            context.setDocBase(docBase.getAbsolutePath());
            context.addLifecycleListener(new Embedded.FixContextListener());
            context.setParentClassLoader(Thread.currentThread().getContextClassLoader());
            resetDefaultLocaleMapping(context);
            addLocaleMappings(context);
            context.setUseRelativeRedirects(false);
            configureTldSkipPatterns(context);
            WebappLoader loader = new WebappLoader();
            loader
                    .setLoaderClass("com.alipay.sofa.ark.web.embed.bes.ArkBesEmbeddedWebappClassLoader");
            loader.setDelegate(true);
            context.setLoader(loader);
            if (isRegisterDefaultServlet()) {
                addDefaultServlet(context);
            }
            if (shouldRegisterJspServlet()) {
                addJspServlet(context);
                addJasperInitializer(context);
            }
            context.addLifecycleListener(new StaticResourceConfigurer(context));
            ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
            context.setParent(host);
            configureContext(context, initializersToUse);
            host.addChild(context);
        }
    }

    /**
     * Override Tomcat's default locale mappings to align with other servers. See
     * {@code org.apache.catalina.util.CharsetMapperDefault.properties}.
     * @param context the context to reset
     */
    private void resetDefaultLocaleMapping(DefaultContext context) {
        context.addLocaleEncodingMappingParameter(Locale.ENGLISH.toString(),
                DEFAULT_CHARSET.displayName());
        context.addLocaleEncodingMappingParameter(Locale.FRENCH.toString(),
                DEFAULT_CHARSET.displayName());
    }

    private void addLocaleMappings(DefaultContext context) {
        for (Map.Entry<Locale, Charset> entry : getLocaleCharsetMappings().entrySet()) {
            context.addLocaleEncodingMappingParameter(entry.getKey().toString(), entry.getValue()
                    .toString());
        }
    }

    private void configureTldSkipPatterns(DefaultContext context) {
        StandardJarScanFilter filter = new StandardJarScanFilter();
        filter.setTldSkip(StringUtils.collectionToCommaDelimitedString(getTldSkipPatterns()));
        context.getJarScanner().setJarScanFilter(filter);
    }

    private void addDefaultServlet(Context context) {
        Wrapper defaultServlet = context.createWrapper();
        defaultServlet.setName("default");
        defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
        defaultServlet.addInitParameter("debug", "0");
        defaultServlet.addInitParameter("listings", "false");
        defaultServlet.setLoadOnStartup(1);
        // Otherwise the default location of a Spring DispatcherServlet cannot be set
        defaultServlet.setOverridable(true);
        context.addChild(defaultServlet);
        context.addServletMappingDecoded("/", "default");
    }

    private void addJspServlet(Context context) {
        Wrapper jspServlet = context.createWrapper();
        jspServlet.setName("jsp");
        jspServlet.setServletClass(getJsp().getClassName());
        jspServlet.addInitParameter("fork", "false");
        for (Map.Entry<String, String> entry : getJsp().getInitParameters().entrySet()) {
            jspServlet.addInitParameter(entry.getKey(), entry.getValue());
        }
        jspServlet.setLoadOnStartup(3);
        context.addChild(jspServlet);
        context.addServletMappingDecoded("*.jsp", "jsp");
        context.addServletMappingDecoded("*.jspx", "jsp");
    }

    private void addJasperInitializer(DefaultContext context) {
        try {
            ServletContainerInitializer initializer = (ServletContainerInitializer) ClassUtils
                    .forName("org.apache.jasper.servlet.JasperInitializer", null).newInstance();
            context.addServletContainerInitializer(initializer, null);
        } catch (Exception ex) {
            // Probably not Tomcat 8
        }
    }

    final class StaticResourceConfigurer implements LifecycleListener {

        private final Context context;

        private StaticResourceConfigurer(Context context) {
            this.context = context;
        }

        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                addResourceJars(getUrlsOfJarsWithMetaInfResources());
            }
        }

        private void addResourceJars(List<URL> resourceJarUrls) {
            for (URL url : resourceJarUrls) {
                String path = url.getPath();
                if (path.endsWith(".jar") || path.endsWith(".jar!/")) {
                    String jar = url.toString();
                    if (!jar.startsWith("jar:")) {
                        // A jar file in the file system. Convert to Jar URL.
                        jar = "jar:" + jar + "!/";
                    }
                    addResourceSet(jar);
                } else {
                    addResourceSet(url.toString());
                }
            }
        }

        private void addResourceSet(String resource) {
            try {
                if (isInsideNestedJar(resource)) {
                    // It's a nested jar but we now don't want the suffix because Tomcat
                    // is going to try and locate it as a root URL (not the resource
                    // inside it)
                    resource = resource.substring(0, resource.length() - 2);
                }
                URL url = new URL(resource);
                String path = "/META-INF/resources";
                this.context.getResources().createWebResourceSet(
                        WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", url, path);
            } catch (Exception ex) {
                // Ignore (probably not a directory)
            }
        }

        private boolean isInsideNestedJar(String dir) {
            return dir.indexOf("!/") < dir.lastIndexOf("!/");
        }
    }

    /**
     * additional processing to the Tomcat server.
     */
    protected WebServer getWebServer(Embedded embedded) {
        return new ArkBesWebServer(embedded, getPort() >= 0, embedded);
    }
}
