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
package com.alipay.sofa.koupleless.base.build.plugin;

import lombok.Data;

import java.util.List;

@Data
public class AdapterPatch {
    /**
     * 当前修改增强后文件的根路径，一般为该类所在 bundle 的根目录
     */
    private String       rootPath;

    /**
     * 当前修改增强后文件的名称
     */
    private String       fileName;

    /**
     * 当前修改增强后文件的 package 名称
     */
    private String       subPath;

    /**
     * 当前增强文件来源的 maven jar sources 文件内容
     */
    private List<String> sourceLines;

    /**
     * 当前增强文件的内容
     */
    private List<String> adaptedLines;

    /**
     * 当前增强部分的 diff patch 内容
     */
    private List<String> patchLines;
}
