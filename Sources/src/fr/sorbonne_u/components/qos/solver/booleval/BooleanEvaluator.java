package fr.sorbonne_u.components.qos.solver.booleval;

import fr.sorbonne_u.components.qos.solver.booleval.ast.*;
import fr.sorbonne_u.components.qos.solver.booleval.ast.nonterminal.*;
import fr.sorbonne_u.components.qos.solver.booleval.lexer.*;
import fr.sorbonne_u.components.qos.solver.booleval.parser.*;

import java.io.*;
import java.util.*;

/**
 * Evaluation of  boolean expression and construction of corresponding AST.
 */
public class BooleanEvaluator {

	public static void main(String[] args) throws InterruptedException {
		Scanner sc = new Scanner((System.in));
		String expression = "";
		if(args.length > 0 && args[0].equals("-f")) {
			while(sc.hasNextLine()) expression += sc.nextLine(); System.out.println(expression);
		} else {
			System.out.println("Insert an expression:");
			expression = sc.nextLine();
		}

		Lexer lexer = new Lexer(new ByteArrayInputStream(expression.getBytes()));
		RecursiveDescentParser parser = new RecursiveDescentParser(lexer);
	    BooleanExpression ast = parser.build();
		System.out.println(String.format("AST: %s", ast));

	}

	/**
	 * Construct the Ast
	 * @param expression the exrp
	 * @return ast
	 */
	public static BooleanExpression makeExprFromString(String expression){
		Lexer lexer = new Lexer(new ByteArrayInputStream(expression.getBytes()));
		RecursiveDescentParser parser = new RecursiveDescentParser(lexer);
		return parser.build();
	}




}
