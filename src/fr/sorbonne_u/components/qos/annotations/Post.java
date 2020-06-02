package fr.sorbonne_u.components.qos.annotations;

import java.lang.annotation.*;

/**
 * Post condition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Post {
	String value() ;
}
