package net.wetheinter.webcomponent.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;

public class JsSupport {

  public static native Document doc()
  /*-{
		return $doc;
  }-*/;

  public static native JsObject object()
  /*-{
		return Object;
  }-*/;

  public static native Console console()
  /*-{
		return $wnd.console;
  }-*/;

  private native Window window()
  /*-{
		return $wnd;
  }-*/;

  public static native Byte boxByte(JavaScriptObject o)
  /*-{
		return typeof o === 'number' ? @java.lang.Byte::new(B)(~~o)
				: typeof o === 'string' ? @java.lang.Byte::new(Ljava/lang/String;)(o)
						: o;
  }-*/;

  public static native Short boxShort(JavaScriptObject o)
  /*-{
		return typeof o === 'number' ? @java.lang.Short::new(S)(~~o)
				: typeof o === 'string' ? @java.lang.Short::new(Ljava/lang/String;)(o)
						: o;
  }-*/;

  public static native Integer boxInteger(JavaScriptObject o)
  /*-{
		return typeof o === 'number' ? @java.lang.Integer::new(I)(~~o)
				: typeof o === 'string' ? @java.lang.Integer::new(Ljava/lang/String;)(o)
						: o;
  }-*/;

  public static native Float boxFloat(JavaScriptObject o)
  /*-{
		return typeof o === 'number' ? @java.lang.Float::new(F)(o)
				: typeof o === 'string' ? @java.lang.Float::new(Ljava/lang/String;)(o)
						: o;
  }-*/;

  public static native Double boxDouble(JavaScriptObject o)
  /*-{
		return typeof o === 'number' ? @java.lang.Double::new(D)(o)
				: typeof o === 'string' ? @java.lang.Double::new(Ljava/lang/String;)(o)
						: o;
  }-*/;

  public static native Character boxCharacter(JavaScriptObject o)
  /*-{
		return typeof o === 'number' ? @java.lang.Character::new(C)(o) :
		typeof o === 'string' ? @java.lang.Character::new(C)(o && o.charAt(0) || 0) : o;
  }-*/;

  public static native Boolean boxBoolean(JavaScriptObject o)
  /*-{
		if (typeof o === 'string') {
			o = o === 'true';
		}
		return typeof o === 'boolean' ? o ? @java.lang.Boolean::TRUE
				: @java.lang.Boolean::FALSE : o;
  }-*/;

  @UnsafeNativeLong
  public static native Long boxLong(JavaScriptObject o)
  /*-{
		if (typeof o === 'number') {
			o = ~~o + '';
		}
		if (typeof o === 'string') {
			return @java.lang.Long::new(Ljava/lang/String;)(o).@java.lang.Long::longValue()();
		}
		if (o.l === undefined) {
			// Can fail if o is not a number, but it will never fail if o is a primitive long
			return o.@java.lang.Number::longValue()();
		}
		return o;
  }-*/;

  public static native byte unboxByte(JavaScriptObject o)
  /*-{
		if (o === undefined) {
			return 0;
		}
		if (typeof o === 'string') {
			o = parseInt(o);
		}
		return typeof o === 'number' ? ~~o : o.@java.lang.Number::byteValue()();
  }-*/;

  public static native short unboxShort(JavaScriptObject o)
  /*-{
		if (o === undefined) {
			return 0;
		}
		if (typeof o === 'string') {
			o = parseInt(o);
		}
		return typeof o === 'number' ? ~~o
				: o.@java.lang.Number::shortValue()();
  }-*/;

  public static native int unboxInteger(JavaScriptObject o)
  /*-{
		if (o === undefined) {
			return 0;
		}
		if (typeof o === 'string') {
			o = parseInt(o);
		}
		return typeof o === 'number' ? ~~o : o.@java.lang.Number::intValue()();
  }-*/;

  public static native float unboxFloat(JavaScriptObject o)
  /*-{
		if (o === undefined) {
			return 0;
		}
		if (typeof o === 'string') {
			o = parseFloat(o);
		}
		return typeof o === 'number' ? o : o.@java.lang.Number::floatValue()();
  }-*/;

  public static native double unboxDouble(JavaScriptObject o)
  /*-{
		if (o === undefined) {
			return 0;
		}
		if (typeof o === 'string') {
			o = parseFloat(o);
		}
		return typeof o === 'number' ? o : o.@java.lang.Number::doubleValue()();
  }-*/;

  public static native char unboxCharacter(JavaScriptObject o)
  /*-{
		if (o === undefined) {
			return 0;
		}
		if (typeof o === 'string') {
			o = o && o.charAt(0) || 0;
		}
		return typeof o === 'number' ? o
				: o.@java.lang.Character::charValue()();
  }-*/;

  public static native boolean unboxBoolean(JavaScriptObject o)
  /*-{
		if (o === undefined) {
			return false;
		}
		return typeof o === 'boolean' ? o
				: typeof o === 'string' ? o === 'true'
						: o.@java.lang.Boolean::booleanValue()();
  }-*/;

  @UnsafeNativeLong
  public static native long unboxLong(JavaScriptObject o)
  /*-{
		if (o === undefined) {
			o = '0';
		}
		if (typeof o === 'number') {
			o = o.toString();
		}
		if (typeof o === 'string') {
			return @java.lang.Long::new(Ljava/lang/String;)(o).@java.lang.Long::longValue()();
		}
		if (o.l === undefined) {
			return o.@java.lang.Number::longValue()();
		}
		return o;
  }-*/;

  /**
   * Constructs a "fake number" that works in both GWT and javascript.
   *
   * This number has the format java expects: {l:0, m:0, h:0}, but also
   * overrides the .valueOf() function to act like a Long.doubleValue() in
   * javascript.
   *
   * Operations in javascript will lose precision due to the use of 52 bit
   * numbers, so if you need full long precision, use only methods originating
   * from java that use the object format.
   *
   * @param o
   *          -> A number in any format that can be expected from javascript
   *          (number, string, Long or long)
   * @return a hybrid long with shims to act like a javascript number.
   */
  @UnsafeNativeLong
  public static native long unboxLongNative(JavaScriptObject o)
  /*-{
		var javaLong = @net.wetheinter.webcomponent.client.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(o);
		var tricky = function() {
			return @java.lang.Long::new(J)(javaLong).@java.lang.Long::doubleValue()();
		}
		tricky.l = javaLong.l;
		tricky.m = javaLong.m;
		tricky.h = javaLong.h;
		tricky.valueOf = function() {
			return @java.lang.Long::new(J)(javaLong).@java.lang.Long::doubleValue()();
		}
		return tricky;
  }-*/;

}
