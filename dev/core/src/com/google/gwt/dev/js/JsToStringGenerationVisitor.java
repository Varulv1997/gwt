/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.impl.NamedRange;
import com.google.gwt.core.ext.linker.impl.StandardStatementRanges;
import com.google.gwt.dev.js.ast.HasName;
import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsCatch;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsContinue;
import com.google.gwt.dev.js.ast.JsDebugger;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsNumericEntry;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsOperator;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsPositionMarker;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.util.tools.shared.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Produces text output from a JavaScript AST.
 */
public class JsToStringGenerationVisitor extends JsVisitor {

  private static final char[] CHARS_BREAK = "break".toCharArray();
  private static final char[] CHARS_CASE = "case".toCharArray();
  private static final char[] CHARS_CATCH = "catch".toCharArray();
  private static final char[] CHARS_CONTINUE = "continue".toCharArray();
  private static final char[] CHARS_DEBUGGER = "debugger".toCharArray();
  private static final char[] CHARS_DEFAULT = "default".toCharArray();
  private static final char[] CHARS_DO = "do".toCharArray();
  private static final char[] CHARS_ELSE = "else".toCharArray();
  private static final char[] CHARS_FALSE = "false".toCharArray();
  private static final char[] CHARS_FINALLY = "finally".toCharArray();
  private static final char[] CHARS_FOR = "for".toCharArray();
  private static final char[] CHARS_FUNCTION = "function".toCharArray();
  private static final char[] CHARS_IF = "if".toCharArray();
  private static final char[] CHARS_IN = "in".toCharArray();
  private static final char[] CHARS_NEW = "new".toCharArray();
  private static final char[] CHARS_NULL = "null".toCharArray();
  private static final char[] CHARS_RETURN = "return".toCharArray();
  private static final char[] CHARS_SWITCH = "switch".toCharArray();
  private static final char[] CHARS_THIS = "this".toCharArray();
  private static final char[] CHARS_THROW = "throw".toCharArray();
  private static final char[] CHARS_TRUE = "true".toCharArray();
  private static final char[] CHARS_TRY = "try".toCharArray();
  private static final char[] CHARS_VAR = "var".toCharArray();
  private static final char[] CHARS_WHILE = "while".toCharArray();
  /**
   * How many lines of code to print inside of a JsBlock when printing terse.
   */
  private static final int JSBLOCK_LINES_TO_PRINT = 3;

  protected boolean needSemi = true;
  private List<NamedRange> classRanges = new ArrayList<NamedRange>();
  private NamedRange currentClassRange;
  private NamedRange programClassRange;

  /**
   * "Global" blocks are either the global block of a fragment, or a block
   * nested directly within some other global block. This definition matters
   * because the statements designated by statementEnds and statementStarts are
   * those that appear directly within these global blocks.
   */
  private Set<JsBlock> globalBlocks = new HashSet<JsBlock>();
  private final TextOutput p;
  private ArrayList<Integer> statementEnds = new ArrayList<Integer>();
  private ArrayList<Integer> statementStarts = new ArrayList<Integer>();
  private final boolean useLongIdents;

  /**
   * Generate the output string using short identifiers.
   */
  public JsToStringGenerationVisitor(TextOutput out) {
    this(out, false);
  }

  /**
   * Generate the output string using short or long identifiers.
   *
   * @param useLongIdents if true, emit all identifiers in long form
   */
  JsToStringGenerationVisitor(TextOutput out, boolean useLongIdents) {
    this.p = out;
    this.useLongIdents = useLongIdents;
  }

  public List<NamedRange> getClassRanges() {
    return classRanges;
  }

  /**
   * Returns a NamedRange pointing at the starting position of the first class in the program and
   * the ending position of the last class in the program. Any bytes before or after this range are
   * considered preamble and epilogue respectively.
   */
  public NamedRange getProgramClassRange() {
    return programClassRange;
  }

  public StatementRanges getStatementRanges() {
    return new StandardStatementRanges(statementStarts, statementEnds);
  }

  @Override
  public boolean visit(JsArrayAccess x, JsContext ctx) {
    JsExpression arrayExpr = x.getArrayExpr();
    _parenPush(x, arrayExpr, false);
    accept(arrayExpr);
    _parenPop(x, arrayExpr, false);
    _lsquare();
    accept(x.getIndexExpr());
    _rsquare();
    return false;
  }

  @Override
  public boolean visit(JsArrayLiteral x, JsContext ctx) {
    _lsquare();
    boolean sep = false;
    for (Object element : x.getExpressions()) {
      JsExpression arg = (JsExpression) element;
      sep = _sepCommaOptSpace(sep);
      _parenPushIfCommaExpr(arg);
      accept(arg);
      _parenPopIfCommaExpr(arg);
    }
    _rsquare();
    return false;
  }

  @Override
  public boolean visit(JsBinaryOperation x, JsContext ctx) {
    JsBinaryOperator op = x.getOperator();
    JsExpression arg1 = x.getArg1();
    _parenPush(x, arg1, !op.isLeftAssociative());
    accept(arg1);
    if (op.isKeyword()) {
      _parenPopOrSpace(x, arg1, !op.isLeftAssociative());
    } else {
      _parenPop(x, arg1, !op.isLeftAssociative());
      _spaceOpt();
    }
    p.print(op.getSymbol());
    JsExpression arg2 = x.getArg2();
    if (_spaceCalc(op, arg2)) {
      _parenPushOrSpace(x, arg2, op.isLeftAssociative());
    } else {
      _spaceOpt();
      _parenPush(x, arg2, op.isLeftAssociative());
    }
    accept(arg2);
    _parenPop(x, arg2, op.isLeftAssociative());
    return false;
  }

  @Override
  public boolean visit(JsBlock x, JsContext ctx) {
    printJsBlock(x, true, true);
    return false;
  }

  @Override
  public boolean visit(JsBooleanLiteral x, JsContext ctx) {
    if (x.getValue()) {
      _true();
    } else {
      _false();
    }
    return false;
  }

  @Override
  public boolean visit(JsBreak x, JsContext ctx) {
    _break();

    JsNameRef label = x.getLabel();
    if (label != null) {
      _space();
      _nameRef(label);
    }

    return false;
  }

  @Override
  public boolean visit(JsCase x, JsContext ctx) {
    _case();
    _space();
    accept(x.getCaseExpr());
    _colon();
    _newlineOpt();

    indent();
    for (Object element : x.getStmts()) {
      JsStatement stmt = (JsStatement) element;
      needSemi = true;
      accept(stmt);
      if (needSemi) {
        _semi();
      }
      _newlineOpt();
    }
    outdent();
    needSemi = false;
    return false;
  }

  @Override
  public boolean visit(JsCatch x, JsContext ctx) {
    _spaceOpt();
    _catch();
    _spaceOpt();
    _lparen();
    _nameDef(x.getParameter().getName());

    // Optional catch condition.
    //
    JsExpression catchCond = x.getCondition();
    if (catchCond != null) {
      _space();
      _if();
      _space();
      accept(catchCond);
    }

    _rparen();
    _spaceOpt();
    accept(x.getBody());

    return false;
  }

  @Override
  public boolean visit(JsPositionMarker x, JsContext ctx) {
    needSemi = false;

    switch (x.getType()) {
      case CLASS_START:
        assert currentClassRange
            == null : "Class start and end boundaries must be matched and not nested.";
        currentClassRange = new NamedRange(x.getName());
        currentClassRange.setStartPosition(p.getPosition());
        currentClassRange.setStartLineNumber(p.getLine());
        break;
      case CLASS_END:
        assert currentClassRange
            != null : "Class start and end boundaries must be matched and not nested.";
        currentClassRange.setEndPosition(p.getPosition());
        currentClassRange.setEndLineNumber(p.getLine());
        classRanges.add(currentClassRange);
        currentClassRange = null;
        break;
      case PROGRAM_START:
        programClassRange = new NamedRange("Program");
        programClassRange.setStartPosition(p.getPosition());
        programClassRange.setStartLineNumber(p.getLine());
        break;
      case PROGRAM_END:
        assert programClassRange != null : "Program start and end boundaries must be matched.";
        programClassRange.setEndPosition(p.getPosition());
        programClassRange.setEndLineNumber(p.getLine());
        break;
      default:
        assert false : x.getType() + " position type is not recognized.";
    }

    return super.visit(x, ctx);
  }

  @Override
  public boolean visit(JsConditional x, JsContext ctx) {
    // Associativity: for the then and else branches, it is safe to insert
    // another
    // ternary expression, but if the test expression is a ternary, it should
    // get parentheses around it.
    {
      JsExpression testExpression = x.getTestExpression();
      _parenPush(x, testExpression, true);
      accept(testExpression);
      _parenPop(x, testExpression, true);
    }
    _questionMark();
    {
      JsExpression thenExpression = x.getThenExpression();
      _parenPush(x, thenExpression, false);
      accept(thenExpression);
      _parenPop(x, thenExpression, false);
    }
    _colon();
    {
      JsExpression elseExpression = x.getElseExpression();
      _parenPush(x, elseExpression, false);
      accept(elseExpression);
      _parenPop(x, elseExpression, false);
    }
    return false;
  }

  @Override
  public boolean visit(JsContinue x, JsContext ctx) {
    _continue();

    JsNameRef label = x.getLabel();
    if (label != null) {
      _space();
      _nameRef(label);
    }

    return false;
  }

  @Override
  public boolean visit(JsDebugger x, JsContext ctx) {
    _debugger();
    return false;
  }

  @Override
  public boolean visit(JsDefault x, JsContext ctx) {
    _default();
    _colon();

    indent();
    for (Object element : x.getStmts()) {
      JsStatement stmt = (JsStatement) element;
      needSemi = true;
      accept(stmt);
      if (needSemi) {
        _semi();
      }
      _newlineOpt();
    }
    outdent();
    needSemi = false;
    return false;
  }

  @Override
  public boolean visit(JsDoWhile x, JsContext ctx) {
    _do();
    _nestedPush(x.getBody(), true);
    accept(x.getBody());
    _nestedPop(x.getBody());
    if (needSemi) {
      _semi();
      _newlineOpt();
    } else {
      _spaceOpt();
      needSemi = true;
    }
    _while();
    _spaceOpt();
    _lparen();
    accept(x.getCondition());
    _rparen();
    return false;
  }

  @Override
  public boolean visit(JsEmpty x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsExprStmt x, JsContext ctx) {
    boolean surroundWithParentheses = JsFirstExpressionVisitor.exec(x);
    if (surroundWithParentheses) {
      _lparen();
    }
    JsExpression expr = x.getExpression();
    accept(expr);
    if (surroundWithParentheses) {
      _rparen();
    }
    return false;
  }

  @Override
  public boolean visit(JsFor x, JsContext ctx) {
    _for();
    _spaceOpt();
    _lparen();

    // The init expressions or var decl.
    //
    if (x.getInitExpr() != null) {
      accept(x.getInitExpr());
    } else if (x.getInitVars() != null) {
      accept(x.getInitVars());
    }

    _semi();

    // The loop test.
    //
    if (x.getCondition() != null) {
      _spaceOpt();
      accept(x.getCondition());
    }

    _semi();

    // The incr expression.
    //
    if (x.getIncrExpr() != null) {
      _spaceOpt();
      accept(x.getIncrExpr());
    }

    _rparen();
    _nestedPush(x.getBody(), false);
    accept(x.getBody());
    _nestedPop(x.getBody());
    return false;
  }

  @Override
  public boolean visit(JsForIn x, JsContext ctx) {
    _for();
    _spaceOpt();
    _lparen();

    if (x.getIterVarName() != null) {
      _var();
      _space();
      _nameDef(x.getIterVarName());

      if (x.getIterExpr() != null) {
        _spaceOpt();
        _assignment();
        _spaceOpt();
        accept(x.getIterExpr());
      }
    } else {
      // Just a name ref.
      //
      accept(x.getIterExpr());
    }

    _space();
    _in();
    _space();
    accept(x.getObjExpr());

    _rparen();
    _nestedPush(x.getBody(), false);
    accept(x.getBody());
    _nestedPop(x.getBody());
    return false;
  }

  // function foo(a, b) {
  // stmts...
  // }
  //
  @Override
  public boolean visit(JsFunction x, JsContext ctx) {
    _function();

    // Functions can be anonymous.
    //
    if (x.getName() != null) {
      _space();
      _nameOf(x);
    }

    _lparen();
    boolean sep = false;
    for (Object element : x.getParameters()) {
      JsParameter param = (JsParameter) element;
      sep = _sepCommaOptSpace(sep);
      accept(param);
    }
    _rparen();

    accept(x.getBody());
    needSemi = true;
    return false;
  }

  @Override
  public boolean visit(JsIf x, JsContext ctx) {
    _if();
    _spaceOpt();
    _lparen();
    accept(x.getIfExpr());
    _rparen();
    JsStatement thenStmt = x.getThenStmt();
    _nestedPush(thenStmt, false);
    accept(thenStmt);
    _nestedPop(thenStmt);
    JsStatement elseStmt = x.getElseStmt();
    if (elseStmt != null) {
      if (needSemi) {
        _semi();
        _newlineOpt();
      } else {
        _spaceOpt();
        needSemi = true;
      }
      _else();
      boolean elseIf = elseStmt instanceof JsIf;
      if (!elseIf) {
        _nestedPush(elseStmt, true);
      } else {
        _space();
      }
      accept(elseStmt);
      if (!elseIf) {
        _nestedPop(elseStmt);
      }
    }
    return false;
  }

  @Override
  public boolean visit(JsInvocation x, JsContext ctx) {
    JsExpression qualifier = x.getQualifier();
    _parenPush(x, qualifier, false);
    accept(qualifier);
    _parenPop(x, qualifier, false);

    _lparen();
    boolean sep = false;
    for (Object element : x.getArguments()) {
      JsExpression arg = (JsExpression) element;
      sep = _sepCommaOptSpace(sep);
      _parenPushIfCommaExpr(arg);
      accept(arg);
      _parenPopIfCommaExpr(arg);
    }
    _rparen();
    return false;
  }

  @Override
  public boolean visit(JsLabel x, JsContext ctx) {
    _nameOf(x);
    _colon();
    _spaceOpt();
    accept(x.getStmt());
    return false;
  }

  @Override
  public boolean visit(JsNameOf x, JsContext ctx) {
    if (useLongIdents) {
      printStringLiteral(x.getName().getIdent());
    } else {
      printStringLiteral(x.getName().getShortIdent());
    }

    return false;
  }

  @Override
  public boolean visit(JsNameRef x, JsContext ctx) {
    JsExpression q = x.getQualifier();
    if (q != null) {
      _parenPush(x, q, false);
      accept(q);
      if (q instanceof JsNumberLiteral) {
        /**
         * Fix for Issue #3796. "42.foo" is not allowed, but "42 .foo" is.
         */
        _space();
      }
      _parenPop(x, q, false);
      _dot();
    }
    _nameRef(x);
    return false;
  }

  @Override
  public boolean visit(JsNew x, JsContext ctx) {
    _new();
    _space();

    JsExpression ctorExpr = x.getConstructorExpression();
    boolean needsParens = JsConstructExpressionVisitor.exec(ctorExpr);
    if (needsParens) {
      _lparen();
    }
    accept(ctorExpr);
    if (needsParens) {
      _rparen();
    }

    /*
     * If a constructor call has no arguments, it may simply be replaced with
     * "new Constructor" with no parentheses.
     */
    List<JsExpression> args = x.getArguments();
    if (args.size() > 0) {
      _lparen();
      boolean sep = false;
      for (JsExpression arg : args) {
        sep = _sepCommaOptSpace(sep);
        _parenPushIfCommaExpr(arg);
        accept(arg);
        _parenPopIfCommaExpr(arg);
      }
      _rparen();
    }

    return false;
  }

  @Override
  public boolean visit(JsNullLiteral x, JsContext ctx) {
    _null();
    return false;
  }

  @Override
  public boolean visit(JsNumberLiteral x, JsContext ctx) {
    double dvalue = x.getValue();
    if (dvalue == 0.0 && 1.0 / dvalue == Double.NEGATIVE_INFINITY) {
      // Negative zero is distinct from 0.0 and (integer) 0
      p.print("-0.");
      return false;
    }

    long lvalue = (long) dvalue;
    if (lvalue == dvalue) {
      p.print(Long.toString(lvalue));
    } else {
      p.print(Double.toString(dvalue));
    }
    return false;
  }

  @Override
  public boolean visit(JsNumericEntry x, JsContext ctx) {
    p.print(Integer.toString(x.getValue()));
    return false;
  }

  @Override
  public boolean visit(JsObjectLiteral x, JsContext ctx) {
    _lbrace();
    boolean sep = false;
    for (JsPropertyInitializer element : x.getPropertyInitializers()) {
      sep = _sepCommaOptSpace(sep);
      accept(element.getLabelExpr());
      _colon();
      JsExpression valueExpr = element.getValueExpr();
      _parenPushIfCommaExpr(valueExpr);
      accept(valueExpr);
      _parenPopIfCommaExpr(valueExpr);
    }
    _rbrace();
    return false;
  }

  @Override
  public boolean visit(JsParameter x, JsContext ctx) {
    _nameOf(x);
    return false;
  }

  @Override
  public boolean visit(JsPostfixOperation x, JsContext ctx) {
    JsUnaryOperator op = x.getOperator();
    JsExpression arg = x.getArg();
    // unary operators always associate correctly (I think)
    _parenPush(x, arg, false);
    accept(arg);
    _parenPop(x, arg, false);
    p.print(op.getSymbol());
    return false;
  }

  @Override
  public boolean visit(JsPrefixOperation x, JsContext ctx) {
    JsUnaryOperator op = x.getOperator();
    p.print(op.getSymbol());
    JsExpression arg = x.getArg();
    if (_spaceCalc(op, arg)) {
      _space();
    }
    // unary operators always associate correctly (I think)
    _parenPush(x, arg, false);
    accept(arg);
    _parenPop(x, arg, false);
    return false;
  }

  @Override
  public boolean visit(JsProgram x, JsContext ctx) {
    p.print("<JsProgram>");
    return false;
  }

  @Override
  public boolean visit(JsProgramFragment x, JsContext ctx) {
    p.print("<JsProgramFragment>");
    return false;
  }

  @Override
  public boolean visit(JsPropertyInitializer x, JsContext ctx) {
    // Since there are separators, we actually print the property init
    // in visit(JsObjectLiteral).
    //
    return false;
  }

  @Override
  public boolean visit(JsRegExp x, JsContext ctx) {
    _slash();
    p.print(x.getPattern());
    _slash();
    String flags = x.getFlags();
    if (flags != null) {
      p.print(flags);
    }
    return false;
  }

  @Override
  public boolean visit(JsReturn x, JsContext ctx) {
    _return();
    JsExpression expr = x.getExpr();
    if (expr != null) {
      _space();
      accept(expr);
    }
    return false;
  }

  @Override
  public boolean visit(JsStringLiteral x, JsContext ctx) {
    printStringLiteral(x.getValue());
    return false;
  }

  @Override
  public boolean visit(JsSwitch x, JsContext ctx) {
    _switch();
    _spaceOpt();
    _lparen();
    accept(x.getExpr());
    _rparen();
    _spaceOpt();
    _blockOpen();
    acceptList(x.getCases());
    _blockClose();
    return false;
  }

  @Override
  public boolean visit(JsThisRef x, JsContext ctx) {
    _this();
    return false;
  }

  @Override
  public boolean visit(JsThrow x, JsContext ctx) {
    _throw();
    _space();
    accept(x.getExpr());
    return false;
  }

  @Override
  public boolean visit(JsTry x, JsContext ctx) {
    _try();
    _spaceOpt();
    accept(x.getTryBlock());

    acceptList(x.getCatches());

    JsBlock finallyBlock = x.getFinallyBlock();
    if (finallyBlock != null) {
      _spaceOpt();
      _finally();
      _spaceOpt();
      accept(finallyBlock);
    }

    return false;
  }

  @Override
  public boolean visit(JsVar x, JsContext ctx) {
    _nameOf(x);
    JsExpression initExpr = x.getInitExpr();
    if (initExpr != null) {
      _spaceOpt();
      _assignment();
      _spaceOpt();
      _parenPushIfCommaExpr(initExpr);
      accept(initExpr);
      _parenPopIfCommaExpr(initExpr);
    }
    return false;
  }

  @Override
  public boolean visit(JsVars x, JsContext ctx) {
    _var();
    _space();
    boolean sep = false;
    for (JsVar var : x) {
      sep = _sepCommaOptSpace(sep);
      accept(var);
    }
    return false;
  }

  @Override
  public boolean visit(JsWhile x, JsContext ctx) {
    _while();
    _spaceOpt();
    _lparen();
    accept(x.getCondition());
    _rparen();
    _nestedPush(x.getBody(), false);
    accept(x.getBody());
    _nestedPop(x.getBody());
    return false;
  }

//CHECKSTYLE_NAMING_OFF

  protected void _newline() {
    p.newline();
  }

  protected void _newlineOpt() {
    p.newlineOpt();
  }

  /**
   * Adds any unbilled JavaScript to the most recently finished child node (if any).
   */
  protected void billChildToHere() {
  }

  protected void printJsBlock(JsBlock x, boolean truncate, boolean finalNewline) {
    boolean needBraces = !x.isGlobalBlock();

    if (needBraces) {
      // Open braces.
      //
      _blockOpen();
    }

    int count = 0;
    for (Iterator<JsStatement> iter = x.getStatements().iterator(); iter.hasNext(); ++count) {
      boolean isGlobal = x.isGlobalBlock() || globalBlocks.contains(x);

      if (truncate && count > JSBLOCK_LINES_TO_PRINT) {
        p.print("[...]");
        _newlineOpt();
        break;
      }
      JsStatement stmt = iter.next();
      needSemi = true;
      boolean shouldRecordPositions = isGlobal && stmt.shouldRecordPosition();
      boolean stmtIsGlobalBlock = false;
      if (isGlobal) {
        if (stmt instanceof JsBlock) {
          // A block inside a global block is still considered global
          stmtIsGlobalBlock = true;
          globalBlocks.add((JsBlock) stmt);
        }
      }
      if (shouldRecordPositions) {
        statementStarts.add(p.getPosition());
      }
      accept(stmt);
      if (stmtIsGlobalBlock) {
        globalBlocks.remove(stmt);
      }
      if (needSemi) {
        /*
         * Special treatment of function decls: If they are the only item in a
         * statement (i.e. not part of an assignment operation), just give them
         * a newline instead of a semi.
         */
        boolean functionStmt = stmt instanceof JsExprStmt
            && ((JsExprStmt) stmt).getExpression() instanceof JsFunction;
        /*
         * Special treatment of the last statement in a block: only a few
         * statements at the end of a block require semicolons.
         */
        boolean lastStatement = !iter.hasNext() && needBraces
            && !JsRequiresSemiVisitor.exec(stmt);
        if (functionStmt) {
          if (lastStatement) {
            _newlineOpt();
          } else {
            _newline();
          }
        } else {
          if (lastStatement) {
            _semiOpt();
          } else {
            _semi();
          }
          _newlineOpt();
          billChildToHere();
        }
      }
      if (shouldRecordPositions) {
        assert (statementStarts.size() == statementEnds.size() + 1);
        statementEnds.add(p.getPosition());
      }
    }

    if (needBraces) {
      // _blockClose() modified
      p.indentOut();
      p.print('}');
      if (finalNewline) {
        _newlineOpt();
      }
    }
    needSemi = false;
  }

  private void _assignment() {
    p.print('=');
  }

  private void _blockClose() {
    p.indentOut();
    p.print('}');
    _newlineOpt();
  }

  private void _blockOpen() {
    p.print('{');
    p.indentIn();
    _newlineOpt();
  }

  private void _break() {
    p.print(CHARS_BREAK);
  }

  private void _case() {
    p.print(CHARS_CASE);
  }

  private void _catch() {
    p.print(CHARS_CATCH);
  }

  private void _colon() {
    p.print(':');
  }

  private void _continue() {
    p.print(CHARS_CONTINUE);
  }

  private void _debugger() {
    p.print(CHARS_DEBUGGER);
  }

  private void _default() {
    p.print(CHARS_DEFAULT);
  }

  private void _do() {
    p.print(CHARS_DO);
  }

  private void _dot() {
    p.print('.');
  }

  private void _else() {
    p.print(CHARS_ELSE);
  }

  private void _false() {
    p.print(CHARS_FALSE);
  }

  private void _finally() {
    p.print(CHARS_FINALLY);
  }

  private void _for() {
    p.print(CHARS_FOR);
  }

  private void _function() {
    p.print(CHARS_FUNCTION);
  }

  private void _if() {
    p.print(CHARS_IF);
  }

  private void _in() {
    p.print(CHARS_IN);
  }

  private void _lbrace() {
    p.print('{');
  }

  private void _lparen() {
    p.print('(');
  }

  private void _lsquare() {
    p.print('[');
  }

  private void _nameDef(JsName name) {
    if (useLongIdents) {
      p.print(name.getIdent());
    } else {
      p.print(name.getShortIdent());
    }
  }

  private void _nameOf(HasName hasName) {
    _nameDef(hasName.getName());
  }

  private void _nameRef(JsNameRef nameRef) {
    if (useLongIdents) {
      p.print(nameRef.getIdent());
    } else {
      p.print(nameRef.getShortIdent());
    }
  }

  private boolean _nestedPop(JsStatement statement) {
    boolean pop = !(statement instanceof JsBlock);
    if (pop) {
      p.indentOut();
    }
    return pop;
  }

  private boolean _nestedPush(JsStatement statement, boolean needSpace) {
    boolean push = !(statement instanceof JsBlock);
    if (push) {
      if (needSpace) {
        _space();
      }
      p.indentIn();
      _newlineOpt();
    } else {
      _spaceOpt();
    }
    return push;
  }

  private void _new() {
    p.print(CHARS_NEW);
  }

  private void _null() {
    p.print(CHARS_NULL);
  }

  private boolean _parenCalc(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    int parentPrec = JsPrecedenceVisitor.exec(parent);
    int childPrec = JsPrecedenceVisitor.exec(child);
    return (parentPrec > childPrec || (parentPrec == childPrec && wrongAssoc));
  }

  private boolean _parenPop(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPop = _parenCalc(parent, child, wrongAssoc);
    if (doPop) {
      _rparen();
    }
    return doPop;
  }

  private boolean _parenPopIfCommaExpr(JsExpression x) {
    boolean doPop = x instanceof JsBinaryOperation
        && ((JsBinaryOperation) x).getOperator() == JsBinaryOperator.COMMA;
    if (doPop) {
      _rparen();
    }
    return doPop;
  }

  private boolean _parenPopOrSpace(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPop = _parenCalc(parent, child, wrongAssoc);
    if (doPop) {
      _rparen();
    } else {
      _space();
    }
    return doPop;
  }

  private boolean _parenPush(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPush = _parenCalc(parent, child, wrongAssoc);
    if (doPush) {
      _lparen();
    }
    return doPush;
  }

  private boolean _parenPushIfCommaExpr(JsExpression x) {
    boolean doPush = x instanceof JsBinaryOperation
        && ((JsBinaryOperation) x).getOperator() == JsBinaryOperator.COMMA;
    if (doPush) {
      _lparen();
    }
    return doPush;
  }

  private boolean _parenPushOrSpace(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPush = _parenCalc(parent, child, wrongAssoc);
    if (doPush) {
      _lparen();
    } else {
      _space();
    }
    return doPush;
  }

  private void _questionMark() {
    p.print('?');
  }

  private void _rbrace() {
    p.print('}');
  }

  private void _return() {
    p.print(CHARS_RETURN);
  }

  private void _rparen() {
    p.print(')');
  }

  private void _rsquare() {
    p.print(']');
  }

  private void _semi() {
    p.print(';');
    billChildToHere();
  }

  private void _semiOpt() {
    p.printOpt(';');
    billChildToHere();
  }

  private boolean _sepCommaOptSpace(boolean sep) {
    if (sep) {
      p.print(',');
      _spaceOpt();
    }
    return true;
  }

  private void _slash() {
    p.print('/');
  }

  private void _space() {
    p.print(' ');
  }

  /**
   * Decide whether, if <code>op</code> is printed followed by <code>arg</code>,
   * there needs to be a space between the operator and expression.
   *
   * @return <code>true</code> if a space needs to be printed
   */
  private boolean _spaceCalc(JsOperator op, JsExpression arg) {
    if (op.isKeyword()) {
      return true;
    }
    if (arg instanceof JsBinaryOperation) {
      JsBinaryOperation binary = (JsBinaryOperation) arg;
      /*
       * If the binary operation has a higher precedence than op, then it won't
       * be parenthesized, so check the first argument of the binary operation.
       */
      if (binary.getOperator().getPrecedence() > op.getPrecedence()) {
        return _spaceCalc(op, binary.getArg1());
      }
      return false;
    }
    if (arg instanceof JsPrefixOperation) {
      JsOperator op2 = ((JsPrefixOperation) arg).getOperator();
      return (op == JsBinaryOperator.SUB || op == JsUnaryOperator.NEG)
          && (op2 == JsUnaryOperator.DEC || op2 == JsUnaryOperator.NEG)
          || (op == JsBinaryOperator.ADD || op == JsUnaryOperator.POS)
          && (op2 == JsUnaryOperator.INC || op2 == JsUnaryOperator.POS);
    }
    if (arg instanceof JsNumberLiteral) {
      JsNumberLiteral literal = (JsNumberLiteral) arg;
      return (op == JsBinaryOperator.SUB || op == JsUnaryOperator.NEG)
          && (literal.getValue() < 0);
    }
    return false;
  }

  private void _spaceOpt() {
    p.printOpt(' ');
  }

  private void _switch() {
    p.print(CHARS_SWITCH);
  }

  private void _this() {
    p.print(CHARS_THIS);
  }

  private void _throw() {
    p.print(CHARS_THROW);
  }

  private void _true() {
    p.print(CHARS_TRUE);
  }

  private void _try() {
    p.print(CHARS_TRY);
  }

  private void _var() {
    p.print(CHARS_VAR);
  }

  private void _while() {
    p.print(CHARS_WHILE);
  }

// CHECKSTYLE_NAMING_ON

  private void indent() {
    p.indentIn();
  }

  private void outdent() {
    p.indentOut();
  }

  private void printStringLiteral(String value) {
    String resultString = StringUtils.javaScriptString(value);
    p.print(resultString);
  }
}
