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
import com.tongweb.commons.license.LicenseSDKProvider;
import com.tongweb.commons.utils.SystemExitUtil;
import com.tongweb.container.*;
import com.tongweb.container.connector.Connector;
import com.tongweb.container.core.AprLifecycleListener;
import com.tongweb.container.core.StandardContext;
import com.tongweb.container.loader.WebappLoader;
import com.tongweb.container.startup.ServletContainer;
import com.tongweb.container.util.ServerInfo;
import com.tongweb.container.valves.FilterValue;
import com.tongweb.container.webresources.StandardRoot;
import com.tongweb.springboot.properties.IoMode;
import com.tongweb.springboot.properties.ManageWebProperties;
import com.tongweb.springboot.properties.PropertyMapper;
import com.tongweb.springboot.properties.TongWebProperties;
import com.tongweb.springboot.starter.CheckIntegrityUtil;
import com.tongweb.springboot.starter.TongWebServletWebServerFactory;
import com.tongweb.web.util.modeler.Registry;
import com.tongweb.web.util.scan.StandardJarScanFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContainerInitializer;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.alipay.sofa.ark.spi.constant.Constants.ROOT_WEB_CONTEXT_PATH;

/**
 * @author chenjian
 */
public class ArkTongWebServletWebServerFactory extends TongWebServletWebServerFactory {

    private static final Charset                    DEFAULT_CHARSET             = StandardCharsets.UTF_8;

    @ArkInject
    private EmbeddedServerService<ServletContainer> embeddedServerService;

    @ArkInject
    private BizManagerService                       bizManagerService;

    private File                                    baseDirectory;

    private String                                  protocol                    = DEFAULT_PROTOCOL;

    private int                                     backgroundProcessorDelay;

    private final List<Connector>                   additionalTongWebConnectors = new ArrayList();

    @Autowired
    private TongWebProperties                       tongWebProperties;
    @Autowired
    private ManageWebProperties                     manageWebProperties;
    @Autowired
    private ApplicationContext                      context;
    private boolean                                 disableMBeanRegistry;
    private Valve                                   filterValve;

    public ArkTongWebServletWebServerFactory() {
    }

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        if (embeddedServerService == null && ArkClient.getInjectionService() != null) {
            // 非应用上下文 (例如: Spring Management Context) 没有经历 Start 生命周期, 不会被注入 ArkServiceInjectProcessor,
            // 因此 @ArkInject 没有被处理, 需要手动处理
            ArkClient.getInjectionService().inject(this);
        }
        ServletContainer servletContainer;
        if (embeddedServerService == null) {
            return super.getWebServer(initializers);
        } else if (embeddedServerService.getEmbedServer(getPort()) == null) {
            embeddedServerService.putEmbedServer(getPort(), initEmbedWebServer(initializers));
            servletContainer = (ServletContainer) embeddedServerService.getEmbedServer(getPort());
        } else {
            servletContainer = (ServletContainer) embeddedServerService.getEmbedServer(getPort());
            prepareContext(servletContainer.getHost(), initializers);
        }
        return getWebServer(servletContainer);
    }

    @PostConstruct
    private void init() {
        this.initDisableMBeanRegistry();
        if (!StringUtils.isEmpty(this.manageWebProperties.getContextPath())
            && (!StringUtils.isEmpty(this.manageWebProperties.getAccess_iplist()) || !StringUtils
                .isEmpty(this.manageWebProperties.getBlocked_iplist()))) {
            this.filterValve = new FilterValue(this.manageWebProperties.convertProp());
        }

    }

    private void initDisableMBeanRegistry() {
        this.disableMBeanRegistry = !this.tongWebProperties.getTongweb().getMbeanregistry()
            .isEnabled();
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

    private ServletContainer initEmbedWebServer(ServletContextInitializer... initializers) {
        System.setProperty("java.security.egd", "file:/dev/./urandom");
        this.addExitHook();
        this.processLicenseValidateConfig();
        if (!ObjectUtils
            .isEmpty(this.tongWebProperties.getTongweb().getAdditionalTldSkipPatterns())) {
            this.getTldSkipPatterns().addAll(
                this.tongWebProperties.getTongweb().getAdditionalTldSkipPatterns());
        }

        if (this.disableMBeanRegistry) {
            Registry.disableRegistry();
        }

        ServletContainer servletContainer = new ServletContainer();
        File baseDir = this.baseDirectory != null ? this.baseDirectory : this
            .createTempDir("tongweb");
        servletContainer.setBaseDir(baseDir.getAbsolutePath());
        if (this.protocol.equalsIgnoreCase(IoMode.APR.getClassName())) {
            LifecycleListener arpLifecycle = new AprLifecycleListener();
            this.addContextLifecycleListeners(arpLifecycle);
        }

        Connector connector = new Connector(this.protocol);
        servletContainer.getService().addConnector(connector);
        this.customizeConnector(connector);
        servletContainer.setConnector(connector);
        servletContainer.getHost().setAutoDeploy(false);
        this.configureEngine(servletContainer.getEngine());
        this.configureAccessLog(servletContainer.getHost());
        this.configureSemaphore(servletContainer.getHost());
        this.configureFilterValue(servletContainer.getHost());
        this.configureRemoteFilter(servletContainer.getHost());
        Iterator var5 = this.additionalTongWebConnectors.iterator();

        while (var5.hasNext()) {
            Connector additionalConnector = (Connector) var5.next();
            servletContainer.getService().addConnector(additionalConnector);
        }

        this.prepareContext(servletContainer.getHost(), initializers);
        this.configureWebApps(servletContainer);
        this.configureWar(servletContainer);
        return servletContainer;
    }

    private void configureFilterValue(Host host) {
        if (this.getFilterValve() != null) {
            host.getPipeline().addValve(this.getFilterValve());
        }

    }

    private void configureAccessLog(Host host) {
        if (this.getAccessLog() != null) {
            host.getPipeline().addValve(this.getAccessLog());
        }

    }

    private void configureSemaphore(Host host) {
        if (this.getSemaphore() != null) {
            host.getPipeline().addValve(this.getSemaphore());
        }

    }

    private void addExitHook() {
        SystemExitUtil.setHook(() -> {
            System.exit(SpringApplication.exit(this.context, new ExitCodeGenerator[]{() -> {
                return 0;
            }}));
        });
    }

    private void putLicenseIps(String value) {
        System.setProperty("server.tongweb.license.license-ips", value);
    }

    private void putPublicKey(String value) {
        System.setProperty("server.tongweb.license.publicKey", value);
    }

    private void putProductKey(String value) {
        System.setProperty("server.tongweb.license.productKey", value);
    }

    private void putTongwebEdition(String value) {
        System.setProperty("server.tongweb.license.tongWebEdition", value);
    }

    private void pusSslEnabled(boolean value) {
        System.setProperty("server.tongweb.license.ssl.enabled", String.valueOf(value));
    }

    private void putKeyStore(String value) {
        System.setProperty("server.tongweb.license.ssl.keyStore", value);
    }

    private void putKeyStorePassword(String value) {
        System.setProperty("server.tongweb.license.ssl.keyStorePassword", value);
    }

    private void putKeyStoreType(String value) {
        System.setProperty("server.tongweb.license.ssl.keyStoreType", value);
    }

    private void putTrustStore(String value) {
        System.setProperty("server.tongweb.license.ssl.trustStore", value);
    }

    private void putTrustStorePassword(String value) {
        System.setProperty("server.tongweb.license.ssl.trustStorePassword", value);
    }

    private void putTrustStoreType(String value) {
        System.setProperty("server.tongweb.license.ssl.trustStoreType", value);
    }

    private void processLicenseValidateConfig() {
        ServerInfo.getServerENumber();
        new CheckIntegrityUtil();
        PropertyMapper propertyMapper = PropertyMapper.get();
        String validType = this.tongWebProperties.getTongweb().getLicense().getType();
        String licensePath = this.tongWebProperties.getTongweb().getLicense().getPath();
        Boolean isSync = this.tongWebProperties.getTongweb().getLicense().isSync();
        System.setProperty("server.tongweb.license.type", validType);
        System.setProperty("server.tongweb.license.filePath", licensePath);
        System.setProperty("server.tongweb.license.isSync", isSync.toString());
        propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().getLicenseIps()).whenHasText().to(this::putLicenseIps);
        propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().getLicensePublicKey()).whenHasText().to(this::putPublicKey);
        if (this.tongWebProperties.getTongweb().getLicense().getSsl() != null) {
            propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().isSslEnabled()).whenNonNull().to(this::pusSslEnabled);
            propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().getSsl().getKeyStore()).whenHasText().to(this::putKeyStore);
            propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().getSsl().getKeyStorePassword()).whenHasText().to(this::putKeyStorePassword);
            propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().getSsl().getKeyStoreType()).whenHasText().to(this::putKeyStoreType);
            propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().getSsl().getTrustStore()).whenHasText().to(this::putTrustStore);
            propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().getSsl().getTrustStorePassword()).whenHasText().to(this::putTrustStorePassword);
            propertyMapper.from(this.tongWebProperties.getTongweb().getLicense().getSsl().getTrustStoreType()).whenHasText().to(this::putTrustStoreType);
        }

        LicenseSDKProvider.validate();
    }

    @Override
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    /**
     * The tongweb protocol to use when create the {@link Connector}.
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
            .setLoaderClass("com.alipay.sofa.ark.web.embed.tongweb.ArkTongWebEmbeddedWebappClassLoader");
    }

    @Override
    protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
        if (host.getState() == LifecycleState.NEW) {
            super.prepareContext(host, initializers);
        } else {
            File documentRoot = getValidDocumentRoot();
            StandardContext context = new StandardContext();
            if (documentRoot != null) {
                context.setResources(new StandardRoot(context));
            }
            context.setName(getContextPath());
            context.setDisplayName(getDisplayName());
            context.setPath(getContextPath());
            File docBase = (documentRoot != null) ? documentRoot : createTempDir("tongweb-docbase");
            context.setDocBase(docBase.getAbsolutePath());
            context.addLifecycleListener(new ServletContainer.FixContextListener());
            context.setParentClassLoader(Thread.currentThread().getContextClassLoader());
            resetDefaultLocaleMapping(context);
            addLocaleMappings(context);
            context.setUseRelativeRedirects(false);
            configureTldSkipPatterns(context);
            WebappLoader loader = new WebappLoader();
            loader
                .setLoaderClass("com.alipay.sofa.ark.web.embed.tongweb.ArkTongWebEmbeddedWebappClassLoader");
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
     * Override tongweb's default locale mappings to align with other servers. See
     * {@code org.apache.catalina.util.CharsetMapperDefault.properties}.
     * @param context the context to reset
     */
    private void resetDefaultLocaleMapping(StandardContext context) {
        context.addLocaleEncodingMappingParameter(Locale.ENGLISH.toString(),
            DEFAULT_CHARSET.displayName());
        context.addLocaleEncodingMappingParameter(Locale.FRENCH.toString(),
            DEFAULT_CHARSET.displayName());
    }

    private void addLocaleMappings(StandardContext context) {
        for (Map.Entry<Locale, Charset> entry : getLocaleCharsetMappings().entrySet()) {
            context.addLocaleEncodingMappingParameter(entry.getKey().toString(), entry.getValue()
                .toString());
        }
    }

    private void configureTldSkipPatterns(StandardContext context) {
        StandardJarScanFilter filter = new StandardJarScanFilter();
        filter.setTldSkip(StringUtils.collectionToCommaDelimitedString(getTldSkipPatterns()));
        context.getJarScanner().setJarScanFilter(filter);
    }

    private void addDefaultServlet(Context context) {
        Wrapper defaultServlet = context.createWrapper();
        defaultServlet.setName("default");
        defaultServlet.setServletClass("com.tongweb.container.servlets.DefaultServlet");
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

    private void addJasperInitializer(StandardContext context) {
        try {
            ServletContainerInitializer initializer = (ServletContainerInitializer) ClassUtils
                .forName("org.apache.jasper.servlet.JasperInitializer", null).newInstance();
            context.addServletContainerInitializer(initializer, null);
        } catch (Exception ex) {
            // Probably not tongweb other version
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
                    // It's a nested jar but we now don't want the suffix because tongweb
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
     * additional processing to the tongweb server.
     */
    protected WebServer getWebServer(ServletContainer embedded) {
        return new ArkTongWebServer(embedded, getPort() >= 0, embedded);
    }
}
