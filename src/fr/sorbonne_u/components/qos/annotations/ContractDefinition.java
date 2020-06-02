package fr.sorbonne_u.components.qos.annotations;

import fr.sorbonne_u.components.qos.qml.interfaces.*;


/**
 * Definition of a contract, has to be present in both interfaces(same name)
 * can be multiple
 */
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ContractDefinition.List.class)
public @interface ContractDefinition {

    String name();
    Class<? extends ContractTypeI> type();
    String[] constraints();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @interface List {
        ContractDefinition[] value();
    }
}
