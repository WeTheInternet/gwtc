package net.wetheinter.webcomponent.client;

import static xapi.components.impl.JsSupport.doc;
import static xapi.polymer.pickers.BooleanPickerElement.NEW_BOOLEAN_PICKER;
import static xapi.polymer.pickers.IntegerPickerElement.NEW_INT_PICKER;
import static xapi.polymer.pickers.ListItemElement.NEW_LIST_ITEM;
import static xapi.polymer.pickers.StringListPickerElement.NEW_STRING_LIST_PICKER;
import static xapi.polymer.pickers.StringPickerElement.NEW_STRING_PICKER;

import xapi.polymer.pickers.BooleanPickerElement;
import xapi.polymer.pickers.IntegerPickerElement;
import xapi.polymer.pickers.ListItemElement;
import xapi.polymer.pickers.StringListPickerElement;
import xapi.polymer.pickers.StringPickerElement;

public class WebComponentTest {

  public void createElements() {

    // TODO setup automated test

    IntegerPickerElement e = NEW_INT_PICKER.newComponent();
    e.setTitle("Int");
    e.setValue(12);
    e.setInstructions("instructions");
    doc().body().appendChild(e.element());

    BooleanPickerElement bool = NEW_BOOLEAN_PICKER.newComponent();
    bool.setTitle("Title");
    bool.setInstructions("iiii");
    bool.setValue(true);
    doc().body().appendChild(bool.element());

    StringPickerElement string = NEW_STRING_PICKER.newComponent();
    string.setTitle("Title");
    string.setInstructions("iiii");
    string.setLabel("testing!");
    doc().body().appendChild(string.element());

    ListItemElement array = NEW_LIST_ITEM.newComponent();
    array.addValue("one");
    array.addValue("two 2");
    array.addValue("three  3");
    doc().body().appendChild(array.element());

    StringListPickerElement listPicker = NEW_STRING_LIST_PICKER.newComponent();
    listPicker.addValue("list 1");
    listPicker.addValue("list  2");
    listPicker.addValue("  list  3  ");
    listPicker.setTitle("String list");
    doc().body().appendChild(listPicker.element());

  }
}
