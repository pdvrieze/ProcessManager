package net.devrieze.annotations;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({PARAMETER, FIELD, LOCAL_VARIABLE})
public @interface Nullable {
  /* Just a marker interface */
}
