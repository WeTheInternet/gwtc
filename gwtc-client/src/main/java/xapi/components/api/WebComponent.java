package xapi.components.api;

public @interface WebComponent {

	String tagName();

	String extendProto() default "HTMLElement";
}
