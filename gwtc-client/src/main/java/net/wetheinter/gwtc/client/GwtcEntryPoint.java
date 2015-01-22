package net.wetheinter.gwtc.client;

import static xapi.components.impl.JsSupport.console;
import static xapi.elemental.X_Elemental.injectCss;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import xapi.components.impl.WebComponentSupport;
import xapi.polymer.core.PolymerTheme;

public class GwtcEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // Allow consumers to override our web component support
    WebComponentSupport injector = GWT.create(WebComponentSupport.class);
    // Polyfill custom element support if necessary
    injector.ensureWebComponentApi(this::onWebComponentApiLoaded);
  }

  private void onWebComponentApiLoaded() {
    // Ensure our <xapi-gwtc /> tag is loaded by logging its factory
    console().log(CompositeXapiGwtc.NEW_COMPOSITE_XAPI_GWTC);
    // Inject our polymer theme
    injectCss(PolymerTheme.class);
  }

}
