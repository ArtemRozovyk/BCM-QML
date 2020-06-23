package fr.sorbonne_u.components.qos.annotations;

import fr.sorbonne_u.components.qos.qml.interfaces.*;

import java.lang.annotation.*;


/**to be used as general method contract**/
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireContract {
    String[] constraints();
    Class<?extends ContractTypeI> contractType();
}
