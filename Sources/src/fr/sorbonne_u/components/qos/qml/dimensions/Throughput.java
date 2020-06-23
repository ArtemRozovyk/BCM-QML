package fr.sorbonne_u.components.qos.qml.dimensions;

import fr.sorbonne_u.components.qos.qml.interfaces.*;

public class Throughput implements DimensionI {
    double throughput;

    public static final boolean IS_INCREASING = true;


    public Throughput (double val){
        this.throughput=val;
    }
    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public boolean isIncreasig() {
        return true;
    }

    @Override
    public boolean isDecreasing() {
        return false;
    }
}
