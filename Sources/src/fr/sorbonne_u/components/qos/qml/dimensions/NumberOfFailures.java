package fr.sorbonne_u.components.qos.qml.dimensions;

import fr.sorbonne_u.components.qos.qml.interfaces.*;

public class NumberOfFailures implements DimensionI {

    long numberOfFalues;

    public static final boolean IS_INCREASING = false;


    public NumberOfFailures( long nbf){
            this.numberOfFalues=nbf;
    }

    @Override
    public Object getValue() {
        return numberOfFalues;
    }

    @Override
    public boolean isIncreasig() {
        return false;
    }

    @Override
    public boolean isDecreasing() {
        return true;
    }


}
