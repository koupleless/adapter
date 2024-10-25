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

import com.bes.enterprise.naming.ContextBindings;
import com.bes.enterprise.springboot.embedded.ConnectorStartFailedException;
import com.bes.enterprise.web.Embedded;
import com.bes.enterprise.webtier.*;
import com.bes.enterprise.webtier.connector.Connector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;

import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * NOTE: Tomcat instance will start immediately when create ArkTomcatWebServer object.
 *
 * @author Brian Clozel
 * @author Kristine Jetzke
 * @author 0.6.0
 * @since 2.0.0
 */
public class ArkBesWebServer implements WebServer {

    private static final Log                logger            = LogFactory
            .getLog(ArkBesWebServer.class);

    private static final AtomicInteger      containerCounter  = new AtomicInteger(-1);

    private final Object                    monitor           = new Object();

    private final Map<Service, Connector[]> serviceConnectors = new HashMap<>();

    private final Embedded                  embedded;

    private final boolean                   autoStart;

    private volatile boolean                started;

    private Thread                          awaitThread;

    private Embedded                        arkEmbeded;

    /**
     * Create a new {@link ArkBesWebServer} instance.
     * @param embedded the underlying BesWebServer
     */
    public ArkBesWebServer(Embedded embedded) {
        this(embedded, true);
    }

    /**
     * Create a new {@link ArkBesWebServer} instance.
     * @param embedded the underlying BesWebServer
     * @param autoStart if the server should be started
     */
    public ArkBesWebServer(Embedded embedded, boolean autoStart) {
        Assert.notNull(embedded, "Bes Server must not be null");
        this.embedded = embedded;
        this.autoStart = autoStart;
        initialize();
    }

    public ArkBesWebServer(Embedded embedded, boolean autoStart, Embedded arkEmbeded) {
        this(embedded, autoStart);
        this.arkEmbeded = arkEmbeded;
    }

    private void initialize() throws WebServerException {
        logger.info("BesWebServer initialized with port(s): " + getPortsDescription(false));
        synchronized (this.monitor) {
            try {
                addInstanceIdToEngineName();

                Context context = findContext();
                context.addLifecycleListener((event) -> {
                    if (context.equals(event.getSource())
                            && Lifecycle.START_EVENT.equals(event.getType())) {
                        // Remove service connectors so that protocol binding doesn't
                        // happen when the service is started.
                        removeServiceConnectors();
                    }
                });

                // Start the server to trigger initialization listeners
                this.embedded.start();

                // We can re-throw failure exception directly in the main thread
                rethrowDeferredStartupExceptions();

                try {
                    ContextBindings.bindClassLoader(context, context.getNamingToken(),
                            Thread.currentThread().getContextClassLoader());
                }
                catch (NamingException ex) {
                    // Naming is not enabled. Continue
                }

                // Unlike Jetty, all BesWebServer threads are daemon threads. We create a
                // blocking non-daemon to stop immediate shutdown
                startDaemonAwaitThread();
            }
            catch (Exception ex) {
                stopSilently();
                throw new WebServerException("Unable to start embedded BesWebServer", ex);
            }
        }
    }

    private Context findContext() {
        for (Container child : this.embedded.getHost().findChildren()) {
            if (child instanceof Context) {
                if (child.getParentClassLoader().equals(
                        Thread.currentThread().getContextClassLoader())) {
                    return (Context) child;
                }
            }
        }
        throw new IllegalStateException("The host does not contain a Context");
    }

    private void addInstanceIdToEngineName() {
        int instanceId = containerCounter.incrementAndGet();
        if (instanceId > 0) { // We already have a BesWebServer container, so just return the existing BesWebServer.
            Engine engine = this.embedded.getEngine();
            engine.setName(engine.getName() + "-" + instanceId);
        }
    }

    private void removeServiceConnectors() {
        for (Service service : this.embedded.getServer().findServices()) {
            Connector[] connectors = service.findConnectors().clone();
            this.serviceConnectors.put(service, connectors);
            for (Connector connector : connectors) {
                service.removeConnector(connector);
            }
        }
    }

    private void rethrowDeferredStartupExceptions() throws Exception {
        Container[] children = this.embedded.getHost().findChildren();
        for (Container container : children) {
            // just to check current biz status
            if (container.getParentClassLoader() == Thread.currentThread().getContextClassLoader()) {
                if (!LifecycleState.STARTED.equals(container.getState())) {
                    throw new IllegalStateException(container + " failed to start");
                }
            }
        }
    }

    private void startDaemonAwaitThread() {
        awaitThread = new Thread("container-" + (containerCounter.get())) {

            @Override
            public void run() {
                getBesCore().getServer().await();
            }

        };
        awaitThread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void start() throws WebServerException {
        synchronized (this.monitor) {
            if (this.started) {
                return;
            }
            try {
                addPreviouslyRemovedConnectors();
                this.embedded.getConnector();
                checkThatConnectorsHaveStarted();
                this.started = true;
                logger.info("BesWebServer started on port(s): " + getPortsDescription(true)
                        + " with context path '" + getContextPath() + "'");
            } catch (ConnectorStartFailedException ex) {
                stopSilently();
                throw ex;
            } catch (Exception ex) {
                throw new WebServerException("Unable to start embedded BesWebServer", ex);
            } finally {
                Context context = findContext();
                ContextBindings.unbindClassLoader(context, context.getNamingToken(), getClass()
                        .getClassLoader());
            }
        }
    }

    void checkThatConnectorsHaveStarted() {
        checkConnectorHasStarted(this.embedded.getConnector());
        for (Connector connector : this.embedded.getService().findConnectors()) {
            checkConnectorHasStarted(connector);
        }
    }

    private void checkConnectorHasStarted(Connector connector) {
        if (LifecycleState.FAILED.equals(connector.getState())) {
            throw new ConnectorStartFailedException(connector.getPort());
        }
    }

    public void stopSilently() {
        stopContext();
        try {
            stopEmbededServerIfNecessary();
        } catch (LifecycleException ex) {
            // Ignore
        }
    }

    private void stopContext() {
        Context context = findContext();
        getBesCore().getHost().removeChild(context);
    }

    private void stopEmbededServerIfNecessary() throws LifecycleException {
        if (embedded != arkEmbeded) {
            embedded.destroy();
        }
        awaitThread.stop();
    }

    void addPreviouslyRemovedConnectors() {
        Service[] services = this.embedded.getServer().findServices();
        for (Service service : services) {
            Connector[] connectors = this.serviceConnectors.get(service);
            if (connectors != null) {
                for (Connector connector : connectors) {
                    service.addConnector(connector);
                    if (!this.autoStart) {
                        stopProtocolHandler(connector);
                    }
                }
                this.serviceConnectors.remove(service);
            }
        }
    }

    private void stopProtocolHandler(Connector connector) {
        try {
            connector.getProtocolHandler().stop();
        } catch (Exception ex) {
            logger.error("Cannot pause connector: ", ex);
        }
    }

    Map<Service, Connector[]> getServiceConnectors() {
        return this.serviceConnectors;
    }

    @Override
    public void stop() throws WebServerException {
        synchronized (this.monitor) {
            boolean wasStarted = this.started;
            try {
                this.started = false;
                try {
                    stopContext();
                    stopEmbededServerIfNecessary();
                } catch (Throwable ex) {
                    // swallow and continue
                }
            } catch (Exception ex) {
                throw new WebServerException("Unable to stop embedded BesWebServer", ex);
            } finally {
                if (wasStarted) {
                    containerCounter.decrementAndGet();
                }
            }
        }
    }

    private String getPortsDescription(boolean localPort) {
        StringBuilder ports = new StringBuilder();
        for (Connector connector : this.embedded.getService().findConnectors()) {
            if (ports.length() != 0) {
                ports.append(' ');
            }
            int port = localPort ? connector.getLocalPort() : connector.getPort();
            ports.append(port).append(" (").append(connector.getScheme()).append(')');
        }
        return ports.toString();
    }

    @Override
    public int getPort() {
        Connector connector = this.embedded.getConnector();
        if (connector != null) {
            return connector.getLocalPort();
        }
        return 0;
    }

    private String getContextPath() {
        return findContext().getPath();
    }

    /**
     * Returns access to the underlying BesWebServer.
     * @return the BesWebServer
     */
    public Embedded getBesCore() {
        return this.embedded;
    }
}
