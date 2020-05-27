package fr.sorbonne_u.components.qos.solver.test;

import fr.sorbonne_u.components.qos.solver.*;
import org.junit.Test;

import static org.junit.Assert.*;


public class SolverTesting {


    String server = "x > 1.4 && x <3.0";
    String client = "x > 1.3 && x <3.1";


    @Test
    public void testCorrectConnection1() {
        server = "x > 1.4 && x <3";
        client = "x > 1.3 && x <3.1";
        boolean implies = ChocoSolver.verifyAll(server, client);
        assertTrue(implies);
        System.err.println("Correct: (" + server + ") -> (" + client + ")");
    }

    @Test
    public void testCorrectConnection2() {
        server = "x > 1.4 && x <1 || y > 1.4 && y <3";
        client = "x > 5 && x <3.1 || y > 1.3 && y <3.1";
        boolean implies = ChocoSolver.verifyAll(server, client);
        assertTrue(implies);
        System.err.println("Correct: (" + server + ") -> (" + client + ")");
    }
    @Test
    public void testCorrectConnection3() {
        server = "x > 1.4 && x <1 || y > 1.4 && y <3 && z > 1.4 && z < 1 ";
        client = "x > 5 && x <3.1 || y > 1.5 && y <3.1 && z > 1.1 && z < 3.1";
        boolean implies = ChocoSolver.verifyAll(server, client);
        assertTrue(implies);
        System.err.println("Correct: (" + server + ") -> (" + client + ")");
    }



    @Test
    public void testWrongConnection3() {
        server = "x > 1.4 && x <3.0";
        client = "x > 1.5 && x <3";
        boolean implies = ChocoSolver.verifyAll(server, client);
        assertFalse(implies);
        System.err.println("Wrong : !((" + server + ") -> (" + client + "))");
    }

    @Test
    public void testWrongConnection4() {
        server = "x > 1.4 && x <1 || y > 1.4 && y <3";
        client = "x > 5 && x <3.1 || y > 1.5 && y <3.1";
        boolean implies = ChocoSolver.verifyAll(server, client);
        assertFalse(implies);
        System.err.println("Wrong: !((" + server + ") -> (" + client + "))");
    }
    @Test
    public void testWrongConnection5() {
        server = "x > 1.4 && x <1 || y > 1.4 && y <3  ";
        client = "x > 5 && x <3.1 || y > 1.2 && y <2  ";
        boolean implies = ChocoSolver.verifyAll(server, client);
        assertFalse(implies);
        System.err.println("Wrong: !((" + server + ") -> (" + client + "))");
    }
    @Test
    public void testWrongConnection6() {
        server = "x > 1.4 && x <1 || y > 1.4 && y <3  ";
        client = "x > 5 && x <3.1 || y > 1.2 && y <2  ";
        boolean implies = ChocoSolver.verifyAll(server, client);
        assertFalse(implies);
        System.err.println("Wrong: !((" + server + ") -> (" + client + "))");
    }
    @Test
    public void testWrongConnection9() {
        server = "x > 1.4 && x <1 || y > 1.4 && y <3 && z > 1.4 && z < 2 ";
        client = "x > 5 && x <3.1 || y > 1.9 && y <2 && z > 1.5 && z < 1";
        boolean implies = ChocoSolver.verifyAll(server, client);
        assertFalse(implies);
        System.err.println("Wrong : !((" + server + ") -> (" + client + "))");
    }


}