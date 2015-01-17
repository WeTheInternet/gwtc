package xapi.polymer;

import static net.wetheinter.webcomponent.client.JsSupport.newElement;
import static net.wetheinter.webcomponent.client.JsSupport.setAttr;
import net.wetheinter.webcomponent.client.api.IsWebComponent;
import net.wetheinter.webcomponent.client.api.OnWebComponentAttributeChanged;
import net.wetheinter.webcomponent.client.api.ToStringIsName;
import net.wetheinter.webcomponent.client.api.WebComponent;
import net.wetheinter.webcomponent.client.api.WebComponentMethod;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;
import elemental.html.InputElement;

@JsType
@WebComponent(tagName=EnumPickerElement.TAG_NAME)
public interface EnumPickerElement <E extends Enum<E>> extends
IsWebComponent<Element>,
AbstractPickerElement<Element>,
OnWebComponentAttributeChanged
{

  String TAG_NAME = "xapi-enum-picker";
  // This type does not have a default factory, since we want to let subinterfaces define specific factories

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  E getValue();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  void setValue(E property);

  @Override
  default void onCreated(Element e) {
    initializePolymer("paper-radio-group");
  }

  @SuppressWarnings("unchecked")
  @WebComponentMethod(useJsniWildcard=true)
  default void render(E selected, E ... all) {
    Element group = getPolymer().element();
    for (E item : all) {
      // element is not actually an <input/>, but javascript doesn't care
      // and neither do we, since we can treat it just like an input
      InputElement radio = newElement("paper-radio-button");
      radio.setAttribute("name", item instanceof ToStringIsName ? item.toString() : item.name());
      radio.setAttribute("label", item.name());
      setAttr(radio, "toggles");
      group.appendChild(radio);
      radio.addEventListener("core-change", e-> maybeUpdate(radio, item), false);
    }
    setValue(selected);// Initialized the provided value
  }

  @WebComponentMethod(useJsniWildcard=true)
  default void maybeUpdate(InputElement radio, E item) {
    if (radio.isChecked()) {
      setValue(item);
    } else if (item == getValue()) {
      setValue(null);
    }
  }

  @Override
  default void onAttributeChanged(String name, String oldVal, String newVal) {
    // Used when the user manually sets <xapi-enum-picker>.value="newVal"
    // This will also be called whenever the radio group is updated via clicks,
    // however, this has no effect as the selected variable already equals newVal
    switch (name) {
    case "value":
      getPolymer().element().setAttribute("selected", newVal);
      break;
    }
  }
}
