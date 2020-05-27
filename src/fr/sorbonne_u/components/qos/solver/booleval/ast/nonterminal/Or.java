package fr.sorbonne_u.components.qos.solver.booleval.ast.nonterminal;

import fr.sorbonne_u.components.qos.solver.booleval.ast.*;

public class Or extends NonTerminal {
	public boolean interpret() {
		return left.interpret() || right.interpret();
	}

	public String toString() {
		return String.format("(%s | %s)", left, right);
	}
}
