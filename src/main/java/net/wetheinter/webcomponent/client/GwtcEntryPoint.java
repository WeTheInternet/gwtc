package net.wetheinter.webcomponent.client;

import static net.wetheinter.gwtc.client.CompositeCompilerElement.NEW_COMPOSITE_COMPILER_ELEMENT;
import static net.wetheinter.gwtc.client.GwtcModePickerElement.NEW_GWTC_MODE_PICKER;
import static net.wetheinter.webcomponent.client.JsSupport.doc;
import static net.wetheinter.webcomponent.client.JsSupport.setAttr;
import static xapi.elemental.X_Elemental.injectCss;
import net.wetheinter.gwtc.client.CompositeCompilerElement;
import net.wetheinter.gwtc.client.GwtcModePickerElement;
import net.wetheinter.webcomponent.client.example.ExampleTheme;
import xapi.polymer.PolymerElement;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dev.util.arg.OptionJsInteropMode.Mode;
import com.google.gwt.dev.util.arg.SourceLevel;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.MessageEvent;
import elemental.html.EventSource;
import elemental.xml.XMLHttpRequest;

public class GwtcEntryPoint implements EntryPoint {

  private static final String IO_URL = GWT.getHostPageBaseURL()+"io";

  @Override
  public void onModuleLoad() {
    // Allow consumers to override our web component support
    WebComponentSupport injector = GWT.create(WebComponentSupport.class);
    // Polyfill custom element support if necessary
    injector.ensureWebComponentApi(this::onWebComponentApiLoaded);
  }

  private void onWebComponentApiLoaded() {

    final PolymerElement compileButton = PolymerElement.newButtonRaised("Compile");
    final CompositeCompilerElement compileOptions = NEW_COMPOSITE_COMPILER_ELEMENT.newComponent();
    final GwtcModePickerElement modePicker = NEW_GWTC_MODE_PICKER.newComponent();

    compileOptions.getModuleName().addValue("net.wetheinter.webcomponent.GwtcTest");
    compileOptions.getJsInteropMode().setValue(Mode.JS);
    compileOptions.getSourceLevel().setValue(SourceLevel.JAVA8);

    Element row = PolymerElement.newLayoutCenterHorizontal();
    setAttr(modePicker.element(), "flex");
    modePicker.getLabelContainer().getStyle().setDisplay("none");
    row.getStyle().setWidth("700px");
    row.appendChild(modePicker.element());
    row.appendChild(compileButton.element());
    doc().body().appendChild(row);
    doc().body().appendChild(compileOptions.element());

    compileButton.onClick(e->{
      String args = compileOptions.getArgs();

      XMLHttpRequest xhr = Browser.getWindow().newXMLHttpRequest();
      xhr.setOnreadystatechange(ev->{
        if (xhr.getReadyState() == XMLHttpRequest.DONE) {
          xhr.setOnreadystatechange(null);
          String id = xhr.getResponseText();
          EventSource io = Browser.getWindow().newEventSource(IO_URL+"?compile="+id);
          io.setOnmessage(event->{
            MessageEvent message = (MessageEvent) event;
            JsSupport.console().log(message);
          });
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
      xhr.open("POST", IO_URL, true);
      xhr.send(modePicker.getValue().name()+"::"+args);

    });

    injectCss(ExampleTheme.class);
  }



}
