package nl.adaptivity.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.LOCAL_VARIABLE })
public @interface RestParam {


  public enum ParamType {
    /**
     * This will use both POST and GET parameters, where GET overrides POST.
     */
    QUERY,
    /**
     * The parameter will be resolved through a post body only. The body should
     * be multipart/form-data or application/x-www-form-urlencoded.
     */
    POST,
    /** The parameter will be resolved through the request query only. */
    GET,
    /**
     * The request body is an xml document and the parameter is the result of
     * evaluating the xpath expression on it. Requires setting {@link #xpath()}
     */
    XPATH,
    /**
     * The parameter is resolved through a variable defined in the {@link RestMethod} annotation on the method.
     */
    VAR,
    /**
     * The parameter is the entire request body. This will be processed using
     * the regular unmarshalling algorithms.
     */
    BODY,
    /**
     * The parameter is the request body, but the method will handle it itself.
     */
    ATTACHMENT,
    /**
     * The parameter is fulfilled by the server by providing the logged in user.
     */
    PRINCIPAL
  }

  String name() default "";

  ParamType type() default ParamType.QUERY;

  String xpath() default "";

}
