package fr.sorbonne_u.components.qos.qml.contracts;

import fr.sorbonne_u.components.qos.qml.cttypes.*;
import fr.sorbonne_u.components.qos.qml.dimensions.*;
import fr.sorbonne_u.components.qos.qml.interfaces.*;

public class SystemReliability implements ContractI {


    private NumberOfFailures numberOfFailures;
    private Availability availability;

    public SystemReliability(NumberOfFailures numberOfFailures, Availability availability) {
        this.numberOfFailures = numberOfFailures;
        this.availability = availability;
    }

    @Override
    public String getName() {
        return "systemReliability";
    }

    @Override
    public Class<? extends ContractTypeI> getType() {
        return Reliability.class;
    }

    @Override
    public String[] getConstraints() {
        return new String[]{"numberOfFailures < 5",
                "availability > 0.9"};
    }
    public NumberOfFailures getNumberOfFailures() {
        return numberOfFailures;
    }

    public Availability getAvailability() {
        return availability;
    }
}
