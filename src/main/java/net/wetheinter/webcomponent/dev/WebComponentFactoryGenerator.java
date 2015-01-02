package net.wetheinter.webcomponent.dev;

import static java.lang.reflect.Modifier.PRIVATE;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.wetheinter.webcomponent.client.JsSupport;
import net.wetheinter.webcomponent.client.JsoConsumer;
import net.wetheinter.webcomponent.client.JsoSupplier;
import net.wetheinter.webcomponent.client.WebComponentBuilder;
import net.wetheinter.webcomponent.client.WebComponentSupport;
import net.wetheinter.webcomponent.client.api.NativelySupported;
import net.wetheinter.webcomponent.client.api.WebComponent;
import net.wetheinter.webcomponent.client.api.WebComponentFactory;
import net.wetheinter.webcomponent.client.api.WebComponentMethod;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.util.collect.Sets;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

public class WebComponentFactoryGenerator extends IncrementalGenerator {

  private static class MethodData {

    private final String originalName;
    private String       name;
    private String       getterName;
    private String       setterName;
    private String       getterClass;
    private String       setterClass;
    private boolean      enumerable   = false;
    private boolean      configurable = true;
    private boolean      writeable    = false;
    public String        valueClass;
    public boolean       mapToAttribute;

    public MethodData(String name) {
      this.originalName = this.name = name;
    }

    private boolean isProperty() {
      return getterName != null || setterName != null;
    }

  }

  private static final Pattern BEAN_NAME = Pattern.compile("(is|get|set)(.+)");

  @Override
  public RebindResult generateIncrementally(TreeLogger logger,
      GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    JClassType type = context.getTypeOracle().findType(typeName);
    WebComponent component = getAndValidateWebComponentAnnotation(type, logger);
    String pkg = type.getPackage().getName();
    String simple = type.getSimpleSourceName();
    String factoryName = simple + "_WebComponentFactory";
    String qualifiedName = pkg + "." + factoryName;

    // TODO reenable this once we add strong hashing support to ensure types
    // have not changed.
    // if (context.tryReuseTypeFromCache(qualifiedName)) {
    // return new RebindResult(RebindMode.USE_ALL_CACHED, qualifiedName);
    // }

    PrintWriter pw = context.tryCreate(logger, pkg, factoryName);
    if (pw == null) {
      return new RebindResult(RebindMode.USE_EXISTING, qualifiedName);
    }

    SourceBuilder<JClassType> sourceBuilder = new SourceBuilder<JClassType>
      ("public final class " + factoryName)
        .setPackage(pkg);
    ClassBuffer out =
      sourceBuilder.getClassBuffer()
        .addInterface(
          WebComponentFactory.class.getCanonicalName() + "<" + simple + ">");
    String builder = out.addImport(WebComponentBuilder.class);
    String support = out.addImport(WebComponentSupport.class);
    String jso = out.addImport(JavaScriptObject.class);
    String proto = generatePrototypeAccessor(out, component.extendProto(), jso);

    Multimap<String, MethodData> methods = HashMultimap.create();
    for (JClassType iface : type.getImplementedInterfaces()) {
      generateFunctionAccessors(logger, context, iface, methods);
    }
    generateFunctionAccessors(logger, context, type, methods);

    String supplier = out.addImport(Supplier.class);
    out
      .createField(supplier + "<" + simple + ">", "ctor")
      .makeStatic()
      .makePrivate();
    // Initialize the web component in a static block
    out
      .println("static {")
      .indent()
      .println(builder + " builder = " + builder + ".create(" + proto + ");");
    for (MethodData method : methods.values()) {
      if (method.isProperty()) {
        out
          .print("builder.addProperty(")
          .println("\"" + method.name + "\", ");
        if (method.getterName != null) {
          String jsoSupplier = out.addImport(JsoSupplier.class);
          String cls = out.addImport(method.getterClass);
          out.println("new " + jsoSupplier + "(" + cls + "." + method.getterName + "()), ");
        } else {
          out.println("null, ");
        }
        if (method.setterName != null) {
          String jsoConsumer = out.addImport(JsoConsumer.class);
          String cls = out.addImport(method.setterClass);
          out.println("new " + jsoConsumer + "(" + cls + "." + method.setterName + "()), ");
        } else {
          out.println("null, ");
        }
        out
          .print(method.enumerable + ", ")
          .println(method.configurable + ");");
      } else {
        String shortName = out.addImport(method.valueClass);
        out
          .print("builder.addValue(")
          .print("\"" + method.name + "\", ")
          .print(shortName + "." + method.originalName + "(null),")
          .print(method.enumerable + ", ")
          .print(method.configurable + ", ")
          .println(method.writeable + ");");
      }
    }
    out
      .print("ctor = " + support + ".register(")
      .print("\"" + component.tagName() + "\"")
      .println(", builder.build());")

      .outdent()
      .println("}")
      .createMethod(
        "public " + type.getSimpleSourceName() + " createWebComponent()")
      .returnValue("ctor.get()");
    ;
    logger.log(Type.ERROR, "\n\n" + sourceBuilder);

    pw.println(sourceBuilder.toString());
    context.commit(logger, pw);

    return new RebindResult(RebindMode.USE_ALL_NEW, qualifiedName);
  }

  private void generateFunctionAccessors(TreeLogger logger,
      GeneratorContext context, JClassType iface,
      Multimap<String, MethodData> results) throws UnableToCompleteException {
    if (iface.getMethods().length == 0) {
      return;
    }
    String pkg = iface.getPackage().getName();
    String simple = iface.getSimpleSourceName();
    String result = simple + "_JsFunctionAccess";
    String qualified = pkg + "." + result;
    SourceBuilder<PrintWriter> source = null;
    // TODO reenable once we can check input source files for freshness
    // if (!context.tryReuseTypeFromCache(qualified)) {
    PrintWriter pw = context.tryCreate(logger, pkg, result);
    if (pw != null) {
      source = new SourceBuilder<PrintWriter>
        ("public final class " + result)
          .setPackage(pkg)
          .setPayload(pw);
      source.getClassBuffer().createConstructor(PRIVATE);
    }
    JClassType stringType = context.getTypeOracle().findType("java.lang.String");
    // }
    for (JMethod method : iface.getMethods()) {
      if (method.getAnnotation(NativelySupported.class) != null) {
        // Do not blow away natively supported methods!
        continue;
      }
      if (method.getEnclosingType() == iface) {
        Collection<MethodData> existing = results.get(qualified);
        MethodData data = new MethodData(method.getName());
        WebComponentMethod metaData =
          method.getAnnotation(WebComponentMethod.class);
        if (metaData != null) {
          if (!metaData.name().isEmpty()) {
            data.name = metaData.name();
          }
          data.mapToAttribute = metaData.mapToAttribute();
          data.configurable = metaData.configurable();
          data.enumerable = metaData.enumerable();
          data.writeable = metaData.writeable();
        }
        if (method.isDefaultMethod()) {
          data.valueClass = qualified;
          existing = Sets.add((Set<MethodData>) existing, data);
          results.putAll(qualified, existing);
          // A default method! Let's generate a method to extract a javascript
          // function that will correctly handle un/boxing when passing values.
          if (source != null) {
            generateDefaultFunctionAccessor(logger, method,
              source.getClassBuffer(), stringType);
          }
        } else {
          // An abstract method should be treated like a JsType method;
          // if it's a getter or a setter, try to use element attributes
          String debeaned = debean(method.getName());
          if (metaData == null || metaData.name().isEmpty()) {
            data.name = debeaned;
          }
          for (MethodData previous : results.values()) {
            if (previous.name.equals(data.name)) {
              if (previous.isProperty()) {
                data = previous;
              } else {
                logger.log(Type.ERROR, "Duplicate property definitions found for web component member with "
                  + "name [" + previous.name + "].  Conflict between " + previous.originalName + " and "
                  + data.originalName);
                throw new UnableToCompleteException();
              }
            }
          }
          JsProperty prop = method.getAnnotation(JsProperty.class);
          if (prop != null || isBeanFormat(method)) {
            // Explicitly a js property, or it looks like a bean. Lets wire it
            // up!
            if (method.getParameterTypes().length == 0) {
              // Getter
              data.getterName = "get_" + method.getName();
              data.getterClass = qualified;
              if (source != null) {
                generateGetter(logger, debeaned, data, method, source, stringType);
              }
            } else {
              // Setter
              data.setterName = "set_" + method.getName();
              data.setterClass = qualified;
              if (source != null) {
                generateSetter(logger, debeaned, data, method, source, stringType);
              }
            }
            existing = Sets.add((Set<MethodData>) existing, data);
            results.putAll(qualified, existing);
          } else {
            logger.log(Type.WARN, "Unable to generate web component implementation for "
              + method.getReadableDeclaration() +
              " of " + method.getEnclosingType().getQualifiedSourceName()
              + ".  The underlying method will only work correctly "
              + "if supplied by the underlying native element");
          }
        }
      }
    }
    if (source != null) {
      source.getPayload().println(source.toString());
      logger.log(Type.ERROR, source.toString());
      context.commit(logger, source.getPayload());
    }

  }

  private void generateSetter(TreeLogger logger, String debeaned, MethodData data, JMethod method,
      SourceBuilder<PrintWriter> source, JType stringType) {
    MethodBuffer out = source.getClassBuffer()
      .createMethod("public static JavaScriptObject set_" + method.getName())
      .makeJsni();
    String paramBoxing = maybeBox(logger, method.getParameterTypes()[0], false, out, stringType);
    boolean fluent = method.getReturnType() != JPrimitiveType.VOID;
    assert !fluent || isAssignableFrom(method.getReturnType(), method.getEnclosingType()) : "Cannot implement fluent method "
      + method.getJsniSignature();
    out
      .println("return function(i) {")
      .indent();
    if (data.mapToAttribute) {
      out
        .print("this.setAttribute('" + debeaned + "',")
        .print(paramBoxing + "i" + (paramBoxing.isEmpty() ? "" : ")"))
        .println(");");
    } else {
      out
        .print("this.__").print(debeaned)
        .print(" = ")
        .print(paramBoxing).print("i").print(paramBoxing.isEmpty() ? "" : ")")
        .println(";");
    }

    if (fluent) {
      out.println("return this;");
    }

    out
      .outdent()
      .println("}");

  }

  private boolean isAssignableFrom(JType returnType, JClassType enclosingType) {
    return returnType instanceof JClassType ? ((JClassType) returnType).isAssignableFrom(enclosingType) : false;
  }

  private void generateGetter(TreeLogger logger, String debeaned, MethodData data, JMethod method,
      SourceBuilder<PrintWriter> source, JType stringType) {
    MethodBuffer out = source.getClassBuffer()
      .createMethod("public static JavaScriptObject get_" + method.getName())
      .makeJsni();
    String paramBoxing = maybeBox(logger, method.getReturnType(), true, out, stringType);
    out
      .println("return function() {")
      .indent()
      .print("return ");
    if (data.mapToAttribute) {
      out.print(paramBoxing)
        // our boxing code will automatically handle primitive conversion
        .print("this.getAttribute('" + debeaned + "')")
        .print(paramBoxing.isEmpty() ? "" : ")");
    } else {
      out.print(paramBoxing)
        .print("this.__" + debeaned)
        .print(paramBoxing.isEmpty() ? "" : ")");
    }
    out
      .println(";")
      .outdent()
      .println("}");

  }

  private boolean isBeanFormat(JMethod method) {
    if (method.getParameterTypes().length == 0) {
      // This, if anything, is a getter.
      return method.getReturnType() != JPrimitiveType.VOID;
    }
    return method.getParameterTypes().length == 1;
  }

  private String debean(String name) {
    Matcher matcher = BEAN_NAME.matcher(name);
    if (matcher.matches()) {
      String match = matcher.group(2);
      return Character.toLowerCase(match.charAt(0)) + (match.length() > 0 ? match.substring(1) : "");
    }
    return name;
  }

  private static final String BOX_HELPER = "@" + JsSupport.class.getName()
                                           + "::";
  private static final String JSO_PARAM  =
                                           "Lcom/google/gwt/core/client/JavaScriptObject;";

  private void generateDefaultFunctionAccessor(TreeLogger logger,
      JMethod method, ClassBuffer cls, JType stringType) {
    final String qualified = method.getEnclosingType().getQualifiedSourceName();
    String typeName = cls.addImport(qualified);
    MethodBuffer out =
      cls
        .createMethod(
          "public static JavaScriptObject " + method.getName() + "()")
        .addParameters(typeName + " o")
        .makeJsni()
        .addImports(JavaScriptObject.class)
        .print("var func = o.@" + qualified + "::" + method.getName() + "(");
    StringBuilder params = new StringBuilder();
    Map<Character, String> boxers = new LinkedHashMap<>();
    char paramName = 'a';
    for (JType param : method.getParameterTypes()) {
      out.print(param.getJNISignature());
      if (params.length() > 0) {
        params.append(',');
      }
      params.append(paramName);
      String boxingPrefix = maybeBox(logger, param, false, out, stringType);
      if (boxingPrefix.isEmpty()) {
        boxers.put(paramName, Character.toString(paramName));
      } else {
        boxers.put(paramName, boxingPrefix + paramName + ")");
      }
      paramName++;
    }
    out
      .println(");")
      .println("return $entry(function(" + params + "){")
      .indent();
    String boxReturn = maybeBox(logger, method.getReturnType(), true, out, stringType);
    if (method.getReturnType() != JPrimitiveType.VOID) {
      // Non void return type; we may need to box/unbox the result
      out.println("return " + boxReturn);
    }
    boolean hasReturn = !boxReturn.isEmpty();
    if (hasReturn) {
      out.indent();
    }
    out
      .println("func(this");
    for (Character c : boxers.keySet()) {
      out.print(", ").println(boxers.get(c));
    }
    out.print(")");
    if (hasReturn) {
      out.println().outdent().print(")");
    }
    out.println(";");
    out
      .outdent()
      .println("});");
  }

  private String maybeBox(TreeLogger logger, JType type, boolean jsToJava, MethodBuffer out, JType stringType) {
    if (type.isPrimitive() == null) {
      // The type is not primitive. If it maps to the object form of a
      // primitive, we must box it if primitive
      switch (type.getQualifiedSourceName()) {
      case "java.lang.Long":
      case "java.lang.Boolean":
      case "java.lang.Byte":
      case "java.lang.Short":
      case "java.lang.Character":
      case "java.lang.Integer":
      case "java.lang.Float":
      case "java.lang.Double":
        return BOX_HELPER + "box" + type.getSimpleSourceName() + "("
          + JSO_PARAM + ")(";
      default:
        if (jsToJava && type instanceof JClassType) {
          JClassType asClass = (JClassType) type;
          try {
            JMethod fromString = asClass.getMethod("fromString", new JType[] { stringType });
            if (fromString.isStatic()) {
              // if fromString is static, we can just invoke it directly
              return "@" + asClass.getQualifiedSourceName() + "::fromString(Ljava/lang/String;)(";
            } else {
              // however, if fromString is instance level, we must construct a
              // new instance
              if (asClass.getConstructor(new JType[0]) == null) {
                logger.log(
                  Type.WARN,
                  "Found method fromString(String) in type "
                    + asClass.getQualifiedSourceName()
                    + ",  but could not use it for autoboxing because the method is "
                    + "instance level and there is no zero-arg constructor available "
                    + "to instantiate the given type");
              } else {
                // we only support 0-arg constructors here
                return "@" + asClass.getQualifiedSourceName() + "::new()().@"
                  + asClass.getQualifiedSourceName() + "::fromString(Ljava/lang/String;)(";
              }
            }
          } catch (NotFoundException ignored) {
            // If there is no fromString method,
            // we just don't perform any boxing.
          }
        }
      }
    } else {
      // The type is primitive, we must unbox whatever is given to us
      switch (type.isPrimitive()) {
      case BOOLEAN:
        return BOX_HELPER + "unboxBoolean(" + JSO_PARAM + ")(";
      case BYTE:
        return BOX_HELPER + "unboxByte(" + JSO_PARAM + ")(";
      case SHORT:
        return BOX_HELPER + "unboxShort(" + JSO_PARAM + ")(";
      case CHAR:
        return BOX_HELPER + "unboxCharacter(" + JSO_PARAM + ")(";
      case INT:
        return BOX_HELPER + "unboxInteger(" + JSO_PARAM + ")(";
      case LONG:
        out.addAnnotation(UnsafeNativeLong.class);
        if (jsToJava) {
          return BOX_HELPER + "unboxLongNative(" + JSO_PARAM + ")(";
        }
        return BOX_HELPER + "unboxLong(" + JSO_PARAM + ")(";
      case FLOAT:
        return BOX_HELPER + "unboxFloat(" + JSO_PARAM + ")(";
      case DOUBLE:
        return BOX_HELPER + "unboxDouble(" + JSO_PARAM + ")(";
      default:
      }
    }
    return "";
  }

  private String generatePrototypeAccessor(ClassBuffer out, String extendProto,
      String jso) {
    out
      .createMethod("private static native " + jso + " proto()")
      .setUseJsni(true)
      .println("return Object.create(" + extendProto + ".prototype);");
    return "proto()";
  }

  private WebComponent getAndValidateWebComponentAnnotation(JClassType type,
      TreeLogger logger) throws UnableToCompleteException {
    WebComponent component = type.getAnnotation(WebComponent.class);
    if (component == null) {
      logger.log(Type.ERROR, "Type " + type.getQualifiedSourceName()
        + " missing required annotation, " + WebComponent.class.getName());
      throw new UnableToCompleteException();
    }
    if (component.tagName().indexOf('-') == -1) {
      logger.log(Type.ERROR,
        "WebCompoenent for " + type.getQualifiedSourceName()
          + " has invalid tag name " + component.tagName() + "; "
          + "Custom elements must contain the - character");
      throw new UnableToCompleteException();
    }
    return component;
  }

  @Override
  public long getVersionId() {
    return 0;
  }

}
