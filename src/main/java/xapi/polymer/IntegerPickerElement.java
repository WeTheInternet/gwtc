package xapi.polymer;

import static net.wetheinter.webcomponent.client.JsSupport.setAttr;
import net.wetheinter.webcomponent.client.api.IsWebComponent;
import net.wetheinter.webcomponent.client.api.OnWebComponentAttributeChanged;
import net.wetheinter.webcomponent.client.api.OnWebComponentCreated;
import net.wetheinter.webcomponent.client.api.WebComponent;
import net.wetheinter.webcomponent.client.api.WebComponentFactory;
import net.wetheinter.webcomponent.client.api.WebComponentMethod;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.shared.GWT;

import elemental.dom.Element;


@JsType
@WebComponent(tagName=IntegerPickerElement.TAG_NAME)
public interface IntegerPickerElement extends
IsWebComponent<Element>,
OnWebComponentAttributeChanged,
OnWebComponentCreated<Element>,
AbstractPickerElement<Element> {

  String TAG_NAME = "xapi-int-picker";
  WebComponentFactory<IntegerPickerElement> NEW_INT_PICKER = GWT.create(IntegerPickerElement.class);

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  int getValue();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  IntegerPickerElement setValue(int property);

  @Override
  default void onCreated(Element element) {
    initializePolymer("paper-slider");
    setAttr(getPolymer().element(), "editable");
    getPolymer().onCoreChange(e -> setValue(getPolymer().valueAsInt()));
  }

  default void setMax(int max) {
    getPolymer().element().setAttribute("max", Integer.toString(max));
  }

  default void setMin(int min) {
    getPolymer().element().setAttribute("min", Integer.toString(min));
  }

  @Override
  default void onAttributeChanged(String name, String oldVal, String newVal) {
    switch (name) {
    case "value":
      getPolymer().setValue(newVal);
      break;
    }
  }

}
