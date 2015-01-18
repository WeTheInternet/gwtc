package net.wetheinter.gwtc.server;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import net.wetheinter.webcomponent.client.io.GwtcIO;

import org.eclipse.aether.repository.LocalArtifactResult;

import xapi.mvn.X_Maven;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class GwtcServlet extends RemoteServiceServlet implements GwtcIO {

  @Override
  public String compile(String target, String arguments) {


    final String[] args = arguments.replaceAll("[ ][ ]+", " ").split(" ");
    final Consumer<String> logger = new Consumer<String>() {
      @Override
      public void accept(String t) {
        System.out.println(t);
      }
    };
    try {
      try {
        GwtcCompiler.compile(args, logger);
      } catch (NoClassDefFoundError e) {
        LocalArtifactResult artifact = X_Maven.loadLocalArtifact("com.google.gwt", "gwt-dev", "2.7.2");
        File location = artifact.getFile();
        URL asUrl = location.toURL();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        List<URL> allUrls = new ArrayList<>();
        allUrls.add(asUrl);
        while (cl != null) {
          if (cl instanceof URLClassLoader) {
            @SuppressWarnings("resource")
            URLClassLoader urls = (URLClassLoader) cl;
            allUrls.addAll(Arrays.asList(urls.getURLs()));
          }
          cl = cl.getParent();
        }
        final URLClassLoader loader = new URLClassLoader(allUrls.toArray(new URL[allUrls.size()]));
        final String clsName = GwtcCompiler.class.getName();
        Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              Class<?> c = loader.loadClass(clsName);
              Method method = c.getMethod("compile", String[].class, Consumer.class);
              method.invoke(null, new Object[]{args}, logger);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
        t.setContextClassLoader(loader);
        t.start();
        t.join();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "hello";
  }

  @Override
  protected String getCodeServerPolicyUrl(String strongName) {
    return "http://localhost:9876/policies/" + strongName + ".gwt.rpc";
  }

}
