package fr.sorbonne_u.components.qos.annotations;

import fr.sorbonne_u.components.qos.*;
import fr.sorbonne_u.components.qos.interfaces.*;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;


/**to be used as general method contract**/
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireContract {
    String[] constraints();
    Class<?extends ContractTypeI> contractType();
}
