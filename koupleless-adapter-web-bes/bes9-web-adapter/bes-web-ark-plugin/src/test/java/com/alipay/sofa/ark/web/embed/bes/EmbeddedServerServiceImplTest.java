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