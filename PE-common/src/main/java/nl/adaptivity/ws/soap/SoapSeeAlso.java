package nl.adaptivity.ws.soap;

import org.jetbrains.annotations.NotNull;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Like @XmlSeeAlso, but allowed on more types.
 * @author pdvrieze
 *
 */
@Target(value={TYPE, METHOD, PARAMETER})
@Retention(RUNTIME)
public @interface SoapSeeAlso {
  @NotNull Class<?>[] value();
}
