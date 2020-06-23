package fr.sorbonne_u.components.qos.solver.booleval.ast.terminal;

import fr.sorbonne_u.components.qos.solver.booleval.ast.*;

public class True extends Terminal {
	public True(String constr) {
		super(true,constr);
	}
}
