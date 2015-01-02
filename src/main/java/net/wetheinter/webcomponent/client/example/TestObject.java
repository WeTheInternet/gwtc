package net.wetheinter.webcomponent.client.example;

public class TestObject {

  public long    val;
  public boolean state;
  public String  text = "";

  @Override
  public String toString() {
    return val + ":" + state + "?" + text;
  }

  public static TestObject fromString(String text) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    TestObject result = new TestObject();
    int index0 = text.indexOf(':');
    int index1 = text.indexOf('?');
    result.val = Long.parseLong(text.substring(0, index0));
    result.state = Boolean.parseBoolean(text.substring(index0 + 1, index1));
    result.text = index1 == text.length() ? "" : text.substring(index1 + 1);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TestObject) {
      TestObject o = (TestObject) obj;
      return val == o.val &&
        state == o.state &&
        (text == null ? o.text == null : text.equals(o.text));
    }
    return false;
  }
}
