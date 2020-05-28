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

    public static boolean IS_DEBUG = true;
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


    static boolean some() throws Exception {
        String server = "x > 1.4 && x <3.0";
        String client = "x > 1.3 && x <3.1";
        List<Expression> clientExprs = new ArrayList<>(Objects.requireNonNull(parseExpressions(client)));
        List<Expression> serverExprs = new ArrayList<>(Objects.requireNonNull(parseExpressions(server)));

        try {
           return verifyAll(serverExprs, clientExprs);
        } catch (ParseException e) {
            e.printStackTrace();
        }
       throw new Exception("someExp");

        //System.out.println(clientExprs.size());
        //System.out.println(serverExprs.size());


    }
    /** Will parse the strings verify if server constraints imply those of client **/

    public static boolean verifyAll(String server,String client){
        List<Expression> clientExprs = new ArrayList<>(Objects.requireNonNull(parseExpressions(client)));
        List<Expression> serverExprs = new ArrayList<>(Objects.requireNonNull(parseExpressions(server)));
        try {
            return verifyAll(serverExprs, clientExprs);
        } catch (ParseException e) {
            e.printStackTrace();
        }
       return false;
    }

    /** Will verify if server constraints imply those of client **/
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
                clientRealVarMap.put(e.var, model.realVar(e.var, minVal, maxVal, 0.0001));
            }
            String operator = getInverseOp(e.op);
            String function=null;
            if(!operator.equals("!=")){
                function = "{0}" + operator + e.value ;
            }else {
                function = "{0}=/="+e.value; //TODO doesnt work
            }

            constraintMap.put("c" + i, model.realIbexGenericConstraint(function, clientRealVarMap.get(e.var)));
            clientTreeStr += "c" + i + " " + (e.nextBoolOp != null ? e.nextBoolOp.charAt(0) : "") + " ";
            i++;
        }
        String serverTreeStr = "";
        for (Expression e : servExpr) {

            if (!clientRealVarMap.containsKey(e.var)) {
                clientRealVarMap.put(e.var, model.realVar(e.var, minVal>=0?minVal:0, maxVal, 0.01));
            }//inverse operators due to bugged not ?
            String operator = e.op.equals("==")?"=":e.op;
            String function;
            if(!operator.equals("!=")){
                function = "{0}" + operator + e.value ;
            }else {
                function = "{0}=/="+e.value; //TODO doesnt work
            }
            constraintMap.put("c" + i, model.realIbexGenericConstraint(function, clientRealVarMap.get(e.var)));
            serverTreeStr += "c" + i + " " + (e.nextBoolOp != null ? e.nextBoolOp.charAt(0) : "") + " ";
            i++;
        }

        //System.out.println(BooleanEvaluator.makeConstraint(BooleanEvaluator.makeExprFromString(serverTreeStr)));
        //System.out.println(BooleanEvaluator.makeConstraint(BooleanEvaluator.makeExprFromString(clientTreeStr)));

        Constraint constraintServer = makeConstraint(model, constraintMap, BooleanEvaluator.makeExprFromString(serverTreeStr),false);
        Constraint constraintClient = makeConstraint(model, constraintMap, BooleanEvaluator.makeExprFromString(clientTreeStr),true);
        //System.out.println(BooleanEvaluator.makeExprFromString(clientTreeStr));
        //System.out.println(BooleanEvaluator.makeConstraintN(BooleanEvaluator.makeExprFromString(clientTreeStr)));
        model.and(constraintServer,constraintClient).post();

        model.getSolver().limitSolution(10);
        model.getSolver().limitTime(15000);
        if(IS_DEBUG){
            System.out.println(model);
        }
        List<Solution> sols = model.getSolver().findAllSolutions();
        if(IS_DEBUG){
            for (Solution s : sols) {
                System.out.println(s);
            }
            System.out.println("Returning solutions size "+sols.size());
        }

        //System.out.println("end");
        //  String server = "x > 1.2 && x <3.0";
        //        String client = "x > 1.1 && x <4";
        return sols.size()==0;
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
            case "!=":
                return "=";
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

    private static Constraint makeConstraint(Model m, Map<String, RealConstraint> cmp, BooleanExpression ast, boolean negate) throws ParseException {
        if (ast instanceof Terminal) {
            RealConstraint constraint = cmp.get(ast.toString());
            Variable v = constraint.getPropagators()[0].getVars()[0];
            return constraint;
        }
        if (ast instanceof And) {
            if(negate)
                return m.or(makeConstraint(m, cmp, ((And) ast).getLeft(), true), makeConstraint(m, cmp, ((And) ast).getRight(), true));
            return m.and(makeConstraint(m, cmp, ((And) ast).getLeft(), false), makeConstraint(m, cmp, ((And) ast).getRight(), false));
        }
        if (ast instanceof Or) {
            if(negate)
                return m.and(makeConstraint(m, cmp, ((Or) ast).getLeft(), true), makeConstraint(m, cmp, ((Or) ast).getRight(), true));
            return m.or(makeConstraint(m, cmp, ((Or) ast).getLeft(), false), makeConstraint(m, cmp, ((Or) ast).getRight(), false));
        }
        if (ast instanceof Not) {
            throw new ParseException("Not doesnt seem to work at the moment", 0);
        }
        throw new ParseException("Ast error", 0);


    }




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

    private static List<Expression> parseExpressions(String origin) {
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

            //System.out.println(equa.length);

        }

        return expressionMap;
    }

    public static void main(String[] args) {
        try {
            //System.out.println(some());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(checkImplicationDouble(0, 0, ""));
    }
    //TODO integer checking
    static boolean checkImplicationInteger(int pLimit, int qLimit, String op) {
        Model model = new Model();

        IntVar ret = model.intVar(0, qLimit);
        IntVar Y = model.intVar(pLimit);
        Constraint c = model.arithm(ret, op, Y);
        model.not(c).post();
        while (model.getSolver().solve()) {
            //System.out.println(ret);
        }
        ;
        return model.getSolver().isFeasible().compareTo(ESat.TRUE) == 0;
    }

    

    static BoolVar cst;
}
