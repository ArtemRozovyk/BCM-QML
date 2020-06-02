package fr.sorbonne_u.components.qos.qml.interfaces;

/**
 * Instance(in QoS sense) of some particular contract type
 * is attached to methods of a component
 */
public interface ContractI {
    /**
     * @return name of the contract
     */
    String getName();

    /**
     * @return the contract type
     */
    Class <? extends ContractTypeI> getType();

    /**
     * @return the constraints
     */
    String [] getConstraints();
}
