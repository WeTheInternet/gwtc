package xapi.polymer;

import static net.wetheinter.webcomponent.client.JsFunctionSupport.mergeConsumer;
import static net.wetheinter.webcomponent.client.JsSupport.addClassName;
import static net.wetheinter.webcomponent.client.JsSupport.hideIfEmpty;
import static net.wetheinter.webcomponent.client.JsSupport.newElement;
import static xapi.polymer.PolymerInput.newInput;
import static xapi.polymer.PolymerLabel.newLabel;

import java.util.function.Consumer;

import net.wetheinter.webcomponent.client.api.IsWebComponent;
import net.wetheinter.webcomponent.client.api.OnWebComponentAttached;
import net.wetheinter.webcomponent.client.api.OnWebComponentCreated;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;

@JsType
public interface AbstractPickerElement <E extends Element> extends
IsWebComponent<E>,
OnWebComponentCreated<E>,
OnWebComponentAttached<E>{

  String pickerFieldTag();

  default String getTitle() {
    return getTitleElement().getInnerText();
  }

  default void setTitle(String title) {
    getTitleElement().setInnerText(title);
    hideIfEmpty(getTitleElement());
  }

  @JsProperty
  Element getTitleElement();

  @JsProperty
  void setTitleElement(Element title);

  default String getInstructions() {
    return getInstructionsElement().getInnerText();
  }

  default void setInstructions(String instructions) {
    getInstructionsElement().setInnerText(instructions);
    hideIfEmpty(getInstructionsElement());
  }

  @JsProperty
  Element getInstructionsElement();

  @JsProperty
  void setInstructionsElement(Element instructions);

  @JsProperty
  Element getLabelContainer();

  @JsProperty
  void setLabelContainer(Element e);

  @Override
  default void onCreated(Element element) {
    addClassName(element, "xapi");
    Element container = getLabelContainer();
    if (container == null) {
      container = shadowRoot();
    }
    Element title = newElement("h3");
    container.appendChild(title);
    setTitleElement(title);

    Element instructions = newElement("div");
    container.appendChild(instructions);
    setInstructionsElement(instructions);

    if (container != shadowRoot() && container.getParentElement() == null) {
      shadowRoot().appendChild(container);
    }

    Consumer<Element> callback = afterCreated();
    if (callback != null) {
      callback.accept(element);
    }
  }

  @JsProperty
  Consumer<Element> afterCreated();

  @JsProperty
  void afterCreated(Consumer<Element> callback);

  default void onAfterCreated(Consumer<Element> callback, boolean prepend) {
    Consumer<Element> existing = afterCreated();
    if (existing == null) {
      afterCreated(callback);
    } else if (prepend){
      afterCreated(mergeConsumer(callback, existing));
    } else {
      afterCreated(mergeConsumer(existing, callback));
    }
  }

  @Override
  default void onAttached(E element) {
    hideIfEmpty(getTitleElement());
    hideIfEmpty(getInstructionsElement());
  }

  default Element initializePolymer(String inputTag) {
    PolymerInput input = newInput().tagName(inputTag);
    PolymerLabel label = newLabel().input(input);
    Element el = label.build();
    shadowRoot().appendChild(el);
    setLabelContainer(el.querySelector(".label"));
    setPolymer((PolymerElement)el.querySelector(inputTag));
    setCoreLabel(el);
    return el;
  }

  @JsProperty
  Element getShadowRoot();

  @JsProperty
  void setShadowRoot(Element e);

  default Element shadowRoot() {
    // disabling shadow root for now as it doesn't really add any value...
//    Element shadow = getShadowRoot();
//    if (shadow == null) {
//      shadow = createShadowRoot(element());
//      setShadowRoot(shadow);
//    }
//    return shadow;
    return element();
  }

  @JsProperty
  Element getCoreLabel();

  @JsProperty
  void setCoreLabel(Element coreLabel);

  @JsProperty
  PolymerElement getPolymer();

  @JsProperty
  void setPolymer(PolymerElement element);

}
