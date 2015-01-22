/**
 *
 */
package net.wetheinter.gwtc.client;

import static xapi.ui.html.api.Style.AlignVertical.Top;
import static xapi.ui.html.api.Style.Display.InlineBlock;
import static xapi.ui.html.api.Style.UnitType.Em;

import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;

import elemental.dom.Element;

import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.components.impl.JsSupport;
import xapi.polymer.core.PaperHeaderPanel;
import xapi.polymer.core.PolymerElement;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.Style;
import xapi.ui.html.api.Style.Unit;

@JsType
@WebComponent(tagName=LogViewElement.TAG_NAME)
@Css(style=@Style(
    names=LogViewElement.TAG_NAME,
    width=@Unit(600),
    display=InlineBlock,
    marginLeft=@Unit(value=2, type=Em),
    verticalAign=Top
  ))
public interface LogViewElement extends
PaperHeaderPanel {

  WebComponentFactory<LogViewElement> NEW_LOG_VIEW = GWT.create(LogViewElement.class);

  String TAG_NAME = "xapi-log";

  @Override
  default void onCreated(Element e) {
    onAfterCreated(el->{
      setHeaderText("Compiler Log");
      PolymerElement button = PolymerElement.newButton("Clear");
      getHeaderPanel().appendChild(button.element());
      button.onClick(this::clear);
      e.getStyle().setDisplay("none");
    }, false);
  }

  default void addLog(String value) {
    element().getStyle().clearDisplay();
    Element existing = JsSupport.newElement("pre");
    getContentPanel().appendChild(existing);
    existing.setInnerText(value);
    // Now set a classname to match the [type] of the log
    int typeStart = value.indexOf('[')+1;
    if (typeStart > 0) {
      int typeEnd = value.indexOf(']', typeStart);
      if (typeEnd > -1) {
        String type = value.substring(typeStart, typeEnd).toLowerCase();
        // Just in case the log message doesn't start with [type], but does
        // have [some text in square brackets], we don't want to add a bunch
        // of classnames that may accidentally collide with other classes
        if (type.indexOf(' ') == -1) {
          existing.setClassName(type);
        }
      }
    }
  }

  default void clear(ClickEvent e) {
    getContentPanel().setInnerHTML("");
  }

  default boolean shouldUpdateScroll() {
    Element content = getScrollContainer();
    return content.getScrollTop() + content.getOffsetHeight() >= content.getScrollHeight() - 10;
  }

}
