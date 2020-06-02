package fr.sorbonne_u.components.qos.qml.cttypes;

import fr.sorbonne_u.components.qos.qml.dimensions.*;
import fr.sorbonne_u.components.qos.qml.interfaces.*;

import java.util.*;

public class Performance implements ContractTypeI {


    Delay delay;
    Throughput throughput;

    public Performance(Delay delay, Throughput throughput) {
        this.delay = delay;
        this.throughput = throughput;
    }

    public Performance() {
    }
}
