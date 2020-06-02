package fr.sorbonne_u.components.qos.annotations;

import java.lang.annotation.*;

/**
 * Require a single contract that has already been declared
 */
@Repeatable(Require.List.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Require {
    String contractName();
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @interface List {
        Require[] value();
    }
}
