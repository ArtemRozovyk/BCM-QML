package fr.sorbonne_u.components.qos.solver.booleval.parser;

import fr.sorbonne_u.components.qos.solver.booleval.ast.*;
import fr.sorbonne_u.components.qos.solver.booleval.ast.nonterminal.*;
import fr.sorbonne_u.components.qos.solver.booleval.ast.terminal.*;
import fr.sorbonne_u.components.qos.solver.booleval.lexer.*;
//
public class RecursiveDescentParser {
	private Lexer lexer;
	private int symbol;
	private BooleanExpression root;



	public RecursiveDescentParser(Lexer lexer) {
		this.lexer = lexer;
	}

	public BooleanExpression build() {
		expression();
		return root;
	}

	private void expression() {
		term();
		while (symbol == Lexer.OR) {
			Or or = new Or();
			or.setLeft(root);
			term();
			or.setRight(root);
			root = or;
		}
	}

	private void term() {
		factor();
		while (symbol == Lexer.AND) {
			And and = new And();
			and.setLeft(root);
			factor();
			and.setRight(root);
			root = and;
		}
	}

	private void factor() {
		symbol = lexer.nextSymbol();
		if (symbol == Lexer.TRUE) {
			root = new True(Lexer.lastWord);
			symbol = lexer.nextSymbol();
		} else if (symbol == Lexer.NOT) {
			Not not = new Not();
			factor();
			not.setChild(root);
			root = not;
		} else if (symbol == Lexer.LEFT) {
			expression();
			symbol = lexer.nextSymbol(); // we don't care about ')'
		} else {
			throw new RuntimeException("Expression Malformed");
		}
	}
}
