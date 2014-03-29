package net.devrieze.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Retention(RetentionPolicy.SOURCE)
@Target({PARAMETER, FIELD, LOCAL_VARIABLE, METHOD})
public @interface Nullable {
  /* Just a marker interface */
}
