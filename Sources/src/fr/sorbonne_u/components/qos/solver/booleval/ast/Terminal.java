package fr.sorbonne_u.components.qos.solver.booleval.ast;

public abstract class Terminal implements BooleanExpression{
	protected boolean value;
	private String constraint;
	public Terminal(boolean value) {
		this.value = value;
	}

	public Terminal(boolean value, String constraint) {
		this.value = value;
		this.constraint = constraint;
	}

	public String toString() {
		if(constraint!=null){
			return constraint;
		}
		return String.format("%s", value);
	}
}
