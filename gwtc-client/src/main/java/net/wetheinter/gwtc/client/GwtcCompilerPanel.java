/**
 *
 */
package net.wetheinter.gwtc.client;

import elemental.dom.Element;

import xapi.components.api.WebComponent;
import xapi.polymer.core.PaperHeaderPanel;

@WebComponent(tagName="xapi-gwtc")
public interface GwtcCompilerPanel extends PaperHeaderPanel {

  @Override
  default public void onCreated(Element e) {
  }

}
