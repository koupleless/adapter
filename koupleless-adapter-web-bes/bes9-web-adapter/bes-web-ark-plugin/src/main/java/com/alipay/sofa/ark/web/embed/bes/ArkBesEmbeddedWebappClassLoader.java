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
package com.alipay.sofa.ark.web.embed.bes;

import com.alipay.sofa.ark.common.log.ArkLoggerFactory;
import com.bes.enterprise.webtier.loader.ParallelWebappClassLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

public class ArkBesEmbeddedWebappClassLoader extends ParallelWebappClassLoader {
    private static final Logger LOGGER = ArkLoggerFactory
                                           .getLogger(ArkBesEmbeddedWebappClassLoader.class);

    public ArkBesEmbeddedWebappClassLoader() {
    }

    public ArkBesEmbeddedWebappClassLoader(ClassLoader parent) {
        super(parent);
    }

    public URL findResource(String name) {
        return null;
    }

    public Enumeration<URL> findResources(String name) throws IOException {
        return Collections.emptyEnumeration();
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            Class<?> result = this.findExistingLoadedClass(name);
            result = result != null ? result : this.doLoadClass(name);
            if (result == null) {
                throw new ClassNotFoundException(name);
            } else {
                return this.resolveIfNecessary(result, resolve);
            }
        }
    }

    private Class<?> findExistingLoadedClass(String name) {
        Class<?> resultClass = this.findLoadedClass0(name);
        resultClass = resultClass != null ? resultClass : this.findLoadedClass(name);
        return resultClass;
    }

    private Class<?> doLoadClass(String name) throws ClassNotFoundException {
        this.checkPackageAccess(name);
        Class result;
        if (!this.delegate && !this.filter(name, true)) {
            result = this.findClassIgnoringNotFound(name);
            return result != null ? result : this.loadFromParent(name);
        } else {
            result = this.loadFromParent(name);
            return result != null ? result : this.findClassIgnoringNotFound(name);
        }
    }

    private Class<?> resolveIfNecessary(Class<?> resultClass, boolean resolve) {
        if (resolve) {
            this.resolveClass(resultClass);
        }

        return resultClass;
    }

    protected void addURL(URL url) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Ignoring request to add " + url + " to the bes classloader");
        }

    }

    private Class<?> loadFromParent(String name) {
        if (this.parent == null) {
            return null;
        } else {
            try {
                return Class.forName(name, false, this.parent);
            } catch (ClassNotFoundException var3) {
                return null;
            }
        }
    }

    private Class<?> findClassIgnoringNotFound(String name) {
        try {
            return this.findClass(name);
        } catch (ClassNotFoundException var3) {
            return null;
        }
    }

    void checkPackageAccess(String name) throws ClassNotFoundException {
        if (this.securityManager != null && name.lastIndexOf(46) >= 0) {
            try {
                this.securityManager.checkPackageAccess(name.substring(0, name.lastIndexOf(46)));
            } catch (SecurityException var3) {
                throw new ClassNotFoundException(
                    "Security Violation, attempt to use Restricted Class: " + name, var3);
            }
        }

    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}
