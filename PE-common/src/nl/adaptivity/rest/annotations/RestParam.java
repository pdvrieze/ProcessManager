package nl.adaptivity.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface RestParam {

  
  public enum ParamType {
    QUERY,
    POST,
    GET,
    XPATH,
    VAR,
    ATTACHMENT,
    PRINCIPAL
  }

  String name() default "";
  
  ParamType type() default ParamType.QUERY;
  
  String xpath() default ""; 

}
