package net.wetheinter.webcomponent.client;

import java.util.function.Consumer;

import com.google.gwt.core.client.JavaScriptObject;

public class JsFunctionSupport {

	public static native JavaScriptObject wrapRunnable(Runnable task)
	/*-{
	  return $entry(function(){
	    task.@java.lang.Runnable::run()();
	  });
	 }-*/;

  public static native <T> JavaScriptObject wrapConsumer(Consumer<T> task)
	/*-{
	  return $entry(function(){
	    task.@java.util.function.Consumer::accept(Ljava/lang/Object;)(arguments[0]);
	  });
	 }-*/;
	
	@SuppressWarnings("rawtypes")
	public static native JavaScriptObject wrapConsumerOfThis(Consumer task)
	/*-{
	  return $entry(function(){
	    task.@java.util.function.Consumer::accept(Ljava/lang/Object;)(this);
	  });
	 }-*/;

	public static native JavaScriptObject merge(JavaScriptObject first, JavaScriptObject second)
	/*-{
	  return function() {
	  	first.apply(this, arguments);
	  	second.apply(this, arguments);
	  };
  }-*/;
}
