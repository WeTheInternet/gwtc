package net.wetheinter.gwtc.client;

import com.google.gwt.core.shared.GWT;

import elemental.dom.Element;
import elemental.dom.NodeList;

import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.polymer.pickers.EnumPickerElement;

@WebComponent(tagName="gwtc-mode-picker")
public interface GwtcModePickerElement extends EnumPickerElement<GwtcMode> {

  WebComponentFactory<GwtcModePickerElement> NEW_GWTC_MODE_PICKER = GWT.create(GwtcModePickerElement.class);

  @Override
  default void onCreated(Element e) {
    afterCreated(el->{
      setTitle("Compile Mode");
      setInstructions("Select which compiler to use");
      GwtcMode current = getValue();
      if (current == null) {
        while (el != null) {
          if (el.hasAttribute("gwtcmode")) {
            current = GwtcMode.valueOf(el.getAttribute("gwtcmode"));
            break;
          }
          el = el.getParentElement();
        }
      }
      if (current == null) {
        current = GwtcMode.Production;
      }
      render(current, GwtcMode.values());
      NodeList inputs = e.querySelectorAll("paper-radio-button");
      for (int i = inputs.length(); i --> 0; ) {
        ((Element)inputs.item(i)).removeAttribute("toggles");
      }
    });
  }
}
