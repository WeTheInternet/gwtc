package net.wetheinter.gwtc.server;

import static xapi.time.X_Time.trySleep;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebListener;

import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.resolution.ArtifactResult;

import xapi.mvn.X_Maven;
import xapi.util.X_Debug;

@WebListener
public class GwtcMonitorService implements ServletContextListener {

  protected static class CompileTask implements Runnable {

    private static final String COMPILER_CLASS = GwtcCompiler.class.getName();
    private static final String CONSUMER_CLASS = Consumer.class.getName();
    private final CompileRequest compileRequest;
    private final ServletContext ctx;
    private final Executor watcherExecutor;
    private ClassLoader classLoader;
    private Thread compilerThread;


    public CompileTask(CompileRequest compileRequest, ClassLoader classLoader, ServletContext ctx) {
      this.compileRequest = compileRequest;
      this.ctx = ctx;
      watcherExecutor = Executors.newCachedThreadPool();
      this.classLoader = classLoader;
    }

    @Override
    public void run() {

      // Launch the compile in a thread with a custom classloader
      final String[] args = compileRequest.getArgs();

      compilerThread = new Thread() {
        @Override
        public void run() {
          boolean success = false;
          try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> compiler = cl.loadClass(COMPILER_CLASS);
            Class<?> consumer = cl.loadClass(CONSUMER_CLASS);
            final Method log = compileRequest.getClass().getMethod("addLog", String.class);
            Method method = compiler.getMethod("compile", String[].class, consumer);
            success = (Boolean)method.invoke(null, args, new Consumer<String>() {
              @Override
              public void accept(String t) {
                try {
                  log.invoke(compileRequest, t);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            compileRequest.addLog("[ERROR] Compile failed: " + e);
          } finally {
            compileRequest.setFinished(true);
            System.out.println("Compile success? "+success);
            final Map<String, Boolean> finished = (Map<String, Boolean>) ctx.getAttribute(ATTR_FINISHED_COMPILES);
            finished.put(compileRequest.getId(), success);
            compilerThread = null;
          }
        }
      };
      compilerThread.setContextClassLoader(classLoader);
      compilerThread.start();
      classLoader = null;

      final Map<String, List<AsyncContext>> compileWatchers =
          (Map<String, List<AsyncContext>>) ctx.getAttribute(ATTR_COMPILE_SUBSCRIPTION);
      // Send notifications to anyone who cares
      while (!compileRequest.isFinished()) {

        List<AsyncContext> watchers = compileWatchers.remove(compileRequest.getId());
        if (watchers == null) {
          trySleep(500, 0);
          continue;
        }
        for (final AsyncContext aCtx : watchers) {
          watcherExecutor.execute(new MessengerTask(compileRequest, aCtx));
        }
      }

    }

  }

  protected static class MessengerTask implements Runnable {

    private final CompileRequest compileRequest;
    private final AsyncContext   ctx;
    int pos = 0;

    public MessengerTask(CompileRequest compileRequest, AsyncContext ctx) {
      this.compileRequest = compileRequest;
      this.ctx = ctx;
    }

    @Override
    public void run() {
      // publish compile info as it comes in
      ServletResponse response = ctx.getResponse();
      try {
        PrintWriter writer = response.getWriter();
        while (!compileRequest.isFinished()) {
          flushMessages(writer);
          trySleep(500, 0);
        }
        flushMessages(writer);
        // Let the client close the context...
        writer.println("event: close");
        writer.println("data: success");
        writer.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private void flushMessages(PrintWriter writer) {
      if (compileRequest.hasNewMessages(pos)) {
        while (compileRequest.hasNewMessages(pos)) {
          writer.println("data: " + compileRequest.getMessage(pos));
          pos++;
        }
        writer.println("id: " + pos);
        writer.println();
        writer.flush();
      }
    }

  }

  static final String ATTR_COMPILE_SUBSCRIPTION = "gwtc-compileWatchers";
  static final String ATTR_COMPILE_REQUESTS     = "gwtc-compiles";
  static final String ATTR_FINISHED_COMPILES    = "gwtc-finished-compiles";

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {
  }

  @SuppressWarnings("unchecked")
  public static void addCompileRequest(ServletContext ctx, CompileRequest req) {
    ((Queue<CompileRequest>) ctx.getAttribute(ATTR_COMPILE_REQUESTS)).add(req);
  }

  @SuppressWarnings("unchecked")
  public static synchronized void addToWatchers(ServletContext ctx, final String id, final AsyncContext async) {
    final Map<String, Boolean> done = (Map<String, Boolean>) ctx.getAttribute(ATTR_FINISHED_COMPILES);
    if (done.containsKey(id)) {
      return;
    }

    final Map<String, List<AsyncContext>> watchers =
        (Map<String, List<AsyncContext>>) ctx.getAttribute(ATTR_COMPILE_SUBSCRIPTION);
    System.out.println("Watchers: " + watchers);
    List<AsyncContext> list = watchers.get(id);
    if (list == null) {
      list = new ArrayList<>();
      watchers.put(id, list);
    }
    for (ListIterator<AsyncContext> iter = list.listIterator(); iter.hasNext();) {
      AsyncContext old = iter.next();
      if (old.getResponse().isCommitted()) {
        iter.remove();
      }
    }
    list.add(async);

    async.addListener(new AsyncListener() {

      @Override
      public void onTimeout(AsyncEvent event) throws IOException {
        clear();
      }

      @Override
      public void onStartAsync(AsyncEvent event) throws IOException {
      }

      @Override
      public void onError(AsyncEvent event) throws IOException {
        System.err.println("Async messaging error detected: " + event);
        clear();
      }

      @Override
      public void onComplete(AsyncEvent event) throws IOException {
        clear();
      }

      private void clear() {
        List<AsyncContext> list = watchers.get(id);
        if (list != null) {
          list.remove(async);
          if (list.isEmpty()) {
            watchers.remove(id);
          }
        }
      }
    });

  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    final ServletContext ctx = sce.getServletContext();

    final Map<String, Boolean> finishedCompiles = new ConcurrentHashMap<>();
    ctx.setAttribute(ATTR_FINISHED_COMPILES, finishedCompiles);

    final Map<String, List<AsyncContext>> compileWatchers = new ConcurrentHashMap<>();
    ctx.setAttribute(ATTR_COMPILE_SUBSCRIPTION, compileWatchers);

    // store new compiles in the process of compiling
    final Queue<CompileRequest> compileRequests = new ConcurrentLinkedQueue<CompileRequest>();
    ctx.setAttribute(ATTR_COMPILE_REQUESTS, compileRequests);

    final Executor compileExecutor = Executors.newCachedThreadPool();

    Executors.newSingleThreadExecutor().execute(new Runnable() {
      @Override
      public void run() {
        while (true)
        {
          while (compileRequests.isEmpty()) {
            trySleep(100, 0);
          }
          if (!compileRequests.isEmpty()) {
            final CompileRequest compileRequest = compileRequests.poll();
            compileExecutor.execute(new CompileTask(compileRequest, getClassloader(compileRequest), ctx));
          }
        }
      }
    });
  }

  private ClassLoader getClassloader(CompileRequest compileRequest) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    List<URL> allUrls = new ArrayList<>();

    // We want to add the gwt-dev jars, since their embedded servlet api inclusions
    // conflict with most web servers and must thus be excluded from the runtime classpath
    allUrls.add(getGwtArtifact("gwt-dev"));
    allUrls.add(getGwtArtifact("gwt-codeserver"));
    allUrls.add(getGwtArtifact("gwt-elemental"));

    while (cl != null) {
      if (cl instanceof URLClassLoader) {
        @SuppressWarnings("resource")
        URLClassLoader urls = (URLClassLoader) cl;
        allUrls.addAll(Arrays.asList(urls.getURLs()));
      }
      cl = cl.getParent();
    }


    return new URLClassLoader(allUrls.toArray(new URL[allUrls.size()]));
  }

  @SuppressWarnings("deprecation")
  private URL getGwtArtifact(String artifactId) {
    LocalArtifactResult artifact = X_Maven.loadLocalArtifact(getGwtGroupId(), artifactId, getGwtVersion());
    File location = artifact.getFile();
    if (location == null) {
      ArtifactResult remoteLoad = X_Maven.loadArtifact(getGwtGroupId(), artifactId, getGwtVersion());
      if (remoteLoad.isResolved()) {
        location = remoteLoad.getArtifact().getFile();
      }
    }
    try {
      return location.toURL();
    } catch (MalformedURLException e) {
      throw X_Debug.rethrow(e);
    }
  }

  protected String getGwtGroupId() {
    return "com.google.gwt";
  }

  protected String getGwtVersion() {
    return "2.7.2";
  }
}
