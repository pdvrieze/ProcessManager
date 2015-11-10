package nl.adaptivity.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that allows an {@link EndpointDescriptor} to be linked to the type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Descriptor {
  Class<? extends EndpointDescriptor> value();
}
