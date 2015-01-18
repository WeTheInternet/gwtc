package net.wetheinter.gwtc.dev;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xapi.bytecode.ClassFile;

class GeneratorOption {

  private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");

  public GeneratorOption(ClassFile owner) {
    this.owner = owner;
  }

  public ClassFile owner;
  public ClassFile option;
  public InvocationModel purpose;
  public boolean handlesMultiples;
  public ArgHandlerType type;
  public String enumType;
  public String tag;
  public InvocationModel defaultArg;
  public InvocationModel defaultValue;
  public boolean isExperimental;
  public boolean isUndocumented;
  public boolean isRequired;
  public boolean isDeprecated;

  @Override
  public int hashCode() {
    return owner.getName().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof GeneratorOption && ((GeneratorOption)obj).owner.getName().equals(owner.getName());
  }

  public String getSimpleName() {
    return owner.getSimpleName();
  }

  public String getFieldName() {
    return toFieldName(owner == null ? tag : owner.getSimpleName());
  }

  private String toFieldName(String field) {
    if (field.charAt(0)=='-') {
      field = field.substring(1);
      if (field.charAt(0) == 'X') {
        field = field.substring(1);
      }
    }
    field = field.replace("ArgHandler", "");
    field = field.replace("Argument", "");
    return Character.toUpperCase(field.charAt(0)) + field.substring(1);
  }

  public String getTagName() {
    return "gwtc-"+toTagName(owner.getSimpleName());
  }

  private String toTagName(String string) {
    Matcher matcher = UPPERCASE.matcher(string);
    StringBuilder b = new StringBuilder();
    int prev = -1;
    while(matcher.find()) {
      if (prev != -1) {
        b.append(string.substring(prev+1, matcher.start()));
        b.append('-');
      }
      prev = matcher.start();
      b.append(Character.toLowerCase(string.charAt(prev)));
    }
    b.append(string.substring(prev+1));
    return b.toString();
  }

  public String getFactoryName() {
    String string = owner.getSimpleName().replace("ArgHandler", "");
    return toFactoryName(string);
  }

  static String toFactoryName(String string) {
    Matcher matcher = UPPERCASE.matcher(string);
    StringBuilder b = new StringBuilder("NEW_");
    int prev = -1;
    while(matcher.find()) {
      if (prev != -1) {
        b.append(string.substring(prev+1, matcher.start()).toUpperCase());
        b.append('_');
      }
      prev = matcher.start();
      b.append(string.charAt(prev));
    }
    b.append(string.substring(prev+1).toUpperCase());
    return b.toString();
  }

  public String getWebComponentName() {
    return getSimpleName().replace("ArgHandler", "") + "Element";
  }
}