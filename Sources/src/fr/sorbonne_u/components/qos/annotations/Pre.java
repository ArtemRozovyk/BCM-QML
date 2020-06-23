package fr.sorbonne_u.components.qos.annotations;

import java.lang.annotation.*;

/**
 * Precondition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pre {
	String expression() ;
}

