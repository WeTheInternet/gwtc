/**
 *
 */
package net.wetheinter.gwtc.client;

import java.util.function.Supplier;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.event.dom.client.ClickEvent;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.EventSource;
import elemental.xml.XMLHttpRequest;

import xapi.components.api.IsWebComponent;
import xapi.components.api.NativelySupported;
import xapi.components.api.OnWebComponentAttached;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.components.impl.JsSupport;
import xapi.polymer.core.PolymerElement;

@JsType
@WebComponent(tagName=GwtcCompileButton.TAG_NAME)
public interface GwtcCompileButton extends
IsWebComponent<Element>,
OnWebComponentAttached<Element>
{

  WebComponentFactory<GwtcCompileButton> NEW_COMPILER_BUTTON = GWT.create(GwtcCompileButton.class);

  String TAG_NAME = "gwtc-compile-button";
  String DEFAULT_URL = GWT.getHostPageBaseURL()+"io";

  @JsProperty
  LogViewElement getLogView();

  @JsProperty
  void setLogView(LogViewElement logView);

  @JsProperty
  Supplier<String> getArgProvider();

  @JsProperty
  void setArgProvider(Supplier<String> argProvider);

  @JsProperty
  String getUrl();

  @JsProperty
  void setUrl(String url);

  @Override
  default void onAttached(Element element) {
    PolymerElement compiler = PolymerElement.newButtonRaised("Compile");
    compiler.onClick(this::compile);
    element().appendChild(compiler.element());
  }

  default void compile(ClickEvent ignored) {
    Supplier<String> supplier = getArgProvider();
    if (supplier == null)
      return;
    String args = supplier.get();

    final String url = getUrl() == null ? DEFAULT_URL : getUrl();
    XMLHttpRequest xhr = Browser.getWindow().newXMLHttpRequest();
    xhr.setOnreadystatechange(ev->{
      if (xhr.getReadyState() == XMLHttpRequest.DONE) {
        xhr.setOnreadystatechange(null);
        String id = xhr.getResponseText();
        EventSource io = Browser.getWindow().newEventSource(url+"?compile="+id);
        io.setOnmessage(onLogReceived(getLogView()));
        io.addEventListener("close", event-> {
          io.close();
        }, true);
        io.setOnopen(event->{
          JsSupport.console().log("Opening stream");
        });
        io.setOnerror(event->{
          JsSupport.console().log("Streaming error");
          JsSupport.console().log(event);
        });
      }
    });
    xhr.open("POST", url, true);
    xhr.send(args);
  }

  @NativelySupported
  static EventListener onLogReceived(LogViewElement logView) {
    return event -> {
      MessageEvent message = (MessageEvent) event;
      String[] lines = ((String)message.getData()).split("\n");
      LogViewElement logs = logView;
      boolean updateScroll = logs.shouldUpdateScroll();
      for (String line : lines) {
        logs.addLog(line);
      }
      if (updateScroll) {
        Element scroll = logs.getScrollContainer();
        scroll.setScrollTop(scroll.getScrollHeight());
      }
    };
  }

}
