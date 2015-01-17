package net.wetheinter.webcomponent.client;

import static net.wetheinter.gwtc.client.CompositeCompilerElement.NEW_COMPOSITE_COMPILER_ELEMENT;
import static net.wetheinter.gwtc.client.GwtcModePickerElement.NEW_GWTC_MODE_PICKER;
import static net.wetheinter.webcomponent.client.JsSupport.doc;
import static net.wetheinter.webcomponent.client.JsSupport.setAttr;
import static xapi.elemental.X_Elemental.injectCss;
import net.wetheinter.gwtc.client.CompositeCompilerElement;
import net.wetheinter.gwtc.client.GwtcModePickerElement;
import net.wetheinter.webcomponent.client.example.ExampleTheme;
import net.wetheinter.webcomponent.client.io.GwtcIO;
import net.wetheinter.webcomponent.client.io.GwtcIOAsync;
import xapi.polymer.PolymerElement;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dev.util.arg.OptionJsInteropMode.Mode;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;

import elemental.dom.Element;

public class GwtcEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // Allow consumers to override our web component support
    WebComponentSupport injector = GWT.create(WebComponentSupport.class);
    // Polyfill custom element support if necessary
    injector.ensureWebComponentApi(this::onWebComponentApiLoaded);
  }

  private void onWebComponentApiLoaded() {
    final GwtcIOAsync async = GWT.create(GwtcIO.class);
    // Modify the RPC service to point to the host page server instead of the GWT codeserver.
    ((RemoteServiceProxy)async).setServiceEntryPoint(com.google.gwt.core.client.GWT.getHostPageBaseURL()+"gwtc");

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
      async.compile("meow", args, new AsyncCallback<String>() {
        @Override
        public void onSuccess(String result) {
          JsSupport.console().log(result);
        }

        @Override
        public void onFailure(Throwable caught) {
          JsSupport.console().log(caught);
        }
      });
    });

    injectCss(ExampleTheme.class);
  }



}
