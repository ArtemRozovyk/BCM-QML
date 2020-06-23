package fr.sorbonne_u.components.qos.solver.booleval.ast.nonterminal;

import fr.sorbonne_u.components.qos.solver.booleval.ast.*;

public class And extends NonTerminal {

	public String toString() {
		return String.format("(%s & %s)", left, right);
	}
}
