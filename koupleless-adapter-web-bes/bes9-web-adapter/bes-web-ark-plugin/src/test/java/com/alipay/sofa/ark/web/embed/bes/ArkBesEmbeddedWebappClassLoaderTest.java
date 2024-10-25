package com.alipay.sofa.ark.web.embed.bes;

import com.bes.enterprise.webtier.LifecycleException;
import com.bes.enterprise.webtier.WebResource;
import com.bes.enterprise.webtier.WebResourceRoot;
import com.bes.enterprise.webtier.loader.ResourceEntry;
import com.bes.enterprise.webtier.loader.WebappClassLoaderBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static com.bes.enterprise.webtier.LifecycleState.STARTED;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ArkBesEmbeddedWebappClassLoaderTest {

    private ArkBesEmbeddedWebappClassLoader arkBesEmbeddedWebappClassLoader = new ArkBesEmbeddedWebappClassLoader();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void loadClass() throws ClassNotFoundException, LifecycleException {
        // 1) Test load class from current class loader's parent
        assertEquals(String.class,arkBesEmbeddedWebappClassLoader.loadClass(String.class.getName(),true));
        // 2) Test load class from current class loader's parent , which is null.
        ArkBesEmbeddedWebappClassLoader arkBesEmbeddedWebappClassLoader2 =  new ArkBesEmbeddedWebappClassLoader();
        try{
            Field field = WebappClassLoaderBase.class.getDeclaredField("parent");
            field.setAccessible(true);
            field.set(arkBesEmbeddedWebappClassLoader2,null);
            arkBesEmbeddedWebappClassLoader2.loadClass(String.class.getName(), true);
            assertFalse(true);
        }catch(Exception e){
            if(e instanceof ClassNotFoundException){
                // we expected ClassNotFoundException is thrown here,so we do nothing.
            }else{
                throw new RuntimeException(e);
            }
        }

        // 3) Test load class without web class loader class cache.
        try {
            Field field = WebappClassLoaderBase.class.getDeclaredField("state");
            field.setAccessible(true);
            field.set(arkBesEmbeddedWebappClassLoader,STARTED);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        WebResourceRoot webResourceRoot = mock(WebResourceRoot.class);
        WebResource webResource = mock(WebResource.class);
        String path = "/"+this.getClass().getName().replace(".","/")+".class";
        when(webResourceRoot.getClassLoaderResource(path)).thenReturn(webResource);
        arkBesEmbeddedWebappClassLoader.setResources(webResourceRoot);

        assertEquals(this.getClass(),
                arkBesEmbeddedWebappClassLoader.loadClass(this.getClass().getName(),true));
        verify(webResourceRoot,times(1)).getClassLoaderResource(path);
        //note: exists always return false to mock resource not found and fallback to parent class loader.
        verify(webResource, times(1)).exists();

        // 4) Test load class with web class loader class cache.
        ConcurrentHashMap<String, ResourceEntry> resourceEntries = new ConcurrentHashMap<>();
        path = "/a/b.class";
        try{
            Field field = WebappClassLoaderBase.class.getDeclaredField("resourceEntries");
            field.setAccessible(true);
            ResourceEntry resourceEntry = new ResourceEntry();
            resourceEntry.loadedClass = this.getClass();
            resourceEntries.put(path,resourceEntry);
            field.set(arkBesEmbeddedWebappClassLoader,resourceEntries);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        assertEquals(this.getClass(), arkBesEmbeddedWebappClassLoader.loadClass("a.b",true));

        // 5) Test load class with delegate TRUE.
        arkBesEmbeddedWebappClassLoader.setDelegate(true);
        assertEquals(String.class,
                arkBesEmbeddedWebappClassLoader.loadClass(String.class.getName(),true));
        assertEquals(this.getClass(),arkBesEmbeddedWebappClassLoader.loadClass("a.b", false));

        // 6) Test load class with delegate FALSE.
        arkBesEmbeddedWebappClassLoader.setDelegate(false);
        assertEquals(WebappClassLoaderBase.class,
                arkBesEmbeddedWebappClassLoader.loadClass(WebappClassLoaderBase.class.getName(),true));
        ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.loadedClass = String.class;
        resourceEntries.put("/org/apache.class",resourceEntry);
        assertEquals(String.class,
                arkBesEmbeddedWebappClassLoader.loadClass("org.apache",false));
    }

    @Test(expected = ClassNotFoundException.class)
    public void testLoadClassNotFound() throws ClassNotFoundException{
        assertEquals(this.getClass(),arkBesEmbeddedWebappClassLoader.loadClass("a.b",true));
    }

    @Test
    public void testOtherMethods() throws IOException, ClassNotFoundException {

        new ArkBesEmbeddedWebappClassLoader(this.getClass().getClassLoader());

        assertNull(arkBesEmbeddedWebappClassLoader.findResource("aaa"));

        assertEquals(false, arkBesEmbeddedWebappClassLoader.findResources("aaa")
                .hasMoreElements());

        arkBesEmbeddedWebappClassLoader.addURL(null);

        try {
            Field field = WebappClassLoaderBase.class.getDeclaredField("securityManager");
            field.setAccessible(true);
            field.set(arkBesEmbeddedWebappClassLoader, new SecurityManager());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        arkBesEmbeddedWebappClassLoader.checkPackageAccess(String.class.getName());
        arkBesEmbeddedWebappClassLoader.checkPackageAccess("java"); // cover wrong class name
    }

    @Test(expected = ClassNotFoundException.class)
    public void testCheckPackageAccessFailed() throws IOException, ClassNotFoundException {

        SecurityManager securityManager = mock(SecurityManager.class);
        doThrow(new SecurityException()).when(securityManager).checkPackageAccess("a");

        try {
            Field field = WebappClassLoaderBase.class.getDeclaredField("securityManager");
            field.setAccessible(true);
            field.set(arkBesEmbeddedWebappClassLoader, securityManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        arkBesEmbeddedWebappClassLoader.checkPackageAccess("a.b");
    }
}