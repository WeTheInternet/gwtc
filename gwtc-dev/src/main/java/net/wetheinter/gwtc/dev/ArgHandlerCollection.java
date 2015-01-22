package net.wetheinter.gwtc.dev;

import static net.wetheinter.gwtc.dev.InvocationModel.append;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xapi.bytecode.BadBytecode;
import xapi.bytecode.ClassFile;
import xapi.bytecode.CodeIterator;
import xapi.bytecode.ConstPool;
import xapi.bytecode.Descriptor;
import xapi.bytecode.FieldrefInfo;
import xapi.bytecode.FloatInfo;
import xapi.bytecode.IntegerInfo;
import xapi.bytecode.InterfaceMethodrefInfo;
import xapi.bytecode.MethodInfo;
import xapi.bytecode.MethodrefInfo;
import xapi.bytecode.NotFoundException;
import xapi.bytecode.StringInfo;
import xapi.bytecode.api.Opcode;
import xapi.bytecode.attributes.CodeAttribute;
import xapi.bytecode.attributes.SignatureAttribute;
import xapi.dev.scanner.impl.ClasspathResourceMap;

import com.google.gwt.dev.codeserver.Options;
import com.google.gwt.dev.util.arg.ArgHandlerDumpSignatures;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.ListMultimap;

class ArgHandlerCollection {

    static final String CODESERVER_OPTIONS = Options.class.getName();
    ListMultimap<String, GeneratorOption> optionMap = ArrayListMultimap.create();
    Map<String, String> hierarchy = new HashMap<>();
    Set<GeneratorOption> needsRescan = new HashSet<>();
    static Set<String> ignorableArgHandlers = ImmutableSet.<String>builder()
    .add(ArgHandlerLogLevel.class.getName())
    .add(ArgHandlerDumpSignatures.class.getName())
    .build();

    public void add(String container, ClassFile argHandler, ClasspathResourceMap scan) throws NotFoundException, BadBytecode {
      GeneratorOption handler = new GeneratorOption(argHandler);
      optionMap.get(container).add(handler);

      handler.type = findType(argHandler, scan);
      handler.handlesMultiples = handler.type == ArgHandlerType.EXTRA;

      for (MethodInfo method : extractMethods(argHandler, scan)) {
        String prefix = "";
        switch(method.getName()) {
        case "<init>":
          String[] params = Descriptor.getParameterTypeNames(method.getDescriptor());
          if (params.length == 1) {
            ClassFile param = scan.findClass(params[0]);
            handler.option = param;
          } else {
            if (params.length > 0 && CODESERVER_OPTIONS.equals(params[0])) {
              handler.option = scan.findClass(params[0]);
            } else {
              if (!ignorableArgHandlers.contains(argHandler.getName())) {
                System.out.println("Non-ignorable handler: ("+argHandler.getQualifiedName()+".java:1) "+method.getDescriptor());
              }
            }
          }
        case "<clinit>":
          break;
        case "getPurpose":
        case "getPurposeSnippet":
          handler.purpose = getReturnedString(method);
          break;
        case "getLabel":
          prefix = "-";
        case "getTag":
          handler.tag = prefix + getReturnedString(method).toString(false);
          if (handler.tag.startsWith("-X")) {
            handler.isExperimental = true;
          }
          assert handler.tag != null;
          break;
        case "getDefaultArgs":
          handler.defaultArg = extractSimpleInvocation(argHandler, method);
          break;
        case "getDefaultValue":
          InvocationModel defaultValue = extractSimpleInvocation(argHandler, method);
          if (defaultValue != null && defaultValue.type == InvocationType.Boolean) {
            // We only record the default values that are hardcoded to boolean true/false.
            // For everything else, we want to allow for true/false/null.
            handler.defaultValue = defaultValue;
          }
        case "getTags":// Only used by LauncherDir to add an alias to -war...
        case "getTagArgs": // Going to ignore this as it's not really necessary
          break;
        case "isExperimental":
          handler.isExperimental = toBoolean(extractSimpleInvocation(argHandler, method));
          break;
        case "isRequired":
          handler.isRequired = toBoolean(extractSimpleInvocation(argHandler, method));
          break;
        case "isUndocumented":
          handler.isUndocumented = toBoolean(extractSimpleInvocation(argHandler, method));
          break;
        // Ignore all these setters as we don't care about what the ArgHandler does with the args
        case "setValue":
        case "setFlag":
        case "setString":
        case "setFile":
        case "setInt":
        case "setDir":
        case "addExtraArg":
          break;
        case "handle":
          handler.handlesMultiples = true;
          break;
        default:
          System.err.println("Unhandled method: "+method.getName());
        }
      }
      if (handler.type == ArgHandlerType.ENUM) {
        SignatureAttribute sigAttr = (SignatureAttribute) argHandler.getAttribute(SignatureAttribute.tag);
        String attr = sigAttr.getSignature();
        handler.enumType = attr.substring(attr.indexOf('<')+2, attr.indexOf(';')).replace('/', '.');
        ClassFile enumType = scan.findClass(handler.enumType);
        if (enumType == null) {
          System.out.println("WARN: Enum type "+handler.enumType+" was not pre-scanned.\n"
            + "It will be manually rescanned, though this will hurt performance."
            + "Consider adding this class to the pre-scanned list.");
          needsRescan.add(handler);
        }
      }
      if (handler.purpose == null) {
        System.err.println("No purpose for "+argHandler);
      } else {
        String purpose = handler.purpose.toString().toLowerCase();
        handler.isDeprecated = purpose.contains("deprecated") || purpose.contains("ignored");
      }
    }

    private Iterable<MethodInfo> extractMethods(ClassFile argHandler, ClasspathResourceMap scan) {
      List<MethodInfo> myMethods = argHandler.getMethods();

      switch (argHandler.getSuperclass()) {
        case "com.google.gwt.util.tools.ArgHandlerDir":
        case "com.google.gwt.util.tools.ArgHandlerFile":
        case "com.google.gwt.util.tools.ArgHandlerFlag":
        case "com.google.gwt.util.tools.ArgHandlerString":
        case "com.google.gwt.util.tools.ArgHandlerInt":
        case "com.google.gwt.util.tools.ArgHandlerExtra":
        case "com.google.gwt.util.tools.ArgHandlerEnum":
        case "com.google.gwt.util.tools.ArgHandler":
          return myMethods;
      }
      Map<String, MethodInfo> allMethods = new HashMap<>();
      for (MethodInfo method : myMethods) {
        allMethods.put(method.getName(), method);
      }
      for (MethodInfo superMethod : extractMethods(scan.findClass(argHandler.getSuperclass()), scan)) {
        if (!allMethods.containsKey(superMethod.getName())) {
          allMethods.put(superMethod.getName(), superMethod);
        }
      }
      return allMethods.values();
    }

    private boolean toBoolean(InvocationModel model) {
      if (model.type != InvocationType.Boolean) {
        throw new AssertionError("Model type must be boolean. You sent: "+model.type+" : "+model);
      }
      return "true".equals(model.value);
    }

    private ArgHandlerType findType(ClassFile argHandler, ClasspathResourceMap scan) {
      if (argHandler == null) {
        return null;
      }
      switch (argHandler.getSuperclass()) {
        case "com.google.gwt.util.tools.ArgHandlerDir":
          return ArgHandlerType.DIRECTORY;
        case "com.google.gwt.util.tools.ArgHandlerFile":
          return ArgHandlerType.FILE;
        case "com.google.gwt.util.tools.ArgHandlerFlag":
          return ArgHandlerType.BOOLEAN;
        case "com.google.gwt.util.tools.ArgHandlerString":
          return ArgHandlerType.STRING;
        case "com.google.gwt.util.tools.ArgHandlerInt":
          return ArgHandlerType.INT;
        case "com.google.gwt.util.tools.ArgHandlerExtra":
          return ArgHandlerType.EXTRA;
        case "com.google.gwt.util.tools.ArgHandlerEnum":
          return ArgHandlerType.ENUM;
        case "com.google.gwt.util.tools.ArgHandler":
          return ArgHandlerType.LIST;
        default:
          argHandler = scan.findClass(argHandler.getSuperclass());
          return findType(argHandler, scan);
      }
    }

    private InvocationModel extractSimpleInvocation(ClassFile argHandler, MethodInfo method) throws BadBytecode {
      CodeAttribute code = method.getCodeAttribute();
      CodeIterator iter = new CodeIterator(code);
      ConstPool cp = method.getConstPool();
      InvocationModel model = null;
      int loaded = Integer.MIN_VALUE;
      boolean equalCheck = false;
      while (iter.hasNext()) {
        int index = iter.next();
        int opCode = iter.byteAt(index);
        switch (opCode) {
        case Opcode.INVOKEINTERFACE:
          int ind = iter.u16bitAt(index+1);
          String methodClass = cp.getInterfaceMethodrefClassName(ind);
          String methodName = cp.getInterfaceMethodrefName(ind);
          if (!("getTag".equals(methodName) && methodClass.equals(argHandler.getQualifiedName()))) {
            model = append(model, methodClass, methodName, InvocationType.Method);
          }
          break;
        case Opcode.INVOKEVIRTUAL:
        case Opcode.INVOKESPECIAL:
        case Opcode.INVOKESTATIC:
          // assert that it's the .toTag() method used as first param to new array
          ind = iter.u16bitAt(index+1);
          methodClass = cp.getMethodrefClassName(ind);
          methodName = cp.getMethodrefName(ind);
          if (!("getTag".equals(methodName) && methodClass.equals(argHandler.getQualifiedName()))) {
            model = append(model, methodClass, methodName, InvocationType.Method);
          }
          break;
        case Opcode.LDC:
          // Loading a string contant.
          int b = iter.byteAt(index+1);
          return append(model, cp.getStringInfo(b));
        case Opcode.GETFIELD:
          ind = iter.u16bitAt(index+1);
          String fieldClass = cp.getFieldrefClassName(ind);
          String fieldName = cp.getFieldrefName(ind);
//          System.err.println(fieldClass + ">"+fieldName);
          model = append(model, fieldClass, fieldName, InvocationType.Field);
          // the reference for this GETFIELD is on the stack...  hrm.
          break;
        case Opcode.GETSTATIC:
          ind = iter.u16bitAt(index+1);
          fieldClass = cp.getFieldrefClassName(ind);
          fieldName = cp.getFieldrefName(ind);
          model = append(model, fieldClass, fieldName, InvocationType.Field);
          break;
        case Opcode.IRETURN:
          if (loaded == 0 || loaded == 1) {
            return append(model, null, loaded == 1 ? "true" : "false", InvocationType.Boolean);
          }
          break;
        case Opcode.ARETURN:
          break;
        // Ignore all the following opcodes; we don't care about the stack, we just want the
        // second argument to the new String[]{} invocation
        case Opcode.ALOAD_0:
        case Opcode.AASTORE:
        case Opcode.BASTORE:
          break;
        case Opcode.IFEQ:
        case Opcode.IF_ICMPEQ:
        case Opcode.IFNE:
          equalCheck  = true;
          break;
        case Opcode.ICONST_0:
          if (equalCheck) {
            equalCheck = false;
          } else {
            loaded = 0;
          }
          break;
        case Opcode.ICONST_1:
          if (equalCheck) {
            equalCheck = false;
          } else {
            loaded = 1;
          }
          break;
        case Opcode.ICONST_2:
          loaded = 2;
          break;
        case Opcode.ANEWARRAY:
        case Opcode.NEW:
        case Opcode.DUP:
        case Opcode.GOTO:
        case Opcode.ATHROW:
          break;
        default:
          System.out.println("Unhandled opcode "+opCode);
        }
      }
      return model;
    }

    private InvocationModel getReturnedString(MethodInfo method) throws BadBytecode, NotFoundException {
      CodeAttribute code = method.getCodeAttribute();
      CodeIterator iter = new CodeIterator(code);
      ConstPool cp = method.getConstPool();
      InvocationModel result = null;
      Object obj;
      while (iter.hasNext()) {
        int next = iter.next();
        int opCode = iter.byteAt(next);
        obj = cp.getItem(opCode);
        switch (opCode) {
        case Opcode.LDC:
          // Load a constant; next byte is the pointer to the String, int or float
          int b = iter.byteAt(next+1);
          obj = cp.getItem(b);
          if (obj instanceof StringInfo) {
            result = append(result, cp.getStringInfo(b));
          } else if (obj instanceof IntegerInfo) {
            result = append(result, String.valueOf(cp.getIntegerInfo(b)));
          } else if (obj instanceof FloatInfo) {
            result = append(result, String.valueOf(cp.getFloatInfo(b)));
          } else {
            System.err.println("Unhandled constant type: "+obj.getClass()+" of "+obj);
          }
          break;
        case Opcode.INVOKEVIRTUAL:
          int more = iter.s16bitAt(next+1);
          obj = cp.getItem(more);
          if (obj instanceof MethodrefInfo) {
            String refClass = cp.getMethodrefClassName(more);
            if (StringBuilder.class.getName().equals(refClass)) {
              // The method contains string fragments to connect.
              // We need to make a recursive method
            } else if ("getPurposeString".equals(cp.getMethodrefName(more))) {
              // getPurposeString is used by ArgHandlerEnum to wrap up some
              // information containing the enum options available; the
              // String we want is already on the stack as the ref variable
              return result;
            }
          }
          break;
        case Opcode.ARETURN:
          return result;
        case Opcode.NEW:
          String argHandler = cp.getClassInfo(iter.s16bitAt(next+1));
          switch (argHandler) {
          case "java.lang.StringBuilder":
            break;
          default:
            System.err.println("Unknown invocation of new for "+argHandler);
          }
          continue;
        case Opcode.DUP:
        case Opcode.ALOAD_0:
          // Some stack manipulation code our naive search pattern can ignore
          continue;
        case Opcode.GETSTATIC:
          int ident = iter.u16bitAt(next+1);
          obj = cp.getItem(ident);
          if (obj instanceof FieldrefInfo) {
            String fieldName = cp.getFieldrefName(ident);
            String fieldClass = cp.getFieldrefClassName(ident);
            result = append(result, fieldClass, fieldName, InvocationType.Field);
          } else if (obj instanceof StringInfo) {
            result = append(result, cp.getStringInfo(ident));
            // return result; ??
          } else {
            System.err.println("getstatic fail" + obj.getClass());
          }
          break;
        case Opcode.INVOKESPECIAL:
          next = iter.u16bitAt(next+1);
          obj = cp.getItem(next);
          if (obj instanceof MethodrefInfo) {
            String methodName = cp.getMethodrefName(next);
            if (!"<init>".equals(methodName)) {
              System.out.println("Unexpected method name: "+methodName);
            }
          }else if (obj instanceof InterfaceMethodrefInfo) {
            String methodName = cp.getInterfaceMethodrefName(next);
            String methodClass = cp.getInterfaceMethodrefClassName(next);
            // TODO check if these are StringBuilder.append to know if we can add + operations
          } else if (obj instanceof FieldrefInfo) {
            String fieldName = cp.getFieldrefName(next);
            String fieldClass = cp.getFieldrefClassName(next);
            result = append(result, fieldClass, fieldName, InvocationType.Field);
          } else if (obj instanceof StringInfo) {
            result = append(result, cp.getStringInfo(next));
          }
          break;
        default:
          System.err.println("Unhandled opcode: "+opCode);
        }
      }
      return null;
    }

    public boolean contains(ClassFile cls) {
      return hierarchy.containsKey(cls.getName());
    }

    public void add(ClassFile cls, ClassFile sup) {
      hierarchy.put(cls.getName(), sup == null ? "" : sup.getName());
    }

  }