package net.wetheinter.gwtc.dev;

import java.util.Set;

import xapi.dev.source.MemberBuffer;

class InvocationModel {
  String value;
  String classType;

  InvocationType type = InvocationType.String;

  InvocationModel next;
  InvocationModel tail;

  public void print(MemberBuffer<?> out)  {
    print(out, true);
  }
  private void print(MemberBuffer<?> out, boolean first)  {
    switch (type) {
    case Field:
      if (first) {
        out.print(out.addImport(classType));
      }
      first = false;
      out.print("."+value);
      break;
    case Method:
      if (first) {
        out.print(out.addImport(classType));
      }
      first = false;
      out.print("."+value+"()");
      break;
    case String:
      if (value != null) {
        out.print("\""+escape(value)+"\"");
      }
      first = true;
      break;
    case Local:
    case Boolean:
      first = true;
      out.print(value);
    }
    if (next != null) {
      out.println(" + ");
      next.print(out, first);
    }
  }

  private String escape(String value) {
    return value.replaceAll("\"", "\\\\\"");
  }

  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean escapeQuotes) {
    return toString(escapeQuotes, true);
  }
  public String toString(boolean escapeQuotes, boolean first) {
    switch (type) {
    case Field:
      return (first ? classType : "")+"."+value  + (next == null ? "" : next.toString(escapeQuotes, false));
    case Local:
    case Boolean:
      return value  + (next == null ? "" : next.toString(escapeQuotes, false));
    case Method:
      return (first ? classType : "")+"."+value + "()"+ (next == null ? "" : next.toString(escapeQuotes, false));
    case String:
      return maybeEscape(escapeQuotes) + (next == null ? "" : next.toString(escapeQuotes, false));
    default:
      throw new UnsupportedOperationException("Unsupported type: "+type);
    }
  }

  private String maybeEscape(boolean escapeQuotes) {
    return escapeQuotes ? "\""+escape(value) + "\"" : value ;
  }

  public static InvocationModel append(InvocationModel provider, String string) {
    return append(provider, null, string, InvocationType.String);
  }

  public static InvocationModel append(InvocationModel provider, String classType, String value, InvocationType type) {
    if (provider == null) {
      provider = new InvocationModel();
      provider.classType = classType;
      provider.value = value;
      provider.type = type;
      provider.tail = provider;
      return provider;
    } else {
      InvocationModel tail = new InvocationModel();
      tail.value = value;
      tail.type = type;
      tail.classType = classType;
      provider.tail.next = tail;// Update previous tail
      provider.tail = tail;// Update pointer to tail
      return provider;
    }
  }
  public void collectTypes(Set<String> foreignTypes) {
    InvocationModel node = this;
    while (node != null) {
      if (node.classType != null) {
        foreignTypes.add(node.classType);
      }
      node = node.next;
    }
  }
}