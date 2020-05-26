package fr.sorbonne_u.components.qos.annotations;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.METHOD;
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
