package net.wetheinter.webcomponent.client.api;

import net.wetheinter.webcomponent.client.WebComponentSupport;
import elemental.dom.Element;

public interface IsWebComponent<E extends Element> extends HasElement<E> {

  @Override
  default E element() {
    return WebComponentSupport.asElement(this);
  }

}
