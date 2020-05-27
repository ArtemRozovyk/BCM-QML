package fr.sorbonne_u.components.qos.solver.booleval.ast.terminal;

import fr.sorbonne_u.components.qos.solver.booleval.ast.*;

public class False extends Terminal {
	public False() {
		super(false);
	}

	public boolean interpret() {
		return value;
	}
}
