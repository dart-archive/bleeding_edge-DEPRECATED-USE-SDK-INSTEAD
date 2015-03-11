// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dart2js.operators;

enum UnaryOperatorKind {
  NOT,
  NEGATE,
  COMPLEMENT,
}

class UnaryOperator {
  final UnaryOperatorKind kind;
  final String name;
  final String selectorName;

  const UnaryOperator(this.kind, this.name, this.selectorName);

  bool get isUserDefinable => selectorName != null;

  String toString() => name;

  /// The unary ! operator.
  static const UnaryOperator NOT =
      const UnaryOperator(UnaryOperatorKind.NOT, '!', null);

  /// The unary - operator.
  static const UnaryOperator NEGATE =
      const UnaryOperator(UnaryOperatorKind.NEGATE, '-', 'unary-');

  /// The unary ~ operator.
  static const UnaryOperator COMPLEMENT =
      const UnaryOperator(UnaryOperatorKind.COMPLEMENT, '~', '~');

  static UnaryOperator parse(String value) {
    switch (value) {
      case '!': return NOT;
      case '-': return NEGATE;
      case '~': return COMPLEMENT;
      default: return null;
    }
  }
}


enum BinaryOperatorKind {
  EQ,
  NOT_EQ,
  INDEX,
  ADD,
  SUB,
  MUL,
  DIV,
  IDIV,
  MOD,
  SHL,
  SHR,
  GTEQ,
  GT,
  LTEQ,
  LT,
  AND,
  OR,
  XOR,
}

class BinaryOperator {
  final BinaryOperatorKind kind;
  final String name;

  const BinaryOperator._(this.kind, this.name);

  bool get isUserDefinable => true;
  String get selectorName => name;

  String toString() => name;

  /// The == operator.
  static const BinaryOperator EQ =
      const BinaryOperator._(BinaryOperatorKind.EQ, '==');

  /// The != operator.
  static const BinaryOperator NOT_EQ = const _NotEqualsOperator();

  /// The [] operator.
  static const BinaryOperator INDEX =
      const BinaryOperator._(BinaryOperatorKind.INDEX, '[]');

  /// The binary + operator.
  static const BinaryOperator ADD =
      const BinaryOperator._(BinaryOperatorKind.ADD, '+');

  /// The binary - operator.
  static const BinaryOperator SUB =
      const BinaryOperator._(BinaryOperatorKind.SUB, '-');

  /// The binary * operator.
  static const BinaryOperator MUL =
      const BinaryOperator._(BinaryOperatorKind.MUL, '*');

  /// The binary / operator.
  static const BinaryOperator DIV =
      const BinaryOperator._(BinaryOperatorKind.DIV, '/');

  /// The binary ~/ operator.
  static const BinaryOperator IDIV =
      const BinaryOperator._(BinaryOperatorKind.IDIV, '~/');

  /// The binary % operator.
  static const BinaryOperator MOD =
      const BinaryOperator._(BinaryOperatorKind.MOD, '%');

  /// The binary << operator.
  static const BinaryOperator SHL =
      const BinaryOperator._(BinaryOperatorKind.SHL, '<<');

  /// The binary >> operator.
  static const BinaryOperator SHR =
      const BinaryOperator._(BinaryOperatorKind.SHR, '>>');

  /// The binary >= operator.
  static const BinaryOperator GTEQ =
      const BinaryOperator._(BinaryOperatorKind.GTEQ, '>=');

  /// The binary > operator.
  static const BinaryOperator GT =
      const BinaryOperator._(BinaryOperatorKind.GT, '>');

  /// The binary <= operator.
  static const BinaryOperator LTEQ =
      const BinaryOperator._(BinaryOperatorKind.LTEQ, '<=');

  /// The binary < operator.
  static const BinaryOperator LT =
      const BinaryOperator._(BinaryOperatorKind.LT, '<');

  /// The binary & operator.
  static const BinaryOperator AND =
      const BinaryOperator._(BinaryOperatorKind.AND, '&');

  /// The binary | operator.
  static const BinaryOperator OR =
      const BinaryOperator._(BinaryOperatorKind.OR, '|');

  /// The binary ^ operator.
  static const BinaryOperator XOR =
      const BinaryOperator._(BinaryOperatorKind.XOR, '^');

  static BinaryOperator parse(String value) {
    switch (value) {
      case '==': return EQ;
      case '!=': return NOT_EQ;
      case '[]': return INDEX;
      case '*': return MUL;
      case '/': return DIV;
      case '%': return MOD;
      case '~/': return IDIV;
      case '+': return ADD;
      case '-': return SUB;
      case '<<': return SHL;
      case '>>': return SHR;
      case '>=': return GTEQ;
      case '>': return GT;
      case '<=': return LTEQ;
      case '<': return LT;
      case '&': return AND;
      case '^': return XOR;
      case '|': return OR;
      default: return null;
    }
  }
}

/// The operator !=, which is not user definable operator but instead is a negation
class _NotEqualsOperator extends BinaryOperator {
  const _NotEqualsOperator() : super._(BinaryOperatorKind.NOT_EQ, '!=');

  bool get isUserDefinable => false;

  String get selectorName => '==';
}

enum AssignmentOperatorKind {
  ASSIGN,
  ADD,
  SUB,
  MUL,
  DIV,
  IDIV,
  MOD,
  SHL,
  SHR,
  AND,
  OR,
  XOR,
}

class AssignmentOperator {
  final AssignmentOperatorKind kind;
  final BinaryOperator binaryOperator;
  final String name;
  final bool isUserDefinable;

  const AssignmentOperator._(this.kind, this.name, this.binaryOperator,
                             {this.isUserDefinable: true});

  String toString() => name;

  /// The = operator.
  static const AssignmentOperator ASSIGN =
      const AssignmentOperator._(AssignmentOperatorKind.ASSIGN, '=',
                                 null, isUserDefinable: false);

  /// The += assignment operator.
  static const AssignmentOperator ADD =
      const AssignmentOperator._(AssignmentOperatorKind.ADD, '+=',
                                 BinaryOperator.ADD);

  /// The -= assignment operator.
  static const AssignmentOperator SUB =
      const AssignmentOperator._(AssignmentOperatorKind.SUB, '-=',
                                 BinaryOperator.SUB);

  /// The *= assignment operator.
  static const AssignmentOperator MUL =
      const AssignmentOperator._(AssignmentOperatorKind.MUL, '*=',
                                 BinaryOperator.MUL);

  /// The /= assignment operator.
  static const AssignmentOperator DIV =
      const AssignmentOperator._(AssignmentOperatorKind.DIV, '/=',
                                 BinaryOperator.DIV);

  /// The ~/= assignment operator.
  static const AssignmentOperator IDIV =
      const AssignmentOperator._(AssignmentOperatorKind.IDIV, '~/=',
                                 BinaryOperator.IDIV);

  /// The %= assignment operator.
  static const AssignmentOperator MOD =
      const AssignmentOperator._(AssignmentOperatorKind.MOD, '%=',
                                 BinaryOperator.MOD);

  /// The <<= assignment operator.
  static const AssignmentOperator SHL =
      const AssignmentOperator._(AssignmentOperatorKind.SHL, '<<=',
                                 BinaryOperator.SHL);

  /// The >>= assignment operator.
  static const AssignmentOperator SHR =
      const AssignmentOperator._(AssignmentOperatorKind.SHR, '>>=',
                                 BinaryOperator.SHR);

  /// The &= assignment operator.
  static const AssignmentOperator AND =
      const AssignmentOperator._(AssignmentOperatorKind.AND, '&=',
                                 BinaryOperator.AND);

  /// The |= assignment operator.
  static const AssignmentOperator OR =
      const AssignmentOperator._(AssignmentOperatorKind.OR, '|=',
                                 BinaryOperator.OR);

  /// The ^= assignment operator.
  static const AssignmentOperator XOR =
      const AssignmentOperator._(AssignmentOperatorKind.XOR, '^=',
                                 BinaryOperator.XOR);

  static AssignmentOperator parse(String value) {
    switch (value) {
      case '=': return ASSIGN;
      case '*=': return MUL;
      case '/=': return DIV;
      case '%=': return MOD;
      case '~/=': return IDIV;
      case '+=': return ADD;
      case '-=': return SUB;
      case '<<=': return SHL;
      case '>>=': return SHR;
      case '&=': return AND;
      case '^=': return XOR;
      case '|=': return OR;
      default: return null;
    }
  }
}


enum IncDecOperatorKind {
  INC, DEC
}

class IncDecOperator {
  final IncDecOperatorKind kind;
  final String name;

  const IncDecOperator._(this.kind, this.name);

  String toString() => name;

  /// The prefix/postfix ++ operator.
  static const IncDecOperator INC =
      const IncDecOperator._(IncDecOperatorKind.INC, '++');

  /// The prefix/postfix -- operator.
  static const IncDecOperator DEC =
      const IncDecOperator._(IncDecOperatorKind.DEC, '--');

  static IncDecOperator parse(String value) {
    switch (value) {
      case '++': return INC;
      case '--': return DEC;
      default: return null;
    }
  }
}