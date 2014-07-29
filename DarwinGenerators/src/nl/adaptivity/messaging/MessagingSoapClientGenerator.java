package nl.adaptivity.messaging;

import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import net.devrieze.util.Annotations;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import nl.adaptivity.ws.soap.SoapSeeAlso;


/**
 * This class can automatically generate soap based clients for endpoints.
 *
 * @author Paul de Vrieze
 */
public class MessagingSoapClientGenerator {


  private static final class ParamInfo {

    public final String name;

    public final Type type;

    public ParamInfo(final Type pType, final String pName) {
      type = pType;
      name = pName;
    }

  }

  private static final Comparator<Method> METHODSORT = new Comparator<Method>() {

    @Override
    public int compare(Method m1, Method m2) {
      // First sort on name
      int result = m1.getName().compareTo(m2.getName());
      if (result!=0) { return result; }

      Class<?>[] p1 = m1.getParameterTypes();
      Class<?>[] p2 = m2.getParameterTypes();
      // Next on parameter list length
      if (p1.length!=p2.length) { return p1.length-p2.length; }

      // Next on the parameter types
      for(int i=0; i<p1.length; ++i) {
        result = p1[i].getSimpleName().compareTo(p2[i].getSimpleName());
        if (result!=0) { return result; }
      }
      // This should not happen as methods can not be the same but for return type in Java (but in jvm can)
      return m1.getReturnType().getSimpleName().compareTo(m2.getReturnType().getSimpleName());
    }

  };

  /**
   * @param args
   */
  public static void main(final String[] args) {
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
    if ((inClasses.size() == 0) || ((inClasses.size() == 1) && (outClass == null)) || (pkg == null)) {
      System.err.println("Not all three of inclass, outclass and package have been provided");
      showHelp();
      return;
    }
    @SuppressWarnings("resource")
    final FileSystem fs = FileSystems.getDefault();
    try {

      final String pkgdir = pkg.replaceAll("\\.", fs.getSeparator());
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
          writeOutFile(inClasses.get(0), pkg, outClass, fs, outfile, urlclassloader);
        } else {
          for (final String inClass : inClasses) {
            final String newOutClass = inClass.substring(inClass.lastIndexOf('.') + 1) + "Client";
            final Path newOutFile = outfile.getParent().resolve(newOutClass + ".java");
            writeOutFile(inClass, pkg, newOutClass, fs, newOutFile, urlclassloader);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(3);
    }

  }

  private static void writeOutFile(final String inClass, final String pkg, final String outClass, final FileSystem fs, final Path outfile, final URLClassLoader classloader) {
    Class<?> endpointClass;
    try {
      endpointClass = classloader.loadClass(inClass);
    } catch (final ClassNotFoundException e) {
      e.printStackTrace();
      return;
    }
    final String pkgname = pkg.replaceAll(fs.getSeparator(), ".");
    try {
      generateJava(outfile, endpointClass, pkgname, outClass);
    } catch (final IOException e) {
      e.printStackTrace();
      return;
    }
  }

  @SuppressWarnings("resource")
  private static URL[] getUrls(final String pClassPath) {
    final FileSystem fs = FileSystems.getDefault();
    final List<URL> result = new ArrayList<>();

    try {
      for (final String element : pClassPath.split(":")) {
        if (element.length()>0) {
          try {
            final URI uri = URI.create(element);
            result.add(uri.toURL());
          } catch (final IllegalArgumentException e) {
            final Path file = fs.getPath(element);
            result.add(file.normalize().toUri().toURL());
          }
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

  private static void generateJava(final Path pOutfile, final Class<?> pEndpointClass, final String pPkgname, final String pOutClass) throws IOException {
    try (final Writer out = new BufferedWriter(new FileWriter(pOutfile.toFile()))) {
      generateJava(out, pEndpointClass, pPkgname, pOutClass);
    }
  }

  private static void generateJava(final Writer pOut, final Class<?> pEndpointClass, final String pPkgname, final String pOutClass) throws IOException {
    writeHead(pOut, pEndpointClass, pPkgname);

    CharArrayWriter buffer = new CharArrayWriter(0x2000);

    Map<String, String> imports = new HashMap<>();
    imports.put("URI","java.net.URI");
    imports.put("Future", "java.util.concurrent.Future");
    imports.put("Arrays", "java.util.Arrays");
    imports.put("JAXBElement", "javax.xml.bind.JAXBElement");
    imports.put("JAXBException", "javax.xml.bind.JAXBException");
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

    writeClassBody(buffer, pEndpointClass, pOutClass, imports);

    List<String> finalStrings = new ArrayList<>(imports.values());
    Collections.sort(finalStrings);
    String oldPrefix=null;
    for(String str: finalStrings) {
      String prefix = str.indexOf('.')<0 ? "" : str.substring(0, str.indexOf('.'));
      if (oldPrefix!=null && (! oldPrefix.equals(prefix))) {
        pOut.write('\n');
      }
      pOut.append("import ").append(str).append(";\n");
      oldPrefix = prefix;
    }
    pOut.write('\n');

    buffer.writeTo(pOut);
  }

  private static void writeHead(final Writer pOut, final Class<?> pEndpointClass, final String pPkgname) throws IOException {
    pOut.write(  "/*\n");
    pOut.write(  " * Generated by MessagingSoapClientGenerator.\n");
    pOut.write(  " * Source class: ");
    pOut.write(    pEndpointClass.getCanonicalName());
    pOut.write("\n */\n\n");

    pOut.write("package ");
    pOut.write(pPkgname);
    pOut.write(";\n\n");
  }

  private static void writeClassBody(final Writer pOut, final Class<?> pEndpointClass, final String pOutClass, Map<String, String> pImports) throws IOException {
    pOut.write("@SuppressWarnings(\"all\")\n");
    pOut.write("public class ");
    pOut.write(pOutClass);
    pOut.write(" {\n\n");

    // Write service location constants / variables.
    boolean finalService = false;
    try {
      final Endpoint instance = (Endpoint) pEndpointClass.newInstance();

      pOut.write("  private static final QName SERVICE = " + qnamestring(instance.getServiceName()) + ";\n");
      pOut.write(appendString(new StringBuilder("  private static final String ENDPOINT = "), instance.getEndpointName()).append(";\n").toString());
      if (instance.getEndpointLocation() != null) {
        pOut.write(appendString(new StringBuilder("  private static final URI LOCATION = URI.create("), instance.getEndpointLocation().toString()).append(");\n\n").toString());
      } else {
        pOut.write("  private static final URI LOCATION = null;\n\n");
      }
      finalService = true;
    } catch (final ClassCastException | InstantiationException | IllegalAccessException e ) { /*
                                            * Ignore failure to instantiate. We
                                            * just generate different code.
                                            */
      e.printStackTrace();
    }
    if (!finalService) {
      pOut.write("  private static QName SERVICE = null;\n");
      pOut.write("  private static String ENDPOINT = null;\n");
      pOut.write("  private static URI LOCATION = null;\n\n");
    }

    // Constructor
    pOut.write("  private ");
    pOut.write(pOutClass);
    pOut.write("() { }\n\n");

    writeMethods(pOut, pEndpointClass, pImports);


    // Initializer in case we can't figure out the locations
    if (!finalService) {
      pOut.write("  private static void init(QName service, String endpoint, URI location) {\n");
      pOut.write("    SERVICE=service;\n");
      pOut.write("    ENDPOINT=endpoint;\n");
      pOut.write("    LOCATION=location;\n");
      pOut.write("  }\n\n");
    }

    pOut.write("}\n");
  }

  private static void writeMethods(final Writer pOut, final Class<?> pEndpointClass, Map<String, String> pImports) throws IOException {
    final Method[] methods = pEndpointClass.getMethods();
    Arrays.sort(methods, METHODSORT);
    for (final Method method : methods) {
      final WebMethod annotation = method.getAnnotation(WebMethod.class);
      if (annotation != null) {
        writeMethod(pOut, method, annotation, pImports);
      }
    }
  }

  private static void writeMethod(final Writer pOut, final Method pMethod, final WebMethod pAnnotation, Map<String, String> pImports) throws IOException {
    String methodName = pAnnotation.operationName();
    String principalName = null;
    if ((methodName == null) || (methodName.length() == 0)) {
      methodName = pMethod.getName();
    }
    pOut.write("  public static");
    writeTypeParams(pOut, pMethod.getTypeParameters(), pImports);
    pOut.write(" Future<");
    writeType(pOut, pMethod.getGenericReturnType(), false, false, pImports);
    pOut.write("> ");
    pOut.write(methodName);
    pOut.write("(");
    boolean firstParam = true;
    final List<ParamInfo> params = new ArrayList<>();
    {
      int paramNo = 0;
      final Type[] parameterTypes = pMethod.getGenericParameterTypes();
      for (int i=0; i<parameterTypes.length; ++i) {
        final Type paramType = parameterTypes[i];
        String name = null;
        boolean isPrincipal = false;
        for (final Annotation annotation : pMethod.getParameterAnnotations()[paramNo]) {
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
          pOut.write(", ");
        }
        writeType(pOut, paramType, true, pMethod.isVarArgs() && i==parameterTypes.length-1, pImports);
        pOut.write(' ');
        pOut.write(name);

        ++paramNo;
      }
    }
    pOut.write(", CompletionListener completionListener) throws JAXBException {\n");
    {
      int paramNo = 0;
      for (final ParamInfo param : params) {
        pOut.write("    final Tripple<String, Class<");
        Class<?> rawtype = getRawType(param.type);
        writeType(pOut, rawtype, false, false, pImports);
        pOut.write(">, ");
        writeType(pOut, param.type, false, false, pImports);
        pOut.write("> param" + paramNo + " = Tripple.<String, Class<");
        writeType(pOut, rawtype, false, false, pImports);
        pOut.write(">, ");
        writeType(pOut, param.type, false, false, pImports);
        pOut.write(">tripple(");
        pOut.write(appendString(new StringBuilder(), param.name).append(", ").toString());
        if (rawtype.isArray()) {
          pOut.write("Array.class, ");
        } else {
          writeType(pOut, rawtype, true, false, pImports);
          pOut.write(".class, ");
        }
        pOut.write(param.name);
        pOut.write(");\n");

        ++paramNo;
      }
    }
    pOut.write("\n");
    pOut.write("    Source message = SoapHelper.createMessage(new QName(");
    if (pAnnotation.operationName() != null) {
      pOut.write(appendString(new StringBuilder(), pAnnotation.operationName()).append("), ").toString());
    } else {
      pOut.write(appendString(new StringBuilder(), pMethod.getName()).append("), ").toString());
    }
    if (principalName != null) {
      pOut.write("Arrays.asList(new JAXBElement<String>(new QName(\"http://adaptivity.nl/ProcessEngine/\",\"principal\"), String.class, "
          + principalName + ".getName())), ");
    }
    pOut.write("Arrays.asList(");
    for (int i = 0; i < params.size(); ++i) {
      if (i > 0) {
        pOut.write(", ");
      }
      pOut.write("param" + i);
    }
    pOut.write("));\n\n");

    pOut.write("    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);\n\n");

    pOut.write("    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, ");
    writeType(pOut, getRawType(pMethod.getGenericReturnType()), true, false, pImports);

    pOut.write(".class, ");

    SoapSeeAlso seeAlso = Annotations.getAnnotation(pMethod.getAnnotations(),SoapSeeAlso.class);
    if (seeAlso==null) {
      pOut.write("new Class<?>[0]");
    } else {
      writeClassArray(pOut, seeAlso.value(), pImports);
    }

    pOut.write(");\n");

    pOut.write("  }\n\n");

  }

  private static void writeClassArray(Writer pOut, Class<?>[] pValue, Map<String, String> pImports) throws IOException {
    if (pValue.length==0) {
      pOut.write("new Class<?>[0]");
      return;
    }
    pOut.write("new Class<?>[] {");
    writeType(pOut, pValue[0], false, false, pImports);
    for(int i=1; i<pValue.length; ++i) {
      pOut.write(", ");
      writeType(pOut, pValue[i], false, false, pImports);
    }
    pOut.write('}');
  }

  private static Class<?> getRawType(Type pType) {
    if (pType instanceof Class) {
      return (Class<?>) pType;
    } else if (pType instanceof ParameterizedType) {
      return getRawType(((ParameterizedType) pType).getRawType());
    } else if (pType instanceof GenericArrayType) {
      final Class<?> componentType = getRawType(((GenericArrayType) pType).getGenericComponentType());
      return Array.newInstance(componentType, 0).getClass();
    } else if (pType instanceof TypeVariable<?>) {
      for(Type bound:((TypeVariable<?>)pType).getBounds()) {
        return getRawType(bound);
      }
      return Object.class;
    } else if (pType instanceof WildcardType) {
      for(Type bound:((WildcardType)pType).getUpperBounds()) {
        return getRawType(bound);
      }
      return Object.class;

    }
    return null;
  }

  private static void writeType(Writer pOut, Type pType, boolean allowPrimitive, boolean varArgs, Map<String, String> pImports) throws IOException {
    if (pType instanceof ParameterizedType) {
      ParameterizedType type = (ParameterizedType) pType;
      writeType(pOut, type.getRawType(), allowPrimitive, varArgs, pImports);
      writeTypes(pOut, type.getActualTypeArguments(), pImports);
    } else if (pType instanceof GenericArrayType) {
      GenericArrayType type = (GenericArrayType) pType;
      writeType(pOut,type.getGenericComponentType(), true, false, pImports);
      if (varArgs) {
        pOut.write("...");
      } else {
        pOut.write("[]");
      }
    } else if (pType instanceof TypeVariable<?>) {
      TypeVariable<?> type = (TypeVariable<?>) pType;
      pOut.write(type.getName());
    } else if (pType instanceof WildcardType) {
      WildcardType type = (WildcardType) pType;
      pOut.write('?');
      {
        Type[] lower = type.getLowerBounds();
        if (lower.length>0) {
          pOut.write(" super ");
          boolean first = true;
          for(Type b:lower) {
            if (first) { first = false; } else { pOut.write(" & "); }
            writeType(pOut, b, false, varArgs, pImports);
          }
        }
      }
      {
        Type[] upper = type.getUpperBounds();
        if (!(upper.length==0 || (upper.length==1 && Object.class.equals(upper[0])))) {
          pOut.write(" extends ");
          boolean first = true;
          for(Type b:upper) {
            if (first) { first = false; } else { pOut.write(" & "); }
            writeType(pOut, b, false, varArgs, pImports);
          }

        }
      }
    } else if (pType instanceof Class) {
      final Class<?> type = (Class<?>)pType;
      if (allowPrimitive || (! type.isPrimitive())) {
        final String canonname = type.getCanonicalName();
        final String pkg = getPackage(canonname);
        final String cls = getName(canonname);
        if ("java.lang".equals(pkg) || type.isPrimitive()) {
          pOut.write(cls);
        } else {
          String imp = pImports.get(cls);
          if (imp==null) {
            pImports.put(cls, canonname);
            pOut.write(cls);
          } else if (imp.equals(canonname)) {
            pOut.write(cls);
          } else { // duplicate, use full name
            pOut.write(canonname);
          }
        }
      } else {
        pOut.write(toBox(type.getSimpleName()));
      }
    } else {
      throw new IllegalArgumentException("Type parameter of type "+pType.getClass().getCanonicalName()+" not supported");
    }
  }

  private static String getPackage(String pName) {
    int i=pName.lastIndexOf('.');
    if (i>=0) {
      return pName.substring(0, i);
    }
    return pName;
  }

  private static String getName(String pName) {
    int i=pName.lastIndexOf('.');
    if (i>=0) {
      return pName.substring(i+1);
    }
    return pName;
  }

  private static String toBox(String pSimpleName) {
    switch (pSimpleName) {
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

  private static void writeTypes(Writer pOut, Type[] pTypes, Map<String, String> pImports) throws IOException {
    if (pTypes.length>0) {
      pOut.write('<');
      writeType(pOut, pTypes[0], false, false, pImports);
      for(int i=1; i< pTypes.length;++i) {
        pOut.write(',');
        writeType(pOut, pTypes[i], false, false, pImports);
      }
      pOut.write('>');
    }

  }

  private static void writeTypeParams(Writer pOut, TypeVariable<Method>[] pParams, Map<String, String> pImports) throws IOException {
    if (pParams.length>0) {
      pOut.write('<');
      boolean first = true;
      for(TypeVariable<Method> param: pParams) {
        if (first) { first = false; } else { pOut.write(", "); }
        pOut.write(param.getName());
        if (param.getBounds().length>0 &&
            (! (param.getBounds().length==1 && Object.class.equals(param.getBounds()[0])))) {
          pOut.write(" extends ");
          boolean boundFirst =true;
          for(Type bound: param.getBounds()) {
            if (boundFirst) { boundFirst = false; } else { pOut.write(" & "); }
            writeType(pOut, bound, false, false, pImports);
          }
        }
      }
      pOut.write('>');
    }

  }

  private static String qnamestring(final QName pQName) {
    final StringBuilder result = new StringBuilder();

    result.append("new QName(");
    appendString(result, pQName.getNamespaceURI()).append(", ");
    appendString(result, pQName.getLocalPart());
    if (pQName.getPrefix() != null) {
      appendString(result.append(", "), pQName.getPrefix());
    }
    result.append(')');
    return result.toString();
  }

  private static StringBuilder appendString(final StringBuilder pResult, final String pString) {
    if (pString == null) {
      pResult.append("null");
    } else {
      pResult.append('"');
      for (int i = 0; i < pString.length(); ++i) {
        final char c = pString.charAt(i);
        switch (c) {
          case '\\':
            pResult.append("\\\\");
            break;
          case '"':
            pResult.append('\\').append(c);
            break;
          case '\t':
            pResult.append("\\t");
            break;
          case '\n':
            pResult.append("\\n");
            break;
          case '\r':
            pResult.append("\\r");
            break;
          default:
            pResult.append(c);
        }
      }
      pResult.append('"');
    }
    return pResult;
  }

}
