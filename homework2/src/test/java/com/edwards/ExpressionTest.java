package com.edwards;

import com.edwards.expressions.Expr;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpressionTest {
    @Test
    public void testPrimitive() {
        var x = new Expr.Constant(2);
        var y = new Expr.Constant(3);

        var add = new Expr.Addition(x, y);
        var mul = new Expr.Multiplication(x, y);
        var exp = new Expr.Exponent(x, y);

        assertEquals(x.val() + y.val(), add.evaluate());
        assertEquals(x.val() * y.val(), mul.evaluate());
        assertEquals(Math.pow(x.val(), y.val()), exp.evaluate());
    }

    @Test
    public void testComplex() {
        var two = new Expr.Constant(2);
        var four = new Expr.Constant(4);
        var negOne = new Expr.Negate(new Expr.Constant(1));
        var sumTwoFour = new Expr.Addition(two, four);
        var mult = new Expr.Multiplication(sumTwoFour, negOne);
        var exp = new Expr.Exponent(mult, new Expr.Constant(2));
        var res = new Expr.Addition(exp, new Expr.Constant(1));

        assertEquals(37, res.evaluate());
    }
}
