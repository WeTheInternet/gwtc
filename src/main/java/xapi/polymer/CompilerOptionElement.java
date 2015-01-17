package xapi.polymer;

import static net.wetheinter.webcomponent.client.JsSupport.attributeExists;
import net.wetheinter.webcomponent.client.api.NativelySupported;

import com.google.gwt.core.client.js.JsType;

@JsType
public interface CompilerOptionElement {

  public default boolean isSet () {
    return attributeExists(this, "value");
  }

  @NativelySupported // By natively, we mean subinterfaces will define this as a js method
  String collect(String previous);

}
