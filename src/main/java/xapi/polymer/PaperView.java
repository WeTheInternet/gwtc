package xapi.polymer;

import net.wetheinter.webcomponent.client.api.HasElement;
import net.wetheinter.webcomponent.client.api.NativelySupported;

import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;

@JsType
public interface PaperView <E extends Element, P extends PaperView<E, P>> extends HasElement<E>{

  @NativelySupported
  P setAttribute(String name, String value);

  default P setAttr(String name) {
    return setAttribute(name, "");
  }

  @NativelySupported
  String getAttribute();
}
