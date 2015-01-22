package net.wetheinter.gwtc.dev;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;

import static xapi.time.X_Time.difference;
import static xapi.time.X_Time.now;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.wetheinter.gwtc.client.CompilerOptionElement;
import net.wetheinter.gwtc.client.GwtcCompileButton;
import net.wetheinter.gwtc.client.GwtcMode;
import net.wetheinter.gwtc.client.GwtcModePickerElement;
import net.wetheinter.gwtc.client.LogViewElement;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dev.ArgProcessorBase;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.LibraryCompiler;
import com.google.gwt.dev.codeserver.Options;
import com.google.gwt.dev.util.arg.SourceLevel;

import elemental.dom.Element;
import elemental.js.util.JsArrayOf;
import elemental.js.util.JsArrayOfString;

import xapi.bytecode.BadBytecode;
import xapi.bytecode.ClassFile;
import xapi.bytecode.CodeIterator;
import xapi.bytecode.ConstPool;
import xapi.bytecode.MethodInfo;
import xapi.bytecode.NotFoundException;
import xapi.bytecode.api.Opcode;
import xapi.bytecode.attributes.CodeAttribute;
import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentAttached;
import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.components.api.OnWebComponentCreated;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.components.api.WebComponentMethod;
import xapi.components.impl.JsSupport;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.polymer.core.PaperHeaderPanel;
import xapi.polymer.pickers.EnumPickerElement;
import xapi.polymer.pickers.IntegerPickerElement;
import xapi.polymer.pickers.OnOffPickerElement;
import xapi.polymer.pickers.StringListPickerElement;
import xapi.polymer.pickers.StringPickerElement;
import xapi.time.api.Moment;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.Style;
import xapi.ui.html.api.Style.Display;

public class ArgHandlerGuiGenerator {
  private static Pattern DIGITS = Pattern.compile("[0-9]+");
  private static String sdmProcessor;
  private static String productionProcessor;
  private static String libraryProcesor;

  public static void main(String ... args) throws BadBytecode, NotFoundException, IOException {
    Moment now = now();
    File target = new File(args.length > 0 ? args[0] : "../gwtc-client/target/generated-sources/xapi").getCanonicalFile();
    String pkg = args.length > 1 ? args[1] : "net.wetheinter.gwtc.client";
    ClasspathScanner scanner = X_Inject.instance(ClasspathScanner.class);
    ClasspathResourceMap scan = scanner
      .matchClassFiles(
        "com/google/gwt.*[/$a-zA-Z]+ArgProcessor[$a-zA-Z0-9]*[.]class",
        "com/google/gwt.*[/]Option[A-Z][$a-zA-Z]+[.]class",
        "com/google/gwt/dev/codeserver/Options.*[.]class",
        SourceLevel.class.getName().replace('.', '/') + ".*.class",
        "com/google/gwt.*[/]ArgHandler[A-Z][a-zA-Z]+[.]class",
        "com/google/gwt/core/ext/TreeLogger.class",
        "com/google/gwt/core/ext/TreeLogger[$]Type.class",
        "com/google/gwt/dev/jjs/JsOutputOption.class",
        "com/google/gwt/dev/js/JsNamespaceOption.class"
      )
      .scan(Thread.currentThread().getContextClassLoader());

      sdmProcessor = Options.class.getName() + "$ArgProcessor";
      productionProcessor = Compiler.class.getName() + "$ArgProcessor";
      libraryProcesor = LibraryCompiler.class.getName()+ "$ArgProcessor";

    final ClassFile
      sdmCls = scan.findClass(sdmProcessor),
      compilerCls = scan.findClass(productionProcessor),
      libraryCls = scan.findClass(libraryProcesor);

    ArgHandlerCollection results = new ArgHandlerCollection();
    collectResults(results, sdmCls, scan);
    collectResults(results, compilerCls, scan);
    collectResults(results, libraryCls, scan);

    final Set<GeneratorOption> allOpts = new HashSet<>();
    for (String key : results.hierarchy.keySet()) {
      List<GeneratorOption> opts = results.optionMap.get(key);
      for (GeneratorOption opt : opts) {
        performWorkarounds(opt);
        allOpts.add(opt);
      }
    }
    // Every ArgHandler type must become a web component!
    for (GeneratorOption opt : allOpts) {
      generateOption(opt, scan, target, pkg);
    }
    generateCompositeType(results, findOptions(results), "XapiGwtc", target, pkg);

    System.out.println("Finished generating web component interfaces in "+difference(now)+".");
    System.exit(0);
  }

  private static Collection<GeneratorOption> findOptions(ArgHandlerCollection results) {
    Comparator<GeneratorOption> byTypeAndName = new Comparator<GeneratorOption>(){
      @Override
      public int compare(GeneratorOption a, GeneratorOption b) {
        if (a.isDeprecated != b.isDeprecated)
          return a.isDeprecated ? 1 : -1;
        if (a.type == b.type)
          return a.getFieldName().compareTo(b.getFieldName());
        else
          return a.type.ordinal() > b.type.ordinal() ? 1 : -1;
      }
    };
    Set<GeneratorOption> opts = new TreeSet<GeneratorOption>(byTypeAndName);
    ArrayList<GeneratorOption> values = new ArrayList<>(results.optionMap.values());
    for (ArgHandlerType type : ArgHandlerType.values()) {
      values.stream()
            .filter(o -> !o.isDeprecated && o.type == type)
            .forEach(o -> opts.add(o));
    }
    return opts;
  }

  private static void generateCompositeType(ArgHandlerCollection results,
      Collection<GeneratorOption> opts, String qualified, File target, String pkg) throws FileNotFoundException, IOException {
    String simpleName = toCompositeName(qualified);
    SourceBuilder<GeneratorOption> source = new SourceBuilder<>("public interface " + simpleName);
    source.setPackage(pkg);
    ClassBuffer out = source.getClassBuffer();

    String ele = out.addImport(Element.class);
    String component = out.addImport(IsWebComponent.class);
    String created = out.addImport(OnWebComponentCreated.class);
    String attributeChanged = out.addImport(OnWebComponentAttributeChanged.class);
    String attached = out.addImport(OnWebComponentAttached.class);
    String headerPanel = out.addImport(PaperHeaderPanel.class);
    String factory = out.addImport(WebComponentFactory.class);
    String gwt = out.addImport(GWT.class);
    String option = out.addImport(CompilerOptionElement.class);
    String array = out.addImport(JsArrayOf.class);
    String arrayType = array+"<"+option+">";
    String cssAnno = out.addImport(Css.class);
    String styleAnno = out.addImport(Style.class);
    String tagName = toTagName(qualified);
    String display = out.addImport(Display.class);

    String css = "@"+cssAnno+"(style=@"+styleAnno+"(\n"+
      "  names=\""+tagName+"\",\n" +
      "  display="+display+".InlineBlock\n"+
    "))";

    out.addAnnotation(css);

    out.createMethod(arrayType + " getOptions()")
       .makeAbstract()
       .addAnnotation(JsProperty.class);

    out.createMethod("void setOptions("+arrayType +"array)")
       .makeAbstract()
       .addAnnotation(JsProperty.class);

    out
      .createField(factory+"<"+simpleName+">", GeneratorOption.toFactoryName(simpleName))
      .getInitializer()
      .println(gwt+".create("+simpleName+".class);");


    out.addInterface(component+"<"+ele+">");
    out.addInterface(created+"<"+ele+">");
    out.addInterface(attached+"<"+ele+">");
    out.addInterface(attributeChanged);
    out.addInterface(headerPanel);

    String type = out.addImport(WebComponent.class);
    out.addAnnotation("@"+type+"(tagName=\""+tagName+"\")");
    out.addAnnotation(JsType.class);

    out
        .createMethod("default void onCreated("+ele+" e)")
        .println("onAfterCreated(el->created(getContentPanel()), true);")
    ;
    MethodBuffer onCreated = out
        .createMethod("default void created("+ele+" e)");
    onCreated
      .println(arrayType+" all = "+array+".create();")
      .println("String attr;")
    ;
    int i = 0;
    String modePicker = out.addImport(GwtcModePickerElement.class);
    String compileButton = out.addImport(GwtcCompileButton.class);
    String logView = out.addImport(LogViewElement.class);
    String supplier = out.addImport(Supplier.class);

    out
        .createMethod("default void onAttached(Element e)")
        .println(compileButton+" button = "+compileButton+".NEW_COMPILER_BUTTON.newComponent();")
        .println(logView+" log = "+logView+".NEW_LOG_VIEW.newComponent();")
        .println("element().appendChild(log.element());")
        .println("button.setLogView(log);")
        .println(supplier+"<String> args = this::getArgs;")
        .println("button.setArgProvider(args);")
        .println("getHeaderPanel().appendChild(button.element());")
    ;


    printGetterSetterElement(modePicker, "Mode", out);
    onCreated
        .println(modePicker +" mode = "+modePicker+".NEW_GWTC_MODE_PICKER.newComponent();")
        .println("setModeElement(mode);")
        .println("attr = element().getAttribute(\"mode\");")
        .println("if (attr == null) {")
        .indentln("attr = \""+GwtcMode.Production.name()+"\";")
        .println("}")
        .println("mode.element().setAttribute(\"value\", attr);")
        .println("e.appendChild(mode.element());")
        .println("mode.addAttributeChangeHandler(m->applyMode(m));")
    ;

    MethodBuffer attrChanged = out
        .createMethod("default void onAttributeChanged(String name, String oldVal, String newVal)")
        .println("switch(name.toLowerCase()){")
        .indent()
        .println("case \"mode\":")
        .indentln("getModeElement().element().setAttribute(\"value\", newVal);")
        .indentln("break;")
    ;
    for (GeneratorOption opt : opts) {
      attrChanged
        .println("case \""+opt.getFieldName().toLowerCase()+ "\":")
        .indentln("get"+opt.getFieldName()+"Element().element().setAttribute(\"value\", newVal);")
        .indentln("break;")
      ;

      String setter = "set"+opt.getFieldName()+"Element";
      printGetterSetterElement(opt.getWebComponentName(), opt.getFieldName(), out);
      printGetterSetterValue(opt.type, opt.getFieldName(), out);

      onCreated
        .print(opt.getWebComponentName()+" i"+i+" = ")
        .print(out.addImportStatic(pkg+"."+opt.getWebComponentName()+"."+opt.getFactoryName()))
        .println(".newComponent();")
        .println(setter+"(i"+i+");")
        .println("all.push(i"+i+");")
        .println("e.appendChild(i"+i+".element());")
        .println("attr = element().getAttribute(\""+opt.getFieldName()+"\");")
        .println("if (attr != null) {")
        .indentln("i"+i+".element().setAttribute(\"value\", attr);")
        .println("}")
      ;
      i++;
    }
    onCreated.println("setOptions(all);");
    onCreated.println("applyMode(mode.getValue());");
    onCreated.println("setHeaderText(\"GWTC\");");
    attrChanged.outdent().println("}");

    out
      .createMethod("default String getArgs()")
      .println(arrayType+" options = getOptions();")
      .println("String args = \"\";")
      .println("for (int i = options.length(); i --> 0;) {")
      .indent()
      .println(option+" option = options.get(i);")
      .println("if (option.isSet()) {")
      .indentln("args = option.collect(args);")
      .println("}")
      .outdent()
      .println("}")
      .returnValue("getModeElement().getValue().name()+\"::\"+args")
    ;

    // Now we need to generate a .setMode() method to toggle the visibility
    // of supported elements
    String compileMode = out.addImport(GwtcMode.class);
    String predicate = out.addImport(Predicate.class);
    MethodBuffer setMode = out
        .createMethod("default void applyMode("+compileMode+" mode)")
        .println("final "+predicate+"<"+option+"> filter;")
        .println("switch(mode){")
        .indent()
        .println("case "+GwtcMode.Production.name()+":")
        .indentln("filter = this::filterProduction;")
        .indentln("break;")
        .println("case "+GwtcMode.Library.name()+":")
        .indentln("filter = this::filterLibrary;")
        .indentln("break;")
        .println("case "+GwtcMode.Superdev.name()+":")
        .indentln("filter = this::filterSuperdev;")
        .indentln("break;")
        .println("default:")
        .indentln("throw new UnsupportedOperationException(\"Unhandled mode: \"+mode);")
        .outdent()
        .println("}")
    ;

    MethodBuffer filterProduction = out
        .createMethod("default boolean filterProduction("+option+" option)");
    MethodBuffer filterLibrary = out
        .createMethod("default boolean filterLibrary("+option+" option)");
    MethodBuffer filterSuperdev = out
        .createMethod("default boolean filterSuperdev("+option+" option)");

    printPredicate(filterProduction, results, productionProcessor);
    printPredicate(filterLibrary, results, libraryProcesor);
    printPredicate(filterSuperdev, results, sdmProcessor);

    filterProduction.returnValue("false");
    filterLibrary.returnValue("false");
    filterSuperdev.returnValue("false");

    setMode
        .println(arrayType+" options = getOptions();")
        .println("for (int i = options.length(); i-->0;){")
        .indent()
        .println(option+" option = options.get(i);")
        .println("(("+ele+")option).getStyle().setDisplay(")
        .indentln("filter.test(option) ? \"block\" : \"none\");")
        .outdent()
        .println("}")
    ;

    saveSource(source, target);
  }

  private static void printGetterSetterElement(String webComponentName,
      String fieldName, ClassBuffer out) {
    out.createMethod(webComponentName +" get"+fieldName+"Element")
      .makeAbstract()
      .addAnnotation(JsProperty.class);

    out.createMethod("void set"+fieldName+"Element("+webComponentName+" component)")
      .makeAbstract()
      .addAnnotation(JsProperty.class);
  }

  private static void printGetterSetterValue(ArgHandlerType type, String fieldName, ClassBuffer out) {

    String webcomponent = out.addImport(WebComponentMethod.class);
    String anno = "@"+webcomponent+"(mapToAttribute=true)";
    out.createMethod("String get"+fieldName)
    .makeAbstract()
    .addAnnotation(anno)
    .addAnnotation(JsProperty.class);

    out.createMethod("void set"+fieldName+"(String component)")
    .makeAbstract()
    .addAnnotation(anno)
    .addAnnotation(JsProperty.class);
  }

  private static void printPredicate(MethodBuffer out,
      ArgHandlerCollection results, String type) {
    out.println("switch(option.tagName().toLowerCase()){").indent();
    while (type != null) {
      for (GeneratorOption opt : results.optionMap.get(type)) {
        out.println("case \""+opt.getTagName()+"\":");
      }
      type = results.hierarchy.get(type);
    }
    out
      .println("return true;")
      .outdent()
      .println("}");
  }

  private static String toCompositeName(String qualified) {
    return "Composite"+toSimpleName(qualified).replace("$", "Element");
  }

  private static String toSimpleName(String qualified) {
    int end = qualified.lastIndexOf('.');
    return qualified.substring(end + 1).replace("ArgProcessor", "");
  }

  private static void saveSource(SourceBuilder<?> source, File target) throws IOException {
    target = new File(target, source.getPackage().replace('.', File.separatorChar));
    if (!target.exists()) {
      target.mkdirs();
    }
    File f = new File(target, source.getSimpleName()+".java");
    if (!f.exists()) {
      f.createNewFile();
    }
    InputStream contents = X_IO.toStreamUtf8(source.toString());
    try (
        FileOutputStream fOut = new FileOutputStream(f)
        ) {
      X_IO.drain(fOut, contents);
    }
  }

  private static String toTagName(String key) {
    key = toSimpleName(key.replace("$", "Composite"));
    key = toLowerCase(key.charAt(0)) + key.substring(1);
    StringBuilder b = new StringBuilder(key.toLowerCase().contains("gwtc") ? "" : "gwtc-");
    for (int i = 0; i < key.length(); i++) {
      if (isUpperCase(key.charAt(i))) {
        b.append('-');
        b.append(toLowerCase(key.charAt(i)));
      } else {
        b.append(key.charAt(i));
      }
    }
    return b.toString();
  }

  private static void generateOption(GeneratorOption opt, ClasspathResourceMap scan, File target, String pkg) throws IOException {
    String simpleName = opt.getWebComponentName();
    SourceBuilder<GeneratorOption> source = new SourceBuilder<>("public interface " + simpleName);
    source.setPackage(pkg);
    ClassBuffer out = source.getClassBuffer();
    String ele = out.addImport(Element.class);

    // Lets add our annotations
    String type = out.addImport(WebComponent.class);
    out.addAnnotation("@"+type+"(tagName=\""+opt.getTagName()+"\")");
    out.addAnnotation(JsType.class);

    // Now add common interfaces
    type = out.addImport(OnWebComponentCreated.class);
    out.addInterface(type+"<"+ele+">");
    type = out.addImport(IsWebComponent.class);
    out.addInterface(type+"<"+ele+">");

    source.setPayload(opt);

    String factory = out.addImport(WebComponentFactory.class);
    String gwt = out.addImport(GWT.class);
    out
      .createField(factory+"<"+simpleName+">", opt.getFactoryName())
      .getInitializer()
      .println(gwt+".create("+simpleName+".class);");

    String fieldName = opt.getFieldName();
    MethodBuffer onCreated = out
        .createMethod("default void onCreated("+ele+" e)")
        .println("afterCreated(el->{")
        .indent();

    out.createMethod("default String tagName()")
       .returnValue("\""+opt.getTagName()+"\"");

    out.addInterface(CompilerOptionElement.class);

    onCreated.print("setInstructions(");
    opt.purpose.print(onCreated);
    onCreated.println(");");

    onCreated.println("setTitle(\""+(opt.tag==null?"Modules to Compile":opt.tag)+"\");");

    MethodBuffer collect = out
        .createMethod("default String collect(String previous)")
        .addAnnotation(Override.class);

    switch (opt.type) {
    case EXTRA:

      // This is for module name, and it is a special case. There are two variants:
      // The standalone type using OptionModuleName, and the messier CodeServer variant,
      // which uses the conglomerate c.g.g.dev.codeserver.Options object.
      // For both cases, there is no flag, and the argument consumes as many strings as you give it.
      createArrayOfStringBean(out, fieldName);

      onCreated.println("setJoiner(\" \");");

      String arrayType = collect.addImport(JsArrayOfString.class);
      collect.println(arrayType + " value = getValue();");
      collect.returnValue("previous + \" \"+ value.join(getJoiner())");
      break;
    case LIST:
      createArrayOfStringBean(out, fieldName);
      onCreated.println("setJoiner(\" :: \");");
      String interleave = collect.addImportStatic(JsSupport.class, "interleave");
      collect.returnValue(interleave+"(getValue(),\""+opt.tag+"\") + previous");
      break;
    case BOOLEAN:
      out.addInterface(OnOffPickerElement.class);
      // For now, we are going to disable this, as it does not add any value
//      if (opt.defaultValue!=null) {
//        onCreated.print("if (");
//        opt.defaultValue.print(onCreated);
//        onCreated.println(") {");
//        onCreated.indentln("setOn();");
//        onCreated.println("} else {");
//        onCreated.indentln("setOff();");
//        onCreated.println("}");
//      }
      String tag = opt.tag.substring(1), start = "-";
      if (opt.isExperimental) {
        start = "-X";
        if (tag.charAt(0) == 'X') {
          tag = tag.substring(1);
        }
      }
      String disable = start+"no" + tag;
      tag = start + tag;
      collect.returnValue("(on() ? \""+tag+" \" : off() ? \""+disable+" \" : \"\")+previous");
      break;
    case ENUM:
      ClassFile enumClass = scan.findClass(opt.enumType);
      if (enumClass == null) {
        System.err.println("Unable to load "+opt.enumType+" without an extra search. "
          + "Please update the initial search pattern for "+ArgHandlerGuiGenerator.class.getName()+", "
          + "as we must now re-scan the entire classpath to find this one class.");
        ClasspathScanner scanner = X_Inject.instance(ClasspathScanner.class);
        scan = scanner
            .matchClassFiles(opt.enumType.replace('.', '/').replace("$", "[$]")+"[.]class")
            .scan(Thread.currentThread().getContextClassLoader());
        enumClass = scan.findClass(opt.enumType);
      }
      String enumType = out.addImport(opt.enumType);
      String enumField = out.addImport(EnumPickerElement.class);
      out.addInterface(enumField+"<"+enumType+">");
      onCreated.println("render(null, "+enumType+".values());");
      collect.returnValue("\""+opt.tag+" \" + getValue()+\" \"+previous");
      break;
    case INT:
      out.addInterface(IntegerPickerElement.class);
      switch (opt.tag) {
      // For int types, we want to set some bounds
      case "-port":
        onCreated.println("setMax(65535);");
        break;
      case "-optimize":
        onCreated.println("setMax(9);");
        break;
      case "-compileTestRecompiles":
      case "-localWorkers":
        onCreated.println("setMax(32);");
        break;
      }
      collect.returnValue("\""+opt.tag+" \" + getValue()+\" \"+previous");
      break;
    case DIRECTORY:
    case FILE:
    case STRING:
      out.addInterface(StringPickerElement.class);
      collect.returnValue("\""+opt.tag+" \" + getValue()+\" \"+previous");
    }

    onCreated
      .outdent()
      .println("});");
    saveSource(source, target);
  }

  private static void performWorkarounds(GeneratorOption opt) {
    if (opt.option != null) {
      if(opt.getFieldName().equals("SourceLevel")) {
        opt.type = ArgHandlerType.ENUM;
        opt.enumType = SourceLevel.class.getName();
      } else if(opt.getFieldName().equals("SetProperties")) {
        opt.type = ArgHandlerType.LIST;
      } else if(opt.getFieldName().equals("LauncherDir")) {
        opt.type = ArgHandlerType.STRING;
      }
    }
  }

  private static void createArrayOfStringBean(ClassBuffer out, String prop) {
    out.addInterface(StringListPickerElement.class);
    String splitter = out.addImportStatic(JsSupport.class, "split");

    out
      .createMethod("default String get"+prop+"()")
      .returnValue("getValue().join(getJoiner());");
    out
      .createMethod("default void set"+prop+"(String modules)")
      .println("setValue("+splitter+"(modules, getJoiner()));");
  }

  private static String titleCase(String fieldName) {
    return Character.toUpperCase(fieldName.charAt(0))+fieldName.substring(1);
  }

  private static void collectResults(ArgHandlerCollection result, ClassFile cls, ClasspathResourceMap scan)
      throws BadBytecode, NotFoundException {
    for (MethodInfo method : cls.getMethods()) {
      if (!"<init>".equals(method.getName())) {
        continue; // only interested in ArgProcessor constructors
      }
      CodeAttribute codeAttr = method.getCodeAttribute();
      ConstPool cp = codeAttr.getConstPool();
      CodeIterator iterator = new CodeIterator(codeAttr);
      int peek = iterator.lookAhead();
      int skip = iterator.skipConstructor();
      if (peek != skip) {
        String superClass = cls.getSuperclass();
        ClassFile sup = scan.findClass(superClass);
        if (sup == null) {
          System.out.println("Skipping missing superclass: " + superClass);
          result.add(cls, (ClassFile) null);
        } else {
          boolean shouldRecurse = !result.contains(sup) && !superClass.equals(ArgProcessorBase.class.getName());
          result.add(cls, sup);
          if (shouldRecurse) {
            collectResults(result, sup, scan);
          }
        }
      }

      while (iterator.hasNext()) {
        int index = iterator.next();
        int opcode = iterator.byteAt(index);
        switch (opcode) {
        case Opcode.NEW: // Looking for calls to new ArgHandler...();
          String argHandler = cp.getClassInfo(iterator.u16bitAt(index + 1));
          ClassFile argHandlerCls = scan.findClass(argHandler);
          if (argHandlerCls == null) {
            System.err.println("ERROR: Unable to load arg handler class: " + argHandler);
          } else {
            if (DIGITS.matcher(argHandlerCls.getSimpleName()).matches()) {
              if ("java.lang.Object".equals(argHandlerCls.getSuperclass())) {
                // This is an anonymous instance of an Options class, which we
                // want to ignore.
                continue;
              }
              argHandlerCls = scan.findClass(argHandlerCls.getSuperclass());
            }
            result.add(cls.getName(), argHandlerCls, scan);
          }
          break;
        case Opcode.INVOKESPECIAL:
        case Opcode.INVOKEVIRTUAL:
        case Opcode.INVOKEINTERFACE:
        case Opcode.DUP:
        case Opcode.ALOAD_0:
        case Opcode.ALOAD_1:
        case Opcode.RET:
        case Opcode.RETURN:
        case Opcode.ACONST_NULL:
          // Ignore these opcodes
          break;
        default:
          System.out.println("unhandled code " + opcode);
        }
      }
    }
  }
}
