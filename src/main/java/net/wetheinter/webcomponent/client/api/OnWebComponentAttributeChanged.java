package net.wetheinter.webcomponent.client.api;

public interface OnWebComponentAttributeChanged extends WebComponentCallback {

  @NativelySupported
  void onAttributeChanged(String name, String oldVal, String newVal);

}
