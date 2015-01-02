package net.wetheinter.webcomponent.client;

import java.util.function.Consumer;

import com.google.gwt.core.client.JavaScriptObject;

public class JsoConsumer implements Consumer<Object> {

  private JavaScriptObject obj;

  public JsoConsumer(JavaScriptObject obj) {
    this.obj = obj;
  }

  @Override
  public native void accept(Object t)
  /*-{
		this.@net.wetheinter.webcomponent.client.JsoConsumer::obj.call(
				this.__caller__ || this, t);
  }-*/;

}
