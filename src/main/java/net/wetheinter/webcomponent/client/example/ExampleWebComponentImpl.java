package net.wetheinter.webcomponent.client.example;

import elemental.dom.Element;

public class ExampleWebComponentImpl implements ExampleWebComponent {

  private int        test;
  private long       value;
  private char       character;
  private Element    element;
  private TestObject testObject;

  @Override
  public void receive(int receiveInt) {
    this.test = receiveInt;
  }

  // @Override
  public int test() {
    return test;
  }

  @Override
  public long getValue() {
    return value;
  }

  @Override
  public ExampleWebComponent setValue(long j) {
    this.value = j;
    return this;
  }

  @Override
  public ExampleWebComponent character(Character c) {
    this.character = c;
    return this;
  }

  @Override
  public Character character() {
    return character;
  }

  @Override
  public Element element() {
    return element;
  }

  @Override
  public ExampleWebComponent setAttribute(String name, String value) {
    element().setAttribute(name, value);
    return this;
  }

  @Override
  public String getAttribute(String name) {
    return element().getAttribute(name);
  }

  @Override
  public TestObject testObject() {
    return testObject;
  }

  @Override
  public ExampleWebComponent testObject(TestObject object) {
    this.testObject = object;
    return this;
  }
}
