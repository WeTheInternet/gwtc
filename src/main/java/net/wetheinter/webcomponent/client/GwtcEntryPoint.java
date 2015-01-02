package net.wetheinter.webcomponent.client;

import net.wetheinter.webcomponent.client.api.WebComponentFactory;
import net.wetheinter.webcomponent.client.example.ExampleWebComponent;
import net.wetheinter.webcomponent.client.example.TestObject;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;

public class GwtcEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // Allow consumers to override our web component support
    WebComponentSupport injector = GWT.create(WebComponentSupport.class);
    // Polyfill custom element support if necessary
    injector.ensureWebComponentApi(this::onWebComponentApiLoaded);
  }

  private void onWebComponentApiLoaded() {
    WebComponentFactory<ExampleWebComponent> factory = GWT.create(ExampleWebComponent.class);
    factory.createWebComponent();
    Document doc = JsSupport.doc();
    ExampleWebComponent element = doc.createElement(ExampleWebComponent.TAG_NAME);
    doc.body().appendChild(element.element());
    element.receive(123);
    invoke(element, 1);
    JsSupport.console().log("" + element.getAttribute("id"));

    TestObject o = new TestObject();
    o.val = 123;
    o.state = true;
    o.text = "test";
    JsSupport.console().log(element.testObject());
    element.testObject(o);
    JsSupport.console().log(element.testObject());
    JsSupport.console().log(element.testObject().equals(o));

  }

  private native void invoke(ExampleWebComponent example, int i)
  /*-{
		example.receive(i);
  }-*/;

}
