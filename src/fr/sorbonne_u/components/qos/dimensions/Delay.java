package fr.sorbonne_u.components.qos.dimensions;


import fr.sorbonne_u.components.qos.interfaces.*;

public class Delay implements DimensionI {

    long delay;

    public static final boolean IS_INCREASING = false;

    public Delay(long delay){
        this.delay=delay;
    }
    @Override
    public Object getValue() {
        return delay;
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
