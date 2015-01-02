package net.wetheinter.webcomponent.client;

import net.wetheinter.webcomponent.client.api.HasElement;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;
import elemental.html.BodyElement;

@JsType
public interface Document {
  @JsType
  public static interface RegisterElement {
    JavaScriptObject call(Document doc, String name, JavaScriptObject prototype);
  }

  @JsProperty
  Document.RegisterElement registerElement();

  <E extends HasElement<? extends Element>> E createElement(String string);

  @JsProperty
  BodyElement body();
}