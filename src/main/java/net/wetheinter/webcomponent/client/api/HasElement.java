package net.wetheinter.webcomponent.client.api;

import elemental.dom.Element;

public interface HasElement<E extends Element> {

  // We don't use .getElement() to avoid colliding w/ existing GWT apis
  @NativelySupported
  E element();

}
