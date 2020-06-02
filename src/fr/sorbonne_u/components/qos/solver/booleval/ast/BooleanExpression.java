package fr.sorbonne_u.components.qos.solver.booleval.ast;

/**
 * <expression>::=<term>{<or><term>}
 * <term>::=<factor>{<and><factor>}
 * <factor>::=<constant>|<not><factor>|(<expression>)
 * <constant>::= [:aplhanum:]
 * <or>::='|'
 * <and>::='&'
 * <not>::='!'
 */
public interface BooleanExpression {
}
