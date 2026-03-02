package com.edwards.expressions;

public sealed interface Expr {
    double evaluate();

    record Constant(double val) implements Expr {
        @Override
        public double evaluate() {
            return val;
        }
    }

    record Negate(Expr expr) implements Expr {
        @Override
        public double evaluate() {
            return -expr.evaluate();
        }
    }

    record Addition(Expr expr1, Expr expr2) implements Expr {
        @Override
        public double evaluate() {
            return expr1.evaluate() + expr2.evaluate();
        }
    }

    record Exponent(Expr expr1, Expr expr2) implements Expr {
        @Override
        public double evaluate() {
            return Math.pow(expr1.evaluate(), expr2.evaluate());
        }
    }

    record Multiplication(Expr expr1, Expr expr2) implements Expr {
        @Override
        public double evaluate() {
            return expr1.evaluate() * expr2.evaluate();
        }
    }
}
