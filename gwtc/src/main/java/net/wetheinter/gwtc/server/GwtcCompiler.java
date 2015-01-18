package net.wetheinter.gwtc.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.ArgProcessorBase;
import com.google.gwt.dev.BootStrapPlatform;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.javac.CompilationProblemReporter;

public class GwtcCompiler {

  protected static class ReflectiveTreeLogger extends TreeLogger {
    String indent = "";
    private final Method accept;
    private final Type level;
    private final Consumer<String> log;

    public ReflectiveTreeLogger(Type level, Consumer<String> log) throws NoSuchMethodException, SecurityException {
      this(level, log, log.getClass().getMethod("accept", String.class));
    }

    public ReflectiveTreeLogger(Type level, Consumer<String> log, Method accept) {
      this.level = level;
      this.accept = accept;
      accept.setAccessible(true);
      this.log = log;
    }

    @Override
    public TreeLogger branch(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
      ReflectiveTreeLogger newLogger = new ReflectiveTreeLogger(level, log, accept);
      newLogger.indent = indent+"  ";
      return newLogger;
    }

    @Override
    public boolean isLoggable(Type type) {
      return !type.isLowerPriorityThan(level);
    }

    @Override
    public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
      if (isLoggable(type)) {
        try {
          accept.invoke(log, indent+msg);
          while (caught != null) {
            accept.invoke(log, indent+caught);
            for (StackTraceElement trace : caught.getStackTrace()) {
              accept.invoke(log, indent+trace);
            }
            caught = caught.getCause();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

}

  public static boolean compile(String[] args, final Consumer<String> log) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

    final CompilerOptions options = new CompilerOptionsImpl();
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Class<?> c = cl.loadClass(Compiler.class.getName()+"$ArgProcessor");
    Constructor<?> ctor = c.getConstructor(CompilerOptions.class);
    ctor.setAccessible(true);
    ArgProcessorBase inst = (ArgProcessorBase) ctor.newInstance(options);

    Type level = options.getLogLevel();
    if (level == null) {
      level = Type.INFO;
    }
    final TreeLogger reflectiveLogger = new ReflectiveTreeLogger(level, log);
    System.out.println("Compiling "+Arrays.asList(args));
    if (inst.processArgs(args)) {
      BootStrapPlatform.applyPlatformHacks();

      if (System.getProperty("java.awt.headless") == null) {
        System.setProperty("java.awt.headless", "true");
      }
      try {
        return new Compiler(options).run(reflectiveLogger);
      } catch (UnableToCompleteException e) {
      } catch (Throwable e) {
        CompilationProblemReporter.logAndTranslateException(reflectiveLogger, e);
      }
    }
    return false;
  }

}
