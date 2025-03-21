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
package com.alipay.sofa.ark.springboot;

import com.alipay.sofa.ark.springboot.condition.ConditionalOnArkEnabled;
import com.alipay.sofa.ark.springboot.web.ArkTongWebServletWebServerFactory;
import com.tongweb.connector.UpgradeProtocol;
import com.tongweb.container.startup.ServletContainer;
import com.tongweb.springboot.servlet.LiteTongWeb;
import com.tongweb.springboot.starter.TongWebServletWebServerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Servlet;

/**
 * adapt to springboot 2.1.9.RELEASE
 * @author chenjian
 */
@Configuration
@ConditionalOnArkEnabled
@ConditionalOnClass(LiteTongWeb.class)
@AutoConfigureBefore(LiteTongWeb.class)
public class ArkTongWebServletLegacyAutoConfiguration {
    @Configuration
    @ConditionalOnClass(value = { Servlet.class, ServletContainer.class, UpgradeProtocol.class,
            TongWebServletWebServerFactory.class }, name = { "com.alipay.sofa.ark.web.embed.tongweb.ArkTongWebEmbeddedWebappClassLoader" })
    @ConditionalOnMissingBean(value = TongWebServletWebServerFactory.class, search = SearchStrategy.CURRENT)
    public static class EmbeddedArkTongWeb {

        @Bean
        @ConditionalOnMissingBean(ArkTongWebServletWebServerFactory.class)
        public TongWebServletWebServerFactory tongWebServletWebServerFactory() {
            return new ArkTongWebServletWebServerFactory();
        }
    }
}