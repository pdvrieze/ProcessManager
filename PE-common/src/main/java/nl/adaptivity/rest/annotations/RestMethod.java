package nl.adaptivity.rest.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface RestMethod {


  enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD

  }

  @NotNull HttpMethod method();

  @NotNull String path();

  /**
   * Expressions that put conditions on post parameters
   */
  @NotNull String[] post() default {};

  /**
   * Expressions that put conditions on get parameters
   */
  @NotNull String[] get() default {};

  /**
   * Expressions that put conditions on any request parameters (post or get)
   */
  @NotNull String[] query() default {};

  @NotNull String contentType() default "";

}
