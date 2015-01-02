package net.wetheinter.webcomponent.client.example;

import static net.wetheinter.webcomponent.client.JsSupport.console;
import net.wetheinter.webcomponent.client.api.IsWebComponent;
import net.wetheinter.webcomponent.client.api.NativelySupported;
import net.wetheinter.webcomponent.client.api.WebComponent;
import net.wetheinter.webcomponent.client.api.WebComponentMethod;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;

@JsType
@WebComponent(tagName = ExampleWebComponent.TAG_NAME)
public interface ExampleWebComponent extends IsWebComponent<Element> {

  String TAG_NAME = "example-web-component";

  public default void receive(int receiveInt) {
    console().log("RECEIVINATE! " + receiveInt + " : " + this);
  }

  @NativelySupported
  ExampleWebComponent setAttribute(String name, String value);

  @NativelySupported
  String getAttribute(String name);

  @JsProperty
  long getValue();

  @JsProperty
  ExampleWebComponent setValue(long j);

  @JsProperty
  ExampleWebComponent character(Character c);

  @JsProperty
  Character character();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  TestObject testObject();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  ExampleWebComponent testObject(TestObject object);
}
