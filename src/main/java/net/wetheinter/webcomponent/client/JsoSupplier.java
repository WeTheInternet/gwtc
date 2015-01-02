package net.wetheinter.webcomponent.client;

import java.util.function.Supplier;

import com.google.gwt.core.client.JavaScriptObject;

public class JsoSupplier implements Supplier<Object> {

  private JavaScriptObject jso;

  public JsoSupplier(JavaScriptObject jso) {
    this.jso = jso;
  }

  @Override
  public native Object get()
  /*-{
		return this.@net.wetheinter.webcomponent.client.JsoSupplier::jso
				.call(this.__caller__ || this);
  }-*/;
}
