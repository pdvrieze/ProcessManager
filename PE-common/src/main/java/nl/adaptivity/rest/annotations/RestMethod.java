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
