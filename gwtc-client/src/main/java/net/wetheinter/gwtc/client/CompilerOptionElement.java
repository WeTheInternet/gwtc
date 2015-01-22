package net.wetheinter.gwtc.client;

import static xapi.components.impl.JsSupport.attributeExists;
import static xapi.components.impl.JsSupport.getObject;
import static xapi.components.impl.JsSupport.jsNotEquals;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import xapi.components.api.NativelySupported;

@JsType
public interface CompilerOptionElement {

  public default boolean isSet () {
    return attributeExists(this, "value") &&
        jsNotEquals(getObject(getObject(this, "style"), "display"), "none");
  }

  @NativelySupported // By natively, we mean subinterfaces will define this as a js method
  String collect(String previous);

  @NativelySupported
  @JsProperty
  String tagName();

}
