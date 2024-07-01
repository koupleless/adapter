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
package com.alipay.sofa.koupleless.dubbo.service;

import com.alipay.sofa.koupleless.common.service.AbstractServiceComponent;
import com.alipay.sofa.koupleless.common.service.ServiceState;
import lombok.Builder;

import static com.alipay.sofa.koupleless.dubbo.service.KouplelessDubboServiceConstants.PROTOCOL;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: DubboServiceComponent.java, v 0.1 2024年05月20日 14:19 立蓬 Exp $
 */

public class DubboServiceComponent extends AbstractServiceComponent {
    private final String dubboProtocol;

    @Builder
    public DubboServiceComponent(String dubboProtocol, String identifier, Object bean,
                                 Class<?> beanClass, Class<?> interfaceType, Object metaData,
                                 ServiceState state) {
        super(PROTOCOL, identifier, bean, beanClass, interfaceType, metaData, state);
        this.dubboProtocol = dubboProtocol;
    }

    public String getDubboProtocol() {
        return dubboProtocol;
    }
}
