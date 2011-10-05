// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Simple expression grammar:
 *
 * comp := expr [ COMPARISONOP expr ]*
 * expr := term [ ADDOP term ]*
 * term := comp [ MULOP comp]*
 * factor := (comp) | - factor | NUMBER | BOOLEAN | STRING | CELLREF | CELLREF : CELLREF | function
 * function := FUNCNAME ( [comp [, comp]*] )
 *
 * BOOLEAN = "TRUE" | "FALSE"
 *
 * The syntax for ops, numbers, cellref, and funcname are defined by the
 * scanner.
 *
 * The operator precendence from high to low is:
 *     *,/
 *     +,-
 *     <,<=,>,>=,=,<>
 *
 * All operators are left-associative (a OP b OP c == (a OP b) OP c).
 */
class Parser {
  Token _lookahead;
  Scanner _scanner;

  Parser(this._scanner) { }

  List<Token> parse() {
    List<Token> output = new List<Token>();
    _advance();
    _comp(output);
    if (_lookahead != null) {
      throw new RuntimeException("parsing completed, unused token '${_lookahead.toUserString()}'");
    }
    return output;
  }

  void _advance() {
    _lookahead = _scanner.nextToken();
  }

  void _comp(List<Token> output) {
    _expr(output);
    while (_lookahead != null && _lookahead.isComparisonOp()) {
      Token t = _lookahead;
      _advance();
      _expr(output);
      _emit(t, output);
    }
  }

  void _emit(Token t, List<Token> output) {
    output.add(t);
  }

  void _error(String message) {
    throw new RuntimeException(message);
  }

  void _expr(List<Token> output) {
    _term(output);
    while (_lookahead != null && _lookahead.isAddOp()) {
      Token t = _lookahead;
      _advance();
      _term(output);
      _emit(t, output);
    }
  }

  void _factor(List<Token> output) {
    if (_lookahead != null && _lookahead.isLParen()) {
      _advance();
      _comp(output);
      if (_lookahead != null && _lookahead.isRParen()) {
        _advance();
      } else {
        _error("Expected ')'");
      }
    } else if (_lookahead != null && _lookahead is OperatorToken && _lookahead.isMinus()) {
      _advance();
      // emit "0 <factor> -"
      _emit(new NumberToken(0.0), output);
      _factor(output);
      _emit(new OperatorToken(OperatorToken.OP_MINUS), output);
    } else if (_lookahead is NumberToken) {
      _emit(_lookahead, output);
      _advance();
    } else if (_lookahead is StringToken) {
      _emit(_lookahead, output);
      _advance();
    } else if (_lookahead is CellRefToken) {
      CellRefToken ref1 = _lookahead;
      CellRefToken ref2 = null;
      _advance();
      if (_lookahead != null && _lookahead is OperatorToken && _lookahead.isColon()) {
        _advance();
        if (_lookahead is CellRefToken) {
          ref2 = _lookahead;
          _advance();
        } else {
          _error("Expected CellRef after 'CellRef:'");
        }
      }
      if (ref2 != null) {
        _emit(new RangeToken(ref1, ref2), output);
      } else {
        _emit(ref1, output);
      }
    } else if (_lookahead is FunctionNameToken) {
      _func(output);
    } else {
      _error("Expected '(', or a number, cell reference, or function name");
    }
  }

  void _func(List<Token> output) {
    int nargs = 0;
    Token func = _lookahead;
    _advance();
    if (_lookahead != null && _lookahead.isLParen()) {
      _advance();
      if (_lookahead != null && !_lookahead.isRParen()) {
        _comp(output);
        nargs++;
      }
      while (_lookahead != null && _lookahead.isComma()) {
        _advance();
        _comp(output);
        nargs++;
      }
      if (_lookahead != null && _lookahead.isRParen()) {
        _advance();
        // Emit the number of arguments encountered
        _emit(new NumberToken(nargs.toDouble()), output);
        _emit(func, output);
      } else {
        _error("Expected ')'");
      }
    } else {
      _error("Expected '('");
    }
  }

  void _term(List<Token> output) {
    _factor(output);
    while (_lookahead != null && _lookahead.isMulOp()) {
      Token t = _lookahead;
      _advance();
      _factor(output);
      _emit(t, output);
    }
  }
}
