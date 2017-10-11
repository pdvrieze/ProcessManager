/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.messaging;

import net.devrieze.util.Annotations;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import nl.adaptivity.ws.soap.SoapSeeAlso;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.namespace.QName;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;


/**
 * This class can automatically generate soap based clients for endpoints.
 *
 * @author Paul de Vrieze
 */
@SuppressWarnings("StringConcatenationMissingWhitespace")
public final class MessagingSoapClientGenerator {


  private static final class ParamInfo {

    public final String mName;

    public final Type mType;

    public ParamInfo(final Type type, final String name) {
      mType = type;
      mName = name;
    }

  }

  private static final Comparator<Method> METHODSORT = new Comparator<Method>() {

    @Override
    public int compare(final Method m1, final Method m2) {
      // First sort on name
      int result = m1.getName().compareTo(m2.getName());
      if (result!=0) { return result; }

      final Class<?>[] param1 = m1.getParameterTypes();
      final Class<?>[] param2 = m2.getParameterTypes();
      // Next on parameter list length
      if (param1.length!=param2.length) { return param1.length>param2.length ? 1 : 0; }

      // Next on the parameter types
      for(int i=0; i<param1.length; ++i) {
        result = param1[i].getSimpleName().compareTo(param2[i].getSimpleName());
        if (result!=0) { return result; }
      }
      // This should not happen as methods can not be the same but for return type in Java (but in jvm can)
      return m1.getReturnType().getSimpleName().compareTo(m2.getReturnType().getSimpleName());
    }

  };

  private static int _errorCount =0;

  private MessagingSoapClientGenerator() { /** No object instances expected.*/}

  /**
   * @param args Standard arguments to the generator
   */
  public static void main(final String[] args) {
    /*
    System.err.println("Parameters:");
    for(String a:args) {
      System.err.print("  ");
      System.err.println(a);
    }
    */
    String pkg = null;
    String outClass = null;
    final List<String> inClasses = new ArrayList<>();
    String cp = ".";
    String dstdir = ".";
    for (int i = 0; i < args.length; ++i) {
      switch (args[i]) {
        case "-package": {
          if (pkg != null) {
            showHelp();
            return;
          }
          pkg = args[++i];
          break;
        }
        case "-out": {
          if (outClass != null) {
            showHelp();
            return;
          }
          outClass = args[++i];
          final int j = outClass.lastIndexOf('.');
          if ((j >= 0) && (pkg == null)) {
            pkg = outClass.substring(0, j);
            outClass = outClass.substring(j + 1);
          }
          break;
        }
        case "-cp": {
          if (!".".equals(cp)) {
            showHelp();
            return;
          }
          cp = args[++i];
          break;
        }
        case "-dstdir": {
          if (! ".".equals(dstdir)) {
            showHelp();
            return;
          }
          dstdir = args[++i];
          break;
        }
        case "-help": {
          showHelp();
          return;
        }
        default: {
          if (args[i].charAt(0) == '-') {
            System.err.println("Unsupported arguments: " + args[i]);
            showHelp();
            return;
          } else {
            inClasses.add(args[i]);
          }
        }
      }
    }
    try {
      generateClientJava(pkg, outClass, inClasses, cp, dstdir);
    } catch (Exception e) {
      e.printStackTrace();
      ++_errorCount;
      System.exit(_errorCount);
    }
    System.exit(0);
  }

  private static void generateClientJava(final String destPkg, String outClass, final List<String> inClasses, final String cp, final String dstdir) {
    if ((inClasses.size() == 0) || (destPkg == null)) {
      System.err.println("Not all three of inclass, outclass and package have been provided");
      showHelp();
      return;
    } else if (inClasses.size()==1 && outClass==null) {
      outClass = getDefaultOutClassName(inClasses.get(0));
    }
    @SuppressWarnings("resource")
    final FileSystem fs = FileSystems.getDefault();
    try {
      final String pkgdir = destPkg.replace(".", fs.getSeparator());
      final String classfilename = outClass + ".java";
      final Path outfile = fs.getPath(dstdir, pkgdir, classfilename);

      // Ensure the parent directory of the outfile exists.
      if (!outfile.getParent().toFile().mkdirs()) {
        if (!outfile.getParent().toFile().exists()) {
          System.err.println("Could not create the directory " + outfile.getParent());
          System.exit(2);
        }
      }

      try (URLClassLoader urlclassloader = URLClassLoader.newInstance(getUrls(cp))) {

        if (inClasses.size() == 1) {
          writeOutFile(inClasses.get(0), destPkg, outClass, fs, outfile, urlclassloader);
        } else {
          for (final String inClass : inClasses) {
            final String newOutClass = getDefaultOutClassName(inClass);
            final Path newOutFile = outfile.getParent().resolve(newOutClass + ".java");
            writeOutFile(inClass, destPkg, newOutClass, fs, newOutFile, urlclassloader);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(3);
    }
  }

  private static String getDefaultOutClassName(final String inClass) {
    return inClass.substring(inClass.lastIndexOf('.') + 1) + "Client";
  }

  private static void writeOutFile(final String inClass, final String pkg, final String outClass, final FileSystem fs, final Path outfile, final URLClassLoader classloader) {
    final Class<?> endpointClass;
    try {
      endpointClass = classloader.loadClass(inClass);
    } catch (final ClassNotFoundException e) {
      e.printStackTrace();
      ++_errorCount;
      return;
    }
    final String pkgname = pkg.replace(fs.getSeparator(), ".");
    try {
      generateJava(outfile, endpointClass, pkgname, outClass);
    } catch (final IOException e) {
      e.printStackTrace();
      ++_errorCount;
    }
  }

  @SuppressWarnings("resource")
  private static URL[] getUrls(final String classPath) {
    final FileSystem fs = FileSystems.getDefault();
    final List<URL> result = new ArrayList<>();

    try {
      for (final String element : classPath.split(":")) {
        if (element.length()>0) {
          URL url;
          try {
            final URI uri = URI.create(element);
            url = uri.toURL();
          } catch (final IllegalArgumentException e) {
            final Path file = fs.getPath(element);
            url=file.normalize().toUri().toURL();
          }
          result.add(url);
        }
      }
    } catch (final MalformedURLException e) {
      System.err.println("Invalid classpath element");
      e.printStackTrace();
    }
    return result.toArray(new URL[result.size()]);

  }

  private static void showHelp() {
    System.out.println("Usage:");
    System.out.println("  -help              : Show this message, ignore everything else");
    System.out.println("  -out <classname>   : The output classname to generate");
    System.out.println("  -package <pkgname> : The output package name to generate");
    System.out.println("  -cp <path>         : The classpath to look for source classes and their dependencies");
    System.out.println("  -dstdir <dirname>  : The directory to write the generated java files");
    System.out.println("  <inputclass>       : The Endpoint that needs a client");
  }

  private static void generateJava(final Path outfile, final Class<?> endpointClass, final String pkgname, final String outClass) throws IOException {
    try (final Writer out = new BufferedWriter(new FileWriter(outfile.toFile()))) {
      generateJava(out, endpointClass, pkgname, outClass);
    }
  }

  private static void generateJava(final Writer out, final Class<?> endpointClass, final String pkgname, final String outClass) throws IOException {
    writeHead(out, endpointClass, pkgname);

    final CharArrayWriter buffer = new CharArrayWriter(0x2000);

    final Map<String, String> imports = new HashMap<>();
    imports.put("URI","java.net.URI");
    imports.put("Future", "java.util.concurrent.Future");
    imports.put("Arrays", "java.util.Arrays");
    imports.put("JAXBElement", "javax.xml.bind.JAXBElement");
    imports.put("JAXBException", "javax.xml.bind.JAXBException");
    imports.put("XmlException", "nl.adaptivity.xml.XmlException");
    imports.put("QName", "javax.xml.namespace.QName");
    imports.put("Source", "javax.xml.transform.Source");
    imports.put("Tripple", "net.devrieze.util.Tripple");
    imports.put("CompletionListener", "nl.adaptivity.messaging.CompletionListener");
    imports.put("Endpoint", "nl.adaptivity.messaging.Endpoint");
    imports.put("EndpointDescriptor", "nl.adaptivity.messaging.EndpointDescriptor");
    imports.put("EndpointDescriptorImpl", "nl.adaptivity.messaging.EndpointDescriptorImpl");
    imports.put("MessagingRegistry", "nl.adaptivity.messaging.MessagingRegistry");
    imports.put("SendableSoapSource", "nl.adaptivity.messaging.SendableSoapSource");
    imports.put("SoapHelper", "nl.adaptivity.ws.soap.SoapHelper");

    writeClassBody(buffer, endpointClass, outClass, imports);

    final List<String> finalStrings = new ArrayList<>(imports.values());
    Collections.sort(finalStrings);
    String oldPrefix=null;
    for(final String str: finalStrings) {
      final String prefix = str.indexOf('.')<0 ? "" : str.substring(0, str.indexOf('.'));
      if (oldPrefix!=null && (! oldPrefix.equals(prefix))) {
        out.write('\n');
      }
      //noinspection resource
      out.append("import ").append(str).append(";\n");
      oldPrefix = prefix;
    }
    out.write('\n');

    buffer.writeTo(out);
  }

  private static void writeHead(final Writer out, final Class<?> endpointClass, final String pkgname) throws IOException {
    out.write(  "/*\n");
    out.write(  " * Generated by MessagingSoapClientGenerator.\n");
    out.write(  " * Source class: ");
    out.write(    endpointClass.getCanonicalName());
    out.write("\n */\n\n");

    out.write("package ");
    out.write(pkgname);
    out.write(";\n\n");
  }

  private static void writeClassBody(final Writer out, final Class<?> endpointClass, final String outClass, final Map<String, String> imports) throws IOException {
    out.write("@SuppressWarnings(\"all\")\n");
    out.write("public class ");
    out.write(outClass);
    out.write(" {\n\n");

    // Write service location constants / variables.
    boolean finalService = false;
    try {
      final EndpointDescriptor instance;
      if(endpointClass.isAnnotationPresent(Descriptor.class)) {
        instance = endpointClass.getAnnotation(Descriptor.class).value().newInstance();
      } else {
        instance = (EndpointDescriptor) endpointClass.newInstance();
      }

      out.write("  private static final QName SERVICE = " + qnamestring(instance.getServiceName()) + ";\n");
      out.write(appendString(new StringBuilder("  private static final String ENDPOINT = "), instance.getEndpointName()).append(";\n").toString());
      if (instance.getEndpointLocation() != null) {
        out.write(appendString(new StringBuilder("  private static final URI LOCATION = URI.create("), instance.getEndpointLocation().toString()).append(");\n\n").toString());
      } else {
        out.write("  private static final URI LOCATION = null;\n\n");
      }
      finalService = true;
    } catch (final ClassCastException | InstantiationException | IllegalAccessException e ) { /*
                                            * Ignore failure to instantiate. We
                                            * just generate different code.
                                            */
      e.printStackTrace();
    }
    if (!finalService) {
      out.write("  private static QName SERVICE = null;\n");
      out.write("  private static String ENDPOINT = null;\n");
      out.write("  private static URI LOCATION = null;\n\n");
    }

    // Constructor
    out.write("  private ");
    out.write(outClass);
    out.write("() { }\n\n");

    writeMethods(out, endpointClass, imports);


    // Initializer in case we can't figure out the locations
    if (!finalService) {
      out.write("  private static void init(QName service, String endpoint, URI location) {\n");
      out.write("    SERVICE=service;\n");
      out.write("    ENDPOINT=endpoint;\n");
      out.write("    LOCATION=location;\n");
      out.write("  }\n\n");
    }

    out.write("}\n");
  }

  private static void writeMethods(final Writer out, final Class<?> endpointClass, final Map<String, String> imports) throws IOException {
    final Method[] methods = endpointClass.getMethods();
    Arrays.sort(methods, METHODSORT);
    for (final Method method : methods) {
      final WebMethod annotation = method.getAnnotation(WebMethod.class);
      if (annotation != null) {
        writeMethod(out, method, annotation, imports);
      }
    }
  }

  private static void writeMethod(final Writer out, final Method method, final WebMethod webMethod, final Map<String, String> imports) throws IOException {
    String methodName = webMethod.operationName();
    String principalName = null;
    if ((methodName == null) || (methodName.length() == 0)) {
      methodName = method.getName();
    }
    out.write("  public static");
    writeTypeParams(out, method.getTypeParameters(), imports);
    out.write(" Future<");
    writeType(out, method.getGenericReturnType(), false, false, imports);
    out.write("> ");
    out.write(methodName);
    out.write("(");
    boolean firstParam = true;
    final List<ParamInfo> params = new ArrayList<>();
    {
      int paramNo = 0;
      final Type[] parameterTypes = method.getGenericParameterTypes();
      for (int i=0; i<parameterTypes.length; ++i) {
        final Type paramType = parameterTypes[i];
        String name = null;
        boolean isPrincipal = false;
        for (final Annotation annotation : method.getParameterAnnotations()[paramNo]) {
          if (annotation instanceof WebParam) {
            final WebParam webparam = (WebParam) annotation;
            if (webparam.name() != null) {
              name = webparam.name();
            }
          }
          if (annotation instanceof RestParam) {
            final RestParam restParam = (RestParam) annotation;
            if (restParam.type() == ParamType.PRINCIPAL) {
              // We nead a principal header
              isPrincipal = true;
              if ((name == null) || (name.length() == 0)) {
                name = restParam.name();
              }
              //noinspection ConstantConditions
              if ((name == null) || (name.length() == 0)) {
                name = "principal";
              }
            }
          }
        }
        if ((name == null) || (name.length() == 0)) {
          name = "param" + paramNo;
        }
        if (isPrincipal) {
          principalName = name;
        } else {
          params.add(new ParamInfo(paramType, name));
        }
        if (firstParam) {
          firstParam = false;
        } else {
          out.write(", ");
        }
        writeType(out, paramType, true, method.isVarArgs() && i==parameterTypes.length-1, imports);
        out.write(' ');
        out.write(name);

        ++paramNo;
      }
    }
    final SoapSeeAlso seeAlso = Annotations.getAnnotation(method.getAnnotations(), SoapSeeAlso.class);
    if (seeAlso == null) {
      out.write(", CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException, XmlException {\n");
    } else {
      out.write(", CompletionListener completionListener) throws JAXBException, XmlException {\n");
    }
    {
      int paramNo = 0;
      for (final ParamInfo param : params) {
        out.write("    final Tripple<String, Class<");
        final Class<?> rawtype = getRawType(param.mType);
        writeType(out, rawtype, false, false, imports);
        out.write(">, ");
        writeType(out, param.mType, false, false, imports);
        out.write("> param"); out.write(Integer.toString(paramNo)); out.write(" = Tripple.<String, Class<");
        writeType(out, rawtype, false, false, imports);
        out.write(">, ");
        writeType(out, param.mType, false, false, imports);
        out.write(">tripple(");
        out.write(appendString(new StringBuilder(), param.mName).append(", ").toString());
        if (rawtype.isArray()) {
          out.write("Array.class, ");
        } else {
          writeType(out, rawtype, true, false, imports);
          out.write(".class, ");
        }
        out.write(param.mName);
        out.write(");\n");

        ++paramNo;
      }
    }
    out.write("\n");
    out.write("    Source message = SoapHelper.createMessage(new QName(");
    if (webMethod.operationName() != null) {
      out.write(appendString(new StringBuilder(), webMethod.operationName()).append("), ").toString());
    } else {
      out.write(appendString(new StringBuilder(), method.getName()).append("), ").toString());
    }
    if (principalName != null) {
      out.write("Arrays.asList(new JAXBElement<String>(new QName(\"http://adaptivity.nl/ProcessEngine/\",\"principal\"), String.class, "
          + principalName + ".getName())), ");
    }
    out.write("Arrays.<Tripple<String, ? extends Class<?>, ?>>asList(");
    for (int i = 0; i < params.size(); ++i) {
      if (i > 0) {
        out.write(", ");
      }
      out.write("param" + i);
    }
    out.write("));\n\n");

    out.write("    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);\n\n");

    out.write("    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, ");
    writeType(out, getRawType(method.getGenericReturnType()), true, false, imports);

    out.write(".class, ");

    if (seeAlso==null) {
      out.write("jaxbcontext");
    } else {
      writeClassArray(out, seeAlso.value(), imports);
    }

    out.write(");\n");

    out.write("  }\n\n");

  }

  private static void writeClassArray(final Writer out, final Class<?>[] value, final Map<String, String> imports) throws IOException {
    if (value.length==0) {
      out.write("new Class<?>[0]");
      return;
    }
    out.write("new Class<?>[] {");
    writeType(out, value[0], false, false, imports);
    for(int i=1; i<value.length; ++i) {
      out.write(", ");
      writeType(out, value[i], false, false, imports);
    }
    out.write('}');
  }

  @SuppressWarnings("LoopStatementThatDoesntLoop")
  private static Class<?> getRawType(final Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedType) {
      return getRawType(((ParameterizedType) type).getRawType());
    } else if (type instanceof GenericArrayType) {
      final Class<?> componentType = getRawType(((GenericArrayType) type).getGenericComponentType());
      return Array.newInstance(componentType, 0).getClass();
    } else if (type instanceof TypeVariable<?>) {
      for(final Type bound:((TypeVariable<?>)type).getBounds()) {
        return getRawType(bound);
      }
      return Object.class;
    } else if (type instanceof WildcardType) {
      for(final Type bound:((WildcardType)type).getUpperBounds()) {
        return getRawType(bound);
      }
      return Object.class;

    }
    return null;
  }

  private static void writeType(final Writer out, final Type type, final boolean allowPrimitive, final boolean varArgs, final Map<String, String> imports) throws IOException {
    if (type instanceof ParameterizedType) {
      final ParameterizedType parameterizedType = (ParameterizedType) type;
      writeType(out, parameterizedType.getRawType(), allowPrimitive, varArgs, imports);
      writeTypes(out, parameterizedType.getActualTypeArguments(), imports);
    } else if (type instanceof GenericArrayType) {
      final GenericArrayType genericArrayType = (GenericArrayType) type;
      writeType(out,genericArrayType.getGenericComponentType(), true, false, imports);
      if (varArgs) {
        out.write("...");
      } else {
        out.write("[]");
      }
    } else if (type instanceof TypeVariable<?>) {
      final TypeVariable<?> typeVariable = (TypeVariable<?>) type;
      out.write(typeVariable.getName());
    } else if (type instanceof WildcardType) {
      final WildcardType wildcardType = (WildcardType) type;
      out.write('?');
      {
        final Type[] lower = wildcardType.getLowerBounds();
        if (lower.length>0) {
          out.write(" super ");
          boolean first = true;
          //noinspection Duplicates
          for(final Type bound:lower) {
            if (first) { first = false; } else { out.write(" & "); }
            writeType(out, bound, false, varArgs, imports);
          }
        }
      }
      {
        final Type[] upper = wildcardType.getUpperBounds();
        if (!(upper.length==0 || (upper.length==1 && Object.class.equals(upper[0])))) {
          out.write(" extends ");
          boolean first = true;
          //noinspection Duplicates
          for(final Type bound:upper) {
            if (first) { first = false; } else { out.write(" & "); }
            writeType(out, bound, false, varArgs, imports);
          }

        }
      }
    } else if (type instanceof Class) {
      final Class<?> clazz = (Class<?>)type;
      if (allowPrimitive || (! clazz.isPrimitive())) {
        final String canonname = clazz.getCanonicalName();
        final String pkg = getPackage(canonname);
        final String cls = getName(canonname);
        if ("java.lang".equals(pkg) || clazz.isPrimitive()) {
          out.write(cls);
        } else {
          final String imp = imports.get(cls);
          if (imp==null) {
            imports.put(cls, canonname);
            out.write(cls);
          } else if (imp.equals(canonname)) {
            out.write(cls);
          } else { // duplicate, use full name
            out.write(canonname);
          }
        }
      } else {
        out.write(toBox(clazz.getSimpleName()));
      }
    } else {
      throw new IllegalArgumentException("Type parameter of type "+type.getClass().getCanonicalName()+" not supported");
    }
  }

  private static String getPackage(final String name) {
    final int i=name.lastIndexOf('.');
    if (i>=0) {
      return name.substring(0, i);
    }
    return name;
  }

  private static String getName(final String name) {
    final int i=name.lastIndexOf('.');
    if (i>=0) {
      return name.substring(i+1);
    }
    return name;
  }

  private static String toBox(final String simpleName) {
    switch (simpleName) {
      case "byte": return "Byte";
      case "short": return "Short";
      case "int": return "Integer";
      case "char": return "Character";
      case "long": return "Long";
      case "float": return "Float";
      case "boolean": return "Boolean";
      case "double": return "Double";
    }
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private static void writeTypes(final Writer out, final Type[] types, final Map<String, String> imports) throws IOException {
    if (types.length>0) {
      out.write('<');
      writeType(out, types[0], false, false, imports);
      for(int i=1; i< types.length;++i) {
        out.write(',');
        writeType(out, types[i], false, false, imports);
      }
      out.write('>');
    }

  }

  private static void writeTypeParams(final Writer out, final TypeVariable<Method>[] params, final Map<String, String> imports) throws IOException {
    if (params.length>0) {
      out.write('<');
      boolean first = true;
      for(final TypeVariable<Method> param: params) {
        if (first) { first = false; } else { out.write(", "); }
        out.write(param.getName());
        if (param.getBounds().length>0 &&
            (! (param.getBounds().length==1 && Object.class.equals(param.getBounds()[0])))) {
          out.write(" extends ");
          boolean boundFirst =true;
          for(final Type bound: param.getBounds()) {
            if (boundFirst) { boundFirst = false; } else { out.write(" & "); }
            writeType(out, bound, false, false, imports);
          }
        }
      }
      out.write('>');
    }

  }

  private static String qnamestring(final QName qName) {
    final StringBuilder result = new StringBuilder();

    result.append("new QName(");
    appendString(result, qName.getNamespaceURI()).append(", ");
    appendString(result, qName.getLocalPart());
    if (qName.getPrefix() != null) {
      appendString(result.append(", "), qName.getPrefix());
    }
    result.append(')');
    return result.toString();
  }

  private static StringBuilder appendString(final StringBuilder result, final String unescapedStr) {
    if (unescapedStr == null) {
      result.append("null");
    } else {
      result.append('"');
      for (int i = 0; i < unescapedStr.length(); ++i) {
        final char c = unescapedStr.charAt(i);
        switch (c) {
          case '\\':
            result.append("\\\\");
            break;
          case '"':
            result.append('\\').append(c);
            break;
          case '\t':
            result.append("\\t");
            break;
          case '\n':
            result.append("\\n");
            break;
          case '\r':
            result.append("\\r");
            break;
          default:
            result.append(c);
        }
      }
      result.append('"');
    }
    return result;
  }

}
