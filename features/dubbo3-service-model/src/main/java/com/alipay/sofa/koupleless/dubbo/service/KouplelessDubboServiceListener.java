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

import com.alipay.sofa.koupleless.common.BizRuntimeContext;
import com.alipay.sofa.koupleless.common.BizRuntimeContextRegistry;
import com.alipay.sofa.koupleless.common.exception.BizRuntimeException;
import com.alipay.sofa.koupleless.common.service.AbstractServiceComponent;
import com.alipay.sofa.koupleless.common.service.ServiceState;
import com.alipay.sofa.koupleless.dubbo.KouplelessDubboUtils;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceListener;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import static com.alipay.sofa.koupleless.common.exception.ErrorCodes.ServiceManager.E200001;
import static com.alipay.sofa.koupleless.common.exception.ErrorCodes.SpringContextManager.E100002;
import static com.alipay.sofa.koupleless.dubbo.service.KouplelessDubboServiceConstants.PROTOCOL;

/**
 * @author lianglipeng.llp@alibaba-inc.com
 * @version $Id: KouplelessDubboServiceListener.java, v 0.1 2024年05月20日 14:15 立蓬 Exp $
 */
public class KouplelessDubboServiceListener implements ServiceListener {

    @Override
    public void exported(ServiceConfig sc) {
        BizRuntimeContext bizRuntimeContext = BizRuntimeContextRegistry
            .getBizRuntimeContextByClassLoader(Thread.currentThread().getContextClassLoader());
        if (bizRuntimeContext == null) {
            throw new BizRuntimeException(E100002, "biz runtime context is null");
        }

        ServiceMetadata serviceMetadata = sc.getServiceMetadata();
        Object target = serviceMetadata.getTarget();
        DubboServiceComponent dubboServiceComponent = DubboServiceComponent.builder()
            .dubboProtocol(KouplelessDubboUtils.parseDubboProtocol(sc))
            .identifier(KouplelessDubboUtils.buildServiceIdentifier(sc)).bean(target)
            .beanClass(target.getClass()).interfaceType(serviceMetadata.getServiceType())
            .metaData(serviceMetadata).state(ServiceState.EXPORTED).build();
        bizRuntimeContext.registerService(dubboServiceComponent);
    }

    @Override
    public void unexported(ServiceConfig sc) {
        BizRuntimeContext bizRuntimeContext = BizRuntimeContextRegistry
            .getBizRuntimeContextByClassLoader(Thread.currentThread().getContextClassLoader());
        if (bizRuntimeContext == null) {
            throw new BizRuntimeException(E100002, "biz runtime context is null");
        }
        AbstractServiceComponent component = bizRuntimeContext.getServiceComponent(PROTOCOL,
            KouplelessDubboUtils.buildServiceIdentifier(sc));
        if (component == null) {
            throw new BizRuntimeException(E200001,
                "service not found:" + KouplelessDubboUtils.buildServiceIdentifier(sc));
        }
        component.setServiceState(ServiceState.UNEXPORTED);
        bizRuntimeContext.unregisterService(component);
    }
}
