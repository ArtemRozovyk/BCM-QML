package fr.sorbonne_u.components.qos.qml.dimensions;

import fr.sorbonne_u.components.qos.qml.interfaces.*;

/**persentage of time that component is available throught the year **/
public class Availability implements DimensionI {

    double availability;

    public static final boolean IS_INCREASING = true;


    public Availability(double a){
        this.availability=a;
    }

    @Override
    public Object getValue() {
        return availability;
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
