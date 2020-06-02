package fr.sorbonne_u.components.qos.solver.booleval.ast.nonterminal;

import fr.sorbonne_u.components.qos.solver.booleval.ast.*;

public class Not extends NonTerminal {

	public void setChild(BooleanExpression child) {
		setLeft(child);
	}

	public void setRight(BooleanExpression right) {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		return String.format("!%s", left);
	}
}
