package fr.sorbonne_u.components.qos.interfaces;

public interface ContractI {
    String getName();
    Class <? extends ContractTypeI> getType();
    String [] getConstraints();
}
