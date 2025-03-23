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
package com.alipay.sofa.koupleless.adapter.thread;

public class BizContextHolder {
    private static final ThreadLocal<BizContext> BIZ_CONTEXT = new ThreadLocal<>();

    public static void set(BizContext context) {
        BIZ_CONTEXT.set(context);
    }

    public static void clear() {
        BIZ_CONTEXT.remove();
    }

    public static BizContext get() {
        return BIZ_CONTEXT.get();
    }

    public static BizContext cloneBizContext() {
        BizContext context = get();
        if (context == null) {
            return null;
        }
        return context.cloneInstance();
    }
}
