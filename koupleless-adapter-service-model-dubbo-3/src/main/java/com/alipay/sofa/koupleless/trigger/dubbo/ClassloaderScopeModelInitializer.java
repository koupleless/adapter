/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2024 All Rights Reserved.
 */
package com.alipay.sofa.koupleless.trigger.dubbo;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

/**
 * @
 * @author lianglipeng.llp@alibaba-inc.com
 * @version 1.2.1
 */
public class ClassloaderScopeModelInitializer implements ScopeModelInitializer {
    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {

    }

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {

    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {
        moduleModel.addClassLoader(Thread.currentThread().getContextClassLoader());
    }
}