package nl.adaptivity.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface RestMethod {


  public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD;

  }

  HttpMethod method();

  String path();

  /**
   * Expressions that put conditions on post parameters
   */
  String[] post() default {};

  /**
   * Expressions that put conditions on get parameters
   */
  String[] get() default {};

  /**
   * Expressions that put conditions on any request parameters (post or get)
   */
  String[] query() default {};

  String contentType() default "";

}
