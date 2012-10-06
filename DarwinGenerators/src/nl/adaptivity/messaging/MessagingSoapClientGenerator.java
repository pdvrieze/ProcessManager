package nl.adaptivity.messaging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.namespace.QName;

import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;


/**
 * This class can automatically generate soap based clients for endpoints.
 *
 * @author Paul de Vrieze
 */
public class MessagingSoapClientGenerator {


  private static final class ParamInfo {

    public final String name;

    public final Class<?> type;

    public ParamInfo(final Class<?> pType, final String pName) {
      type = pType;
      name = pName;
    }

  }

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
          if ("." != cp) {
            showHelp();
            return;
          }
          cp = args[++i];
          break;
        }
        case "-dstdir": {
          if ("." != dstdir) {
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
    final FileSystem fs = FileSystems.getDefault();

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

    URLClassLoader urlclassloader;
    urlclassloader = URLClassLoader.newInstance(getUrls(cp));

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

  private static URL[] getUrls(final String pClassPath) {
    final FileSystem fs = FileSystems.getDefault();
    final List<URL> result = new ArrayList<>();

    try {
      for (final String element : pClassPath.split(":")) {
        try {
          final URI uri = URI.create(element);
          result.add(uri.toURL());
        } catch (final IllegalArgumentException e) {
          final Path file = fs.getPath(element);
          result.add(file.normalize().toUri().toURL());
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
    final Writer out = new BufferedWriter(new FileWriter(pOutfile.toFile()));
    try {
      generateJava(out, pEndpointClass, pPkgname, pOutClass);
    } finally {
      out.close();
    }
  }

  private static void generateJava(final Writer pOut, final Class<?> pEndpointClass, final String pPkgname, final String pOutClass) throws IOException {
    pOut.write("/*\n");
    pOut.write(" * Generated by MessagingSoapClientGenerator.\n");
    pOut.write(" */\n\n");

    pOut.write("package ");
    pOut.write(pPkgname);
    pOut.write(";\n\n");

    pOut.write("import java.net.URI;\n" + "import java.util.concurrent.Future;\n\n" +

    "import javax.xml.bind.JAXBElement;\n" + "import javax.xml.bind.JAXBException;\n" + "import javax.xml.namespace.QName;\n"
        + "import javax.xml.transform.Source;\n\n" +

        "import net.devrieze.util.Tripple;\n\n" +

        "import nl.adaptivity.messaging.CompletionListener;\n" + "import nl.adaptivity.messaging.Endpoint;\n"
        + "import nl.adaptivity.messaging.EndPointDescriptor;\n" + "import nl.adaptivity.messaging.MessagingRegistry;\n"
        + "import nl.adaptivity.messaging.SendableSoapSource;\n" + "import nl.adaptivity.ws.soap.SoapHelper;\n\n");

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
    } catch (final ClassCastException e) { /*
                                            * Ignore failure to instantiate. We
                                            * just generate different code.
                                            */
    } catch (final InstantiationException e) { /*
                                                * Ignore failure to instantiate.
                                                * We just generate different
                                                * code.
                                                */
    } catch (final IllegalAccessException e) { /*
                                                * Ignore failure to instantiate.
                                                * We just generate different
                                                * code.
                                                */
      e.printStackTrace();
      System.exit(1);
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

    writeMethods(pOut, pEndpointClass);


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

  private static void writeMethods(final Writer pOut, final Class<?> pEndpointClass) throws IOException {
    for (final Method method : pEndpointClass.getMethods()) {
      final WebMethod annotation = method.getAnnotation(WebMethod.class);
      if (annotation != null) {
        writeMethod(pOut, method, annotation);
      }
    }
  }

  private static void writeMethod(final Writer pOut, final Method pMethod, final WebMethod pAnnotation) throws IOException {
    String methodName = pAnnotation.operationName();
    String principalName = null;
    if ((methodName == null) || (methodName.length() == 0)) {
      methodName = pMethod.getName();
    }
    pOut.write("  public static Future<");
    pOut.write(pMethod.getReturnType().getCanonicalName());
    pOut.write("> ");
    pOut.write(methodName);
    pOut.write("(");
    boolean firstParam = true;
    final List<ParamInfo> params = new ArrayList<>();
    {
      int paramNo = 0;
      for (final Class<?> paramType : pMethod.getParameterTypes()) {
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

        pOut.write(paramType.getCanonicalName());
        pOut.write(' ');
        pOut.write(name);

        ++paramNo;
      }
    }
    pOut.write(", CompletionListener completionListener) throws JAXBException {\n");
    {
      int paramNo = 0;
      for (final ParamInfo param : params) {
        pOut.write("    final Tripple<String, Class<?>, Object> param" + paramNo + " = Tripple.<String, Class<?>, Object>tripple(");
        pOut.write(appendString(new StringBuilder(), param.name).append(", ").toString());
        if (param.type.isArray()) {
          pOut.write("Array.class, ");
        } else {
          if (param.type.isPrimitive()) {
            pOut.write(param.type.getSimpleName());
          } else {
            pOut.write(param.type.getCanonicalName());
          }
          pOut.write(".class, ");
        }
        pOut.write(param.name);
        pOut.write(");\n");

        ++paramNo;
      }
    }
    pOut.write("\n");
    pOut.write("    @SuppressWarnings(\"unchecked\")\n");
    pOut.write("    Source message = SoapHelper.createMessage(new QName(");
    if (pAnnotation.operationName() != null) {
      pOut.write(appendString(new StringBuilder(), pAnnotation.operationName()).append("), ").toString());
    } else {
      pOut.write(appendString(new StringBuilder(), pMethod.getName()).append("), ").toString());
    }
    if (principalName != null) {
      pOut.write("java.util.Arrays.asList(new JAXBElement<String>(new QName(\"http://adaptivity.nl/ProcessEngine/\",\"principal\"), String.class, "
          + principalName + ".getName())), ");
    }
    for (int i = 0; i < params.size(); ++i) {
      if (i > 0) {
        pOut.write(", ");
      }
      pOut.write("param" + i);
    }
    pOut.write(");\n\n");

    pOut.write("    EndpointDescriptor endpoint = new EndPointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);\n\n");

    pOut.write("    return MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, ");
    pOut.write(pMethod.getReturnType().getCanonicalName());

    pOut.write(".class);\n");

    pOut.write("  }\n\n");

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
