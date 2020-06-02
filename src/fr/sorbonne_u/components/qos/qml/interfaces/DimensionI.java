package fr.sorbonne_u.components.qos.qml.interfaces;

/**
 * Dimension is used to characterize a particular QoS aspect
 */
public interface DimensionI {
    /**
     *
     * @return containing value
     */
    Object getValue();
    boolean isIncreasig();
    boolean isDecreasing();
}
