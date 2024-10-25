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

import com.bes.enterprise.web.Embedded;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class EmbeddedServerServiceImplTest {

    private EmbeddedServerServiceImpl embeddedServerServiceImpl = new EmbeddedServerServiceImpl();
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void setEmbedServer() {

        embeddedServerServiceImpl.setEmbedServer(null);
        assertEquals(null, embeddedServerServiceImpl.getEmbedServer());

        Embedded embedded = new Embedded();
        embeddedServerServiceImpl.setEmbedServer(embedded);
        assertEquals(embedded, embeddedServerServiceImpl.getEmbedServer());

        Embedded embedded2 = new Embedded();
        embeddedServerServiceImpl.setEmbedServer(embedded2);
        // should be still old one
        assertEquals(embedded, embeddedServerServiceImpl.getEmbedServer());
    }
}