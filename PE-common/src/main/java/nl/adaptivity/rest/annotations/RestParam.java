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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.rest.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.LOCAL_VARIABLE })
public @interface RestParam {


  enum ParamType {
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

  @NotNull String name() default "";

  @NotNull ParamType type() default ParamType.QUERY;

  @NotNull String xpath() default "";

}
