/*
 * Copyright (c) 1998, 2022 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2022 IBM Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
//     Markus Karg - allow arguments to be specified multiple times in argumentIndices
//     05/07/2009-1.1.1 Dave Brosius
//       - 263904: [PATCH] ExpressionOperator doesn't compare arrays correctly
//     01/23/2018-2.7 Will Dazey
//       - 530214: trim operation should not bind parameters
//     02/01/2022: Tomas Kraus
//       - Issue 1442: Implement New Jakarta Persistence 3.1 Features
package org.eclipse.persistence.expressions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.internal.expressions.ArgumentListFunctionExpression;
import org.eclipse.persistence.internal.expressions.ExpressionJavaPrinter;
import org.eclipse.persistence.internal.expressions.ExpressionSQLPrinter;
import org.eclipse.persistence.internal.expressions.ExtractOperator;
import org.eclipse.persistence.internal.expressions.FunctionExpression;
import org.eclipse.persistence.internal.expressions.LogicalExpression;
import org.eclipse.persistence.internal.expressions.ObjectExpression;
import org.eclipse.persistence.internal.expressions.RelationExpression;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.helper.JavaPlatform;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;

/**
 * <p>
 * <b>Purpose</b>: ADVANCED: The expression operator is used internally to define SQL operations and functions.
 * It is possible for an advanced user to define their own operators.
 */
public class ExpressionOperator implements Serializable {

    /** Required for serialization compatibility. */
    static final long serialVersionUID = -7066100204792043980L;
    protected int selector;
    protected String name;

    // ListExpressionOperator uses its own start/separator/terminator strings
    private String[] databaseStrings;

    protected boolean isPrefix = false;
    protected boolean isRepeating = false;
    protected Class<?> nodeClass;
    protected int type;
    protected int[] argumentIndices = null;

    /** Contains user defined operators */
    protected static Map<Integer, ExpressionOperator> allOperators = initializeOperators();
    /** Contains internal defined operators meant as placeholders for platform operators */
    protected static Map<Integer, ExpressionOperator> allInternalOperators = initializeInternalOperators();

    protected static final Map<String, Integer> platformOperatorSelectors = initializePlatformOperatorSelectors();
    protected static final Map<Integer, String> platformOperatorNames = initializePlatformOperatorNames();
    protected String[] javaStrings;

    /** Allow operator to disable/enable binding for the whole expression.
     * Set to 'null' to enable `isArgumentBindingSupported` finer detail. */
    protected Boolean isBindingSupported = true;

    /** Operator types */
    public static final int LogicalOperator = 1;
    public static final int ComparisonOperator = 2;
    public static final int AggregateOperator = 3;
    public static final int OrderOperator = 4;
    public static final int FunctionOperator = 5;

    /** Logical operators */
    public static final int And = 1;
    public static final int Or = 2;
    public static final int Not = 3;

    /** Comparison operators */
    public static final int Equal = 4;
    public static final int NotEqual = 5;
    public static final int EqualOuterJoin = 6;
    public static final int LessThan = 7;
    public static final int LessThanEqual = 8;
    public static final int GreaterThan = 9;
    public static final int GreaterThanEqual = 10;
    public static final int Like = 11;
    public static final int NotLike = 12;
    public static final int In = 13;
    public static final int InSubQuery = 129;
    public static final int NotIn = 14;
    public static final int NotInSubQuery = 130;
    public static final int Between = 15;
    public static final int NotBetween = 16;
    public static final int IsNull = 17;
    public static final int NotNull = 18;
    public static final int Exists = 86;
    public static final int NotExists = 88;
    public static final int LikeEscape = 89;
    public static final int NotLikeEscape = 134;
    public static final int Decode = 105;
    public static final int Case = 117;
    public static final int NullIf = 131;
    public static final int Coalesce = 132;
    public static final int CaseCondition = 136;
    public static final int Regexp = 141;

    /** Aggregate operators */
    public static final int Count = 19;
    public static final int Sum = 20;
    public static final int Average = 21;
    public static final int Maximum = 22;
    public static final int Minimum = 23;
    public static final int StandardDeviation = 24;
    public static final int Variance = 25;
    public static final int Distinct = 87;

    public static final int As = 148;

    /** Union operators */
    public static final int Union = 142;
    public static final int UnionAll = 143;
    public static final int Intersect = 144;
    public static final int IntersectAll = 145;
    public static final int Except = 146;
    public static final int ExceptAll = 147;

    /** Ordering operators */
    public static final int Ascending = 26;
    public static final int Descending = 27;
    public static final int NullsFirst = 139;
    public static final int NullsLast = 140;

    /** Function operators */

    // General
    public static final int ToUpperCase = 28;
    public static final int ToLowerCase = 29;
    public static final int Chr = 30;
    public static final int Concat = 31;
    public static final int HexToRaw = 32;
    public static final int Initcap = 33;
    public static final int Instring = 34;
    public static final int Soundex = 35;
    public static final int LeftPad = 36;
    public static final int LeftTrim = 37;
    public static final int Replace = 38;
    public static final int RightPad = 39;
    public static final int RightTrim = 40;
    public static final int Substring = 41;
    public static final int ToNumber = 42;
    public static final int Translate = 43;
    public static final int Trim = 44;
    public static final int Ascii = 45;
    public static final int Length = 46;
    public static final int CharIndex = 96;
    public static final int CharLength = 97;
    public static final int Difference = 98;
    public static final int Reverse = 99;
    public static final int Replicate = 100;
    public static final int Right = 101;
    public static final int Locate = 112;
    public static final int Locate2 = 113;
    public static final int ToChar = 114;
    public static final int ToCharWithFormat = 115;
    public static final int RightTrim2 = 116;
    public static final int Any = 118;
    public static final int Some = 119;
    public static final int All = 120;
    public static final int Trim2 = 121;
    public static final int LeftTrim2 = 122;
    public static final int SubstringSingleArg = 133;
    public static final int Cast = 137;
    public static final int Extract = 138;

    // Date
    public static final int AddMonths = 47;
    public static final int DateToString = 48;
    public static final int LastDay = 49;
    public static final int MonthsBetween = 50;
    public static final int NextDay = 51;
    public static final int RoundDate = 52;
    public static final int ToDate = 53;
    /**
     * Function to obtain the current timestamp on the database including date
     * and time components. This corresponds to the JPQL function
     * current_timestamp.
     */
    public static final int Today = 54;
    public static final int AddDate = 90;
    public static final int DateName = 92;
    public static final int DatePart = 93;
    public static final int DateDifference = 94;
    public static final int TruncateDate = 102;
    public static final int NewTime = 103;
    public static final int Nvl = 104;
    /**
     * Function to obtain the current date on the database with date components
     * only but without time components. This corresponds to the JPQL function
     * current_date.
     */
    public static final int CurrentDate = 123;
    /**
     * Function to obtain the current time on the database with time components
     * only but without date components. This corresponds to the JPQL function
     * current_time.
     */
    public static final int CurrentTime = 128;

    public static final int LocalDate = 149;
    public static final int LocalTime = 150;
    public static final int LocalDateTime = 151;

    // Math
    public static final int Ceil = 55;
    public static final int Cos = 56;
    public static final int Cosh = 57;
    public static final int Abs = 58;
    public static final int Acos = 59;
    public static final int Asin = 60;
    public static final int Atan = 61;
    public static final int Exp = 62;
    public static final int Sqrt = 63;
    public static final int Floor = 64;
    public static final int Ln = 65;
    public static final int Log = 66;
    public static final int Mod = 67;
    public static final int Power = 68;
    public static final int Round = 69;
    public static final int Sign = 70;
    public static final int Sin = 71;
    public static final int Sinh = 72;
    public static final int Tan = 73;
    public static final int Tanh = 74;
    public static final int Trunc = 75;
    public static final int Greatest = 76;
    public static final int Least = 77;
    public static final int Add = 78;
    public static final int Subtract = 79;
    public static final int Divide = 80;
    public static final int Multiply = 81;
    public static final int Atan2 = 91;
    public static final int Cot = 95;
    public static final int Negate = 135;

    // Object-relational
    public static final int Deref = 82;
    public static final int Ref = 83;
    public static final int RefToHex = 84;
    public static final int Value = 85;

    //XML Specific
    public static final int ExtractXml = 106;
    public static final int ExtractValue = 107;
    public static final int ExistsNode = 108;
    public static final int GetStringVal = 109;
    public static final int GetNumberVal = 110;
    public static final int IsFragment = 111;

    // Spatial
    public static final int SDO_WITHIN_DISTANCE = 124;
    public static final int SDO_RELATE = 125;
    public static final int SDO_FILTER = 126;
    public static final int SDO_NN = 127;

    /**
     * ADVANCED:
     * Create a new operator.
     */
    public ExpressionOperator() {
        this.type = FunctionOperator;
        // For bug 2780072 provide default behavior to make this class more useable.
        setNodeClass(ClassConstants.FunctionExpression_Class);
    }

    /**
     * ADVANCED:
     * Create a new operator with the given name(s) and strings to print.
     */
    public ExpressionOperator(int selector, List<String> newDatabaseStrings) {
        this.type = FunctionOperator;
        // For bug 2780072 provide default behavior to make this class more useable.
        setNodeClass(ClassConstants.FunctionExpression_Class);
        this.selector = selector;
        this.printsAs(newDatabaseStrings);
    }

    /**
     * PUBLIC:
     * Return if binding is compatible with this operator.
     */
    public Boolean isBindingSupported() {
        return isBindingSupported;
    }

    /**
     * PUBLIC:
     * Set if binding is compatible with this operator.
     * Some databases do not allow binding, or require casting with certain operators.
     */
    public void setIsBindingSupported(boolean isBindingSupported) {
        this.isBindingSupported = isBindingSupported;
    }

    /**
     * INTERNAL:
     * Return if the operator is equal to the other.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }
        ExpressionOperator operator = (ExpressionOperator) object;
        if (getSelector() == 0) {
            return Arrays.equals(getDatabaseStrings(0), operator.getDatabaseStrings(0));
        } else {
            return getSelector() == operator.getSelector();
        }
    }

    /**
     * INTERNAL:
     * Return the hash-code based on the unique selector.
     */
    @Override
    public int hashCode() {
        return getSelector();
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator abs() {
        return simpleFunction(Abs, "ABS");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator acos() {
        return simpleFunction(Acos, "ACOS");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator add() {
        return ExpressionOperator.simpleMath(ExpressionOperator.Add, "+");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator addDate() {
        ExpressionOperator exOperator = simpleThreeArgumentFunction(AddDate, "DATEADD");
        int[] indices = new int[3];
        indices[0] = 1;
        indices[1] = 2;
        indices[2] = 0;

        exOperator.setArgumentIndices(indices);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator addMonths() {
        return simpleTwoArgumentFunction(AddMonths, "ADD_MONTHS");
    }

    /**
     * ADVANCED:
     * Add an operator to the user defined list of operators.
     */
    public static void addOperator(ExpressionOperator exOperator) {
        allOperators.put(exOperator.getSelector(), exOperator);
    }

    /**
     * INTERNAL:
     */
    private static void addOperator(Map<Integer, ExpressionOperator> map, ExpressionOperator exOperator) {
        map.put(Integer.valueOf(exOperator.getSelector()), exOperator);
    }

    /**
     * ADVANCED:
     * Define a name for a user defined operator.
     */
    public static void registerOperator(int selector, String name) {
        platformOperatorNames.put(selector, name);
        platformOperatorSelectors.put(name, selector);
    }

    /**
     * INTERNAL:
     * Create the AND operator.
     */
    public static ExpressionOperator and() {
        return simpleLogical(And, "AND", "and");
    }

    /**
     * INTERNAL:
     * Apply this to an object in memory.
     * Throw an error if the function is not supported.
     */
    public Object applyFunction(Object source, List arguments) {
        if (source instanceof String) {
            if (this.selector == ToUpperCase) {
                return ((String)source).toUpperCase();
            } else if (this.selector == ToLowerCase) {
                return ((String)source).toLowerCase();
            } else if ((this.selector == Concat) && (arguments.size() == 1) && (arguments.get(0) instanceof String)) {
                return ((String)source).concat((String)arguments.get(0));
            } else if ((this.selector == Substring) && (arguments.size() == 2) && (arguments.get(0) instanceof Number) && (arguments.get(1) instanceof Number)) {
                // assume the first parameter to be 1-based first index of the substring, the second - substring length.
                int beginIndexInclusive = ((Number)arguments.get(0)).intValue() - 1;
                int endIndexExclusive = beginIndexInclusive +  ((Number)arguments.get(1)).intValue();
                return ((String)source).substring(beginIndexInclusive, endIndexExclusive);
            } else if ((this.selector == SubstringSingleArg) && (arguments.size() == 1) && (arguments.get(0) instanceof Number)) {
                int beginIndexInclusive = ((Number)arguments.get(0)).intValue() - 1;
                int endIndexExclusive = ((String)source).length();
                return ((String)source).substring(beginIndexInclusive, endIndexExclusive);
            } else if (this.selector == ToNumber) {
                return new java.math.BigDecimal((String)source);
            } else if (this.selector == Trim) {
                return ((String)source).trim();
            } else if (this.selector == Length) {
                return ((String) source).length();
            }
        } else if (source instanceof Number) {
            if (this.selector == Ceil) {
                return Math.ceil(((Number) source).doubleValue());
            } else if (this.selector == Cos) {
                return Math.cos(((Number) source).doubleValue());
            } else if (this.selector == Abs) {
                return Math.abs(((Number) source).doubleValue());
            } else if (this.selector == Acos) {
                return Math.acos(((Number) source).doubleValue());
            } else if (this.selector == Asin) {
                return Math.asin(((Number) source).doubleValue());
            } else if (this.selector == Atan) {
                return Math.atan(((Number) source).doubleValue());
            } else if (this.selector == Exp) {
                return Math.exp(((Number) source).doubleValue());
            } else if (this.selector == Sqrt) {
                return Math.sqrt(((Number) source).doubleValue());
            } else if (this.selector == Floor) {
                return Math.floor(((Number) source).doubleValue());
            } else if (this.selector == Log) {
                return Math.log(((Number) source).doubleValue());
            } else if ((this.selector == Power) && (arguments.size() == 1) && (arguments.get(0) instanceof Number)) {
                return Math.pow(((Number) source).doubleValue(), (((Number) arguments.get(0)).doubleValue()));
            } else if (this.selector == Round) {
                return (double) Math.round(((Number) source).doubleValue());
            } else if (this.selector == Sin) {
                return Math.sin(((Number) source).doubleValue());
            } else if (this.selector == Tan) {
                return Math.tan(((Number) source).doubleValue());
            } else if ((this.selector == Greatest) && (arguments.size() == 1) && (arguments.get(0) instanceof Number)) {
                return Math.max(((Number) source).doubleValue(), (((Number) arguments.get(0)).doubleValue()));
            } else if ((this.selector == Least) && (arguments.size() == 1) && (arguments.get(0) instanceof Number)) {
                return Math.min(((Number) source).doubleValue(), (((Number) arguments.get(0)).doubleValue()));
            } else if ((this.selector == Add) && (arguments.size() == 1) && (arguments.get(0) instanceof Number)) {
                return ((Number) source).doubleValue() + (((Number) arguments.get(0)).doubleValue());
            } else if ((this.selector == Subtract) && (arguments.size() == 1) && (arguments.get(0) instanceof Number)) {
                return ((Number) source).doubleValue() - (((Number) arguments.get(0)).doubleValue());
            } else if ((this.selector == Divide) && (arguments.size() == 1) && (arguments.get(0) instanceof Number)) {
                return ((Number) source).doubleValue() / (((Number) arguments.get(0)).doubleValue());
            } else if ((this.selector == Multiply) && (arguments.size() == 1) && (arguments.get(0) instanceof Number)) {
                return ((Number) source).doubleValue() * (((Number) arguments.get(0)).doubleValue());
            }
        }

        throw QueryException.cannotConformExpression();
    }

    /**
     * INTERNAL:
     * Create the ASCENDING operator.
     */
    public static ExpressionOperator ascending() {
        return simpleOrdering(Ascending, "ASC", "ascending");
    }

    /**
     * INTERNAL:
     * Create the AS operator.
     */
    public static ExpressionOperator as() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(As);
        exOperator.printsAs(" AS ");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the NULLS FIRST ordering operator.
     */
    public static ExpressionOperator nullsFirst() {
        return simpleOrdering(NullsFirst, "NULLS FIRST", "nullsFirst");
    }

    /**
     * INTERNAL:
     * Create the NULLS LAST ordering operator.
     */
    public static ExpressionOperator nullsLast() {
        return simpleOrdering(NullsLast, "NULLS LAST", "nullsLast");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator ascii() {
        return simpleFunction(Ascii, "ASCII");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator asin() {
        return simpleFunction(Asin, "ASIN");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator atan() {
        return simpleFunction(Atan, "ATAN");
    }

    /**
     * INTERNAL:
     * Create the AVERAGE operator.
     */
    public static ExpressionOperator average() {
        return simpleAggregate(Average, "AVG", "average");
    }

    /**
     * ADVANCED:
     * Tell the operator to be postfix, i.e. its strings start printing after
     * those of its first argument.
     */
    public void bePostfix() {
        isPrefix = false;
    }

    /**
     * ADVANCED:
     * Tell the operator to be pretfix, i.e. its strings start printing before
     * those of its first argument.
     */
    public void bePrefix() {
        isPrefix = true;
    }

    /**
     * INTERNAL:
     * Make this a repeating argument. Currently unused.
     */
    public void beRepeating() {
        isRepeating = true;
    }

    /**
     * INTERNAL:
     * Create the BETWEEN Operator
     */
    public static ExpressionOperator between() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(Between);
        result.setType(ComparisonOperator);
        List<String> v = new ArrayList<>();
        v.add("(");
        v.add(" BETWEEN ");
        v.add(" AND ");
        v.add(")");
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Create the NOT BETWEEN operator
     */
    public static ExpressionOperator notBetween() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(NotBetween);
        result.setType(ComparisonOperator);
        List<String> v = new ArrayList<>();
        v.add("(");
        v.add(" NOT BETWEEN ");
        v.add(" AND ");
        v.add(")");;
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Build operator.
     * Note: This operator works differently from other operators.
     * @see Expression#caseStatement(Map, Object)
     */
    public static ExpressionOperator caseStatement() {
        ListExpressionOperator exOperator = new ListExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Case);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.ArgumentListFunctionExpression_Class);
        exOperator.setIsBindingSupported(false);
        exOperator.setStartString("CASE ");
        exOperator.setSeparators(new String[]{" WHEN ", " THEN "});
        exOperator.setTerminationStrings(new String[]{" ELSE ", " END"});
        return exOperator;
    }


    /**
     * INTERNAL:
     * Build operator.
     * Note: This operator works differently from other operators.
     * @see Expression#caseStatement(Map, Object)
     */
    public static ExpressionOperator caseConditionStatement() {
        ListExpressionOperator exOperator = new ListExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(CaseCondition);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.ArgumentListFunctionExpression_Class);
        exOperator.setIsBindingSupported(false);
        exOperator.setStartStrings(new String[]{"CASE WHEN ", " THEN "});
        exOperator.setSeparators(new String[]{" WHEN ", " THEN "});
        exOperator.setTerminationStrings(new String[]{" ELSE ", " END "});
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator ceil() {
        return simpleFunction(Ceil, "CEIL");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator charIndex() {
        return simpleTwoArgumentFunction(CharIndex, "CHARINDEX");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator charLength() {
        return simpleFunction(CharLength, "CHAR_LENGTH");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator chr() {
        return simpleFunction(Chr, "CHR");
    }

    /**
     * INTERNAL:
     * Build operator.
     * Note: This operator works differently from other operators.
     * @see Expression#caseStatement(Map, Object)
     */
    public static ExpressionOperator coalesce() {
        ListExpressionOperator exOperator = new ListExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Coalesce);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.ArgumentListFunctionExpression_Class);
        exOperator.setStartString("COALESCE(");
        exOperator.setSeparator(", ");
        exOperator.setTerminationString(")");
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator concat() {
        ExpressionOperator operator = simpleMath(Concat, "+");
        operator.setIsBindingSupported(false);
        return operator;
    }

    /**
     * INTERNAL:
     * Compare between in memory.
     */
    public boolean conformBetween(Object left, Object right) {
        Object start = ((Vector)right).elementAt(0);
        Object end = ((Vector)right).elementAt(1);
        if ((left == null) || (start == null) || (end == null)) {
            return false;
        }
        if ((left instanceof Number) && (start instanceof Number) && (end instanceof Number)) {
            return ((((Number)left).doubleValue()) >= (((Number)start).doubleValue())) && ((((Number)left).doubleValue()) <= (((Number)end).doubleValue()));
        } else if ((left instanceof String) && (start instanceof String) && (end instanceof String)) {
            return ((((String)left).compareTo(((String)start)) > 0) || (((String)left).compareTo(((String)start)) == 0)) && ((((String)left).compareTo(((String)end)) < 0) || (((String)left).compareTo(((String)end)) == 0));
        } else if ((left instanceof java.util.Date) && (start instanceof java.util.Date) && (end instanceof java.util.Date)) {
            return (((java.util.Date)left).after(((java.util.Date)start)) || left.equals((start))) && (((java.util.Date)left).before(((java.util.Date)end)) || left.equals((end)));
        } else if ((left instanceof java.util.Calendar) && (start instanceof java.util.Calendar) && (end instanceof java.util.Calendar)) {
            return (((java.util.Calendar)left).after(start) || left.equals((start))) && (((java.util.Calendar)left).before(end) || left.equals((end)));
        }

        throw QueryException.cannotConformExpression();
    }

    /**
     * INTERNAL:
     * Compare like in memory.
     * This only works for % not _.
     */
    public boolean conformLike(Object left, Object right) {
        if ((right == null) && (left == null)) {
            return true;
        }
        if (!(right instanceof String) || !(left instanceof String)) {
            throw QueryException.cannotConformExpression();
        }
        String likeString = (String)right;
        if (likeString.indexOf('_') != -1) {
            throw QueryException.cannotConformExpression();
        }
        String value = (String)left;
        if (likeString.indexOf('%') == -1) {
            // No % symbols
            return left.equals(right);
        }
        boolean strictStart = !likeString.startsWith("%");
        boolean strictEnd = !likeString.endsWith("%");
        StringTokenizer tokens = new StringTokenizer(likeString, "%");
        int lastPosition = 0;
        String lastToken = null;
        if (strictStart) {
            lastToken = tokens.nextToken();
            if (!value.startsWith(lastToken)) {
                return false;
            }
        }
        while (tokens.hasMoreTokens()) {
            lastToken = tokens.nextToken();
            lastPosition = value.indexOf(lastToken, lastPosition);
            if (lastPosition < 0) {
                return false;
            }
        }
        if (strictEnd) {
            return value.endsWith(lastToken);
        }
        return true;
    }

    public ExpressionOperator clone(){
        ExpressionOperator clone = new ExpressionOperator();
        this.copyTo(clone);
        return clone;
    }

    public void copyTo(ExpressionOperator operator){
        if(operator == null)
            return;

        operator.selector = selector;
        operator.isPrefix = isPrefix;
        operator.isRepeating = isRepeating;
        operator.nodeClass = nodeClass;
        operator.type = type;
        operator.databaseStrings = databaseStrings == null ? null : Helper.copyStringArray(databaseStrings);
        operator.argumentIndices = argumentIndices == null ? null : Helper.copyIntArray(argumentIndices);
        operator.javaStrings = javaStrings == null ? null : Helper.copyStringArray(javaStrings);
        operator.isBindingSupported = isBindingSupported == null ? null : new Boolean(isBindingSupported);
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator cos() {
        return simpleFunction(Cos, "COS");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator cosh() {
        return simpleFunction(Cosh, "COSH");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator cot() {
        return simpleFunction(Cot, "COT");
    }

    /**
     * INTERNAL:
     * Create the COUNT operator.
     */
    public static ExpressionOperator count() {
        return simpleAggregate(Count, "COUNT", "count");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator dateDifference() {
        return simpleThreeArgumentFunction(DateDifference, "DATEDIFF");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator dateName() {
        return simpleTwoArgumentFunction(DateName, "DATENAME");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator datePart() {
        return simpleTwoArgumentFunction(DatePart, "DATEPART");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator dateToString() {
        return simpleFunction(DateToString, "TO_CHAR");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator toChar() {
        return simpleFunction(ToChar, "TO_CHAR");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator toCharWithFormat() {
        return simpleTwoArgumentFunction(ToCharWithFormat, "TO_CHAR");
    }

    /**
     * INTERNAL:
     * Build operator.
     * Note: This operator works differently from other operators.
     * @see Expression#decode(Map, String)
     */
    public static ExpressionOperator decode() {
        ExpressionOperator exOperator = new ExpressionOperator();

        exOperator.setSelector(Decode);

        exOperator.setNodeClass(FunctionExpression.class);
        exOperator.setType(FunctionOperator);
        exOperator.bePrefix();
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator deref() {
        return simpleFunction(Deref, "DEREF");
    }

    /**
     * INTERNAL:
     * Create the DESCENDING operator.
     */
    public static ExpressionOperator descending() {
        return simpleOrdering(Descending, "DESC", "descending");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator difference() {
        return simpleTwoArgumentFunction(Difference, "DIFFERENCE");
    }

    /**
     * INTERNAL:
     * Create the DISTINCT operator.
     */
    public static ExpressionOperator distinct() {
        return simpleFunction(Distinct, "DISTINCT", "distinct");
    }

    /**
     * INTERNAL:
     * Create the DISTINCT operator.
     */
    public static ExpressionOperator divide() {
        return ExpressionOperator.simpleMath(ExpressionOperator.Divide, "/");
    }

    /**
     * INTERNAL:
     * Compare the values in memory.
     * Used for in-memory querying, all operators are not support.
     */
    public boolean doesRelationConform(Object left, Object right) {
        // Big ugly case statement follows.
        // Java is really verbose, the Smalltalk equivalent to this... left perform: self selector with: right
        // Note, compareTo for String returns a number <= -1 if the String is less than.  We assumed that
        // it would return -1.  The same thing for strings that are greater than (ie it returns >= 1). PWK
        // Equals

        if (this.selector == Equal || this.selector == NotEqual) {
            if ((left == null) && (right == null)) {
                return  this.selector == Equal;
            } else if ((left == null) || (right == null)) {
                return this.selector == NotEqual;
            }
            if (left instanceof Number && left instanceof Comparable && right instanceof Comparable
                && left.getClass().equals (right.getClass())) {
                return (((Comparable) left).compareTo( right) == 0) == (this.selector == Equal);
            }
            if (((left instanceof Number) && (right instanceof Number)) && (left.getClass() != right.getClass())) {
                double leftDouble = ((Number)left).doubleValue();
                double rightDouble = ((Number)right).doubleValue();
                if (Double.isNaN(leftDouble) && Double.isNaN(rightDouble)){
                    return this.selector == Equal;
                }
                return (leftDouble == rightDouble) == (this.selector == Equal);
            }
            return left.equals( right) == (this.selector == Equal);
        } else if (this.selector == IsNull) {
            return (left == null);
        }
        if (this.selector == NotNull) {
            return (left != null);
        }
        // Less thans, greater thans
        else if (this.selector == LessThan) {// You have got to love polymorphism in Java, NOT!!!
            if ((left == null) || (right == null)) {
                return false;
            }
            if ((left instanceof Number) && (right instanceof Number)) {
                return (((Number)left).doubleValue()) < (((Number)right).doubleValue());
            } else if ((left instanceof String) && (right instanceof String)) {
                return ((String)left).compareTo(((String)right)) < 0;
            } else if ((left instanceof java.util.Date) && (right instanceof java.util.Date)) {
                return ((java.util.Date)left).before(((java.util.Date)right));
            } else if ((left instanceof java.util.Calendar) && (right instanceof java.util.Calendar)) {
                return ((java.util.Calendar)left).before(right);
            }
        } else if (this.selector == LessThanEqual) {
            if ((left == null) && (right == null)) {
                return true;
            } else if ((left == null) || (right == null)) {
                return false;
            }
            if ((left instanceof Number) && (right instanceof Number)) {
                return (((Number)left).doubleValue()) <= (((Number)right).doubleValue());
            } else if ((left instanceof String) && (right instanceof String)) {
                int compareValue = ((String)left).compareTo(((String)right));
                return (compareValue < 0) || (compareValue == 0);
            } else if ((left instanceof java.util.Date) && (right instanceof java.util.Date)) {
                return left.equals((right)) || ((java.util.Date)left).before(((java.util.Date)right));
            } else if ((left instanceof java.util.Calendar) && (right instanceof java.util.Calendar)) {
                return left.equals((right)) || ((java.util.Calendar)left).before(right);
            }
        } else if (this.selector == GreaterThan) {
            if ((left == null) || (right == null)) {
                return false;
            }
            if ((left instanceof Number) && (right instanceof Number)) {
                return (((Number)left).doubleValue()) > (((Number)right).doubleValue());
            } else if ((left instanceof String) && (right instanceof String)) {
                int compareValue = ((String)left).compareTo(((String)right));
                return (compareValue > 0);
            } else if ((left instanceof java.util.Date) && (right instanceof java.util.Date)) {
                return ((java.util.Date)left).after(((java.util.Date)right));
            } else if ((left instanceof java.util.Calendar) && (right instanceof java.util.Calendar)) {
                return ((java.util.Calendar)left).after(right);
            }
        } else if (this.selector == GreaterThanEqual) {
            if ((left == null) && (right == null)) {
                return true;
            } else if ((left == null) || (right == null)) {
                return false;
            }
            if ((left instanceof Number) && (right instanceof Number)) {
                return (((Number)left).doubleValue()) >= (((Number)right).doubleValue());
            } else if ((left instanceof String) && (right instanceof String)) {
                int compareValue = ((String)left).compareTo(((String)right));
                return (compareValue > 0) || (compareValue == 0);
            } else if ((left instanceof java.util.Date) && (right instanceof java.util.Date)) {
                return left.equals((right)) || ((java.util.Date)left).after(((java.util.Date)right));
            } else if ((left instanceof java.util.Calendar) && (right instanceof java.util.Calendar)) {
                return left.equals((right)) || ((java.util.Calendar)left).after(right);
            }
        }
        // Between
        else if ((this.selector == Between) && (right instanceof Vector) && (((Vector)right).size() == 2)) {
            return conformBetween(left, right);
        } else if ((this.selector == NotBetween) && (right instanceof Vector) && (((Vector)right).size() == 2)) {
            return !conformBetween(left, right);
        }
        // In
        else if ((this.selector == In) && (right instanceof Collection)) {
            return ((Collection)right).contains(left);
        } else if ((this.selector == NotIn) && (right instanceof Collection)) {
            return !((Collection)right).contains(left);
        }
        // Like
        //conformLike(left, right);
        else if (((this.selector == Like) || (this.selector == NotLike)) && (right instanceof Vector) && (((Vector)right).size() == 1)) {
            Boolean doesLikeConform = JavaPlatform.conformLike(left, ((Vector)right).get(0));
            if (doesLikeConform != null) {
                if (doesLikeConform) {
                    return this.selector == Like;// Negate for NotLike
                } else {
                    return this.selector != Like;// Negate for NotLike
                }
            }
        }
        // Regexp
        else if ((this.selector == Regexp) && (right instanceof Vector) && (((Vector)right).size() == 1)) {
            Boolean doesConform = JavaPlatform.conformRegexp(left, ((Vector)right).get(0));
            if (doesConform != null) {
                return doesConform;
            }
        }

        throw QueryException.cannotConformExpression();
    }

    public static ExpressionOperator equal() {
        return ExpressionOperator.simpleRelation(ExpressionOperator.Equal, "=", "equal");
    }

    /**
     * INTERNAL:
     * Initialize the outer join operator
     * Note: This is merely a shell which is incomplete, and
     * so will be replaced by the platform's operator when we
     * go to print. We need to create this here so that the expression
     * class is correct, normally it assumes functions for unknown operators.
     */
    public static ExpressionOperator equalOuterJoin() {
        return simpleRelation(EqualOuterJoin, "=*");
    }

    /**
     * INTERNAL:
     * Create the EXISTS operator.
     */
    public static ExpressionOperator exists() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Exists);
        List<String> v = new ArrayList<>(2);
        v.add("EXISTS ");
        v.add("");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator exp() {
        return simpleFunction(Exp, "EXP");
    }

    /**
     * INTERNAL:
     * Create an expression for this operator, using the given base.
     */
    public Expression expressionFor(Expression base) {
        return expressionForArguments(base, new ArrayList<>(0));
    }

    /**
     * INTERNAL:
     * Create an expression for this operator, using the given base and a single argument.
     */
    public Expression expressionFor(Expression base, Object value) {
        return newExpressionForArgument(base, value);
    }

    /**
     * INTERNAL:
     * Create an expression for this operator, using the given base and a single argument.
     * Base is used last in the expression
     */
    public Expression expressionForWithBaseLast(Expression base, Object value) {
        return newExpressionForArgumentWithBaseLast(base, value);
    }

    /**
     * INTERNAL:
     * Create an expression for this operator, using the given base and arguments.
     */
    public Expression expressionForArguments(Expression base, List arguments) {
        return newExpressionForArguments(base, arguments);
    }

    /**
     * INTERNAL:
     * Create the extract expression operator
     */
    public static ExpressionOperator extractXml() {
        ExpressionOperator result = new ExpressionOperator();
        List<String> v = new ArrayList<>();
        v.add("extract(");
        v.add(",");
        v.add(")");
        result.printsAs(v);
        result.bePrefix();
        result.setSelector(ExtractXml);
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Create the extractValue expression operator
     */
    public static ExpressionOperator extractValue() {
        ExpressionOperator result = new ExpressionOperator();
        List<String> v = new ArrayList<>();
        v.add("extractValue(");
        v.add(",");
        v.add(")");
        result.printsAs(v);
        result.bePrefix();
        result.setSelector(ExtractValue);
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Create the existsNode expression operator
     */
    public static ExpressionOperator existsNode() {
        ExpressionOperator result = new ExpressionOperator();
        List<String> v = new ArrayList<>();
        v.add("existsNode(");
        v.add(",");
        v.add(")");
        result.printsAs(v);
        result.bePrefix();
        result.setSelector(ExistsNode);
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    public static ExpressionOperator getStringVal() {
        ExpressionOperator result = new ExpressionOperator();
        List<String> v = new ArrayList<>();
        v.add(".getStringVal()");
        result.printsAs(v);
        result.bePostfix();
        result.setSelector(GetStringVal);
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    public static ExpressionOperator getNumberVal() {
        ExpressionOperator result = new ExpressionOperator();
        List<String> v = new ArrayList<>();
        v.add(".getNumberVal()");
        result.printsAs(v);
        result.bePostfix();
        result.setSelector(GetNumberVal);
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    public static ExpressionOperator isFragment() {
        ExpressionOperator result = new ExpressionOperator();
        List<String> v = new ArrayList<>();
        v.add(".isFragment()");
        result.printsAs(v);
        result.bePostfix();
        result.setSelector(IsFragment);
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator floor() {
        return simpleFunction(Floor, "FLOOR");
    }

    /**
     * ADVANCED:
     * Return the map of all operators.
     */
    public static Map<Integer, ExpressionOperator> getAllOperators() {
        return allOperators;
    }

    /**
     * INTERNAL:
     */
    public static Map<Integer, ExpressionOperator> getAllInternalOperators() {
        return allInternalOperators;
    }

    public static Map<String, Integer> getPlatformOperatorSelectors() {
        return platformOperatorSelectors;
    }

    /**
     * INTERNAL:
     */
    public String[] getDatabaseStrings() {
        return databaseStrings;
    }

    /**
     * INTERNAL:
     */
    public String[] getDatabaseStrings(int arguments) {
        return databaseStrings;
    }

    /**
     * INTERNAL:
     */
    public String[] getJavaStrings() {
        return javaStrings;
    }

    /**
     * INTERNAL:
     */
    public Class<?> getNodeClass() {
        return nodeClass;
    }

    /**
     * INTERNAL:
     * Lookup the operator with the given id.
     * <p>
     * This will only check user defined operators. For operators defined internally, see {@link ExpressionOperator#getInternalOperator(Integer)}
     */
    public static ExpressionOperator getOperator(Integer selector) {
        return getAllOperators().get(selector);
    }

    /**
     * INTERNAL:
     * Lookup the internal operator with the given id.
     */
    public static ExpressionOperator getInternalOperator(Integer selector) {
        return getAllInternalOperators().get(selector);
    }

    /**
     * INTERNAL:
     * Return the selector id.
     */
    public int getSelector() {
        return selector;
    }

    /**
     * INTERNAL:
     * Return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * INTERNAL:
     * Set the name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * ADVANCED:
     * Return the type of function.
     * This must be one of the static function types defined in this class.
     */
    public int getType() {
        return this.type;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator greaterThan() {
        return ExpressionOperator.simpleRelation(ExpressionOperator.GreaterThan, ">", "greaterThan");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator greaterThanEqual() {
        return ExpressionOperator.simpleRelation(ExpressionOperator.GreaterThanEqual, ">=", "greaterThanEqual");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator greatest() {
        return simpleTwoArgumentFunction(Greatest, "GREATEST");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator hexToRaw() {
        return simpleFunction(HexToRaw, "HEXTORAW");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator ifNull() {
        return simpleTwoArgumentFunction(Nvl, "NVL");
    }

    /**
     * INTERNAL:
     * Create the IN operator.
     */
    public static ExpressionOperator in() {
        return simpleRelation(In, "IN");
    }

    /**
     * INTERNAL:
     * Create the IN operator taking a subquery.
     * Note, the subquery itself comes with parenethesis, so the IN operator
     * should not add any parenethesis.
     */
    public static ExpressionOperator inSubQuery() {
        ExpressionOperator result = new ExpressionOperator();
        result.setType(ExpressionOperator.FunctionOperator);
        result.setSelector(InSubQuery);
        List<String> v = new ArrayList<>(1);
        v.add(" IN ");
        result.printsAs(v);
        result.bePostfix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator initcap() {
        return simpleFunction(Initcap, "INITCAP");
    }

    /**
     * INTERNAL:
     */
    protected static void initializeAggregateFunctionOperators() {
        addOperator(count());
        addOperator(sum());
        addOperator(average());
        addOperator(minimum());
        addOperator(maximum());
    }

    /**
     * INTERNAL:
     */
    protected static void initializeComparisonOperators() {
        // Comparison Operators
        addOperator(between());
        addOperator(notBetween());
        addOperator(isNull());
        addOperator(notNull());
    }

    /**
     * INTERNAL:
     */
    protected static void initializeFunctionOperators() {
        // Function Operators
        addOperator(like());
        addOperator(likeEscape());
        addOperator(notLike());
        addOperator(notLikeEscape());
        addOperator(exists());
        addOperator(notExists());
        addOperator(notOperator());
        addOperator(as());
        addOperator(any());
        addOperator(some());
        addOperator(all());
        addOperator(inSubQuery());
        addOperator(notInSubQuery());
        addOperator(coalesce());
        addOperator(caseStatement());
        addOperator(caseConditionStatement());
        addOperator(distinct());
    }

    /**
     * INTERNAL:
     */
    protected static void initializeLogicalOperators() {
        // Logical Operators
        addOperator(and());
        addOperator(or());
    }

    /**
     * INTERNAL:
     */
    protected static void initializeOrderOperators() {
        // Order Operators
        addOperator(ascending());
        addOperator(descending());
        addOperator(nullsFirst());
        addOperator(nullsLast());
    }

    /**
     * INTERNAL:
     */
    protected static void initializeRelationOperators() {
        // Relation Operators
        addOperator(equal());
        addOperator(notEqual());
        addOperator(lessThan());
        addOperator(lessThanEqual());
        addOperator(greaterThan());
        addOperator(greaterThanEqual());
        addOperator(in());
        addOperator(notIn());
    }

    /**
     * INTERNAL:
     */
    public static Map<Integer, ExpressionOperator>  initializeOperators() {
        resetOperators();
        return allOperators;
    }

    /**
     * INTERNAL:
     */
    private static Map<Integer, ExpressionOperator> initializeInternalOperators() {
        Map<Integer, ExpressionOperator> allTempOperators = new HashMap<Integer, ExpressionOperator>();

        // Aggregate Function Operators
        addOperator(allTempOperators, count());
        addOperator(allTempOperators, sum());
        addOperator(allTempOperators, average());
        addOperator(allTempOperators, minimum());
        addOperator(allTempOperators, maximum());

        // Comparison Operators
        addOperator(allTempOperators, between());
        addOperator(allTempOperators, notBetween());
        addOperator(allTempOperators, isNull());
        addOperator(allTempOperators, notNull());

        // Function Operators
        addOperator(allTempOperators, like());
        addOperator(allTempOperators, likeEscape());
        addOperator(allTempOperators, notLike());
        addOperator(allTempOperators, notLikeEscape());
        addOperator(allTempOperators, exists());
        addOperator(allTempOperators, notExists());
        addOperator(allTempOperators, notOperator());
        addOperator(allTempOperators, as());
        addOperator(allTempOperators, any());
        addOperator(allTempOperators, some());
        addOperator(allTempOperators, all());
        addOperator(allTempOperators, inSubQuery());
        addOperator(allTempOperators, notInSubQuery());
        addOperator(allTempOperators, coalesce());
        addOperator(allTempOperators, caseStatement());
        addOperator(allTempOperators, caseConditionStatement());
        addOperator(allTempOperators, distinct());

        // Logical Operators
        addOperator(allTempOperators, and());
        addOperator(allTempOperators, or());

        // Order Operators
        addOperator(allTempOperators, ascending());
        addOperator(allTempOperators, descending());
        addOperator(allTempOperators, nullsFirst());
        addOperator(allTempOperators, nullsLast());

        // Relation Operators
        addOperator(allTempOperators, equal());
        addOperator(allTempOperators, notEqual());
        addOperator(allTempOperators, lessThan());
        addOperator(allTempOperators, lessThanEqual());
        addOperator(allTempOperators, greaterThan());
        addOperator(allTempOperators, greaterThanEqual());
        addOperator(allTempOperators, in());
        addOperator(allTempOperators, notIn());

        return allTempOperators;
    }

    /**
     * INTERNAL:
     * Initialize a mapping to the platform operator names for usage with exceptions.
     */
    public static String getPlatformOperatorName(int operator) {
        String name = (String)getPlatformOperatorNames().get(operator);
        if (name == null) {
            name = String.valueOf(operator);
        }
        return name;
    }

    /**
     * INTERNAL:
     * Initialize a mapping to the platform operator names for usage with exceptions.
     */
    public static Map<Integer, String> getPlatformOperatorNames() {
        return platformOperatorNames;
    }

    /**
     * INTERNAL:
     * Initialize a mapping to the platform operator names for usage with exceptions.
     */
    public static Map<Integer, String> initializePlatformOperatorNames() {
        Map<Integer, String> platformOperatorNames = new HashMap<>();
        platformOperatorNames.put(ToUpperCase, "ToUpperCase");
        platformOperatorNames.put(ToLowerCase, "ToLowerCase");
        platformOperatorNames.put(Chr, "Chr");
        platformOperatorNames.put(Concat, "Concat");
        platformOperatorNames.put(Coalesce, "Coalesce");
        platformOperatorNames.put(Case, "Case");
        platformOperatorNames.put(CaseCondition, "Case(codition)");
        platformOperatorNames.put(HexToRaw, "HexToRaw");
        platformOperatorNames.put(Initcap, "Initcap");
        platformOperatorNames.put(Instring, "Instring");
        platformOperatorNames.put(Soundex, "Soundex");
        platformOperatorNames.put(LeftPad, "LeftPad");
        platformOperatorNames.put(LeftTrim, "LeftTrim");
        platformOperatorNames.put(RightPad, "RightPad");
        platformOperatorNames.put(RightTrim, "RightTrim");
        platformOperatorNames.put(Substring, "Substring");
        platformOperatorNames.put(SubstringSingleArg, "Substring");
        platformOperatorNames.put(Translate, "Translate");
        platformOperatorNames.put(Ascii, "Ascii");
        platformOperatorNames.put(Length, "Length");
        platformOperatorNames.put(CharIndex, "CharIndex");
        platformOperatorNames.put(CharLength, "CharLength");
        platformOperatorNames.put(Difference, "Difference");
        platformOperatorNames.put(Reverse, "Reverse");
        platformOperatorNames.put(Replicate, "Replicate");
        platformOperatorNames.put(Right, "Right");
        platformOperatorNames.put(Locate, "Locate");
        platformOperatorNames.put(Locate2, "Locate");
        platformOperatorNames.put(ToNumber, "ToNumber");
        platformOperatorNames.put(ToChar, "ToChar");
        platformOperatorNames.put(ToCharWithFormat, "ToChar");
        platformOperatorNames.put(AddMonths, "AddMonths");
        platformOperatorNames.put(DateToString, "DateToString");
        platformOperatorNames.put(MonthsBetween, "MonthsBetween");
        platformOperatorNames.put(NextDay, "NextDay");
        platformOperatorNames.put(RoundDate, "RoundDate");
        platformOperatorNames.put(AddDate, "AddDate");
        platformOperatorNames.put(DateName, "DateName");
        platformOperatorNames.put(DatePart, "DatePart");
        platformOperatorNames.put(DateDifference, "DateDifference");
        platformOperatorNames.put(TruncateDate, "TruncateDate");
        platformOperatorNames.put(Extract, "Extract");
        platformOperatorNames.put(Cast, "Cast");
        platformOperatorNames.put(NewTime, "NewTime");
        platformOperatorNames.put(Nvl, "Nvl");
        platformOperatorNames.put(NewTime, "NewTime");
        platformOperatorNames.put(Ceil, "Ceil");
        platformOperatorNames.put(Cos, "Cos");
        platformOperatorNames.put(Cosh, "Cosh");
        platformOperatorNames.put(Abs, "Abs");
        platformOperatorNames.put(Acos, "Acos");
        platformOperatorNames.put(Asin, "Asin");
        platformOperatorNames.put(Atan, "Atan");
        platformOperatorNames.put(Exp, "Exp");
        platformOperatorNames.put(Sqrt, "Sqrt");
        platformOperatorNames.put(Floor, "Floor");
        platformOperatorNames.put(Ln, "Ln");
        platformOperatorNames.put(Log, "Log");
        platformOperatorNames.put(Mod, "Mod");
        platformOperatorNames.put(Power, "Power");
        platformOperatorNames.put(Round, "Round");
        platformOperatorNames.put(Sign, "Sign");
        platformOperatorNames.put(Sin, "Sin");
        platformOperatorNames.put(Sinh, "Sinh");
        platformOperatorNames.put(Tan, "Tan");
        platformOperatorNames.put(Tanh, "Tanh");
        platformOperatorNames.put(Trunc, "Trunc");
        platformOperatorNames.put(Greatest, "Greatest");
        platformOperatorNames.put(Least, "Least");
        platformOperatorNames.put(Add, "Add");
        platformOperatorNames.put(Subtract, "Subtract");
        platformOperatorNames.put(Divide, "Divide");
        platformOperatorNames.put(Multiply, "Multiply");
        platformOperatorNames.put(Atan2, "Atan2");
        platformOperatorNames.put(Cot, "Cot");
        platformOperatorNames.put(Deref, "Deref");
        platformOperatorNames.put(Ref, "Ref");
        platformOperatorNames.put(RefToHex, "RefToHex");
        platformOperatorNames.put(Value, "Value");
        platformOperatorNames.put(ExtractXml, "ExtractXml");
        platformOperatorNames.put(ExtractValue, "ExtractValue");
        platformOperatorNames.put(ExistsNode, "ExistsNode");
        platformOperatorNames.put(GetStringVal, "GetStringVal");
        platformOperatorNames.put(GetNumberVal, "GetNumberVal");
        platformOperatorNames.put(IsFragment, "IsFragment");
        platformOperatorNames.put(SDO_WITHIN_DISTANCE, "MDSYS.SDO_WITHIN_DISTANCE");
        platformOperatorNames.put(SDO_RELATE, "MDSYS.SDO_RELATE");
        platformOperatorNames.put(SDO_FILTER, "MDSYS.SDO_FILTER");
        platformOperatorNames.put(SDO_NN, "MDSYS.SDO_NN");
        platformOperatorNames.put(NullIf, "NullIf");
        platformOperatorNames.put(Regexp, "REGEXP");
        platformOperatorNames.put(Union, "UNION");
        platformOperatorNames.put(UnionAll, "UNION ALL");
        platformOperatorNames.put(Intersect, "INTERSECT");
        platformOperatorNames.put(IntersectAll, "INTERSECT ALL");
        platformOperatorNames.put(Except, "EXCEPT");
        platformOperatorNames.put(ExceptAll, "EXCEPT ALL");
        return platformOperatorNames;
    }

    /**
     * INTERNAL:
     * Initialize a mapping to the platform operator names for usage with exceptions.
     */
    public static Map<String, Integer> initializePlatformOperatorSelectors() {
        Map<String, Integer> platformOperatorNames = new HashMap<>();
        platformOperatorNames.put("ToUpperCase", ToUpperCase);
        platformOperatorNames.put("ToLowerCase", ToLowerCase);
        platformOperatorNames.put("Chr", Chr);
        platformOperatorNames.put("Concat", Concat);
        platformOperatorNames.put("Coalesce", Coalesce);
        platformOperatorNames.put("Case", Case);
        platformOperatorNames.put("HexToRaw", HexToRaw);
        platformOperatorNames.put("Initcap", Initcap);
        platformOperatorNames.put("Instring", Instring);
        platformOperatorNames.put("Soundex", Soundex);
        platformOperatorNames.put("LeftPad", LeftPad);
        platformOperatorNames.put("LeftTrim", LeftTrim);
        platformOperatorNames.put("RightPad", RightPad);
        platformOperatorNames.put("RightTrim", RightTrim);
        platformOperatorNames.put("Substring", Substring);
        platformOperatorNames.put("Translate", Translate);
        platformOperatorNames.put("Ascii", Ascii);
        platformOperatorNames.put("Length", Length);
        platformOperatorNames.put("CharIndex", CharIndex);
        platformOperatorNames.put("CharLength", CharLength);
        platformOperatorNames.put("Difference", Difference);
        platformOperatorNames.put("Reverse", Reverse);
        platformOperatorNames.put("Replicate", Replicate);
        platformOperatorNames.put("Right", Right);
        platformOperatorNames.put("Locate", Locate);
        platformOperatorNames.put("ToNumber", ToNumber);
        platformOperatorNames.put("ToChar", ToChar);
        platformOperatorNames.put("AddMonths", AddMonths);
        platformOperatorNames.put("DateToString", DateToString);
        platformOperatorNames.put("MonthsBetween", MonthsBetween);
        platformOperatorNames.put("NextDay", NextDay);
        platformOperatorNames.put("RoundDate", RoundDate);
        platformOperatorNames.put("AddDate", AddDate);
        platformOperatorNames.put("DateName", DateName);
        platformOperatorNames.put("DatePart", DatePart);
        platformOperatorNames.put("DateDifference", DateDifference);
        platformOperatorNames.put("TruncateDate", TruncateDate);
        platformOperatorNames.put("NewTime", NewTime);
        platformOperatorNames.put("Nvl", Nvl);
        platformOperatorNames.put("NewTime", NewTime);
        platformOperatorNames.put("Ceil", Ceil);
        platformOperatorNames.put("Cos", Cos);
        platformOperatorNames.put("Cosh", Cosh);
        platformOperatorNames.put("Abs", Abs);
        platformOperatorNames.put("Acos", Acos);
        platformOperatorNames.put("Asin", Asin);
        platformOperatorNames.put("Atan", Atan);
        platformOperatorNames.put("Exp", Exp);
        platformOperatorNames.put("Sqrt", Sqrt);
        platformOperatorNames.put("Floor", Floor);
        platformOperatorNames.put("Ln", Ln);
        platformOperatorNames.put("Log", Log);
        platformOperatorNames.put("Mod", Mod);
        platformOperatorNames.put("Power", Power);
        platformOperatorNames.put("Round", Round);
        platformOperatorNames.put("Sign", Sign);
        platformOperatorNames.put("Sin", Sin);
        platformOperatorNames.put("Sinh", Sinh);
        platformOperatorNames.put("Tan", Tan);
        platformOperatorNames.put("Tanh", Tanh);
        platformOperatorNames.put("Trunc", Trunc);
        platformOperatorNames.put("Greatest", Greatest);
        platformOperatorNames.put("Least", Least);
        platformOperatorNames.put("Add", Add);
        platformOperatorNames.put("Subtract", Subtract);
        platformOperatorNames.put("Divide", Divide);
        platformOperatorNames.put("Multiply", Multiply);
        platformOperatorNames.put("Atan2", Atan2);
        platformOperatorNames.put("Cot", Cot);
        platformOperatorNames.put("Deref", Deref);
        platformOperatorNames.put("Ref", Ref);
        platformOperatorNames.put("RefToHex", RefToHex);
        platformOperatorNames.put("Value", Value);
        platformOperatorNames.put("Cast", Cast);
        platformOperatorNames.put("Extract", Extract);
        platformOperatorNames.put("ExtractXml", ExtractXml);
        platformOperatorNames.put("ExtractValue", ExtractValue);
        platformOperatorNames.put("ExistsNode", ExistsNode);
        platformOperatorNames.put("GetStringVal", GetStringVal);
        platformOperatorNames.put("GetNumberVal", GetNumberVal);
        platformOperatorNames.put("IsFragment", IsFragment);
        platformOperatorNames.put("SDO_WITHIN_DISTANCE", SDO_WITHIN_DISTANCE);
        platformOperatorNames.put("SDO_RELATE", SDO_RELATE);
        platformOperatorNames.put("SDO_FILTER", SDO_FILTER);
        platformOperatorNames.put("SDO_NN", SDO_NN);
        platformOperatorNames.put("NullIf", NullIf);
        return platformOperatorNames;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator instring() {
        return simpleTwoArgumentFunction(Instring, "INSTR");
    }

    /**
     * Aggregate functions are function in the select such as COUNT.
     */
    public boolean isAggregateOperator() {
        return getType() == AggregateOperator;
    }

    /**
     * Comparison functions are functions such as = and {@literal >}.
     */
    public boolean isComparisonOperator() {
        return getType() == ComparisonOperator;
    }

    /**
     * INTERNAL:
     * If we have all the required information, this operator is complete
     * and can be used as is. Otherwise we will need to look up a platform-
     * specific operator.
     */
    public boolean isComplete() {
        return (databaseStrings != null) && (databaseStrings.length != 0);
    }

    /**
     * General functions are any normal function such as UPPER.
     */
    public boolean isFunctionOperator() {
        return getType() == FunctionOperator;
    }

    /**
     * Logical functions are functions such as and and or.
     */
    public boolean isLogicalOperator() {
        return getType() == LogicalOperator;
    }

    /**
     * INTERNAL:
     * Create the ISNULL operator.
     */
    public static ExpressionOperator isNull() {
        ExpressionOperator result = new ExpressionOperator();
        result.setType(ComparisonOperator);
        result.setSelector(IsNull);
        List<String> v = new ArrayList<>();
        v.add("(");
        v.add(" IS NULL)");
        result.printsAs(v);
        result.bePrefix();
        result.printsJavaAs(".isNull()");
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * Order functions are used in the order by such as ASC.
     */
    public boolean isOrderOperator() {
        return getType() == OrderOperator;
    }

    /**
     * ADVANCED:
     * Return true if this is a prefix operator.
     */
    public boolean isPrefix() {
        return isPrefix;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator lastDay() {
        return simpleFunction(LastDay, "LAST_DAY");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator least() {
        return simpleTwoArgumentFunction(Least, "LEAST");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator leftPad() {
        return simpleThreeArgumentFunction(LeftPad, "LPAD");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator leftTrim() {
        return simpleFunction(LeftTrim, "LTRIM");
    }

    /**
     * INTERNAL:
     * Build leftTrim operator that takes one parameter.
     */
    public static ExpressionOperator leftTrim2() {
        ExpressionOperator operator = simpleTwoArgumentFunction(LeftTrim2, "LTRIM");
        return operator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator length() {
        return simpleFunction(Length, "LENGTH");
    }

    public static ExpressionOperator lessThan() {
        return ExpressionOperator.simpleRelation(ExpressionOperator.LessThan, "<", "lessThan");
    }

    public static ExpressionOperator lessThanEqual() {
        return ExpressionOperator.simpleRelation(ExpressionOperator.LessThanEqual, "<=", "lessThanEqual");
    }

    /**
     * INTERNAL:
     * Create the LIKE operator.
     */
    public static ExpressionOperator like() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(Like);
        result.setType(FunctionOperator);
        List<String> v = new ArrayList<>(3);
        v.add("");
        v.add(" LIKE ");
        v.add("");
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        v = new ArrayList<>(2);
        v.add(".like(");
        v.add(")");
        result.printsJavaAs(v);
        return result;
    }

    /**
     * INTERNAL:
     * Create the REGEXP operator.
     * REGEXP allows for comparison through regular expression,
     * this is supported by many databases and with be part of the next SQL standard.
     */
    public static ExpressionOperator regexp() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(Regexp);
        result.setType(FunctionOperator);
        List<String> v = new ArrayList<>(3);
        v.add("");
        v.add(" REGEXP ");
        v.add("");
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        v = new ArrayList<>(2);
        v.add(".regexp(");
        v.add(")");
        result.printsJavaAs(v);
        return result;
    }

    /**
     * INTERNAL:
     * Create the LIKE operator with ESCAPE.
     */
    public static ExpressionOperator likeEscape() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(LikeEscape);
        result.setType(FunctionOperator);
        List<String> v = new ArrayList<>();
        v.add("");
        v.add(" LIKE ");
        v.add(" ESCAPE ");
        v.add("");
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        result.setIsBindingSupported(false);
        return result;
    }

    /**
     * INTERNAL:
     * Create the LIKE operator with ESCAPE.
     */
    public static ExpressionOperator notLikeEscape() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(NotLikeEscape);
        result.setType(FunctionOperator);
        List<String> v = new ArrayList<>();
        v.add("");
        v.add(" NOT LIKE ");
        v.add(" ESCAPE ");
        v.add("");
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        result.setIsBindingSupported(false);
        return result;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator ln() {
        return simpleFunction(Ln, "LN");
    }

    /**
     * INTERNAL:
     * Build locate operator i.e. LOCATE("ob", t0.F_NAME)
     */
    public static ExpressionOperator locate() {
        ExpressionOperator expOperator = simpleTwoArgumentFunction(Locate, "LOCATE");
        int[] argumentIndices = new int[2];
        argumentIndices[0] = 1;
        argumentIndices[1] = 0;
        expOperator.setArgumentIndices(argumentIndices);
        expOperator.setIsBindingSupported(false);
        return expOperator;
    }

    /**
     * INTERNAL:
     * Build locate operator with 3 params i.e. LOCATE("coffee", t0.DESCRIP, 4).
     * Last parameter is a start at.
     */
    public static ExpressionOperator locate2() {
        ExpressionOperator expOperator = simpleThreeArgumentFunction(Locate2, "LOCATE");
        int[] argumentIndices = new int[3];
        argumentIndices[0] = 1;
        argumentIndices[1] = 0;
        argumentIndices[2] = 2;
        expOperator.setArgumentIndices(argumentIndices);
        expOperator.setIsBindingSupported(false);
        return expOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator log() {
        return simpleFunction(Log, "LOG");
    }

    /**
     * INTERNAL:
     * Create the MAXIMUM operator.
     */
    public static ExpressionOperator maximum() {
        return simpleAggregate(Maximum, "MAX", "maximum");
    }

    /**
     * INTERNAL:
     * Create the MINIMUM operator.
     */
    public static ExpressionOperator minimum() {
        return simpleAggregate(Minimum, "MIN", "minimum");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator mod() {
        ExpressionOperator operator = simpleTwoArgumentFunction(Mod, "MOD");
        operator.setIsBindingSupported(false);
        return operator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator monthsBetween() {
        return simpleTwoArgumentFunction(MonthsBetween, "MONTHS_BETWEEN");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator multiply() {
        return ExpressionOperator.simpleMath(ExpressionOperator.Multiply, "*");
    }

    /**
     * INTERNAL:
     * Create a new expression. Optimized for the single argument case.
     */
    public Expression newExpressionForArgument(Expression base, Object singleArgument) {
        if (singleArgument == null) {
            if (this.selector == Equal) {
                return base.isNull();
            } else if (this.selector == NotEqual) {
                return base.notNull();
            }
        }
        Expression node = createNode();
        node.create(base, singleArgument, this);
        return node;
    }

    /**
     * INTERNAL:
     * Instantiate an instance of the operator's node class.
     */
    protected Expression createNode() {
        // PERF: Avoid reflection for common cases.
        if (this.nodeClass == ClassConstants.ArgumentListFunctionExpression_Class){
            return new ArgumentListFunctionExpression();
        } else if (this.nodeClass == ClassConstants.FunctionExpression_Class) {
            return new FunctionExpression();

        } else if (this.nodeClass == ClassConstants.RelationExpression_Class) {
            return new RelationExpression();
        } else if (this.nodeClass == ClassConstants.LogicalExpression_Class) {
            return new LogicalExpression();
        }
        try {
            return (Expression) PrivilegedAccessHelper.callDoPrivilegedWithException(
                    () -> PrivilegedAccessHelper.newInstanceFromClass(getNodeClass())
            );
        } catch (Exception ex) {
            throw new InternalError(ex.toString());
        }
    }

    /**
     * INTERNAL:
     * Create a new expression. Optimized for the single argument case with base last
     */
    public Expression newExpressionForArgumentWithBaseLast(Expression base, Object singleArgument) {
        if (singleArgument == null) {
            if (this.selector == Equal) {
                return base.isNull();
            } else if (this.selector == NotEqual) {
                return base.notNull();
            }
        }
        Expression node = createNode();
        node.createWithBaseLast(base, singleArgument, this);
        return node;
    }

    /**
     * INTERNAL:
     * The general case.
     */
    public Expression newExpressionForArguments(Expression base, List arguments) {
        if ((arguments.size() == 1) && (arguments.get(0) == null)) {
            if (this.selector == Equal) {
                return base.isNull();
            } else if (this.selector == NotEqual) {
                return base.notNull();
            }
        }
        Expression node = createNode();
        node.create(base, arguments, this);
        return node;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator negate() {
        return simpleFunction(Negate, "-");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator newTime() {
        return simpleThreeArgumentFunction(NewTime, "NEW_TIME");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator nextDay() {
        return simpleTwoArgumentFunction(NextDay, "NEXT_DAY");
    }

    public static ExpressionOperator notEqual() {
        return ExpressionOperator.simpleRelation(ExpressionOperator.NotEqual, "<>", "notEqual");
    }

    /**
     * INTERNAL:
     * Create the NOT EXISTS operator.
     */
    public static ExpressionOperator notExists() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(NotExists);
        List<String> v = new ArrayList<>(2);
        v.add("NOT EXISTS ");
        v.add("");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the NOTIN operator.
     */
    public static ExpressionOperator notIn() {
        return simpleRelation(NotIn, "NOT IN");
    }

    /**
     * INTERNAL:
     * Create the NOTIN operator taking a subQuery.
     * Note, the subquery itself comes with parenethesis, so the IN operator
     * should not add any parenethesis.
     */
    public static ExpressionOperator notInSubQuery() {
        ExpressionOperator result = new ExpressionOperator();
        result.setType(ExpressionOperator.FunctionOperator);
        result.setSelector(NotInSubQuery);
        List<String> v = new ArrayList<>(1);
        v.add(" NOT IN ");
        result.printsAs(v);
        result.bePostfix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Create the NOTLIKE operator.
     */
    public static ExpressionOperator notLike() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(NotLike);
        result.setType(FunctionOperator);
        List<String> v = new ArrayList<>();
        v.add("");
        v.add(" NOT LIKE ");
        v.add("");
        result.printsAs(v);
        result.bePrefix();
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        v = new ArrayList<>(2);
        v.add(".notLike(");
        v.add(")");
        result.printsJavaAs(v);
        return result;
    }

    /**
     * INTERNAL:
     * Create the NOTNULL operator.
     */
    public static ExpressionOperator notNull() {
        ExpressionOperator result = new ExpressionOperator();
        result.setType(ComparisonOperator);
        result.setSelector(NotNull);
        List<String> v = new ArrayList<>();
        v.add("(");
        v.add(" IS NOT NULL)");
        result.printsAs(v);
        result.bePrefix();
        result.printsJavaAs(".notNull()");
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Create the NOT operator.
     */
    public static ExpressionOperator notOperator() {
        ExpressionOperator result = new ExpressionOperator();
        result.setSelector(Not);
        List<String> v = new ArrayList<>();
        v.add("NOT (");
        v.add(")");
        result.printsAs(v);
        result.bePrefix();
        result.printsJavaAs(".not()");
        result.setNodeClass(ClassConstants.FunctionExpression_Class);
        return result;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator nullIf() {
        return simpleTwoArgumentFunction(NullIf, "NULLIF");
    }

    /**
     * INTERNAL:
     * Create the OR operator.
     */
    public static ExpressionOperator or() {
        return simpleLogical(Or, "OR", "or");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator power() {
        return simpleTwoArgumentFunction(Power, "POWER");
    }

    /**
     * INTERNAL: Print the collection onto the SQL stream.
     */
    public void printCollection(List<Expression> items, ExpressionSQLPrinter printer) {
        /*
         * If this ExpressionOperator does not support binding, and the platform allows,
         * then disable binding for the whole query
         */
        if (printer.getPlatform().isDynamicSQLRequiredForFunctions() && Boolean.FALSE.equals(isBindingSupported())) {
            printer.getCall().setUsesBinding(false);
        }

        int dbStringIndex = 0;
        if (isPrefix()) {
            printer.printString(getDatabaseStrings()[0]);
            dbStringIndex = 1;
        }

        int[] indices = getArgumentIndices(items.size());
        String[] dbStrings = getDatabaseStrings(items.size());
        for (int i = 0; i < indices.length; i++) {
            final int index = indices[i];
            Expression item = items.get(index);

            if ((this.selector == Ref) || ((this.selector == Deref) && (item.isObjectExpression()))) {
                DatabaseTable alias = item.aliasForTable(((ObjectExpression)item).getDescriptor().getTables().firstElement());
                printer.printString(alias.getNameDelimited(printer.getPlatform()));
            } else if ((this.selector == Count) && (item.isExpressionBuilder())) {
                printer.printString("*");
            } else {
                item.printSQL(printer);
            }
            if (dbStringIndex < dbStrings.length) {
                printer.printString(dbStrings[dbStringIndex++]);
            }
        }
    }

    /**
     * INTERNAL: Print the collection onto the SQL stream.
     */
    public void printJavaCollection(List<Expression> items, ExpressionJavaPrinter printer) {
        int javaStringIndex = 0;

        for (int i = 0; i < items.size(); i++) {
            Expression item = items.get(i);
            item.printJava(printer);
            if (javaStringIndex < getJavaStrings().length) {
                printer.printString(getJavaStrings()[javaStringIndex++]);
            }
        }
    }

    /**
     * INTERNAL:
     * For performance, special case printing two children, since it's by far the most common
     */
    public void printDuo(Expression first, Expression second, ExpressionSQLPrinter printer) {
        /*
         * If this ExpressionOperator does not support binding, and the platform allows,
         * then disable binding for the whole query
         */
        if (printer.getPlatform().isDynamicSQLRequiredForFunctions() && Boolean.FALSE.equals(isBindingSupported())) {
            printer.getCall().setUsesBinding(false);
        }

        int dbStringIndex = 0;
        if (isPrefix()) {
            printer.printString(getDatabaseStrings()[0]);
            dbStringIndex = 1;
        }

        first.printSQL(printer);
        if (dbStringIndex < getDatabaseStrings().length) {
            printer.printString(getDatabaseStrings()[dbStringIndex++]);
        }
        if (second != null) {
            second.printSQL(printer);
            if (dbStringIndex < getDatabaseStrings().length) {
                printer.printString(getDatabaseStrings()[dbStringIndex++]);
            }
        }
    }

    /**
     * INTERNAL:
     * For performance, special case printing two children, since it's by far the most common
     */
    public void printJavaDuo(Expression first, Expression second, ExpressionJavaPrinter printer) {
        int javaStringIndex = 0;

        first.printJava(printer);
        if (javaStringIndex < getJavaStrings().length) {
            printer.printString(getJavaStrings()[javaStringIndex++]);
        }
        if (second != null) {
            second.printJava(printer);
            if (javaStringIndex < getJavaStrings().length) {
                printer.printString(getJavaStrings()[javaStringIndex]);
            }
        }
    }

    /**
     * ADVANCED:
     * Set the single string for this operator.
     */
    public void printsAs(String s) {
        List<String> v = new ArrayList<>(1);
        v.add(s);
        printsAs(v);
    }

    /**
     * ADVANCED:
     * Set the strings for this operator.
     */
    public void printsAs(List<String> dbStrings) {
        this.databaseStrings = new String[dbStrings.size()];
        for (int i = 0; i < dbStrings.size(); i++) {
            getDatabaseStrings()[i] = dbStrings.get(i);
        }
    }

    /**
     * ADVANCED:
     * Set the single string for this operator.
     */
    public void printsJavaAs(String s) {
        List<String> v = new ArrayList<>(1);
        v.add(s);
        printsJavaAs(v);
    }

    /**
     * ADVANCED:
     * Set the strings for this operator.
     */
    public void printsJavaAs(List<String> dbStrings) {
        this.javaStrings = new String[dbStrings.size()];
        for (int i = 0; i < dbStrings.size(); i++) {
            getJavaStrings()[i] = dbStrings.get(i);
        }
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator ref() {
        return simpleFunction(Ref, "REF");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator refToHex() {
        return simpleFunction(RefToHex, "REFTOHEX");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator replace() {
        ExpressionOperator operator = simpleThreeArgumentFunction(Replace, "REPLACE");
        operator.setIsBindingSupported(false);
        return operator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator replicate() {
        return simpleTwoArgumentFunction(Replicate, "REPLICATE");
    }

    /**
     * INTERNAL:
     * Reset all the operators.
     */
    public static void resetOperators() {
        allOperators = new HashMap<Integer, ExpressionOperator>();
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator reverse() {
        return simpleFunction(Reverse, "REVERSE");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator right() {
        return simpleTwoArgumentFunction(Right, "RIGHT");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator rightPad() {
        return simpleThreeArgumentFunction(RightPad, "RPAD");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator rightTrim() {
        return simpleFunction(RightTrim, "RTRIM");
    }

    /**
     * INTERNAL:
     * Build rightTrim operator that takes one parameter.
     */
    public static ExpressionOperator rightTrim2() {
        // bug 2916893 rightTrim(substring) broken
        ExpressionOperator operator = simpleTwoArgumentFunction(RightTrim2, "RTRIM");
        return operator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator round() {
        return simpleTwoArgumentFunction(Round, "ROUND");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator roundDate() {
        return simpleTwoArgumentFunction(RoundDate, "ROUND");
    }

    /**
     * ADVANCED: Set the array of indexes to use when building the SQL function.
     *
     * The index of the array is the position in the printout, from left to right, starting with zero.
     * The value of the array entry is the number of the argument to print at that particular output position.
     * So each argument can be used zero, one or many times.
     */
    public void setArgumentIndices(int[] indices) {
        argumentIndices = indices;
    }

    /**
     * Return the argumentIndices if set, otherwise initialize argumentIndices to the provided size
     */
    public int[] getArgumentIndices(int size) {
        int[] indices = this.argumentIndices;
        if (indices != null) {
            return indices;
        }

        indices = new int[size];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        this.argumentIndices = indices;
        return indices;
    }

    /**
     * ADVANCED:
     * Set the node class for this operator. For user-defined functions this is
     * set automatically but can be changed.
     * <p>A list of Operator types, an example, and the node class used follows.
     * <p>LogicalOperator     AND           LogicalExpression
     * <p>ComparisonOperator  {@literal <>} RelationExpression
     * <p>AggregateOperator   COUNT         FunctionExpression
     * <p>OrderOperator       ASCENDING             "
     * <p>FunctionOperator    RTRIM                 "
     * <p>Node classes given belong to org.eclipse.persistence.internal.expressions.
     */
    public void setNodeClass(Class<?> nodeClass) {
        this.nodeClass = nodeClass;
    }

    /**
     * INTERNAL:
     * Set the selector id.
     */
    public void setSelector(int selector) {
        this.selector = selector;
    }

    /**
     * ADVANCED:
     * Set the type of function.
     * This must be one of the static function types defined in this class.
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator sign() {
        return simpleFunction(Sign, "SIGN");
    }

    /**
     * INTERNAL:
     * Create an operator for a simple aggregate given a Java name and a single
     * String for the database (parentheses will be added automatically).
     */
    public static ExpressionOperator simpleAggregate(int selector, String databaseName, String javaName) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(AggregateOperator);
        exOperator.setSelector(selector);
        List<String> v = new ArrayList<>(2);
        v.add(databaseName + "(");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.printsJavaAs("." + javaName + "()");
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create an operator for a simple function given a Java name and a single
     * String for the database (parentheses will be added automatically).
     */
    public static ExpressionOperator simpleFunction(int selector, String databaseName) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(selector);
        exOperator.setName(databaseName);
        List<String> v = new ArrayList<>(2);
        v.add(databaseName + "(");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create an operator for a simple function call without parentheses
     */
    public static ExpressionOperator simpleFunctionNoParentheses(int selector, String databaseName) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(selector);
        List<String> v = new ArrayList<>(1);
        v.add(databaseName);
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }


    /**
     * INTERNAL:
     * Create an operator for a simple function given a Java name and a single
     * String for the database (parentheses will be added automatically).
     */
    public static ExpressionOperator simpleFunction(int selector, String databaseName, String javaName) {
        ExpressionOperator exOperator = simpleFunction(selector, databaseName);
        exOperator.printsJavaAs("." + javaName + "()");
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create an operator for a simple logical given a Java name and a single
     * String for the database (parentheses will be added automatically).
     */
    public static ExpressionOperator simpleLogical(int selector, String databaseName, String javaName) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(LogicalOperator);
        exOperator.setSelector(selector);
        exOperator.printsAs(" " + databaseName + " ");
        exOperator.bePostfix();
        List<String> v = new ArrayList<>(2);
        v.add("." + javaName + "(");
        v.add(")");
        exOperator.printsJavaAs(v);
        exOperator.setNodeClass(ClassConstants.LogicalExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create an operator for a simple math operation, i.e. +, -, *, /
     */
    public static ExpressionOperator simpleMath(int selector, String databaseName) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(selector);
        List<String> v = new ArrayList<>(3);
        v.add("(");
        v.add(" " + databaseName + " ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        exOperator.setIsBindingSupported(false);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create an operator for a simple ordering given a Java name and a single
     * String for the database (parentheses will be added automatically).
     */
    public static ExpressionOperator simpleOrdering(int selector, String databaseName, String javaName) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(OrderOperator);
        exOperator.setSelector(selector);
        exOperator.printsAs(" " + databaseName);
        exOperator.bePostfix();
        exOperator.printsJavaAs("." + javaName + "()");
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create an operator for a simple relation given a Java name and a single
     * String for the database (parentheses will be added automatically).
     */
    public static ExpressionOperator simpleRelation(int selector, String databaseName) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(ComparisonOperator);
        exOperator.setSelector(selector);
        exOperator.printsAs(" " + databaseName + " ");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.RelationExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create an operator for a simple relation given a Java name and a single
     * String for the database (parentheses will be added automatically).
     */
    public static ExpressionOperator simpleRelation(int selector, String databaseName, String javaName) {
        ExpressionOperator exOperator = simpleRelation(selector, databaseName);
        List<String> v = new ArrayList<>(2);
        v.add("." + javaName + "(");
        v.add(")");
        exOperator.printsJavaAs(v);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator simpleThreeArgumentFunction(int selector, String dbString) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(selector);
        exOperator.setName(dbString);
        List<String> v = new ArrayList<>(4);
        v.add(dbString + "(");
        v.add(", ");
        v.add(", ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator simpleTwoArgumentFunction(int selector, String dbString) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(selector);
        exOperator.setName(dbString);
        List<String> v = new ArrayList<>(5);
        v.add(dbString + "(");
        v.add(", ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * e.g.: ... "Bob" CONCAT "Smith" ...
     * Parentheses will not be addded. [RMB - March 5 2000]
     */
    public static ExpressionOperator simpleLogicalNoParens(int selector, String dbString) {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(selector);
        List<String> v = new ArrayList<>(5);
        v.add("");
        v.add(" " + dbString + " ");
        v.add("");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator sin() {
        return simpleFunction(Sin, "SIN");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator sinh() {
        return simpleFunction(Sinh, "SINH");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator soundex() {
        return simpleFunction(Soundex, "SOUNDEX");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator sqrt() {
        return simpleFunction(Sqrt, "SQRT");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator standardDeviation() {
        return simpleAggregate(StandardDeviation, "STDDEV", "standardDeviation");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator substring() {
        ExpressionOperator operator = simpleThreeArgumentFunction(Substring, "SUBSTR");
        operator.setIsBindingSupported(false);
        return operator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator substringSingleArg() {
        return simpleTwoArgumentFunction(SubstringSingleArg, "SUBSTR");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator subtract() {
        return ExpressionOperator.simpleMath(ExpressionOperator.Subtract, "-");
    }

    /**
     * INTERNAL:
     * Create the SUM operator.
     */
    public static ExpressionOperator sum() {
        return simpleAggregate(Sum, "SUM", "sum");
    }


    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator tan() {
        return simpleFunction(Tan, "TAN");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator tanh() {
        return simpleFunction(Tanh, "TANH");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator toDate() {
        return simpleFunction(ToDate, "TO_DATE");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator today() {
        return currentTimeStamp();
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator currentTimeStamp() {
        return simpleFunctionNoParentheses(Today,  "CURRENT_TIMESTAMP");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator currentDate() {
        return simpleFunctionNoParentheses(CurrentDate,  "CURRENT_DATE");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator currentTime() {
        return simpleFunctionNoParentheses(CurrentTime, "CURRENT_TIME");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator localDate() {
        return simpleFunctionNoParentheses(LocalDate,  "CURRENT_DATE");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator localTime() {
        return simpleFunctionNoParentheses(LocalTime, "CURRENT_TIME");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator localDateTime() {
        return simpleFunctionNoParentheses(LocalDateTime,  "CURRENT_TIMESTAMP");
    }

    /**
     * INTERNAL:
     * Create the toLowerCase operator.
     */
    public static ExpressionOperator toLowerCase() {
        return simpleFunction(ToLowerCase, "LOWER", "toLowerCase");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator toNumber() {
        return simpleFunction(ToNumber, "TO_NUMBER");
    }

    /**
     * Print a debug representation of this operator.
     */
    @Override
    public String toString() {
        String[] dbStrings = getDatabaseStrings();
        if ((dbStrings == null) || (dbStrings.length == 0)) {
            //CR#... Print a useful name for the missing platform operator.
            return "platform operator - " + getPlatformOperatorName(this.selector);
        } else {
            return "operator " + Arrays.asList(dbStrings);
        }
    }

    /**
     * INTERNAL:
     * Create the TOUPPERCASE operator.
     */
    public static ExpressionOperator toUpperCase() {
        return simpleFunction(ToUpperCase, "UPPER", "toUpperCase");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator translate() {
        ExpressionOperator operator = simpleThreeArgumentFunction(Translate, "TRANSLATE");
        operator.setIsBindingSupported(false);
        return operator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator trim() {
        return simpleFunction(Trim, "TRIM");
    }

    /**
     * INTERNAL:
     * Build Trim operator.
     */
    public static ExpressionOperator trim2() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Trim2);
        List<String> v = new ArrayList<>(5);
        v.add("TRIM(");
        v.add(" FROM ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        // Bug 573094
        int[] indices = { 1, 0 };
        exOperator.setArgumentIndices(indices);
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        exOperator.setIsBindingSupported(false);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator trunc() {
        return simpleTwoArgumentFunction(Trunc, "TRUNC");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator truncateDate() {
        return simpleTwoArgumentFunction(TruncateDate, "TRUNC");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator cast() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Cast);
        exOperator.setName("CAST");
        List<String> v = new ArrayList<>(5);
        v.add("CAST(");
        v.add(" AS ");
        v.add(")");
        exOperator.printsAs(v);
        exOperator.bePrefix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator extract() {
        return new ExtractOperator();
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator value() {
        return simpleFunction(Value, "VALUE");
    }

    /**
     * INTERNAL:
     * Build operator.
     */
    public static ExpressionOperator variance() {
        return simpleAggregate(Variance, "VARIANCE", "variance");
    }

    /**
     * INTERNAL:
     * Create the ANY operator.
     */
    public static ExpressionOperator any() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Any);
        exOperator.printsAs("ANY");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the SOME operator.
     */
    public static ExpressionOperator some() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Some);
        exOperator.printsAs("SOME");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the ALL operator.
     */
    public static ExpressionOperator all() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(All);
        exOperator.printsAs("ALL");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the UNION operator.
     */
    public static ExpressionOperator union() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Union);
        exOperator.printsAs("UNION ");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the UNION ALL operator.
     */
    public static ExpressionOperator unionAll() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(UnionAll);
        exOperator.printsAs("UNION ALL ");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the INTERSECT operator.
     */
    public static ExpressionOperator intersect() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Intersect);
        exOperator.printsAs("INTERSECT ");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the INTERSECT ALL operator.
     */
    public static ExpressionOperator intersectAll() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(IntersectAll);
        exOperator.printsAs("INTERSECT ALL ");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the EXCEPT operator.
     */
    public static ExpressionOperator except() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(Except);
        exOperator.printsAs("EXCEPT ");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Create the EXCEPT ALL operator.
     */
    public static ExpressionOperator exceptAll() {
        ExpressionOperator exOperator = new ExpressionOperator();
        exOperator.setType(FunctionOperator);
        exOperator.setSelector(ExceptAll);
        exOperator.printsAs("EXCEPT ALL ");
        exOperator.bePostfix();
        exOperator.setNodeClass(ClassConstants.FunctionExpression_Class);
        return exOperator;
    }

    /**
     * INTERNAL:
     * Indicates whether operator has selector Any or Some
     */
    public boolean isAny() {
        return  selector == ExpressionOperator.Any ||
                selector == ExpressionOperator.Some;
    }
    /**
     * INTERNAL:
     * Indicates whether operator has selector All
     */
    public boolean isAll() {
        return  selector == ExpressionOperator.All;
    }
    /**
     * INTERNAL:
     * Indicates whether operator has selector Any, Some or All
     */
    public boolean isAnyOrAll() {
        return  isAny() || isAll();
    }
}
