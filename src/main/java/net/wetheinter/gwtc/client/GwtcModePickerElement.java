package net.wetheinter.gwtc.client;

import net.wetheinter.webcomponent.client.api.WebComponent;
import net.wetheinter.webcomponent.client.api.WebComponentFactory;
import xapi.polymer.EnumPickerElement;

import com.google.gwt.core.shared.GWT;

import elemental.dom.Element;

@WebComponent(tagName="gwtc-mode-picker")
public interface GwtcModePickerElement extends EnumPickerElement<GwtcMode> {

  WebComponentFactory<GwtcModePickerElement> NEW_GWTC_MODE_PICKER = GWT.create(GwtcModePickerElement.class);

  @Override
  default void onCreated(Element e) {
    afterCreated(el->{
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
    });
  }
}
