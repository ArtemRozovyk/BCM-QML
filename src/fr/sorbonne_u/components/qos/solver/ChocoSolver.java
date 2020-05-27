package fr.sorbonne_u.components.qos.solver;

import org.chocosolver.solver.*;
import org.chocosolver.solver.constraints.*;
import org.chocosolver.solver.constraints.real.*;
import org.chocosolver.solver.variables.*;
import org.chocosolver.util.*;
import fr.sorbonne_u.components.qos.solver.booleval.*;
import fr.sorbonne_u.components.qos.solver.booleval.ast.*;
import fr.sorbonne_u.components.qos.solver.booleval.ast.nonterminal.*;

import java.security.*;
import java.text.*;
import java.util.*;

public class ChocoSolver {


    public static boolean checkImplication(Expression servExpr, Expression clientExpr) {


        return false;
    }

    static public final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";


    static class Expression {
        String var;
        String op;
        String value;
        String operationToCarry;
        String nextBoolOp;

        public Expression(String var, String op, String value, String operationToCarry, String nextBoolOp) {
            this.var = var;
            this.op = op;
            this.value = value;
            this.operationToCarry = operationToCarry;
            this.nextBoolOp = nextBoolOp;
        }

        public Expression(String var, String op, String value, String operationToCarry) {
            this.var = var;
            this.op = op;
            this.value = value;
            this.operationToCarry = operationToCarry;
        }
    }


    static void some() {
        String server = "x > 1.4 && x <3.0";
        String client = "x > 1.3 && x <3.1";
        List<Expression> clientExprs = new ArrayList<>(Objects.requireNonNull(getExpressions(client)));
        List<Expression> serverExprs = new ArrayList<>(Objects.requireNonNull(getExpressions(server)));

        try {
            boolean b = verifyAll(serverExprs, clientExprs);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        System.out.println(clientExprs.size());
        System.out.println(serverExprs.size());


    }

    private static boolean verifyAll(List<Expression> servExpr, List<Expression> clientExprs) throws ParseException {
        double maxVal = findMaxValue(servExpr, clientExprs) + 0.1;
        double minVal = findMinValue(servExpr, clientExprs) - 0.1;
        Model model = new Model("Environment Generation");
        Map<String, RealVar> clientRealVarMap = new HashMap<>();
        Map<String, RealConstraint> constraintMap = new HashMap<>();

        int i = 1;
        String clientTreeStr = "";
        for (Expression e : clientExprs) {
            if (!clientRealVarMap.containsKey(e.var)) {
                clientRealVarMap.put(e.var, model.realVar(e.var, minVal, maxVal, 0.01));
            }
            constraintMap.put("c" + i, model.realIbexGenericConstraint("{0}" + getInverseOp(e.op) + e.value, clientRealVarMap.get(e.var)));
            clientTreeStr += "c" + i + " " + (e.nextBoolOp != null ? getInverseOp(e.nextBoolOp.charAt(0) + "") : "") + " ";
            i++;
        }
        String serverTreeStr = "";
        for (Expression e : servExpr) {

            if (!clientRealVarMap.containsKey(e.var)) {
                clientRealVarMap.put(e.var, model.realVar(e.var, minVal, maxVal, 0.01));
            }//inverse operators due to bugged not ?

            constraintMap.put("c" + i, model.realIbexGenericConstraint("{0}" + e.op + e.value, clientRealVarMap.get(e.var)));
            serverTreeStr += "c" + i + " " + (e.nextBoolOp != null ? e.nextBoolOp.charAt(0) : "") + " ";
            i++;
        }

        System.out.println(BooleanEvaluator.makeConstraint(BooleanEvaluator.makeExprFromString(serverTreeStr)));
        System.out.println(BooleanEvaluator.makeConstraint(BooleanEvaluator.makeExprFromString(clientTreeStr)));

        Constraint constraintServer = makeConstraint(model, constraintMap, BooleanEvaluator.makeExprFromString(serverTreeStr));
        Constraint constraintClient = makeConstraint(model, constraintMap, BooleanEvaluator.makeExprFromString(clientTreeStr));

        model.and(constraintServer,constraintClient).post();

        model.getSolver().limitSolution(10);
        model.getSolver().limitTime(15000);
        System.out.println(model);
        for (Solution s : model.getSolver().findAllSolutions()) {
            System.out.println(s);
        }
        System.out.println("end");
        //  String server = "x > 1.2 && x <3.0";
        //        String client = "x > 1.1 && x <4";
        return false;
    }

    private static String getInverseOp(String op) {

        switch (op) {
            case "<":
                return ">=";
            case ">":
                return "<=";
            case "<=":
                return ">";
            case ">=":
                return "<";
            case "==":
                return "!=";
            case ":=":
                return "==";
            case "&":
                return "|";
            case "|":
                return "&";
        }
        return "errorInversing";
    }

    private static double findMinValue(List<Expression> servExpr, List<Expression> clientExprs) {
        double i = Double.MAX_VALUE;
        for (Expression e : clientExprs) {
            double value = Double.parseDouble(e.value);
            if (value < i) {
                i = value;
            }
        }
        for (Expression e : servExpr) {
            double value = Double.parseDouble(e.value);
            if (value < i) {
                i = value;
            }
        }
        return i;
    }

    private static Constraint makeConstraint(Model m, Map<String, RealConstraint> cmp, BooleanExpression ast) throws ParseException {
        if (ast instanceof Terminal) {
            RealConstraint constraint = cmp.get(ast.toString());
            Variable v = constraint.getPropagators()[0].getVars()[0];
            return constraint;
        }
        if (ast instanceof And) {
            return m.and(makeConstraint(m, cmp, ((And) ast).getLeft()), makeConstraint(m, cmp, ((And) ast).getRight()));
        }
        if (ast instanceof Or) {
            return m.or(makeConstraint(m, cmp, ((Or) ast).getLeft()), makeConstraint(m, cmp, ((Or) ast).getRight()));
        }
        if (ast instanceof Not) {
            throw new ParseException("Not doesnt seem to work at the moment", 0);
        }
        throw new ParseException("Ast error", 0);


    }





        /*
        Model model = new Model("Environment Generation");
        System.out.println(model.getName());


        //A
        RealVar x_a = model.realVar("X_a", 0, 3, 0.1);
        RealVar y_a = model.realVar("Y_a", 0, 3, 0.1);


        RealVar cost = model.realVar(0.3);
        RealVar cost2 = model.realVar(1.3);

        model.post(model.realIbexGenericConstraint("{0}<{1}", x_a, cost));
        model.post(model.realIbexGenericConstraint("{0}<{1}", x_a, cost2));


        while (model.getSolver().solve()) {
            System.out.println(x_a + " x_a ");
        }

        return model.getSolver().isFeasible().compareTo(ESat.TRUE) == 0;
    }*/

    private static double findMaxValue(List<Expression> clivarExprs, List<Expression> servvarExprs) {
        double i = -1;
        for (Expression e : clivarExprs) {
            double value = Double.parseDouble(e.value);
            if (value > i) {
                i = value;
            }
        }
        for (Expression e : servvarExprs) {
            double value = Double.parseDouble(e.value);
            if (value > i) {
                i = value;
            }
        }
        return i;
    }

    private static List<Expression> getExpressions(String origin) {
        String[] res = Arrays.stream(origin.split(String.format(WITH_DELIMITER, "&&|\\|\\|"))).map(String::trim).toArray(String[]::new);

        List<Expression> expressionMap = new ArrayList<>();
        Expression lastAdded = null;
        for (String s : res) {
            if (s.equals("&&") || s.equals("||") && lastAdded != null) {
                lastAdded.nextBoolOp = s;
                continue;
            }
            String[] equa = Arrays.stream(s.split
                    (String.format(WITH_DELIMITER, "!=|>|<|==|<=|>="))).map(String::trim).toArray(String[]::new);

            if (equa.length != 3) throw new InvalidParameterException("expr");
            if (equa[0].contains("%")) {
                String[] opeartionOnVar = Arrays.stream(equa[0].split(String.format(WITH_DELIMITER, "%"))).map(String::trim).toArray(String[]::new);
                if (opeartionOnVar.length != 3) throw new InvalidParameterException("opvar");
                lastAdded = new Expression(opeartionOnVar[0], equa[1], equa[2], opeartionOnVar[1] + opeartionOnVar[2], null);
            } else {
                lastAdded = new Expression(equa[0], equa[1], equa[2], null);
            }
            expressionMap.add(lastAdded);

            System.out.println(equa.length);

        }

        return expressionMap;
    }

    public static void main(String[] args) {
        some();
        //System.out.println(checkImplicationDouble(0, 0, ""));
    }

    static boolean checkImplicationInteger(int pLimit, int qLimit, String op) {
        Model model = new Model();

        IntVar ret = model.intVar(0, qLimit);
        IntVar Y = model.intVar(pLimit);
        Constraint c = model.arithm(ret, op, Y);
        model.not(c).post();
        while (model.getSolver().solve()) {
            System.out.println(ret);
        }
        ;
        return model.getSolver().isFeasible().compareTo(ESat.TRUE) == 0;
    }

    static boolean checkImplicationDouble(double pLimit, double qLimit, String op) {

        // x > 3 && y < 4 && y < 1 || x> 2
        Model model = new Model("Environment Generation");
        RealVar x_a = model.realVar("X_a", .1d, 4.d, 1.E-1);

        RealConstraint c1 = model.realIbexGenericConstraint("{0} > 0.8;{0} < 3;", x_a);
        RealConstraint c2 = model.realIbexGenericConstraint("{0} <= 0.7", x_a);
        RealConstraint c3 = model.realIbexGenericConstraint("{0} >= 4", x_a);
        model.post(model.and(c1, model.or(c2, c3)));

        model.getSolver().limitSolution(3);
        System.out.println(model);
        for (Solution s : model.getSolver().findAllSolutions()) {
            System.out.println(s);
        }
        System.out.println(model.getSolver().getFailCount());

        System.out.println(cst);
        return model.getSolver().isFeasible().compareTo(ESat.TRUE) == 0;
    }

    static BoolVar cst;
}
