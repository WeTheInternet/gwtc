package xapi.polymer;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;

/**
 * For elements that have boolean on/off state, but also wish to handle the null or "no choice made state",
 * it is preferable to use an {@link EnumPickerElement} with the {@link OnOff} enum type.
 *
 * @author James X Nelson (james@wetheinter.net)
 *
 */
@JsType
public interface OnOffPickerElement extends EnumPickerElement<OnOffPickerElement.OnOff>{

  static enum OnOff {
    ON, OFF
  }

  @Override
  default void onCreated(Element e) {
    render(null, OnOff.values());
  }

  @JsProperty
  String getOnString();

  @JsProperty
  void setOnString(String onString);

  @JsProperty
  String getOffString();

  @JsProperty
  void setOffString(String onString);

  @JsProperty
  String getNullString();

  @JsProperty
  void setNullString(String onString);

  default String stringValue() {
    OnOff value = getValue();
    if (value == null) {

    }
    if (value == OnOff.ON) {
      return getOnString();
    } else {
      return getOffString();
    }
  }

}
