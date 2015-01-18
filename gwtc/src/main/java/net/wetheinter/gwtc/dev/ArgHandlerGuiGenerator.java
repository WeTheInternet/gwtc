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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import net.wetheinter.webcomponent.client.JsSupport;
import net.wetheinter.webcomponent.client.api.IsWebComponent;
import net.wetheinter.webcomponent.client.api.OnWebComponentCreated;
import net.wetheinter.webcomponent.client.api.WebComponent;
import net.wetheinter.webcomponent.client.api.WebComponentFactory;
import xapi.bytecode.BadBytecode;
import xapi.bytecode.ClassFile;
import xapi.bytecode.CodeIterator;
import xapi.bytecode.ConstPool;
import xapi.bytecode.MethodInfo;
import xapi.bytecode.NotFoundException;
import xapi.bytecode.api.Opcode;
import xapi.bytecode.attributes.CodeAttribute;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.polymer.BooleanPickerElement;
import xapi.polymer.CompilerOptionElement;
import xapi.polymer.EnumPickerElement;
import xapi.polymer.IntegerPickerElement;
import xapi.polymer.StringListPickerElement;
import xapi.polymer.StringPickerElement;
import xapi.time.api.Moment;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dev.ArgProcessorBase;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.LibraryCompiler;
import com.google.gwt.dev.codeserver.Options;
import com.google.gwt.dev.util.arg.OptionSetProperties;
import com.google.gwt.dev.util.arg.OptionSourceLevel;
import com.google.gwt.dev.util.arg.SourceLevel;

import elemental.dom.Element;
import elemental.js.util.JsArrayOf;
import elemental.js.util.JsArrayOfString;

public class ArgHandlerGuiGenerator {
  private static Pattern DIGITS = Pattern.compile("[0-9]+");

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

    final String
      sdm = Options.class.getName() + "$ArgProcessor",
      compile = Compiler.class.getName() + "$ArgProcessor",
      library = LibraryCompiler.class.getName()+ "$ArgProcessor";

    final ClassFile
      sdmCls = scan.findClass(sdm),
      compilerCls = scan.findClass(compile),
      libraryCls = scan.findClass(library);

    ArgHandlerCollection results = new ArgHandlerCollection();
    collectResults(results, sdmCls, scan);
    collectResults(results, compilerCls, scan);
    collectResults(results, libraryCls, scan);

    final Set<GeneratorOption> allOpts = new HashSet<>();
    for (String key : results.hierarchy.keySet()) {
      List<GeneratorOption> opts = results.optionMap.get(key);
      for (GeneratorOption opt : opts) {
        allOpts.add(opt);
      }
    }
    // Every ArgHandler type must become a web component!
    for (GeneratorOption opt : allOpts) {
      generateOption(opt, scan, target, pkg);
    }
    for (String key : results.hierarchy.keySet()) {
      generateCompositeType(results, key, target, pkg);
    }

    System.out.println("Finished generating web component interfaces in "+difference(now)+".");
    System.exit(0);
  }

  private static void generateCompositeType(ArgHandlerCollection results, String qualified, File target, String pkg) throws FileNotFoundException, IOException {
    String simpleName = toCompositeName(qualified);
    SourceBuilder<GeneratorOption> source = new SourceBuilder<>("public interface " + simpleName);
    source.setPackage(pkg);
    ClassBuffer out = source.getClassBuffer();

    String ele = out.addImport(Element.class);
    String component = out.addImport(IsWebComponent.class);
    String created = out.addImport(OnWebComponentCreated.class);
    String factory = out.addImport(WebComponentFactory.class);
    String gwt = out.addImport(GWT.class);
    String option = out.addImport(CompilerOptionElement.class);
    String array = out.addImport(JsArrayOf.class);
    String arrayType = array+"<"+option+">";

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
    Set<GeneratorOption> opts = new TreeSet<GeneratorOption>(new Comparator<GeneratorOption>(){
      @Override
      public int compare(GeneratorOption a, GeneratorOption b) {
        if (a.isDeprecated != b.isDeprecated) {
          return a.isDeprecated ? 1 : -1;
        }
        if (a.type == b.type) {
          return a.owner.getQualifiedName().compareTo(b.owner.getQualifiedName());
        } else {
          return a.type.ordinal() - b.type.ordinal();
        }
      }
    });
    String superType = qualified;
    while (superType != null) {
      opts.addAll(results.optionMap.get(superType));
      superType = results.hierarchy.get(superType);
      if ("CompositeBase".equals(superType)) {
        break;
      }
    }
    String type = out.addImport(WebComponent.class);
    out.addAnnotation("@"+type+"(tagName=\""+toTagName(qualified)+"\")");
    out.addAnnotation(JsType.class);

    MethodBuffer onCreated = out
        .createMethod("default void onCreated("+ele+" e)");
    onCreated.println(arrayType+" all = "+array+".create();");
    int i = 0;
    for (GeneratorOption opt : opts) {

      out.createMethod(opt.getWebComponentName() +" get"+opt.getFieldName())
         .makeAbstract()
         .addAnnotation(JsProperty.class);
      String setter = "set"+opt.getFieldName();
      out.createMethod("void "+setter+"("+opt.getWebComponentName()+" component)")
        .makeAbstract()
        .addAnnotation(JsProperty.class);

      onCreated
        .print(opt.getWebComponentName()+" i"+i+" = ")
        .print(out.addImportStatic(pkg+"."+opt.getWebComponentName()+"."+opt.getFactoryName()))
        .println(".newComponent();")
        .println(setter+"(i"+i+");")
        .println("all.push(i"+i+");")
        .println("e.appendChild(i"+i+".element());")
      ;
      i++;
    }
    onCreated.println("setOptions(all);");

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
      .returnValue("args")
    ;

    saveSource(source, target);
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
    StringBuilder b = new StringBuilder("gwtc-");
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

    out.addInterface(CompilerOptionElement.class);

    onCreated.print("setInstructions(");
    opt.purpose.print(onCreated);
    onCreated.println(");");

    onCreated.println("setTitle(\""+(opt.tag==null?"Modules to Compile":opt.tag)+"\");");

    performWorkarounds(opt);

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
      out.addInterface(BooleanPickerElement.class);
      if (opt.defaultValue!=null) {
        onCreated.print("setValue(");
        opt.defaultValue.print(onCreated);
        onCreated.println(");");
      }
      String tag = opt.tag.substring(1), start = "-";
      if (opt.isExperimental) {
        start = "-X";
        if (tag.charAt(0) == 'X') {
          tag = tag.substring(1);
        }
      }
      String disable = start+"no" + tag;
      tag = start + tag;
      collect.returnValue("(getValue() ? \""+tag+" \" : \""+disable+" \")+previous");
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
      if(opt.option.getName().equals(OptionSourceLevel.class.getName())) {
        opt.type = ArgHandlerType.ENUM;
        opt.enumType = SourceLevel.class.getName();
      } else if(opt.option.getName().equals(OptionSetProperties.class.getName())) {
        opt.type = ArgHandlerType.LIST;
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
