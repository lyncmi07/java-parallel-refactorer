package safety;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;

/**
 * Simplifies a string equation of an array access' index to allow for it to be checked for equivalence against other indexes of the same array.
 * @author michaellynch
 *
 */
public final class SimplifiedEquation {
    private long iTotal;
    private long nTotal;

    /**
     * Creates a new SimplifiedEquation with the total number of Is and constant value.
     * @param iTotal The number of Is that make up this index equation.
     * @param nTotal The value of the constant offset to I that makes up this index equation.
     */
    private SimplifiedEquation(long iTotal, long nTotal) {
        this.iTotal = iTotal;
        this.nTotal = nTotal;
    }

    public long getITotal() {
        return iTotal;
    }
    public long getNTotal() {
        return nTotal;
    }

    /**
     * Adds this equation to another returning the added result.
     * @param e The equation to add.
     * @return The result of adding the two equations as a simplified result.
     */
    private SimplifiedEquation add(SimplifiedEquation e) {
        return new SimplifiedEquation(iTotal + e.iTotal, nTotal + e.nTotal);
    }

    /**
     * Subtracts the given equation from the current returning the subtracted result.
     * @param e The equation to subtract.
     * @return The result of subtracting the two equations from each other as a simplified result.
     */
    private SimplifiedEquation sub(SimplifiedEquation e) {
        return new SimplifiedEquation(iTotal - e.iTotal, nTotal - e.nTotal);
    }

    /**
     * Multiplies this equation to the given returning the multiplied result.
     * @param e The equation to multiply.
     * @return The result of multiplying the two equations to each other as a simplified result.
     * @throws NumberFormatException When the equation evaluates to having powers of I.
     */
    private SimplifiedEquation multiply(SimplifiedEquation e) throws NumberFormatException {
        if(iTotal > 0 && e.iTotal > 0) {
            throw new NumberFormatException();
        } else {
            return new SimplifiedEquation((iTotal * e.nTotal) + (e.iTotal * nTotal), nTotal * e.nTotal);
        }
    }

    /**
     * Divides the given equation from the current returning the divided result.
     * @param e the equation to divide by.
     * @return The result of dividing the two equations from each other as a simplified result.
     * @throws NumberFormatException When the equation evaluates to having powers of I.
     */
    private SimplifiedEquation divide(SimplifiedEquation e) throws NumberFormatException {
        if(iTotal > 0 && e.iTotal > 0) {
            throw new NumberFormatException();
        } else {
            return new SimplifiedEquation((iTotal / e.nTotal) + (e.iTotal / nTotal), nTotal / e.nTotal);
        }
    }

    /**
     * Creates a SimplifiedEquation from an index expression.
     * @param exprToSimplify The expression to simplify.
     * @param iteratorIdent The identifier of the iterator which the class counts as i.
     * @return The the index expression evaluated to its most simplified form.
     * @throws NumberFormatException When the expression cannot be represented as a SimplifiedEquation.
     */
    public static SimplifiedEquation createSimplifiedEquation(Expression exprToSimplify, String iteratorIdent) throws NumberFormatException {
        if(exprToSimplify.isEnclosedExpr()) {
            return createSimplifiedEquation(exprToSimplify.asEnclosedExpr().getInner(), iteratorIdent);
        } else if(exprToSimplify.isNameExpr() && 
            exprToSimplify.asNameExpr().getNameAsString().equals(iteratorIdent)) {
            return new SimplifiedEquation(1, 0);
        } else if(exprToSimplify.isCastExpr()) {
            return createSimplifiedEquation(exprToSimplify.asCastExpr().getExpression(), iteratorIdent);
        } else if(exprToSimplify.isIntegerLiteralExpr()) {
            return new SimplifiedEquation(0, exprToSimplify.asIntegerLiteralExpr().asInt());
        } else if(exprToSimplify.isLongLiteralExpr()) {
            return new SimplifiedEquation(0, exprToSimplify.asLongLiteralExpr().asLong());
        } else if(exprToSimplify.isDoubleLiteralExpr()) {
            return new SimplifiedEquation(0, (long)exprToSimplify.asDoubleLiteralExpr().asDouble());
        } else if(exprToSimplify.isBinaryExpr()) {
            return getEquationFromBinaryExpr(exprToSimplify.asBinaryExpr(), iteratorIdent);
        }
        
        throw new NumberFormatException();
    }

    private static SimplifiedEquation getEquationFromBinaryExpr(BinaryExpr expr, String iteratorIdent) throws NumberFormatException{
        if(expr.getOperator() == Operator.PLUS) {
            return createSimplifiedEquation(expr.getLeft(), iteratorIdent).add(
                createSimplifiedEquation(expr.getRight(), iteratorIdent)
            );
        } else if(expr.getOperator() == Operator.MINUS) {
            return createSimplifiedEquation(expr.getLeft(), iteratorIdent).sub(
                createSimplifiedEquation(expr.getRight(), iteratorIdent)  
            );
        } else if(expr.getOperator() == Operator.MULTIPLY) {
            return createSimplifiedEquation(expr.getLeft(), iteratorIdent).multiply(
                createSimplifiedEquation(expr.getRight(), iteratorIdent)
            );
        } else if(expr.getOperator() == Operator.DIVIDE) {
            return createSimplifiedEquation(expr.getLeft(), iteratorIdent).divide(
                createSimplifiedEquation(expr.getRight(), iteratorIdent)
            );
        }

        throw new NumberFormatException();
    }

    @Override
    public boolean equals(Object e) {
        if(e instanceof SimplifiedEquation) {
            return (iTotal == ((SimplifiedEquation)e).iTotal && nTotal == ((SimplifiedEquation)e).nTotal);
        }

        return false;
    }
}