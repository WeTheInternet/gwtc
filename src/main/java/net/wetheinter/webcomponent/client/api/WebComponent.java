package net.wetheinter.webcomponent.client.api;

public @interface WebComponent {

	String tagName();

	String extendProto() default "HTMLElement";
}
