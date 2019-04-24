import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class ExpressionElement {
    public static final String LEFT_PARENTHESES = "(";
    public static final String RIGHT_PARENTHESES = ")";

    protected static boolean isLegalDelimeter(String content) {
        if(LEFT_PARENTHESES.equals(content) || RIGHT_PARENTHESES.equals(content))
            return true;
        return false;
    }

    public static final String PLUS = "+";
    public static final String MINUS = "-";
    public static final String MULTIPLE = "×";
    public static final String DIVIDE = "÷";

    protected static final Map<String, Integer> operatorPriority = new HashMap<>();

    static {
        operatorPriority.put(PLUS, 1);
        operatorPriority.put(MINUS, 1);
        operatorPriority.put(MULTIPLE, 2);
        operatorPriority.put(DIVIDE, 2);
    }

    protected static boolean isLegalOperator(String content) {
        if(!operatorPriority.containsKey(content))
            return false;
        return true;
    }

    protected static boolean isLegalOperand(String content) {
        Pattern numberPat = Pattern.compile("(\\+|-)?(\\d+\\.)?\\d+");
        Matcher mat = numberPat.matcher(content);
        if(!mat.matches())
            return false;
        return true;
    }

    protected final String content;

    protected ExpressionElement(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return content.toString();
    }
}

class ExpressionOperator extends ExpressionElement implements Comparable<ExpressionOperator> {
    public static final ExpressionOperator OP_MINUS = new ExpressionOperator(ExpressionElement.MINUS);
    public static final ExpressionOperator OP_PLUS = new ExpressionOperator(ExpressionElement.PLUS);
    public static final ExpressionOperator OP_MULTIPLE = new ExpressionOperator(ExpressionElement.MULTIPLE);
    public static final ExpressionOperator OP_DIVIDE = new ExpressionOperator(ExpressionElement.DIVIDE);

    private final int priority;

    private ExpressionOperator(String content) {
        super(content);

        if(!isLegalOperator(content))
            throw new IllegalArgumentException("operator " + content + "is illegal");

        this.priority = operatorPriority.get(content) ;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(ExpressionOperator other) {
        return this.priority - other.priority;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(obj == this)
            return true;
        if(obj.getClass() != this.getClass())
            return false;

        ExpressionOperator other = (ExpressionOperator) obj;

        return content.equals(other.content);
    }
}

class ExpressionOperand extends ExpressionElement implements Comparable<ExpressionOperand> {
    private final double value;

    public ExpressionOperand(String content) {
        super(content);

        try {
            value = Double.parseDouble(content);
        }
        catch(NumberFormatException e) {
            throw new IllegalArgumentException(content + " is a illegal");
        }
    }

    public ExpressionOperand(double value) {
        super(Double.toString(value));
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int compareTo(ExpressionOperand other) {
        return Double.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }
}

class ExpressionDelimeter extends ExpressionElement {
    public static final ExpressionDelimeter DM_LEFT_PARENTHESES = new ExpressionDelimeter(ExpressionElement.LEFT_PARENTHESES);
    public static final ExpressionDelimeter DM_RIGHT_PARENTHESES = new ExpressionDelimeter(ExpressionElement.RIGHT_PARENTHESES);

    private ExpressionDelimeter(String content) {
        super(content);

        if(!isLegalDelimeter(content))
            throw new IllegalArgumentException("Delimeter " + content + " is illegal");
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(obj == this)
            return true;
        if(obj.getClass() != this.getClass())
            return false;

        ExpressionDelimeter other = (ExpressionDelimeter) obj;

        return content.equals(other.content);
    }
}

abstract class Expression implements Iterable<ExpressionElement> {
    protected final List<ExpressionElement> expression = new ArrayList<>();

    public boolean append(ExpressionElement e) {
        if(e == null) 
            return false;
        
        expression.add(e);

        return true;
    }

    public boolean append(String content) {
        switch(content) {
            case ExpressionElement.LEFT_PARENTHESES:
                expression.add(ExpressionDelimeter.DM_LEFT_PARENTHESES);
                break;
            case ExpressionElement.RIGHT_PARENTHESES:
                expression.add(ExpressionDelimeter.DM_RIGHT_PARENTHESES);
                break;
            case ExpressionElement.PLUS:
                expression.add(ExpressionOperator.OP_PLUS);
                break;
            case ExpressionElement.MINUS:
                expression.add(ExpressionOperator.OP_MINUS);
                break;
            case ExpressionElement.MULTIPLE:
                expression.add(ExpressionOperator.OP_MULTIPLE);
                break;
            case ExpressionElement.DIVIDE:
                expression.add(ExpressionOperator.OP_DIVIDE);
                break;
            default:
                try {
                    ExpressionOperand operand = new ExpressionOperand(content);
                    expression.add(operand);
                }
                catch(Exception e) {
                    return false;
                }
        }

        return true;
    }

    @Override
    public String toString() {
        boolean firstAdd = true;
        StringBuilder sb = new StringBuilder();

        for(ExpressionElement e : expression) {
            if(!firstAdd)
                sb.append(" ");
            else
                firstAdd = false;

            sb.append(e.toString());
        }

        return sb.toString();
    }

    @Override
    public Iterator<ExpressionElement> iterator() {
        return expression.iterator();
    }

    public void clear() {
        expression.clear();
    }

    public abstract double getResultValue() throws Exception;
}

class SuffixExpression extends Expression {
    private double doPlus(ExpressionOperand a, ExpressionOperand b) {
        return a.getValue() + b.getValue();
    }

    private double doMinus(ExpressionOperand a, ExpressionOperand b) {
        return a.getValue() - b.getValue();
    }

    private double doMultiple(ExpressionOperand a, ExpressionOperand b) {
        return a.getValue() * b.getValue();
    }

    private double doDivide(ExpressionOperand a, ExpressionOperand b) {
        return a.getValue() / b.getValue();
    }

    @Override
    public double getResultValue() throws Exception {
        SimpleStack<ExpressionOperand> scalc = new SimpleStack<>();

        for(ExpressionElement e : expression) {
            if(e instanceof ExpressionOperand)
                scalc.push((ExpressionOperand)e);
            else if(e instanceof ExpressionOperator) {
                ExpressionOperator operator = (ExpressionOperator) e;
                ExpressionOperand opf = scalc.pop();
                ExpressionOperand ops = scalc.pop();
                ExpressionOperand temp = null;

                if(opf == null || ops == null)
                    throw new Exception("expression is illegal");

                if(operator.equals(ExpressionOperator.OP_PLUS))
                    temp = new ExpressionOperand(doPlus(ops, opf));
                else if(operator.equals(ExpressionOperator.OP_MINUS))
                    temp = new ExpressionOperand(doMinus(ops, opf));
                else if(operator.equals(ExpressionOperator.OP_MULTIPLE))
                    temp = new ExpressionOperand(doMultiple(ops, opf));
                else if(operator.equals(ExpressionOperator.OP_DIVIDE))
                    temp = new ExpressionOperand(doDivide(ops, opf));

                scalc.push(temp);
            }
        }

        if(scalc.size() != 1)
            throw new Exception("expression is illegal");

        return scalc.pop().getValue();
    }
}

class InfixExpression extends Expression {
    public SuffixExpression toSuffixExpression() {
        SuffixExpression suffix = new SuffixExpression();
        SimpleStack<ExpressionElement> sop = new SimpleStack<>();

        for(ExpressionElement e : expression) {
            if(e instanceof ExpressionOperand)
                suffix.append(e);
            else if(e instanceof ExpressionDelimeter) {
                if(e.equals(ExpressionDelimeter.DM_LEFT_PARENTHESES))
                    sop.push(e);
                else if(e.equals(ExpressionDelimeter.DM_RIGHT_PARENTHESES)) {
                    while(!sop.isEmpty() && !sop.peek().equals(ExpressionDelimeter.DM_LEFT_PARENTHESES))
                        suffix.append(sop.pop());

                    if(!sop.isEmpty())
                        sop.pop();
                }
            }
            else if(e instanceof ExpressionOperator) {
                while(!sop.isEmpty() && sop.peek() instanceof ExpressionOperator && 0 >= ((ExpressionOperator)e).compareTo((ExpressionOperator)sop.peek()))
                    suffix.append(sop.pop());

                sop.push(e);
            }
        }

        while(!sop.isEmpty())
            suffix.append(sop.pop());

        return suffix;
    }

    @Override
    public double getResultValue() throws Exception {
        return toSuffixExpression().getResultValue() ;
    }
}

class SimpleStack<E> {
    private final Deque<E> deque = new LinkedList<E>();

    public E pop() {
        return deque.pop();
    }

    public E peek() {
        return deque.peek();
    }

    public void push(E e) {
        deque.push(e);
    }

    public int size() {
        return deque.size();
    }

    public boolean isEmpty() {
        return deque.size() == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("top[");

        for(E e : deque)
            sb.append(e.toString() + ".");

        sb.append("]bottom");

        return sb.toString();
    }
}
