package net.wetheinter.webcomponent.client.api;

public @interface WebComponentMethod {

  String name() default "";

  boolean writeable() default false;

  boolean configurable() default true;

  boolean enumerable() default false;

  /**
   * @return true to have properties map to/from element attributes.
   */
  boolean mapToAttribute() default false;
}
