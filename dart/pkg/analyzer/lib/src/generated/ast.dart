// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// This code was auto-generated, is not intended to be edited, and is subject to
// significant change. Please see the README file for more information.

library engine.ast;

import 'dart:collection';

import 'constant.dart';
import 'element.dart';
import 'engine.dart' show AnalysisEngine;
import 'java_core.dart';
import 'java_engine.dart';
import 'parser.dart';
import 'scanner.dart';
import 'source.dart' show LineInfo, Source;
import 'utilities_collection.dart' show TokenMap;
import 'utilities_dart.dart';

/**
 * Instances of the class `AdjacentStrings` represents two or more string literals that are
 * implicitly concatenated because of being adjacent (separated only by whitespace).
 *
 * While the grammar only allows adjacent strings when all of the strings are of the same kind
 * (single line or multi-line), this class doesn't enforce that restriction.
 *
 * <pre>
 * adjacentStrings ::=
 *     [StringLiteral] [StringLiteral]+
 * </pre>
 */
class AdjacentStrings extends StringLiteral {
  /**
   * The strings that are implicitly concatenated.
   */
  NodeList<StringLiteral> _strings;

  /**
   * Initialize a newly created list of adjacent strings.
   *
   * @param strings the strings that are implicitly concatenated
   */
  AdjacentStrings(List<StringLiteral> strings) {
    _strings = new NodeList<StringLiteral>(this, strings);
  }

  @override
  Token get beginToken => _strings.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()..addAll(_strings);

  @override
  Token get endToken => _strings.endToken;

  /**
   * Return the strings that are implicitly concatenated.
   *
   * @return the strings that are implicitly concatenated
   */
  NodeList<StringLiteral> get strings => _strings;

  @override
  accept(AstVisitor visitor) => visitor.visitAdjacentStrings(this);

  @override
  void appendStringValue(StringBuffer buffer) {
    for (StringLiteral stringLiteral in strings) {
      stringLiteral.appendStringValue(buffer);
    }
  }

  @override
  void visitChildren(AstVisitor visitor) {
    _strings.accept(visitor);
  }
}

/**
 * The abstract class `AnnotatedNode` defines the behavior of nodes that can be annotated with
 * both a comment and metadata.
 */
abstract class AnnotatedNode extends AstNode {
  /**
   * The documentation comment associated with this node, or `null` if this node does not have
   * a documentation comment associated with it.
   */
  Comment _comment;

  /**
   * The annotations associated with this node.
   */
  NodeList<Annotation> _metadata;

  /**
   * Initialize a newly created node.
   *
   * @param comment the documentation comment associated with this node
   * @param metadata the annotations associated with this node
   */
  AnnotatedNode(Comment comment, List<Annotation> metadata) {
    _comment = becomeParentOf(comment);
    _metadata = new NodeList<Annotation>(this, metadata);
  }

  @override
  Token get beginToken {
    if (_comment == null) {
      if (_metadata.isEmpty) {
        return firstTokenAfterCommentAndMetadata;
      }
      return _metadata.beginToken;
    } else if (_metadata.isEmpty) {
      return _comment.beginToken;
    }
    Token commentToken = _comment.beginToken;
    Token metadataToken = _metadata.beginToken;
    if (commentToken.offset < metadataToken.offset) {
      return commentToken;
    }
    return metadataToken;
  }

  /**
   * Return the documentation comment associated with this node, or `null` if this node does
   * not have a documentation comment associated with it.
   *
   * @return the documentation comment associated with this node
   */
  Comment get documentationComment => _comment;

  /**
   * Set the documentation comment associated with this node to the given comment.
   *
   * @param comment the documentation comment to be associated with this node
   */
  void set documentationComment(Comment comment) {
    _comment = becomeParentOf(comment);
  }

  /**
   * Return the first token following the comment and metadata.
   *
   * @return the first token following the comment and metadata
   */
  Token get firstTokenAfterCommentAndMetadata;

  /**
   * Return the annotations associated with this node.
   *
   * @return the annotations associated with this node
   */
  NodeList<Annotation> get metadata => _metadata;

  /**
   * Set the metadata associated with this node to the given metadata.
   *
   * @param metadata the metadata to be associated with this node
   */
  void set metadata(List<Annotation> metadata) {
    _metadata.clear();
    _metadata.addAll(metadata);
  }

  /**
   * Return an array containing the comment and annotations associated with this node, sorted in
   * lexical order.
   *
   * @return the comment and annotations associated with this node in the order in which they
   *         appeared in the original source
   */
  List<AstNode> get sortedCommentAndAnnotations {
    return <AstNode>[]
        ..add(_comment)
        ..addAll(_metadata)
        ..sort(AstNode.LEXICAL_ORDER);
  }

  ChildEntities get _childEntities {
    ChildEntities result = new ChildEntities();
    if (_commentIsBeforeAnnotations()) {
      result
          ..add(_comment)
          ..addAll(_metadata);
    } else {
      result.addAll(sortedCommentAndAnnotations);
    }
    return result;
  }

  @override
  void visitChildren(AstVisitor visitor) {
    if (_commentIsBeforeAnnotations()) {
      safelyVisitChild(_comment, visitor);
      _metadata.accept(visitor);
    } else {
      for (AstNode child in sortedCommentAndAnnotations) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Return `true` if the comment is lexically before any annotations.
   *
   * @return `true` if the comment is lexically before any annotations
   */
  bool _commentIsBeforeAnnotations() {
    if (_comment == null || _metadata.isEmpty) {
      return true;
    }
    Annotation firstAnnotation = _metadata[0];
    return _comment.offset < firstAnnotation.offset;
  }
}

/**
 * Instances of the class `Annotation` represent an annotation that can be associated with an
 * AST node.
 *
 * <pre>
 * metadata ::=
 *     annotation*
 *
 * annotation ::=
 *     '@' [Identifier] ('.' [SimpleIdentifier])? [ArgumentList]?
 * </pre>
 */
class Annotation extends AstNode {
  /**
   * The at sign that introduced the annotation.
   */
  Token atSign;

  /**
   * The name of the class defining the constructor that is being invoked or the name of the field
   * that is being referenced.
   */
  Identifier _name;

  /**
   * The period before the constructor name, or `null` if this annotation is not the
   * invocation of a named constructor.
   */
  Token period;

  /**
   * The name of the constructor being invoked, or `null` if this annotation is not the
   * invocation of a named constructor.
   */
  SimpleIdentifier _constructorName;

  /**
   * The arguments to the constructor being invoked, or `null` if this annotation is not the
   * invocation of a constructor.
   */
  ArgumentList _arguments;

  /**
   * The element associated with this annotation, or `null` if the AST structure has not been
   * resolved or if this annotation could not be resolved.
   */
  Element _element;

  /**
   * The element annotation representing this annotation in the element model.
   */
  ElementAnnotation elementAnnotation;

  /**
   * Initialize a newly created annotation.
   *
   * @param atSign the at sign that introduced the annotation
   * @param name the name of the class defining the constructor that is being invoked or the name of
   *          the field that is being referenced
   * @param period the period before the constructor name, or `null` if this annotation is not
   *          the invocation of a named constructor
   * @param constructorName the name of the constructor being invoked, or `null` if this
   *          annotation is not the invocation of a named constructor
   * @param arguments the arguments to the constructor being invoked, or `null` if this
   *          annotation is not the invocation of a constructor
   */
  Annotation(this.atSign, Identifier name, this.period,
      SimpleIdentifier constructorName, ArgumentList arguments) {
    _name = becomeParentOf(name);
    _constructorName = becomeParentOf(constructorName);
    _arguments = becomeParentOf(arguments);
  }

  /**
   * Return the arguments to the constructor being invoked, or `null` if this annotation is
   * not the invocation of a constructor.
   *
   * @return the arguments to the constructor being invoked
   */
  ArgumentList get arguments => _arguments;

  /**
   * Set the arguments to the constructor being invoked to the given arguments.
   *
   * @param arguments the arguments to the constructor being invoked
   */
  void set arguments(ArgumentList arguments) {
    _arguments = becomeParentOf(arguments);
  }

  @override
  Token get beginToken => atSign;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(atSign)
      ..add(_name)
      ..add(period)
      ..add(_constructorName)
      ..add(_arguments);

  /**
   * Return the name of the constructor being invoked, or `null` if this annotation is not the
   * invocation of a named constructor.
   *
   * @return the name of the constructor being invoked
   */
  SimpleIdentifier get constructorName => _constructorName;

  /**
   * Set the name of the constructor being invoked to the given name.
   *
   * @param constructorName the name of the constructor being invoked
   */
  void set constructorName(SimpleIdentifier constructorName) {
    _constructorName = becomeParentOf(constructorName);
  }

  /**
   * Return the element associated with this annotation, or `null` if the AST structure has
   * not been resolved or if this annotation could not be resolved.
   *
   * @return the element associated with this annotation
   */
  Element get element {
    if (_element != null) {
      return _element;
    } else if (_name != null) {
      return _name.staticElement;
    }
    return null;
  }

  /**
   * Set the element associated with this annotation based.
   *
   * @param element the element to be associated with this identifier
   */
  void set element(Element element) {
    _element = element;
  }

  @override
  Token get endToken {
    if (_arguments != null) {
      return _arguments.endToken;
    } else if (_constructorName != null) {
      return _constructorName.endToken;
    }
    return _name.endToken;
  }

  /**
   * Return the name of the class defining the constructor that is being invoked or the name of the
   * field that is being referenced.
   *
   * @return the name of the constructor being invoked or the name of the field being referenced
   */
  Identifier get name => _name;

  /**
   * Set the name of the class defining the constructor that is being invoked or the name of the
   * field that is being referenced to the given name.
   *
   * @param name the name of the constructor being invoked or the name of the field being referenced
   */
  void set name(Identifier name) {
    _name = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitAnnotation(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_constructorName, visitor);
    safelyVisitChild(_arguments, visitor);
  }
}

/**
 * Instances of the class `ArgumentList` represent a list of arguments in the invocation of a
 * executable element: a function, method, or constructor.
 *
 * <pre>
 * argumentList ::=
 *     '(' arguments? ')'
 *
 * arguments ::=
 *     [NamedExpression] (',' [NamedExpression])*
 *   | [Expression] (',' [NamedExpression])*
 * </pre>
 */
class ArgumentList extends AstNode {
  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The expressions producing the values of the arguments.
   */
  NodeList<Expression> _arguments;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * An array containing the elements representing the parameters corresponding to each of the
   * arguments in this list, or `null` if the AST has not been resolved or if the function or
   * method being invoked could not be determined based on static type information. The array must
   * be the same length as the number of arguments, but can contain `null` entries if a given
   * argument does not correspond to a formal parameter.
   */
  List<ParameterElement> _correspondingStaticParameters;

  /**
   * An array containing the elements representing the parameters corresponding to each of the
   * arguments in this list, or `null` if the AST has not been resolved or if the function or
   * method being invoked could not be determined based on propagated type information. The array
   * must be the same length as the number of arguments, but can contain `null` entries if a
   * given argument does not correspond to a formal parameter.
   */
  List<ParameterElement> _correspondingPropagatedParameters;

  /**
   * Initialize a newly created list of arguments.
   *
   * @param leftParenthesis the left parenthesis
   * @param arguments the expressions producing the values of the arguments
   * @param rightParenthesis the right parenthesis
   */
  ArgumentList(this.leftParenthesis, List<Expression> arguments,
      this.rightParenthesis) {
    _arguments = new NodeList<Expression>(this, arguments);
  }

  /**
   * Return the expressions producing the values of the arguments. Although the language requires
   * that positional arguments appear before named arguments, this class allows them to be
   * intermixed.
   *
   * @return the expressions producing the values of the arguments
   */
  NodeList<Expression> get arguments => _arguments;

  @override
  Token get beginToken => leftParenthesis;

  /**
   * TODO(paulberry): Add commas.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(leftParenthesis)
      ..addAll(_arguments)
      ..add(rightParenthesis);

  /**
   * Set the parameter elements corresponding to each of the arguments in this list to the given
   * array of parameters. The array of parameters must be the same length as the number of
   * arguments, but can contain `null` entries if a given argument does not correspond to a
   * formal parameter.
   *
   * @param parameters the parameter elements corresponding to the arguments
   */
  void set
      correspondingPropagatedParameters(List<ParameterElement> parameters) {
    if (parameters.length != _arguments.length) {
      throw new IllegalArgumentException(
          "Expected ${_arguments.length} parameters, not ${parameters.length}");
    }
    _correspondingPropagatedParameters = parameters;
  }

  /**
   * Set the parameter elements corresponding to each of the arguments in this list to the given
   * array of parameters. The array of parameters must be the same length as the number of
   * arguments, but can contain `null` entries if a given argument does not correspond to a
   * formal parameter.
   *
   * @param parameters the parameter elements corresponding to the arguments
   */
  void set correspondingStaticParameters(List<ParameterElement> parameters) {
    if (parameters.length != _arguments.length) {
      throw new IllegalArgumentException(
          "Expected ${_arguments.length} parameters, not ${parameters.length}");
    }
    _correspondingStaticParameters = parameters;
  }

  @override
  Token get endToken => rightParenthesis;

  @override
  accept(AstVisitor visitor) => visitor.visitArgumentList(this);

  /**
   * If the given expression is a child of this list, and the AST structure has been resolved, and
   * the function being invoked is known based on propagated type information, and the expression
   * corresponds to one of the parameters of the function being invoked, then return the parameter
   * element representing the parameter to which the value of the given expression will be bound.
   * Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.propagatedParameterElement].
   *
   * @param expression the expression corresponding to the parameter to be returned
   * @return the parameter element representing the parameter to which the value of the expression
   *         will be bound
   */
  ParameterElement getPropagatedParameterElementFor(Expression expression) {
    if (_correspondingPropagatedParameters == null) {
      // Either the AST structure has not been resolved or the invocation
      // of which this list is a part could not be resolved.
      return null;
    }
    int index = _arguments.indexOf(expression);
    if (index < 0) {
      // The expression isn't a child of this node.
      return null;
    }
    return _correspondingPropagatedParameters[index];
  }

  /**
   * If the given expression is a child of this list, and the AST structure has been resolved, and
   * the function being invoked is known based on static type information, and the expression
   * corresponds to one of the parameters of the function being invoked, then return the parameter
   * element representing the parameter to which the value of the given expression will be bound.
   * Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.staticParameterElement].
   *
   * @param expression the expression corresponding to the parameter to be returned
   * @return the parameter element representing the parameter to which the value of the expression
   *         will be bound
   */
  ParameterElement getStaticParameterElementFor(Expression expression) {
    if (_correspondingStaticParameters == null) {
      // Either the AST structure has not been resolved or the invocation
      // of which this list is a part could not be resolved.
      return null;
    }
    int index = _arguments.indexOf(expression);
    if (index < 0) {
      // The expression isn't a child of this node.
      return null;
    }
    return _correspondingStaticParameters[index];
  }

  @override
  void visitChildren(AstVisitor visitor) {
    _arguments.accept(visitor);
  }
}

/**
 * Instances of the class `AsExpression` represent an 'as' expression.
 *
 * <pre>
 * asExpression ::=
 *     [Expression] 'as' [TypeName]
 * </pre>
 */
class AsExpression extends Expression {
  /**
   * The expression used to compute the value being cast.
   */
  Expression _expression;

  /**
   * The as operator.
   */
  Token asOperator;

  /**
   * The name of the type being cast to.
   */
  TypeName _type;

  /**
   * Initialize a newly created as expression.
   *
   * @param expression the expression used to compute the value being cast
   * @param asOperator the as operator
   * @param type the name of the type being cast to
   */
  AsExpression(Expression expression, this.asOperator, TypeName type) {
    _expression = becomeParentOf(expression);
    _type = becomeParentOf(type);
  }

  @override
  Token get beginToken => _expression.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_expression)
      ..add(asOperator)
      ..add(_type);

  @override
  Token get endToken => _type.endToken;

  /**
   * Return the expression used to compute the value being cast.
   *
   * @return the expression used to compute the value being cast
   */
  Expression get expression => _expression;

  /**
   * Set the expression used to compute the value being cast to the given expression.
   *
   * @param expression the expression used to compute the value being cast
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  int get precedence => 7;

  /**
   * Return the name of the type being cast to.
   *
   * @return the name of the type being cast to
   */
  TypeName get type => _type;

  /**
   * Set the name of the type being cast to to the given name.
   *
   * @param name the name of the type being cast to
   */
  void set type(TypeName name) {
    _type = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitAsExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
    safelyVisitChild(_type, visitor);
  }
}

/**
 * Instances of the class `AssertStatement` represent an assert statement.
 *
 * <pre>
 * assertStatement ::=
 *     'assert' '(' [Expression] ')' ';'
 * </pre>
 */
class AssertStatement extends Statement {
  /**
   * The token representing the 'assert' keyword.
   */
  Token keyword;

  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The condition that is being asserted to be `true`.
   */
  Expression _condition;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * The semicolon terminating the statement.
   */
  Token semicolon;

  /**
   * Initialize a newly created assert statement.
   *
   * @param keyword the token representing the 'assert' keyword
   * @param leftParenthesis the left parenthesis
   * @param condition the condition that is being asserted to be `true`
   * @param rightParenthesis the right parenthesis
   * @param semicolon the semicolon terminating the statement
   */
  AssertStatement(this.keyword, this.leftParenthesis, Expression condition,
      this.rightParenthesis, this.semicolon) {
    _condition = becomeParentOf(condition);
  }

  @override
  Token get beginToken => keyword;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(leftParenthesis)
      ..add(_condition)
      ..add(rightParenthesis)
      ..add(semicolon);

  /**
   * Return the condition that is being asserted to be `true`.
   *
   * @return the condition that is being asserted to be `true`
   */
  Expression get condition => _condition;

  /**
   * Set the condition that is being asserted to be `true` to the given expression.
   *
   * @param the condition that is being asserted to be `true`
   */
  void set condition(Expression condition) {
    _condition = becomeParentOf(condition);
  }

  @override
  Token get endToken => semicolon;

  @override
  accept(AstVisitor visitor) => visitor.visitAssertStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_condition, visitor);
  }
}

/**
 * Instances of the class `AssignmentExpression` represent an assignment expression.
 *
 * <pre>
 * assignmentExpression ::=
 *     [Expression] [Token] [Expression]
 * </pre>
 */
class AssignmentExpression extends Expression {
  /**
   * The expression used to compute the left hand side.
   */
  Expression _leftHandSide;

  /**
   * The assignment operator being applied.
   */
  Token operator;

  /**
   * The expression used to compute the right hand side.
   */
  Expression _rightHandSide;

  /**
   * The element associated with the operator based on the static type of the left-hand-side, or
   * `null` if the AST structure has not been resolved, if the operator is not a compound
   * operator, or if the operator could not be resolved.
   */
  MethodElement staticElement;

  /**
   * The element associated with the operator based on the propagated type of the left-hand-side, or
   * `null` if the AST structure has not been resolved, if the operator is not a compound
   * operator, or if the operator could not be resolved.
   */
  MethodElement propagatedElement;

  /**
   * Initialize a newly created assignment expression.
   *
   * @param leftHandSide the expression used to compute the left hand side
   * @param operator the assignment operator being applied
   * @param rightHandSide the expression used to compute the right hand side
   */
  AssignmentExpression(Expression leftHandSide, this.operator,
      Expression rightHandSide) {
    if (leftHandSide == null || rightHandSide == null) {
      String message;
      if (leftHandSide == null) {
        if (rightHandSide == null) {
          message = "Both the left-hand and right-hand sides are null";
        } else {
          message = "The left-hand size is null";
        }
      } else {
        message = "The right-hand size is null";
      }
      AnalysisEngine.instance.logger.logError(
          message,
          new CaughtException(new AnalysisException(message), null));
    }
    _leftHandSide = becomeParentOf(leftHandSide);
    _rightHandSide = becomeParentOf(rightHandSide);
  }

  @override
  Token get beginToken => _leftHandSide.beginToken;

  /**
   * Return the best element available for this operator. If resolution was able to find a better
   * element based on type propagation, that element will be returned. Otherwise, the element found
   * using the result of static analysis will be returned. If resolution has not been performed,
   * then `null` will be returned.
   *
   * @return the best element available for this operator
   */
  MethodElement get bestElement {
    MethodElement element = propagatedElement;
    if (element == null) {
      element = staticElement;
    }
    return element;
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_leftHandSide)
      ..add(operator)
      ..add(_rightHandSide);

  @override
  Token get endToken => _rightHandSide.endToken;

  /**
   * Set the expression used to compute the left hand side to the given expression.
   *
   * @return the expression used to compute the left hand side
   */
  Expression get leftHandSide => _leftHandSide;

  /**
   * Return the expression used to compute the left hand side.
   *
   * @param expression the expression used to compute the left hand side
   */
  void set leftHandSide(Expression expression) {
    _leftHandSide = becomeParentOf(expression);
  }

  @override
  int get precedence => 1;

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on
   * propagated type information, then return the parameter element representing the parameter to
   * which the value of the right operand will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.propagatedParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  ParameterElement get propagatedParameterElementForRightHandSide {
    ExecutableElement executableElement = null;
    if (propagatedElement != null) {
      executableElement = propagatedElement;
    } else {
      if (_leftHandSide is Identifier) {
        Identifier identifier = _leftHandSide as Identifier;
        Element leftElement = identifier.propagatedElement;
        if (leftElement is ExecutableElement) {
          executableElement = leftElement;
        }
      }
      if (_leftHandSide is PropertyAccess) {
        SimpleIdentifier identifier =
            (_leftHandSide as PropertyAccess).propertyName;
        Element leftElement = identifier.propagatedElement;
        if (leftElement is ExecutableElement) {
          executableElement = leftElement;
        }
      }
    }
    if (executableElement == null) {
      return null;
    }
    List<ParameterElement> parameters = executableElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  /**
   * Return the expression used to compute the right hand side.
   *
   * @return the expression used to compute the right hand side
   */
  Expression get rightHandSide => _rightHandSide;

  /**
   * Set the expression used to compute the left hand side to the given expression.
   *
   * @param expression the expression used to compute the left hand side
   */
  void set rightHandSide(Expression expression) {
    _rightHandSide = becomeParentOf(expression);
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on static
   * type information, then return the parameter element representing the parameter to which the
   * value of the right operand will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.staticParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  ParameterElement get staticParameterElementForRightHandSide {
    ExecutableElement executableElement = null;
    if (staticElement != null) {
      executableElement = staticElement;
    } else {
      if (_leftHandSide is Identifier) {
        Element leftElement = (_leftHandSide as Identifier).staticElement;
        if (leftElement is ExecutableElement) {
          executableElement = leftElement;
        }
      }
      if (_leftHandSide is PropertyAccess) {
        Element leftElement =
            (_leftHandSide as PropertyAccess).propertyName.staticElement;
        if (leftElement is ExecutableElement) {
          executableElement = leftElement;
        }
      }
    }
    if (executableElement == null) {
      return null;
    }
    List<ParameterElement> parameters = executableElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  @override
  accept(AstVisitor visitor) => visitor.visitAssignmentExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_leftHandSide, visitor);
    safelyVisitChild(_rightHandSide, visitor);
  }
}

/**
 * An `AstCloner` is an AST visitor that will clone any AST structure that it
 * visits. The cloner will only clone the structure, it will not preserve any
 * resolution results or properties associated with the nodes.
 */
class AstCloner implements AstVisitor<AstNode> {
  /**
   * A flag indicating whether tokens should be cloned while cloning an AST
   * structure.
   */
  final bool cloneTokens;

  /**
   * Initialize a newly created AST cloner to optionally clone tokens while
   * cloning AST nodes if [cloneTokens] is `true`.
   */
  AstCloner([this.cloneTokens = false]);

  /**
   * Return a clone of the given [node].
   */
  AstNode cloneNode(AstNode node) {
    if (node == null) {
      return null;
    }
    return node.accept(this) as AstNode;
  }

  /**
   * Return a list containing cloned versions of the nodes in the given list of
   * [nodes].
   */
  List<AstNode> cloneNodeList(NodeList nodes) {
    int count = nodes.length;
    List clonedNodes = new List();
    for (int i = 0; i < count; i++) {
      clonedNodes.add((nodes[i]).accept(this) as AstNode);
    }
    return clonedNodes;
  }

  /**
   * Clone the given [token] if tokens are supposed to be cloned.
   */
  Token cloneToken(Token token) {
    if (cloneTokens) {
      return (token == null ? null : token.copy());
    } else {
      return token;
    }
  }

  /**
   * Clone the given [tokens] if tokens are supposed to be cloned.
   */
  List<Token> cloneTokenList(List<Token> tokens) {
    if (cloneTokens) {
      return tokens.map((Token token) => token.copy()).toList();
    }
    return tokens;
  }

  @override
  AdjacentStrings visitAdjacentStrings(AdjacentStrings node) =>
      new AdjacentStrings(cloneNodeList(node.strings));

  @override
  Annotation visitAnnotation(Annotation node) =>
      new Annotation(
          cloneToken(node.atSign),
          cloneNode(node.name),
          cloneToken(node.period),
          cloneNode(node.constructorName),
          cloneNode(node.arguments));

  @override
  ArgumentList visitArgumentList(ArgumentList node) =>
      new ArgumentList(
          cloneToken(node.leftParenthesis),
          cloneNodeList(node.arguments),
          cloneToken(node.rightParenthesis));

  @override
  AsExpression visitAsExpression(AsExpression node) =>
      new AsExpression(
          cloneNode(node.expression),
          cloneToken(node.asOperator),
          cloneNode(node.type));

  @override
  AstNode visitAssertStatement(AssertStatement node) =>
      new AssertStatement(
          cloneToken(node.keyword),
          cloneToken(node.leftParenthesis),
          cloneNode(node.condition),
          cloneToken(node.rightParenthesis),
          cloneToken(node.semicolon));

  @override
  AssignmentExpression visitAssignmentExpression(AssignmentExpression node) =>
      new AssignmentExpression(
          cloneNode(node.leftHandSide),
          cloneToken(node.operator),
          cloneNode(node.rightHandSide));

  @override
  AwaitExpression visitAwaitExpression(AwaitExpression node) =>
      new AwaitExpression(cloneToken(node.awaitKeyword), cloneNode(node.expression));

  @override
  BinaryExpression visitBinaryExpression(BinaryExpression node) =>
      new BinaryExpression(
          cloneNode(node.leftOperand),
          cloneToken(node.operator),
          cloneNode(node.rightOperand));

  @override
  Block visitBlock(Block node) =>
      new Block(
          cloneToken(node.leftBracket),
          cloneNodeList(node.statements),
          cloneToken(node.rightBracket));

  @override
  BlockFunctionBody visitBlockFunctionBody(BlockFunctionBody node) =>
      new BlockFunctionBody(
          cloneToken(node.keyword),
          cloneToken(node.star),
          cloneNode(node.block));

  @override
  BooleanLiteral visitBooleanLiteral(BooleanLiteral node) =>
      new BooleanLiteral(cloneToken(node.literal), node.value);

  @override
  BreakStatement visitBreakStatement(BreakStatement node) =>
      new BreakStatement(
          cloneToken(node.keyword),
          cloneNode(node.label),
          cloneToken(node.semicolon));

  @override
  CascadeExpression visitCascadeExpression(CascadeExpression node) =>
      new CascadeExpression(
          cloneNode(node.target),
          cloneNodeList(node.cascadeSections));

  @override
  CatchClause visitCatchClause(CatchClause node) =>
      new CatchClause(
          cloneToken(node.onKeyword),
          cloneNode(node.exceptionType),
          cloneToken(node.catchKeyword),
          cloneToken(node.leftParenthesis),
          cloneNode(node.exceptionParameter),
          cloneToken(node.comma),
          cloneNode(node.stackTraceParameter),
          cloneToken(node.rightParenthesis),
          cloneNode(node.body));

  @override
  ClassDeclaration visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration copy = new ClassDeclaration(
        cloneNode(node.documentationComment),
        cloneNodeList(node.metadata),
        cloneToken(node.abstractKeyword),
        cloneToken(node.classKeyword),
        cloneNode(node.name),
        cloneNode(node.typeParameters),
        cloneNode(node.extendsClause),
        cloneNode(node.withClause),
        cloneNode(node.implementsClause),
        cloneToken(node.leftBracket),
        cloneNodeList(node.members),
        cloneToken(node.rightBracket));
    copy.nativeClause = cloneNode(node.nativeClause);
    return copy;
  }

  @override
  ClassTypeAlias visitClassTypeAlias(ClassTypeAlias node) =>
      new ClassTypeAlias(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.keyword),
          cloneNode(node.name),
          cloneNode(node.typeParameters),
          cloneToken(node.equals),
          cloneToken(node.abstractKeyword),
          cloneNode(node.superclass),
          cloneNode(node.withClause),
          cloneNode(node.implementsClause),
          cloneToken(node.semicolon));

  @override
  Comment visitComment(Comment node) {
    if (node.isDocumentation) {
      return Comment.createDocumentationCommentWithReferences(
          cloneTokenList(node.tokens),
          cloneNodeList(node.references));
    } else if (node.isBlock) {
      return Comment.createBlockComment(cloneTokenList(node.tokens));
    }
    return Comment.createEndOfLineComment(cloneTokenList(node.tokens));
  }

  @override
  CommentReference visitCommentReference(CommentReference node) =>
      new CommentReference(cloneToken(node.newKeyword), cloneNode(node.identifier));

  @override
  CompilationUnit visitCompilationUnit(CompilationUnit node) {
    CompilationUnit clone = new CompilationUnit(
        cloneToken(node.beginToken),
        cloneNode(node.scriptTag),
        cloneNodeList(node.directives),
        cloneNodeList(node.declarations),
        cloneToken(node.endToken));
    clone.lineInfo = node.lineInfo;
    return clone;
  }

  @override
  ConditionalExpression
      visitConditionalExpression(ConditionalExpression node) =>
      new ConditionalExpression(
          cloneNode(node.condition),
          cloneToken(node.question),
          cloneNode(node.thenExpression),
          cloneToken(node.colon),
          cloneNode(node.elseExpression));

  @override
  ConstructorDeclaration
      visitConstructorDeclaration(ConstructorDeclaration node) =>
      new ConstructorDeclaration(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.externalKeyword),
          cloneToken(node.constKeyword),
          cloneToken(node.factoryKeyword),
          cloneNode(node.returnType),
          cloneToken(node.period),
          cloneNode(node.name),
          cloneNode(node.parameters),
          cloneToken(node.separator),
          cloneNodeList(node.initializers),
          cloneNode(node.redirectedConstructor),
          cloneNode(node.body));

  @override
  ConstructorFieldInitializer
      visitConstructorFieldInitializer(ConstructorFieldInitializer node) =>
      new ConstructorFieldInitializer(
          cloneToken(node.keyword),
          cloneToken(node.period),
          cloneNode(node.fieldName),
          cloneToken(node.equals),
          cloneNode(node.expression));

  @override
  ConstructorName visitConstructorName(ConstructorName node) =>
      new ConstructorName(
          cloneNode(node.type),
          cloneToken(node.period),
          cloneNode(node.name));

  @override
  ContinueStatement visitContinueStatement(ContinueStatement node) =>
      new ContinueStatement(
          cloneToken(node.keyword),
          cloneNode(node.label),
          cloneToken(node.semicolon));

  @override
  DeclaredIdentifier visitDeclaredIdentifier(DeclaredIdentifier node) =>
      new DeclaredIdentifier(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.keyword),
          cloneNode(node.type),
          cloneNode(node.identifier));

  @override
  DefaultFormalParameter
      visitDefaultFormalParameter(DefaultFormalParameter node) =>
      new DefaultFormalParameter(
          cloneNode(node.parameter),
          node.kind,
          cloneToken(node.separator),
          cloneNode(node.defaultValue));

  @override
  DoStatement visitDoStatement(DoStatement node) =>
      new DoStatement(
          cloneToken(node.doKeyword),
          cloneNode(node.body),
          cloneToken(node.whileKeyword),
          cloneToken(node.leftParenthesis),
          cloneNode(node.condition),
          cloneToken(node.rightParenthesis),
          cloneToken(node.semicolon));

  @override
  DoubleLiteral visitDoubleLiteral(DoubleLiteral node) =>
      new DoubleLiteral(cloneToken(node.literal), node.value);

  @override
  EmptyFunctionBody visitEmptyFunctionBody(EmptyFunctionBody node) =>
      new EmptyFunctionBody(cloneToken(node.semicolon));

  @override
  EmptyStatement visitEmptyStatement(EmptyStatement node) =>
      new EmptyStatement(cloneToken(node.semicolon));

  @override
  AstNode visitEnumConstantDeclaration(EnumConstantDeclaration node) =>
      new EnumConstantDeclaration(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneNode(node.name));

  @override
  EnumDeclaration visitEnumDeclaration(EnumDeclaration node) =>
      new EnumDeclaration(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.keyword),
          cloneNode(node.name),
          cloneToken(node.leftBracket),
          cloneNodeList(node.constants),
          cloneToken(node.rightBracket));

  @override
  ExportDirective visitExportDirective(ExportDirective node) {
    ExportDirective directive = new ExportDirective(
        cloneNode(node.documentationComment),
        cloneNodeList(node.metadata),
        cloneToken(node.keyword),
        cloneNode(node.uri),
        cloneNodeList(node.combinators),
        cloneToken(node.semicolon));
    directive.source = node.source;
    directive.uriContent = node.uriContent;
    return directive;
  }

  @override
  ExpressionFunctionBody
      visitExpressionFunctionBody(ExpressionFunctionBody node) =>
      new ExpressionFunctionBody(
          cloneToken(node.keyword),
          cloneToken(node.functionDefinition),
          cloneNode(node.expression),
          cloneToken(node.semicolon));

  @override
  ExpressionStatement visitExpressionStatement(ExpressionStatement node) =>
      new ExpressionStatement(cloneNode(node.expression), cloneToken(node.semicolon));

  @override
  ExtendsClause visitExtendsClause(ExtendsClause node) =>
      new ExtendsClause(cloneToken(node.keyword), cloneNode(node.superclass));

  @override
  FieldDeclaration visitFieldDeclaration(FieldDeclaration node) =>
      new FieldDeclaration(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.staticKeyword),
          cloneNode(node.fields),
          cloneToken(node.semicolon));

  @override
  FieldFormalParameter visitFieldFormalParameter(FieldFormalParameter node) =>
      new FieldFormalParameter(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.keyword),
          cloneNode(node.type),
          cloneToken(node.thisToken),
          cloneToken(node.period),
          cloneNode(node.identifier),
          cloneNode(node.parameters));

  @override
  ForEachStatement visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.loopVariable;
    if (loopVariable == null) {
      return new ForEachStatement.con2(
          cloneToken(node.awaitKeyword),
          cloneToken(node.forKeyword),
          cloneToken(node.leftParenthesis),
          cloneNode(node.identifier),
          cloneToken(node.inKeyword),
          cloneNode(node.iterable),
          cloneToken(node.rightParenthesis),
          cloneNode(node.body));
    }
    return new ForEachStatement.con1(
        cloneToken(node.awaitKeyword),
        cloneToken(node.forKeyword),
        cloneToken(node.leftParenthesis),
        cloneNode(loopVariable),
        cloneToken(node.inKeyword),
        cloneNode(node.iterable),
        cloneToken(node.rightParenthesis),
        cloneNode(node.body));
  }

  @override
  FormalParameterList visitFormalParameterList(FormalParameterList node) =>
      new FormalParameterList(
          cloneToken(node.leftParenthesis),
          cloneNodeList(node.parameters),
          cloneToken(node.leftDelimiter),
          cloneToken(node.rightDelimiter),
          cloneToken(node.rightParenthesis));

  @override
  ForStatement visitForStatement(ForStatement node) =>
      new ForStatement(
          cloneToken(node.forKeyword),
          cloneToken(node.leftParenthesis),
          cloneNode(node.variables),
          cloneNode(node.initialization),
          cloneToken(node.leftSeparator),
          cloneNode(node.condition),
          cloneToken(node.rightSeparator),
          cloneNodeList(node.updaters),
          cloneToken(node.rightParenthesis),
          cloneNode(node.body));

  @override
  FunctionDeclaration visitFunctionDeclaration(FunctionDeclaration node) =>
      new FunctionDeclaration(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.externalKeyword),
          cloneNode(node.returnType),
          cloneToken(node.propertyKeyword),
          cloneNode(node.name),
          cloneNode(node.functionExpression));

  @override
  FunctionDeclarationStatement
      visitFunctionDeclarationStatement(FunctionDeclarationStatement node) =>
      new FunctionDeclarationStatement(cloneNode(node.functionDeclaration));

  @override
  FunctionExpression visitFunctionExpression(FunctionExpression node) =>
      new FunctionExpression(cloneNode(node.parameters), cloneNode(node.body));

  @override
  FunctionExpressionInvocation
      visitFunctionExpressionInvocation(FunctionExpressionInvocation node) =>
      new FunctionExpressionInvocation(
          cloneNode(node.function),
          cloneNode(node.argumentList));

  @override
  FunctionTypeAlias visitFunctionTypeAlias(FunctionTypeAlias node) =>
      new FunctionTypeAlias(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.keyword),
          cloneNode(node.returnType),
          cloneNode(node.name),
          cloneNode(node.typeParameters),
          cloneNode(node.parameters),
          cloneToken(node.semicolon));

  @override
  FunctionTypedFormalParameter
      visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) =>
      new FunctionTypedFormalParameter(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneNode(node.returnType),
          cloneNode(node.identifier),
          cloneNode(node.parameters));

  @override
  HideCombinator visitHideCombinator(HideCombinator node) =>
      new HideCombinator(cloneToken(node.keyword), cloneNodeList(node.hiddenNames));

  @override
  IfStatement visitIfStatement(IfStatement node) =>
      new IfStatement(
          cloneToken(node.ifKeyword),
          cloneToken(node.leftParenthesis),
          cloneNode(node.condition),
          cloneToken(node.rightParenthesis),
          cloneNode(node.thenStatement),
          cloneToken(node.elseKeyword),
          cloneNode(node.elseStatement));

  @override
  ImplementsClause visitImplementsClause(ImplementsClause node) =>
      new ImplementsClause(cloneToken(node.keyword), cloneNodeList(node.interfaces));

  @override
  ImportDirective visitImportDirective(ImportDirective node) {
    ImportDirective directive = new ImportDirective(
        cloneNode(node.documentationComment),
        cloneNodeList(node.metadata),
        cloneToken(node.keyword),
        cloneNode(node.uri),
        cloneToken(node.deferredToken),
        cloneToken(node.asToken),
        cloneNode(node.prefix),
        cloneNodeList(node.combinators),
        cloneToken(node.semicolon));
    directive.source = node.source;
    directive.uriContent = node.uriContent;
    return directive;
  }

  @override
  IndexExpression visitIndexExpression(IndexExpression node) {
    Token period = node.period;
    if (period == null) {
      return new IndexExpression.forTarget(
          cloneNode(node.target),
          cloneToken(node.leftBracket),
          cloneNode(node.index),
          cloneToken(node.rightBracket));
    } else {
      return new IndexExpression.forCascade(
          cloneToken(period),
          cloneToken(node.leftBracket),
          cloneNode(node.index),
          cloneToken(node.rightBracket));
    }
  }

  @override
  InstanceCreationExpression
      visitInstanceCreationExpression(InstanceCreationExpression node) =>
      new InstanceCreationExpression(
          cloneToken(node.keyword),
          cloneNode(node.constructorName),
          cloneNode(node.argumentList));

  @override
  IntegerLiteral visitIntegerLiteral(IntegerLiteral node) =>
      new IntegerLiteral(cloneToken(node.literal), node.value);

  @override
  InterpolationExpression
      visitInterpolationExpression(InterpolationExpression node) =>
      new InterpolationExpression(
          cloneToken(node.leftBracket),
          cloneNode(node.expression),
          cloneToken(node.rightBracket));

  @override
  InterpolationString visitInterpolationString(InterpolationString node) =>
      new InterpolationString(cloneToken(node.contents), node.value);

  @override
  IsExpression visitIsExpression(IsExpression node) =>
      new IsExpression(
          cloneNode(node.expression),
          cloneToken(node.isOperator),
          cloneToken(node.notOperator),
          cloneNode(node.type));

  @override
  Label visitLabel(Label node) =>
      new Label(cloneNode(node.label), cloneToken(node.colon));

  @override
  LabeledStatement visitLabeledStatement(LabeledStatement node) =>
      new LabeledStatement(cloneNodeList(node.labels), cloneNode(node.statement));

  @override
  LibraryDirective visitLibraryDirective(LibraryDirective node) =>
      new LibraryDirective(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.libraryToken),
          cloneNode(node.name),
          cloneToken(node.semicolon));

  @override
  LibraryIdentifier visitLibraryIdentifier(LibraryIdentifier node) =>
      new LibraryIdentifier(cloneNodeList(node.components));

  @override
  ListLiteral visitListLiteral(ListLiteral node) =>
      new ListLiteral(
          cloneToken(node.constKeyword),
          cloneNode(node.typeArguments),
          cloneToken(node.leftBracket),
          cloneNodeList(node.elements),
          cloneToken(node.rightBracket));

  @override
  MapLiteral visitMapLiteral(MapLiteral node) =>
      new MapLiteral(
          cloneToken(node.constKeyword),
          cloneNode(node.typeArguments),
          cloneToken(node.leftBracket),
          cloneNodeList(node.entries),
          cloneToken(node.rightBracket));

  @override
  MapLiteralEntry visitMapLiteralEntry(MapLiteralEntry node) =>
      new MapLiteralEntry(
          cloneNode(node.key),
          cloneToken(node.separator),
          cloneNode(node.value));

  @override
  MethodDeclaration visitMethodDeclaration(MethodDeclaration node) =>
      new MethodDeclaration(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.externalKeyword),
          cloneToken(node.modifierKeyword),
          cloneNode(node.returnType),
          cloneToken(node.propertyKeyword),
          cloneToken(node.operatorKeyword),
          cloneNode(node.name),
          cloneNode(node.parameters),
          cloneNode(node.body));

  @override
  MethodInvocation visitMethodInvocation(MethodInvocation node) =>
      new MethodInvocation(
          cloneNode(node.target),
          cloneToken(node.period),
          cloneNode(node.methodName),
          cloneNode(node.argumentList));

  @override
  NamedExpression visitNamedExpression(NamedExpression node) =>
      new NamedExpression(cloneNode(node.name), cloneNode(node.expression));

  @override
  AstNode visitNativeClause(NativeClause node) =>
      new NativeClause(cloneToken(node.keyword), cloneNode(node.name));

  @override
  NativeFunctionBody visitNativeFunctionBody(NativeFunctionBody node) =>
      new NativeFunctionBody(
          cloneToken(node.nativeToken),
          cloneNode(node.stringLiteral),
          cloneToken(node.semicolon));

  @override
  NullLiteral visitNullLiteral(NullLiteral node) =>
      new NullLiteral(cloneToken(node.literal));

  @override
  ParenthesizedExpression
      visitParenthesizedExpression(ParenthesizedExpression node) =>
      new ParenthesizedExpression(
          cloneToken(node.leftParenthesis),
          cloneNode(node.expression),
          cloneToken(node.rightParenthesis));

  @override
  PartDirective visitPartDirective(PartDirective node) {
    PartDirective directive = new PartDirective(
        cloneNode(node.documentationComment),
        cloneNodeList(node.metadata),
        cloneToken(node.partToken),
        cloneNode(node.uri),
        cloneToken(node.semicolon));
    directive.source = node.source;
    directive.uriContent = node.uriContent;
    return directive;
  }

  @override
  PartOfDirective visitPartOfDirective(PartOfDirective node) =>
      new PartOfDirective(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.partToken),
          cloneToken(node.ofToken),
          cloneNode(node.libraryName),
          cloneToken(node.semicolon));

  @override
  PostfixExpression visitPostfixExpression(PostfixExpression node) =>
      new PostfixExpression(cloneNode(node.operand), cloneToken(node.operator));

  @override
  PrefixedIdentifier visitPrefixedIdentifier(PrefixedIdentifier node) =>
      new PrefixedIdentifier(
          cloneNode(node.prefix),
          cloneToken(node.period),
          cloneNode(node.identifier));

  @override
  PrefixExpression visitPrefixExpression(PrefixExpression node) =>
      new PrefixExpression(cloneToken(node.operator), cloneNode(node.operand));

  @override
  PropertyAccess visitPropertyAccess(PropertyAccess node) =>
      new PropertyAccess(
          cloneNode(node.target),
          cloneToken(node.operator),
          cloneNode(node.propertyName));

  @override
  RedirectingConstructorInvocation
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) =>
      new RedirectingConstructorInvocation(
          cloneToken(node.keyword),
          cloneToken(node.period),
          cloneNode(node.constructorName),
          cloneNode(node.argumentList));

  @override
  RethrowExpression visitRethrowExpression(RethrowExpression node) =>
      new RethrowExpression(cloneToken(node.keyword));

  @override
  ReturnStatement visitReturnStatement(ReturnStatement node) =>
      new ReturnStatement(
          cloneToken(node.keyword),
          cloneNode(node.expression),
          cloneToken(node.semicolon));

  @override
  ScriptTag visitScriptTag(ScriptTag node) =>
      new ScriptTag(cloneToken(node.scriptTag));

  @override
  ShowCombinator visitShowCombinator(ShowCombinator node) =>
      new ShowCombinator(cloneToken(node.keyword), cloneNodeList(node.shownNames));

  @override
  SimpleFormalParameter
      visitSimpleFormalParameter(SimpleFormalParameter node) =>
      new SimpleFormalParameter(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneToken(node.keyword),
          cloneNode(node.type),
          cloneNode(node.identifier));

  @override
  SimpleIdentifier visitSimpleIdentifier(SimpleIdentifier node) =>
      new SimpleIdentifier(cloneToken(node.token));

  @override
  SimpleStringLiteral visitSimpleStringLiteral(SimpleStringLiteral node) =>
      new SimpleStringLiteral(cloneToken(node.literal), node.value);

  @override
  StringInterpolation visitStringInterpolation(StringInterpolation node) =>
      new StringInterpolation(cloneNodeList(node.elements));

  @override
  SuperConstructorInvocation
      visitSuperConstructorInvocation(SuperConstructorInvocation node) =>
      new SuperConstructorInvocation(
          cloneToken(node.keyword),
          cloneToken(node.period),
          cloneNode(node.constructorName),
          cloneNode(node.argumentList));

  @override
  SuperExpression visitSuperExpression(SuperExpression node) =>
      new SuperExpression(cloneToken(node.keyword));

  @override
  SwitchCase visitSwitchCase(SwitchCase node) =>
      new SwitchCase(
          cloneNodeList(node.labels),
          cloneToken(node.keyword),
          cloneNode(node.expression),
          cloneToken(node.colon),
          cloneNodeList(node.statements));

  @override
  SwitchDefault visitSwitchDefault(SwitchDefault node) =>
      new SwitchDefault(
          cloneNodeList(node.labels),
          cloneToken(node.keyword),
          cloneToken(node.colon),
          cloneNodeList(node.statements));

  @override
  SwitchStatement visitSwitchStatement(SwitchStatement node) =>
      new SwitchStatement(
          cloneToken(node.keyword),
          cloneToken(node.leftParenthesis),
          cloneNode(node.expression),
          cloneToken(node.rightParenthesis),
          cloneToken(node.leftBracket),
          cloneNodeList(node.members),
          cloneToken(node.rightBracket));

  @override
  SymbolLiteral visitSymbolLiteral(SymbolLiteral node) =>
      new SymbolLiteral(cloneToken(node.poundSign), cloneTokenList(node.components));

  @override
  ThisExpression visitThisExpression(ThisExpression node) =>
      new ThisExpression(cloneToken(node.keyword));

  @override
  ThrowExpression visitThrowExpression(ThrowExpression node) =>
      new ThrowExpression(cloneToken(node.keyword), cloneNode(node.expression));

  @override
  TopLevelVariableDeclaration
      visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) =>
      new TopLevelVariableDeclaration(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneNode(node.variables),
          cloneToken(node.semicolon));

  @override
  TryStatement visitTryStatement(TryStatement node) =>
      new TryStatement(
          cloneToken(node.tryKeyword),
          cloneNode(node.body),
          cloneNodeList(node.catchClauses),
          cloneToken(node.finallyKeyword),
          cloneNode(node.finallyBlock));

  @override
  TypeArgumentList visitTypeArgumentList(TypeArgumentList node) =>
      new TypeArgumentList(
          cloneToken(node.leftBracket),
          cloneNodeList(node.arguments),
          cloneToken(node.rightBracket));

  @override
  TypeName visitTypeName(TypeName node) =>
      new TypeName(cloneNode(node.name), cloneNode(node.typeArguments));

  @override
  TypeParameter visitTypeParameter(TypeParameter node) =>
      new TypeParameter(
          cloneNode(node.documentationComment),
          cloneNodeList(node.metadata),
          cloneNode(node.name),
          cloneToken(node.keyword),
          cloneNode(node.bound));

  @override
  TypeParameterList visitTypeParameterList(TypeParameterList node) =>
      new TypeParameterList(
          cloneToken(node.leftBracket),
          cloneNodeList(node.typeParameters),
          cloneToken(node.rightBracket));

  @override
  VariableDeclaration visitVariableDeclaration(VariableDeclaration node) =>
      new VariableDeclaration(
          null,
          cloneNodeList(node.metadata),
          cloneNode(node.name),
          cloneToken(node.equals),
          cloneNode(node.initializer));

  @override
  VariableDeclarationList
      visitVariableDeclarationList(VariableDeclarationList node) =>
      new VariableDeclarationList(
          null,
          cloneNodeList(node.metadata),
          cloneToken(node.keyword),
          cloneNode(node.type),
          cloneNodeList(node.variables));

  @override
  VariableDeclarationStatement
      visitVariableDeclarationStatement(VariableDeclarationStatement node) =>
      new VariableDeclarationStatement(
          cloneNode(node.variables),
          cloneToken(node.semicolon));

  @override
  WhileStatement visitWhileStatement(WhileStatement node) =>
      new WhileStatement(
          cloneToken(node.keyword),
          cloneToken(node.leftParenthesis),
          cloneNode(node.condition),
          cloneToken(node.rightParenthesis),
          cloneNode(node.body));

  @override
  WithClause visitWithClause(WithClause node) =>
      new WithClause(cloneToken(node.withKeyword), cloneNodeList(node.mixinTypes));

  @override
  YieldStatement visitYieldStatement(YieldStatement node) =>
      new YieldStatement(
          cloneToken(node.yieldKeyword),
          cloneToken(node.star),
          cloneNode(node.expression),
          cloneToken(node.semicolon));
}

/**
 * An `AstComparator` compares the structure of two AstNodes to see whether
 * they are equal.
 */
class AstComparator implements AstVisitor<bool> {
  /**
   * The AST node with which the node being visited is to be compared. This is only valid at the
   * beginning of each visit method (until [isEqualNodes] is invoked).
   */
  AstNode _other;

  /**
   * Return `true` if the [first] node and the [second] node have the same
   * structure.
   *
   * *Note:* This method is only visible for testing purposes and should not be
   * used by clients.
   */
  bool isEqualNodes(AstNode first, AstNode second) {
    if (first == null) {
      return second == null;
    } else if (second == null) {
      return false;
    } else if (first.runtimeType != second.runtimeType) {
      return false;
    }
    _other = second;
    return first.accept(this);
  }

  /**
   * Return `true` if the [first] token and the [second] token have the same
   * structure.
   *
   * *Note:* This method is only visible for testing purposes and should not be
   * used by clients.
   */
  bool isEqualTokens(Token first, Token second) {
    if (first == null) {
      return second == null;
    } else if (second == null) {
      return false;
    } else if (identical(first, second)) {
      return true;
    }
    return first.offset == second.offset &&
        first.length == second.length &&
        first.lexeme == second.lexeme;
  }

  @override
  bool visitAdjacentStrings(AdjacentStrings node) {
    AdjacentStrings other = _other as AdjacentStrings;
    return _isEqualNodeLists(node.strings, other.strings);
  }

  @override
  bool visitAnnotation(Annotation node) {
    Annotation other = _other as Annotation;
    return isEqualTokens(node.atSign, other.atSign) &&
        isEqualNodes(node.name, other.name) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.constructorName, other.constructorName) &&
        isEqualNodes(node.arguments, other.arguments);
  }

  @override
  bool visitArgumentList(ArgumentList node) {
    ArgumentList other = _other as ArgumentList;
    return isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        _isEqualNodeLists(node.arguments, other.arguments) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis);
  }

  @override
  bool visitAsExpression(AsExpression node) {
    AsExpression other = _other as AsExpression;
    return isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.asOperator, other.asOperator) &&
        isEqualNodes(node.type, other.type);
  }

  @override
  bool visitAssertStatement(AssertStatement node) {
    AssertStatement other = _other as AssertStatement;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.condition, other.condition) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitAssignmentExpression(AssignmentExpression node) {
    AssignmentExpression other = _other as AssignmentExpression;
    return isEqualNodes(node.leftHandSide, other.leftHandSide) &&
        isEqualTokens(node.operator, other.operator) &&
        isEqualNodes(node.rightHandSide, other.rightHandSide);
  }

  @override
  bool visitAwaitExpression(AwaitExpression node) {
    AwaitExpression other = _other as AwaitExpression;
    return isEqualTokens(node.awaitKeyword, other.awaitKeyword) &&
        isEqualNodes(node.expression, other.expression);
  }

  @override
  bool visitBinaryExpression(BinaryExpression node) {
    BinaryExpression other = _other as BinaryExpression;
    return isEqualNodes(node.leftOperand, other.leftOperand) &&
        isEqualTokens(node.operator, other.operator) &&
        isEqualNodes(node.rightOperand, other.rightOperand);
  }

  @override
  bool visitBlock(Block node) {
    Block other = _other as Block;
    return isEqualTokens(node.leftBracket, other.leftBracket) &&
        _isEqualNodeLists(node.statements, other.statements) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitBlockFunctionBody(BlockFunctionBody node) {
    BlockFunctionBody other = _other as BlockFunctionBody;
    return isEqualNodes(node.block, other.block);
  }

  @override
  bool visitBooleanLiteral(BooleanLiteral node) {
    BooleanLiteral other = _other as BooleanLiteral;
    return isEqualTokens(node.literal, other.literal) &&
        node.value == other.value;
  }

  @override
  bool visitBreakStatement(BreakStatement node) {
    BreakStatement other = _other as BreakStatement;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.label, other.label) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitCascadeExpression(CascadeExpression node) {
    CascadeExpression other = _other as CascadeExpression;
    return isEqualNodes(node.target, other.target) &&
        _isEqualNodeLists(node.cascadeSections, other.cascadeSections);
  }

  @override
  bool visitCatchClause(CatchClause node) {
    CatchClause other = _other as CatchClause;
    return isEqualTokens(node.onKeyword, other.onKeyword) &&
        isEqualNodes(node.exceptionType, other.exceptionType) &&
        isEqualTokens(node.catchKeyword, other.catchKeyword) &&
        isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.exceptionParameter, other.exceptionParameter) &&
        isEqualTokens(node.comma, other.comma) &&
        isEqualNodes(node.stackTraceParameter, other.stackTraceParameter) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis) &&
        isEqualNodes(node.body, other.body);
  }

  @override
  bool visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration other = _other as ClassDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.abstractKeyword, other.abstractKeyword) &&
        isEqualTokens(node.classKeyword, other.classKeyword) &&
        isEqualNodes(node.name, other.name) &&
        isEqualNodes(node.typeParameters, other.typeParameters) &&
        isEqualNodes(node.extendsClause, other.extendsClause) &&
        isEqualNodes(node.withClause, other.withClause) &&
        isEqualNodes(node.implementsClause, other.implementsClause) &&
        isEqualTokens(node.leftBracket, other.leftBracket) &&
        _isEqualNodeLists(node.members, other.members) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitClassTypeAlias(ClassTypeAlias node) {
    ClassTypeAlias other = _other as ClassTypeAlias;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.name, other.name) &&
        isEqualNodes(node.typeParameters, other.typeParameters) &&
        isEqualTokens(node.equals, other.equals) &&
        isEqualTokens(node.abstractKeyword, other.abstractKeyword) &&
        isEqualNodes(node.superclass, other.superclass) &&
        isEqualNodes(node.withClause, other.withClause) &&
        isEqualNodes(node.implementsClause, other.implementsClause) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitComment(Comment node) {
    Comment other = _other as Comment;
    return _isEqualNodeLists(node.references, other.references);
  }

  @override
  bool visitCommentReference(CommentReference node) {
    CommentReference other = _other as CommentReference;
    return isEqualTokens(node.newKeyword, other.newKeyword) &&
        isEqualNodes(node.identifier, other.identifier);
  }

  @override
  bool visitCompilationUnit(CompilationUnit node) {
    CompilationUnit other = _other as CompilationUnit;
    return isEqualTokens(node.beginToken, other.beginToken) &&
        isEqualNodes(node.scriptTag, other.scriptTag) &&
        _isEqualNodeLists(node.directives, other.directives) &&
        _isEqualNodeLists(node.declarations, other.declarations) &&
        isEqualTokens(node.endToken, other.endToken);
  }

  @override
  bool visitConditionalExpression(ConditionalExpression node) {
    ConditionalExpression other = _other as ConditionalExpression;
    return isEqualNodes(node.condition, other.condition) &&
        isEqualTokens(node.question, other.question) &&
        isEqualNodes(node.thenExpression, other.thenExpression) &&
        isEqualTokens(node.colon, other.colon) &&
        isEqualNodes(node.elseExpression, other.elseExpression);
  }

  @override
  bool visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorDeclaration other = _other as ConstructorDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.externalKeyword, other.externalKeyword) &&
        isEqualTokens(node.constKeyword, other.constKeyword) &&
        isEqualTokens(node.factoryKeyword, other.factoryKeyword) &&
        isEqualNodes(node.returnType, other.returnType) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.name, other.name) &&
        isEqualNodes(node.parameters, other.parameters) &&
        isEqualTokens(node.separator, other.separator) &&
        _isEqualNodeLists(node.initializers, other.initializers) &&
        isEqualNodes(node.redirectedConstructor, other.redirectedConstructor) &&
        isEqualNodes(node.body, other.body);
  }

  @override
  bool visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    ConstructorFieldInitializer other = _other as ConstructorFieldInitializer;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.fieldName, other.fieldName) &&
        isEqualTokens(node.equals, other.equals) &&
        isEqualNodes(node.expression, other.expression);
  }

  @override
  bool visitConstructorName(ConstructorName node) {
    ConstructorName other = _other as ConstructorName;
    return isEqualNodes(node.type, other.type) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.name, other.name);
  }

  @override
  bool visitContinueStatement(ContinueStatement node) {
    ContinueStatement other = _other as ContinueStatement;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.label, other.label) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitDeclaredIdentifier(DeclaredIdentifier node) {
    DeclaredIdentifier other = _other as DeclaredIdentifier;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.type, other.type) &&
        isEqualNodes(node.identifier, other.identifier);
  }

  @override
  bool visitDefaultFormalParameter(DefaultFormalParameter node) {
    DefaultFormalParameter other = _other as DefaultFormalParameter;
    return isEqualNodes(node.parameter, other.parameter) &&
        node.kind == other.kind &&
        isEqualTokens(node.separator, other.separator) &&
        isEqualNodes(node.defaultValue, other.defaultValue);
  }

  @override
  bool visitDoStatement(DoStatement node) {
    DoStatement other = _other as DoStatement;
    return isEqualTokens(node.doKeyword, other.doKeyword) &&
        isEqualNodes(node.body, other.body) &&
        isEqualTokens(node.whileKeyword, other.whileKeyword) &&
        isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.condition, other.condition) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitDoubleLiteral(DoubleLiteral node) {
    DoubleLiteral other = _other as DoubleLiteral;
    return isEqualTokens(node.literal, other.literal) &&
        node.value == other.value;
  }

  @override
  bool visitEmptyFunctionBody(EmptyFunctionBody node) {
    EmptyFunctionBody other = _other as EmptyFunctionBody;
    return isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitEmptyStatement(EmptyStatement node) {
    EmptyStatement other = _other as EmptyStatement;
    return isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    EnumConstantDeclaration other = _other as EnumConstantDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualNodes(node.name, other.name);
  }

  @override
  bool visitEnumDeclaration(EnumDeclaration node) {
    EnumDeclaration other = _other as EnumDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.name, other.name) &&
        isEqualTokens(node.leftBracket, other.leftBracket) &&
        _isEqualNodeLists(node.constants, other.constants) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitExportDirective(ExportDirective node) {
    ExportDirective other = _other as ExportDirective;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.uri, other.uri) &&
        _isEqualNodeLists(node.combinators, other.combinators) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitExpressionFunctionBody(ExpressionFunctionBody node) {
    ExpressionFunctionBody other = _other as ExpressionFunctionBody;
    return isEqualTokens(node.functionDefinition, other.functionDefinition) &&
        isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitExpressionStatement(ExpressionStatement node) {
    ExpressionStatement other = _other as ExpressionStatement;
    return isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitExtendsClause(ExtendsClause node) {
    ExtendsClause other = _other as ExtendsClause;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.superclass, other.superclass);
  }

  @override
  bool visitFieldDeclaration(FieldDeclaration node) {
    FieldDeclaration other = _other as FieldDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.staticKeyword, other.staticKeyword) &&
        isEqualNodes(node.fields, other.fields) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitFieldFormalParameter(FieldFormalParameter node) {
    FieldFormalParameter other = _other as FieldFormalParameter;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.type, other.type) &&
        isEqualTokens(node.thisToken, other.thisToken) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.identifier, other.identifier);
  }

  @override
  bool visitForEachStatement(ForEachStatement node) {
    ForEachStatement other = _other as ForEachStatement;
    return isEqualTokens(node.forKeyword, other.forKeyword) &&
        isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.loopVariable, other.loopVariable) &&
        isEqualTokens(node.inKeyword, other.inKeyword) &&
        isEqualNodes(node.iterable, other.iterable) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis) &&
        isEqualNodes(node.body, other.body);
  }

  @override
  bool visitFormalParameterList(FormalParameterList node) {
    FormalParameterList other = _other as FormalParameterList;
    return isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        _isEqualNodeLists(node.parameters, other.parameters) &&
        isEqualTokens(node.leftDelimiter, other.leftDelimiter) &&
        isEqualTokens(node.rightDelimiter, other.rightDelimiter) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis);
  }

  @override
  bool visitForStatement(ForStatement node) {
    ForStatement other = _other as ForStatement;
    return isEqualTokens(node.forKeyword, other.forKeyword) &&
        isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.variables, other.variables) &&
        isEqualNodes(node.initialization, other.initialization) &&
        isEqualTokens(node.leftSeparator, other.leftSeparator) &&
        isEqualNodes(node.condition, other.condition) &&
        isEqualTokens(node.rightSeparator, other.rightSeparator) &&
        _isEqualNodeLists(node.updaters, other.updaters) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis) &&
        isEqualNodes(node.body, other.body);
  }

  @override
  bool visitFunctionDeclaration(FunctionDeclaration node) {
    FunctionDeclaration other = _other as FunctionDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.externalKeyword, other.externalKeyword) &&
        isEqualNodes(node.returnType, other.returnType) &&
        isEqualTokens(node.propertyKeyword, other.propertyKeyword) &&
        isEqualNodes(node.name, other.name) &&
        isEqualNodes(node.functionExpression, other.functionExpression);
  }

  @override
  bool visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    FunctionDeclarationStatement other = _other as FunctionDeclarationStatement;
    return isEqualNodes(node.functionDeclaration, other.functionDeclaration);
  }

  @override
  bool visitFunctionExpression(FunctionExpression node) {
    FunctionExpression other = _other as FunctionExpression;
    return isEqualNodes(node.parameters, other.parameters) &&
        isEqualNodes(node.body, other.body);
  }

  @override
  bool visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    FunctionExpressionInvocation other = _other as FunctionExpressionInvocation;
    return isEqualNodes(node.function, other.function) &&
        isEqualNodes(node.argumentList, other.argumentList);
  }

  @override
  bool visitFunctionTypeAlias(FunctionTypeAlias node) {
    FunctionTypeAlias other = _other as FunctionTypeAlias;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.returnType, other.returnType) &&
        isEqualNodes(node.name, other.name) &&
        isEqualNodes(node.typeParameters, other.typeParameters) &&
        isEqualNodes(node.parameters, other.parameters) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    FunctionTypedFormalParameter other = _other as FunctionTypedFormalParameter;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualNodes(node.returnType, other.returnType) &&
        isEqualNodes(node.identifier, other.identifier) &&
        isEqualNodes(node.parameters, other.parameters);
  }

  @override
  bool visitHideCombinator(HideCombinator node) {
    HideCombinator other = _other as HideCombinator;
    return isEqualTokens(node.keyword, other.keyword) &&
        _isEqualNodeLists(node.hiddenNames, other.hiddenNames);
  }

  @override
  bool visitIfStatement(IfStatement node) {
    IfStatement other = _other as IfStatement;
    return isEqualTokens(node.ifKeyword, other.ifKeyword) &&
        isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.condition, other.condition) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis) &&
        isEqualNodes(node.thenStatement, other.thenStatement) &&
        isEqualTokens(node.elseKeyword, other.elseKeyword) &&
        isEqualNodes(node.elseStatement, other.elseStatement);
  }

  @override
  bool visitImplementsClause(ImplementsClause node) {
    ImplementsClause other = _other as ImplementsClause;
    return isEqualTokens(node.keyword, other.keyword) &&
        _isEqualNodeLists(node.interfaces, other.interfaces);
  }

  @override
  bool visitImportDirective(ImportDirective node) {
    ImportDirective other = _other as ImportDirective;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.uri, other.uri) &&
        isEqualTokens(node.asToken, other.asToken) &&
        isEqualNodes(node.prefix, other.prefix) &&
        _isEqualNodeLists(node.combinators, other.combinators) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitIndexExpression(IndexExpression node) {
    IndexExpression other = _other as IndexExpression;
    return isEqualNodes(node.target, other.target) &&
        isEqualTokens(node.leftBracket, other.leftBracket) &&
        isEqualNodes(node.index, other.index) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitInstanceCreationExpression(InstanceCreationExpression node) {
    InstanceCreationExpression other = _other as InstanceCreationExpression;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.constructorName, other.constructorName) &&
        isEqualNodes(node.argumentList, other.argumentList);
  }

  @override
  bool visitIntegerLiteral(IntegerLiteral node) {
    IntegerLiteral other = _other as IntegerLiteral;
    return isEqualTokens(node.literal, other.literal) &&
        (node.value == other.value);
  }

  @override
  bool visitInterpolationExpression(InterpolationExpression node) {
    InterpolationExpression other = _other as InterpolationExpression;
    return isEqualTokens(node.leftBracket, other.leftBracket) &&
        isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitInterpolationString(InterpolationString node) {
    InterpolationString other = _other as InterpolationString;
    return isEqualTokens(node.contents, other.contents) &&
        node.value == other.value;
  }

  @override
  bool visitIsExpression(IsExpression node) {
    IsExpression other = _other as IsExpression;
    return isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.isOperator, other.isOperator) &&
        isEqualTokens(node.notOperator, other.notOperator) &&
        isEqualNodes(node.type, other.type);
  }

  @override
  bool visitLabel(Label node) {
    Label other = _other as Label;
    return isEqualNodes(node.label, other.label) &&
        isEqualTokens(node.colon, other.colon);
  }

  @override
  bool visitLabeledStatement(LabeledStatement node) {
    LabeledStatement other = _other as LabeledStatement;
    return _isEqualNodeLists(node.labels, other.labels) &&
        isEqualNodes(node.statement, other.statement);
  }

  @override
  bool visitLibraryDirective(LibraryDirective node) {
    LibraryDirective other = _other as LibraryDirective;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.libraryToken, other.libraryToken) &&
        isEqualNodes(node.name, other.name) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitLibraryIdentifier(LibraryIdentifier node) {
    LibraryIdentifier other = _other as LibraryIdentifier;
    return _isEqualNodeLists(node.components, other.components);
  }

  @override
  bool visitListLiteral(ListLiteral node) {
    ListLiteral other = _other as ListLiteral;
    return isEqualTokens(node.constKeyword, other.constKeyword) &&
        isEqualNodes(node.typeArguments, other.typeArguments) &&
        isEqualTokens(node.leftBracket, other.leftBracket) &&
        _isEqualNodeLists(node.elements, other.elements) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitMapLiteral(MapLiteral node) {
    MapLiteral other = _other as MapLiteral;
    return isEqualTokens(node.constKeyword, other.constKeyword) &&
        isEqualNodes(node.typeArguments, other.typeArguments) &&
        isEqualTokens(node.leftBracket, other.leftBracket) &&
        _isEqualNodeLists(node.entries, other.entries) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitMapLiteralEntry(MapLiteralEntry node) {
    MapLiteralEntry other = _other as MapLiteralEntry;
    return isEqualNodes(node.key, other.key) &&
        isEqualTokens(node.separator, other.separator) &&
        isEqualNodes(node.value, other.value);
  }

  @override
  bool visitMethodDeclaration(MethodDeclaration node) {
    MethodDeclaration other = _other as MethodDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.externalKeyword, other.externalKeyword) &&
        isEqualTokens(node.modifierKeyword, other.modifierKeyword) &&
        isEqualNodes(node.returnType, other.returnType) &&
        isEqualTokens(node.propertyKeyword, other.propertyKeyword) &&
        isEqualTokens(node.propertyKeyword, other.propertyKeyword) &&
        isEqualNodes(node.name, other.name) &&
        isEqualNodes(node.parameters, other.parameters) &&
        isEqualNodes(node.body, other.body);
  }

  @override
  bool visitMethodInvocation(MethodInvocation node) {
    MethodInvocation other = _other as MethodInvocation;
    return isEqualNodes(node.target, other.target) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.methodName, other.methodName) &&
        isEqualNodes(node.argumentList, other.argumentList);
  }

  @override
  bool visitNamedExpression(NamedExpression node) {
    NamedExpression other = _other as NamedExpression;
    return isEqualNodes(node.name, other.name) &&
        isEqualNodes(node.expression, other.expression);
  }

  @override
  bool visitNativeClause(NativeClause node) {
    NativeClause other = _other as NativeClause;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.name, other.name);
  }

  @override
  bool visitNativeFunctionBody(NativeFunctionBody node) {
    NativeFunctionBody other = _other as NativeFunctionBody;
    return isEqualTokens(node.nativeToken, other.nativeToken) &&
        isEqualNodes(node.stringLiteral, other.stringLiteral) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitNullLiteral(NullLiteral node) {
    NullLiteral other = _other as NullLiteral;
    return isEqualTokens(node.literal, other.literal);
  }

  @override
  bool visitParenthesizedExpression(ParenthesizedExpression node) {
    ParenthesizedExpression other = _other as ParenthesizedExpression;
    return isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis);
  }

  @override
  bool visitPartDirective(PartDirective node) {
    PartDirective other = _other as PartDirective;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.partToken, other.partToken) &&
        isEqualNodes(node.uri, other.uri) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitPartOfDirective(PartOfDirective node) {
    PartOfDirective other = _other as PartOfDirective;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.partToken, other.partToken) &&
        isEqualTokens(node.ofToken, other.ofToken) &&
        isEqualNodes(node.libraryName, other.libraryName) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitPostfixExpression(PostfixExpression node) {
    PostfixExpression other = _other as PostfixExpression;
    return isEqualNodes(node.operand, other.operand) &&
        isEqualTokens(node.operator, other.operator);
  }

  @override
  bool visitPrefixedIdentifier(PrefixedIdentifier node) {
    PrefixedIdentifier other = _other as PrefixedIdentifier;
    return isEqualNodes(node.prefix, other.prefix) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.identifier, other.identifier);
  }

  @override
  bool visitPrefixExpression(PrefixExpression node) {
    PrefixExpression other = _other as PrefixExpression;
    return isEqualTokens(node.operator, other.operator) &&
        isEqualNodes(node.operand, other.operand);
  }

  @override
  bool visitPropertyAccess(PropertyAccess node) {
    PropertyAccess other = _other as PropertyAccess;
    return isEqualNodes(node.target, other.target) &&
        isEqualTokens(node.operator, other.operator) &&
        isEqualNodes(node.propertyName, other.propertyName);
  }

  @override
  bool
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    RedirectingConstructorInvocation other =
        _other as RedirectingConstructorInvocation;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.constructorName, other.constructorName) &&
        isEqualNodes(node.argumentList, other.argumentList);
  }

  @override
  bool visitRethrowExpression(RethrowExpression node) {
    RethrowExpression other = _other as RethrowExpression;
    return isEqualTokens(node.keyword, other.keyword);
  }

  @override
  bool visitReturnStatement(ReturnStatement node) {
    ReturnStatement other = _other as ReturnStatement;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitScriptTag(ScriptTag node) {
    ScriptTag other = _other as ScriptTag;
    return isEqualTokens(node.scriptTag, other.scriptTag);
  }

  @override
  bool visitShowCombinator(ShowCombinator node) {
    ShowCombinator other = _other as ShowCombinator;
    return isEqualTokens(node.keyword, other.keyword) &&
        _isEqualNodeLists(node.shownNames, other.shownNames);
  }

  @override
  bool visitSimpleFormalParameter(SimpleFormalParameter node) {
    SimpleFormalParameter other = _other as SimpleFormalParameter;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.type, other.type) &&
        isEqualNodes(node.identifier, other.identifier);
  }

  @override
  bool visitSimpleIdentifier(SimpleIdentifier node) {
    SimpleIdentifier other = _other as SimpleIdentifier;
    return isEqualTokens(node.token, other.token);
  }

  @override
  bool visitSimpleStringLiteral(SimpleStringLiteral node) {
    SimpleStringLiteral other = _other as SimpleStringLiteral;
    return isEqualTokens(node.literal, other.literal) &&
        (node.value == other.value);
  }

  @override
  bool visitStringInterpolation(StringInterpolation node) {
    StringInterpolation other = _other as StringInterpolation;
    return _isEqualNodeLists(node.elements, other.elements);
  }

  @override
  bool visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    SuperConstructorInvocation other = _other as SuperConstructorInvocation;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualTokens(node.period, other.period) &&
        isEqualNodes(node.constructorName, other.constructorName) &&
        isEqualNodes(node.argumentList, other.argumentList);
  }

  @override
  bool visitSuperExpression(SuperExpression node) {
    SuperExpression other = _other as SuperExpression;
    return isEqualTokens(node.keyword, other.keyword);
  }

  @override
  bool visitSwitchCase(SwitchCase node) {
    SwitchCase other = _other as SwitchCase;
    return _isEqualNodeLists(node.labels, other.labels) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.colon, other.colon) &&
        _isEqualNodeLists(node.statements, other.statements);
  }

  @override
  bool visitSwitchDefault(SwitchDefault node) {
    SwitchDefault other = _other as SwitchDefault;
    return _isEqualNodeLists(node.labels, other.labels) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualTokens(node.colon, other.colon) &&
        _isEqualNodeLists(node.statements, other.statements);
  }

  @override
  bool visitSwitchStatement(SwitchStatement node) {
    SwitchStatement other = _other as SwitchStatement;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis) &&
        isEqualTokens(node.leftBracket, other.leftBracket) &&
        _isEqualNodeLists(node.members, other.members) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitSymbolLiteral(SymbolLiteral node) {
    SymbolLiteral other = _other as SymbolLiteral;
    return isEqualTokens(node.poundSign, other.poundSign) &&
        _isEqualTokenLists(node.components, other.components);
  }

  @override
  bool visitThisExpression(ThisExpression node) {
    ThisExpression other = _other as ThisExpression;
    return isEqualTokens(node.keyword, other.keyword);
  }

  @override
  bool visitThrowExpression(ThrowExpression node) {
    ThrowExpression other = _other as ThrowExpression;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.expression, other.expression);
  }

  @override
  bool visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    TopLevelVariableDeclaration other = _other as TopLevelVariableDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualNodes(node.variables, other.variables) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitTryStatement(TryStatement node) {
    TryStatement other = _other as TryStatement;
    return isEqualTokens(node.tryKeyword, other.tryKeyword) &&
        isEqualNodes(node.body, other.body) &&
        _isEqualNodeLists(node.catchClauses, other.catchClauses) &&
        isEqualTokens(node.finallyKeyword, other.finallyKeyword) &&
        isEqualNodes(node.finallyBlock, other.finallyBlock);
  }

  @override
  bool visitTypeArgumentList(TypeArgumentList node) {
    TypeArgumentList other = _other as TypeArgumentList;
    return isEqualTokens(node.leftBracket, other.leftBracket) &&
        _isEqualNodeLists(node.arguments, other.arguments) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitTypeName(TypeName node) {
    TypeName other = _other as TypeName;
    return isEqualNodes(node.name, other.name) &&
        isEqualNodes(node.typeArguments, other.typeArguments);
  }

  @override
  bool visitTypeParameter(TypeParameter node) {
    TypeParameter other = _other as TypeParameter;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualNodes(node.name, other.name) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.bound, other.bound);
  }

  @override
  bool visitTypeParameterList(TypeParameterList node) {
    TypeParameterList other = _other as TypeParameterList;
    return isEqualTokens(node.leftBracket, other.leftBracket) &&
        _isEqualNodeLists(node.typeParameters, other.typeParameters) &&
        isEqualTokens(node.rightBracket, other.rightBracket);
  }

  @override
  bool visitVariableDeclaration(VariableDeclaration node) {
    VariableDeclaration other = _other as VariableDeclaration;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualNodes(node.name, other.name) &&
        isEqualTokens(node.equals, other.equals) &&
        isEqualNodes(node.initializer, other.initializer);
  }

  @override
  bool visitVariableDeclarationList(VariableDeclarationList node) {
    VariableDeclarationList other = _other as VariableDeclarationList;
    return isEqualNodes(
        node.documentationComment,
        other.documentationComment) &&
        _isEqualNodeLists(node.metadata, other.metadata) &&
        isEqualTokens(node.keyword, other.keyword) &&
        isEqualNodes(node.type, other.type) &&
        _isEqualNodeLists(node.variables, other.variables);
  }

  @override
  bool visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    VariableDeclarationStatement other = _other as VariableDeclarationStatement;
    return isEqualNodes(node.variables, other.variables) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  @override
  bool visitWhileStatement(WhileStatement node) {
    WhileStatement other = _other as WhileStatement;
    return isEqualTokens(node.keyword, other.keyword) &&
        isEqualTokens(node.leftParenthesis, other.leftParenthesis) &&
        isEqualNodes(node.condition, other.condition) &&
        isEqualTokens(node.rightParenthesis, other.rightParenthesis) &&
        isEqualNodes(node.body, other.body);
  }

  @override
  bool visitWithClause(WithClause node) {
    WithClause other = _other as WithClause;
    return isEqualTokens(node.withKeyword, other.withKeyword) &&
        _isEqualNodeLists(node.mixinTypes, other.mixinTypes);
  }

  @override
  bool visitYieldStatement(YieldStatement node) {
    YieldStatement other = _other as YieldStatement;
    return isEqualTokens(node.yieldKeyword, other.yieldKeyword) &&
        isEqualNodes(node.expression, other.expression) &&
        isEqualTokens(node.semicolon, other.semicolon);
  }

  /**
   * Return `true` if the given lists of AST nodes have the same size and corresponding
   * elements are equal.
   *
   * @param first the first node being compared
   * @param second the second node being compared
   * @return `true` if the given AST nodes have the same size and corresponding elements are
   *         equal
   */
  bool _isEqualNodeLists(NodeList first, NodeList second) {
    if (first == null) {
      return second == null;
    } else if (second == null) {
      return false;
    }
    int size = first.length;
    if (second.length != size) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!isEqualNodes(first[i], second[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return `true` if the given arrays of tokens have the same length and corresponding
   * elements are equal.
   *
   * @param first the first node being compared
   * @param second the second node being compared
   * @return `true` if the given arrays of tokens have the same length and corresponding
   *         elements are equal
   */
  bool _isEqualTokenLists(List<Token> first, List<Token> second) {
    int length = first.length;
    if (second.length != length) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (!isEqualTokens(first[i], second[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return `true` if the [first] node and the [second] node are equal.
   */
  static bool equalNodes(AstNode first, AstNode second) {
    AstComparator comparator = new AstComparator();
    return comparator.isEqualNodes(first, second);
  }
}

/**
 * The abstract class `AstNode` defines the behavior common to all nodes in the AST structure
 * for a Dart program.
 */
abstract class AstNode {
  /**
   * An empty array of ast nodes.
   */
  static const List<AstNode> EMPTY_ARRAY = const <AstNode>[];

  /**
   * A comparator that can be used to sort AST nodes in lexical order. In other words,
   * `compare` will return a negative value if the offset of the first node is less than the
   * offset of the second node, zero (0) if the nodes have the same offset, and a positive value if
   * if the offset of the first node is greater than the offset of the second node.
   */
  static Comparator<AstNode> LEXICAL_ORDER =
      (AstNode first, AstNode second) => second.offset - first.offset;

  /**
   * The parent of the node, or `null` if the node is the root of an AST structure.
   */
  AstNode _parent;

  /**
   * A table mapping the names of properties to their values, or `null` if this node does not
   * have any properties associated with it.
   */
  Map<String, Object> _propertyMap;

  /**
   * Return the first token included in this node's source range.
   *
   * @return the first token included in this node's source range
   */
  Token get beginToken;

  /**
   * Iterate through all the entities (either AST nodes or tokens) which make
   * up the contents of this node, including doc comments but excluding other
   * comments.
   */
  Iterable get childEntities;

  /**
   * Return the offset of the character immediately following the last character of this node's
   * source range. This is equivalent to `node.getOffset() + node.getLength()`. For a
   * compilation unit this will be equal to the length of the unit's source. For synthetic nodes
   * this will be equivalent to the node's offset (because the length is zero (0) by definition).
   *
   * @return the offset of the character just past the node's source range
   */
  int get end => offset + length;

  /**
   * Return the last token included in this node's source range.
   *
   * @return the last token included in this node's source range
   */
  Token get endToken;

  /**
   * Return `true` if this node is a synthetic node. A synthetic node is a node that was
   * introduced by the parser in order to recover from an error in the code. Synthetic nodes always
   * have a length of zero (`0`).
   *
   * @return `true` if this node is a synthetic node
   */
  bool get isSynthetic => false;

  /**
   * Return the number of characters in the node's source range.
   *
   * @return the number of characters in the node's source range
   */
  int get length {
    Token beginToken = this.beginToken;
    Token endToken = this.endToken;
    if (beginToken == null || endToken == null) {
      return -1;
    }
    return endToken.offset + endToken.length - beginToken.offset;
  }

  /**
   * Return the offset from the beginning of the file to the first character in the node's source
   * range.
   *
   * @return the offset from the beginning of the file to the first character in the node's source
   *         range
   */
  int get offset {
    Token beginToken = this.beginToken;
    if (beginToken == null) {
      return -1;
    }
    return beginToken.offset;
  }

  /**
   * Return this node's parent node, or `null` if this node is the root of an AST structure.
   *
   * Note that the relationship between an AST node and its parent node may change over the lifetime
   * of a node.
   *
   * @return the parent of this node, or `null` if none
   */
  AstNode get parent => _parent;

  /**
   * Set the parent of this node to the given node.
   *
   * @param newParent the node that is to be made the parent of this node
   */
  @deprecated
  void set parent(AstNode newParent) {
    _parent = newParent;
  }

  /**
   * Return the node at the root of this node's AST structure. Note that this method's performance
   * is linear with respect to the depth of the node in the AST structure (O(depth)).
   *
   * @return the node at the root of this node's AST structure
   */
  AstNode get root {
    AstNode root = this;
    AstNode parent = this.parent;
    while (parent != null) {
      root = parent;
      parent = root.parent;
    }
    return root;
  }

  /**
   * Use the given visitor to visit this node.
   *
   * @param visitor the visitor that will visit this node
   * @return the value returned by the visitor as a result of visiting this node
   */
  accept(AstVisitor visitor);

  /**
   * Make this node the parent of the given child node.
   *
   * @param child the node that will become a child of this node
   * @return the node that was made a child of this node
   */
  AstNode becomeParentOf(AstNode child) {
    if (child != null) {
      child._parent = this;
    }
    return child;
  }

  /**
   * Return the node of the given class that most immediately encloses this node, or `null` if
   * there is no enclosing node of the given class.
   *
   * @param nodeClass the class of the node to be returned
   * @return the node of the given type that encloses this node
   */
  AstNode getAncestor(Predicate<AstNode> predicate) {
    AstNode node = this;
    while (node != null && !predicate(node)) {
      node = node.parent;
    }
    return node;
  }

  /**
   * Return the value of the property with the given name, or `null` if this node does not
   * have a property with the given name.
   *
   * @return the value of the property with the given name
   */
  Object getProperty(String propertyName) {
    if (_propertyMap == null) {
      return null;
    }
    return _propertyMap[propertyName];
  }

  /**
   * If the given child is not `null`, use the given visitor to visit it.
   *
   * @param child the child to be visited
   * @param visitor the visitor that will be used to visit the child
   */
  void safelyVisitChild(AstNode child, AstVisitor visitor) {
    if (child != null) {
      child.accept(visitor);
    }
  }

  /**
   * Set the value of the property with the given name to the given value. If the value is
   * `null`, the property will effectively be removed.
   *
   * @param propertyName the name of the property whose value is to be set
   * @param propertyValue the new value of the property
   */
  void setProperty(String propertyName, Object propertyValue) {
    if (propertyValue == null) {
      if (_propertyMap != null) {
        _propertyMap.remove(propertyName);
        if (_propertyMap.isEmpty) {
          _propertyMap = null;
        }
      }
    } else {
      if (_propertyMap == null) {
        _propertyMap = new HashMap<String, Object>();
      }
      _propertyMap[propertyName] = propertyValue;
    }
  }

  /**
   * Return a textual description of this node in a form approximating valid source. The returned
   * string will not be valid source primarily in the case where the node itself is not well-formed.
   *
   * @return the source code equivalent of this node
   */
  String toSource() {
    PrintStringWriter writer = new PrintStringWriter();
    accept(new ToSourceVisitor(writer));
    return writer.toString();
  }

  @override
  String toString() => toSource();

  /**
   * Use the given visitor to visit all of the children of this node. The children will be visited
   * in source order.
   *
   * @param visitor the visitor that will be used to visit the children of this node
   */
  void visitChildren(AstVisitor visitor);
}

/**
 * The interface `AstVisitor` defines the behavior of objects that can be used to visit an AST
 * structure.
 */
abstract class AstVisitor<R> {
  R visitAdjacentStrings(AdjacentStrings node);

  R visitAnnotation(Annotation node);

  R visitArgumentList(ArgumentList node);

  R visitAsExpression(AsExpression node);

  R visitAssertStatement(AssertStatement assertStatement);

  R visitAssignmentExpression(AssignmentExpression node);

  R visitAwaitExpression(AwaitExpression node);

  R visitBinaryExpression(BinaryExpression node);

  R visitBlock(Block node);

  R visitBlockFunctionBody(BlockFunctionBody node);

  R visitBooleanLiteral(BooleanLiteral node);

  R visitBreakStatement(BreakStatement node);

  R visitCascadeExpression(CascadeExpression node);

  R visitCatchClause(CatchClause node);

  R visitClassDeclaration(ClassDeclaration node);

  R visitClassTypeAlias(ClassTypeAlias node);

  R visitComment(Comment node);

  R visitCommentReference(CommentReference node);

  R visitCompilationUnit(CompilationUnit node);

  R visitConditionalExpression(ConditionalExpression node);

  R visitConstructorDeclaration(ConstructorDeclaration node);

  R visitConstructorFieldInitializer(ConstructorFieldInitializer node);

  R visitConstructorName(ConstructorName node);

  R visitContinueStatement(ContinueStatement node);

  R visitDeclaredIdentifier(DeclaredIdentifier node);

  R visitDefaultFormalParameter(DefaultFormalParameter node);

  R visitDoStatement(DoStatement node);

  R visitDoubleLiteral(DoubleLiteral node);

  R visitEmptyFunctionBody(EmptyFunctionBody node);

  R visitEmptyStatement(EmptyStatement node);

  R visitEnumConstantDeclaration(EnumConstantDeclaration node);

  R visitEnumDeclaration(EnumDeclaration node);

  R visitExportDirective(ExportDirective node);

  R visitExpressionFunctionBody(ExpressionFunctionBody node);

  R visitExpressionStatement(ExpressionStatement node);

  R visitExtendsClause(ExtendsClause node);

  R visitFieldDeclaration(FieldDeclaration node);

  R visitFieldFormalParameter(FieldFormalParameter node);

  R visitForEachStatement(ForEachStatement node);

  R visitFormalParameterList(FormalParameterList node);

  R visitForStatement(ForStatement node);

  R visitFunctionDeclaration(FunctionDeclaration node);

  R visitFunctionDeclarationStatement(FunctionDeclarationStatement node);

  R visitFunctionExpression(FunctionExpression node);

  R visitFunctionExpressionInvocation(FunctionExpressionInvocation node);

  R visitFunctionTypeAlias(FunctionTypeAlias functionTypeAlias);

  R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node);

  R visitHideCombinator(HideCombinator node);

  R visitIfStatement(IfStatement node);

  R visitImplementsClause(ImplementsClause node);

  R visitImportDirective(ImportDirective node);

  R visitIndexExpression(IndexExpression node);

  R visitInstanceCreationExpression(InstanceCreationExpression node);

  R visitIntegerLiteral(IntegerLiteral node);

  R visitInterpolationExpression(InterpolationExpression node);

  R visitInterpolationString(InterpolationString node);

  R visitIsExpression(IsExpression node);

  R visitLabel(Label node);

  R visitLabeledStatement(LabeledStatement node);

  R visitLibraryDirective(LibraryDirective node);

  R visitLibraryIdentifier(LibraryIdentifier node);

  R visitListLiteral(ListLiteral node);

  R visitMapLiteral(MapLiteral node);

  R visitMapLiteralEntry(MapLiteralEntry node);

  R visitMethodDeclaration(MethodDeclaration node);

  R visitMethodInvocation(MethodInvocation node);

  R visitNamedExpression(NamedExpression node);

  R visitNativeClause(NativeClause node);

  R visitNativeFunctionBody(NativeFunctionBody node);

  R visitNullLiteral(NullLiteral node);

  R visitParenthesizedExpression(ParenthesizedExpression node);

  R visitPartDirective(PartDirective node);

  R visitPartOfDirective(PartOfDirective node);

  R visitPostfixExpression(PostfixExpression node);

  R visitPrefixedIdentifier(PrefixedIdentifier node);

  R visitPrefixExpression(PrefixExpression node);

  R visitPropertyAccess(PropertyAccess node);

  R
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node);

  R visitRethrowExpression(RethrowExpression node);

  R visitReturnStatement(ReturnStatement node);

  R visitScriptTag(ScriptTag node);

  R visitShowCombinator(ShowCombinator node);

  R visitSimpleFormalParameter(SimpleFormalParameter node);

  R visitSimpleIdentifier(SimpleIdentifier node);

  R visitSimpleStringLiteral(SimpleStringLiteral node);

  R visitStringInterpolation(StringInterpolation node);

  R visitSuperConstructorInvocation(SuperConstructorInvocation node);

  R visitSuperExpression(SuperExpression node);

  R visitSwitchCase(SwitchCase node);

  R visitSwitchDefault(SwitchDefault node);

  R visitSwitchStatement(SwitchStatement node);

  R visitSymbolLiteral(SymbolLiteral node);

  R visitThisExpression(ThisExpression node);

  R visitThrowExpression(ThrowExpression node);

  R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node);

  R visitTryStatement(TryStatement node);

  R visitTypeArgumentList(TypeArgumentList node);

  R visitTypeName(TypeName node);

  R visitTypeParameter(TypeParameter node);

  R visitTypeParameterList(TypeParameterList node);

  R visitVariableDeclaration(VariableDeclaration node);

  R visitVariableDeclarationList(VariableDeclarationList node);

  R visitVariableDeclarationStatement(VariableDeclarationStatement node);

  R visitWhileStatement(WhileStatement node);

  R visitWithClause(WithClause node);

  R visitYieldStatement(YieldStatement node);
}

/**
 * Instances of the class `AwaitExpression` implement an await expression.
 */
class AwaitExpression extends Expression {
  /**
   * The 'await' keyword.
   */
  Token awaitKeyword;

  /**
   * The expression whose value is being waited on.
   */
  Expression _expression;

  /**
   * Initialize a newly created await expression.
   *
   * @param awaitKeyword the 'await' keyword
   * @param expression the expression whose value is being waited on
   */
  AwaitExpression(this.awaitKeyword, Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken {
    if (awaitKeyword != null) {
      return awaitKeyword;
    }
    return _expression.beginToken;
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(awaitKeyword)
      ..add(_expression);

  @override
  Token get endToken => _expression.endToken;

  /**
   * Return the expression whose value is being waited on.
   *
   * @return the expression whose value is being waited on
   */
  Expression get expression => _expression;

  /**
   * Set the expression whose value is being waited on to the given expression.
   *
   * @param expression the expression whose value is being waited on
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  int get precedence => 0;

  @override
  accept(AstVisitor visitor) => visitor.visitAwaitExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * Instances of the class `BinaryExpression` represent a binary (infix) expression.
 *
 * <pre>
 * binaryExpression ::=
 *     [Expression] [Token] [Expression]
 * </pre>
 */
class BinaryExpression extends Expression {
  /**
   * The expression used to compute the left operand.
   */
  Expression _leftOperand;

  /**
   * The binary operator being applied.
   */
  Token operator;

  /**
   * The expression used to compute the right operand.
   */
  Expression _rightOperand;

  /**
   * The element associated with the operator based on the static type of the left operand, or
   * `null` if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved.
   */
  MethodElement staticElement;

  /**
   * The element associated with the operator based on the propagated type of the left operand, or
   * `null` if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved.
   */
  MethodElement propagatedElement;

  /**
   * Initialize a newly created binary expression.
   *
   * @param leftOperand the expression used to compute the left operand
   * @param operator the binary operator being applied
   * @param rightOperand the expression used to compute the right operand
   */
  BinaryExpression(Expression leftOperand, this.operator,
      Expression rightOperand) {
    _leftOperand = becomeParentOf(leftOperand);
    _rightOperand = becomeParentOf(rightOperand);
  }

  @override
  Token get beginToken => _leftOperand.beginToken;

  /**
   * Return the best element available for this operator. If resolution was able to find a better
   * element based on type propagation, that element will be returned. Otherwise, the element found
   * using the result of static analysis will be returned. If resolution has not been performed,
   * then `null` will be returned.
   *
   * @return the best element available for this operator
   */
  MethodElement get bestElement {
    MethodElement element = propagatedElement;
    if (element == null) {
      element = staticElement;
    }
    return element;
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_leftOperand)
      ..add(operator)
      ..add(_rightOperand);

  @override
  Token get endToken => _rightOperand.endToken;

  /**
   * Return the expression used to compute the left operand.
   *
   * @return the expression used to compute the left operand
   */
  Expression get leftOperand => _leftOperand;

  /**
   * Set the expression used to compute the left operand to the given expression.
   *
   * @param expression the expression used to compute the left operand
   */
  void set leftOperand(Expression expression) {
    _leftOperand = becomeParentOf(expression);
  }

  @override
  int get precedence => operator.type.precedence;

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on
   * propagated type information, then return the parameter element representing the parameter to
   * which the value of the right operand will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.propagatedParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  ParameterElement get propagatedParameterElementForRightOperand {
    if (propagatedElement == null) {
      return null;
    }
    List<ParameterElement> parameters = propagatedElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  /**
   * Return the expression used to compute the right operand.
   *
   * @return the expression used to compute the right operand
   */
  Expression get rightOperand => _rightOperand;

  /**
   * Set the expression used to compute the right operand to the given expression.
   *
   * @param expression the expression used to compute the right operand
   */
  void set rightOperand(Expression expression) {
    _rightOperand = becomeParentOf(expression);
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on static
   * type information, then return the parameter element representing the parameter to which the
   * value of the right operand will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.staticParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  ParameterElement get staticParameterElementForRightOperand {
    if (staticElement == null) {
      return null;
    }
    List<ParameterElement> parameters = staticElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  @override
  accept(AstVisitor visitor) => visitor.visitBinaryExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_leftOperand, visitor);
    safelyVisitChild(_rightOperand, visitor);
  }
}

/**
 * Instances of the class `Block` represent a sequence of statements.
 *
 * <pre>
 * block ::=
 *     '{' statement* '}'
 * </pre>
 */
class Block extends Statement {
  /**
   * The left curly bracket.
   */
  Token leftBracket;

  /**
   * The statements contained in the block.
   */
  NodeList<Statement> _statements;

  /**
   * The right curly bracket.
   */
  Token rightBracket;

  /**
   * Initialize a newly created block of code.
   *
   * @param leftBracket the left curly bracket
   * @param statements the statements contained in the block
   * @param rightBracket the right curly bracket
   */
  Block(this.leftBracket, List<Statement> statements, this.rightBracket) {
    _statements = new NodeList<Statement>(this, statements);
  }

  @override
  Token get beginToken => leftBracket;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(leftBracket)
      ..addAll(_statements)
      ..add(rightBracket);

  @override
  Token get endToken => rightBracket;

  /**
   * Return the statements contained in the block.
   *
   * @return the statements contained in the block
   */
  NodeList<Statement> get statements => _statements;

  @override
  accept(AstVisitor visitor) => visitor.visitBlock(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _statements.accept(visitor);
  }
}

/**
 * Instances of the class `BlockFunctionBody` represent a function body that consists of a
 * block of statements.
 *
 * <pre>
 * blockFunctionBody ::=
 *     ('async' | 'async' '*' | 'sync' '*')? [Block]
 * </pre>
 */
class BlockFunctionBody extends FunctionBody {
  /**
   * The token representing the 'async' or 'sync' keyword, or `null` if there is no such
   * keyword.
   */
  Token keyword;

  /**
   * The star optionally following the 'async' or following the 'sync' keyword.
   */
  Token star;

  /**
   * The block representing the body of the function.
   */
  Block _block;

  /**
   * Initialize a newly created function body consisting of a block of statements.
   *
   * @param keyword the token representing the 'async' or 'sync' keyword
   * @param star the star following the 'async' or 'sync' keyword
   * @param block the block representing the body of the function
   */
  BlockFunctionBody(this.keyword, this.star, Block block) {
    _block = becomeParentOf(block);
  }

  @override
  Token get beginToken => _block.beginToken;

  /**
   * Return the block representing the body of the function.
   *
   * @return the block representing the body of the function
   */
  Block get block => _block;

  /**
   * Set the block representing the body of the function to the given block.
   *
   * @param block the block representing the body of the function
   */
  void set block(Block block) {
    _block = becomeParentOf(block);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(star)
      ..add(_block);

  @override
  Token get endToken => _block.endToken;

  @override
  bool get isAsynchronous {
    if (keyword == null) {
      return false;
    }
    String keywordValue = keyword.lexeme;
    return keywordValue == Parser.ASYNC;
  }

  @override
  bool get isGenerator => star != null;

  @override
  bool get isSynchronous => keyword == null || keyword.lexeme != Parser.ASYNC;

  @override
  accept(AstVisitor visitor) => visitor.visitBlockFunctionBody(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_block, visitor);
  }
}

/**
 * Instances of the class `BooleanLiteral` represent a boolean literal expression.
 *
 * <pre>
 * booleanLiteral ::=
 *     'false' | 'true'
 * </pre>
 */
class BooleanLiteral extends Literal {
  /**
   * The token representing the literal.
   */
  Token literal;

  /**
   * The value of the literal.
   */
  bool value = false;

  /**
   * Initialize a newly created boolean literal.
   *
   * @param literal the token representing the literal
   * @param value the value of the literal
   */
  BooleanLiteral(this.literal, this.value);

  @override
  Token get beginToken => literal;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(literal);

  @override
  Token get endToken => literal;

  @override
  bool get isSynthetic => literal.isSynthetic;

  @override
  accept(AstVisitor visitor) => visitor.visitBooleanLiteral(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `BreadthFirstVisitor` implement an AST visitor that will recursively
 * visit all of the nodes in an AST structure, similar to [GeneralizingAstVisitor]. This
 * visitor uses a breadth-first ordering rather than the depth-first ordering of
 * [GeneralizingAstVisitor].
 *
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general visit method. Failure to do so will cause the visit methods
 * for superclasses of the node to not be invoked and will cause the children of the visited node to
 * not be visited.
 *
 * In addition, subclasses should <b>not</b> explicitly visit the children of a node, but should
 * ensure that the method [visitNode] is used to visit the children (either directly
 * or indirectly). Failure to do will break the order in which nodes are visited.
 */
class BreadthFirstVisitor<R> extends GeneralizingAstVisitor<R> {
  /**
   * A queue holding the nodes that have not yet been visited in the order in which they ought to be
   * visited.
   */
  Queue<AstNode> _queue = new Queue<AstNode>();

  /**
   * A visitor, used to visit the children of the current node, that will add the nodes it visits to
   * the [queue].
   */
  GeneralizingAstVisitor<Object> _childVisitor;

  BreadthFirstVisitor() {
    _childVisitor = new GeneralizingAstVisitor_BreadthFirstVisitor(this);
  }

  /**
   * Visit all nodes in the tree starting at the given `root` node, in breadth-first order.
   *
   * @param root the root of the AST structure to be visited
   */
  void visitAllNodes(AstNode root) {
    _queue.add(root);
    while (!_queue.isEmpty) {
      AstNode next = _queue.removeFirst();
      next.accept(this);
    }
  }

  @override
  R visitNode(AstNode node) {
    node.visitChildren(_childVisitor);
    return null;
  }
}

/**
 * Instances of the class `BreakStatement` represent a break statement.
 *
 * <pre>
 * breakStatement ::=
 *     'break' [SimpleIdentifier]? ';'
 * </pre>
 */
class BreakStatement extends Statement {
  /**
   * The token representing the 'break' keyword.
   */
  Token keyword;

  /**
   * The label associated with the statement, or `null` if there is no label.
   */
  SimpleIdentifier _label;

  /**
   * The semicolon terminating the statement.
   */
  Token semicolon;

  /**
   * The AstNode which this break statement is breaking from.  This will be
   * either a Statement (in the case of breaking out of a loop) or a
   * SwitchMember (in the case of a labeled break statement whose label matches
   * a label on a switch case in an enclosing switch statement).  Null if the
   * AST has not yet been resolved or if the target could not be resolved.
   * Note that if the source code has errors, the target may be invalid (e.g.
   * trying to break to a switch case).
   */
  AstNode target;

  /**
   * Initialize a newly created break statement.
   *
   * @param keyword the token representing the 'break' keyword
   * @param label the label associated with the statement
   * @param semicolon the semicolon terminating the statement
   */
  BreakStatement(this.keyword, SimpleIdentifier label, this.semicolon) {
    _label = becomeParentOf(label);
  }

  @override
  Token get beginToken => keyword;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(_label)
      ..add(semicolon);

  @override
  Token get endToken => semicolon;

  /**
   * Return the label associated with the statement, or `null` if there is no label.
   *
   * @return the label associated with the statement
   */
  SimpleIdentifier get label => _label;

  /**
   * Set the label associated with the statement to the given identifier.
   *
   * @param identifier the label associated with the statement
   */
  void set label(SimpleIdentifier identifier) {
    _label = becomeParentOf(identifier);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitBreakStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_label, visitor);
  }
}

/**
 * Instances of the class `CascadeExpression` represent a sequence of cascaded expressions:
 * expressions that share a common target. There are three kinds of expressions that can be used in
 * a cascade expression: [IndexExpression], [MethodInvocation] and
 * [PropertyAccess].
 *
 * <pre>
 * cascadeExpression ::=
 *     [Expression] cascadeSection*
 *
 * cascadeSection ::=
 *     '..'  (cascadeSelector arguments*) (assignableSelector arguments*)* (assignmentOperator expressionWithoutCascade)?
 *
 * cascadeSelector ::=
 *     '[ ' expression '] '
 *   | identifier
 * </pre>
 */
class CascadeExpression extends Expression {
  /**
   * The target of the cascade sections.
   */
  Expression _target;

  /**
   * The cascade sections sharing the common target.
   */
  NodeList<Expression> _cascadeSections;

  /**
   * Initialize a newly created cascade expression.
   *
   * @param target the target of the cascade sections
   * @param cascadeSections the cascade sections sharing the common target
   */
  CascadeExpression(Expression target, List<Expression> cascadeSections) {
    _target = becomeParentOf(target);
    _cascadeSections = new NodeList<Expression>(this, cascadeSections);
  }

  @override
  Token get beginToken => _target.beginToken;

  /**
   * Return the cascade sections sharing the common target.
   *
   * @return the cascade sections sharing the common target
   */
  NodeList<Expression> get cascadeSections => _cascadeSections;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_target)
      ..addAll(_cascadeSections);

  @override
  Token get endToken => _cascadeSections.endToken;

  @override
  int get precedence => 2;

  /**
   * Return the target of the cascade sections.
   *
   * @return the target of the cascade sections
   */
  Expression get target => _target;

  /**
   * Set the target of the cascade sections to the given expression.
   *
   * @param target the target of the cascade sections
   */
  void set target(Expression target) {
    _target = becomeParentOf(target);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitCascadeExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_target, visitor);
    _cascadeSections.accept(visitor);
  }
}

/**
 * Instances of the class `CatchClause` represent a catch clause within a try statement.
 *
 * <pre>
 * onPart ::=
 *     catchPart [Block]
 *   | 'on' type catchPart? [Block]
 *
 * catchPart ::=
 *     'catch' '(' [SimpleIdentifier] (',' [SimpleIdentifier])? ')'
 * </pre>
 */
class CatchClause extends AstNode {
  /**
   * The token representing the 'on' keyword, or `null` if there is no 'on' keyword.
   */
  Token onKeyword;

  /**
   * The type of exceptions caught by this catch clause, or `null` if this catch clause
   * catches every type of exception.
   */
  TypeName _exceptionType;

  /**
   * The token representing the 'catch' keyword, or `null` if there is no 'catch' keyword.
   */
  Token catchKeyword;

  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The parameter whose value will be the exception that was thrown.
   */
  SimpleIdentifier _exceptionParameter;

  /**
   * The comma separating the exception parameter from the stack trace parameter, or `null` if
   * there is no stack trace parameter.
   */
  Token comma;

  /**
   * The parameter whose value will be the stack trace associated with the exception, or
   * `null` if there is no stack trace parameter.
   */
  SimpleIdentifier _stackTraceParameter;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * The body of the catch block.
   */
  Block _body;

  /**
   * Initialize a newly created catch clause.
   *
   * @param onKeyword the token representing the 'on' keyword
   * @param exceptionType the type of exceptions caught by this catch clause
   * @param leftParenthesis the left parenthesis
   * @param exceptionParameter the parameter whose value will be the exception that was thrown
   * @param comma the comma separating the exception parameter from the stack trace parameter
   * @param stackTraceParameter the parameter whose value will be the stack trace associated with
   *          the exception
   * @param rightParenthesis the right parenthesis
   * @param body the body of the catch block
   */
  CatchClause(this.onKeyword, TypeName exceptionType, this.catchKeyword,
      this.leftParenthesis, SimpleIdentifier exceptionParameter, this.comma,
      SimpleIdentifier stackTraceParameter, this.rightParenthesis, Block body) {
    _exceptionType = becomeParentOf(exceptionType);
    _exceptionParameter = becomeParentOf(exceptionParameter);
    _stackTraceParameter = becomeParentOf(stackTraceParameter);
    _body = becomeParentOf(body);
  }

  @override
  Token get beginToken {
    if (onKeyword != null) {
      return onKeyword;
    }
    return catchKeyword;
  }

  /**
   * Return the body of the catch block.
   *
   * @return the body of the catch block
   */
  Block get body => _body;

  /**
   * Set the body of the catch block to the given block.
   *
   * @param block the body of the catch block
   */
  void set body(Block block) {
    _body = becomeParentOf(block);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(onKeyword)
      ..add(_exceptionType)
      ..add(catchKeyword)
      ..add(leftParenthesis)
      ..add(_exceptionParameter)
      ..add(comma)
      ..add(_stackTraceParameter)
      ..add(rightParenthesis)
      ..add(_body);

  @override
  Token get endToken => _body.endToken;

  /**
   * Return the parameter whose value will be the exception that was thrown.
   *
   * @return the parameter whose value will be the exception that was thrown
   */
  SimpleIdentifier get exceptionParameter => _exceptionParameter;

  /**
   * Set the parameter whose value will be the exception that was thrown to the given parameter.
   *
   * @param parameter the parameter whose value will be the exception that was thrown
   */
  void set exceptionParameter(SimpleIdentifier parameter) {
    _exceptionParameter = becomeParentOf(parameter);
  }

  /**
   * Return the type of exceptions caught by this catch clause, or `null` if this catch clause
   * catches every type of exception.
   *
   * @return the type of exceptions caught by this catch clause
   */
  TypeName get exceptionType => _exceptionType;

  /**
   * Set the type of exceptions caught by this catch clause to the given type.
   *
   * @param exceptionType the type of exceptions caught by this catch clause
   */
  void set exceptionType(TypeName exceptionType) {
    _exceptionType = becomeParentOf(exceptionType);
  }

  /**
   * Return the parameter whose value will be the stack trace associated with the exception, or
   * `null` if there is no stack trace parameter.
   *
   * @return the parameter whose value will be the stack trace associated with the exception
   */
  SimpleIdentifier get stackTraceParameter => _stackTraceParameter;

  /**
   * Set the parameter whose value will be the stack trace associated with the exception to the
   * given parameter.
   *
   * @param parameter the parameter whose value will be the stack trace associated with the
   *          exception
   */
  void set stackTraceParameter(SimpleIdentifier parameter) {
    _stackTraceParameter = becomeParentOf(parameter);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitCatchClause(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_exceptionType, visitor);
    safelyVisitChild(_exceptionParameter, visitor);
    safelyVisitChild(_stackTraceParameter, visitor);
    safelyVisitChild(_body, visitor);
  }
}

/**
 * Helper class to allow iteration of child entities of an AST node.
 */
class ChildEntities extends Object with IterableMixin implements Iterable {
  List _entities = [];

  @override
  Iterator get iterator => _entities.iterator;

  /**
   * Add an AST node or token as the next child entity, if it is not null.
   */
  void add(entity) {
    if (entity != null) {
      assert(entity is Token || entity is AstNode);
      _entities.add(entity);
    }
  }

  /**
   * Add the given items as the next child entities, if [items] is not null.
   */
  void addAll(Iterable items) {
    if (items != null) {
      _entities.addAll(items);
    }
  }
}

/**
 * Instances of the class `ClassDeclaration` represent the declaration of a class.
 *
 * <pre>
 * classDeclaration ::=
 *     'abstract'? 'class' [SimpleIdentifier] [TypeParameterList]?
 *     ([ExtendsClause] [WithClause]?)?
 *     [ImplementsClause]?
 *     '{' [ClassMember]* '}'
 * </pre>
 */
class ClassDeclaration extends CompilationUnitMember {
  /**
   * The 'abstract' keyword, or `null` if the keyword was absent.
   */
  Token abstractKeyword;

  /**
   * The token representing the 'class' keyword.
   */
  Token classKeyword;

  /**
   * The name of the class being declared.
   */
  SimpleIdentifier _name;

  /**
   * The type parameters for the class, or `null` if the class does not have any type
   * parameters.
   */
  TypeParameterList _typeParameters;

  /**
   * The extends clause for the class, or `null` if the class does not extend any other class.
   */
  ExtendsClause _extendsClause;

  /**
   * The with clause for the class, or `null` if the class does not have a with clause.
   */
  WithClause _withClause;

  /**
   * The implements clause for the class, or `null` if the class does not implement any
   * interfaces.
   */
  ImplementsClause _implementsClause;

  /**
   * The native clause for the class, or `null` if the class does not have a native clause.
   */
  NativeClause _nativeClause;

  /**
   * The left curly bracket.
   */
  Token leftBracket;

  /**
   * The members defined by the class.
   */
  NodeList<ClassMember> _members;

  /**
   * The right curly bracket.
   */
  Token rightBracket;

  /**
   * Initialize a newly created class declaration.
   *
   * @param comment the documentation comment associated with this class
   * @param metadata the annotations associated with this class
   * @param abstractKeyword the 'abstract' keyword, or `null` if the keyword was absent
   * @param classKeyword the token representing the 'class' keyword
   * @param name the name of the class being declared
   * @param typeParameters the type parameters for the class
   * @param extendsClause the extends clause for the class
   * @param withClause the with clause for the class
   * @param implementsClause the implements clause for the class
   * @param leftBracket the left curly bracket
   * @param members the members defined by the class
   * @param rightBracket the right curly bracket
   */
  ClassDeclaration(Comment comment, List<Annotation> metadata,
      this.abstractKeyword, this.classKeyword, SimpleIdentifier name,
      TypeParameterList typeParameters, ExtendsClause extendsClause,
      WithClause withClause, ImplementsClause implementsClause, this.leftBracket,
      List<ClassMember> members, this.rightBracket)
      : super(comment, metadata) {
    _name = becomeParentOf(name);
    _typeParameters = becomeParentOf(typeParameters);
    _extendsClause = becomeParentOf(extendsClause);
    _withClause = becomeParentOf(withClause);
    _implementsClause = becomeParentOf(implementsClause);
    _members = new NodeList<ClassMember>(this, members);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(abstractKeyword)
      ..add(classKeyword)
      ..add(_name)
      ..add(_typeParameters)
      ..add(_extendsClause)
      ..add(_withClause)
      ..add(_implementsClause)
      ..add(_nativeClause)
      ..add(leftBracket)
      ..addAll(members)
      ..add(rightBracket);

  @override
  ClassElement get element =>
      _name != null ? (_name.staticElement as ClassElement) : null;

  @override
  Token get endToken => rightBracket;

  /**
   * Return the extends clause for this class, or `null` if the class does not extend any
   * other class.
   *
   * @return the extends clause for this class
   */
  ExtendsClause get extendsClause => _extendsClause;

  /**
   * Set the extends clause for this class to the given clause.
   *
   * @param extendsClause the extends clause for this class
   */
  void set extendsClause(ExtendsClause extendsClause) {
    _extendsClause = becomeParentOf(extendsClause);
  }

  @override
  Token get firstTokenAfterCommentAndMetadata {
    if (abstractKeyword != null) {
      return abstractKeyword;
    }
    return classKeyword;
  }

  /**
   * Return the implements clause for the class, or `null` if the class does not implement any
   * interfaces.
   *
   * @return the implements clause for the class
   */
  ImplementsClause get implementsClause => _implementsClause;

  /**
   * Set the implements clause for the class to the given clause.
   *
   * @param implementsClause the implements clause for the class
   */
  void set implementsClause(ImplementsClause implementsClause) {
    _implementsClause = becomeParentOf(implementsClause);
  }

  /**
   * Return `true` if this class is declared to be an abstract class.
   *
   * @return `true` if this class is declared to be an abstract class
   */
  bool get isAbstract => abstractKeyword != null;

  /**
   * Return the members defined by the class.
   *
   * @return the members defined by the class
   */
  NodeList<ClassMember> get members => _members;

  /**
   * Return the name of the class being declared.
   *
   * @return the name of the class being declared
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the class being declared to the given identifier.
   *
   * @param identifier the name of the class being declared
   */
  void set name(SimpleIdentifier identifier) {
    _name = becomeParentOf(identifier);
  }

  /**
   * Return the native clause for this class, or `null` if the class does not have a native
   * cluse.
   *
   * @return the native clause for this class
   */
  NativeClause get nativeClause => _nativeClause;

  /**
   * Set the native clause for this class to the given clause.
   *
   * @param nativeClause the native clause for this class
   */
  void set nativeClause(NativeClause nativeClause) {
    _nativeClause = becomeParentOf(nativeClause);
  }

  /**
   * Return the type parameters for the class, or `null` if the class does not have any type
   * parameters.
   *
   * @return the type parameters for the class
   */
  TypeParameterList get typeParameters => _typeParameters;

  /**
   * Set the type parameters for the class to the given list of type parameters.
   *
   * @param typeParameters the type parameters for the class
   */
  void set typeParameters(TypeParameterList typeParameters) {
    _typeParameters = becomeParentOf(typeParameters);
  }

  /**
   * Return the with clause for the class, or `null` if the class does not have a with clause.
   *
   * @return the with clause for the class
   */
  WithClause get withClause => _withClause;

  /**
   * Set the with clause for the class to the given clause.
   *
   * @param withClause the with clause for the class
   */
  void set withClause(WithClause withClause) {
    _withClause = becomeParentOf(withClause);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitClassDeclaration(this);

  /**
   * Return the constructor declared in the class with the given name.
   *
   * @param name the name of the constructor to find, `null` for default
   * @return the found constructor or `null` if not found
   */
  ConstructorDeclaration getConstructor(String name) {
    for (ClassMember classMember in _members) {
      if (classMember is ConstructorDeclaration) {
        ConstructorDeclaration constructor = classMember;
        SimpleIdentifier constructorName = constructor.name;
        if (name == null && constructorName == null) {
          return constructor;
        }
        if (constructorName != null && constructorName.name == name) {
          return constructor;
        }
      }
    }
    return null;
  }

  /**
   * Return the field declared in the class with the given name.
   *
   * @param name the name of the field to find
   * @return the found field or `null` if not found
   */
  VariableDeclaration getField(String name) {
    for (ClassMember classMember in _members) {
      if (classMember is FieldDeclaration) {
        FieldDeclaration fieldDeclaration = classMember;
        NodeList<VariableDeclaration> fields =
            fieldDeclaration.fields.variables;
        for (VariableDeclaration field in fields) {
          SimpleIdentifier fieldName = field.name;
          if (fieldName != null && name == fieldName.name) {
            return field;
          }
        }
      }
    }
    return null;
  }

  /**
   * Return the method declared in the class with the given name.
   *
   * @param name the name of the method to find
   * @return the found method or `null` if not found
   */
  MethodDeclaration getMethod(String name) {
    for (ClassMember classMember in _members) {
      if (classMember is MethodDeclaration) {
        MethodDeclaration method = classMember;
        SimpleIdentifier methodName = method.name;
        if (methodName != null && name == methodName.name) {
          return method;
        }
      }
    }
    return null;
  }

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_typeParameters, visitor);
    safelyVisitChild(_extendsClause, visitor);
    safelyVisitChild(_withClause, visitor);
    safelyVisitChild(_implementsClause, visitor);
    safelyVisitChild(_nativeClause, visitor);
    members.accept(visitor);
  }
}

/**
 * The abstract class `ClassMember` defines the behavior common to nodes that declare a name
 * within the scope of a class.
 */
abstract class ClassMember extends Declaration {
  /**
   * Initialize a newly created member of a class.
   *
   * @param comment the documentation comment associated with this member
   * @param metadata the annotations associated with this member
   */
  ClassMember(Comment comment, List<Annotation> metadata)
      : super(comment, metadata);
}

/**
 * Instances of the class `ClassTypeAlias` represent a class type alias.
 *
 * <pre>
 * classTypeAlias ::=
 *     [SimpleIdentifier] [TypeParameterList]? '=' 'abstract'? mixinApplication
 *
 * mixinApplication ::=
 *     [TypeName] [WithClause] [ImplementsClause]? ';'
 * </pre>
 */
class ClassTypeAlias extends TypeAlias {
  /**
   * The name of the class being declared.
   */
  SimpleIdentifier _name;

  /**
   * The type parameters for the class, or `null` if the class does not have any type
   * parameters.
   */
  TypeParameterList _typeParameters;

  /**
   * The token for the '=' separating the name from the definition.
   */
  Token equals;

  /**
   * The token for the 'abstract' keyword, or `null` if this is not defining an abstract
   * class.
   */
  Token abstractKeyword;

  /**
   * The name of the superclass of the class being declared.
   */
  TypeName _superclass;

  /**
   * The with clause for this class.
   */
  WithClause _withClause;

  /**
   * The implements clause for this class, or `null` if there is no implements clause.
   */
  ImplementsClause _implementsClause;

  /**
   * Initialize a newly created class type alias.
   *
   * @param comment the documentation comment associated with this type alias
   * @param metadata the annotations associated with this type alias
   * @param keyword the token representing the 'typedef' keyword
   * @param name the name of the class being declared
   * @param typeParameters the type parameters for the class
   * @param equals the token for the '=' separating the name from the definition
   * @param abstractKeyword the token for the 'abstract' keyword
   * @param superclass the name of the superclass of the class being declared
   * @param withClause the with clause for this class
   * @param implementsClause the implements clause for this class
   * @param semicolon the semicolon terminating the declaration
   */
  ClassTypeAlias(Comment comment, List<Annotation> metadata, Token keyword,
      SimpleIdentifier name, TypeParameterList typeParameters, this.equals,
      this.abstractKeyword, TypeName superclass, WithClause withClause,
      ImplementsClause implementsClause, Token semicolon)
      : super(comment, metadata, keyword, semicolon) {
    _name = becomeParentOf(name);
    _typeParameters = becomeParentOf(typeParameters);
    _superclass = becomeParentOf(superclass);
    _withClause = becomeParentOf(withClause);
    _implementsClause = becomeParentOf(implementsClause);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(keyword)
      ..add(_name)
      ..add(_typeParameters)
      ..add(equals)
      ..add(abstractKeyword)
      ..add(_superclass)
      ..add(_withClause)
      ..add(_implementsClause)
      ..add(semicolon);

  @override
  ClassElement get element =>
      _name != null ? (_name.staticElement as ClassElement) : null;

  /**
   * Return the implements clause for this class, or `null` if there is no implements clause.
   *
   * @return the implements clause for this class
   */
  ImplementsClause get implementsClause => _implementsClause;

  /**
   * Set the implements clause for this class to the given implements clause.
   *
   * @param implementsClause the implements clause for this class
   */
  void set implementsClause(ImplementsClause implementsClause) {
    _implementsClause = becomeParentOf(implementsClause);
  }

  /**
   * Return `true` if this class is declared to be an abstract class.
   *
   * @return `true` if this class is declared to be an abstract class
   */
  bool get isAbstract => abstractKeyword != null;

  /**
   * Return the name of the class being declared.
   *
   * @return the name of the class being declared
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the class being declared to the given identifier.
   *
   * @param name the name of the class being declared
   */
  void set name(SimpleIdentifier name) {
    _name = becomeParentOf(name);
  }

  /**
   * Return the name of the superclass of the class being declared.
   *
   * @return the name of the superclass of the class being declared
   */
  TypeName get superclass => _superclass;

  /**
   * Set the name of the superclass of the class being declared to the given name.
   *
   * @param superclass the name of the superclass of the class being declared
   */
  void set superclass(TypeName superclass) {
    _superclass = becomeParentOf(superclass);
  }

  /**
   * Return the type parameters for the class, or `null` if the class does not have any type
   * parameters.
   *
   * @return the type parameters for the class
   */
  TypeParameterList get typeParameters => _typeParameters;

  /**
   * Set the type parameters for the class to the given list of parameters.
   *
   * @param typeParameters the type parameters for the class
   */
  void set typeParameters(TypeParameterList typeParameters) {
    _typeParameters = becomeParentOf(typeParameters);
  }

  /**
   * Return the with clause for this class.
   *
   * @return the with clause for this class
   */
  WithClause get withClause => _withClause;

  /**
   * Set the with clause for this class to the given with clause.
   *
   * @param withClause the with clause for this class
   */
  void set withClause(WithClause withClause) {
    _withClause = becomeParentOf(withClause);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitClassTypeAlias(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_typeParameters, visitor);
    safelyVisitChild(_superclass, visitor);
    safelyVisitChild(_withClause, visitor);
    safelyVisitChild(_implementsClause, visitor);
  }
}

/**
 * Instances of the class `Combinator` represent the combinator associated with an import
 * directive.
 *
 * <pre>
 * combinator ::=
 *     [HideCombinator]
 *   | [ShowCombinator]
 * </pre>
 */
abstract class Combinator extends AstNode {
  /**
   * The keyword specifying what kind of processing is to be done on the imported names.
   */
  Token keyword;

  /**
   * Initialize a newly created import combinator.
   *
   * @param keyword the keyword specifying what kind of processing is to be done on the imported
   *          names
   */
  Combinator(this.keyword);

  @override
  Token get beginToken => keyword;
}

/**
 * Instances of the class `Comment` represent a comment within the source code.
 *
 * <pre>
 * comment ::=
 *     endOfLineComment
 *   | blockComment
 *   | documentationComment
 *
 * endOfLineComment ::=
 *     '//' (CHARACTER - EOL)* EOL
 *
 * blockComment ::=
 *     '/ *' CHARACTER* '&#42;/'
 *
 * documentationComment ::=
 *     '/ **' (CHARACTER | [CommentReference])* '&#42;/'
 *   | ('///' (CHARACTER - EOL)* EOL)+
 * </pre>
 */
class Comment extends AstNode {
  /**
   * The tokens representing the comment.
   */
  final List<Token> tokens;

  /**
   * The type of the comment.
   */
  final CommentType _type;

  /**
   * The references embedded within the documentation comment. This list will be empty unless this
   * is a documentation comment that has references embedded within it.
   */
  NodeList<CommentReference> _references;

  /**
   * Initialize a newly created comment.
   *
   * @param tokens the tokens representing the comment
   * @param type the type of the comment
   * @param references the references embedded within the documentation comment
   */
  Comment(this.tokens, this._type, List<CommentReference> references) {
    _references = new NodeList<CommentReference>(this, references);
  }

  @override
  Token get beginToken => tokens[0];

  @override
  Iterable get childEntities => new ChildEntities()..addAll(tokens);

  @override
  Token get endToken => tokens[tokens.length - 1];

  /**
   * Return `true` if this is a block comment.
   *
   * @return `true` if this is a block comment
   */
  bool get isBlock => _type == CommentType.BLOCK;

  /**
   * Return `true` if this is a documentation comment.
   *
   * @return `true` if this is a documentation comment
   */
  bool get isDocumentation => _type == CommentType.DOCUMENTATION;

  /**
   * Return `true` if this is an end-of-line comment.
   *
   * @return `true` if this is an end-of-line comment
   */
  bool get isEndOfLine => _type == CommentType.END_OF_LINE;

  /**
   * Return the references embedded within the documentation comment.
   *
   * @return the references embedded within the documentation comment
   */
  NodeList<CommentReference> get references => _references;

  @override
  accept(AstVisitor visitor) => visitor.visitComment(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _references.accept(visitor);
  }

  /**
   * Create a block comment.
   *
   * @param tokens the tokens representing the comment
   * @return the block comment that was created
   */
  static Comment createBlockComment(List<Token> tokens) =>
      new Comment(tokens, CommentType.BLOCK, null);

  /**
   * Create a documentation comment.
   *
   * @param tokens the tokens representing the comment
   * @return the documentation comment that was created
   */
  static Comment createDocumentationComment(List<Token> tokens) =>
      new Comment(tokens, CommentType.DOCUMENTATION, new List<CommentReference>());

  /**
   * Create a documentation comment.
   *
   * @param tokens the tokens representing the comment
   * @param references the references embedded within the documentation comment
   * @return the documentation comment that was created
   */
  static Comment createDocumentationCommentWithReferences(List<Token> tokens,
      List<CommentReference> references) =>
      new Comment(tokens, CommentType.DOCUMENTATION, references);

  /**
   * Create an end-of-line comment.
   *
   * @param tokens the tokens representing the comment
   * @return the end-of-line comment that was created
   */
  static Comment createEndOfLineComment(List<Token> tokens) =>
      new Comment(tokens, CommentType.END_OF_LINE, null);
}

/**
 * Instances of the class `CommentReference` represent a reference to a Dart element that is
 * found within a documentation comment.
 *
 * <pre>
 * commentReference ::=
 *     '[' 'new'? [Identifier] ']'
 * </pre>
 */
class CommentReference extends AstNode {
  /**
   * The token representing the 'new' keyword, or `null` if there was no 'new' keyword.
   */
  Token newKeyword;

  /**
   * The identifier being referenced.
   */
  Identifier _identifier;

  /**
   * Initialize a newly created reference to a Dart element.
   *
   * @param newKeyword the token representing the 'new' keyword
   * @param identifier the identifier being referenced
   */
  CommentReference(this.newKeyword, Identifier identifier) {
    _identifier = becomeParentOf(identifier);
  }

  @override
  Token get beginToken => _identifier.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(newKeyword)
      ..add(_identifier);

  @override
  Token get endToken => _identifier.endToken;

  /**
   * Return the identifier being referenced.
   *
   * @return the identifier being referenced
   */
  Identifier get identifier => _identifier;

  /**
   * Set the identifier being referenced to the given identifier.
   *
   * @param identifier the identifier being referenced
   */
  void set identifier(Identifier identifier) {
    _identifier = becomeParentOf(identifier);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitCommentReference(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_identifier, visitor);
  }
}

/**
 * The enumeration `CommentType` encodes all the different types of comments
 * that are recognized by the parser.
 */
class CommentType {
  /**
   * A block comment.
   */
  static const CommentType BLOCK = const CommentType('BLOCK');

  /**
   * A documentation comment.
   */
  static const CommentType DOCUMENTATION = const CommentType('DOCUMENTATION');

  /**
   * An end-of-line comment.
   */
  static const CommentType END_OF_LINE = const CommentType('END_OF_LINE');

  /**
   * The name of the comment type.
   */
  final String name;

  /**
   * Initialize a newly created comment type to have the given [name].
   */
  const CommentType(this.name);

  @override
  String toString() => name;
}

/**
 * Instances of the class `CompilationUnit` represent a compilation unit.
 *
 * While the grammar restricts the order of the directives and declarations within a compilation
 * unit, this class does not enforce those restrictions. In particular, the children of a
 * compilation unit will be visited in lexical order even if lexical order does not conform to the
 * restrictions of the grammar.
 *
 * <pre>
 * compilationUnit ::=
 *     directives declarations
 *
 * directives ::=
 *     [ScriptTag]? [LibraryDirective]? namespaceDirective* [PartDirective]*
 *   | [PartOfDirective]
 *
 * namespaceDirective ::=
 *     [ImportDirective]
 *   | [ExportDirective]
 *
 * declarations ::=
 *     [CompilationUnitMember]*
 * </pre>
 */
class CompilationUnit extends AstNode {
  /**
   * The first token in the token stream that was parsed to form this compilation unit.
   */
  Token beginToken;

  /**
   * The script tag at the beginning of the compilation unit, or `null` if there is no script
   * tag in this compilation unit.
   */
  ScriptTag _scriptTag;

  /**
   * The directives contained in this compilation unit.
   */
  NodeList<Directive> _directives;

  /**
   * The declarations contained in this compilation unit.
   */
  NodeList<CompilationUnitMember> _declarations;

  /**
   * The last token in the token stream that was parsed to form this compilation unit. This token
   * should always have a type of [TokenType.EOF].
   */
  final Token endToken;

  /**
   * The element associated with this compilation unit, or `null` if the AST structure has not
   * been resolved.
   */
  CompilationUnitElement element;

  /**
   * The line information for this compilation unit.
   */
  LineInfo lineInfo;

  /**
   * Initialize a newly created compilation unit to have the given directives and declarations.
   *
   * @param beginToken the first token in the token stream
   * @param scriptTag the script tag at the beginning of the compilation unit
   * @param directives the directives contained in this compilation unit
   * @param declarations the declarations contained in this compilation unit
   * @param endToken the last token in the token stream
   */
  CompilationUnit(this.beginToken, ScriptTag scriptTag,
      List<Directive> directives, List<CompilationUnitMember> declarations,
      this.endToken) {
    _scriptTag = becomeParentOf(scriptTag);
    _directives = new NodeList<Directive>(this, directives);
    _declarations = new NodeList<CompilationUnitMember>(this, declarations);
  }

  @override
  Iterable get childEntities {
    ChildEntities result = new ChildEntities()..add(_scriptTag);
    if (_directivesAreBeforeDeclarations()) {
      result
          ..addAll(_directives)
          ..addAll(_declarations);
    } else {
      result.addAll(sortedDirectivesAndDeclarations);
    }
    return result;
  }

  /**
   * Return the declarations contained in this compilation unit.
   *
   * @return the declarations contained in this compilation unit
   */
  NodeList<CompilationUnitMember> get declarations => _declarations;

  /**
   * Return the directives contained in this compilation unit.
   *
   * @return the directives contained in this compilation unit
   */
  NodeList<Directive> get directives => _directives;

  @override
  int get length {
    Token endToken = this.endToken;
    if (endToken == null) {
      return 0;
    }
    return endToken.offset + endToken.length;
  }

  @override
  int get offset => 0;

  /**
   * Return the script tag at the beginning of the compilation unit, or `null` if there is no
   * script tag in this compilation unit.
   *
   * @return the script tag at the beginning of the compilation unit
   */
  ScriptTag get scriptTag => _scriptTag;

  /**
   * Set the script tag at the beginning of the compilation unit to the given script tag.
   *
   * @param scriptTag the script tag at the beginning of the compilation unit
   */
  void set scriptTag(ScriptTag scriptTag) {
    _scriptTag = becomeParentOf(scriptTag);
  }

  /**
   * Return an array containing all of the directives and declarations in this compilation unit,
   * sorted in lexical order.
   *
   * @return the directives and declarations in this compilation unit in the order in which they
   *         appeared in the original source
   */
  List<AstNode> get sortedDirectivesAndDeclarations {
    return <AstNode>[]
        ..addAll(_directives)
        ..addAll(_declarations)
        ..sort(AstNode.LEXICAL_ORDER);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitCompilationUnit(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_scriptTag, visitor);
    if (_directivesAreBeforeDeclarations()) {
      _directives.accept(visitor);
      _declarations.accept(visitor);
    } else {
      for (AstNode child in sortedDirectivesAndDeclarations) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Return `true` if all of the directives are lexically before any declarations.
   *
   * @return `true` if all of the directives are lexically before any declarations
   */
  bool _directivesAreBeforeDeclarations() {
    if (_directives.isEmpty || _declarations.isEmpty) {
      return true;
    }
    Directive lastDirective = _directives[_directives.length - 1];
    CompilationUnitMember firstDeclaration = _declarations[0];
    return lastDirective.offset < firstDeclaration.offset;
  }
}

/**
 * Instances of the class `CompilationUnitMember` defines the behavior common to nodes that
 * declare a name within the scope of a compilation unit.
 *
 * <pre>
 * compilationUnitMember ::=
 *     [ClassDeclaration]
 *   | [TypeAlias]
 *   | [FunctionDeclaration]
 *   | [MethodDeclaration]
 *   | [VariableDeclaration]
 *   | [VariableDeclaration]
 * </pre>
 */
abstract class CompilationUnitMember extends Declaration {
  /**
   * Initialize a newly created generic compilation unit member.
   *
   * @param comment the documentation comment associated with this member
   * @param metadata the annotations associated with this member
   */
  CompilationUnitMember(Comment comment, List<Annotation> metadata)
      : super(comment, metadata);
}

/**
 * Instances of the class `ConditionalExpression` represent a conditional expression.
 *
 * <pre>
 * conditionalExpression ::=
 *     [Expression] '?' [Expression] ':' [Expression]
 * </pre>
 */
class ConditionalExpression extends Expression {
  /**
   * The condition used to determine which of the expressions is executed next.
   */
  Expression _condition;

  /**
   * The token used to separate the condition from the then expression.
   */
  Token question;

  /**
   * The expression that is executed if the condition evaluates to `true`.
   */
  Expression _thenExpression;

  /**
   * The token used to separate the then expression from the else expression.
   */
  Token colon;

  /**
   * The expression that is executed if the condition evaluates to `false`.
   */
  Expression _elseExpression;

  /**
   * Initialize a newly created conditional expression.
   *
   * @param condition the condition used to determine which expression is executed next
   * @param question the token used to separate the condition from the then expression
   * @param thenExpression the expression that is executed if the condition evaluates to
   *          `true`
   * @param colon the token used to separate the then expression from the else expression
   * @param elseExpression the expression that is executed if the condition evaluates to
   *          `false`
   */
  ConditionalExpression(Expression condition, this.question,
      Expression thenExpression, this.colon, Expression elseExpression) {
    _condition = becomeParentOf(condition);
    _thenExpression = becomeParentOf(thenExpression);
    _elseExpression = becomeParentOf(elseExpression);
  }

  @override
  Token get beginToken => _condition.beginToken;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_condition)
      ..add(question)
      ..add(_thenExpression)
      ..add(colon)
      ..add(_elseExpression);

  /**
   * Return the condition used to determine which of the expressions is executed next.
   *
   * @return the condition used to determine which expression is executed next
   */
  Expression get condition => _condition;

  /**
   * Set the condition used to determine which of the expressions is executed next to the given
   * expression.
   *
   * @param expression the condition used to determine which expression is executed next
   */
  void set condition(Expression expression) {
    _condition = becomeParentOf(expression);
  }

  /**
   * Return the expression that is executed if the condition evaluates to `false`.
   *
   * @return the expression that is executed if the condition evaluates to `false`
   */
  Expression get elseExpression => _elseExpression;

  /**
   * Set the expression that is executed if the condition evaluates to `false` to the given
   * expression.
   *
   * @param expression the expression that is executed if the condition evaluates to `false`
   */
  void set elseExpression(Expression expression) {
    _elseExpression = becomeParentOf(expression);
  }

  @override
  Token get endToken => _elseExpression.endToken;

  @override
  int get precedence => 3;

  /**
   * Return the expression that is executed if the condition evaluates to `true`.
   *
   * @return the expression that is executed if the condition evaluates to `true`
   */
  Expression get thenExpression => _thenExpression;

  /**
   * Set the expression that is executed if the condition evaluates to `true` to the given
   * expression.
   *
   * @param expression the expression that is executed if the condition evaluates to `true`
   */
  void set thenExpression(Expression expression) {
    _thenExpression = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitConditionalExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_condition, visitor);
    safelyVisitChild(_thenExpression, visitor);
    safelyVisitChild(_elseExpression, visitor);
  }
}

/**
 * Instances of the class `ConstantEvaluator` evaluate constant expressions to produce their
 * compile-time value. According to the Dart Language Specification: <blockquote> A constant
 * expression is one of the following:
 * * A literal number.
 * * A literal boolean.
 * * A literal string where any interpolated expression is a compile-time constant that evaluates
 * to a numeric, string or boolean value or to `null`.
 * * `null`.
 * * A reference to a static constant variable.
 * * An identifier expression that denotes a constant variable, a class or a type parameter.
 * * A constant constructor invocation.
 * * A constant list literal.
 * * A constant map literal.
 * * A simple or qualified identifier denoting a top-level function or a static method.
 * * A parenthesized expression `(e)` where `e` is a constant expression.
 * * An expression of one of the forms `identical(e1, e2)`, `e1 == e2`,
 * `e1 != e2` where `e1` and `e2` are constant expressions that evaluate to a
 * numeric, string or boolean value or to `null`.
 * * An expression of one of the forms `!e`, `e1 && e2` or `e1 || e2`, where
 * `e`, `e1` and `e2` are constant expressions that evaluate to a boolean value or
 * to `null`.
 * * An expression of one of the forms `~e`, `e1 ^ e2`, `e1 & e2`,
 * `e1 | e2`, `e1 >> e2` or `e1 << e2`, where `e`, `e1` and `e2`
 * are constant expressions that evaluate to an integer value or to `null`.
 * * An expression of one of the forms `-e`, `e1 + e2`, `e1 - e2`,
 * `e1 * e2`, `e1 / e2`, `e1 ~/ e2`, `e1 > e2`, `e1 < e2`,
 * `e1 >= e2`, `e1 <= e2` or `e1 % e2`, where `e`, `e1` and `e2`
 * are constant expressions that evaluate to a numeric value or to `null`.
 * </blockquote> The values returned by instances of this class are therefore `null` and
 * instances of the classes `Boolean`, `BigInteger`, `Double`, `String`, and
 * `DartObject`.
 *
 * In addition, this class defines several values that can be returned to indicate various
 * conditions encountered during evaluation. These are documented with the static field that define
 * those values.
 */
class ConstantEvaluator extends GeneralizingAstVisitor<Object> {
  /**
   * The value returned for expressions (or non-expression nodes) that are not compile-time constant
   * expressions.
   */
  static Object NOT_A_CONSTANT = new Object();

  @override
  Object visitAdjacentStrings(AdjacentStrings node) {
    StringBuffer buffer = new StringBuffer();
    for (StringLiteral string in node.strings) {
      Object value = string.accept(this);
      if (identical(value, NOT_A_CONSTANT)) {
        return value;
      }
      buffer.write(value);
    }
    return buffer.toString();
  }

  @override
  Object visitBinaryExpression(BinaryExpression node) {
    Object leftOperand = node.leftOperand.accept(this);
    if (identical(leftOperand, NOT_A_CONSTANT)) {
      return leftOperand;
    }
    Object rightOperand = node.rightOperand.accept(this);
    if (identical(rightOperand, NOT_A_CONSTANT)) {
      return rightOperand;
    }
    while (true) {
      if (node.operator.type == TokenType.AMPERSAND) {
        // integer or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand & rightOperand;
        }
      } else if (node.operator.type == TokenType.AMPERSAND_AMPERSAND) {
        // boolean or {@code null}
        if (leftOperand is bool && rightOperand is bool) {
          return leftOperand && rightOperand;
        }
      } else if (node.operator.type == TokenType.BANG_EQ) {
        // numeric, string, boolean, or {@code null}
        if (leftOperand is bool && rightOperand is bool) {
          return leftOperand != rightOperand;
        } else if (leftOperand is int && rightOperand is int) {
          return leftOperand != rightOperand;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand != rightOperand;
        } else if (leftOperand is String && rightOperand is String) {
          return leftOperand != rightOperand;
        }
      } else if (node.operator.type == TokenType.BAR) {
        // integer or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand | rightOperand;
        }
      } else if (node.operator.type == TokenType.BAR_BAR) {
        // boolean or {@code null}
        if (leftOperand is bool && rightOperand is bool) {
          return leftOperand || rightOperand;
        }
      } else if (node.operator.type == TokenType.CARET) {
        // integer or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand ^ rightOperand;
        }
      } else if (node.operator.type == TokenType.EQ_EQ) {
        // numeric, string, boolean, or {@code null}
        if (leftOperand is bool && rightOperand is bool) {
          return leftOperand == rightOperand;
        } else if (leftOperand is int && rightOperand is int) {
          return leftOperand == rightOperand;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand == rightOperand;
        } else if (leftOperand is String && rightOperand is String) {
          return leftOperand == rightOperand;
        }
      } else if (node.operator.type == TokenType.GT) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand.compareTo(rightOperand) > 0;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand.compareTo(rightOperand) > 0;
        }
      } else if (node.operator.type == TokenType.GT_EQ) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand.compareTo(rightOperand) >= 0;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand.compareTo(rightOperand) >= 0;
        }
      } else if (node.operator.type == TokenType.GT_GT) {
        // integer or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand >> rightOperand;
        }
      } else if (node.operator.type == TokenType.LT) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand.compareTo(rightOperand) < 0;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand.compareTo(rightOperand) < 0;
        }
      } else if (node.operator.type == TokenType.LT_EQ) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand.compareTo(rightOperand) <= 0;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand.compareTo(rightOperand) <= 0;
        }
      } else if (node.operator.type == TokenType.LT_LT) {
        // integer or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand << rightOperand;
        }
      } else if (node.operator.type == TokenType.MINUS) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand - rightOperand;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand - rightOperand;
        }
      } else if (node.operator.type == TokenType.PERCENT) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand.remainder(rightOperand);
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand % rightOperand;
        }
      } else if (node.operator.type == TokenType.PLUS) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand + rightOperand;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand + rightOperand;
        }
      } else if (node.operator.type == TokenType.STAR) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          return leftOperand * rightOperand;
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand * rightOperand;
        }
      } else if (node.operator.type == TokenType.SLASH) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          if (rightOperand != 0) {
            return leftOperand ~/ rightOperand;
          } else {
            return leftOperand.toDouble() / rightOperand.toDouble();
          }
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand / rightOperand;
        }
      } else if (node.operator.type == TokenType.TILDE_SLASH) {
        // numeric or {@code null}
        if (leftOperand is int && rightOperand is int) {
          if (rightOperand != 0) {
            return leftOperand ~/ rightOperand;
          } else {
            return 0;
          }
        } else if (leftOperand is double && rightOperand is double) {
          return leftOperand ~/ rightOperand;
        }
      } else {
      }
      break;
    }
    // TODO(brianwilkerson) This doesn't handle numeric conversions.
    return visitExpression(node);
  }

  @override
  Object visitBooleanLiteral(BooleanLiteral node) => node.value ? true : false;

  @override
  Object visitDoubleLiteral(DoubleLiteral node) => node.value;

  @override
  Object visitIntegerLiteral(IntegerLiteral node) => node.value;

  @override
  Object visitInterpolationExpression(InterpolationExpression node) {
    Object value = node.expression.accept(this);
    if (value == null ||
        value is bool ||
        value is String ||
        value is int ||
        value is double) {
      return value;
    }
    return NOT_A_CONSTANT;
  }

  @override
  Object visitInterpolationString(InterpolationString node) => node.value;

  @override
  Object visitListLiteral(ListLiteral node) {
    List<Object> list = new List<Object>();
    for (Expression element in node.elements) {
      Object value = element.accept(this);
      if (identical(value, NOT_A_CONSTANT)) {
        return value;
      }
      list.add(value);
    }
    return list;
  }

  @override
  Object visitMapLiteral(MapLiteral node) {
    HashMap<String, Object> map = new HashMap<String, Object>();
    for (MapLiteralEntry entry in node.entries) {
      Object key = entry.key.accept(this);
      Object value = entry.value.accept(this);
      if (key is! String || identical(value, NOT_A_CONSTANT)) {
        return NOT_A_CONSTANT;
      }
      map[(key as String)] = value;
    }
    return map;
  }

  @override
  Object visitMethodInvocation(MethodInvocation node) => visitNode(node);

  @override
  Object visitNode(AstNode node) => NOT_A_CONSTANT;

  @override
  Object visitNullLiteral(NullLiteral node) => null;

  @override
  Object visitParenthesizedExpression(ParenthesizedExpression node) =>
      node.expression.accept(this);

  @override
  Object visitPrefixedIdentifier(PrefixedIdentifier node) =>
      _getConstantValue(null);

  @override
  Object visitPrefixExpression(PrefixExpression node) {
    Object operand = node.operand.accept(this);
    if (identical(operand, NOT_A_CONSTANT)) {
      return operand;
    }
    while (true) {
      if (node.operator.type == TokenType.BANG) {
        if (identical(operand, true)) {
          return false;
        } else if (identical(operand, false)) {
          return true;
        }
      } else if (node.operator.type == TokenType.TILDE) {
        if (operand is int) {
          return ~operand;
        }
      } else if (node.operator.type == TokenType.MINUS) {
        if (operand == null) {
          return null;
        } else if (operand is int) {
          return -operand;
        } else if (operand is double) {
          return -operand;
        }
      } else {
      }
      break;
    }
    return NOT_A_CONSTANT;
  }

  @override
  Object visitPropertyAccess(PropertyAccess node) => _getConstantValue(null);

  @override
  Object visitSimpleIdentifier(SimpleIdentifier node) =>
      _getConstantValue(null);

  @override
  Object visitSimpleStringLiteral(SimpleStringLiteral node) => node.value;

  @override
  Object visitStringInterpolation(StringInterpolation node) {
    StringBuffer buffer = new StringBuffer();
    for (InterpolationElement element in node.elements) {
      Object value = element.accept(this);
      if (identical(value, NOT_A_CONSTANT)) {
        return value;
      }
      buffer.write(value);
    }
    return buffer.toString();
  }

  @override
  Object visitSymbolLiteral(SymbolLiteral node) {
    // TODO(brianwilkerson) This isn't optimal because a Symbol is not a String.
    StringBuffer buffer = new StringBuffer();
    for (Token component in node.components) {
      if (buffer.length > 0) {
        buffer.writeCharCode(0x2E);
      }
      buffer.write(component.lexeme);
    }
    return buffer.toString();
  }

  /**
   * Return the constant value of the static constant represented by the given element.
   *
   * @param element the element whose value is to be returned
   * @return the constant value of the static constant
   */
  Object _getConstantValue(Element element) {
    // TODO(brianwilkerson) Implement this
    if (element is FieldElement) {
      FieldElement field = element;
      if (field.isStatic && field.isConst) {
        //field.getConstantValue();
      }
      //    } else if (element instanceof VariableElement) {
      //      VariableElement variable = (VariableElement) element;
      //      if (variable.isStatic() && variable.isConst()) {
      //        //variable.getConstantValue();
      //      }
    }
    return NOT_A_CONSTANT;
  }
}

/**
 * Instances of the class `ConstructorDeclaration` represent a constructor declaration.
 *
 * <pre>
 * constructorDeclaration ::=
 *     constructorSignature [FunctionBody]?
 *   | constructorName formalParameterList ':' 'this' ('.' [SimpleIdentifier])? arguments
 *
 * constructorSignature ::=
 *     'external'? constructorName formalParameterList initializerList?
 *   | 'external'? 'factory' factoryName formalParameterList initializerList?
 *   | 'external'? 'const'  constructorName formalParameterList initializerList?
 *
 * constructorName ::=
 *     [SimpleIdentifier] ('.' [SimpleIdentifier])?
 *
 * factoryName ::=
 *     [Identifier] ('.' [SimpleIdentifier])?
 *
 * initializerList ::=
 *     ':' [ConstructorInitializer] (',' [ConstructorInitializer])*
 * </pre>
 */
class ConstructorDeclaration extends ClassMember {
  /**
   * The token for the 'external' keyword, or `null` if the constructor is not external.
   */
  Token externalKeyword;

  /**
   * The token for the 'const' keyword, or `null` if the constructor is not a const
   * constructor.
   */
  Token constKeyword;

  /**
   * The token for the 'factory' keyword, or `null` if the constructor is not a factory
   * constructor.
   */
  Token factoryKeyword;

  /**
   * The type of object being created. This can be different than the type in which the constructor
   * is being declared if the constructor is the implementation of a factory constructor.
   */
  Identifier _returnType;

  /**
   * The token for the period before the constructor name, or `null` if the constructor being
   * declared is unnamed.
   */
  Token period;

  /**
   * The name of the constructor, or `null` if the constructor being declared is unnamed.
   */
  SimpleIdentifier _name;

  /**
   * The parameters associated with the constructor.
   */
  FormalParameterList _parameters;

  /**
   * The token for the separator (colon or equals) before the initializer list or redirection, or
   * `null` if there are no initializers.
   */
  Token separator;

  /**
   * The initializers associated with the constructor.
   */
  NodeList<ConstructorInitializer> _initializers;

  /**
   * The name of the constructor to which this constructor will be redirected, or `null` if
   * this is not a redirecting factory constructor.
   */
  ConstructorName _redirectedConstructor;

  /**
   * The body of the constructor, or `null` if the constructor does not have a body.
   */
  FunctionBody _body;

  /**
   * The element associated with this constructor, or `null` if the AST structure has not been
   * resolved or if this constructor could not be resolved.
   */
  ConstructorElement element;

  /**
   * Initialize a newly created constructor declaration.
   *
   * @param externalKeyword the token for the 'external' keyword
   * @param comment the documentation comment associated with this constructor
   * @param metadata the annotations associated with this constructor
   * @param constKeyword the token for the 'const' keyword
   * @param factoryKeyword the token for the 'factory' keyword
   * @param returnType the return type of the constructor
   * @param period the token for the period before the constructor name
   * @param name the name of the constructor
   * @param parameters the parameters associated with the constructor
   * @param separator the token for the colon or equals before the initializers
   * @param initializers the initializers associated with the constructor
   * @param redirectedConstructor the name of the constructor to which this constructor will be
   *          redirected
   * @param body the body of the constructor
   */
  ConstructorDeclaration(Comment comment, List<Annotation> metadata,
      this.externalKeyword, this.constKeyword, this.factoryKeyword,
      Identifier returnType, this.period, SimpleIdentifier name,
      FormalParameterList parameters, this.separator,
      List<ConstructorInitializer> initializers,
      ConstructorName redirectedConstructor, FunctionBody body)
      : super(comment, metadata) {
    _returnType = becomeParentOf(returnType);
    _name = becomeParentOf(name);
    _parameters = becomeParentOf(parameters);
    _initializers = new NodeList<ConstructorInitializer>(this, initializers);
    _redirectedConstructor = becomeParentOf(redirectedConstructor);
    _body = becomeParentOf(body);
  }

  /**
   * Return the body of the constructor, or `null` if the constructor does not have a body.
   *
   * @return the body of the constructor
   */
  FunctionBody get body => _body;

  /**
   * Set the body of the constructor to the given function body.
   *
   * @param functionBody the body of the constructor
   */
  void set body(FunctionBody functionBody) {
    _body = becomeParentOf(functionBody);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(externalKeyword)
      ..add(constKeyword)
      ..add(factoryKeyword)
      ..add(_returnType)
      ..add(period)
      ..add(_name)
      ..add(_parameters)
      ..add(separator)
      ..addAll(initializers)
      ..add(_redirectedConstructor)
      ..add(_body);

  @override
  Token get endToken {
    if (_body != null) {
      return _body.endToken;
    } else if (!_initializers.isEmpty) {
      return _initializers.endToken;
    }
    return _parameters.endToken;
  }

  @override
  Token get firstTokenAfterCommentAndMetadata {
    Token leftMost =
        Token.lexicallyFirst([externalKeyword, constKeyword, factoryKeyword]);
    if (leftMost != null) {
      return leftMost;
    }
    return _returnType.beginToken;
  }

  /**
   * Return the initializers associated with the constructor.
   *
   * @return the initializers associated with the constructor
   */
  NodeList<ConstructorInitializer> get initializers => _initializers;

  /**
   * Return the name of the constructor, or `null` if the constructor being declared is
   * unnamed.
   *
   * @return the name of the constructor
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the constructor to the given identifier.
   *
   * @param identifier the name of the constructor
   */
  void set name(SimpleIdentifier identifier) {
    _name = becomeParentOf(identifier);
  }

  /**
   * Return the parameters associated with the constructor.
   *
   * @return the parameters associated with the constructor
   */
  FormalParameterList get parameters => _parameters;

  /**
   * Set the parameters associated with the constructor to the given list of parameters.
   *
   * @param parameters the parameters associated with the constructor
   */
  void set parameters(FormalParameterList parameters) {
    _parameters = becomeParentOf(parameters);
  }

  /**
   * Return the name of the constructor to which this constructor will be redirected, or
   * `null` if this is not a redirecting factory constructor.
   *
   * @return the name of the constructor to which this constructor will be redirected
   */
  ConstructorName get redirectedConstructor => _redirectedConstructor;

  /**
   * Set the name of the constructor to which this constructor will be redirected to the given
   * constructor name.
   *
   * @param redirectedConstructor the name of the constructor to which this constructor will be
   *          redirected
   */
  void set redirectedConstructor(ConstructorName redirectedConstructor) {
    _redirectedConstructor = becomeParentOf(redirectedConstructor);
  }

  /**
   * Return the type of object being created. This can be different than the type in which the
   * constructor is being declared if the constructor is the implementation of a factory
   * constructor.
   *
   * @return the type of object being created
   */
  Identifier get returnType => _returnType;

  /**
   * Set the type of object being created to the given type name.
   *
   * @param typeName the type of object being created
   */
  void set returnType(Identifier typeName) {
    _returnType = becomeParentOf(typeName);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitConstructorDeclaration(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_returnType, visitor);
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_parameters, visitor);
    _initializers.accept(visitor);
    safelyVisitChild(_redirectedConstructor, visitor);
    safelyVisitChild(_body, visitor);
  }
}

/**
 * Instances of the class `ConstructorFieldInitializer` represent the initialization of a
 * field within a constructor's initialization list.
 *
 * <pre>
 * fieldInitializer ::=
 *     ('this' '.')? [SimpleIdentifier] '=' [Expression]
 * </pre>
 */
class ConstructorFieldInitializer extends ConstructorInitializer {
  /**
   * The token for the 'this' keyword, or `null` if there is no 'this' keyword.
   */
  Token keyword;

  /**
   * The token for the period after the 'this' keyword, or `null` if there is no 'this'
   * keyword.
   */
  Token period;

  /**
   * The name of the field being initialized.
   */
  SimpleIdentifier _fieldName;

  /**
   * The token for the equal sign between the field name and the expression.
   */
  Token equals;

  /**
   * The expression computing the value to which the field will be initialized.
   */
  Expression _expression;

  /**
   * Initialize a newly created field initializer to initialize the field with the given name to the
   * value of the given expression.
   *
   * @param keyword the token for the 'this' keyword
   * @param period the token for the period after the 'this' keyword
   * @param fieldName the name of the field being initialized
   * @param equals the token for the equal sign between the field name and the expression
   * @param expression the expression computing the value to which the field will be initialized
   */
  ConstructorFieldInitializer(this.keyword, this.period,
      SimpleIdentifier fieldName, this.equals, Expression expression) {
    _fieldName = becomeParentOf(fieldName);
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken {
    if (keyword != null) {
      return keyword;
    }
    return _fieldName.beginToken;
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(period)
      ..add(_fieldName)
      ..add(equals)
      ..add(_expression);

  @override
  Token get endToken => _expression.endToken;

  /**
   * Return the expression computing the value to which the field will be initialized.
   *
   * @return the expression computing the value to which the field will be initialized
   */
  Expression get expression => _expression;

  /**
   * Set the expression computing the value to which the field will be initialized to the given
   * expression.
   *
   * @param expression the expression computing the value to which the field will be initialized
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  /**
   * Return the name of the field being initialized.
   *
   * @return the name of the field being initialized
   */
  SimpleIdentifier get fieldName => _fieldName;

  /**
   * Set the name of the field being initialized to the given identifier.
   *
   * @param identifier the name of the field being initialized
   */
  void set fieldName(SimpleIdentifier identifier) {
    _fieldName = becomeParentOf(identifier);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitConstructorFieldInitializer(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_fieldName, visitor);
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * Instances of the class `ConstructorInitializer` defines the behavior of nodes that can
 * occur in the initializer list of a constructor declaration.
 *
 * <pre>
 * constructorInitializer ::=
 *     [SuperConstructorInvocation]
 *   | [ConstructorFieldInitializer]
 * </pre>
 */
abstract class ConstructorInitializer extends AstNode {
}

/**
 * Instances of the class `ConstructorName` represent the name of the constructor.
 *
 * <pre>
 * constructorName:
 *     type ('.' identifier)?
 * </pre>
 */
class ConstructorName extends AstNode {
  /**
   * The name of the type defining the constructor.
   */
  TypeName _type;

  /**
   * The token for the period before the constructor name, or `null` if the specified
   * constructor is the unnamed constructor.
   */
  Token period;

  /**
   * The name of the constructor, or `null` if the specified constructor is the unnamed
   * constructor.
   */
  SimpleIdentifier _name;

  /**
   * The element associated with this constructor name based on static type information, or
   * `null` if the AST structure has not been resolved or if this constructor name could not
   * be resolved.
   */
  ConstructorElement staticElement;

  /**
   * Initialize a newly created constructor name.
   *
   * @param type the name of the type defining the constructor
   * @param period the token for the period before the constructor name
   * @param name the name of the constructor
   */
  ConstructorName(TypeName type, this.period, SimpleIdentifier name) {
    _type = becomeParentOf(type);
    _name = becomeParentOf(name);
  }

  @override
  Token get beginToken => _type.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_type)
      ..add(period)
      ..add(_name);

  @override
  Token get endToken {
    if (_name != null) {
      return _name.endToken;
    }
    return _type.endToken;
  }

  /**
   * Return the name of the constructor, or `null` if the specified constructor is the unnamed
   * constructor.
   *
   * @return the name of the constructor
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the constructor to the given name.
   *
   * @param name the name of the constructor
   */
  void set name(SimpleIdentifier name) {
    _name = becomeParentOf(name);
  }

  /**
   * Return the name of the type defining the constructor.
   *
   * @return the name of the type defining the constructor
   */
  TypeName get type => _type;

  /**
   * Set the name of the type defining the constructor to the given type name.
   *
   * @param type the name of the type defining the constructor
   */
  void set type(TypeName type) {
    _type = becomeParentOf(type);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitConstructorName(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_type, visitor);
    safelyVisitChild(_name, visitor);
  }
}

/**
 * Instances of the class `ContinueStatement` represent a continue statement.
 *
 * <pre>
 * continueStatement ::=
 *     'continue' [SimpleIdentifier]? ';'
 * </pre>
 */
class ContinueStatement extends Statement {
  /**
   * The token representing the 'continue' keyword.
   */
  Token keyword;

  /**
   * The label associated with the statement, or `null` if there is no label.
   */
  SimpleIdentifier _label;

  /**
   * The semicolon terminating the statement.
   */
  Token semicolon;

  /**
   * The AstNode which this continue statement is continuing to.  This will be
   * either a Statement (in the case of continuing a loop) or a SwitchMember
   * (in the case of continuing from one switch case to another).  Null if the
   * AST has not yet been resolved or if the target could not be resolved.
   * Note that if the source code has errors, the target may be invalid (e.g.
   * the target may be in an enclosing function).
   */
  AstNode target;

  /**
   * Initialize a newly created continue statement.
   *
   * @param keyword the token representing the 'continue' keyword
   * @param label the label associated with the statement
   * @param semicolon the semicolon terminating the statement
   */
  ContinueStatement(this.keyword, SimpleIdentifier label, this.semicolon) {
    _label = becomeParentOf(label);
  }

  @override
  Token get beginToken => keyword;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(_label)
      ..add(semicolon);

  @override
  Token get endToken => semicolon;

  /**
   * Return the label associated with the statement, or `null` if there is no label.
   *
   * @return the label associated with the statement
   */
  SimpleIdentifier get label => _label;

  /**
   * Set the label associated with the statement to the given label.
   *
   * @param identifier the label associated with the statement
   */
  void set label(SimpleIdentifier identifier) {
    _label = becomeParentOf(identifier);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitContinueStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_label, visitor);
  }
}

/**
 * The abstract class `Declaration` defines the behavior common to nodes that represent the
 * declaration of a name. Each declared name is visible within a name scope.
 */
abstract class Declaration extends AnnotatedNode {
  /**
   * Initialize a newly created declaration.
   *
   * @param comment the documentation comment associated with this declaration
   * @param metadata the annotations associated with this declaration
   */
  Declaration(Comment comment, List<Annotation> metadata)
      : super(comment, metadata);

  /**
   * Return the element associated with this declaration, or `null` if either this node
   * corresponds to a list of declarations or if the AST structure has not been resolved.
   *
   * @return the element associated with this declaration
   */
  Element get element;
}

/**
 * Instances of the class `DeclaredIdentifier` represent the declaration of a single
 * identifier.
 *
 * <pre>
 * declaredIdentifier ::=
 *     [Annotation] finalConstVarOrType [SimpleIdentifier]
 * </pre>
 */
class DeclaredIdentifier extends Declaration {
  /**
   * The token representing either the 'final', 'const' or 'var' keyword, or `null` if no
   * keyword was used.
   */
  Token keyword;

  /**
   * The name of the declared type of the parameter, or `null` if the parameter does not have
   * a declared type.
   */
  TypeName _type;

  /**
   * The name of the variable being declared.
   */
  SimpleIdentifier _identifier;

  /**
   * Initialize a newly created formal parameter.
   *
   * @param comment the documentation comment associated with this parameter
   * @param metadata the annotations associated with this parameter
   * @param keyword the token representing either the 'final', 'const' or 'var' keyword
   * @param type the name of the declared type of the parameter
   * @param identifier the name of the parameter being declared
   */
  DeclaredIdentifier(Comment comment, List<Annotation> metadata, this.keyword,
      TypeName type, SimpleIdentifier identifier)
      : super(comment, metadata) {
    _type = becomeParentOf(type);
    _identifier = becomeParentOf(identifier);
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => super._childEntities
      ..add(keyword)
      ..add(_type)
      ..add(_identifier);

  @override
  LocalVariableElement get element {
    if (_identifier == null) {
      return null;
    }
    return _identifier.staticElement as LocalVariableElement;
  }

  @override
  Token get endToken => _identifier.endToken;

  @override
  Token get firstTokenAfterCommentAndMetadata {
    if (keyword != null) {
      return keyword;
    } else if (_type != null) {
      return _type.beginToken;
    }
    return _identifier.beginToken;
  }

  /**
   * Return the name of the variable being declared.
   *
   * @return the name of the variable being declared
   */
  SimpleIdentifier get identifier => _identifier;

  /**
   * Set the name of the variable being declared to the given name.
   *
   * @param identifier the new name of the variable being declared
   */
  void set identifier(SimpleIdentifier identifier) {
    _identifier = becomeParentOf(identifier);
  }

  /**
   * Return `true` if this variable was declared with the 'const' modifier.
   *
   * @return `true` if this variable was declared with the 'const' modifier
   */
  bool get isConst =>
      (keyword is KeywordToken) && (keyword as KeywordToken).keyword == Keyword.CONST;

  /**
   * Return `true` if this variable was declared with the 'final' modifier. Variables that are
   * declared with the 'const' modifier will return `false` even though they are implicitly
   * final.
   *
   * @return `true` if this variable was declared with the 'final' modifier
   */
  bool get isFinal =>
      (keyword is KeywordToken) && (keyword as KeywordToken).keyword == Keyword.FINAL;

  /**
   * Return the name of the declared type of the parameter, or `null` if the parameter does
   * not have a declared type.
   *
   * @return the name of the declared type of the parameter
   */
  TypeName get type => _type;

  /**
   * Set the name of the declared type of the parameter to the given type name.
   *
   * @param typeName the name of the declared type of the parameter
   */
  void set type(TypeName typeName) {
    _type = becomeParentOf(typeName);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitDeclaredIdentifier(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_type, visitor);
    safelyVisitChild(_identifier, visitor);
  }
}

/**
 * Instances of the class `DefaultFormalParameter` represent a formal parameter with a default
 * value. There are two kinds of parameters that are both represented by this class: named formal
 * parameters and positional formal parameters.
 *
 * <pre>
 * defaultFormalParameter ::=
 *     [NormalFormalParameter] ('=' [Expression])?
 *
 * defaultNamedParameter ::=
 *     [NormalFormalParameter] (':' [Expression])?
 * </pre>
 */
class DefaultFormalParameter extends FormalParameter {
  /**
   * The formal parameter with which the default value is associated.
   */
  NormalFormalParameter _parameter;

  /**
   * The kind of this parameter.
   */
  ParameterKind kind;

  /**
   * The token separating the parameter from the default value, or `null` if there is no
   * default value.
   */
  Token separator;

  /**
   * The expression computing the default value for the parameter, or `null` if there is no
   * default value.
   */
  Expression _defaultValue;

  /**
   * Initialize a newly created default formal parameter.
   *
   * @param parameter the formal parameter with which the default value is associated
   * @param kind the kind of this parameter
   * @param separator the token separating the parameter from the default value
   * @param defaultValue the expression computing the default value for the parameter
   */
  DefaultFormalParameter(NormalFormalParameter parameter, this.kind,
      this.separator, Expression defaultValue) {
    _parameter = becomeParentOf(parameter);
    _defaultValue = becomeParentOf(defaultValue);
  }

  @override
  Token get beginToken => _parameter.beginToken;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_parameter)
      ..add(separator)
      ..add(_defaultValue);

  /**
   * Return the expression computing the default value for the parameter, or `null` if there
   * is no default value.
   *
   * @return the expression computing the default value for the parameter
   */
  Expression get defaultValue => _defaultValue;

  /**
   * Set the expression computing the default value for the parameter to the given expression.
   *
   * @param expression the expression computing the default value for the parameter
   */
  void set defaultValue(Expression expression) {
    _defaultValue = becomeParentOf(expression);
  }

  @override
  Token get endToken {
    if (_defaultValue != null) {
      return _defaultValue.endToken;
    }
    return _parameter.endToken;
  }

  @override
  SimpleIdentifier get identifier => _parameter.identifier;

  @override
  bool get isConst => _parameter != null && _parameter.isConst;

  @override
  bool get isFinal => _parameter != null && _parameter.isFinal;

  /**
   * Return the formal parameter with which the default value is associated.
   *
   * @return the formal parameter with which the default value is associated
   */
  NormalFormalParameter get parameter => _parameter;

  /**
   * Set the formal parameter with which the default value is associated to the given parameter.
   *
   * @param formalParameter the formal parameter with which the default value is associated
   */
  void set parameter(NormalFormalParameter formalParameter) {
    _parameter = becomeParentOf(formalParameter);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitDefaultFormalParameter(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_parameter, visitor);
    safelyVisitChild(_defaultValue, visitor);
  }
}

/**
 * This recursive Ast visitor is used to run over [Expression]s to determine if the expression
 * is composed by at least one deferred [PrefixedIdentifier].
 *
 * See [PrefixedIdentifier.isDeferred].
 */
class DeferredLibraryReferenceDetector extends RecursiveAstVisitor<Object> {
  bool _result = false;

  /**
   * Return `true` if the visitor found a [PrefixedIdentifier] that returned
   * `true` to the [PrefixedIdentifier.isDeferred] query.
   */
  bool get result => _result;

  @override
  Object visitPrefixedIdentifier(PrefixedIdentifier node) {
    if (!_result) {
      if (node.isDeferred) {
        _result = true;
      }
    }
    return null;
  }
}

/**
 * The abstract class `Directive` defines the behavior common to nodes that represent a
 * directive.
 *
 * <pre>
 * directive ::=
 *     [ExportDirective]
 *   | [ImportDirective]
 *   | [LibraryDirective]
 *   | [PartDirective]
 *   | [PartOfDirective]
 * </pre>
 */
abstract class Directive extends AnnotatedNode {
  /**
   * The element associated with this directive, or `null` if the AST structure has not been
   * resolved or if this directive could not be resolved.
   */
  Element element;

  /**
   * Initialize a newly create directive.
   *
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   */
  Directive(Comment comment, List<Annotation> metadata)
      : super(comment, metadata);

  /**
   * Return the token representing the keyword that introduces this directive ('import', 'export',
   * 'library' or 'part').
   *
   * @return the token representing the keyword that introduces this directive
   */
  Token get keyword;
}

/**
 * Instances of the class `DoStatement` represent a do statement.
 *
 * <pre>
 * doStatement ::=
 *     'do' [Statement] 'while' '(' [Expression] ')' ';'
 * </pre>
 */
class DoStatement extends Statement {
  /**
   * The token representing the 'do' keyword.
   */
  Token doKeyword;

  /**
   * The body of the loop.
   */
  Statement _body;

  /**
   * The token representing the 'while' keyword.
   */
  Token whileKeyword;

  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The condition that determines when the loop will terminate.
   */
  Expression _condition;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * The semicolon terminating the statement.
   */
  Token semicolon;

  /**
   * Initialize a newly created do loop.
   *
   * @param doKeyword the token representing the 'do' keyword
   * @param body the body of the loop
   * @param whileKeyword the token representing the 'while' keyword
   * @param leftParenthesis the left parenthesis
   * @param condition the condition that determines when the loop will terminate
   * @param rightParenthesis the right parenthesis
   * @param semicolon the semicolon terminating the statement
   */
  DoStatement(this.doKeyword, Statement body, this.whileKeyword,
      this.leftParenthesis, Expression condition, this.rightParenthesis,
      this.semicolon) {
    _body = becomeParentOf(body);
    _condition = becomeParentOf(condition);
  }

  @override
  Token get beginToken => doKeyword;

  /**
   * Return the body of the loop.
   *
   * @return the body of the loop
   */
  Statement get body => _body;

  /**
   * Set the body of the loop to the given statement.
   *
   * @param statement the body of the loop
   */
  void set body(Statement statement) {
    _body = becomeParentOf(statement);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(doKeyword)
      ..add(_body)
      ..add(whileKeyword)
      ..add(leftParenthesis)
      ..add(_condition)
      ..add(rightParenthesis)
      ..add(semicolon);

  /**
   * Return the condition that determines when the loop will terminate.
   *
   * @return the condition that determines when the loop will terminate
   */
  Expression get condition => _condition;

  /**
   * Set the condition that determines when the loop will terminate to the given expression.
   *
   * @param expression the condition that determines when the loop will terminate
   */
  void set condition(Expression expression) {
    _condition = becomeParentOf(expression);
  }

  @override
  Token get endToken => semicolon;

  @override
  accept(AstVisitor visitor) => visitor.visitDoStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_body, visitor);
    safelyVisitChild(_condition, visitor);
  }
}

/**
 * Instances of the class `DoubleLiteral` represent a floating point literal expression.
 *
 * <pre>
 * doubleLiteral ::=
 *     decimalDigit+ ('.' decimalDigit*)? exponent?
 *   | '.' decimalDigit+ exponent?
 *
 * exponent ::=
 *     ('e' | 'E') ('+' | '-')? decimalDigit+
 * </pre>
 */
class DoubleLiteral extends Literal {
  /**
   * The token representing the literal.
   */
  Token literal;

  /**
   * The value of the literal.
   */
  double value = 0.0;

  /**
   * Initialize a newly created floating point literal.
   *
   * @param literal the token representing the literal
   * @param value the value of the literal
   */
  DoubleLiteral(this.literal, this.value);

  @override
  Token get beginToken => literal;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(literal);

  @override
  Token get endToken => literal;

  @override
  accept(AstVisitor visitor) => visitor.visitDoubleLiteral(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `ElementLocator` locate the [Element]
 * associated with a given [AstNode].
 */
class ElementLocator {
  /**
   * Locate the [Element] associated with the given [AstNode].
   *
   * @param node the node (not `null`)
   * @return the associated element, or `null` if none is found
   */
  static Element locate(AstNode node) {
    ElementLocator_ElementMapper mapper = new ElementLocator_ElementMapper();
    return node.accept(mapper);
  }

  /**
   * Locate the [Element] associated with the given [AstNode] and offset.
   *
   * @param node the node (not `null`)
   * @param offset the offset relative to source
   * @return the associated element, or `null` if none is found
   */
  static Element locateWithOffset(AstNode node, int offset) {
    if (node == null) {
      return null;
    }
    // try to get Element from node
    Element nodeElement = locate(node);
    if (nodeElement != null) {
      return nodeElement;
    }
    // no Element
    return null;
  }
}

/**
 * Visitor that maps nodes to elements.
 */
class ElementLocator_ElementMapper extends GeneralizingAstVisitor<Element> {
  @override
  Element visitAnnotation(Annotation node) => node.element;

  @override
  Element visitAssignmentExpression(AssignmentExpression node) =>
      node.bestElement;

  @override
  Element visitBinaryExpression(BinaryExpression node) => node.bestElement;

  @override
  Element visitClassDeclaration(ClassDeclaration node) => node.element;

  @override
  Element visitCompilationUnit(CompilationUnit node) => node.element;

  @override
  Element visitConstructorDeclaration(ConstructorDeclaration node) =>
      node.element;

  @override
  Element visitFunctionDeclaration(FunctionDeclaration node) => node.element;

  @override
  Element visitIdentifier(Identifier node) {
    AstNode parent = node.parent;
    // Type name in Annotation
    if (parent is Annotation) {
      Annotation annotation = parent;
      if (identical(annotation.name, node) &&
          annotation.constructorName == null) {
        return annotation.element;
      }
    }
    // Extra work to map Constructor Declarations to their associated
    // Constructor Elements
    if (parent is ConstructorDeclaration) {
      ConstructorDeclaration decl = parent;
      Identifier returnType = decl.returnType;
      if (identical(returnType, node)) {
        SimpleIdentifier name = decl.name;
        if (name != null) {
          return name.bestElement;
        }
        Element element = node.bestElement;
        if (element is ClassElement) {
          return element.unnamedConstructor;
        }
      }
    }
    if (parent is LibraryIdentifier) {
      AstNode grandParent = parent.parent;
      if (grandParent is PartOfDirective) {
        Element element = grandParent.element;
        if (element is LibraryElement) {
          return element.definingCompilationUnit;
        }
      }
    }
    return node.bestElement;
  }

  @override
  Element visitImportDirective(ImportDirective node) => node.element;

  @override
  Element visitIndexExpression(IndexExpression node) => node.bestElement;

  @override
  Element visitInstanceCreationExpression(InstanceCreationExpression node) =>
      node.staticElement;

  @override
  Element visitLibraryDirective(LibraryDirective node) => node.element;

  @override
  Element visitMethodDeclaration(MethodDeclaration node) => node.element;

  @override
  Element visitMethodInvocation(MethodInvocation node) =>
      node.methodName.bestElement;

  @override
  Element visitPostfixExpression(PostfixExpression node) => node.bestElement;

  @override
  Element visitPrefixedIdentifier(PrefixedIdentifier node) => node.bestElement;

  @override
  Element visitPrefixExpression(PrefixExpression node) => node.bestElement;

  @override
  Element visitStringLiteral(StringLiteral node) {
    AstNode parent = node.parent;
    if (parent is UriBasedDirective) {
      return parent.uriElement;
    }
    return null;
  }

  @override
  Element visitVariableDeclaration(VariableDeclaration node) => node.element;
}

/**
 * Instances of the class `EmptyFunctionBody` represent an empty function body, which can only
 * appear in constructors or abstract methods.
 *
 * <pre>
 * emptyFunctionBody ::=
 *     ';'
 * </pre>
 */
class EmptyFunctionBody extends FunctionBody {
  /**
   * The token representing the semicolon that marks the end of the function body.
   */
  Token semicolon;

  /**
   * Initialize a newly created function body.
   *
   * @param semicolon the token representing the semicolon that marks the end of the function body
   */
  EmptyFunctionBody(this.semicolon);

  @override
  Token get beginToken => semicolon;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(semicolon);

  @override
  Token get endToken => semicolon;

  @override
  accept(AstVisitor visitor) => visitor.visitEmptyFunctionBody(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // Empty function bodies have no children.
  }
}

/**
 * Instances of the class `EmptyStatement` represent an empty statement.
 *
 * <pre>
 * emptyStatement ::=
 *     ';'
 * </pre>
 */
class EmptyStatement extends Statement {
  /**
   * The semicolon terminating the statement.
   */
  Token semicolon;

  /**
   * Initialize a newly created empty statement.
   *
   * @param semicolon the semicolon terminating the statement
   */
  EmptyStatement(this.semicolon);

  @override
  Token get beginToken => semicolon;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(semicolon);

  @override
  Token get endToken => semicolon;

  @override
  accept(AstVisitor visitor) => visitor.visitEmptyStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `EnumConstantDeclaration` represent the declaration of an enum
 * constant.
 */
class EnumConstantDeclaration extends Declaration {
  /**
   * The name of the constant.
   */
  SimpleIdentifier _name;

  /**
   * Initialize a newly created enum constant declaration.
   *
   * @param comment the documentation comment associated with this declaration
   * @param metadata the annotations associated with this declaration
   * @param name the name of the constant
   */
  EnumConstantDeclaration(Comment comment, List<Annotation> metadata,
      SimpleIdentifier name)
      : super(comment, metadata) {
    _name = becomeParentOf(name);
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => super._childEntities..add(_name);

  @override
  FieldElement get element =>
      _name == null ? null : (_name.staticElement as FieldElement);

  @override
  Token get endToken => _name.endToken;

  @override
  Token get firstTokenAfterCommentAndMetadata => _name.beginToken;

  /**
   * Return the name of the constant.
   *
   * @return the name of the constant
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the constant to the given name.
   *
   * @param name the name of the constant
   */
  void set name(SimpleIdentifier name) {
    _name = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitEnumConstantDeclaration(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_name, visitor);
  }
}

/**
 * Instances of the class `EnumDeclaration` represent the declaration of an enumeration.
 *
 * <pre>
 * enumType ::=
 *     metadata 'enum' [SimpleIdentifier] '{' [SimpleIdentifier] (',' [SimpleIdentifier])* (',')? '}'
 * </pre>
 */
class EnumDeclaration extends CompilationUnitMember {
  /**
   * The 'enum' keyword.
   */
  Token keyword;

  /**
   * The name of the enumeration.
   */
  SimpleIdentifier _name;

  /**
   * The left curly bracket.
   */
  Token leftBracket;

  /**
   * The enumeration constants being declared.
   */
  NodeList<EnumConstantDeclaration> _constants;

  /**
   * The right curly bracket.
   */
  Token rightBracket;

  /**
   * Initialize a newly created enumeration declaration.
   *
   * @param comment the documentation comment associated with this member
   * @param metadata the annotations associated with this member
   * @param keyword the 'enum' keyword
   * @param name the name of the enumeration
   * @param leftBracket the left curly bracket
   * @param constants the enumeration constants being declared
   * @param rightBracket the right curly bracket
   */
  EnumDeclaration(Comment comment, List<Annotation> metadata, this.keyword,
      SimpleIdentifier name, this.leftBracket,
      List<EnumConstantDeclaration> constants, this.rightBracket)
      : super(comment, metadata) {
    _name = becomeParentOf(name);
    _constants = new NodeList<EnumConstantDeclaration>(this, constants);
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => super._childEntities
      ..add(keyword)
      ..add(_name)
      ..add(leftBracket)
      ..addAll(_constants)
      ..add(rightBracket);

  /**
   * Return the enumeration constants being declared.
   *
   * @return the enumeration constants being declared
   */
  NodeList<EnumConstantDeclaration> get constants => _constants;

  @override
  ClassElement get element =>
      _name != null ? (_name.staticElement as ClassElement) : null;

  @override
  Token get endToken => rightBracket;

  @override
  Token get firstTokenAfterCommentAndMetadata => keyword;

  /**
   * Return the name of the enumeration.
   *
   * @return the name of the enumeration
   */
  SimpleIdentifier get name => _name;

  /**
   * set the name of the enumeration to the given identifier.
   *
   * @param name the name of the enumeration
   */
  void set name(SimpleIdentifier name) {
    _name = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitEnumDeclaration(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_name, visitor);
    _constants.accept(visitor);
  }
}

/**
 * Ephemeral identifiers are created as needed to mimic the presence of an empty identifier.
 */
class EphemeralIdentifier extends SimpleIdentifier {
  EphemeralIdentifier(AstNode parent, int location)
      : super(new StringToken(TokenType.IDENTIFIER, "", location)) {
    parent.becomeParentOf(this);
  }
}

/**
 * Instances of the class `ExportDirective` represent an export directive.
 *
 * <pre>
 * exportDirective ::=
 *     [Annotation] 'export' [StringLiteral] [Combinator]* ';'
 * </pre>
 */
class ExportDirective extends NamespaceDirective {
  /**
   * Initialize a newly created export directive.
   *
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param keyword the token representing the 'export' keyword
   * @param libraryUri the URI of the library being exported
   * @param combinators the combinators used to control which names are exported
   * @param semicolon the semicolon terminating the directive
   */
  ExportDirective(Comment comment, List<Annotation> metadata, Token keyword,
      StringLiteral libraryUri, List<Combinator> combinators, Token semicolon)
      : super(comment, metadata, keyword, libraryUri, combinators, semicolon);

  @override
  Iterable get childEntities => super._childEntities
      ..add(_uri)
      ..addAll(combinators)
      ..add(semicolon);

  @override
  ExportElement get element => super.element as ExportElement;

  @override
  LibraryElement get uriElement {
    if (element != null) {
      return element.exportedLibrary;
    }
    return null;
  }

  @override
  accept(AstVisitor visitor) => visitor.visitExportDirective(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    combinators.accept(visitor);
  }
}

/**
 * Instances of the class `Expression` defines the behavior common to nodes that represent an
 * expression.
 *
 * <pre>
 * expression ::=
 *     [AssignmentExpression]
 *   | [ConditionalExpression] cascadeSection*
 *   | [ThrowExpression]
 * </pre>
 */
abstract class Expression extends AstNode {
  /**
   * An empty list of expressions.
   */
  static const List<Expression> EMPTY_ARRAY = const <Expression>[];

  /**
   * The static type of this expression, or `null` if the AST structure has not been resolved.
   */
  DartType staticType;

  /**
   * The propagated type of this expression, or `null` if type propagation has not been
   * performed on the AST structure.
   */
  DartType propagatedType;

  /**
   * Return the best parameter element information available for this expression. If type
   * propagation was able to find a better parameter element than static analysis, that type will be
   * returned. Otherwise, the result of static analysis will be returned.
   *
   * @return the parameter element representing the parameter to which the value of this expression
   *         will be bound
   */
  ParameterElement get bestParameterElement {
    ParameterElement propagatedElement = propagatedParameterElement;
    if (propagatedElement != null) {
      return propagatedElement;
    }
    return staticParameterElement;
  }

  /**
   * Return the best type information available for this expression. If type propagation was able to
   * find a better type than static analysis, that type will be returned. Otherwise, the result of
   * static analysis will be returned. If no type analysis has been performed, then the type
   * 'dynamic' will be returned.
   *
   * @return the best type information available for this expression
   */
  DartType get bestType {
    if (propagatedType != null) {
      return propagatedType;
    } else if (staticType != null) {
      return staticType;
    }
    return DynamicTypeImpl.instance;
  }

  /**
   * Return `true` if this expression is syntactically valid for the LHS of an
   * [AssignmentExpression].
   *
   * @return `true` if this expression matches the `assignableExpression` production
   */
  bool get isAssignable => false;

  /**
   * Return the precedence of this expression. The precedence is a positive integer value that
   * defines how the source code is parsed into an AST. For example `a * b + c` is parsed as
   * `(a * b) + c` because the precedence of `*` is greater than the precedence of
   * `+`.
   *
   * You should not assume that returned values will stay the same, they might change as result of
   * specification change. Only relative order should be used.
   *
   * @return the precedence of this expression
   */
  int get precedence;

  /**
   * If this expression is an argument to an invocation, and the AST structure has been resolved,
   * and the function being invoked is known based on propagated type information, and this
   * expression corresponds to one of the parameters of the function being invoked, then return the
   * parameter element representing the parameter to which the value of this expression will be
   * bound. Otherwise, return `null`.
   *
   * @return the parameter element representing the parameter to which the value of this expression
   *         will be bound
   */
  ParameterElement get propagatedParameterElement {
    AstNode parent = this.parent;
    if (parent is ArgumentList) {
      return parent.getPropagatedParameterElementFor(this);
    } else if (parent is IndexExpression) {
      IndexExpression indexExpression = parent;
      if (identical(indexExpression.index, this)) {
        return indexExpression.propagatedParameterElementForIndex;
      }
    } else if (parent is BinaryExpression) {
      BinaryExpression binaryExpression = parent;
      if (identical(binaryExpression.rightOperand, this)) {
        return binaryExpression.propagatedParameterElementForRightOperand;
      }
    } else if (parent is AssignmentExpression) {
      AssignmentExpression assignmentExpression = parent;
      if (identical(assignmentExpression.rightHandSide, this)) {
        return assignmentExpression.propagatedParameterElementForRightHandSide;
      }
    } else if (parent is PrefixExpression) {
      return parent.propagatedParameterElementForOperand;
    } else if (parent is PostfixExpression) {
      return parent.propagatedParameterElementForOperand;
    }
    return null;
  }

  /**
   * If this expression is an argument to an invocation, and the AST structure has been resolved,
   * and the function being invoked is known based on static type information, and this expression
   * corresponds to one of the parameters of the function being invoked, then return the parameter
   * element representing the parameter to which the value of this expression will be bound.
   * Otherwise, return `null`.
   *
   * @return the parameter element representing the parameter to which the value of this expression
   *         will be bound
   */
  ParameterElement get staticParameterElement {
    AstNode parent = this.parent;
    if (parent is ArgumentList) {
      return parent.getStaticParameterElementFor(this);
    } else if (parent is IndexExpression) {
      IndexExpression indexExpression = parent;
      if (identical(indexExpression.index, this)) {
        return indexExpression.staticParameterElementForIndex;
      }
    } else if (parent is BinaryExpression) {
      BinaryExpression binaryExpression = parent;
      if (identical(binaryExpression.rightOperand, this)) {
        return binaryExpression.staticParameterElementForRightOperand;
      }
    } else if (parent is AssignmentExpression) {
      AssignmentExpression assignmentExpression = parent;
      if (identical(assignmentExpression.rightHandSide, this)) {
        return assignmentExpression.staticParameterElementForRightHandSide;
      }
    } else if (parent is PrefixExpression) {
      return parent.staticParameterElementForOperand;
    } else if (parent is PostfixExpression) {
      return parent.staticParameterElementForOperand;
    }
    return null;
  }
}

/**
 * Instances of the class `ExpressionFunctionBody` represent a function body consisting of a
 * single expression.
 *
 * <pre>
 * expressionFunctionBody ::=
 *     'async'? '=>' [Expression] ';'
 * </pre>
 */
class ExpressionFunctionBody extends FunctionBody {
  /**
   * The token representing the 'async' keyword, or `null` if there is no such keyword.
   */
  Token keyword;

  /**
   * The token introducing the expression that represents the body of the function.
   */
  Token functionDefinition;

  /**
   * The expression representing the body of the function.
   */
  Expression _expression;

  /**
   * The semicolon terminating the statement.
   */
  Token semicolon;

  /**
   * Initialize a newly created function body consisting of a block of statements.
   *
   * @param keyword the token representing the 'async' keyword
   * @param functionDefinition the token introducing the expression that represents the body of the
   *          function
   * @param expression the expression representing the body of the function
   * @param semicolon the semicolon terminating the statement
   */
  ExpressionFunctionBody(this.keyword, this.functionDefinition,
      Expression expression, this.semicolon) {
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken => functionDefinition;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(functionDefinition)
      ..add(_expression)
      ..add(semicolon);

  @override
  Token get endToken {
    if (semicolon != null) {
      return semicolon;
    }
    return _expression.endToken;
  }

  /**
   * Return the expression representing the body of the function.
   *
   * @return the expression representing the body of the function
   */
  Expression get expression => _expression;

  /**
   * Set the expression representing the body of the function to the given expression.
   *
   * @param expression the expression representing the body of the function
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  bool get isAsynchronous => keyword != null;

  @override
  bool get isSynchronous => keyword == null;

  @override
  accept(AstVisitor visitor) => visitor.visitExpressionFunctionBody(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * Instances of the class `ExpressionStatement` wrap an expression as a statement.
 *
 * <pre>
 * expressionStatement ::=
 *     [Expression]? ';'
 * </pre>
 */
class ExpressionStatement extends Statement {
  /**
   * The expression that comprises the statement.
   */
  Expression _expression;

  /**
   * The semicolon terminating the statement, or `null` if the expression is a function
   * expression and therefore isn't followed by a semicolon.
   */
  Token semicolon;

  /**
   * Initialize a newly created expression statement.
   *
   * @param expression the expression that comprises the statement
   * @param semicolon the semicolon terminating the statement
   */
  ExpressionStatement(Expression expression, this.semicolon) {
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken => _expression.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_expression)
      ..add(semicolon);

  @override
  Token get endToken {
    if (semicolon != null) {
      return semicolon;
    }
    return _expression.endToken;
  }

  /**
   * Return the expression that comprises the statement.
   *
   * @return the expression that comprises the statement
   */
  Expression get expression => _expression;

  /**
   * Set the expression that comprises the statement to the given expression.
   *
   * @param expression the expression that comprises the statement
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  bool get isSynthetic => _expression.isSynthetic && semicolon.isSynthetic;

  @override
  accept(AstVisitor visitor) => visitor.visitExpressionStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * Instances of the class `ExtendsClause` represent the "extends" clause in a class
 * declaration.
 *
 * <pre>
 * extendsClause ::=
 *     'extends' [TypeName]
 * </pre>
 */
class ExtendsClause extends AstNode {
  /**
   * The token representing the 'extends' keyword.
   */
  Token keyword;

  /**
   * The name of the class that is being extended.
   */
  TypeName _superclass;

  /**
   * Initialize a newly created extends clause.
   *
   * @param keyword the token representing the 'extends' keyword
   * @param superclass the name of the class that is being extended
   */
  ExtendsClause(this.keyword, TypeName superclass) {
    _superclass = becomeParentOf(superclass);
  }

  @override
  Token get beginToken => keyword;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(_superclass);

  @override
  Token get endToken => _superclass.endToken;

  /**
   * Return the name of the class that is being extended.
   *
   * @return the name of the class that is being extended
   */
  TypeName get superclass => _superclass;

  /**
   * Set the name of the class that is being extended to the given name.
   *
   * @param name the name of the class that is being extended
   */
  void set superclass(TypeName name) {
    _superclass = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitExtendsClause(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_superclass, visitor);
  }
}

/**
 * Instances of the class `FieldDeclaration` represent the declaration of one or more fields
 * of the same type.
 *
 * <pre>
 * fieldDeclaration ::=
 *     'static'? [VariableDeclarationList] ';'
 * </pre>
 */
class FieldDeclaration extends ClassMember {
  /**
   * The token representing the 'static' keyword, or `null` if the fields are not static.
   */
  Token staticKeyword;

  /**
   * The fields being declared.
   */
  VariableDeclarationList _fieldList;

  /**
   * The semicolon terminating the declaration.
   */
  Token semicolon;

  /**
   * Initialize a newly created field declaration.
   *
   * @param comment the documentation comment associated with this field
   * @param metadata the annotations associated with this field
   * @param staticKeyword the token representing the 'static' keyword
   * @param fieldList the fields being declared
   * @param semicolon the semicolon terminating the declaration
   */
  FieldDeclaration(Comment comment, List<Annotation> metadata,
      this.staticKeyword, VariableDeclarationList fieldList, this.semicolon)
      : super(comment, metadata) {
    _fieldList = becomeParentOf(fieldList);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(staticKeyword)
      ..add(_fieldList)
      ..add(semicolon);

  @override
  Element get element => null;

  @override
  Token get endToken => semicolon;

  /**
   * Return the fields being declared.
   *
   * @return the fields being declared
   */
  VariableDeclarationList get fields => _fieldList;

  /**
   * Set the fields being declared to the given list of variables.
   *
   * @param fieldList the fields being declared
   */
  void set fields(VariableDeclarationList fieldList) {
    _fieldList = becomeParentOf(fieldList);
  }

  @override
  Token get firstTokenAfterCommentAndMetadata {
    if (staticKeyword != null) {
      return staticKeyword;
    }
    return _fieldList.beginToken;
  }

  /**
   * Return `true` if the fields are static.
   *
   * @return `true` if the fields are declared to be static
   */
  bool get isStatic => staticKeyword != null;

  @override
  accept(AstVisitor visitor) => visitor.visitFieldDeclaration(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_fieldList, visitor);
  }
}

/**
 * Instances of the class `FieldFormalParameter` represent a field formal parameter.
 *
 * <pre>
 * fieldFormalParameter ::=
 *     ('final' [TypeName] | 'const' [TypeName] | 'var' | [TypeName])? 'this' '.' [SimpleIdentifier] [FormalParameterList]?
 * </pre>
 */
class FieldFormalParameter extends NormalFormalParameter {
  /**
   * The token representing either the 'final', 'const' or 'var' keyword, or `null` if no
   * keyword was used.
   */
  Token keyword;

  /**
   * The name of the declared type of the parameter, or `null` if the parameter does not have
   * a declared type.
   */
  TypeName _type;

  /**
   * The token representing the 'this' keyword.
   */
  Token thisToken;

  /**
   * The token representing the period.
   */
  Token period;

  /**
   * The parameters of the function-typed parameter, or `null` if this is not a function-typed
   * field formal parameter.
   */
  FormalParameterList _parameters;

  /**
   * Initialize a newly created formal parameter.
   *
   * @param comment the documentation comment associated with this parameter
   * @param metadata the annotations associated with this parameter
   * @param keyword the token representing either the 'final', 'const' or 'var' keyword
   * @param type the name of the declared type of the parameter
   * @param thisToken the token representing the 'this' keyword
   * @param period the token representing the period
   * @param identifier the name of the parameter being declared
   * @param parameters the parameters of the function-typed parameter, or `null` if this is
   *          not a function-typed field formal parameter
   */
  FieldFormalParameter(Comment comment, List<Annotation> metadata, this.keyword,
      TypeName type, this.thisToken, this.period, SimpleIdentifier identifier,
      FormalParameterList parameters)
      : super(comment, metadata, identifier) {
    _type = becomeParentOf(type);
    _parameters = becomeParentOf(parameters);
  }

  @override
  Token get beginToken {
    if (keyword != null) {
      return keyword;
    } else if (_type != null) {
      return _type.beginToken;
    }
    return thisToken;
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(keyword)
      ..add(_type)
      ..add(thisToken)
      ..add(period)
      ..add(identifier)
      ..add(_parameters);

  @override
  Token get endToken {
    if (_parameters != null) {
      return _parameters.endToken;
    }
    return identifier.endToken;
  }

  @override
  bool get isConst =>
      (keyword is KeywordToken) && (keyword as KeywordToken).keyword == Keyword.CONST;

  @override
  bool get isFinal =>
      (keyword is KeywordToken) && (keyword as KeywordToken).keyword == Keyword.FINAL;

  /**
   * Return the parameters of the function-typed parameter, or `null` if this is not a
   * function-typed field formal parameter.
   *
   * @return the parameters of the function-typed parameter
   */
  FormalParameterList get parameters => _parameters;

  /**
   * Set the parameters of the function-typed parameter to the given parameters.
   *
   * @param parameters the parameters of the function-typed parameter
   */
  void set parameters(FormalParameterList parameters) {
    _parameters = becomeParentOf(parameters);
  }

  /**
   * Return the name of the declared type of the parameter, or `null` if the parameter does
   * not have a declared type. Note that if this is a function-typed field formal parameter this is
   * the return type of the function.
   *
   * @return the name of the declared type of the parameter
   */
  TypeName get type => _type;

  /**
   * Set the name of the declared type of the parameter to the given type name.
   *
   * @param typeName the name of the declared type of the parameter
   */
  void set type(TypeName typeName) {
    _type = becomeParentOf(typeName);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitFieldFormalParameter(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_type, visitor);
    safelyVisitChild(identifier, visitor);
    safelyVisitChild(_parameters, visitor);
  }
}

/**
 * Instances of the class `ForEachStatement` represent a for-each statement.
 *
 * <pre>
 * forEachStatement ::=
 *     'await'? 'for' '(' [DeclaredIdentifier] 'in' [Expression] ')' [Block]
 *   | 'await'? 'for' '(' [SimpleIdentifier] 'in' [Expression] ')' [Block]
 * </pre>
 */
class ForEachStatement extends Statement {
  /**
   * The token representing the 'await' keyword, or `null` if there is no 'await' keyword.
   */
  Token awaitKeyword;

  /**
   * The token representing the 'for' keyword.
   */
  Token forKeyword;

  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The declaration of the loop variable, or `null` if the loop variable is a simple
   * identifier.
   */
  DeclaredIdentifier _loopVariable;

  /**
   * The loop variable, or `null` if the loop variable is declared in the 'for'.
   */
  SimpleIdentifier _identifier;

  /**
   * The token representing the 'in' keyword.
   */
  Token inKeyword;

  /**
   * The expression evaluated to produce the iterator.
   */
  Expression _iterable;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * The body of the loop.
   */
  Statement _body;

  /**
   * Initialize a newly created for-each statement.
   *
   * @param awaitKeyword the token representing the 'await' keyword
   * @param forKeyword the token representing the 'for' keyword
   * @param leftParenthesis the left parenthesis
   * @param loopVariable the declaration of the loop variable
   * @param iterator the expression evaluated to produce the iterator
   * @param rightParenthesis the right parenthesis
   * @param body the body of the loop
   */
  ForEachStatement.con1(this.awaitKeyword, this.forKeyword,
      this.leftParenthesis, DeclaredIdentifier loopVariable, this.inKeyword,
      Expression iterator, this.rightParenthesis, Statement body) {
    _loopVariable = becomeParentOf(loopVariable);
    _iterable = becomeParentOf(iterator);
    _body = becomeParentOf(body);
  }

  /**
   * Initialize a newly created for-each statement.
   *
   * @param awaitKeyword the token representing the 'await' keyword
   * @param forKeyword the token representing the 'for' keyword
   * @param leftParenthesis the left parenthesis
   * @param identifier the loop variable
   * @param iterator the expression evaluated to produce the iterator
   * @param rightParenthesis the right parenthesis
   * @param body the body of the loop
   */
  ForEachStatement.con2(this.awaitKeyword, this.forKeyword,
      this.leftParenthesis, SimpleIdentifier identifier, this.inKeyword,
      Expression iterator, this.rightParenthesis, Statement body) {
    _identifier = becomeParentOf(identifier);
    _iterable = becomeParentOf(iterator);
    _body = becomeParentOf(body);
  }

  @override
  Token get beginToken => forKeyword;

  /**
   * Return the body of the loop.
   *
   * @return the body of the loop
   */
  Statement get body => _body;

  /**
   * Set the body of the loop to the given block.
   *
   * @param body the body of the loop
   */
  void set body(Statement body) {
    _body = becomeParentOf(body);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(awaitKeyword)
      ..add(forKeyword)
      ..add(leftParenthesis)
      ..add(_loopVariable)
      ..add(_identifier)
      ..add(inKeyword)
      ..add(_iterable)
      ..add(rightParenthesis)
      ..add(_body);

  @override
  Token get endToken => _body.endToken;

  /**
   * Return the loop variable, or `null` if the loop variable is declared in the 'for'.
   *
   * @return the loop variable
   */
  SimpleIdentifier get identifier => _identifier;

  /**
   * Set the loop variable to the given variable.
   *
   * @param identifier the loop variable
   */
  void set identifier(SimpleIdentifier identifier) {
    _identifier = becomeParentOf(identifier);
  }

  /**
   * Return the expression evaluated to produce the iterator.
   *
   * @return the expression evaluated to produce the iterator
   */
  Expression get iterable => _iterable;

  /**
   * Set the expression evaluated to produce the iterator to the given expression.
   *
   * @param expression the expression evaluated to produce the iterator
   */
  void set iterable(Expression expression) {
    _iterable = becomeParentOf(expression);
  }

  /**
   * Return the expression evaluated to produce the iterator.
   *
   * Deprecated, use [iterable] instead.
   */
  @deprecated
  Expression get iterator => iterable;

  /**
   * Return the declaration of the loop variable, or `null` if the loop variable is a simple
   * identifier.
   *
   * @return the declaration of the loop variable
   */
  DeclaredIdentifier get loopVariable => _loopVariable;

  /**
   * Set the declaration of the loop variable to the given variable.
   *
   * @param variable the declaration of the loop variable
   */
  void set loopVariable(DeclaredIdentifier variable) {
    _loopVariable = becomeParentOf(variable);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitForEachStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_loopVariable, visitor);
    safelyVisitChild(_identifier, visitor);
    safelyVisitChild(_iterable, visitor);
    safelyVisitChild(_body, visitor);
  }
}

/**
 * The abstract class `FormalParameter` defines the behavior of objects representing a
 * parameter to a function.
 *
 * <pre>
 * formalParameter ::=
 *     [NormalFormalParameter]
 *   | [DefaultFormalParameter]
 * </pre>
 */
abstract class FormalParameter extends AstNode {
  /**
   * Return the element representing this parameter, or `null` if this parameter has not been
   * resolved.
   *
   * @return the element representing this parameter
   */
  ParameterElement get element {
    SimpleIdentifier identifier = this.identifier;
    if (identifier == null) {
      return null;
    }
    return identifier.staticElement as ParameterElement;
  }

  /**
   * Return the name of the parameter being declared.
   *
   * @return the name of the parameter being declared
   */
  SimpleIdentifier get identifier;

  /**
   * Return `true` if this parameter was declared with the 'const' modifier.
   *
   * @return `true` if this parameter was declared with the 'const' modifier
   */
  bool get isConst;

  /**
   * Return `true` if this parameter was declared with the 'final' modifier. Parameters that
   * are declared with the 'const' modifier will return `false` even though they are
   * implicitly final.
   *
   * @return `true` if this parameter was declared with the 'final' modifier
   */
  bool get isFinal;

  /**
   * Return the kind of this parameter.
   *
   * @return the kind of this parameter
   */
  ParameterKind get kind;
}

/**
 * Instances of the class `FormalParameterList` represent the formal parameter list of a
 * method declaration, function declaration, or function type alias.
 *
 * While the grammar requires all optional formal parameters to follow all of the normal formal
 * parameters and at most one grouping of optional formal parameters, this class does not enforce
 * those constraints. All parameters are flattened into a single list, which can have any or all
 * kinds of parameters (normal, named, and positional) in any order.
 *
 * <pre>
 * formalParameterList ::=
 *     '(' ')'
 *   | '(' normalFormalParameters (',' optionalFormalParameters)? ')'
 *   | '(' optionalFormalParameters ')'
 *
 * normalFormalParameters ::=
 *     [NormalFormalParameter] (',' [NormalFormalParameter])*
 *
 * optionalFormalParameters ::=
 *     optionalPositionalFormalParameters
 *   | namedFormalParameters
 *
 * optionalPositionalFormalParameters ::=
 *     '[' [DefaultFormalParameter] (',' [DefaultFormalParameter])* ']'
 *
 * namedFormalParameters ::=
 *     '{' [DefaultFormalParameter] (',' [DefaultFormalParameter])* '}'
 * </pre>
 */
class FormalParameterList extends AstNode {
  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The parameters associated with the method.
   */
  NodeList<FormalParameter> _parameters;

  /**
   * The left square bracket ('[') or left curly brace ('{') introducing the optional parameters, or
   * `null` if there are no optional parameters.
   */
  Token leftDelimiter;

  /**
   * The right square bracket (']') or right curly brace ('}') introducing the optional parameters,
   * or `null` if there are no optional parameters.
   */
  Token rightDelimiter;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * Initialize a newly created parameter list.
   *
   * @param leftParenthesis the left parenthesis
   * @param parameters the parameters associated with the method
   * @param leftDelimiter the left delimiter introducing the optional parameters
   * @param rightDelimiter the right delimiter introducing the optional parameters
   * @param rightParenthesis the right parenthesis
   */
  FormalParameterList(this.leftParenthesis, List<FormalParameter> parameters,
      this.leftDelimiter, this.rightDelimiter, this.rightParenthesis) {
    _parameters = new NodeList<FormalParameter>(this, parameters);
  }

  @override
  Token get beginToken => leftParenthesis;

  @override
  Iterable get childEntities {
    // TODO(paulberry): include commas.
    ChildEntities result = new ChildEntities()..add(leftParenthesis);
    bool leftDelimiterNeeded = leftDelimiter != null;
    for (FormalParameter parameter in _parameters) {
      if (leftDelimiterNeeded && leftDelimiter.offset < parameter.offset) {
        result.add(leftDelimiter);
        leftDelimiterNeeded = false;
      }
      result.add(parameter);
    }
    return result
        ..add(rightDelimiter)
        ..add(rightParenthesis);
  }

  @override
  Token get endToken => rightParenthesis;

  /**
   * Return an array containing the elements representing the parameters in this list. The array
   * will contain `null`s if the parameters in this list have not been resolved.
   *
   * @return the elements representing the parameters in this list
   */
  List<ParameterElement> get parameterElements {
    int count = _parameters.length;
    List<ParameterElement> types = new List<ParameterElement>(count);
    for (int i = 0; i < count; i++) {
      types[i] = _parameters[i].element;
    }
    return types;
  }

  /**
   * Return the parameters associated with the method.
   *
   * @return the parameters associated with the method
   */
  NodeList<FormalParameter> get parameters => _parameters;

  @override
  accept(AstVisitor visitor) => visitor.visitFormalParameterList(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _parameters.accept(visitor);
  }
}

/**
 * Instances of the class `ForStatement` represent a for statement.
 *
 * <pre>
 * forStatement ::=
 *     'for' '(' forLoopParts ')' [Statement]
 *
 * forLoopParts ::=
 *     forInitializerStatement ';' [Expression]? ';' [Expression]?
 *
 * forInitializerStatement ::=
 *     [DefaultFormalParameter]
 *   | [Expression]?
 * </pre>
 */
class ForStatement extends Statement {
  /**
   * The token representing the 'for' keyword.
   */
  Token forKeyword;

  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The declaration of the loop variables, or `null` if there are no variables. Note that a
   * for statement cannot have both a variable list and an initialization expression, but can
   * validly have neither.
   */
  VariableDeclarationList _variableList;

  /**
   * The initialization expression, or `null` if there is no initialization expression. Note
   * that a for statement cannot have both a variable list and an initialization expression, but can
   * validly have neither.
   */
  Expression _initialization;

  /**
   * The semicolon separating the initializer and the condition.
   */
  Token leftSeparator;

  /**
   * The condition used to determine when to terminate the loop, or `null` if there is no
   * condition.
   */
  Expression _condition;

  /**
   * The semicolon separating the condition and the updater.
   */
  Token rightSeparator;

  /**
   * The list of expressions run after each execution of the loop body.
   */
  NodeList<Expression> _updaters;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * The body of the loop.
   */
  Statement _body;

  /**
   * Initialize a newly created for statement.
   *
   * @param forKeyword the token representing the 'for' keyword
   * @param leftParenthesis the left parenthesis
   * @param variableList the declaration of the loop variables
   * @param initialization the initialization expression
   * @param leftSeparator the semicolon separating the initializer and the condition
   * @param condition the condition used to determine when to terminate the loop
   * @param rightSeparator the semicolon separating the condition and the updater
   * @param updaters the list of expressions run after each execution of the loop body
   * @param rightParenthesis the right parenthesis
   * @param body the body of the loop
   */
  ForStatement(this.forKeyword, this.leftParenthesis,
      VariableDeclarationList variableList, Expression initialization,
      this.leftSeparator, Expression condition, this.rightSeparator,
      List<Expression> updaters, this.rightParenthesis, Statement body) {
    _variableList = becomeParentOf(variableList);
    _initialization = becomeParentOf(initialization);
    _condition = becomeParentOf(condition);
    _updaters = new NodeList<Expression>(this, updaters);
    _body = becomeParentOf(body);
  }

  @override
  Token get beginToken => forKeyword;

  /**
   * Return the body of the loop.
   *
   * @return the body of the loop
   */
  Statement get body => _body;

  /**
   * Set the body of the loop to the given statement.
   *
   * @param body the body of the loop
   */
  void set body(Statement body) {
    _body = becomeParentOf(body);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(forKeyword)
      ..add(leftParenthesis)
      ..add(_variableList)
      ..add(_initialization)
      ..add(leftSeparator)
      ..add(_condition)
      ..add(rightSeparator)
      ..addAll(_updaters)
      ..add(rightParenthesis)
      ..add(_body);

  /**
   * Return the condition used to determine when to terminate the loop, or `null` if there is
   * no condition.
   *
   * @return the condition used to determine when to terminate the loop
   */
  Expression get condition => _condition;

  /**
   * Set the condition used to determine when to terminate the loop to the given expression.
   *
   * @param expression the condition used to determine when to terminate the loop
   */
  void set condition(Expression expression) {
    _condition = becomeParentOf(expression);
  }

  @override
  Token get endToken => _body.endToken;

  /**
   * Return the initialization expression, or `null` if there is no initialization expression.
   *
   * @return the initialization expression
   */
  Expression get initialization => _initialization;

  /**
   * Set the initialization expression to the given expression.
   *
   * @param initialization the initialization expression
   */
  void set initialization(Expression initialization) {
    _initialization = becomeParentOf(initialization);
  }

  /**
   * Return the list of expressions run after each execution of the loop body.
   *
   * @return the list of expressions run after each execution of the loop body
   */
  NodeList<Expression> get updaters => _updaters;

  /**
   * Return the declaration of the loop variables, or `null` if there are no variables.
   *
   * @return the declaration of the loop variables, or `null` if there are no variables
   */
  VariableDeclarationList get variables => _variableList;

  /**
   * Set the declaration of the loop variables to the given parameter.
   *
   * @param variableList the declaration of the loop variables
   */
  void set variables(VariableDeclarationList variableList) {
    _variableList = becomeParentOf(variableList);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitForStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_variableList, visitor);
    safelyVisitChild(_initialization, visitor);
    safelyVisitChild(_condition, visitor);
    _updaters.accept(visitor);
    safelyVisitChild(_body, visitor);
  }
}

/**
 * The abstract class `FunctionBody` defines the behavior common to objects representing the
 * body of a function or method.
 *
 * <pre>
 * functionBody ::=
 *     [BlockFunctionBody]
 *   | [EmptyFunctionBody]
 *   | [ExpressionFunctionBody]
 * </pre>
 */
abstract class FunctionBody extends AstNode {
  /**
   * Return `true` if this function body is asynchronous.
   *
   * @return `true` if this function body is asynchronous
   */
  bool get isAsynchronous => false;

  /**
   * Return `true` if this function body is a generator.
   *
   * @return `true` if this function body is a generator
   */
  bool get isGenerator => false;

  /**
   * Return `true` if this function body is synchronous.
   *
   * @return `true` if this function body is synchronous
   */
  bool get isSynchronous => true;

  /**
   * Return the token representing the 'async' or 'sync' keyword, or `null` if there is no
   * such keyword.
   *
   * @return the token representing the 'async' or 'sync' keyword
   */
  Token get keyword => null;

  /**
   * Return the star following the 'async' or 'sync' keyword, or `null` if there is no star.
   *
   * @return the star following the 'async' or 'sync' keyword
   */
  Token get star => null;
}

/**
 * Instances of the class `FunctionDeclaration` wrap a [FunctionExpression] as a top-level declaration.
 *
 * <pre>
 * functionDeclaration ::=
 *     'external' functionSignature
 *   | functionSignature [FunctionBody]
 *
 * functionSignature ::=
 *     [Type]? ('get' | 'set')? [SimpleIdentifier] [FormalParameterList]
 * </pre>
 */
class FunctionDeclaration extends CompilationUnitMember {
  /**
   * The token representing the 'external' keyword, or `null` if this is not an external
   * function.
   */
  Token externalKeyword;

  /**
   * The return type of the function, or `null` if no return type was declared.
   */
  TypeName _returnType;

  /**
   * The token representing the 'get' or 'set' keyword, or `null` if this is a function
   * declaration rather than a property declaration.
   */
  Token propertyKeyword;

  /**
   * The name of the function, or `null` if the function is not named.
   */
  SimpleIdentifier _name;

  /**
   * The function expression being wrapped.
   */
  FunctionExpression _functionExpression;

  /**
   * Initialize a newly created function declaration.
   *
   * @param comment the documentation comment associated with this function
   * @param metadata the annotations associated with this function
   * @param externalKeyword the token representing the 'external' keyword
   * @param returnType the return type of the function
   * @param propertyKeyword the token representing the 'get' or 'set' keyword
   * @param name the name of the function
   * @param functionExpression the function expression being wrapped
   */
  FunctionDeclaration(Comment comment, List<Annotation> metadata,
      this.externalKeyword, TypeName returnType, this.propertyKeyword,
      SimpleIdentifier name, FunctionExpression functionExpression)
      : super(comment, metadata) {
    _returnType = becomeParentOf(returnType);
    _name = becomeParentOf(name);
    _functionExpression = becomeParentOf(functionExpression);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(externalKeyword)
      ..add(_returnType)
      ..add(propertyKeyword)
      ..add(_name)
      ..add(_functionExpression);

  @override
  ExecutableElement get element =>
      _name != null ? (_name.staticElement as ExecutableElement) : null;

  @override
  Token get endToken => _functionExpression.endToken;

  @override
  Token get firstTokenAfterCommentAndMetadata {
    if (externalKeyword != null) {
      return externalKeyword;
    } else if (_returnType != null) {
      return _returnType.beginToken;
    } else if (propertyKeyword != null) {
      return propertyKeyword;
    } else if (_name != null) {
      return _name.beginToken;
    }
    return _functionExpression.beginToken;
  }

  /**
   * Return the function expression being wrapped.
   *
   * @return the function expression being wrapped
   */
  FunctionExpression get functionExpression => _functionExpression;

  /**
   * Set the function expression being wrapped to the given function expression.
   *
   * @param functionExpression the function expression being wrapped
   */
  void set functionExpression(FunctionExpression functionExpression) {
    _functionExpression = becomeParentOf(functionExpression);
  }

  /**
   * Return `true` if this function declares a getter.
   *
   * @return `true` if this function declares a getter
   */
  bool get isGetter =>
      propertyKeyword != null &&
          (propertyKeyword as KeywordToken).keyword == Keyword.GET;

  /**
   * Return `true` if this function declares a setter.
   *
   * @return `true` if this function declares a setter
   */
  bool get isSetter =>
      propertyKeyword != null &&
          (propertyKeyword as KeywordToken).keyword == Keyword.SET;

  /**
   * Return the name of the function, or `null` if the function is not named.
   *
   * @return the name of the function
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the function to the given identifier.
   *
   * @param identifier the name of the function
   */
  void set name(SimpleIdentifier identifier) {
    _name = becomeParentOf(identifier);
  }

  /**
   * Return the return type of the function, or `null` if no return type was declared.
   *
   * @return the return type of the function
   */
  TypeName get returnType => _returnType;

  /**
   * Set the return type of the function to the given name.
   *
   * @param returnType the return type of the function
   */
  void set returnType(TypeName returnType) {
    _returnType = becomeParentOf(returnType);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitFunctionDeclaration(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_returnType, visitor);
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_functionExpression, visitor);
  }
}

/**
 * Instances of the class `FunctionDeclarationStatement` wrap a [FunctionDeclaration
 ] as a statement.
 */
class FunctionDeclarationStatement extends Statement {
  /**
   * The function declaration being wrapped.
   */
  FunctionDeclaration _functionDeclaration;

  /**
   * Initialize a newly created function declaration statement.
   *
   * @param functionDeclaration the the function declaration being wrapped
   */
  FunctionDeclarationStatement(FunctionDeclaration functionDeclaration) {
    _functionDeclaration = becomeParentOf(functionDeclaration);
  }

  @override
  Token get beginToken => _functionDeclaration.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()..add(_functionDeclaration);

  @override
  Token get endToken => _functionDeclaration.endToken;

  /**
   * Return the function declaration being wrapped.
   *
   * @return the function declaration being wrapped
   */
  FunctionDeclaration get functionDeclaration => _functionDeclaration;

  /**
   * Set the function declaration being wrapped to the given function declaration.
   *
   * @param functionDeclaration the function declaration being wrapped
   */
  void set functionDeclaration(FunctionDeclaration functionDeclaration) {
    _functionDeclaration = becomeParentOf(functionDeclaration);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitFunctionDeclarationStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_functionDeclaration, visitor);
  }
}

/**
 * Instances of the class `FunctionExpression` represent a function expression.
 *
 * <pre>
 * functionExpression ::=
 *     [FormalParameterList] [FunctionBody]
 * </pre>
 */
class FunctionExpression extends Expression {
  /**
   * The parameters associated with the function.
   */
  FormalParameterList _parameters;

  /**
   * The body of the function, or `null` if this is an external function.
   */
  FunctionBody _body;

  /**
   * The element associated with the function, or `null` if the AST structure has not been
   * resolved.
   */
  ExecutableElement element;

  /**
   * Initialize a newly created function declaration.
   *
   * @param parameters the parameters associated with the function
   * @param body the body of the function
   */
  FunctionExpression(FormalParameterList parameters, FunctionBody body) {
    _parameters = becomeParentOf(parameters);
    _body = becomeParentOf(body);
  }

  @override
  Token get beginToken {
    if (_parameters != null) {
      return _parameters.beginToken;
    } else if (_body != null) {
      return _body.beginToken;
    }
    // This should never be reached because external functions must be named,
    // hence either the body or the name should be non-null.
    throw new IllegalStateException("Non-external functions must have a body");
  }

  /**
   * Return the body of the function, or `null` if this is an external function.
   *
   * @return the body of the function
   */
  FunctionBody get body => _body;

  /**
   * Set the body of the function to the given function body.
   *
   * @param functionBody the body of the function
   */
  void set body(FunctionBody functionBody) {
    _body = becomeParentOf(functionBody);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_parameters)
      ..add(_body);

  @override
  Token get endToken {
    if (_body != null) {
      return _body.endToken;
    } else if (_parameters != null) {
      return _parameters.endToken;
    }
    // This should never be reached because external functions must be named,
    // hence either the body or the name should be non-null.
    throw new IllegalStateException("Non-external functions must have a body");
  }

  /**
   * Return the parameters associated with the function.
   *
   * @return the parameters associated with the function
   */
  FormalParameterList get parameters => _parameters;

  /**
   * Set the parameters associated with the function to the given list of parameters.
   *
   * @param parameters the parameters associated with the function
   */
  void set parameters(FormalParameterList parameters) {
    _parameters = becomeParentOf(parameters);
  }

  @override
  int get precedence => 16;

  @override
  accept(AstVisitor visitor) => visitor.visitFunctionExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_parameters, visitor);
    safelyVisitChild(_body, visitor);
  }
}

/**
 * Instances of the class `FunctionExpressionInvocation` represent the invocation of a
 * function resulting from evaluating an expression. Invocations of methods and other forms of
 * functions are represented by [MethodInvocation] nodes. Invocations of
 * getters and setters are represented by either [PrefixedIdentifier] or
 * [PropertyAccess] nodes.
 *
 * <pre>
 * functionExpressionInvoction ::=
 *     [Expression] [ArgumentList]
 * </pre>
 */
class FunctionExpressionInvocation extends Expression {
  /**
   * The expression producing the function being invoked.
   */
  Expression _function;

  /**
   * The list of arguments to the function.
   */
  ArgumentList _argumentList;

  /**
   * The element associated with the function being invoked based on static type information, or
   * `null` if the AST structure has not been resolved or the function could not be resolved.
   */
  ExecutableElement staticElement;

  /**
   * The element associated with the function being invoked based on propagated type information, or
   * `null` if the AST structure has not been resolved or the function could not be resolved.
   */
  ExecutableElement propagatedElement;

  /**
   * Initialize a newly created function expression invocation.
   *
   * @param function the expression producing the function being invoked
   * @param argumentList the list of arguments to the method
   */
  FunctionExpressionInvocation(Expression function, ArgumentList argumentList) {
    _function = becomeParentOf(function);
    _argumentList = becomeParentOf(argumentList);
  }

  /**
   * Return the list of arguments to the method.
   *
   * @return the list of arguments to the method
   */
  ArgumentList get argumentList => _argumentList;

  /**
   * Set the list of arguments to the method to the given list.
   *
   * @param argumentList the list of arguments to the method
   */
  void set argumentList(ArgumentList argumentList) {
    _argumentList = becomeParentOf(argumentList);
  }

  @override
  Token get beginToken => _function.beginToken;

  /**
   * Return the best element available for the function being invoked. If resolution was able to
   * find a better element based on type propagation, that element will be returned. Otherwise, the
   * element found using the result of static analysis will be returned. If resolution has not been
   * performed, then `null` will be returned.
   *
   * @return the best element available for this function
   */
  ExecutableElement get bestElement {
    ExecutableElement element = propagatedElement;
    if (element == null) {
      element = staticElement;
    }
    return element;
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_function)
      ..add(_argumentList);

  @override
  Token get endToken => _argumentList.endToken;

  /**
   * Return the expression producing the function being invoked.
   *
   * @return the expression producing the function being invoked
   */
  Expression get function => _function;

  /**
   * Set the expression producing the function being invoked to the given expression.
   *
   * @param function the expression producing the function being invoked
   */
  void set function(Expression function) {
    _function = becomeParentOf(function);
  }

  @override
  int get precedence => 15;

  @override
  accept(AstVisitor visitor) => visitor.visitFunctionExpressionInvocation(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_function, visitor);
    safelyVisitChild(_argumentList, visitor);
  }
}

/**
 * Instances of the class `FunctionTypeAlias` represent a function type alias.
 *
 * <pre>
 * functionTypeAlias ::=
 *      functionPrefix [TypeParameterList]? [FormalParameterList] ';'
 *
 * functionPrefix ::=
 *     [TypeName]? [SimpleIdentifier]
 * </pre>
 */
class FunctionTypeAlias extends TypeAlias {
  /**
   * The name of the return type of the function type being defined, or `null` if no return
   * type was given.
   */
  TypeName _returnType;

  /**
   * The name of the function type being declared.
   */
  SimpleIdentifier _name;

  /**
   * The type parameters for the function type, or `null` if the function type does not have
   * any type parameters.
   */
  TypeParameterList _typeParameters;

  /**
   * The parameters associated with the function type.
   */
  FormalParameterList _parameters;

  /**
   * Initialize a newly created function type alias.
   *
   * @param comment the documentation comment associated with this type alias
   * @param metadata the annotations associated with this type alias
   * @param keyword the token representing the 'typedef' keyword
   * @param returnType the name of the return type of the function type being defined
   * @param name the name of the type being declared
   * @param typeParameters the type parameters for the type
   * @param parameters the parameters associated with the function
   * @param semicolon the semicolon terminating the declaration
   */
  FunctionTypeAlias(Comment comment, List<Annotation> metadata, Token keyword,
      TypeName returnType, SimpleIdentifier name, TypeParameterList typeParameters,
      FormalParameterList parameters, Token semicolon)
      : super(comment, metadata, keyword, semicolon) {
    _returnType = becomeParentOf(returnType);
    _name = becomeParentOf(name);
    _typeParameters = becomeParentOf(typeParameters);
    _parameters = becomeParentOf(parameters);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(keyword)
      ..add(_returnType)
      ..add(_name)
      ..add(_typeParameters)
      ..add(_parameters)
      ..add(semicolon);

  @override
  FunctionTypeAliasElement get element =>
      _name != null ? (_name.staticElement as FunctionTypeAliasElement) : null;

  /**
   * Return the name of the function type being declared.
   *
   * @return the name of the function type being declared
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the function type being declared to the given identifier.
   *
   * @param name the name of the function type being declared
   */
  void set name(SimpleIdentifier name) {
    _name = becomeParentOf(name);
  }

  /**
   * Return the parameters associated with the function type.
   *
   * @return the parameters associated with the function type
   */
  FormalParameterList get parameters => _parameters;

  /**
   * Set the parameters associated with the function type to the given list of parameters.
   *
   * @param parameters the parameters associated with the function type
   */
  void set parameters(FormalParameterList parameters) {
    _parameters = becomeParentOf(parameters);
  }

  /**
   * Return the name of the return type of the function type being defined, or `null` if no
   * return type was given.
   *
   * @return the name of the return type of the function type being defined
   */
  TypeName get returnType => _returnType;

  /**
   * Set the name of the return type of the function type being defined to the given type name.
   *
   * @param typeName the name of the return type of the function type being defined
   */
  void set returnType(TypeName typeName) {
    _returnType = becomeParentOf(typeName);
  }

  /**
   * Return the type parameters for the function type, or `null` if the function type does not
   * have any type parameters.
   *
   * @return the type parameters for the function type
   */
  TypeParameterList get typeParameters => _typeParameters;

  /**
   * Set the type parameters for the function type to the given list of parameters.
   *
   * @param typeParameters the type parameters for the function type
   */
  void set typeParameters(TypeParameterList typeParameters) {
    _typeParameters = becomeParentOf(typeParameters);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitFunctionTypeAlias(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_returnType, visitor);
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_typeParameters, visitor);
    safelyVisitChild(_parameters, visitor);
  }
}

/**
 * Instances of the class `FunctionTypedFormalParameter` represent a function-typed formal
 * parameter.
 *
 * <pre>
 * functionSignature ::=
 *     [TypeName]? [SimpleIdentifier] [FormalParameterList]
 * </pre>
 */
class FunctionTypedFormalParameter extends NormalFormalParameter {
  /**
   * The return type of the function, or `null` if the function does not have a return type.
   */
  TypeName _returnType;

  /**
   * The parameters of the function-typed parameter.
   */
  FormalParameterList _parameters;

  /**
   * Initialize a newly created formal parameter.
   *
   * @param comment the documentation comment associated with this parameter
   * @param metadata the annotations associated with this parameter
   * @param returnType the return type of the function, or `null` if the function does not
   *          have a return type
   * @param identifier the name of the function-typed parameter
   * @param parameters the parameters of the function-typed parameter
   */
  FunctionTypedFormalParameter(Comment comment, List<Annotation> metadata,
      TypeName returnType, SimpleIdentifier identifier,
      FormalParameterList parameters)
      : super(comment, metadata, identifier) {
    _returnType = becomeParentOf(returnType);
    _parameters = becomeParentOf(parameters);
  }

  @override
  Token get beginToken {
    if (_returnType != null) {
      return _returnType.beginToken;
    }
    return identifier.beginToken;
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => super._childEntities
      ..add(_returnType)
      ..add(identifier)
      ..add(parameters);

  @override
  Token get endToken => _parameters.endToken;

  @override
  bool get isConst => false;

  @override
  bool get isFinal => false;

  /**
   * Return the parameters of the function-typed parameter.
   *
   * @return the parameters of the function-typed parameter
   */
  FormalParameterList get parameters => _parameters;

  /**
   * Set the parameters of the function-typed parameter to the given parameters.
   *
   * @param parameters the parameters of the function-typed parameter
   */
  void set parameters(FormalParameterList parameters) {
    _parameters = becomeParentOf(parameters);
  }

  /**
   * Return the return type of the function, or `null` if the function does not have a return
   * type.
   *
   * @return the return type of the function
   */
  TypeName get returnType => _returnType;

  /**
   * Set the return type of the function to the given type.
   *
   * @param returnType the return type of the function
   */
  void set returnType(TypeName returnType) {
    _returnType = becomeParentOf(returnType);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitFunctionTypedFormalParameter(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_returnType, visitor);
    safelyVisitChild(identifier, visitor);
    safelyVisitChild(_parameters, visitor);
  }
}

/**
 * Instances of the class `GeneralizingAstVisitor` implement an AST visitor that will
 * recursively visit all of the nodes in an AST structure (like instances of the class
 * [RecursiveAstVisitor]). In addition, when a node of a specific type is visited not only
 * will the visit method for that specific type of node be invoked, but additional methods for the
 * superclasses of that node will also be invoked. For example, using an instance of this class to
 * visit a [Block] will cause the method [visitBlock] to be invoked but will
 * also cause the methods [visitStatement] and [visitNode] to be
 * subsequently invoked. This allows visitors to be written that visit all statements without
 * needing to override the visit method for each of the specific subclasses of [Statement].
 *
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general visit method. Failure to do so will cause the visit methods
 * for superclasses of the node to not be invoked and will cause the children of the visited node to
 * not be visited.
 */
class GeneralizingAstVisitor<R> implements AstVisitor<R> {
  @override
  R visitAdjacentStrings(AdjacentStrings node) => visitStringLiteral(node);

  R visitAnnotatedNode(AnnotatedNode node) => visitNode(node);

  @override
  R visitAnnotation(Annotation node) => visitNode(node);

  @override
  R visitArgumentList(ArgumentList node) => visitNode(node);

  @override
  R visitAsExpression(AsExpression node) => visitExpression(node);

  @override
  R visitAssertStatement(AssertStatement node) => visitStatement(node);

  @override
  R visitAssignmentExpression(AssignmentExpression node) =>
      visitExpression(node);

  @override
  R visitAwaitExpression(AwaitExpression node) => visitExpression(node);

  @override
  R visitBinaryExpression(BinaryExpression node) => visitExpression(node);

  @override
  R visitBlock(Block node) => visitStatement(node);

  @override
  R visitBlockFunctionBody(BlockFunctionBody node) => visitFunctionBody(node);

  @override
  R visitBooleanLiteral(BooleanLiteral node) => visitLiteral(node);

  @override
  R visitBreakStatement(BreakStatement node) => visitStatement(node);

  @override
  R visitCascadeExpression(CascadeExpression node) => visitExpression(node);

  @override
  R visitCatchClause(CatchClause node) => visitNode(node);

  @override
  R visitClassDeclaration(ClassDeclaration node) =>
      visitCompilationUnitMember(node);

  R visitClassMember(ClassMember node) => visitDeclaration(node);

  @override
  R visitClassTypeAlias(ClassTypeAlias node) => visitTypeAlias(node);

  R visitCombinator(Combinator node) => visitNode(node);

  @override
  R visitComment(Comment node) => visitNode(node);

  @override
  R visitCommentReference(CommentReference node) => visitNode(node);

  @override
  R visitCompilationUnit(CompilationUnit node) => visitNode(node);

  R visitCompilationUnitMember(CompilationUnitMember node) =>
      visitDeclaration(node);

  @override
  R visitConditionalExpression(ConditionalExpression node) =>
      visitExpression(node);

  @override
  R visitConstructorDeclaration(ConstructorDeclaration node) =>
      visitClassMember(node);

  @override
  R visitConstructorFieldInitializer(ConstructorFieldInitializer node) =>
      visitConstructorInitializer(node);

  R visitConstructorInitializer(ConstructorInitializer node) => visitNode(node);

  @override
  R visitConstructorName(ConstructorName node) => visitNode(node);

  @override
  R visitContinueStatement(ContinueStatement node) => visitStatement(node);

  R visitDeclaration(Declaration node) => visitAnnotatedNode(node);

  @override
  R visitDeclaredIdentifier(DeclaredIdentifier node) => visitDeclaration(node);

  @override
  R visitDefaultFormalParameter(DefaultFormalParameter node) =>
      visitFormalParameter(node);

  R visitDirective(Directive node) => visitAnnotatedNode(node);

  @override
  R visitDoStatement(DoStatement node) => visitStatement(node);

  @override
  R visitDoubleLiteral(DoubleLiteral node) => visitLiteral(node);

  @override
  R visitEmptyFunctionBody(EmptyFunctionBody node) => visitFunctionBody(node);

  @override
  R visitEmptyStatement(EmptyStatement node) => visitStatement(node);

  @override
  R visitEnumConstantDeclaration(EnumConstantDeclaration node) =>
      visitDeclaration(node);

  @override
  R visitEnumDeclaration(EnumDeclaration node) =>
      visitCompilationUnitMember(node);

  @override
  R visitExportDirective(ExportDirective node) => visitNamespaceDirective(node);

  R visitExpression(Expression node) => visitNode(node);

  @override
  R visitExpressionFunctionBody(ExpressionFunctionBody node) =>
      visitFunctionBody(node);

  @override
  R visitExpressionStatement(ExpressionStatement node) => visitStatement(node);

  @override
  R visitExtendsClause(ExtendsClause node) => visitNode(node);

  @override
  R visitFieldDeclaration(FieldDeclaration node) => visitClassMember(node);

  @override
  R visitFieldFormalParameter(FieldFormalParameter node) =>
      visitNormalFormalParameter(node);

  @override
  R visitForEachStatement(ForEachStatement node) => visitStatement(node);

  R visitFormalParameter(FormalParameter node) => visitNode(node);

  @override
  R visitFormalParameterList(FormalParameterList node) => visitNode(node);

  @override
  R visitForStatement(ForStatement node) => visitStatement(node);

  R visitFunctionBody(FunctionBody node) => visitNode(node);

  @override
  R visitFunctionDeclaration(FunctionDeclaration node) =>
      visitCompilationUnitMember(node);

  @override
  R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) =>
      visitStatement(node);

  @override
  R visitFunctionExpression(FunctionExpression node) => visitExpression(node);

  @override
  R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) =>
      visitExpression(node);

  @override
  R visitFunctionTypeAlias(FunctionTypeAlias node) => visitTypeAlias(node);

  @override
  R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) =>
      visitNormalFormalParameter(node);

  @override
  R visitHideCombinator(HideCombinator node) => visitCombinator(node);

  R visitIdentifier(Identifier node) => visitExpression(node);

  @override
  R visitIfStatement(IfStatement node) => visitStatement(node);

  @override
  R visitImplementsClause(ImplementsClause node) => visitNode(node);

  @override
  R visitImportDirective(ImportDirective node) => visitNamespaceDirective(node);

  @override
  R visitIndexExpression(IndexExpression node) => visitExpression(node);

  @override
  R visitInstanceCreationExpression(InstanceCreationExpression node) =>
      visitExpression(node);

  @override
  R visitIntegerLiteral(IntegerLiteral node) => visitLiteral(node);

  R visitInterpolationElement(InterpolationElement node) => visitNode(node);

  @override
  R visitInterpolationExpression(InterpolationExpression node) =>
      visitInterpolationElement(node);

  @override
  R visitInterpolationString(InterpolationString node) =>
      visitInterpolationElement(node);

  @override
  R visitIsExpression(IsExpression node) => visitExpression(node);

  @override
  R visitLabel(Label node) => visitNode(node);

  @override
  R visitLabeledStatement(LabeledStatement node) => visitStatement(node);

  @override
  R visitLibraryDirective(LibraryDirective node) => visitDirective(node);

  @override
  R visitLibraryIdentifier(LibraryIdentifier node) => visitIdentifier(node);

  @override
  R visitListLiteral(ListLiteral node) => visitTypedLiteral(node);

  R visitLiteral(Literal node) => visitExpression(node);

  @override
  R visitMapLiteral(MapLiteral node) => visitTypedLiteral(node);

  @override
  R visitMapLiteralEntry(MapLiteralEntry node) => visitNode(node);

  @override
  R visitMethodDeclaration(MethodDeclaration node) => visitClassMember(node);

  @override
  R visitMethodInvocation(MethodInvocation node) => visitExpression(node);

  @override
  R visitNamedExpression(NamedExpression node) => visitExpression(node);

  R visitNamespaceDirective(NamespaceDirective node) =>
      visitUriBasedDirective(node);

  @override
  R visitNativeClause(NativeClause node) => visitNode(node);

  @override
  R visitNativeFunctionBody(NativeFunctionBody node) => visitFunctionBody(node);

  R visitNode(AstNode node) {
    node.visitChildren(this);
    return null;
  }

  R visitNormalFormalParameter(NormalFormalParameter node) =>
      visitFormalParameter(node);

  @override
  R visitNullLiteral(NullLiteral node) => visitLiteral(node);

  @override
  R visitParenthesizedExpression(ParenthesizedExpression node) =>
      visitExpression(node);

  @override
  R visitPartDirective(PartDirective node) => visitUriBasedDirective(node);

  @override
  R visitPartOfDirective(PartOfDirective node) => visitDirective(node);

  @override
  R visitPostfixExpression(PostfixExpression node) => visitExpression(node);

  @override
  R visitPrefixedIdentifier(PrefixedIdentifier node) => visitIdentifier(node);

  @override
  R visitPrefixExpression(PrefixExpression node) => visitExpression(node);

  @override
  R visitPropertyAccess(PropertyAccess node) => visitExpression(node);

  @override
  R
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) =>
      visitConstructorInitializer(node);

  @override
  R visitRethrowExpression(RethrowExpression node) => visitExpression(node);

  @override
  R visitReturnStatement(ReturnStatement node) => visitStatement(node);

  @override
  R visitScriptTag(ScriptTag scriptTag) => visitNode(scriptTag);

  @override
  R visitShowCombinator(ShowCombinator node) => visitCombinator(node);

  @override
  R visitSimpleFormalParameter(SimpleFormalParameter node) =>
      visitNormalFormalParameter(node);

  @override
  R visitSimpleIdentifier(SimpleIdentifier node) => visitIdentifier(node);

  @override
  R visitSimpleStringLiteral(SimpleStringLiteral node) =>
      visitSingleStringLiteral(node);

  R visitSingleStringLiteral(SingleStringLiteral node) =>
      visitStringLiteral(node);

  R visitStatement(Statement node) => visitNode(node);

  @override
  R visitStringInterpolation(StringInterpolation node) =>
      visitSingleStringLiteral(node);

  R visitStringLiteral(StringLiteral node) => visitLiteral(node);

  @override
  R visitSuperConstructorInvocation(SuperConstructorInvocation node) =>
      visitConstructorInitializer(node);

  @override
  R visitSuperExpression(SuperExpression node) => visitExpression(node);

  @override
  R visitSwitchCase(SwitchCase node) => visitSwitchMember(node);

  @override
  R visitSwitchDefault(SwitchDefault node) => visitSwitchMember(node);

  R visitSwitchMember(SwitchMember node) => visitNode(node);

  @override
  R visitSwitchStatement(SwitchStatement node) => visitStatement(node);

  @override
  R visitSymbolLiteral(SymbolLiteral node) => visitLiteral(node);

  @override
  R visitThisExpression(ThisExpression node) => visitExpression(node);

  @override
  R visitThrowExpression(ThrowExpression node) => visitExpression(node);

  @override
  R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) =>
      visitCompilationUnitMember(node);

  @override
  R visitTryStatement(TryStatement node) => visitStatement(node);

  R visitTypeAlias(TypeAlias node) => visitCompilationUnitMember(node);

  @override
  R visitTypeArgumentList(TypeArgumentList node) => visitNode(node);

  R visitTypedLiteral(TypedLiteral node) => visitLiteral(node);

  @override
  R visitTypeName(TypeName node) => visitNode(node);

  @override
  R visitTypeParameter(TypeParameter node) => visitNode(node);

  @override
  R visitTypeParameterList(TypeParameterList node) => visitNode(node);

  R visitUriBasedDirective(UriBasedDirective node) => visitDirective(node);

  @override
  R visitVariableDeclaration(VariableDeclaration node) =>
      visitDeclaration(node);

  @override
  R visitVariableDeclarationList(VariableDeclarationList node) =>
      visitNode(node);

  @override
  R visitVariableDeclarationStatement(VariableDeclarationStatement node) =>
      visitStatement(node);

  @override
  R visitWhileStatement(WhileStatement node) => visitStatement(node);

  @override
  R visitWithClause(WithClause node) => visitNode(node);

  @override
  R visitYieldStatement(YieldStatement node) => visitStatement(node);
}

class GeneralizingAstVisitor_BreadthFirstVisitor extends
    GeneralizingAstVisitor<Object> {
  final BreadthFirstVisitor BreadthFirstVisitor_this;

  GeneralizingAstVisitor_BreadthFirstVisitor(this.BreadthFirstVisitor_this)
      : super();

  @override
  Object visitNode(AstNode node) {
    BreadthFirstVisitor_this._queue.add(node);
    return null;
  }
}

/**
 * Instances of the class `HideCombinator` represent a combinator that restricts the names
 * being imported to those that are not in a given list.
 *
 * <pre>
 * hideCombinator ::=
 *     'hide' [SimpleIdentifier] (',' [SimpleIdentifier])*
 * </pre>
 */
class HideCombinator extends Combinator {
  /**
   * The list of names from the library that are hidden by this combinator.
   */
  NodeList<SimpleIdentifier> _hiddenNames;

  /**
   * Initialize a newly created import show combinator.
   *
   * @param keyword the comma introducing the combinator
   * @param hiddenNames the list of names from the library that are hidden by this combinator
   */
  HideCombinator(Token keyword, List<SimpleIdentifier> hiddenNames)
      : super(keyword) {
    _hiddenNames = new NodeList<SimpleIdentifier>(this, hiddenNames);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..addAll(_hiddenNames);

  @override
  Token get endToken => _hiddenNames.endToken;

  /**
   * Return the list of names from the library that are hidden by this combinator.
   *
   * @return the list of names from the library that are hidden by this combinator
   */
  NodeList<SimpleIdentifier> get hiddenNames => _hiddenNames;

  @override
  accept(AstVisitor visitor) => visitor.visitHideCombinator(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _hiddenNames.accept(visitor);
  }
}

/**
 * The abstract class `Identifier` defines the behavior common to nodes that represent an
 * identifier.
 *
 * <pre>
 * identifier ::=
 *     [SimpleIdentifier]
 *   | [PrefixedIdentifier]
 * </pre>
 */
abstract class Identifier extends Expression {
  /**
   * Return the best element available for this operator. If resolution was able to find a better
   * element based on type propagation, that element will be returned. Otherwise, the element found
   * using the result of static analysis will be returned. If resolution has not been performed,
   * then `null` will be returned.
   *
   * @return the best element available for this operator
   */
  Element get bestElement;

  @override
  bool get isAssignable => true;

  /**
   * Return the lexical representation of the identifier.
   *
   * @return the lexical representation of the identifier
   */
  String get name;

  /**
   * Return the element associated with this identifier based on propagated type information, or
   * `null` if the AST structure has not been resolved or if this identifier could not be
   * resolved. One example of the latter case is an identifier that is not defined within the scope
   * in which it appears.
   *
   * @return the element associated with this identifier
   */
  Element get propagatedElement;

  /**
   * Return the element associated with this identifier based on static type information, or
   * `null` if the AST structure has not been resolved or if this identifier could not be
   * resolved. One example of the latter case is an identifier that is not defined within the scope
   * in which it appears
   *
   * @return the element associated with the operator
   */
  Element get staticElement;

  /**
   * Return `true` if the given name is visible only within the library in which it is
   * declared.
   *
   * @param name the name being tested
   * @return `true` if the given name is private
   */
  static bool isPrivateName(String name) =>
      StringUtilities.startsWithChar(name, 0x5F);
}

/**
 * Instances of the class `IfStatement` represent an if statement.
 *
 * <pre>
 * ifStatement ::=
 *     'if' '(' [Expression] ')' [Statement] ('else' [Statement])?
 * </pre>
 */
class IfStatement extends Statement {
  /**
   * The token representing the 'if' keyword.
   */
  Token ifKeyword;

  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The condition used to determine which of the statements is executed next.
   */
  Expression _condition;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * The statement that is executed if the condition evaluates to `true`.
   */
  Statement _thenStatement;

  /**
   * The token representing the 'else' keyword, or `null` if there is no else statement.
   */
  Token elseKeyword;

  /**
   * The statement that is executed if the condition evaluates to `false`, or `null` if
   * there is no else statement.
   */
  Statement _elseStatement;

  /**
   * Initialize a newly created if statement.
   *
   * @param ifKeyword the token representing the 'if' keyword
   * @param leftParenthesis the left parenthesis
   * @param condition the condition used to determine which of the statements is executed next
   * @param rightParenthesis the right parenthesis
   * @param thenStatement the statement that is executed if the condition evaluates to `true`
   * @param elseKeyword the token representing the 'else' keyword
   * @param elseStatement the statement that is executed if the condition evaluates to `false`
   */
  IfStatement(this.ifKeyword, this.leftParenthesis, Expression condition,
      this.rightParenthesis, Statement thenStatement, this.elseKeyword,
      Statement elseStatement) {
    _condition = becomeParentOf(condition);
    _thenStatement = becomeParentOf(thenStatement);
    _elseStatement = becomeParentOf(elseStatement);
  }

  @override
  Token get beginToken => ifKeyword;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(ifKeyword)
      ..add(leftParenthesis)
      ..add(_condition)
      ..add(rightParenthesis)
      ..add(_thenStatement)
      ..add(elseKeyword)
      ..add(_elseStatement);

  /**
   * Return the condition used to determine which of the statements is executed next.
   *
   * @return the condition used to determine which statement is executed next
   */
  Expression get condition => _condition;

  /**
   * Set the condition used to determine which of the statements is executed next to the given
   * expression.
   *
   * @param expression the condition used to determine which statement is executed next
   */
  void set condition(Expression expression) {
    _condition = becomeParentOf(expression);
  }

  /**
   * Return the statement that is executed if the condition evaluates to `false`, or
   * `null` if there is no else statement.
   *
   * @return the statement that is executed if the condition evaluates to `false`
   */
  Statement get elseStatement => _elseStatement;

  /**
   * Set the statement that is executed if the condition evaluates to `false` to the given
   * statement.
   *
   * @param statement the statement that is executed if the condition evaluates to `false`
   */
  void set elseStatement(Statement statement) {
    _elseStatement = becomeParentOf(statement);
  }

  @override
  Token get endToken {
    if (_elseStatement != null) {
      return _elseStatement.endToken;
    }
    return _thenStatement.endToken;
  }

  /**
   * Return the statement that is executed if the condition evaluates to `true`.
   *
   * @return the statement that is executed if the condition evaluates to `true`
   */
  Statement get thenStatement => _thenStatement;

  /**
   * Set the statement that is executed if the condition evaluates to `true` to the given
   * statement.
   *
   * @param statement the statement that is executed if the condition evaluates to `true`
   */
  void set thenStatement(Statement statement) {
    _thenStatement = becomeParentOf(statement);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitIfStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_condition, visitor);
    safelyVisitChild(_thenStatement, visitor);
    safelyVisitChild(_elseStatement, visitor);
  }
}

/**
 * Instances of the class `ImplementsClause` represent the "implements" clause in an class
 * declaration.
 *
 * <pre>
 * implementsClause ::=
 *     'implements' [TypeName] (',' [TypeName])*
 * </pre>
 */
class ImplementsClause extends AstNode {
  /**
   * The token representing the 'implements' keyword.
   */
  Token keyword;

  /**
   * The interfaces that are being implemented.
   */
  NodeList<TypeName> _interfaces;

  /**
   * Initialize a newly created implements clause.
   *
   * @param keyword the token representing the 'implements' keyword
   * @param interfaces the interfaces that are being implemented
   */
  ImplementsClause(this.keyword, List<TypeName> interfaces) {
    _interfaces = new NodeList<TypeName>(this, interfaces);
  }

  @override
  Token get beginToken => keyword;

  /**
   * TODO(paulberry): add commas.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..addAll(interfaces);

  @override
  Token get endToken => _interfaces.endToken;

  /**
   * Return the list of the interfaces that are being implemented.
   *
   * @return the list of the interfaces that are being implemented
   */
  NodeList<TypeName> get interfaces => _interfaces;

  @override
  accept(AstVisitor visitor) => visitor.visitImplementsClause(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _interfaces.accept(visitor);
  }
}

/**
 * Instances of the class `ImportDirective` represent an import directive.
 *
 * <pre>
 * importDirective ::=
 *     [Annotation] 'import' [StringLiteral] ('as' identifier)? [Combinator]* ';'
 *   | [Annotation] 'import' [StringLiteral] 'deferred' 'as' identifier [Combinator]* ';'
 * </pre>
 */
class ImportDirective extends NamespaceDirective {
  static Comparator<ImportDirective> COMPARATOR =
      (ImportDirective import1, ImportDirective import2) {
    //
    // uri
    //
    StringLiteral uri1 = import1.uri;
    StringLiteral uri2 = import2.uri;
    String uriStr1 = uri1.stringValue;
    String uriStr2 = uri2.stringValue;
    if (uriStr1 != null || uriStr2 != null) {
      if (uriStr1 == null) {
        return -1;
      } else if (uriStr2 == null) {
        return 1;
      } else {
        int compare = uriStr1.compareTo(uriStr2);
        if (compare != 0) {
          return compare;
        }
      }
    }
    //
    // as
    //
    SimpleIdentifier prefix1 = import1.prefix;
    SimpleIdentifier prefix2 = import2.prefix;
    String prefixStr1 = prefix1 != null ? prefix1.name : null;
    String prefixStr2 = prefix2 != null ? prefix2.name : null;
    if (prefixStr1 != null || prefixStr2 != null) {
      if (prefixStr1 == null) {
        return -1;
      } else if (prefixStr2 == null) {
        return 1;
      } else {
        int compare = prefixStr1.compareTo(prefixStr2);
        if (compare != 0) {
          return compare;
        }
      }
    }
    //
    // hides and shows
    //
    NodeList<Combinator> combinators1 = import1.combinators;
    List<String> allHides1 = new List<String>();
    List<String> allShows1 = new List<String>();
    for (Combinator combinator in combinators1) {
      if (combinator is HideCombinator) {
        NodeList<SimpleIdentifier> hides = combinator.hiddenNames;
        for (SimpleIdentifier simpleIdentifier in hides) {
          allHides1.add(simpleIdentifier.name);
        }
      } else {
        NodeList<SimpleIdentifier> shows =
            (combinator as ShowCombinator).shownNames;
        for (SimpleIdentifier simpleIdentifier in shows) {
          allShows1.add(simpleIdentifier.name);
        }
      }
    }
    NodeList<Combinator> combinators2 = import2.combinators;
    List<String> allHides2 = new List<String>();
    List<String> allShows2 = new List<String>();
    for (Combinator combinator in combinators2) {
      if (combinator is HideCombinator) {
        NodeList<SimpleIdentifier> hides = combinator.hiddenNames;
        for (SimpleIdentifier simpleIdentifier in hides) {
          allHides2.add(simpleIdentifier.name);
        }
      } else {
        NodeList<SimpleIdentifier> shows =
            (combinator as ShowCombinator).shownNames;
        for (SimpleIdentifier simpleIdentifier in shows) {
          allShows2.add(simpleIdentifier.name);
        }
      }
    }
    // test lengths of combinator lists first
    if (allHides1.length != allHides2.length) {
      return allHides1.length - allHides2.length;
    }
    if (allShows1.length != allShows2.length) {
      return allShows1.length - allShows2.length;
    }
    // next ensure that the lists are equivalent
    if (!javaCollectionContainsAll(allHides1, allHides2)) {
      return -1;
    }
    if (!javaCollectionContainsAll(allShows1, allShows2)) {
      return -1;
    }
    return 0;
  };

  /**
   * The token representing the 'deferred' token, or `null` if the imported is not deferred.
   */
  Token deferredToken;

  /**
   * The token representing the 'as' token, or `null` if the imported names are not prefixed.
   */
  Token asToken;

  /**
   * The prefix to be used with the imported names, or `null` if the imported names are not
   * prefixed.
   */
  SimpleIdentifier _prefix;

  /**
   * Initialize a newly created import directive.
   *
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param keyword the token representing the 'import' keyword
   * @param libraryUri the URI of the library being imported
   * @param deferredToken the token representing the 'deferred' token
   * @param asToken the token representing the 'as' token
   * @param prefix the prefix to be used with the imported names
   * @param combinators the combinators used to control how names are imported
   * @param semicolon the semicolon terminating the directive
   */
  ImportDirective(Comment comment, List<Annotation> metadata, Token keyword,
      StringLiteral libraryUri, this.deferredToken, this.asToken,
      SimpleIdentifier prefix, List<Combinator> combinators, Token semicolon)
      : super(comment, metadata, keyword, libraryUri, combinators, semicolon) {
    _prefix = becomeParentOf(prefix);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(_uri)
      ..add(deferredToken)
      ..add(asToken)
      ..add(_prefix)
      ..addAll(combinators)
      ..add(semicolon);

  @override
  ImportElement get element => super.element as ImportElement;

  /**
   * Return the prefix to be used with the imported names, or `null` if the imported names are
   * not prefixed.
   *
   * @return the prefix to be used with the imported names
   */
  SimpleIdentifier get prefix => _prefix;

  /**
   * Set the prefix to be used with the imported names to the given identifier.
   *
   * @param prefix the prefix to be used with the imported names
   */
  void set prefix(SimpleIdentifier prefix) {
    _prefix = becomeParentOf(prefix);
  }

  @override
  LibraryElement get uriElement {
    ImportElement element = this.element;
    if (element == null) {
      return null;
    }
    return element.importedLibrary;
  }

  @override
  accept(AstVisitor visitor) => visitor.visitImportDirective(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_prefix, visitor);
    combinators.accept(visitor);
  }
}

/**
 * Instances of the class `IncrementalAstCloner` implement an object that will clone any AST
 * structure that it visits. The cloner will clone the structure, replacing the specified ASTNode
 * with a new ASTNode, mapping the old token stream to a new token stream, and preserving resolution
 * results.
 */
class IncrementalAstCloner implements AstVisitor<AstNode> {
  /**
   * The node to be replaced during the cloning process.
   */
  final AstNode _oldNode;

  /**
   * The replacement node used during the cloning process.
   */
  final AstNode _newNode;

  /**
   * A mapping of old tokens to new tokens used during the cloning process.
   */
  final TokenMap _tokenMap;

  /**
   * Construct a new instance that will replace `oldNode` with `newNode` in the process
   * of cloning an existing AST structure.
   *
   * @param oldNode the node to be replaced
   * @param newNode the replacement node
   * @param tokenMap a mapping of old tokens to new tokens (not `null`)
   */
  IncrementalAstCloner(this._oldNode, this._newNode, this._tokenMap);

  @override
  AdjacentStrings visitAdjacentStrings(AdjacentStrings node) =>
      new AdjacentStrings(_cloneNodeList(node.strings));

  @override
  Annotation visitAnnotation(Annotation node) {
    Annotation copy = new Annotation(
        _mapToken(node.atSign),
        _cloneNode(node.name),
        _mapToken(node.period),
        _cloneNode(node.constructorName),
        _cloneNode(node.arguments));
    copy.element = node.element;
    return copy;
  }

  @override
  ArgumentList visitArgumentList(ArgumentList node) =>
      new ArgumentList(
          _mapToken(node.leftParenthesis),
          _cloneNodeList(node.arguments),
          _mapToken(node.rightParenthesis));

  @override
  AsExpression visitAsExpression(AsExpression node) {
    AsExpression copy = new AsExpression(
        _cloneNode(node.expression),
        _mapToken(node.asOperator),
        _cloneNode(node.type));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  AstNode visitAssertStatement(AssertStatement node) =>
      new AssertStatement(
          _mapToken(node.keyword),
          _mapToken(node.leftParenthesis),
          _cloneNode(node.condition),
          _mapToken(node.rightParenthesis),
          _mapToken(node.semicolon));

  @override
  AssignmentExpression visitAssignmentExpression(AssignmentExpression node) {
    AssignmentExpression copy = new AssignmentExpression(
        _cloneNode(node.leftHandSide),
        _mapToken(node.operator),
        _cloneNode(node.rightHandSide));
    copy.propagatedElement = node.propagatedElement;
    copy.propagatedType = node.propagatedType;
    copy.staticElement = node.staticElement;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  AwaitExpression visitAwaitExpression(AwaitExpression node) =>
      new AwaitExpression(_mapToken(node.awaitKeyword), _cloneNode(node.expression));

  @override
  BinaryExpression visitBinaryExpression(BinaryExpression node) {
    BinaryExpression copy = new BinaryExpression(
        _cloneNode(node.leftOperand),
        _mapToken(node.operator),
        _cloneNode(node.rightOperand));
    copy.propagatedElement = node.propagatedElement;
    copy.propagatedType = node.propagatedType;
    copy.staticElement = node.staticElement;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  Block visitBlock(Block node) =>
      new Block(
          _mapToken(node.leftBracket),
          _cloneNodeList(node.statements),
          _mapToken(node.rightBracket));

  @override
  BlockFunctionBody visitBlockFunctionBody(BlockFunctionBody node) =>
      new BlockFunctionBody(
          _mapToken(node.keyword),
          _mapToken(node.star),
          _cloneNode(node.block));

  @override
  BooleanLiteral visitBooleanLiteral(BooleanLiteral node) {
    BooleanLiteral copy =
        new BooleanLiteral(_mapToken(node.literal), node.value);
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  BreakStatement visitBreakStatement(BreakStatement node) =>
      new BreakStatement(
          _mapToken(node.keyword),
          _cloneNode(node.label),
          _mapToken(node.semicolon));

  @override
  CascadeExpression visitCascadeExpression(CascadeExpression node) {
    CascadeExpression copy = new CascadeExpression(
        _cloneNode(node.target),
        _cloneNodeList(node.cascadeSections));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  CatchClause visitCatchClause(CatchClause node) =>
      new CatchClause(
          _mapToken(node.onKeyword),
          _cloneNode(node.exceptionType),
          _mapToken(node.catchKeyword),
          _mapToken(node.leftParenthesis),
          _cloneNode(node.exceptionParameter),
          _mapToken(node.comma),
          _cloneNode(node.stackTraceParameter),
          _mapToken(node.rightParenthesis),
          _cloneNode(node.body));

  @override
  ClassDeclaration visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration copy = new ClassDeclaration(
        _cloneNode(node.documentationComment),
        _cloneNodeList(node.metadata),
        _mapToken(node.abstractKeyword),
        _mapToken(node.classKeyword),
        _cloneNode(node.name),
        _cloneNode(node.typeParameters),
        _cloneNode(node.extendsClause),
        _cloneNode(node.withClause),
        _cloneNode(node.implementsClause),
        _mapToken(node.leftBracket),
        _cloneNodeList(node.members),
        _mapToken(node.rightBracket));
    copy.nativeClause = _cloneNode(node.nativeClause);
    return copy;
  }

  @override
  ClassTypeAlias visitClassTypeAlias(ClassTypeAlias node) =>
      new ClassTypeAlias(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.keyword),
          _cloneNode(node.name),
          _cloneNode(node.typeParameters),
          _mapToken(node.equals),
          _mapToken(node.abstractKeyword),
          _cloneNode(node.superclass),
          _cloneNode(node.withClause),
          _cloneNode(node.implementsClause),
          _mapToken(node.semicolon));

  @override
  Comment visitComment(Comment node) {
    if (node.isDocumentation) {
      return Comment.createDocumentationCommentWithReferences(
          _mapTokens(node.tokens),
          _cloneNodeList(node.references));
    } else if (node.isBlock) {
      return Comment.createBlockComment(_mapTokens(node.tokens));
    }
    return Comment.createEndOfLineComment(_mapTokens(node.tokens));
  }

  @override
  CommentReference visitCommentReference(CommentReference node) =>
      new CommentReference(_mapToken(node.newKeyword), _cloneNode(node.identifier));

  @override
  CompilationUnit visitCompilationUnit(CompilationUnit node) {
    CompilationUnit copy = new CompilationUnit(
        _mapToken(node.beginToken),
        _cloneNode(node.scriptTag),
        _cloneNodeList(node.directives),
        _cloneNodeList(node.declarations),
        _mapToken(node.endToken));
    copy.lineInfo = node.lineInfo;
    copy.element = node.element;
    return copy;
  }

  @override
  ConditionalExpression visitConditionalExpression(ConditionalExpression node) {
    ConditionalExpression copy = new ConditionalExpression(
        _cloneNode(node.condition),
        _mapToken(node.question),
        _cloneNode(node.thenExpression),
        _mapToken(node.colon),
        _cloneNode(node.elseExpression));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  ConstructorDeclaration
      visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorDeclaration copy = new ConstructorDeclaration(
        _cloneNode(node.documentationComment),
        _cloneNodeList(node.metadata),
        _mapToken(node.externalKeyword),
        _mapToken(node.constKeyword),
        _mapToken(node.factoryKeyword),
        _cloneNode(node.returnType),
        _mapToken(node.period),
        _cloneNode(node.name),
        _cloneNode(node.parameters),
        _mapToken(node.separator),
        _cloneNodeList(node.initializers),
        _cloneNode(node.redirectedConstructor),
        _cloneNode(node.body));
    copy.element = node.element;
    return copy;
  }

  @override
  ConstructorFieldInitializer
      visitConstructorFieldInitializer(ConstructorFieldInitializer node) =>
      new ConstructorFieldInitializer(
          _mapToken(node.keyword),
          _mapToken(node.period),
          _cloneNode(node.fieldName),
          _mapToken(node.equals),
          _cloneNode(node.expression));

  @override
  ConstructorName visitConstructorName(ConstructorName node) {
    ConstructorName copy = new ConstructorName(
        _cloneNode(node.type),
        _mapToken(node.period),
        _cloneNode(node.name));
    copy.staticElement = node.staticElement;
    return copy;
  }

  @override
  ContinueStatement visitContinueStatement(ContinueStatement node) =>
      new ContinueStatement(
          _mapToken(node.keyword),
          _cloneNode(node.label),
          _mapToken(node.semicolon));

  @override
  DeclaredIdentifier visitDeclaredIdentifier(DeclaredIdentifier node) =>
      new DeclaredIdentifier(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.keyword),
          _cloneNode(node.type),
          _cloneNode(node.identifier));

  @override
  DefaultFormalParameter
      visitDefaultFormalParameter(DefaultFormalParameter node) =>
      new DefaultFormalParameter(
          _cloneNode(node.parameter),
          node.kind,
          _mapToken(node.separator),
          _cloneNode(node.defaultValue));

  @override
  DoStatement visitDoStatement(DoStatement node) =>
      new DoStatement(
          _mapToken(node.doKeyword),
          _cloneNode(node.body),
          _mapToken(node.whileKeyword),
          _mapToken(node.leftParenthesis),
          _cloneNode(node.condition),
          _mapToken(node.rightParenthesis),
          _mapToken(node.semicolon));

  @override
  DoubleLiteral visitDoubleLiteral(DoubleLiteral node) {
    DoubleLiteral copy = new DoubleLiteral(_mapToken(node.literal), node.value);
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  EmptyFunctionBody visitEmptyFunctionBody(EmptyFunctionBody node) =>
      new EmptyFunctionBody(_mapToken(node.semicolon));

  @override
  EmptyStatement visitEmptyStatement(EmptyStatement node) =>
      new EmptyStatement(_mapToken(node.semicolon));

  @override
  AstNode visitEnumConstantDeclaration(EnumConstantDeclaration node) =>
      new EnumConstantDeclaration(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _cloneNode(node.name));

  @override
  AstNode visitEnumDeclaration(EnumDeclaration node) =>
      new EnumDeclaration(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.keyword),
          _cloneNode(node.name),
          _mapToken(node.leftBracket),
          _cloneNodeList(node.constants),
          _mapToken(node.rightBracket));

  @override
  ExportDirective visitExportDirective(ExportDirective node) {
    ExportDirective copy = new ExportDirective(
        _cloneNode(node.documentationComment),
        _cloneNodeList(node.metadata),
        _mapToken(node.keyword),
        _cloneNode(node.uri),
        _cloneNodeList(node.combinators),
        _mapToken(node.semicolon));
    copy.element = node.element;
    return copy;
  }

  @override
  ExpressionFunctionBody
      visitExpressionFunctionBody(ExpressionFunctionBody node) =>
      new ExpressionFunctionBody(
          _mapToken(node.keyword),
          _mapToken(node.functionDefinition),
          _cloneNode(node.expression),
          _mapToken(node.semicolon));

  @override
  ExpressionStatement visitExpressionStatement(ExpressionStatement node) =>
      new ExpressionStatement(_cloneNode(node.expression), _mapToken(node.semicolon));

  @override
  ExtendsClause visitExtendsClause(ExtendsClause node) =>
      new ExtendsClause(_mapToken(node.keyword), _cloneNode(node.superclass));

  @override
  FieldDeclaration visitFieldDeclaration(FieldDeclaration node) =>
      new FieldDeclaration(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.staticKeyword),
          _cloneNode(node.fields),
          _mapToken(node.semicolon));

  @override
  FieldFormalParameter visitFieldFormalParameter(FieldFormalParameter node) =>
      new FieldFormalParameter(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.keyword),
          _cloneNode(node.type),
          _mapToken(node.thisToken),
          _mapToken(node.period),
          _cloneNode(node.identifier),
          _cloneNode(node.parameters));

  @override
  ForEachStatement visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.loopVariable;
    if (loopVariable == null) {
      return new ForEachStatement.con2(
          _mapToken(node.awaitKeyword),
          _mapToken(node.forKeyword),
          _mapToken(node.leftParenthesis),
          _cloneNode(node.identifier),
          _mapToken(node.inKeyword),
          _cloneNode(node.iterable),
          _mapToken(node.rightParenthesis),
          _cloneNode(node.body));
    }
    return new ForEachStatement.con1(
        _mapToken(node.awaitKeyword),
        _mapToken(node.forKeyword),
        _mapToken(node.leftParenthesis),
        _cloneNode(loopVariable),
        _mapToken(node.inKeyword),
        _cloneNode(node.iterable),
        _mapToken(node.rightParenthesis),
        _cloneNode(node.body));
  }

  @override
  FormalParameterList visitFormalParameterList(FormalParameterList node) =>
      new FormalParameterList(
          _mapToken(node.leftParenthesis),
          _cloneNodeList(node.parameters),
          _mapToken(node.leftDelimiter),
          _mapToken(node.rightDelimiter),
          _mapToken(node.rightParenthesis));

  @override
  ForStatement visitForStatement(ForStatement node) =>
      new ForStatement(
          _mapToken(node.forKeyword),
          _mapToken(node.leftParenthesis),
          _cloneNode(node.variables),
          _cloneNode(node.initialization),
          _mapToken(node.leftSeparator),
          _cloneNode(node.condition),
          _mapToken(node.rightSeparator),
          _cloneNodeList(node.updaters),
          _mapToken(node.rightParenthesis),
          _cloneNode(node.body));

  @override
  FunctionDeclaration visitFunctionDeclaration(FunctionDeclaration node) =>
      new FunctionDeclaration(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.externalKeyword),
          _cloneNode(node.returnType),
          _mapToken(node.propertyKeyword),
          _cloneNode(node.name),
          _cloneNode(node.functionExpression));

  @override
  FunctionDeclarationStatement
      visitFunctionDeclarationStatement(FunctionDeclarationStatement node) =>
      new FunctionDeclarationStatement(_cloneNode(node.functionDeclaration));

  @override
  FunctionExpression visitFunctionExpression(FunctionExpression node) {
    FunctionExpression copy =
        new FunctionExpression(_cloneNode(node.parameters), _cloneNode(node.body));
    copy.element = node.element;
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  FunctionExpressionInvocation
      visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    FunctionExpressionInvocation copy = new FunctionExpressionInvocation(
        _cloneNode(node.function),
        _cloneNode(node.argumentList));
    copy.propagatedElement = node.propagatedElement;
    copy.propagatedType = node.propagatedType;
    copy.staticElement = node.staticElement;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  FunctionTypeAlias visitFunctionTypeAlias(FunctionTypeAlias node) =>
      new FunctionTypeAlias(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.keyword),
          _cloneNode(node.returnType),
          _cloneNode(node.name),
          _cloneNode(node.typeParameters),
          _cloneNode(node.parameters),
          _mapToken(node.semicolon));

  @override
  FunctionTypedFormalParameter
      visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) =>
      new FunctionTypedFormalParameter(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _cloneNode(node.returnType),
          _cloneNode(node.identifier),
          _cloneNode(node.parameters));

  @override
  HideCombinator visitHideCombinator(HideCombinator node) =>
      new HideCombinator(_mapToken(node.keyword), _cloneNodeList(node.hiddenNames));

  @override
  IfStatement visitIfStatement(IfStatement node) =>
      new IfStatement(
          _mapToken(node.ifKeyword),
          _mapToken(node.leftParenthesis),
          _cloneNode(node.condition),
          _mapToken(node.rightParenthesis),
          _cloneNode(node.thenStatement),
          _mapToken(node.elseKeyword),
          _cloneNode(node.elseStatement));

  @override
  ImplementsClause visitImplementsClause(ImplementsClause node) =>
      new ImplementsClause(_mapToken(node.keyword), _cloneNodeList(node.interfaces));

  @override
  ImportDirective visitImportDirective(ImportDirective node) =>
      new ImportDirective(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.keyword),
          _cloneNode(node.uri),
          _mapToken(node.deferredToken),
          _mapToken(node.asToken),
          _cloneNode(node.prefix),
          _cloneNodeList(node.combinators),
          _mapToken(node.semicolon));

  @override
  IndexExpression visitIndexExpression(IndexExpression node) {
    Token period = _mapToken(node.period);
    IndexExpression copy;
    if (period == null) {
      copy = new IndexExpression.forTarget(
          _cloneNode(node.target),
          _mapToken(node.leftBracket),
          _cloneNode(node.index),
          _mapToken(node.rightBracket));
    } else {
      copy = new IndexExpression.forCascade(
          period,
          _mapToken(node.leftBracket),
          _cloneNode(node.index),
          _mapToken(node.rightBracket));
    }
    copy.auxiliaryElements = node.auxiliaryElements;
    copy.propagatedElement = node.propagatedElement;
    copy.propagatedType = node.propagatedType;
    copy.staticElement = node.staticElement;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  InstanceCreationExpression
      visitInstanceCreationExpression(InstanceCreationExpression node) {
    InstanceCreationExpression copy = new InstanceCreationExpression(
        _mapToken(node.keyword),
        _cloneNode(node.constructorName),
        _cloneNode(node.argumentList));
    copy.propagatedType = node.propagatedType;
    copy.staticElement = node.staticElement;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  IntegerLiteral visitIntegerLiteral(IntegerLiteral node) {
    IntegerLiteral copy =
        new IntegerLiteral(_mapToken(node.literal), node.value);
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  InterpolationExpression
      visitInterpolationExpression(InterpolationExpression node) =>
      new InterpolationExpression(
          _mapToken(node.leftBracket),
          _cloneNode(node.expression),
          _mapToken(node.rightBracket));

  @override
  InterpolationString visitInterpolationString(InterpolationString node) =>
      new InterpolationString(_mapToken(node.contents), node.value);

  @override
  IsExpression visitIsExpression(IsExpression node) {
    IsExpression copy = new IsExpression(
        _cloneNode(node.expression),
        _mapToken(node.isOperator),
        _mapToken(node.notOperator),
        _cloneNode(node.type));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  Label visitLabel(Label node) =>
      new Label(_cloneNode(node.label), _mapToken(node.colon));

  @override
  LabeledStatement visitLabeledStatement(LabeledStatement node) =>
      new LabeledStatement(_cloneNodeList(node.labels), _cloneNode(node.statement));

  @override
  LibraryDirective visitLibraryDirective(LibraryDirective node) =>
      new LibraryDirective(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.libraryToken),
          _cloneNode(node.name),
          _mapToken(node.semicolon));

  @override
  LibraryIdentifier visitLibraryIdentifier(LibraryIdentifier node) {
    LibraryIdentifier copy =
        new LibraryIdentifier(_cloneNodeList(node.components));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  ListLiteral visitListLiteral(ListLiteral node) {
    ListLiteral copy = new ListLiteral(
        _mapToken(node.constKeyword),
        _cloneNode(node.typeArguments),
        _mapToken(node.leftBracket),
        _cloneNodeList(node.elements),
        _mapToken(node.rightBracket));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  MapLiteral visitMapLiteral(MapLiteral node) {
    MapLiteral copy = new MapLiteral(
        _mapToken(node.constKeyword),
        _cloneNode(node.typeArguments),
        _mapToken(node.leftBracket),
        _cloneNodeList(node.entries),
        _mapToken(node.rightBracket));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  MapLiteralEntry visitMapLiteralEntry(MapLiteralEntry node) =>
      new MapLiteralEntry(
          _cloneNode(node.key),
          _mapToken(node.separator),
          _cloneNode(node.value));

  @override
  MethodDeclaration visitMethodDeclaration(MethodDeclaration node) =>
      new MethodDeclaration(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.externalKeyword),
          _mapToken(node.modifierKeyword),
          _cloneNode(node.returnType),
          _mapToken(node.propertyKeyword),
          _mapToken(node.operatorKeyword),
          _cloneNode(node.name),
          _cloneNode(node.parameters),
          _cloneNode(node.body));

  @override
  MethodInvocation visitMethodInvocation(MethodInvocation node) {
    MethodInvocation copy = new MethodInvocation(
        _cloneNode(node.target),
        _mapToken(node.period),
        _cloneNode(node.methodName),
        _cloneNode(node.argumentList));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  NamedExpression visitNamedExpression(NamedExpression node) {
    NamedExpression copy =
        new NamedExpression(_cloneNode(node.name), _cloneNode(node.expression));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  AstNode visitNativeClause(NativeClause node) =>
      new NativeClause(_mapToken(node.keyword), _cloneNode(node.name));

  @override
  NativeFunctionBody visitNativeFunctionBody(NativeFunctionBody node) =>
      new NativeFunctionBody(
          _mapToken(node.nativeToken),
          _cloneNode(node.stringLiteral),
          _mapToken(node.semicolon));

  @override
  NullLiteral visitNullLiteral(NullLiteral node) {
    NullLiteral copy = new NullLiteral(_mapToken(node.literal));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  ParenthesizedExpression
      visitParenthesizedExpression(ParenthesizedExpression node) {
    ParenthesizedExpression copy = new ParenthesizedExpression(
        _mapToken(node.leftParenthesis),
        _cloneNode(node.expression),
        _mapToken(node.rightParenthesis));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  PartDirective visitPartDirective(PartDirective node) {
    PartDirective copy = new PartDirective(
        _cloneNode(node.documentationComment),
        _cloneNodeList(node.metadata),
        _mapToken(node.partToken),
        _cloneNode(node.uri),
        _mapToken(node.semicolon));
    copy.element = node.element;
    return copy;
  }

  @override
  PartOfDirective visitPartOfDirective(PartOfDirective node) {
    PartOfDirective copy = new PartOfDirective(
        _cloneNode(node.documentationComment),
        _cloneNodeList(node.metadata),
        _mapToken(node.partToken),
        _mapToken(node.ofToken),
        _cloneNode(node.libraryName),
        _mapToken(node.semicolon));
    copy.element = node.element;
    return copy;
  }

  @override
  PostfixExpression visitPostfixExpression(PostfixExpression node) {
    PostfixExpression copy =
        new PostfixExpression(_cloneNode(node.operand), _mapToken(node.operator));
    copy.propagatedElement = node.propagatedElement;
    copy.propagatedType = node.propagatedType;
    copy.staticElement = node.staticElement;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  PrefixedIdentifier visitPrefixedIdentifier(PrefixedIdentifier node) {
    PrefixedIdentifier copy = new PrefixedIdentifier(
        _cloneNode(node.prefix),
        _mapToken(node.period),
        _cloneNode(node.identifier));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  PrefixExpression visitPrefixExpression(PrefixExpression node) {
    PrefixExpression copy =
        new PrefixExpression(_mapToken(node.operator), _cloneNode(node.operand));
    copy.propagatedElement = node.propagatedElement;
    copy.propagatedType = node.propagatedType;
    copy.staticElement = node.staticElement;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  PropertyAccess visitPropertyAccess(PropertyAccess node) {
    PropertyAccess copy = new PropertyAccess(
        _cloneNode(node.target),
        _mapToken(node.operator),
        _cloneNode(node.propertyName));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  RedirectingConstructorInvocation
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    RedirectingConstructorInvocation copy =
        new RedirectingConstructorInvocation(
            _mapToken(node.keyword),
            _mapToken(node.period),
            _cloneNode(node.constructorName),
            _cloneNode(node.argumentList));
    copy.staticElement = node.staticElement;
    return copy;
  }

  @override
  RethrowExpression visitRethrowExpression(RethrowExpression node) {
    RethrowExpression copy = new RethrowExpression(_mapToken(node.keyword));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  ReturnStatement visitReturnStatement(ReturnStatement node) =>
      new ReturnStatement(
          _mapToken(node.keyword),
          _cloneNode(node.expression),
          _mapToken(node.semicolon));

  @override
  ScriptTag visitScriptTag(ScriptTag node) =>
      new ScriptTag(_mapToken(node.scriptTag));

  @override
  ShowCombinator visitShowCombinator(ShowCombinator node) =>
      new ShowCombinator(_mapToken(node.keyword), _cloneNodeList(node.shownNames));

  @override
  SimpleFormalParameter
      visitSimpleFormalParameter(SimpleFormalParameter node) =>
      new SimpleFormalParameter(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _mapToken(node.keyword),
          _cloneNode(node.type),
          _cloneNode(node.identifier));

  @override
  SimpleIdentifier visitSimpleIdentifier(SimpleIdentifier node) {
    Token mappedToken = _mapToken(node.token);
    if (mappedToken == null) {
      // This only happens for SimpleIdentifiers created by the parser as part
      // of scanning documentation comments (the tokens for those identifiers
      // are not in the original token stream and hence do not get copied).
      // This extra check can be removed if the scanner is changed to scan
      // documentation comments for the parser.
      mappedToken = node.token;
    }
    SimpleIdentifier copy = new SimpleIdentifier(mappedToken);
    copy.auxiliaryElements = node.auxiliaryElements;
    copy.propagatedElement = node.propagatedElement;
    copy.propagatedType = node.propagatedType;
    copy.staticElement = node.staticElement;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  SimpleStringLiteral visitSimpleStringLiteral(SimpleStringLiteral node) {
    SimpleStringLiteral copy =
        new SimpleStringLiteral(_mapToken(node.literal), node.value);
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  StringInterpolation visitStringInterpolation(StringInterpolation node) {
    StringInterpolation copy =
        new StringInterpolation(_cloneNodeList(node.elements));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  SuperConstructorInvocation
      visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    SuperConstructorInvocation copy = new SuperConstructorInvocation(
        _mapToken(node.keyword),
        _mapToken(node.period),
        _cloneNode(node.constructorName),
        _cloneNode(node.argumentList));
    copy.staticElement = node.staticElement;
    return copy;
  }

  @override
  SuperExpression visitSuperExpression(SuperExpression node) {
    SuperExpression copy = new SuperExpression(_mapToken(node.keyword));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  SwitchCase visitSwitchCase(SwitchCase node) =>
      new SwitchCase(
          _cloneNodeList(node.labels),
          _mapToken(node.keyword),
          _cloneNode(node.expression),
          _mapToken(node.colon),
          _cloneNodeList(node.statements));

  @override
  SwitchDefault visitSwitchDefault(SwitchDefault node) =>
      new SwitchDefault(
          _cloneNodeList(node.labels),
          _mapToken(node.keyword),
          _mapToken(node.colon),
          _cloneNodeList(node.statements));

  @override
  SwitchStatement visitSwitchStatement(SwitchStatement node) =>
      new SwitchStatement(
          _mapToken(node.keyword),
          _mapToken(node.leftParenthesis),
          _cloneNode(node.expression),
          _mapToken(node.rightParenthesis),
          _mapToken(node.leftBracket),
          _cloneNodeList(node.members),
          _mapToken(node.rightBracket));

  @override
  AstNode visitSymbolLiteral(SymbolLiteral node) {
    SymbolLiteral copy =
        new SymbolLiteral(_mapToken(node.poundSign), _mapTokens(node.components));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  ThisExpression visitThisExpression(ThisExpression node) {
    ThisExpression copy = new ThisExpression(_mapToken(node.keyword));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  ThrowExpression visitThrowExpression(ThrowExpression node) {
    ThrowExpression copy =
        new ThrowExpression(_mapToken(node.keyword), _cloneNode(node.expression));
    copy.propagatedType = node.propagatedType;
    copy.staticType = node.staticType;
    return copy;
  }

  @override
  TopLevelVariableDeclaration
      visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) =>
      new TopLevelVariableDeclaration(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _cloneNode(node.variables),
          _mapToken(node.semicolon));

  @override
  TryStatement visitTryStatement(TryStatement node) =>
      new TryStatement(
          _mapToken(node.tryKeyword),
          _cloneNode(node.body),
          _cloneNodeList(node.catchClauses),
          _mapToken(node.finallyKeyword),
          _cloneNode(node.finallyBlock));

  @override
  TypeArgumentList visitTypeArgumentList(TypeArgumentList node) =>
      new TypeArgumentList(
          _mapToken(node.leftBracket),
          _cloneNodeList(node.arguments),
          _mapToken(node.rightBracket));

  @override
  TypeName visitTypeName(TypeName node) {
    TypeName copy =
        new TypeName(_cloneNode(node.name), _cloneNode(node.typeArguments));
    copy.type = node.type;
    return copy;
  }

  @override
  TypeParameter visitTypeParameter(TypeParameter node) =>
      new TypeParameter(
          _cloneNode(node.documentationComment),
          _cloneNodeList(node.metadata),
          _cloneNode(node.name),
          _mapToken(node.keyword),
          _cloneNode(node.bound));

  @override
  TypeParameterList visitTypeParameterList(TypeParameterList node) =>
      new TypeParameterList(
          _mapToken(node.leftBracket),
          _cloneNodeList(node.typeParameters),
          _mapToken(node.rightBracket));

  @override
  VariableDeclaration visitVariableDeclaration(VariableDeclaration node) =>
      new VariableDeclaration(
          null,
          _cloneNodeList(node.metadata),
          _cloneNode(node.name),
          _mapToken(node.equals),
          _cloneNode(node.initializer));

  @override
  VariableDeclarationList
      visitVariableDeclarationList(VariableDeclarationList node) =>
      new VariableDeclarationList(
          null,
          _cloneNodeList(node.metadata),
          _mapToken(node.keyword),
          _cloneNode(node.type),
          _cloneNodeList(node.variables));

  @override
  VariableDeclarationStatement
      visitVariableDeclarationStatement(VariableDeclarationStatement node) =>
      new VariableDeclarationStatement(
          _cloneNode(node.variables),
          _mapToken(node.semicolon));

  @override
  WhileStatement visitWhileStatement(WhileStatement node) =>
      new WhileStatement(
          _mapToken(node.keyword),
          _mapToken(node.leftParenthesis),
          _cloneNode(node.condition),
          _mapToken(node.rightParenthesis),
          _cloneNode(node.body));

  @override
  WithClause visitWithClause(WithClause node) =>
      new WithClause(_mapToken(node.withKeyword), _cloneNodeList(node.mixinTypes));

  @override
  YieldStatement visitYieldStatement(YieldStatement node) =>
      new YieldStatement(
          _mapToken(node.yieldKeyword),
          _mapToken(node.star),
          _cloneNode(node.expression),
          _mapToken(node.semicolon));

  AstNode _cloneNode(AstNode node) {
    if (node == null) {
      return null;
    }
    if (identical(node, _oldNode)) {
      return _newNode;
    }
    return node.accept(this) as AstNode;
  }

  List _cloneNodeList(NodeList nodes) {
    List clonedNodes = new List();
    for (AstNode node in nodes) {
      clonedNodes.add(_cloneNode(node));
    }
    return clonedNodes;
  }

  Token _mapToken(Token oldToken) {
    if (oldToken == null) {
      return null;
    }
    return _tokenMap.get(oldToken);
  }

  List<Token> _mapTokens(List<Token> oldTokens) {
    List<Token> newTokens = new List<Token>(oldTokens.length);
    for (int index = 0; index < newTokens.length; index++) {
      newTokens[index] = _mapToken(oldTokens[index]);
    }
    return newTokens;
  }
}

/**
 * Instances of the class `IndexExpression` represent an index expression.
 *
 * <pre>
 * indexExpression ::=
 *     [Expression] '[' [Expression] ']'
 * </pre>
 */
class IndexExpression extends Expression {
  /**
   * The expression used to compute the object being indexed, or `null` if this index
   * expression is part of a cascade expression.
   */
  Expression _target;

  /**
   * The period ("..") before a cascaded index expression, or `null` if this index expression
   * is not part of a cascade expression.
   */
  Token period;

  /**
   * The left square bracket.
   */
  Token leftBracket;

  /**
   * The expression used to compute the index.
   */
  Expression _index;

  /**
   * The right square bracket.
   */
  Token rightBracket;

  /**
   * The element associated with the operator based on the static type of the target, or
   * `null` if the AST structure has not been resolved or if the operator could not be
   * resolved.
   */
  MethodElement staticElement;

  /**
   * The element associated with the operator based on the propagated type of the target, or
   * `null` if the AST structure has not been resolved or if the operator could not be
   * resolved.
   */
  MethodElement propagatedElement;

  /**
   * If this expression is both in a getter and setter context, the [AuxiliaryElements] will
   * be set to hold onto the static and propagated information. The auxiliary element will hold onto
   * the elements from the getter context.
   */
  AuxiliaryElements auxiliaryElements = null;

  /**
   * Initialize a newly created index expression.
   *
   * @param period the period ("..") before a cascaded index expression
   * @param leftBracket the left square bracket
   * @param index the expression used to compute the index
   * @param rightBracket the right square bracket
   */
  IndexExpression.forCascade(this.period, this.leftBracket, Expression index,
      this.rightBracket) {
    _index = becomeParentOf(index);
  }

  /**
   * Initialize a newly created index expression.
   *
   * @param target the expression used to compute the object being indexed
   * @param leftBracket the left square bracket
   * @param index the expression used to compute the index
   * @param rightBracket the right square bracket
   */
  IndexExpression.forTarget(Expression target, this.leftBracket,
      Expression index, this.rightBracket) {
    _target = becomeParentOf(target);
    _index = becomeParentOf(index);
  }

  @override
  Token get beginToken {
    if (_target != null) {
      return _target.beginToken;
    }
    return period;
  }

  /**
   * Return the best element available for this operator. If resolution was able to find a better
   * element based on type propagation, that element will be returned. Otherwise, the element found
   * using the result of static analysis will be returned. If resolution has not been performed,
   * then `null` will be returned.
   *
   * @return the best element available for this operator
   */
  MethodElement get bestElement {
    MethodElement element = propagatedElement;
    if (element == null) {
      element = staticElement;
    }
    return element;
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_target)
      ..add(period)
      ..add(leftBracket)
      ..add(_index)
      ..add(rightBracket);

  @override
  Token get endToken => rightBracket;

  /**
   * Return the expression used to compute the index.
   *
   * @return the expression used to compute the index
   */
  Expression get index => _index;

  /**
   * Set the expression used to compute the index to the given expression.
   *
   * @param expression the expression used to compute the index
   */
  void set index(Expression expression) {
    _index = becomeParentOf(expression);
  }

  @override
  bool get isAssignable => true;

  /**
   * Return `true` if this expression is cascaded. If it is, then the target of this
   * expression is not stored locally but is stored in the nearest ancestor that is a
   * [CascadeExpression].
   *
   * @return `true` if this expression is cascaded
   */
  bool get isCascaded => period != null;

  @override
  int get precedence => 15;

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on
   * propagated type information, then return the parameter element representing the parameter to
   * which the value of the index expression will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.propagatedParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the index
   *         expression will be bound
   */
  ParameterElement get propagatedParameterElementForIndex {
    if (propagatedElement == null) {
      return null;
    }
    List<ParameterElement> parameters = propagatedElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  /**
   * Return the expression used to compute the object being indexed. If this index expression is not
   * part of a cascade expression, then this is the same as [getTarget]. If this index
   * expression is part of a cascade expression, then the target expression stored with the cascade
   * expression is returned.
   *
   * @return the expression used to compute the object being indexed
   * See [target].
   */
  Expression get realTarget {
    if (isCascaded) {
      AstNode ancestor = parent;
      while (ancestor is! CascadeExpression) {
        if (ancestor == null) {
          return _target;
        }
        ancestor = ancestor.parent;
      }
      return (ancestor as CascadeExpression).target;
    }
    return _target;
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on static
   * type information, then return the parameter element representing the parameter to which the
   * value of the index expression will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.staticParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the index
   *         expression will be bound
   */
  ParameterElement get staticParameterElementForIndex {
    if (staticElement == null) {
      return null;
    }
    List<ParameterElement> parameters = staticElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  /**
   * Return the expression used to compute the object being indexed, or `null` if this index
   * expression is part of a cascade expression.
   *
   * @return the expression used to compute the object being indexed
   * See [realTarget].
   */
  Expression get target => _target;

  /**
   * Set the expression used to compute the object being indexed to the given expression.
   *
   * @param expression the expression used to compute the object being indexed
   */
  void set target(Expression expression) {
    _target = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitIndexExpression(this);

  /**
   * Return `true` if this expression is computing a right-hand value.
   *
   * Note that [inGetterContext] and [inSetterContext] are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return `true`
   * when invoked on the same node.
   *
   * @return `true` if this expression is in a context where the operator '[]' will be invoked
   */
  bool inGetterContext() {
    AstNode parent = this.parent;
    if (parent is AssignmentExpression) {
      AssignmentExpression assignment = parent;
      if (identical(assignment.leftHandSide, this) &&
          assignment.operator.type == TokenType.EQ) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return `true` if this expression is computing a left-hand value.
   *
   * Note that [inGetterContext] and [inSetterContext] are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return `true`
   * when invoked on the same node.
   *
   * @return `true` if this expression is in a context where the operator '[]=' will be
   *         invoked
   */
  bool inSetterContext() {
    AstNode parent = this.parent;
    if (parent is PrefixExpression) {
      return parent.operator.type.isIncrementOperator;
    } else if (parent is PostfixExpression) {
      return true;
    } else if (parent is AssignmentExpression) {
      return identical(parent.leftHandSide, this);
    }
    return false;
  }

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_target, visitor);
    safelyVisitChild(_index, visitor);
  }
}

/**
 * Instances of the class `InstanceCreationExpression` represent an instance creation
 * expression.
 *
 * <pre>
 * newExpression ::=
 *     ('new' | 'const') [TypeName] ('.' [SimpleIdentifier])? [ArgumentList]
 * </pre>
 */
class InstanceCreationExpression extends Expression {
  /**
   * The keyword used to indicate how an object should be created.
   */
  Token keyword;

  /**
   * The name of the constructor to be invoked.
   */
  ConstructorName _constructorName;

  /**
   * The list of arguments to the constructor.
   */
  ArgumentList _argumentList;

  /**
   * The element associated with the constructor based on static type information, or `null`
   * if the AST structure has not been resolved or if the constructor could not be resolved.
   */
  ConstructorElement staticElement;

  /**
   * The result of evaluating this expression, if it is constant.
   */
  EvaluationResultImpl evaluationResult;

  /**
   * Initialize a newly created instance creation expression.
   *
   * @param keyword the keyword used to indicate how an object should be created
   * @param constructorName the name of the constructor to be invoked
   * @param argumentList the list of arguments to the constructor
   */
  InstanceCreationExpression(this.keyword, ConstructorName constructorName,
      ArgumentList argumentList) {
    _constructorName = becomeParentOf(constructorName);
    _argumentList = becomeParentOf(argumentList);
  }

  /**
   * Return the list of arguments to the constructor.
   *
   * @return the list of arguments to the constructor
   */
  ArgumentList get argumentList => _argumentList;

  /**
   * Set the list of arguments to the constructor to the given list.
   *
   * @param argumentList the list of arguments to the constructor
   */
  void set argumentList(ArgumentList argumentList) {
    _argumentList = becomeParentOf(argumentList);
  }

  @override
  Token get beginToken => keyword;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(_constructorName)
      ..add(_argumentList);

  /**
   * Return the name of the constructor to be invoked.
   *
   * @return the name of the constructor to be invoked
   */
  ConstructorName get constructorName => _constructorName;

  /**
   * Set the name of the constructor to be invoked to the given name.
   *
   * @param constructorName the name of the constructor to be invoked
   */
  void set constructorName(ConstructorName constructorName) {
    _constructorName = becomeParentOf(constructorName);
  }

  @override
  Token get endToken => _argumentList.endToken;

  /**
   * Return `true` if this creation expression is used to invoke a constant constructor.
   *
   * @return `true` if this creation expression is used to invoke a constant constructor
   */
  bool get isConst =>
      keyword is KeywordToken && (keyword as KeywordToken).keyword == Keyword.CONST;

  @override
  int get precedence => 16;

  @override
  accept(AstVisitor visitor) => visitor.visitInstanceCreationExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_constructorName, visitor);
    safelyVisitChild(_argumentList, visitor);
  }
}

/**
 * Instances of the class `IntegerLiteral` represent an integer literal expression.
 *
 * <pre>
 * integerLiteral ::=
 *     decimalIntegerLiteral
 *   | hexidecimalIntegerLiteral
 *
 * decimalIntegerLiteral ::=
 *     decimalDigit+
 *
 * hexidecimalIntegerLiteral ::=
 *     '0x' hexidecimalDigit+
 *   | '0X' hexidecimalDigit+
 * </pre>
 */
class IntegerLiteral extends Literal {
  /**
   * The token representing the literal.
   */
  Token literal;

  /**
   * The value of the literal.
   */
  int value = 0;

  /**
   * Initialize a newly created integer literal.
   *
   * @param literal the token representing the literal
   * @param value the value of the literal
   */
  IntegerLiteral(this.literal, this.value);

  @override
  Token get beginToken => literal;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(literal);

  @override
  Token get endToken => literal;

  @override
  accept(AstVisitor visitor) => visitor.visitIntegerLiteral(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * The abstract class `InterpolationElement` defines the behavior common to elements within a
 * [StringInterpolation].
 *
 * <pre>
 * interpolationElement ::=
 *     [InterpolationExpression]
 *   | [InterpolationString]
 * </pre>
 */
abstract class InterpolationElement extends AstNode {
}

/**
 * Instances of the class `InterpolationExpression` represent an expression embedded in a
 * string interpolation.
 *
 * <pre>
 * interpolationExpression ::=
 *     '$' [SimpleIdentifier]
 *   | '$' '{' [Expression] '}'
 * </pre>
 */
class InterpolationExpression extends InterpolationElement {
  /**
   * The token used to introduce the interpolation expression; either '$' if the expression is a
   * simple identifier or '${' if the expression is a full expression.
   */
  Token leftBracket;

  /**
   * The expression to be evaluated for the value to be converted into a string.
   */
  Expression _expression;

  /**
   * The right curly bracket, or `null` if the expression is an identifier without brackets.
   */
  Token rightBracket;

  /**
   * Initialize a newly created interpolation expression.
   *
   * @param leftBracket the left curly bracket
   * @param expression the expression to be evaluated for the value to be converted into a string
   * @param rightBracket the right curly bracket
   */
  InterpolationExpression(this.leftBracket, Expression expression,
      this.rightBracket) {
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken => leftBracket;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(leftBracket)
      ..add(_expression)
      ..add(rightBracket);

  @override
  Token get endToken {
    if (rightBracket != null) {
      return rightBracket;
    }
    return _expression.endToken;
  }

  /**
   * Return the expression to be evaluated for the value to be converted into a string.
   *
   * @return the expression to be evaluated for the value to be converted into a string
   */
  Expression get expression => _expression;

  /**
   * Set the expression to be evaluated for the value to be converted into a string to the given
   * expression.
   *
   * @param expression the expression to be evaluated for the value to be converted into a string
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitInterpolationExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * Instances of the class `InterpolationString` represent a non-empty substring of an
 * interpolated string.
 *
 * <pre>
 * interpolationString ::=
 *     characters
 * </pre>
 */
class InterpolationString extends InterpolationElement {
  /**
   * The characters that will be added to the string.
   */
  Token contents;

  /**
   * The value of the literal.
   */
  String _value;

  /**
   * Initialize a newly created string of characters that are part of a string interpolation.
   *
   * @param the characters that will be added to the string
   * @param value the value of the literal
   */
  InterpolationString(this.contents, String value) {
    _value = value;
  }

  @override
  Token get beginToken => contents;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(contents);

  /**
   * Return the offset of the after-last contents character.
   */
  int get contentsEnd {
    int end = contents.end;
    String lexeme = contents.lexeme;
    if (StringUtilities.endsWith3(lexeme, 0x22, 0x22, 0x22) ||
        StringUtilities.endsWith3(lexeme, 0x27, 0x27, 0x27)) {
      end -= 3;
    } else {
      end -= 1;
    }
    return end;
  }

  /**
   * Return the offset of the first contents character.
   */
  int get contentsOffset {
    int offset = contents.offset;
    String lexeme = contents.lexeme;
    if (lexeme.codeUnitAt(0) == 0x72) {
      offset += 1;
    }
    if (StringUtilities.startsWith3(lexeme, offset, 0x22, 0x22, 0x22) ||
        StringUtilities.startsWith3(lexeme, offset, 0x27, 0x27, 0x27)) {
      offset += 3;
    } else {
      offset += 1;
    }
    return offset;
  }

  @override
  Token get endToken => contents;

  /**
   * Return the value of the literal.
   *
   * @return the value of the literal
   */
  String get value => _value;

  /**
   * Set the value of the literal to the given string.
   *
   * @param string the value of the literal
   */
  void set value(String string) {
    _value = string;
  }

  @override
  accept(AstVisitor visitor) => visitor.visitInterpolationString(this);

  @override
  void visitChildren(AstVisitor visitor) {
  }
}

/**
 * Instances of the class `IsExpression` represent an is expression.
 *
 * <pre>
 * isExpression ::=
 *     [Expression] 'is' '!'? [TypeName]
 * </pre>
 */
class IsExpression extends Expression {
  /**
   * The expression used to compute the value whose type is being tested.
   */
  Expression _expression;

  /**
   * The is operator.
   */
  Token isOperator;

  /**
   * The not operator, or `null` if the sense of the test is not negated.
   */
  Token notOperator;

  /**
   * The name of the type being tested for.
   */
  TypeName _type;

  /**
   * Initialize a newly created is expression.
   *
   * @param expression the expression used to compute the value whose type is being tested
   * @param isOperator the is operator
   * @param notOperator the not operator, or `null` if the sense of the test is not negated
   * @param type the name of the type being tested for
   */
  IsExpression(Expression expression, this.isOperator, this.notOperator,
      TypeName type) {
    _expression = becomeParentOf(expression);
    _type = becomeParentOf(type);
  }

  @override
  Token get beginToken => _expression.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_expression)
      ..add(isOperator)
      ..add(notOperator)
      ..add(_type);

  @override
  Token get endToken => _type.endToken;

  /**
   * Return the expression used to compute the value whose type is being tested.
   *
   * @return the expression used to compute the value whose type is being tested
   */
  Expression get expression => _expression;

  /**
   * Set the expression used to compute the value whose type is being tested to the given
   * expression.
   *
   * @param expression the expression used to compute the value whose type is being tested
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  int get precedence => 7;

  /**
   * Return the name of the type being tested for.
   *
   * @return the name of the type being tested for
   */
  TypeName get type => _type;

  /**
   * Set the name of the type being tested for to the given name.
   *
   * @param name the name of the type being tested for
   */
  void set type(TypeName name) {
    _type = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitIsExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
    safelyVisitChild(_type, visitor);
  }
}

/**
 * Instances of the class `Label` represent a label.
 *
 * <pre>
 * label ::=
 *     [SimpleIdentifier] ':'
 * </pre>
 */
class Label extends AstNode {
  /**
   * The label being associated with the statement.
   */
  SimpleIdentifier _label;

  /**
   * The colon that separates the label from the statement.
   */
  Token colon;

  /**
   * Initialize a newly created label.
   *
   * @param label the label being applied
   * @param colon the colon that separates the label from whatever follows
   */
  Label(SimpleIdentifier label, this.colon) {
    _label = becomeParentOf(label);
  }

  @override
  Token get beginToken => _label.beginToken;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_label)
      ..add(colon);

  @override
  Token get endToken => colon;

  /**
   * Return the label being associated with the statement.
   *
   * @return the label being associated with the statement
   */
  SimpleIdentifier get label => _label;

  /**
   * Set the label being associated with the statement to the given label.
   *
   * @param label the label being associated with the statement
   */
  void set label(SimpleIdentifier label) {
    _label = becomeParentOf(label);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitLabel(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_label, visitor);
  }
}

/**
 * Instances of the class `LabeledStatement` represent a statement that has a label associated
 * with them.
 *
 * <pre>
 * labeledStatement ::=
 *    [Label]+ [Statement]
 * </pre>
 */
class LabeledStatement extends Statement {
  /**
   * The labels being associated with the statement.
   */
  NodeList<Label> _labels;

  /**
   * The statement with which the labels are being associated.
   */
  Statement _statement;

  /**
   * Initialize a newly created labeled statement.
   *
   * @param labels the labels being associated with the statement
   * @param statement the statement with which the labels are being associated
   */
  LabeledStatement(List<Label> labels, Statement statement) {
    _labels = new NodeList<Label>(this, labels);
    _statement = becomeParentOf(statement);
  }

  @override
  Token get beginToken {
    if (!_labels.isEmpty) {
      return _labels.beginToken;
    }
    return _statement.beginToken;
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..addAll(_labels)
      ..add(_statement);

  @override
  Token get endToken => _statement.endToken;

  /**
   * Return the labels being associated with the statement.
   *
   * @return the labels being associated with the statement
   */
  NodeList<Label> get labels => _labels;

  /**
   * Return the statement with which the labels are being associated.
   *
   * @return the statement with which the labels are being associated
   */
  Statement get statement => _statement;

  /**
   * Set the statement with which the labels are being associated to the given statement.
   *
   * @param statement the statement with which the labels are being associated
   */
  void set statement(Statement statement) {
    _statement = becomeParentOf(statement);
  }

  @override
  Statement get unlabeled => _statement.unlabeled;

  @override
  accept(AstVisitor visitor) => visitor.visitLabeledStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _labels.accept(visitor);
    safelyVisitChild(_statement, visitor);
  }
}

/**
 * Instances of the class `LibraryDirective` represent a library directive.
 *
 * <pre>
 * libraryDirective ::=
 *     [Annotation] 'library' [Identifier] ';'
 * </pre>
 */
class LibraryDirective extends Directive {
  /**
   * The token representing the 'library' token.
   */
  Token libraryToken;

  /**
   * The name of the library being defined.
   */
  LibraryIdentifier _name;

  /**
   * The semicolon terminating the directive.
   */
  Token semicolon;

  /**
   * Initialize a newly created library directive.
   *
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param libraryToken the token representing the 'library' token
   * @param name the name of the library being defined
   * @param semicolon the semicolon terminating the directive
   */
  LibraryDirective(Comment comment, List<Annotation> metadata,
      this.libraryToken, LibraryIdentifier name, this.semicolon)
      : super(comment, metadata) {
    _name = becomeParentOf(name);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(libraryToken)
      ..add(_name)
      ..add(semicolon);

  @override
  Token get endToken => semicolon;

  @override
  Token get firstTokenAfterCommentAndMetadata => libraryToken;

  @override
  Token get keyword => libraryToken;

  /**
   * Return the name of the library being defined.
   *
   * @return the name of the library being defined
   */
  LibraryIdentifier get name => _name;

  /**
   * Set the name of the library being defined to the given name.
   *
   * @param name the name of the library being defined
   */
  void set name(LibraryIdentifier name) {
    _name = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitLibraryDirective(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_name, visitor);
  }
}

/**
 * Instances of the class `LibraryIdentifier` represent the identifier for a library.
 *
 * <pre>
 * libraryIdentifier ::=
 *     [SimpleIdentifier] ('.' [SimpleIdentifier])*
 * </pre>
 */
class LibraryIdentifier extends Identifier {
  /**
   * The components of the identifier.
   */
  NodeList<SimpleIdentifier> _components;

  /**
   * Initialize a newly created prefixed identifier.
   *
   * @param components the components of the identifier
   */
  LibraryIdentifier(List<SimpleIdentifier> components) {
    _components = new NodeList<SimpleIdentifier>(this, components);
  }

  @override
  Token get beginToken => _components.beginToken;

  @override
  Element get bestElement => staticElement;

  /**
   * TODO(paulberry): add "." tokens.
   */
  @override
  Iterable get childEntities => new ChildEntities()..addAll(_components);

  /**
   * Return the components of the identifier.
   *
   * @return the components of the identifier
   */
  NodeList<SimpleIdentifier> get components => _components;

  @override
  Token get endToken => _components.endToken;

  @override
  String get name {
    StringBuffer buffer = new StringBuffer();
    bool needsPeriod = false;
    for (SimpleIdentifier identifier in _components) {
      if (needsPeriod) {
        buffer.write(".");
      } else {
        needsPeriod = true;
      }
      buffer.write(identifier.name);
    }
    return buffer.toString();
  }

  @override
  int get precedence => 15;

  @override
  Element get propagatedElement => null;

  @override
  Element get staticElement => null;

  @override
  accept(AstVisitor visitor) => visitor.visitLibraryIdentifier(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _components.accept(visitor);
  }
}

/**
 * Instances of the class `ListLiteral` represent a list literal.
 *
 * <pre>
 * listLiteral ::=
 *     'const'? ('<' [TypeName] '>')? '[' ([Expression] ','?)? ']'
 * </pre>
 */
class ListLiteral extends TypedLiteral {
  /**
   * The left square bracket.
   */
  Token leftBracket;

  /**
   * The expressions used to compute the elements of the list.
   */
  NodeList<Expression> _elements;

  /**
   * The right square bracket.
   */
  Token rightBracket;

  /**
   * Initialize a newly created list literal.
   *
   * @param constKeyword the token representing the 'const' keyword
   * @param typeArguments the type argument associated with this literal, or `null` if no type
   *          arguments were declared
   * @param leftBracket the left square bracket
   * @param elements the expressions used to compute the elements of the list
   * @param rightBracket the right square bracket
   */
  ListLiteral(Token constKeyword, TypeArgumentList typeArguments,
      this.leftBracket, List<Expression> elements, this.rightBracket)
      : super(constKeyword, typeArguments) {
    _elements = new NodeList<Expression>(this, elements);
  }

  @override
  Token get beginToken {
    if (constKeyword != null) {
      return constKeyword;
    }
    TypeArgumentList typeArguments = this.typeArguments;
    if (typeArguments != null) {
      return typeArguments.beginToken;
    }
    return leftBracket;
  }

  /**
   * TODO(paulberry): add commas.
   */
  @override
  Iterable get childEntities => super._childEntities
      ..add(leftBracket)
      ..addAll(_elements)
      ..add(rightBracket);

  /**
   * Return the expressions used to compute the elements of the list.
   *
   * @return the expressions used to compute the elements of the list
   */
  NodeList<Expression> get elements => _elements;

  @override
  Token get endToken => rightBracket;

  @override
  accept(AstVisitor visitor) => visitor.visitListLiteral(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    _elements.accept(visitor);
  }
}

/**
 * The abstract class `Literal` defines the behavior common to nodes that represent a literal
 * expression.
 *
 * <pre>
 * literal ::=
 *     [BooleanLiteral]
 *   | [DoubleLiteral]
 *   | [IntegerLiteral]
 *   | [ListLiteral]
 *   | [MapLiteral]
 *   | [NullLiteral]
 *   | [StringLiteral]
 * </pre>
 */
abstract class Literal extends Expression {
  @override
  int get precedence => 16;
}

/**
 * Instances of the class `MapLiteral` represent a literal map.
 *
 * <pre>
 * mapLiteral ::=
 *     'const'? ('<' [TypeName] (',' [TypeName])* '>')? '{' ([MapLiteralEntry] (',' [MapLiteralEntry])* ','?)? '}'
 * </pre>
 */
class MapLiteral extends TypedLiteral {
  /**
   * The left curly bracket.
   */
  Token leftBracket;

  /**
   * The entries in the map.
   */
  NodeList<MapLiteralEntry> _entries;

  /**
   * The right curly bracket.
   */
  Token rightBracket;

  /**
   * Initialize a newly created map literal.
   *
   * @param constKeyword the token representing the 'const' keyword
   * @param typeArguments the type argument associated with this literal, or `null` if no type
   *          arguments were declared
   * @param leftBracket the left curly bracket
   * @param entries the entries in the map
   * @param rightBracket the right curly bracket
   */
  MapLiteral(Token constKeyword, TypeArgumentList typeArguments,
      this.leftBracket, List<MapLiteralEntry> entries, this.rightBracket)
      : super(constKeyword, typeArguments) {
    _entries = new NodeList<MapLiteralEntry>(this, entries);
  }

  @override
  Token get beginToken {
    if (constKeyword != null) {
      return constKeyword;
    }
    TypeArgumentList typeArguments = this.typeArguments;
    if (typeArguments != null) {
      return typeArguments.beginToken;
    }
    return leftBracket;
  }

  /**
   * TODO(paulberry): untested.
   * TODO(paulberry): add commas.
   */
  @override
  Iterable get childEntities => super._childEntities
      ..add(leftBracket)
      ..addAll(entries)
      ..add(rightBracket);

  @override
  Token get endToken => rightBracket;

  /**
   * Return the entries in the map.
   *
   * @return the entries in the map
   */
  NodeList<MapLiteralEntry> get entries => _entries;

  @override
  accept(AstVisitor visitor) => visitor.visitMapLiteral(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    _entries.accept(visitor);
  }
}

/**
 * Instances of the class `MapLiteralEntry` represent a single key/value pair in a map
 * literal.
 *
 * <pre>
 * mapLiteralEntry ::=
 *     [Expression] ':' [Expression]
 * </pre>
 */
class MapLiteralEntry extends AstNode {
  /**
   * The expression computing the key with which the value will be associated.
   */
  Expression _key;

  /**
   * The colon that separates the key from the value.
   */
  Token separator;

  /**
   * The expression computing the value that will be associated with the key.
   */
  Expression _value;

  /**
   * Initialize a newly created map literal entry.
   *
   * @param key the expression computing the key with which the value will be associated
   * @param separator the colon that separates the key from the value
   * @param value the expression computing the value that will be associated with the key
   */
  MapLiteralEntry(Expression key, this.separator, Expression value) {
    _key = becomeParentOf(key);
    _value = becomeParentOf(value);
  }

  @override
  Token get beginToken => _key.beginToken;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_key)
      ..add(separator)
      ..add(_value);

  @override
  Token get endToken => _value.endToken;

  /**
   * Return the expression computing the key with which the value will be associated.
   *
   * @return the expression computing the key with which the value will be associated
   */
  Expression get key => _key;

  /**
   * Set the expression computing the key with which the value will be associated to the given
   * string.
   *
   * @param string the expression computing the key with which the value will be associated
   */
  void set key(Expression string) {
    _key = becomeParentOf(string);
  }

  /**
   * Return the expression computing the value that will be associated with the key.
   *
   * @return the expression computing the value that will be associated with the key
   */
  Expression get value => _value;

  /**
   * Set the expression computing the value that will be associated with the key to the given
   * expression.
   *
   * @param expression the expression computing the value that will be associated with the key
   */
  void set value(Expression expression) {
    _value = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitMapLiteralEntry(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_key, visitor);
    safelyVisitChild(_value, visitor);
  }
}

/**
 * Instances of the class `MethodDeclaration` represent a method declaration.
 *
 * <pre>
 * methodDeclaration ::=
 *     methodSignature [FunctionBody]
 *
 * methodSignature ::=
 *     'external'? ('abstract' | 'static')? [Type]? ('get' | 'set')? methodName
 *     [FormalParameterList]
 *
 * methodName ::=
 *     [SimpleIdentifier]
 *   | 'operator' [SimpleIdentifier]
 * </pre>
 */
class MethodDeclaration extends ClassMember {
  /**
   * The token for the 'external' keyword, or `null` if the constructor is not external.
   */
  Token externalKeyword;

  /**
   * The token representing the 'abstract' or 'static' keyword, or `null` if neither modifier
   * was specified.
   */
  Token modifierKeyword;

  /**
   * The return type of the method, or `null` if no return type was declared.
   */
  TypeName _returnType;

  /**
   * The token representing the 'get' or 'set' keyword, or `null` if this is a method
   * declaration rather than a property declaration.
   */
  Token propertyKeyword;

  /**
   * The token representing the 'operator' keyword, or `null` if this method does not declare
   * an operator.
   */
  Token operatorKeyword;

  /**
   * The name of the method.
   */
  SimpleIdentifier _name;

  /**
   * The parameters associated with the method, or `null` if this method declares a getter.
   */
  FormalParameterList _parameters;

  /**
   * The body of the method.
   */
  FunctionBody _body;

  /**
   * Initialize a newly created method declaration.
   *
   * @param externalKeyword the token for the 'external' keyword
   * @param comment the documentation comment associated with this method
   * @param metadata the annotations associated with this method
   * @param modifierKeyword the token representing the 'abstract' or 'static' keyword
   * @param returnType the return type of the method
   * @param propertyKeyword the token representing the 'get' or 'set' keyword
   * @param operatorKeyword the token representing the 'operator' keyword
   * @param name the name of the method
   * @param parameters the parameters associated with the method, or `null` if this method
   *          declares a getter
   * @param body the body of the method
   */
  MethodDeclaration(Comment comment, List<Annotation> metadata,
      this.externalKeyword, this.modifierKeyword, TypeName returnType,
      this.propertyKeyword, this.operatorKeyword, SimpleIdentifier name,
      FormalParameterList parameters, FunctionBody body)
      : super(comment, metadata) {
    _returnType = becomeParentOf(returnType);
    _name = becomeParentOf(name);
    _parameters = becomeParentOf(parameters);
    _body = becomeParentOf(body);
  }

  /**
   * Return the body of the method.
   *
   * @return the body of the method
   */
  FunctionBody get body => _body;

  /**
   * Set the body of the method to the given function body.
   *
   * @param functionBody the body of the method
   */
  void set body(FunctionBody functionBody) {
    _body = becomeParentOf(functionBody);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(externalKeyword)
      ..add(modifierKeyword)
      ..add(_returnType)
      ..add(propertyKeyword)
      ..add(operatorKeyword)
      ..add(_name)
      ..add(_parameters)
      ..add(_body);

  /**
   * Return the element associated with this method, or `null` if the AST structure has not
   * been resolved. The element can either be a [MethodElement], if this represents the
   * declaration of a normal method, or a [PropertyAccessorElement] if this represents the
   * declaration of either a getter or a setter.
   *
   * @return the element associated with this method
   */
  @override
  ExecutableElement get element =>
      _name != null ? (_name.staticElement as ExecutableElement) : null;

  @override
  Token get endToken => _body.endToken;

  @override
  Token get firstTokenAfterCommentAndMetadata {
    if (modifierKeyword != null) {
      return modifierKeyword;
    } else if (_returnType != null) {
      return _returnType.beginToken;
    } else if (propertyKeyword != null) {
      return propertyKeyword;
    } else if (operatorKeyword != null) {
      return operatorKeyword;
    }
    return _name.beginToken;
  }

  /**
   * Return `true` if this method is declared to be an abstract method.
   *
   * @return `true` if this method is declared to be an abstract method
   */
  bool get isAbstract =>
      externalKeyword == null && (_body is EmptyFunctionBody);

  /**
   * Return `true` if this method declares a getter.
   *
   * @return `true` if this method declares a getter
   */
  bool get isGetter =>
      propertyKeyword != null &&
          (propertyKeyword as KeywordToken).keyword == Keyword.GET;

  /**
   * Return `true` if this method declares an operator.
   *
   * @return `true` if this method declares an operator
   */
  bool get isOperator => operatorKeyword != null;

  /**
   * Return `true` if this method declares a setter.
   *
   * @return `true` if this method declares a setter
   */
  bool get isSetter =>
      propertyKeyword != null &&
          (propertyKeyword as KeywordToken).keyword == Keyword.SET;

  /**
   * Return `true` if this method is declared to be a static method.
   *
   * @return `true` if this method is declared to be a static method
   */
  bool get isStatic =>
      modifierKeyword != null &&
          (modifierKeyword as KeywordToken).keyword == Keyword.STATIC;

  /**
   * Return the name of the method.
   *
   * @return the name of the method
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the method to the given identifier.
   *
   * @param identifier the name of the method
   */
  void set name(SimpleIdentifier identifier) {
    _name = becomeParentOf(identifier);
  }

  /**
   * Return the parameters associated with the method, or `null` if this method declares a
   * getter.
   *
   * @return the parameters associated with the method
   */
  FormalParameterList get parameters => _parameters;

  /**
   * Set the parameters associated with the method to the given list of parameters.
   *
   * @param parameters the parameters associated with the method
   */
  void set parameters(FormalParameterList parameters) {
    _parameters = becomeParentOf(parameters);
  }

  /**
   * Return the return type of the method, or `null` if no return type was declared.
   *
   * @return the return type of the method
   */
  TypeName get returnType => _returnType;

  /**
   * Set the return type of the method to the given type name.
   *
   * @param typeName the return type of the method
   */
  void set returnType(TypeName typeName) {
    _returnType = becomeParentOf(typeName);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitMethodDeclaration(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_returnType, visitor);
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_parameters, visitor);
    safelyVisitChild(_body, visitor);
  }
}

/**
 * Instances of the class `MethodInvocation` represent the invocation of either a function or
 * a method. Invocations of functions resulting from evaluating an expression are represented by
 * [FunctionExpressionInvocation] nodes. Invocations of getters
 * and setters are represented by either [PrefixedIdentifier] or
 * [PropertyAccess] nodes.
 *
 * <pre>
 * methodInvoction ::=
 *     ([Expression] '.')? [SimpleIdentifier] [ArgumentList]
 * </pre>
 */
class MethodInvocation extends Expression {
  /**
   * The expression producing the object on which the method is defined, or `null` if there is
   * no target (that is, the target is implicitly `this`).
   */
  Expression _target;

  /**
   * The period that separates the target from the method name, or `null` if there is no
   * target.
   */
  Token period;

  /**
   * The name of the method being invoked.
   */
  SimpleIdentifier _methodName;

  /**
   * The list of arguments to the method.
   */
  ArgumentList _argumentList;

  /**
   * Initialize a newly created method invocation.
   *
   * @param target the expression producing the object on which the method is defined
   * @param period the period that separates the target from the method name
   * @param methodName the name of the method being invoked
   * @param argumentList the list of arguments to the method
   */
  MethodInvocation(Expression target, this.period, SimpleIdentifier methodName,
      ArgumentList argumentList) {
    _target = becomeParentOf(target);
    _methodName = becomeParentOf(methodName);
    _argumentList = becomeParentOf(argumentList);
  }

  /**
   * Return the list of arguments to the method.
   *
   * @return the list of arguments to the method
   */
  ArgumentList get argumentList => _argumentList;

  /**
   * Set the list of arguments to the method to the given list.
   *
   * @param argumentList the list of arguments to the method
   */
  void set argumentList(ArgumentList argumentList) {
    _argumentList = becomeParentOf(argumentList);
  }

  @override
  Token get beginToken {
    if (_target != null) {
      return _target.beginToken;
    } else if (period != null) {
      return period;
    }
    return _methodName.beginToken;
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_target)
      ..add(period)
      ..add(_methodName)
      ..add(_argumentList);

  @override
  Token get endToken => _argumentList.endToken;

  /**
   * Return `true` if this expression is cascaded. If it is, then the target of this
   * expression is not stored locally but is stored in the nearest ancestor that is a
   * [CascadeExpression].
   *
   * @return `true` if this expression is cascaded
   */
  bool get isCascaded =>
      period != null && period.type == TokenType.PERIOD_PERIOD;

  /**
   * Return the name of the method being invoked.
   *
   * @return the name of the method being invoked
   */
  SimpleIdentifier get methodName => _methodName;

  /**
   * Set the name of the method being invoked to the given identifier.
   *
   * @param identifier the name of the method being invoked
   */
  void set methodName(SimpleIdentifier identifier) {
    _methodName = becomeParentOf(identifier);
  }

  @override
  int get precedence => 15;

  /**
   * Return the expression used to compute the receiver of the invocation. If this invocation is not
   * part of a cascade expression, then this is the same as [getTarget]. If this invocation
   * is part of a cascade expression, then the target stored with the cascade expression is
   * returned.
   *
   * @return the expression used to compute the receiver of the invocation
   * See [target].
   */
  Expression get realTarget {
    if (isCascaded) {
      AstNode ancestor = parent;
      while (ancestor is! CascadeExpression) {
        if (ancestor == null) {
          return _target;
        }
        ancestor = ancestor.parent;
      }
      return (ancestor as CascadeExpression).target;
    }
    return _target;
  }

  /**
   * Return the expression producing the object on which the method is defined, or `null` if
   * there is no target (that is, the target is implicitly `this`) or if this method
   * invocation is part of a cascade expression.
   *
   * @return the expression producing the object on which the method is defined
   * See [realTarget].
   */
  Expression get target => _target;

  /**
   * Set the expression producing the object on which the method is defined to the given expression.
   *
   * @param expression the expression producing the object on which the method is defined
   */
  void set target(Expression expression) {
    _target = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitMethodInvocation(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_target, visitor);
    safelyVisitChild(_methodName, visitor);
    safelyVisitChild(_argumentList, visitor);
  }
}

/**
 * Instances of the class `NamedExpression` represent an expression that has a name associated
 * with it. They are used in method invocations when there are named parameters.
 *
 * <pre>
 * namedExpression ::=
 *     [Label] [Expression]
 * </pre>
 */
class NamedExpression extends Expression {
  /**
   * The name associated with the expression.
   */
  Label _name;

  /**
   * The expression with which the name is associated.
   */
  Expression _expression;

  /**
   * Initialize a newly created named expression.
   *
   * @param name the name associated with the expression
   * @param expression the expression with which the name is associated
   */
  NamedExpression(Label name, Expression expression) {
    _name = becomeParentOf(name);
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken => _name.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_name)
      ..add(_expression);

  /**
   * Return the element representing the parameter being named by this expression, or `null`
   * if the AST structure has not been resolved or if there is no parameter with the same name as
   * this expression.
   *
   * @return the element representing the parameter being named by this expression
   */
  ParameterElement get element {
    Element element = _name.label.staticElement;
    if (element is ParameterElement) {
      return element;
    }
    return null;
  }

  @override
  Token get endToken => _expression.endToken;

  /**
   * Return the expression with which the name is associated.
   *
   * @return the expression with which the name is associated
   */
  Expression get expression => _expression;

  /**
   * Set the expression with which the name is associated to the given expression.
   *
   * @param expression the expression with which the name is associated
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  /**
   * Return the name associated with the expression.
   *
   * @return the name associated with the expression
   */
  Label get name => _name;

  /**
   * Set the name associated with the expression to the given identifier.
   *
   * @param identifier the name associated with the expression
   */
  void set name(Label identifier) {
    _name = becomeParentOf(identifier);
  }

  @override
  int get precedence => 0;

  @override
  accept(AstVisitor visitor) => visitor.visitNamedExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * The abstract class `NamespaceDirective` defines the behavior common to nodes that represent
 * a directive that impacts the namespace of a library.
 *
 * <pre>
 * directive ::=
 *     [ExportDirective]
 *   | [ImportDirective]
 * </pre>
 */
abstract class NamespaceDirective extends UriBasedDirective {
  /**
   * The token representing the 'import' or 'export' keyword.
   */
  Token keyword;

  /**
   * The combinators used to control which names are imported or exported.
   */
  NodeList<Combinator> _combinators;

  /**
   * The semicolon terminating the directive.
   */
  Token semicolon;

  /**
   * Initialize a newly created namespace directive.
   *
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param keyword the token representing the 'import' or 'export' keyword
   * @param libraryUri the URI of the library being imported or exported
   * @param combinators the combinators used to control which names are imported or exported
   * @param semicolon the semicolon terminating the directive
   */
  NamespaceDirective(Comment comment, List<Annotation> metadata, this.keyword,
      StringLiteral libraryUri, List<Combinator> combinators, this.semicolon)
      : super(comment, metadata, libraryUri) {
    _combinators = new NodeList<Combinator>(this, combinators);
  }

  /**
   * Return the combinators used to control how names are imported or exported.
   *
   * @return the combinators used to control how names are imported or exported
   */
  NodeList<Combinator> get combinators => _combinators;

  @override
  Token get endToken => semicolon;

  @override
  Token get firstTokenAfterCommentAndMetadata => keyword;

  @override
  LibraryElement get uriElement;
}

/**
 * Instances of the class `NativeClause` represent the "native" clause in an class
 * declaration.
 *
 * <pre>
 * nativeClause ::=
 *     'native' [StringLiteral]
 * </pre>
 */
class NativeClause extends AstNode {
  /**
   * The token representing the 'native' keyword.
   */
  Token keyword;

  /**
   * The name of the native object that implements the class.
   */
  StringLiteral _name;

  /**
   * Initialize a newly created native clause.
   *
   * @param keyword the token representing the 'native' keyword
   * @param name the name of the native object that implements the class.
   */
  NativeClause(this.keyword, StringLiteral name) {
    _name = becomeParentOf(name);
  }

  @override
  Token get beginToken => keyword;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(_name);

  @override
  Token get endToken => _name.endToken;

  /**
   * Return the name of the native object that implements the class.
   *
   * @return the name of the native object that implements the class
   */
  StringLiteral get name => _name;

  /**
   * Sets the name of the native object that implements the class.
   *
   * @param name the name of the native object that implements the class.
   */
  void set name(StringLiteral name) {
    _name = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitNativeClause(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_name, visitor);
  }
}

/**
 * Instances of the class `NativeFunctionBody` represent a function body that consists of a
 * native keyword followed by a string literal.
 *
 * <pre>
 * nativeFunctionBody ::=
 *     'native' [SimpleStringLiteral] ';'
 * </pre>
 */
class NativeFunctionBody extends FunctionBody {
  /**
   * The token representing 'native' that marks the start of the function body.
   */
  Token nativeToken;

  /**
   * The string literal, after the 'native' token.
   */
  StringLiteral _stringLiteral;

  /**
   * The token representing the semicolon that marks the end of the function body.
   */
  Token semicolon;

  /**
   * Initialize a newly created function body consisting of the 'native' token, a string literal,
   * and a semicolon.
   *
   * @param nativeToken the token representing 'native' that marks the start of the function body
   * @param stringLiteral the string literal
   * @param semicolon the token representing the semicolon that marks the end of the function body
   */
  NativeFunctionBody(this.nativeToken, StringLiteral stringLiteral,
      this.semicolon) {
    _stringLiteral = becomeParentOf(stringLiteral);
  }

  @override
  Token get beginToken => nativeToken;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(nativeToken)
      ..add(_stringLiteral)
      ..add(semicolon);

  @override
  Token get endToken => semicolon;

  /**
   * Return the string literal representing the string after the 'native' token.
   *
   * @return the string literal representing the string after the 'native' token
   */
  StringLiteral get stringLiteral => _stringLiteral;

  /**
   * Set the string literal representing the string after the 'native' token to the given string.
   *
   * @param stringLiteral the string literal representing the string after the 'native' token
   */
  void set stringLiteral(StringLiteral stringLiteral) {
    _stringLiteral = becomeParentOf(stringLiteral);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitNativeFunctionBody(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_stringLiteral, visitor);
  }
}

/**
 * Instances of the class `NodeList` represent a list of AST nodes that have a
 * common parent.
 */
class NodeList<E extends AstNode> extends Object with ListMixin<E> {
  /**
   * The node that is the parent of each of the elements in the list.
   */
  AstNode owner;

  /**
   * The elements contained in the list.
   */
  List<E> _elements = <E>[];

  /**
   * Initialize a newly created list of nodes such that all of the nodes that
   * are added to the list will have their parent set to the given [owner]. The
   * list will initially be populated with the given [elements].
   */
  NodeList(this.owner, [List<E> elements]) {
    addAll(elements);
  }

  /**
   * Return the first token included in this node list's source range, or `null`
   * if the list is empty.
   */
  Token get beginToken {
    if (_elements.length == 0) {
      return null;
    }
    return _elements[0].beginToken;
  }

  /**
   * Return the last token included in this node list's source range, or `null`
   * if the list is empty.
   */
  Token get endToken {
    int length = _elements.length;
    if (length == 0) {
      return null;
    }
    return _elements[length - 1].endToken;
  }

  int get length => _elements.length;

  @deprecated
  void set length(int value) {
    throw new UnsupportedError("Cannot resize NodeList.");
  }

  E operator [](int index) {
    if (index < 0 || index >= _elements.length) {
      throw new RangeError("Index: $index, Size: ${_elements.length}");
    }
    return _elements[index];
  }

  void operator []=(int index, E node) {
    if (index < 0 || index >= _elements.length) {
      throw new RangeError("Index: $index, Size: ${_elements.length}");
    }
    owner.becomeParentOf(node);
    _elements[index] = node;
  }

  /**
   * Use the given [visitor] to visit each of the nodes in this list.
   */
  accept(AstVisitor visitor) {
    int length = _elements.length;
    for (var i = 0; i < length; i++) {
      _elements[i].accept(visitor);
    }
  }

  @override
  void add(E node) {
    insert(length, node);
  }

  @override
  bool addAll(Iterable<E> nodes) {
    if (nodes != null && !nodes.isEmpty) {
      _elements.addAll(nodes);
      for (E node in nodes) {
        owner.becomeParentOf(node);
      }
      return true;
    }
    return false;
  }

  @override
  void clear() {
    _elements = <E>[];
  }

  @override
  void insert(int index, E node) {
    int length = _elements.length;
    if (index < 0 || index > length) {
      throw new RangeError("Index: $index, Size: ${_elements.length}");
    }
    owner.becomeParentOf(node);
    if (length == 0) {
      _elements.add(node);
    } else {
      _elements.insert(index, node);
    }
  }

  @override
  E removeAt(int index) {
    if (index < 0 || index >= _elements.length) {
      throw new RangeError("Index: $index, Size: ${_elements.length}");
    }
    E removedNode = _elements[index];
    _elements.removeAt(index);
    return removedNode;
  }

  /**
   * Create an empty list with the given [owner].
   *
   * Use "new NodeList<E>(owner)"
   */
  @deprecated
  static NodeList create(AstNode owner) => new NodeList(owner);
}

/**
 * Instances of the class `NodeLocator` locate the [AstNode] associated with a
 * source range, given the AST structure built from the source. More specifically, they will return
 * the [AstNode] with the shortest length whose source range completely encompasses
 * the specified range.
 */
class NodeLocator extends UnifyingAstVisitor<Object> {
  /**
   * The start offset of the range used to identify the node.
   */
  int _startOffset = 0;

  /**
   * The end offset of the range used to identify the node.
   */
  int _endOffset = 0;

  /**
   * The element that was found that corresponds to the given source range, or `null` if there
   * is no such element.
   */
  AstNode _foundNode;

  /**
   * Initialize a newly created locator to locate one or more [AstNode] by locating
   * the node within an AST structure that corresponds to the given offset in the source.
   *
   * @param offset the offset used to identify the node
   */
  NodeLocator.con1(int offset) : this.con2(offset, offset);

  /**
   * Initialize a newly created locator to locate one or more [AstNode] by locating
   * the node within an AST structure that corresponds to the given range of characters in the
   * source.
   *
   * @param start the start offset of the range used to identify the node
   * @param end the end offset of the range used to identify the node
   */
  NodeLocator.con2(this._startOffset, this._endOffset);

  /**
   * Return the node that was found that corresponds to the given source range, or `null` if
   * there is no such node.
   *
   * @return the node that was found
   */
  AstNode get foundNode => _foundNode;

  /**
   * Search within the given AST node for an identifier representing a [DartElement] in the specified source range. Return the element that was found, or `null` if
   * no element was found.
   *
   * @param node the AST node within which to search
   * @return the element that was found
   */
  AstNode searchWithin(AstNode node) {
    if (node == null) {
      return null;
    }
    try {
      node.accept(this);
    } on NodeLocator_NodeFoundException catch (exception) {
      // A node with the right source position was found.
    } catch (exception, stackTrace) {
      AnalysisEngine.instance.logger.logInformation(
          "Unable to locate element at offset ($_startOffset - $_endOffset)",
          new CaughtException(exception, stackTrace));
      return null;
    }
    return _foundNode;
  }

  @override
  Object visitNode(AstNode node) {
    int start = node.offset;
    int end = start + node.length;
    if (end < _startOffset) {
      return null;
    }
    if (start > _endOffset) {
      return null;
    }
    try {
      node.visitChildren(this);
    } on NodeLocator_NodeFoundException catch (exception) {
      rethrow;
    } catch (exception, stackTrace) {
      // Ignore the exception and proceed in order to visit the rest of the
      // structure.
      AnalysisEngine.instance.logger.logInformation(
          "Exception caught while traversing an AST structure.",
          new CaughtException(exception, stackTrace));
    }
    if (start <= _startOffset && _endOffset <= end) {
      _foundNode = node;
      throw new NodeLocator_NodeFoundException();
    }
    return null;
  }
}

/**
 * Instances of the class `NodeFoundException` are used to cancel visiting after a node has
 * been found.
 */
class NodeLocator_NodeFoundException extends RuntimeException {
}

/**
 * Instances of the class `NodeReplacer` implement an object that will replace one child node
 * in an AST node with another node.
 */
class NodeReplacer implements AstVisitor<bool> {
  final AstNode _oldNode;

  final AstNode _newNode;

  NodeReplacer(this._oldNode, this._newNode);

  @override
  bool visitAdjacentStrings(AdjacentStrings node) {
    if (_replaceInList(node.strings)) {
      return true;
    }
    return visitNode(node);
  }

  bool visitAnnotatedNode(AnnotatedNode node) {
    if (identical(node.documentationComment, _oldNode)) {
      node.documentationComment = _newNode as Comment;
      return true;
    } else if (_replaceInList(node.metadata)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitAnnotation(Annotation node) {
    if (identical(node.arguments, _oldNode)) {
      node.arguments = _newNode as ArgumentList;
      return true;
    } else if (identical(node.constructorName, _oldNode)) {
      node.constructorName = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.name, _oldNode)) {
      node.name = _newNode as Identifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitArgumentList(ArgumentList node) {
    if (_replaceInList(node.arguments)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitAsExpression(AsExpression node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    } else if (identical(node.type, _oldNode)) {
      node.type = _newNode as TypeName;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitAssertStatement(AssertStatement node) {
    if (identical(node.condition, _oldNode)) {
      node.condition = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitAssignmentExpression(AssignmentExpression node) {
    if (identical(node.leftHandSide, _oldNode)) {
      node.leftHandSide = _newNode as Expression;
      return true;
    } else if (identical(node.rightHandSide, _oldNode)) {
      node.rightHandSide = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitAwaitExpression(AwaitExpression node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
    }
    return visitNode(node);
  }

  @override
  bool visitBinaryExpression(BinaryExpression node) {
    if (identical(node.leftOperand, _oldNode)) {
      node.leftOperand = _newNode as Expression;
      return true;
    } else if (identical(node.rightOperand, _oldNode)) {
      node.rightOperand = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitBlock(Block node) {
    if (_replaceInList(node.statements)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitBlockFunctionBody(BlockFunctionBody node) {
    if (identical(node.block, _oldNode)) {
      node.block = _newNode as Block;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitBooleanLiteral(BooleanLiteral node) => visitNode(node);

  @override
  bool visitBreakStatement(BreakStatement node) {
    if (identical(node.label, _oldNode)) {
      node.label = _newNode as SimpleIdentifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitCascadeExpression(CascadeExpression node) {
    if (identical(node.target, _oldNode)) {
      node.target = _newNode as Expression;
      return true;
    } else if (_replaceInList(node.cascadeSections)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitCatchClause(CatchClause node) {
    if (identical(node.exceptionType, _oldNode)) {
      node.exceptionType = _newNode as TypeName;
      return true;
    } else if (identical(node.exceptionParameter, _oldNode)) {
      node.exceptionParameter = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.stackTraceParameter, _oldNode)) {
      node.stackTraceParameter = _newNode as SimpleIdentifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitClassDeclaration(ClassDeclaration node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.typeParameters, _oldNode)) {
      node.typeParameters = _newNode as TypeParameterList;
      return true;
    } else if (identical(node.extendsClause, _oldNode)) {
      node.extendsClause = _newNode as ExtendsClause;
      return true;
    } else if (identical(node.withClause, _oldNode)) {
      node.withClause = _newNode as WithClause;
      return true;
    } else if (identical(node.implementsClause, _oldNode)) {
      node.implementsClause = _newNode as ImplementsClause;
      return true;
    } else if (identical(node.nativeClause, _oldNode)) {
      node.nativeClause = _newNode as NativeClause;
      return true;
    } else if (_replaceInList(node.members)) {
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitClassTypeAlias(ClassTypeAlias node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.typeParameters, _oldNode)) {
      node.typeParameters = _newNode as TypeParameterList;
      return true;
    } else if (identical(node.superclass, _oldNode)) {
      node.superclass = _newNode as TypeName;
      return true;
    } else if (identical(node.withClause, _oldNode)) {
      node.withClause = _newNode as WithClause;
      return true;
    } else if (identical(node.implementsClause, _oldNode)) {
      node.implementsClause = _newNode as ImplementsClause;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitComment(Comment node) {
    if (_replaceInList(node.references)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitCommentReference(CommentReference node) {
    if (identical(node.identifier, _oldNode)) {
      node.identifier = _newNode as Identifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitCompilationUnit(CompilationUnit node) {
    if (identical(node.scriptTag, _oldNode)) {
      node.scriptTag = _newNode as ScriptTag;
      return true;
    } else if (_replaceInList(node.directives)) {
      return true;
    } else if (_replaceInList(node.declarations)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitConditionalExpression(ConditionalExpression node) {
    if (identical(node.condition, _oldNode)) {
      node.condition = _newNode as Expression;
      return true;
    } else if (identical(node.thenExpression, _oldNode)) {
      node.thenExpression = _newNode as Expression;
      return true;
    } else if (identical(node.elseExpression, _oldNode)) {
      node.elseExpression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitConstructorDeclaration(ConstructorDeclaration node) {
    if (identical(node.returnType, _oldNode)) {
      node.returnType = _newNode as Identifier;
      return true;
    } else if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.parameters, _oldNode)) {
      node.parameters = _newNode as FormalParameterList;
      return true;
    } else if (identical(node.redirectedConstructor, _oldNode)) {
      node.redirectedConstructor = _newNode as ConstructorName;
      return true;
    } else if (identical(node.body, _oldNode)) {
      node.body = _newNode as FunctionBody;
      return true;
    } else if (_replaceInList(node.initializers)) {
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    if (identical(node.fieldName, _oldNode)) {
      node.fieldName = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitConstructorName(ConstructorName node) {
    if (identical(node.type, _oldNode)) {
      node.type = _newNode as TypeName;
      return true;
    } else if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitContinueStatement(ContinueStatement node) {
    if (identical(node.label, _oldNode)) {
      node.label = _newNode as SimpleIdentifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitDeclaredIdentifier(DeclaredIdentifier node) {
    if (identical(node.type, _oldNode)) {
      node.type = _newNode as TypeName;
      return true;
    } else if (identical(node.identifier, _oldNode)) {
      node.identifier = _newNode as SimpleIdentifier;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitDefaultFormalParameter(DefaultFormalParameter node) {
    if (identical(node.parameter, _oldNode)) {
      node.parameter = _newNode as NormalFormalParameter;
      return true;
    } else if (identical(node.defaultValue, _oldNode)) {
      node.defaultValue = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitDoStatement(DoStatement node) {
    if (identical(node.body, _oldNode)) {
      node.body = _newNode as Statement;
      return true;
    } else if (identical(node.condition, _oldNode)) {
      node.condition = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitDoubleLiteral(DoubleLiteral node) => visitNode(node);

  @override
  bool visitEmptyFunctionBody(EmptyFunctionBody node) => visitNode(node);

  @override
  bool visitEmptyStatement(EmptyStatement node) => visitNode(node);

  @override
  bool visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitEnumDeclaration(EnumDeclaration node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (_replaceInList(node.constants)) {
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitExportDirective(ExportDirective node) =>
      visitNamespaceDirective(node);

  @override
  bool visitExpressionFunctionBody(ExpressionFunctionBody node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitExpressionStatement(ExpressionStatement node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitExtendsClause(ExtendsClause node) {
    if (identical(node.superclass, _oldNode)) {
      node.superclass = _newNode as TypeName;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitFieldDeclaration(FieldDeclaration node) {
    if (identical(node.fields, _oldNode)) {
      node.fields = _newNode as VariableDeclarationList;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitFieldFormalParameter(FieldFormalParameter node) {
    if (identical(node.type, _oldNode)) {
      node.type = _newNode as TypeName;
      return true;
    } else if (identical(node.parameters, _oldNode)) {
      node.parameters = _newNode as FormalParameterList;
      return true;
    }
    return visitNormalFormalParameter(node);
  }

  @override
  bool visitForEachStatement(ForEachStatement node) {
    if (identical(node.loopVariable, _oldNode)) {
      node.loopVariable = _newNode as DeclaredIdentifier;
      return true;
    } else if (identical(node.identifier, _oldNode)) {
      node.identifier = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.iterable, _oldNode)) {
      node.iterable = _newNode as Expression;
      return true;
    } else if (identical(node.body, _oldNode)) {
      node.body = _newNode as Statement;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitFormalParameterList(FormalParameterList node) {
    if (_replaceInList(node.parameters)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitForStatement(ForStatement node) {
    if (identical(node.variables, _oldNode)) {
      node.variables = _newNode as VariableDeclarationList;
      return true;
    } else if (identical(node.initialization, _oldNode)) {
      node.initialization = _newNode as Expression;
      return true;
    } else if (identical(node.condition, _oldNode)) {
      node.condition = _newNode as Expression;
      return true;
    } else if (identical(node.body, _oldNode)) {
      node.body = _newNode as Statement;
      return true;
    } else if (_replaceInList(node.updaters)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitFunctionDeclaration(FunctionDeclaration node) {
    if (identical(node.returnType, _oldNode)) {
      node.returnType = _newNode as TypeName;
      return true;
    } else if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.functionExpression, _oldNode)) {
      node.functionExpression = _newNode as FunctionExpression;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    if (identical(node.functionDeclaration, _oldNode)) {
      node.functionDeclaration = _newNode as FunctionDeclaration;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitFunctionExpression(FunctionExpression node) {
    if (identical(node.parameters, _oldNode)) {
      node.parameters = _newNode as FormalParameterList;
      return true;
    } else if (identical(node.body, _oldNode)) {
      node.body = _newNode as FunctionBody;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    if (identical(node.function, _oldNode)) {
      node.function = _newNode as Expression;
      return true;
    } else if (identical(node.argumentList, _oldNode)) {
      node.argumentList = _newNode as ArgumentList;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitFunctionTypeAlias(FunctionTypeAlias node) {
    if (identical(node.returnType, _oldNode)) {
      node.returnType = _newNode as TypeName;
      return true;
    } else if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.typeParameters, _oldNode)) {
      node.typeParameters = _newNode as TypeParameterList;
      return true;
    } else if (identical(node.parameters, _oldNode)) {
      node.parameters = _newNode as FormalParameterList;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    if (identical(node.returnType, _oldNode)) {
      node.returnType = _newNode as TypeName;
      return true;
    } else if (identical(node.parameters, _oldNode)) {
      node.parameters = _newNode as FormalParameterList;
      return true;
    }
    return visitNormalFormalParameter(node);
  }

  @override
  bool visitHideCombinator(HideCombinator node) {
    if (_replaceInList(node.hiddenNames)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitIfStatement(IfStatement node) {
    if (identical(node.condition, _oldNode)) {
      node.condition = _newNode as Expression;
      return true;
    } else if (identical(node.thenStatement, _oldNode)) {
      node.thenStatement = _newNode as Statement;
      return true;
    } else if (identical(node.elseStatement, _oldNode)) {
      node.elseStatement = _newNode as Statement;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitImplementsClause(ImplementsClause node) {
    if (_replaceInList(node.interfaces)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitImportDirective(ImportDirective node) {
    if (identical(node.prefix, _oldNode)) {
      node.prefix = _newNode as SimpleIdentifier;
      return true;
    }
    return visitNamespaceDirective(node);
  }

  @override
  bool visitIndexExpression(IndexExpression node) {
    if (identical(node.target, _oldNode)) {
      node.target = _newNode as Expression;
      return true;
    } else if (identical(node.index, _oldNode)) {
      node.index = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitInstanceCreationExpression(InstanceCreationExpression node) {
    if (identical(node.constructorName, _oldNode)) {
      node.constructorName = _newNode as ConstructorName;
      return true;
    } else if (identical(node.argumentList, _oldNode)) {
      node.argumentList = _newNode as ArgumentList;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitIntegerLiteral(IntegerLiteral node) => visitNode(node);

  @override
  bool visitInterpolationExpression(InterpolationExpression node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitInterpolationString(InterpolationString node) => visitNode(node);

  @override
  bool visitIsExpression(IsExpression node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    } else if (identical(node.type, _oldNode)) {
      node.type = _newNode as TypeName;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitLabel(Label node) {
    if (identical(node.label, _oldNode)) {
      node.label = _newNode as SimpleIdentifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitLabeledStatement(LabeledStatement node) {
    if (identical(node.statement, _oldNode)) {
      node.statement = _newNode as Statement;
      return true;
    } else if (_replaceInList(node.labels)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitLibraryDirective(LibraryDirective node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as LibraryIdentifier;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitLibraryIdentifier(LibraryIdentifier node) {
    if (_replaceInList(node.components)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitListLiteral(ListLiteral node) {
    if (_replaceInList(node.elements)) {
      return true;
    }
    return visitTypedLiteral(node);
  }

  @override
  bool visitMapLiteral(MapLiteral node) {
    if (_replaceInList(node.entries)) {
      return true;
    }
    return visitTypedLiteral(node);
  }

  @override
  bool visitMapLiteralEntry(MapLiteralEntry node) {
    if (identical(node.key, _oldNode)) {
      node.key = _newNode as Expression;
      return true;
    } else if (identical(node.value, _oldNode)) {
      node.value = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitMethodDeclaration(MethodDeclaration node) {
    if (identical(node.returnType, _oldNode)) {
      node.returnType = _newNode as TypeName;
      return true;
    } else if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.parameters, _oldNode)) {
      node.parameters = _newNode as FormalParameterList;
      return true;
    } else if (identical(node.body, _oldNode)) {
      node.body = _newNode as FunctionBody;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitMethodInvocation(MethodInvocation node) {
    if (identical(node.target, _oldNode)) {
      node.target = _newNode as Expression;
      return true;
    } else if (identical(node.methodName, _oldNode)) {
      node.methodName = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.argumentList, _oldNode)) {
      node.argumentList = _newNode as ArgumentList;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitNamedExpression(NamedExpression node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as Label;
      return true;
    } else if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  bool visitNamespaceDirective(NamespaceDirective node) {
    if (_replaceInList(node.combinators)) {
      return true;
    }
    return visitUriBasedDirective(node);
  }

  @override
  bool visitNativeClause(NativeClause node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as StringLiteral;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitNativeFunctionBody(NativeFunctionBody node) {
    if (identical(node.stringLiteral, _oldNode)) {
      node.stringLiteral = _newNode as StringLiteral;
      return true;
    }
    return visitNode(node);
  }

  bool visitNode(AstNode node) {
    throw new IllegalArgumentException(
        "The old node is not a child of it's parent");
  }

  bool visitNormalFormalParameter(NormalFormalParameter node) {
    if (identical(node.documentationComment, _oldNode)) {
      node.documentationComment = _newNode as Comment;
      return true;
    } else if (identical(node.identifier, _oldNode)) {
      node.identifier = _newNode as SimpleIdentifier;
      return true;
    } else if (_replaceInList(node.metadata)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitNullLiteral(NullLiteral node) => visitNode(node);

  @override
  bool visitParenthesizedExpression(ParenthesizedExpression node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitPartDirective(PartDirective node) => visitUriBasedDirective(node);

  @override
  bool visitPartOfDirective(PartOfDirective node) {
    if (identical(node.libraryName, _oldNode)) {
      node.libraryName = _newNode as LibraryIdentifier;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitPostfixExpression(PostfixExpression node) {
    if (identical(node.operand, _oldNode)) {
      node.operand = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitPrefixedIdentifier(PrefixedIdentifier node) {
    if (identical(node.prefix, _oldNode)) {
      node.prefix = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.identifier, _oldNode)) {
      node.identifier = _newNode as SimpleIdentifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitPrefixExpression(PrefixExpression node) {
    if (identical(node.operand, _oldNode)) {
      node.operand = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitPropertyAccess(PropertyAccess node) {
    if (identical(node.target, _oldNode)) {
      node.target = _newNode as Expression;
      return true;
    } else if (identical(node.propertyName, _oldNode)) {
      node.propertyName = _newNode as SimpleIdentifier;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    if (identical(node.constructorName, _oldNode)) {
      node.constructorName = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.argumentList, _oldNode)) {
      node.argumentList = _newNode as ArgumentList;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitRethrowExpression(RethrowExpression node) => visitNode(node);

  @override
  bool visitReturnStatement(ReturnStatement node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitScriptTag(ScriptTag scriptTag) => visitNode(scriptTag);

  @override
  bool visitShowCombinator(ShowCombinator node) {
    if (_replaceInList(node.shownNames)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitSimpleFormalParameter(SimpleFormalParameter node) {
    if (identical(node.type, _oldNode)) {
      node.type = _newNode as TypeName;
      return true;
    }
    return visitNormalFormalParameter(node);
  }

  @override
  bool visitSimpleIdentifier(SimpleIdentifier node) => visitNode(node);

  @override
  bool visitSimpleStringLiteral(SimpleStringLiteral node) => visitNode(node);

  @override
  bool visitStringInterpolation(StringInterpolation node) {
    if (_replaceInList(node.elements)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    if (identical(node.constructorName, _oldNode)) {
      node.constructorName = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.argumentList, _oldNode)) {
      node.argumentList = _newNode as ArgumentList;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitSuperExpression(SuperExpression node) => visitNode(node);

  @override
  bool visitSwitchCase(SwitchCase node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitSwitchMember(node);
  }

  @override
  bool visitSwitchDefault(SwitchDefault node) => visitSwitchMember(node);

  bool visitSwitchMember(SwitchMember node) {
    if (_replaceInList(node.labels)) {
      return true;
    } else if (_replaceInList(node.statements)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitSwitchStatement(SwitchStatement node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    } else if (_replaceInList(node.members)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitSymbolLiteral(SymbolLiteral node) => visitNode(node);

  @override
  bool visitThisExpression(ThisExpression node) => visitNode(node);

  @override
  bool visitThrowExpression(ThrowExpression node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    if (identical(node.variables, _oldNode)) {
      node.variables = _newNode as VariableDeclarationList;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitTryStatement(TryStatement node) {
    if (identical(node.body, _oldNode)) {
      node.body = _newNode as Block;
      return true;
    } else if (identical(node.finallyBlock, _oldNode)) {
      node.finallyBlock = _newNode as Block;
      return true;
    } else if (_replaceInList(node.catchClauses)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitTypeArgumentList(TypeArgumentList node) {
    if (_replaceInList(node.arguments)) {
      return true;
    }
    return visitNode(node);
  }

  bool visitTypedLiteral(TypedLiteral node) {
    if (identical(node.typeArguments, _oldNode)) {
      node.typeArguments = _newNode as TypeArgumentList;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitTypeName(TypeName node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as Identifier;
      return true;
    } else if (identical(node.typeArguments, _oldNode)) {
      node.typeArguments = _newNode as TypeArgumentList;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitTypeParameter(TypeParameter node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.bound, _oldNode)) {
      node.bound = _newNode as TypeName;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitTypeParameterList(TypeParameterList node) {
    if (_replaceInList(node.typeParameters)) {
      return true;
    }
    return visitNode(node);
  }

  bool visitUriBasedDirective(UriBasedDirective node) {
    if (identical(node.uri, _oldNode)) {
      node.uri = _newNode as StringLiteral;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitVariableDeclaration(VariableDeclaration node) {
    if (identical(node.name, _oldNode)) {
      node.name = _newNode as SimpleIdentifier;
      return true;
    } else if (identical(node.initializer, _oldNode)) {
      node.initializer = _newNode as Expression;
      return true;
    }
    return visitAnnotatedNode(node);
  }

  @override
  bool visitVariableDeclarationList(VariableDeclarationList node) {
    if (identical(node.type, _oldNode)) {
      node.type = _newNode as TypeName;
      return true;
    } else if (_replaceInList(node.variables)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    if (identical(node.variables, _oldNode)) {
      node.variables = _newNode as VariableDeclarationList;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitWhileStatement(WhileStatement node) {
    if (identical(node.condition, _oldNode)) {
      node.condition = _newNode as Expression;
      return true;
    } else if (identical(node.body, _oldNode)) {
      node.body = _newNode as Statement;
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitWithClause(WithClause node) {
    if (_replaceInList(node.mixinTypes)) {
      return true;
    }
    return visitNode(node);
  }

  @override
  bool visitYieldStatement(YieldStatement node) {
    if (identical(node.expression, _oldNode)) {
      node.expression = _newNode as Expression;
    }
    return visitNode(node);
  }

  bool _replaceInList(NodeList list) {
    int count = list.length;
    for (int i = 0; i < count; i++) {
      if (identical(_oldNode, list[i])) {
        list[i] = _newNode;
        return true;
      }
    }
    return false;
  }

  /**
   * Replace the old node with the new node in the AST structure containing the old node.
   *
   * @param oldNode
   * @param newNode
   * @return `true` if the replacement was successful
   * @throws IllegalArgumentException if either node is `null`, if the old node does not have
   *           a parent node, or if the AST structure has been corrupted
   */
  static bool replace(AstNode oldNode, AstNode newNode) {
    if (oldNode == null || newNode == null) {
      throw new IllegalArgumentException(
          "The old and new nodes must be non-null");
    } else if (identical(oldNode, newNode)) {
      return true;
    }
    AstNode parent = oldNode.parent;
    if (parent == null) {
      throw new IllegalArgumentException(
          "The old node is not a child of another node");
    }
    NodeReplacer replacer = new NodeReplacer(oldNode, newNode);
    return parent.accept(replacer);
  }
}

/**
 * The abstract class `NormalFormalParameter` defines the behavior common to formal parameters
 * that are required (are not optional).
 *
 * <pre>
 * normalFormalParameter ::=
 *     [FunctionTypedFormalParameter]
 *   | [FieldFormalParameter]
 *   | [SimpleFormalParameter]
 * </pre>
 */
abstract class NormalFormalParameter extends FormalParameter {
  /**
   * The documentation comment associated with this parameter, or `null` if this parameter
   * does not have a documentation comment associated with it.
   */
  Comment _comment;

  /**
   * The annotations associated with this parameter.
   */
  NodeList<Annotation> _metadata;

  /**
   * The name of the parameter being declared.
   */
  SimpleIdentifier _identifier;

  /**
   * Initialize a newly created formal parameter.
   *
   * @param comment the documentation comment associated with this parameter
   * @param metadata the annotations associated with this parameter
   * @param identifier the name of the parameter being declared
   */
  NormalFormalParameter(Comment comment, List<Annotation> metadata,
      SimpleIdentifier identifier) {
    _comment = becomeParentOf(comment);
    _metadata = new NodeList<Annotation>(this, metadata);
    _identifier = becomeParentOf(identifier);
  }

  /**
   * Return the documentation comment associated with this parameter, or `null` if this
   * parameter does not have a documentation comment associated with it.
   *
   * @return the documentation comment associated with this parameter
   */
  Comment get documentationComment => _comment;

  /**
   * Set the documentation comment associated with this parameter to the given comment
   *
   * @param comment the documentation comment to be associated with this parameter
   */
  void set documentationComment(Comment comment) {
    _comment = becomeParentOf(comment);
  }

  @override
  SimpleIdentifier get identifier => _identifier;

  /**
   * Set the name of the parameter being declared to the given identifier.
   *
   * @param identifier the name of the parameter being declared
   */
  void set identifier(SimpleIdentifier identifier) {
    _identifier = becomeParentOf(identifier);
  }

  @override
  ParameterKind get kind {
    AstNode parent = this.parent;
    if (parent is DefaultFormalParameter) {
      return parent.kind;
    }
    return ParameterKind.REQUIRED;
  }

  /**
   * Return the annotations associated with this parameter.
   *
   * @return the annotations associated with this parameter
   */
  NodeList<Annotation> get metadata => _metadata;

  /**
   * Set the metadata associated with this node to the given metadata.
   *
   * @param metadata the metadata to be associated with this node
   */
  void set metadata(List<Annotation> metadata) {
    _metadata.clear();
    _metadata.addAll(metadata);
  }

  /**
   * Return an array containing the comment and annotations associated with this parameter, sorted
   * in lexical order.
   *
   * @return the comment and annotations associated with this parameter in the order in which they
   *         appeared in the original source
   */
  List<AstNode> get sortedCommentAndAnnotations {
    return <AstNode>[]
        ..add(_comment)
        ..addAll(_metadata)
        ..sort(AstNode.LEXICAL_ORDER);
  }

  ChildEntities get _childEntities {
    ChildEntities result = new ChildEntities();
    if (_commentIsBeforeAnnotations()) {
      result
          ..add(_comment)
          ..addAll(_metadata);
    } else {
      result.addAll(sortedCommentAndAnnotations);
    }
    return result;
  }

  @override
  void visitChildren(AstVisitor visitor) {
    //
    // Note that subclasses are responsible for visiting the identifier because
    // they often need to visit other nodes before visiting the identifier.
    //
    if (_commentIsBeforeAnnotations()) {
      safelyVisitChild(_comment, visitor);
      _metadata.accept(visitor);
    } else {
      for (AstNode child in sortedCommentAndAnnotations) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Return `true` if the comment is lexically before any annotations.
   *
   * @return `true` if the comment is lexically before any annotations
   */
  bool _commentIsBeforeAnnotations() {
    if (_comment == null || _metadata.isEmpty) {
      return true;
    }
    Annotation firstAnnotation = _metadata[0];
    return _comment.offset < firstAnnotation.offset;
  }
}

/**
 * Instances of the class `NullLiteral` represent a null literal expression.
 *
 * <pre>
 * nullLiteral ::=
 *     'null'
 * </pre>
 */
class NullLiteral extends Literal {
  /**
   * The token representing the literal.
   */
  Token literal;

  /**
   * Initialize a newly created null literal.
   *
   * @param token the token representing the literal
   */
  NullLiteral(this.literal);

  @override
  Token get beginToken => literal;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(literal);

  @override
  Token get endToken => literal;

  @override
  accept(AstVisitor visitor) => visitor.visitNullLiteral(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `ParenthesizedExpression` represent a parenthesized expression.
 *
 * <pre>
 * parenthesizedExpression ::=
 *     '(' [Expression] ')'
 * </pre>
 */
class ParenthesizedExpression extends Expression {
  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The expression within the parentheses.
   */
  Expression _expression;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * Initialize a newly created parenthesized expression.
   *
   * @param leftParenthesis the left parenthesis
   * @param expression the expression within the parentheses
   * @param rightParenthesis the right parenthesis
   */
  ParenthesizedExpression(this.leftParenthesis, Expression expression,
      this.rightParenthesis) {
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken => leftParenthesis;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(leftParenthesis)
      ..add(_expression)
      ..add(rightParenthesis);

  @override
  Token get endToken => rightParenthesis;

  /**
   * Return the expression within the parentheses.
   *
   * @return the expression within the parentheses
   */
  Expression get expression => _expression;

  /**
   * Set the expression within the parentheses to the given expression.
   *
   * @param expression the expression within the parentheses
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  int get precedence => 15;

  @override
  accept(AstVisitor visitor) => visitor.visitParenthesizedExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * Instances of the class `PartDirective` represent a part directive.
 *
 * <pre>
 * partDirective ::=
 *     [Annotation] 'part' [StringLiteral] ';'
 * </pre>
 */
class PartDirective extends UriBasedDirective {
  /**
   * The token representing the 'part' token.
   */
  Token partToken;

  /**
   * The semicolon terminating the directive.
   */
  Token semicolon;

  /**
   * Initialize a newly created part directive.
   *
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param partToken the token representing the 'part' token
   * @param partUri the URI of the part being included
   * @param semicolon the semicolon terminating the directive
   */
  PartDirective(Comment comment, List<Annotation> metadata, this.partToken,
      StringLiteral partUri, this.semicolon)
      : super(comment, metadata, partUri);

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => super._childEntities
      ..add(partToken)
      ..add(_uri)
      ..add(semicolon);

  @override
  Token get endToken => semicolon;

  @override
  Token get firstTokenAfterCommentAndMetadata => partToken;

  @override
  Token get keyword => partToken;

  @override
  CompilationUnitElement get uriElement => element as CompilationUnitElement;

  @override
  accept(AstVisitor visitor) => visitor.visitPartDirective(this);
}

/**
 * Instances of the class `PartOfDirective` represent a part-of directive.
 *
 * <pre>
 * partOfDirective ::=
 *     [Annotation] 'part' 'of' [Identifier] ';'
 * </pre>
 */
class PartOfDirective extends Directive {
  /**
   * The token representing the 'part' token.
   */
  Token partToken;

  /**
   * The token representing the 'of' token.
   */
  Token ofToken;

  /**
   * The name of the library that the containing compilation unit is part of.
   */
  LibraryIdentifier _libraryName;

  /**
   * The semicolon terminating the directive.
   */
  Token semicolon;

  /**
   * Initialize a newly created part-of directive.
   *
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param partToken the token representing the 'part' token
   * @param ofToken the token representing the 'of' token
   * @param libraryName the name of the library that the containing compilation unit is part of
   * @param semicolon the semicolon terminating the directive
   */
  PartOfDirective(Comment comment, List<Annotation> metadata, this.partToken,
      this.ofToken, LibraryIdentifier libraryName, this.semicolon)
      : super(comment, metadata) {
    _libraryName = becomeParentOf(libraryName);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(partToken)
      ..add(ofToken)
      ..add(_libraryName)
      ..add(semicolon);

  @override
  Token get endToken => semicolon;

  @override
  Token get firstTokenAfterCommentAndMetadata => partToken;

  @override
  Token get keyword => partToken;

  /**
   * Return the name of the library that the containing compilation unit is part of.
   *
   * @return the name of the library that the containing compilation unit is part of
   */
  LibraryIdentifier get libraryName => _libraryName;

  /**
   * Set the name of the library that the containing compilation unit is part of to the given name.
   *
   * @param libraryName the name of the library that the containing compilation unit is part of
   */
  void set libraryName(LibraryIdentifier libraryName) {
    _libraryName = becomeParentOf(libraryName);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitPartOfDirective(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_libraryName, visitor);
  }
}

/**
 * Instances of the class `PostfixExpression` represent a postfix unary expression.
 *
 * <pre>
 * postfixExpression ::=
 *     [Expression] [Token]
 * </pre>
 */
class PostfixExpression extends Expression {
  /**
   * The expression computing the operand for the operator.
   */
  Expression _operand;

  /**
   * The postfix operator being applied to the operand.
   */
  Token operator;

  /**
   * The element associated with this the operator based on the propagated type of the operand, or
   * `null` if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved.
   */
  MethodElement propagatedElement;

  /**
   * The element associated with the operator based on the static type of the operand, or
   * `null` if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved.
   */
  MethodElement staticElement;

  /**
   * Initialize a newly created postfix expression.
   *
   * @param operand the expression computing the operand for the operator
   * @param operator the postfix operator being applied to the operand
   */
  PostfixExpression(Expression operand, this.operator) {
    _operand = becomeParentOf(operand);
  }

  @override
  Token get beginToken => _operand.beginToken;

  /**
   * Return the best element available for this operator. If resolution was able to find a better
   * element based on type propagation, that element will be returned. Otherwise, the element found
   * using the result of static analysis will be returned. If resolution has not been performed,
   * then `null` will be returned.
   *
   * @return the best element available for this operator
   */
  MethodElement get bestElement {
    MethodElement element = propagatedElement;
    if (element == null) {
      element = staticElement;
    }
    return element;
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_operand)
      ..add(operator);

  @override
  Token get endToken => operator;

  /**
   * Return the expression computing the operand for the operator.
   *
   * @return the expression computing the operand for the operator
   */
  Expression get operand => _operand;

  /**
   * Set the expression computing the operand for the operator to the given expression.
   *
   * @param expression the expression computing the operand for the operator
   */
  void set operand(Expression expression) {
    _operand = becomeParentOf(expression);
  }

  @override
  int get precedence => 15;

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on
   * propagated type information, then return the parameter element representing the parameter to
   * which the value of the operand will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.propagatedParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  ParameterElement get propagatedParameterElementForOperand {
    if (propagatedElement == null) {
      return null;
    }
    List<ParameterElement> parameters = propagatedElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on static
   * type information, then return the parameter element representing the parameter to which the
   * value of the operand will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.staticParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  ParameterElement get staticParameterElementForOperand {
    if (staticElement == null) {
      return null;
    }
    List<ParameterElement> parameters = staticElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  @override
  accept(AstVisitor visitor) => visitor.visitPostfixExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_operand, visitor);
  }
}

/**
 * Instances of the class `PrefixedIdentifier` represent either an identifier that is prefixed
 * or an access to an object property where the target of the property access is a simple
 * identifier.
 *
 * <pre>
 * prefixedIdentifier ::=
 *     [SimpleIdentifier] '.' [SimpleIdentifier]
 * </pre>
 */
class PrefixedIdentifier extends Identifier {
  /**
   * The prefix associated with the library in which the identifier is defined.
   */
  SimpleIdentifier _prefix;

  /**
   * The period used to separate the prefix from the identifier.
   */
  Token period;

  /**
   * The identifier being prefixed.
   */
  SimpleIdentifier _identifier;

  /**
   * Initialize a newly created prefixed identifier.
   *
   * @param prefix the identifier being prefixed
   * @param period the period used to separate the prefix from the identifier
   * @param identifier the prefix associated with the library in which the identifier is defined
   */
  PrefixedIdentifier(SimpleIdentifier prefix, this.period,
      SimpleIdentifier identifier) {
    _prefix = becomeParentOf(prefix);
    _identifier = becomeParentOf(identifier);
  }

  @override
  Token get beginToken => _prefix.beginToken;

  @override
  Element get bestElement {
    if (_identifier == null) {
      return null;
    }
    return _identifier.bestElement;
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_prefix)
      ..add(period)
      ..add(_identifier);

  @override
  Token get endToken => _identifier.endToken;

  /**
   * Return the identifier being prefixed.
   *
   * @return the identifier being prefixed
   */
  SimpleIdentifier get identifier => _identifier;

  /**
   * Set the identifier being prefixed to the given identifier.
   *
   * @param identifier the identifier being prefixed
   */
  void set identifier(SimpleIdentifier identifier) {
    _identifier = becomeParentOf(identifier);
  }

  /**
   * Return `true` if this type is a deferred type.
   *
   * 15.1 Static Types: A type <i>T</i> is deferred iff it is of the form </i>p.T</i> where <i>p</i>
   * is a deferred prefix.
   *
   * @return `true` if this type is a deferred type
   */
  bool get isDeferred {
    Element element = _prefix.staticElement;
    if (element is! PrefixElement) {
      return false;
    }
    PrefixElement prefixElement = element as PrefixElement;
    List<ImportElement> imports =
        prefixElement.enclosingElement.getImportsWithPrefix(prefixElement);
    if (imports.length != 1) {
      return false;
    }
    return imports[0].isDeferred;
  }

  @override
  String get name => "${_prefix.name}.${_identifier.name}";

  @override
  int get precedence => 15;

  /**
   * Return the prefix associated with the library in which the identifier is defined.
   *
   * @return the prefix associated with the library in which the identifier is defined
   */
  SimpleIdentifier get prefix => _prefix;

  /**
   * Set the prefix associated with the library in which the identifier is defined to the given
   * identifier.
   *
   * @param identifier the prefix associated with the library in which the identifier is defined
   */
  void set prefix(SimpleIdentifier identifier) {
    _prefix = becomeParentOf(identifier);
  }

  @override
  Element get propagatedElement {
    if (_identifier == null) {
      return null;
    }
    return _identifier.propagatedElement;
  }

  @override
  Element get staticElement {
    if (_identifier == null) {
      return null;
    }
    return _identifier.staticElement;
  }

  @override
  accept(AstVisitor visitor) => visitor.visitPrefixedIdentifier(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_prefix, visitor);
    safelyVisitChild(_identifier, visitor);
  }
}

/**
 * Instances of the class `PrefixExpression` represent a prefix unary expression.
 *
 * <pre>
 * prefixExpression ::=
 *     [Token] [Expression]
 * </pre>
 */
class PrefixExpression extends Expression {
  /**
   * The prefix operator being applied to the operand.
   */
  Token operator;

  /**
   * The expression computing the operand for the operator.
   */
  Expression _operand;

  /**
   * The element associated with the operator based on the static type of the operand, or
   * `null` if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved.
   */
  MethodElement staticElement;

  /**
   * The element associated with the operator based on the propagated type of the operand, or
   * `null` if the AST structure has not been resolved, if the operator is not user definable,
   * or if the operator could not be resolved.
   */
  MethodElement propagatedElement;

  /**
   * Initialize a newly created prefix expression.
   *
   * @param operator the prefix operator being applied to the operand
   * @param operand the expression computing the operand for the operator
   */
  PrefixExpression(this.operator, Expression operand) {
    _operand = becomeParentOf(operand);
  }

  @override
  Token get beginToken => operator;

  /**
   * Return the best element available for this operator. If resolution was able to find a better
   * element based on type propagation, that element will be returned. Otherwise, the element found
   * using the result of static analysis will be returned. If resolution has not been performed,
   * then `null` will be returned.
   *
   * @return the best element available for this operator
   */
  MethodElement get bestElement {
    MethodElement element = propagatedElement;
    if (element == null) {
      element = staticElement;
    }
    return element;
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(operator)
      ..add(_operand);

  @override
  Token get endToken => _operand.endToken;

  /**
   * Return the expression computing the operand for the operator.
   *
   * @return the expression computing the operand for the operator
   */
  Expression get operand => _operand;

  /**
   * Set the expression computing the operand for the operator to the given expression.
   *
   * @param expression the expression computing the operand for the operator
   */
  void set operand(Expression expression) {
    _operand = becomeParentOf(expression);
  }

  @override
  int get precedence => 14;

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on
   * propagated type information, then return the parameter element representing the parameter to
   * which the value of the operand will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.propagatedParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  ParameterElement get propagatedParameterElementForOperand {
    if (propagatedElement == null) {
      return null;
    }
    List<ParameterElement> parameters = propagatedElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  /**
   * If the AST structure has been resolved, and the function being invoked is known based on static
   * type information, then return the parameter element representing the parameter to which the
   * value of the operand will be bound. Otherwise, return `null`.
   *
   * This method is only intended to be used by [Expression.staticParameterElement].
   *
   * @return the parameter element representing the parameter to which the value of the right
   *         operand will be bound
   */
  ParameterElement get staticParameterElementForOperand {
    if (staticElement == null) {
      return null;
    }
    List<ParameterElement> parameters = staticElement.parameters;
    if (parameters.length < 1) {
      return null;
    }
    return parameters[0];
  }

  @override
  accept(AstVisitor visitor) => visitor.visitPrefixExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_operand, visitor);
  }
}

/**
 * Instances of the class `PropertyAccess` represent the access of a property of an object.
 *
 * Note, however, that accesses to properties of objects can also be represented as
 * [PrefixedIdentifier] nodes in cases where the target is also a simple
 * identifier.
 *
 * <pre>
 * propertyAccess ::=
 *     [Expression] '.' [SimpleIdentifier]
 * </pre>
 */
class PropertyAccess extends Expression {
  /**
   * The expression computing the object defining the property being accessed.
   */
  Expression _target;

  /**
   * The property access operator.
   */
  Token operator;

  /**
   * The name of the property being accessed.
   */
  SimpleIdentifier _propertyName;

  /**
   * Initialize a newly created property access expression.
   *
   * @param target the expression computing the object defining the property being accessed
   * @param operator the property access operator
   * @param propertyName the name of the property being accessed
   */
  PropertyAccess(Expression target, this.operator,
      SimpleIdentifier propertyName) {
    _target = becomeParentOf(target);
    _propertyName = becomeParentOf(propertyName);
  }

  @override
  Token get beginToken {
    if (_target != null) {
      return _target.beginToken;
    }
    return operator;
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_target)
      ..add(operator)
      ..add(_propertyName);

  @override
  Token get endToken => _propertyName.endToken;

  @override
  bool get isAssignable => true;

  /**
   * Return `true` if this expression is cascaded. If it is, then the target of this
   * expression is not stored locally but is stored in the nearest ancestor that is a
   * [CascadeExpression].
   *
   * @return `true` if this expression is cascaded
   */
  bool get isCascaded =>
      operator != null && operator.type == TokenType.PERIOD_PERIOD;

  @override
  int get precedence => 15;

  /**
   * Return the name of the property being accessed.
   *
   * @return the name of the property being accessed
   */
  SimpleIdentifier get propertyName => _propertyName;

  /**
   * Set the name of the property being accessed to the given identifier.
   *
   * @param identifier the name of the property being accessed
   */
  void set propertyName(SimpleIdentifier identifier) {
    _propertyName = becomeParentOf(identifier);
  }

  /**
   * Return the expression used to compute the receiver of the invocation. If this invocation is not
   * part of a cascade expression, then this is the same as [getTarget]. If this invocation
   * is part of a cascade expression, then the target stored with the cascade expression is
   * returned.
   *
   * @return the expression used to compute the receiver of the invocation
   * See [target].
   */
  Expression get realTarget {
    if (isCascaded) {
      AstNode ancestor = parent;
      while (ancestor is! CascadeExpression) {
        if (ancestor == null) {
          return _target;
        }
        ancestor = ancestor.parent;
      }
      return (ancestor as CascadeExpression).target;
    }
    return _target;
  }

  /**
   * Return the expression computing the object defining the property being accessed, or
   * `null` if this property access is part of a cascade expression.
   *
   * @return the expression computing the object defining the property being accessed
   * See [realTarget].
   */
  Expression get target => _target;

  /**
   * Set the expression computing the object defining the property being accessed to the given
   * expression.
   *
   * @param expression the expression computing the object defining the property being accessed
   */
  void set target(Expression expression) {
    _target = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitPropertyAccess(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_target, visitor);
    safelyVisitChild(_propertyName, visitor);
  }
}

/**
 * Instances of the class `RecursiveAstVisitor` implement an AST visitor that will recursively
 * visit all of the nodes in an AST structure. For example, using an instance of this class to visit
 * a [Block] will also cause all of the statements in the block to be visited.
 *
 * Subclasses that override a visit method must either invoke the overridden visit method or must
 * explicitly ask the visited node to visit its children. Failure to do so will cause the children
 * of the visited node to not be visited.
 */
class RecursiveAstVisitor<R> implements AstVisitor<R> {
  @override
  R visitAdjacentStrings(AdjacentStrings node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitAnnotation(Annotation node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitArgumentList(ArgumentList node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitAsExpression(AsExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitAssertStatement(AssertStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitAssignmentExpression(AssignmentExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitAwaitExpression(AwaitExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitBinaryExpression(BinaryExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitBlock(Block node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitBlockFunctionBody(BlockFunctionBody node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitBooleanLiteral(BooleanLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitBreakStatement(BreakStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitCascadeExpression(CascadeExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitCatchClause(CatchClause node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitClassDeclaration(ClassDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitClassTypeAlias(ClassTypeAlias node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitComment(Comment node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitCommentReference(CommentReference node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitCompilationUnit(CompilationUnit node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitConditionalExpression(ConditionalExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitConstructorDeclaration(ConstructorDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitConstructorName(ConstructorName node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitContinueStatement(ContinueStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitDeclaredIdentifier(DeclaredIdentifier node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitDefaultFormalParameter(DefaultFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitDoStatement(DoStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitDoubleLiteral(DoubleLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitEmptyFunctionBody(EmptyFunctionBody node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitEmptyStatement(EmptyStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitEnumDeclaration(EnumDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitExportDirective(ExportDirective node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitExpressionFunctionBody(ExpressionFunctionBody node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitExpressionStatement(ExpressionStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitExtendsClause(ExtendsClause node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFieldDeclaration(FieldDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFieldFormalParameter(FieldFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitForEachStatement(ForEachStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFormalParameterList(FormalParameterList node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitForStatement(ForStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFunctionDeclaration(FunctionDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFunctionExpression(FunctionExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFunctionTypeAlias(FunctionTypeAlias node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitHideCombinator(HideCombinator node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitIfStatement(IfStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitImplementsClause(ImplementsClause node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitImportDirective(ImportDirective node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitIndexExpression(IndexExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitInstanceCreationExpression(InstanceCreationExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitIntegerLiteral(IntegerLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitInterpolationExpression(InterpolationExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitInterpolationString(InterpolationString node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitIsExpression(IsExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitLabel(Label node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitLabeledStatement(LabeledStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitLibraryDirective(LibraryDirective node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitLibraryIdentifier(LibraryIdentifier node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitListLiteral(ListLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitMapLiteral(MapLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitMapLiteralEntry(MapLiteralEntry node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitMethodDeclaration(MethodDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitMethodInvocation(MethodInvocation node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitNamedExpression(NamedExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitNativeClause(NativeClause node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitNativeFunctionBody(NativeFunctionBody node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitNullLiteral(NullLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitParenthesizedExpression(ParenthesizedExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitPartDirective(PartDirective node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitPartOfDirective(PartOfDirective node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitPostfixExpression(PostfixExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitPrefixedIdentifier(PrefixedIdentifier node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitPrefixExpression(PrefixExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitPropertyAccess(PropertyAccess node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitRethrowExpression(RethrowExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitReturnStatement(ReturnStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitScriptTag(ScriptTag node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitShowCombinator(ShowCombinator node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSimpleFormalParameter(SimpleFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSimpleIdentifier(SimpleIdentifier node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSimpleStringLiteral(SimpleStringLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitStringInterpolation(StringInterpolation node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSuperExpression(SuperExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSwitchCase(SwitchCase node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSwitchDefault(SwitchDefault node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSwitchStatement(SwitchStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitSymbolLiteral(SymbolLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitThisExpression(ThisExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitThrowExpression(ThrowExpression node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitTryStatement(TryStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitTypeArgumentList(TypeArgumentList node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitTypeName(TypeName node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitTypeParameter(TypeParameter node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitTypeParameterList(TypeParameterList node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitVariableDeclaration(VariableDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitVariableDeclarationList(VariableDeclarationList node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitWhileStatement(WhileStatement node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitWithClause(WithClause node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitYieldStatement(YieldStatement node) {
    node.visitChildren(this);
    return null;
  }
}

/**
 * Instances of the class `RedirectingConstructorInvocation` represent the invocation of a
 * another constructor in the same class from within a constructor's initialization list.
 *
 * <pre>
 * redirectingConstructorInvocation ::=
 *     'this' ('.' identifier)? arguments
 * </pre>
 */
class RedirectingConstructorInvocation extends ConstructorInitializer {
  /**
   * The token for the 'this' keyword.
   */
  Token keyword;

  /**
   * The token for the period before the name of the constructor that is being invoked, or
   * `null` if the unnamed constructor is being invoked.
   */
  Token period;

  /**
   * The name of the constructor that is being invoked, or `null` if the unnamed constructor
   * is being invoked.
   */
  SimpleIdentifier _constructorName;

  /**
   * The list of arguments to the constructor.
   */
  ArgumentList _argumentList;

  /**
   * The element associated with the constructor based on static type information, or `null`
   * if the AST structure has not been resolved or if the constructor could not be resolved.
   */
  ConstructorElement staticElement;

  /**
   * Initialize a newly created redirecting invocation to invoke the constructor with the given name
   * with the given arguments.
   *
   * @param keyword the token for the 'this' keyword
   * @param period the token for the period before the name of the constructor that is being invoked
   * @param constructorName the name of the constructor that is being invoked
   * @param argumentList the list of arguments to the constructor
   */
  RedirectingConstructorInvocation(this.keyword, this.period,
      SimpleIdentifier constructorName, ArgumentList argumentList) {
    _constructorName = becomeParentOf(constructorName);
    _argumentList = becomeParentOf(argumentList);
  }

  /**
   * Return the list of arguments to the constructor.
   *
   * @return the list of arguments to the constructor
   */
  ArgumentList get argumentList => _argumentList;

  /**
   * Set the list of arguments to the constructor to the given list.
   *
   * @param argumentList the list of arguments to the constructor
   */
  void set argumentList(ArgumentList argumentList) {
    _argumentList = becomeParentOf(argumentList);
  }

  @override
  Token get beginToken => keyword;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(period)
      ..add(_constructorName)
      ..add(_argumentList);

  /**
   * Return the name of the constructor that is being invoked, or `null` if the unnamed
   * constructor is being invoked.
   *
   * @return the name of the constructor that is being invoked
   */
  SimpleIdentifier get constructorName => _constructorName;

  /**
   * Set the name of the constructor that is being invoked to the given identifier.
   *
   * @param identifier the name of the constructor that is being invoked
   */
  void set constructorName(SimpleIdentifier identifier) {
    _constructorName = becomeParentOf(identifier);
  }

  @override
  Token get endToken => _argumentList.endToken;

  @override
  accept(AstVisitor visitor) =>
      visitor.visitRedirectingConstructorInvocation(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_constructorName, visitor);
    safelyVisitChild(_argumentList, visitor);
  }
}

/**
 * Instances of the class `RethrowExpression` represent a rethrow expression.
 *
 * <pre>
 * rethrowExpression ::=
 *     'rethrow'
 * </pre>
 */
class RethrowExpression extends Expression {
  /**
   * The token representing the 'rethrow' keyword.
   */
  Token keyword;

  /**
   * Initialize a newly created rethrow expression.
   *
   * @param keyword the token representing the 'rethrow' keyword
   */
  RethrowExpression(this.keyword);

  @override
  Token get beginToken => keyword;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(keyword);

  @override
  Token get endToken => keyword;

  @override
  int get precedence => 0;

  @override
  accept(AstVisitor visitor) => visitor.visitRethrowExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `ReturnStatement` represent a return statement.
 *
 * <pre>
 * returnStatement ::=
 *     'return' [Expression]? ';'
 * </pre>
 */
class ReturnStatement extends Statement {
  /**
   * The token representing the 'return' keyword.
   */
  Token keyword;

  /**
   * The expression computing the value to be returned, or `null` if no explicit value was
   * provided.
   */
  Expression _expression;

  /**
   * The semicolon terminating the statement.
   */
  Token semicolon;

  /**
   * Initialize a newly created return statement.
   *
   * @param keyword the token representing the 'return' keyword
   * @param expression the expression computing the value to be returned
   * @param semicolon the semicolon terminating the statement
   */
  ReturnStatement(this.keyword, Expression expression, this.semicolon) {
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken => keyword;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(_expression)
      ..add(semicolon);

  @override
  Token get endToken => semicolon;

  /**
   * Return the expression computing the value to be returned, or `null` if no explicit value
   * was provided.
   *
   * @return the expression computing the value to be returned
   */
  Expression get expression => _expression;

  /**
   * Set the expression computing the value to be returned to the given expression.
   *
   * @param expression the expression computing the value to be returned
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitReturnStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * Traverse the AST from initial child node to successive parents, building a collection of local
 * variable and parameter names visible to the initial child node. In case of name shadowing, the
 * first name seen is the most specific one so names are not redefined.
 *
 * Completion test code coverage is 95%. The two basic blocks that are not executed cannot be
 * executed. They are included for future reference.
 */
class ScopedNameFinder extends GeneralizingAstVisitor<Object> {
  Declaration _declarationNode;

  AstNode _immediateChild;

  Map<String, SimpleIdentifier> _locals =
      new HashMap<String, SimpleIdentifier>();

  final int _position;

  bool _referenceIsWithinLocalFunction = false;

  ScopedNameFinder(this._position);

  Declaration get declaration => _declarationNode;

  Map<String, SimpleIdentifier> get locals => _locals;

  @override
  Object visitBlock(Block node) {
    _checkStatements(node.statements);
    return super.visitBlock(node);
  }

  @override
  Object visitCatchClause(CatchClause node) {
    _addToScope(node.exceptionParameter);
    _addToScope(node.stackTraceParameter);
    return super.visitCatchClause(node);
  }

  @override
  Object visitConstructorDeclaration(ConstructorDeclaration node) {
    if (!identical(_immediateChild, node.parameters)) {
      _addParameters(node.parameters.parameters);
    }
    _declarationNode = node;
    return null;
  }

  @override
  Object visitFieldDeclaration(FieldDeclaration node) {
    _declarationNode = node;
    return null;
  }

  @override
  Object visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.loopVariable;
    if (loopVariable != null) {
      _addToScope(loopVariable.identifier);
    }
    return super.visitForEachStatement(node);
  }

  @override
  Object visitForStatement(ForStatement node) {
    if (!identical(_immediateChild, node.variables) && node.variables != null) {
      _addVariables(node.variables.variables);
    }
    return super.visitForStatement(node);
  }

  @override
  Object visitFunctionDeclaration(FunctionDeclaration node) {
    if (node.parent is! FunctionDeclarationStatement) {
      _declarationNode = node;
      return null;
    }
    return super.visitFunctionDeclaration(node);
  }

  @override
  Object visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    _referenceIsWithinLocalFunction = true;
    return super.visitFunctionDeclarationStatement(node);
  }

  @override
  Object visitFunctionExpression(FunctionExpression node) {
    if (node.parameters != null &&
        !identical(_immediateChild, node.parameters)) {
      _addParameters(node.parameters.parameters);
    }
    return super.visitFunctionExpression(node);
  }

  @override
  Object visitMethodDeclaration(MethodDeclaration node) {
    _declarationNode = node;
    if (node.parameters == null) {
      return null;
    }
    if (!identical(_immediateChild, node.parameters)) {
      _addParameters(node.parameters.parameters);
    }
    return null;
  }

  @override
  Object visitNode(AstNode node) {
    _immediateChild = node;
    AstNode parent = node.parent;
    if (parent != null) {
      parent.accept(this);
    }
    return null;
  }

  @override
  Object visitSwitchMember(SwitchMember node) {
    _checkStatements(node.statements);
    return super.visitSwitchMember(node);
  }

  @override
  Object visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    _declarationNode = node;
    return null;
  }

  @override
  Object visitTypeAlias(TypeAlias node) {
    _declarationNode = node;
    return null;
  }

  void _addParameters(NodeList<FormalParameter> vars) {
    for (FormalParameter var2 in vars) {
      _addToScope(var2.identifier);
    }
  }

  void _addToScope(SimpleIdentifier identifier) {
    if (identifier != null && _isInRange(identifier)) {
      String name = identifier.name;
      if (!_locals.containsKey(name)) {
        _locals[name] = identifier;
      }
    }
  }

  void _addVariables(NodeList<VariableDeclaration> vars) {
    for (VariableDeclaration var2 in vars) {
      _addToScope(var2.name);
    }
  }

  /**
   * Some statements define names that are visible downstream. There aren't many of these.
   *
   * @param statements the list of statements to check for name definitions
   */
  void _checkStatements(List<Statement> statements) {
    for (Statement stmt in statements) {
      if (identical(stmt, _immediateChild)) {
        return;
      }
      if (stmt is VariableDeclarationStatement) {
        _addVariables(stmt.variables.variables);
      } else if (stmt is FunctionDeclarationStatement &&
          !_referenceIsWithinLocalFunction) {
        _addToScope(stmt.functionDeclaration.name);
      }
    }
  }

  bool _isInRange(AstNode node) {
    if (_position < 0) {
      // if source position is not set then all nodes are in range
      return true;
      // not reached
    }
    return node.end < _position;
  }
}

/**
 * Instances of the class `ScriptTag` represent the script tag that can optionally occur at
 * the beginning of a compilation unit.
 *
 * <pre>
 * scriptTag ::=
 *     '#!' (~NEWLINE)* NEWLINE
 * </pre>
 */
class ScriptTag extends AstNode {
  /**
   * The token representing this script tag.
   */
  Token scriptTag;

  /**
   * Initialize a newly created script tag.
   *
   * @param scriptTag the token representing this script tag
   */
  ScriptTag(this.scriptTag);

  @override
  Token get beginToken => scriptTag;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(scriptTag);

  @override
  Token get endToken => scriptTag;

  @override
  accept(AstVisitor visitor) => visitor.visitScriptTag(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `ShowCombinator` represent a combinator that restricts the names
 * being imported to those in a given list.
 *
 * <pre>
 * showCombinator ::=
 *     'show' [SimpleIdentifier] (',' [SimpleIdentifier])*
 * </pre>
 */
class ShowCombinator extends Combinator {
  /**
   * The list of names from the library that are made visible by this combinator.
   */
  NodeList<SimpleIdentifier> _shownNames;

  /**
   * Initialize a newly created import show combinator.
   *
   * @param keyword the comma introducing the combinator
   * @param shownNames the list of names from the library that are made visible by this combinator
   */
  ShowCombinator(Token keyword, List<SimpleIdentifier> shownNames)
      : super(keyword) {
    _shownNames = new NodeList<SimpleIdentifier>(this, shownNames);
  }

  /**
   * TODO(paulberry): add commas.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..addAll(_shownNames);

  @override
  Token get endToken => _shownNames.endToken;

  /**
   * Return the list of names from the library that are made visible by this combinator.
   *
   * @return the list of names from the library that are made visible by this combinator
   */
  NodeList<SimpleIdentifier> get shownNames => _shownNames;

  @override
  accept(AstVisitor visitor) => visitor.visitShowCombinator(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _shownNames.accept(visitor);
  }
}

/**
 * Instances of the class `SimpleAstVisitor` implement an AST visitor that will do nothing
 * when visiting an AST node. It is intended to be a superclass for classes that use the visitor
 * pattern primarily as a dispatch mechanism (and hence don't need to recursively visit a whole
 * structure) and that only need to visit a small number of node types.
 */
class SimpleAstVisitor<R> implements AstVisitor<R> {
  @override
  R visitAdjacentStrings(AdjacentStrings node) => null;

  @override
  R visitAnnotation(Annotation node) => null;

  @override
  R visitArgumentList(ArgumentList node) => null;

  @override
  R visitAsExpression(AsExpression node) => null;

  @override
  R visitAssertStatement(AssertStatement node) => null;

  @override
  R visitAssignmentExpression(AssignmentExpression node) => null;

  @override
  R visitAwaitExpression(AwaitExpression node) => null;

  @override
  R visitBinaryExpression(BinaryExpression node) => null;

  @override
  R visitBlock(Block node) => null;

  @override
  R visitBlockFunctionBody(BlockFunctionBody node) => null;

  @override
  R visitBooleanLiteral(BooleanLiteral node) => null;

  @override
  R visitBreakStatement(BreakStatement node) => null;

  @override
  R visitCascadeExpression(CascadeExpression node) => null;

  @override
  R visitCatchClause(CatchClause node) => null;

  @override
  R visitClassDeclaration(ClassDeclaration node) => null;

  @override
  R visitClassTypeAlias(ClassTypeAlias node) => null;

  @override
  R visitComment(Comment node) => null;

  @override
  R visitCommentReference(CommentReference node) => null;

  @override
  R visitCompilationUnit(CompilationUnit node) => null;

  @override
  R visitConditionalExpression(ConditionalExpression node) => null;

  @override
  R visitConstructorDeclaration(ConstructorDeclaration node) => null;

  @override
  R visitConstructorFieldInitializer(ConstructorFieldInitializer node) => null;

  @override
  R visitConstructorName(ConstructorName node) => null;

  @override
  R visitContinueStatement(ContinueStatement node) => null;

  @override
  R visitDeclaredIdentifier(DeclaredIdentifier node) => null;

  @override
  R visitDefaultFormalParameter(DefaultFormalParameter node) => null;

  @override
  R visitDoStatement(DoStatement node) => null;

  @override
  R visitDoubleLiteral(DoubleLiteral node) => null;

  @override
  R visitEmptyFunctionBody(EmptyFunctionBody node) => null;

  @override
  R visitEmptyStatement(EmptyStatement node) => null;

  @override
  R visitEnumConstantDeclaration(EnumConstantDeclaration node) => null;

  @override
  R visitEnumDeclaration(EnumDeclaration node) => null;

  @override
  R visitExportDirective(ExportDirective node) => null;

  @override
  R visitExpressionFunctionBody(ExpressionFunctionBody node) => null;

  @override
  R visitExpressionStatement(ExpressionStatement node) => null;

  @override
  R visitExtendsClause(ExtendsClause node) => null;

  @override
  R visitFieldDeclaration(FieldDeclaration node) => null;

  @override
  R visitFieldFormalParameter(FieldFormalParameter node) => null;

  @override
  R visitForEachStatement(ForEachStatement node) => null;

  @override
  R visitFormalParameterList(FormalParameterList node) => null;

  @override
  R visitForStatement(ForStatement node) => null;

  @override
  R visitFunctionDeclaration(FunctionDeclaration node) => null;

  @override
  R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) =>
      null;

  @override
  R visitFunctionExpression(FunctionExpression node) => null;

  @override
  R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) =>
      null;

  @override
  R visitFunctionTypeAlias(FunctionTypeAlias node) => null;

  @override
  R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) =>
      null;

  @override
  R visitHideCombinator(HideCombinator node) => null;

  @override
  R visitIfStatement(IfStatement node) => null;

  @override
  R visitImplementsClause(ImplementsClause node) => null;

  @override
  R visitImportDirective(ImportDirective node) => null;

  @override
  R visitIndexExpression(IndexExpression node) => null;

  @override
  R visitInstanceCreationExpression(InstanceCreationExpression node) => null;

  @override
  R visitIntegerLiteral(IntegerLiteral node) => null;

  @override
  R visitInterpolationExpression(InterpolationExpression node) => null;

  @override
  R visitInterpolationString(InterpolationString node) => null;

  @override
  R visitIsExpression(IsExpression node) => null;

  @override
  R visitLabel(Label node) => null;

  @override
  R visitLabeledStatement(LabeledStatement node) => null;

  @override
  R visitLibraryDirective(LibraryDirective node) => null;

  @override
  R visitLibraryIdentifier(LibraryIdentifier node) => null;

  @override
  R visitListLiteral(ListLiteral node) => null;

  @override
  R visitMapLiteral(MapLiteral node) => null;

  @override
  R visitMapLiteralEntry(MapLiteralEntry node) => null;

  @override
  R visitMethodDeclaration(MethodDeclaration node) => null;

  @override
  R visitMethodInvocation(MethodInvocation node) => null;

  @override
  R visitNamedExpression(NamedExpression node) => null;

  @override
  R visitNativeClause(NativeClause node) => null;

  @override
  R visitNativeFunctionBody(NativeFunctionBody node) => null;

  @override
  R visitNullLiteral(NullLiteral node) => null;

  @override
  R visitParenthesizedExpression(ParenthesizedExpression node) => null;

  @override
  R visitPartDirective(PartDirective node) => null;

  @override
  R visitPartOfDirective(PartOfDirective node) => null;

  @override
  R visitPostfixExpression(PostfixExpression node) => null;

  @override
  R visitPrefixedIdentifier(PrefixedIdentifier node) => null;

  @override
  R visitPrefixExpression(PrefixExpression node) => null;

  @override
  R visitPropertyAccess(PropertyAccess node) => null;

  @override
  R
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) =>
      null;

  @override
  R visitRethrowExpression(RethrowExpression node) => null;

  @override
  R visitReturnStatement(ReturnStatement node) => null;

  @override
  R visitScriptTag(ScriptTag node) => null;

  @override
  R visitShowCombinator(ShowCombinator node) => null;

  @override
  R visitSimpleFormalParameter(SimpleFormalParameter node) => null;

  @override
  R visitSimpleIdentifier(SimpleIdentifier node) => null;

  @override
  R visitSimpleStringLiteral(SimpleStringLiteral node) => null;

  @override
  R visitStringInterpolation(StringInterpolation node) => null;

  @override
  R visitSuperConstructorInvocation(SuperConstructorInvocation node) => null;

  @override
  R visitSuperExpression(SuperExpression node) => null;

  @override
  R visitSwitchCase(SwitchCase node) => null;

  @override
  R visitSwitchDefault(SwitchDefault node) => null;

  @override
  R visitSwitchStatement(SwitchStatement node) => null;

  @override
  R visitSymbolLiteral(SymbolLiteral node) => null;

  @override
  R visitThisExpression(ThisExpression node) => null;

  @override
  R visitThrowExpression(ThrowExpression node) => null;

  @override
  R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) => null;

  @override
  R visitTryStatement(TryStatement node) => null;

  @override
  R visitTypeArgumentList(TypeArgumentList node) => null;

  @override
  R visitTypeName(TypeName node) => null;

  @override
  R visitTypeParameter(TypeParameter node) => null;

  @override
  R visitTypeParameterList(TypeParameterList node) => null;

  @override
  R visitVariableDeclaration(VariableDeclaration node) => null;

  @override
  R visitVariableDeclarationList(VariableDeclarationList node) => null;

  @override
  R visitVariableDeclarationStatement(VariableDeclarationStatement node) =>
      null;

  @override
  R visitWhileStatement(WhileStatement node) => null;

  @override
  R visitWithClause(WithClause node) => null;

  @override
  R visitYieldStatement(YieldStatement node) => null;
}

/**
 * Instances of the class `SimpleFormalParameter` represent a simple formal parameter.
 *
 * <pre>
 * simpleFormalParameter ::=
 *     ('final' [TypeName] | 'var' | [TypeName])? [SimpleIdentifier]
 * </pre>
 */
class SimpleFormalParameter extends NormalFormalParameter {
  /**
   * The token representing either the 'final', 'const' or 'var' keyword, or `null` if no
   * keyword was used.
   */
  Token keyword;

  /**
   * The name of the declared type of the parameter, or `null` if the parameter does not have
   * a declared type.
   */
  TypeName _type;

  /**
   * Initialize a newly created formal parameter.
   *
   * @param comment the documentation comment associated with this parameter
   * @param metadata the annotations associated with this parameter
   * @param keyword the token representing either the 'final', 'const' or 'var' keyword
   * @param type the name of the declared type of the parameter
   * @param identifier the name of the parameter being declared
   */
  SimpleFormalParameter(Comment comment, List<Annotation> metadata,
      this.keyword, TypeName type, SimpleIdentifier identifier)
      : super(comment, metadata, identifier) {
    _type = becomeParentOf(type);
  }

  @override
  Token get beginToken {
    NodeList<Annotation> metadata = this.metadata;
    if (!metadata.isEmpty) {
      return metadata.beginToken;
    } else if (keyword != null) {
      return keyword;
    } else if (_type != null) {
      return _type.beginToken;
    }
    return identifier.beginToken;
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => super._childEntities
      ..add(keyword)
      ..add(_type)
      ..add(identifier);

  @override
  Token get endToken => identifier.endToken;

  @override
  bool get isConst =>
      (keyword is KeywordToken) && (keyword as KeywordToken).keyword == Keyword.CONST;

  @override
  bool get isFinal =>
      (keyword is KeywordToken) && (keyword as KeywordToken).keyword == Keyword.FINAL;

  /**
   * Return the name of the declared type of the parameter, or `null` if the parameter does
   * not have a declared type.
   *
   * @return the name of the declared type of the parameter
   */
  TypeName get type => _type;

  /**
   * Set the name of the declared type of the parameter to the given type name.
   *
   * @param typeName the name of the declared type of the parameter
   */
  void set type(TypeName typeName) {
    _type = becomeParentOf(typeName);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitSimpleFormalParameter(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_type, visitor);
    safelyVisitChild(identifier, visitor);
  }
}

/**
 * Instances of the class `SimpleIdentifier` represent a simple identifier.
 *
 * <pre>
 * simpleIdentifier ::=
 *     initialCharacter internalCharacter*
 *
 * initialCharacter ::= '_' | '$' | letter
 *
 * internalCharacter ::= '_' | '$' | letter | digit
 * </pre>
 */
class SimpleIdentifier extends Identifier {
  /**
   * The token representing the identifier.
   */
  Token token;

  /**
   * The element associated with this identifier based on static type information, or `null`
   * if the AST structure has not been resolved or if this identifier could not be resolved.
   */
  Element _staticElement;

  /**
   * The element associated with this identifier based on propagated type information, or
   * `null` if the AST structure has not been resolved or if this identifier could not be
   * resolved.
   */
  Element _propagatedElement;

  /**
   * If this expression is both in a getter and setter context, the [AuxiliaryElements] will
   * be set to hold onto the static and propagated information. The auxiliary element will hold onto
   * the elements from the getter context.
   */
  AuxiliaryElements auxiliaryElements = null;

  /**
   * Initialize a newly created identifier.
   *
   * @param token the token representing the identifier
   */
  SimpleIdentifier(this.token);

  @override
  Token get beginToken => token;

  @override
  Element get bestElement {
    if (_propagatedElement == null) {
      return _staticElement;
    }
    return _propagatedElement;
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(token);

  @override
  Token get endToken => token;

  /**
   * Returns `true` if this identifier is the "name" part of a prefixed identifier or a method
   * invocation.
   *
   * @return `true` if this identifier is the "name" part of a prefixed identifier or a method
   *         invocation
   */
  bool get isQualified {
    AstNode parent = this.parent;
    if (parent is PrefixedIdentifier) {
      return identical(parent.identifier, this);
    }
    if (parent is PropertyAccess) {
      return identical(parent.propertyName, this);
    }
    if (parent is MethodInvocation) {
      MethodInvocation invocation = parent;
      return identical(invocation.methodName, this) &&
          invocation.realTarget != null;
    }
    return false;
  }

  @override
  bool get isSynthetic => token.isSynthetic;

  @override
  String get name => token.lexeme;

  @override
  int get precedence => 16;

  @override
  Element get propagatedElement => _propagatedElement;

  /**
   * Set the element associated with this identifier based on propagated type information to the
   * given element.
   *
   * @param element the element to be associated with this identifier
   */
  void set propagatedElement(Element element) {
    _propagatedElement = _validateElement(element);
  }

  @override
  Element get staticElement => _staticElement;

  /**
   * Set the element associated with this identifier based on static type information to the given
   * element.
   *
   * @param element the element to be associated with this identifier
   */
  void set staticElement(Element element) {
    _staticElement = _validateElement(element);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitSimpleIdentifier(this);

  /**
   * Return `true` if this identifier is the name being declared in a declaration.
   *
   * @return `true` if this identifier is the name being declared in a declaration
   */
  bool inDeclarationContext() {
    AstNode parent = this.parent;
    if (parent is CatchClause) {
      CatchClause clause = parent;
      return identical(this, clause.exceptionParameter) ||
          identical(this, clause.stackTraceParameter);
    } else if (parent is ClassDeclaration) {
      return identical(this, parent.name);
    } else if (parent is ClassTypeAlias) {
      return identical(this, parent.name);
    } else if (parent is ConstructorDeclaration) {
      return identical(this, parent.name);
    } else if (parent is DeclaredIdentifier) {
      return identical(this, parent.identifier);
    } else if (parent is EnumDeclaration) {
      return identical(this, parent.name);
    } else if (parent is EnumConstantDeclaration) {
      return identical(this, parent.name);
    } else if (parent is FunctionDeclaration) {
      return identical(this, parent.name);
    } else if (parent is FunctionTypeAlias) {
      return identical(this, parent.name);
    } else if (parent is ImportDirective) {
      return identical(this, parent.prefix);
    } else if (parent is Label) {
      return identical(this, parent.label) &&
          (parent.parent is LabeledStatement);
    } else if (parent is MethodDeclaration) {
      return identical(this, parent.name);
    } else if (parent is FunctionTypedFormalParameter ||
        parent is SimpleFormalParameter) {
      return identical(this, (parent as NormalFormalParameter).identifier);
    } else if (parent is TypeParameter) {
      return identical(this, parent.name);
    } else if (parent is VariableDeclaration) {
      return identical(this, parent.name);
    }
    return false;
  }

  /**
   * Return `true` if this expression is computing a right-hand value.
   *
   * Note that [inGetterContext] and [inSetterContext] are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return `true`
   * when invoked on the same node.
   *
   * @return `true` if this expression is in a context where a getter will be invoked
   */
  bool inGetterContext() {
    AstNode parent = this.parent;
    AstNode target = this;
    // skip prefix
    if (parent is PrefixedIdentifier) {
      PrefixedIdentifier prefixed = parent as PrefixedIdentifier;
      if (identical(prefixed.prefix, this)) {
        return true;
      }
      parent = prefixed.parent;
      target = prefixed;
    } else if (parent is PropertyAccess) {
      PropertyAccess access = parent as PropertyAccess;
      if (identical(access.target, this)) {
        return true;
      }
      parent = access.parent;
      target = access;
    }
    // skip label
    if (parent is Label) {
      return false;
    }
    // analyze usage
    if (parent is AssignmentExpression) {
      AssignmentExpression expr = parent as AssignmentExpression;
      if (identical(expr.leftHandSide, target) &&
          expr.operator.type == TokenType.EQ) {
        return false;
      }
    }
    if (parent is ForEachStatement) {
      ForEachStatement stmt = parent as ForEachStatement;
      if (identical(stmt.identifier, target)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return `true` if this expression is computing a left-hand value.
   *
   * Note that [inGetterContext] and [inSetterContext] are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return `true`
   * when invoked on the same node.
   *
   * @return `true` if this expression is in a context where a setter will be invoked
   */
  bool inSetterContext() {
    AstNode parent = this.parent;
    AstNode target = this;
    // skip prefix
    if (parent is PrefixedIdentifier) {
      PrefixedIdentifier prefixed = parent as PrefixedIdentifier;
      // if this is the prefix, then return false
      if (identical(prefixed.prefix, this)) {
        return false;
      }
      parent = prefixed.parent;
      target = prefixed;
    } else if (parent is PropertyAccess) {
      PropertyAccess access = parent as PropertyAccess;
      if (identical(access.target, this)) {
        return false;
      }
      parent = access.parent;
      target = access;
    }
    // analyze usage
    if (parent is PrefixExpression) {
      return (parent as PrefixExpression).operator.type.isIncrementOperator;
    } else if (parent is PostfixExpression) {
      return true;
    } else if (parent is AssignmentExpression) {
      return identical((parent as AssignmentExpression).leftHandSide, target);
    } else if (parent is ForEachStatement) {
      return identical((parent as ForEachStatement).identifier, target);
    }
    return false;
  }

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }

  /**
   * Return the given element if it is valid, or report the problem and return `null` if it is
   * not appropriate.
   *
   * @param parent the parent of the element, used for reporting when there is a problem
   * @param isValid `true` if the element is appropriate
   * @param element the element to be associated with this identifier
   * @return the element to be associated with this identifier
   */
  Element _returnOrReportElement(AstNode parent, bool isValid,
      Element element) {
    if (!isValid) {
      AnalysisEngine.instance.logger.logInformation(
          "Internal error: attempting to set the name of a ${parent.runtimeType} to a ${element.runtimeType}",
          new CaughtException(new AnalysisException(), null));
      return null;
    }
    return element;
  }

  /**
   * Return the given element if it is an appropriate element based on the parent of this
   * identifier, or `null` if it is not appropriate.
   *
   * @param element the element to be associated with this identifier
   * @return the element to be associated with this identifier
   */
  Element _validateElement(Element element) {
    if (element == null) {
      return null;
    }
    AstNode parent = this.parent;
    if (parent is ClassDeclaration && identical(parent.name, this)) {
      return _returnOrReportElement(parent, element is ClassElement, element);
    } else if (parent is ClassTypeAlias && identical(parent.name, this)) {
      return _returnOrReportElement(parent, element is ClassElement, element);
    } else if (parent is DeclaredIdentifier &&
        identical(parent.identifier, this)) {
      return _returnOrReportElement(
          parent,
          element is LocalVariableElement,
          element);
    } else if (parent is FormalParameter &&
        identical(parent.identifier, this)) {
      return _returnOrReportElement(
          parent,
          element is ParameterElement,
          element);
    } else if (parent is FunctionDeclaration && identical(parent.name, this)) {
      return _returnOrReportElement(
          parent,
          element is ExecutableElement,
          element);
    } else if (parent is FunctionTypeAlias && identical(parent.name, this)) {
      return _returnOrReportElement(
          parent,
          element is FunctionTypeAliasElement,
          element);
    } else if (parent is MethodDeclaration && identical(parent.name, this)) {
      return _returnOrReportElement(
          parent,
          element is ExecutableElement,
          element);
    } else if (parent is TypeParameter && identical(parent.name, this)) {
      return _returnOrReportElement(
          parent,
          element is TypeParameterElement,
          element);
    } else if (parent is VariableDeclaration && identical(parent.name, this)) {
      return _returnOrReportElement(
          parent,
          element is VariableElement,
          element);
    }
    return element;
  }
}

/**
 * Instances of the class `SimpleStringLiteral` represent a string literal expression that
 * does not contain any interpolations.
 *
 * <pre>
 * simpleStringLiteral ::=
 *     rawStringLiteral
 *   | basicStringLiteral
 *
 * rawStringLiteral ::=
 *     'r' basicStringLiteral
 *
 * simpleStringLiteral ::=
 *     multiLineStringLiteral
 *   | singleLineStringLiteral
 *
 * multiLineStringLiteral ::=
 *     "'''" characters "'''"
 *   | '"""' characters '"""'
 *
 * singleLineStringLiteral ::=
 *     "'" characters "'"
 *     '"' characters '"'
 * </pre>
 */
class SimpleStringLiteral extends SingleStringLiteral {
  /**
   * The token representing the literal.
   */
  Token literal;

  /**
   * The value of the literal.
   */
  String _value;

  /**
   * The toolkit specific element associated with this literal, or `null`.
   */
  Element toolkitElement;

  /**
   * Initialize a newly created simple string literal.
   *
   * @param literal the token representing the literal
   * @param value the value of the literal
   */
  SimpleStringLiteral(this.literal, String value) {
    _value = StringUtilities.intern(value);
  }

  @override
  Token get beginToken => literal;

  @override
  Iterable get childEntities => new ChildEntities()..add(literal);

  @override
  int get contentsEnd {
    return contentsOffset + value.length;
  }

  @override
  int get contentsOffset {
    int contentsOffset = 0;
    if (isRaw) {
      contentsOffset += 1;
    }
    if (isMultiline) {
      contentsOffset += 3;
    } else {
      contentsOffset += 1;
    }
    return offset + contentsOffset;
  }

  @override
  Token get endToken => literal;

  @override
  bool get isMultiline {
    String lexeme = literal.lexeme;
    if (lexeme.length < 3) {
      return false;
    }
    // skip 'r'
    int offset = 0;
    if (isRaw) {
      offset = 1;
    }
    // check prefix
    return StringUtilities.startsWith3(lexeme, offset, 0x22, 0x22, 0x22) ||
        StringUtilities.startsWith3(lexeme, offset, 0x27, 0x27, 0x27);
  }

  @override
  bool get isRaw => literal.lexeme.codeUnitAt(0) == 0x72;

  @override
  bool get isSingleQuoted {
    String lexeme = literal.lexeme;
    if (lexeme.isEmpty) {
      return false;
    }
    int codeZero = lexeme.codeUnitAt(0);
    if (codeZero == 0x72) {
      return lexeme.length > 1 && lexeme.codeUnitAt(1) == 0x27;
    }
    return codeZero == 0x27;
  }

  @override
  bool get isSynthetic => literal.isSynthetic;

  /**
   * Return the value of the literal.
   *
   * @return the value of the literal
   */
  String get value => _value;

  /**
   * Set the value of the literal to the given string.
   *
   * @param string the value of the literal
   */
  void set value(String string) {
    _value = StringUtilities.intern(_value);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitSimpleStringLiteral(this);

  @override
  void appendStringValue(StringBuffer buffer) {
    buffer.write(value);
  }

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class [SingleStringLiteral] represent a single string
 * literal expression.
 *
 * <pre>
 * singleStringLiteral ::=
 *     [SimpleStringLiteral]
 *   | [StringInterpolation]
 * </pre>
 */
abstract class SingleStringLiteral extends StringLiteral {
  /**
   * Return the offset of the after-last contents character.
   */
  int get contentsEnd;

  /**
   * Return the offset of the first contents character.
   */
  int get contentsOffset;

  /**
   * Return `true` if this string literal is a multi-line string.
   */
  bool get isMultiline;

  /**
   * Return `true` if this string literal is a raw string.
   */
  bool get isRaw;

  /**
   * Return `true` if this string literal uses single qoutes (' or ''').
   * Return `false` if this string literal uses double qoutes (" or """).
   */
  bool get isSingleQuoted;
}

/**
 * Instances of the class `Statement` defines the behavior common to nodes that represent a
 * statement.
 *
 * <pre>
 * statement ::=
 *     [Block]
 *   | [VariableDeclarationStatement]
 *   | [ForStatement]
 *   | [ForEachStatement]
 *   | [WhileStatement]
 *   | [DoStatement]
 *   | [SwitchStatement]
 *   | [IfStatement]
 *   | [TryStatement]
 *   | [BreakStatement]
 *   | [ContinueStatement]
 *   | [ReturnStatement]
 *   | [ExpressionStatement]
 *   | [FunctionDeclarationStatement]
 * </pre>
 */
abstract class Statement extends AstNode {
  /**
   * If this is a labeled statement, return the unlabeled portion of the
   * statement.  Otherwise return the statement itself.
   */
  Statement get unlabeled => this;
}

/**
 * Instances of the class `StringInterpolation` represent a string interpolation literal.
 *
 * <pre>
 * stringInterpolation ::=
 *     ''' [InterpolationElement]* '''
 *   | '"' [InterpolationElement]* '"'
 * </pre>
 */
class StringInterpolation extends SingleStringLiteral {
  /**
   * The elements that will be composed to produce the resulting string.
   */
  NodeList<InterpolationElement> _elements;

  /**
   * Initialize a newly created string interpolation expression.
   *
   * @param elements the elements that will be composed to produce the resulting string
   */
  StringInterpolation(List<InterpolationElement> elements) {
    _elements = new NodeList<InterpolationElement>(this, elements);
  }

  @override
  Token get beginToken => _elements.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()..addAll(_elements);

  @override
  int get contentsEnd {
    InterpolationString element = _elements.last;
    return element.contentsEnd;
  }

  @override
  int get contentsOffset {
    InterpolationString element = _elements.first;
    return element.contentsOffset;
  }

  /**
   * Return the elements that will be composed to produce the resulting string.
   *
   * @return the elements that will be composed to produce the resulting string
   */
  NodeList<InterpolationElement> get elements => _elements;

  @override
  Token get endToken => _elements.endToken;

  @override
  bool get isMultiline {
    InterpolationString element = _elements.first;
    String lexeme = element.contents.lexeme;
    if (lexeme.length < 3) {
      return false;
    }
    return StringUtilities.startsWith3(lexeme, 0, 0x22, 0x22, 0x22) ||
        StringUtilities.startsWith3(lexeme, 0, 0x27, 0x27, 0x27);
  }

  @override
  bool get isRaw => false;

  @override
  bool get isSingleQuoted {
    InterpolationString lastString = _elements.first;
    String lexeme = lastString.contents.lexeme;
    return StringUtilities.startsWithChar(lexeme, 0x27);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitStringInterpolation(this);

  @override
  void appendStringValue(StringBuffer buffer) {
    throw new IllegalArgumentException();
  }

  @override
  void visitChildren(AstVisitor visitor) {
    _elements.accept(visitor);
  }
}

/**
 * Instances of the class `StringLiteral` represent a string literal expression.
 *
 * <pre>
 * stringLiteral ::=
 *     [SimpleStringLiteral]
 *   | [AdjacentStrings]
 *   | [StringInterpolation]
 * </pre>
 */
abstract class StringLiteral extends Literal {
  /**
   * Return the value of the string literal, or `null` if the string is not a
   * constant string without any string interpolation.
   */
  String get stringValue {
    StringBuffer buffer = new StringBuffer();
    try {
      appendStringValue(buffer);
    } on IllegalArgumentException catch (exception) {
      return null;
    }
    return buffer.toString();
  }

  /**
   * Append the value of this string literal to the given [buffer]. Throw an
   * [IllegalArgumentException] if the string is not a constant string without
   * any string interpolation.
   */
  void appendStringValue(StringBuffer buffer);
}

/**
 * Instances of the class `SuperConstructorInvocation` represent the invocation of a
 * superclass' constructor from within a constructor's initialization list.
 *
 * <pre>
 * superInvocation ::=
 *     'super' ('.' [SimpleIdentifier])? [ArgumentList]
 * </pre>
 */
class SuperConstructorInvocation extends ConstructorInitializer {
  /**
   * The token for the 'super' keyword.
   */
  Token keyword;

  /**
   * The token for the period before the name of the constructor that is being invoked, or
   * `null` if the unnamed constructor is being invoked.
   */
  Token period;

  /**
   * The name of the constructor that is being invoked, or `null` if the unnamed constructor
   * is being invoked.
   */
  SimpleIdentifier _constructorName;

  /**
   * The list of arguments to the constructor.
   */
  ArgumentList _argumentList;

  /**
   * The element associated with the constructor based on static type information, or `null`
   * if the AST structure has not been resolved or if the constructor could not be resolved.
   */
  ConstructorElement staticElement;

  /**
   * Initialize a newly created super invocation to invoke the inherited constructor with the given
   * name with the given arguments.
   *
   * @param keyword the token for the 'super' keyword
   * @param period the token for the period before the name of the constructor that is being invoked
   * @param constructorName the name of the constructor that is being invoked
   * @param argumentList the list of arguments to the constructor
   */
  SuperConstructorInvocation(this.keyword, this.period,
      SimpleIdentifier constructorName, ArgumentList argumentList) {
    _constructorName = becomeParentOf(constructorName);
    _argumentList = becomeParentOf(argumentList);
  }

  /**
   * Return the list of arguments to the constructor.
   *
   * @return the list of arguments to the constructor
   */
  ArgumentList get argumentList => _argumentList;

  /**
   * Set the list of arguments to the constructor to the given list.
   *
   * @param argumentList the list of arguments to the constructor
   */
  void set argumentList(ArgumentList argumentList) {
    _argumentList = becomeParentOf(argumentList);
  }

  @override
  Token get beginToken => keyword;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(period)
      ..add(_constructorName)
      ..add(_argumentList);

  /**
   * Return the name of the constructor that is being invoked, or `null` if the unnamed
   * constructor is being invoked.
   *
   * @return the name of the constructor that is being invoked
   */
  SimpleIdentifier get constructorName => _constructorName;

  /**
   * Set the name of the constructor that is being invoked to the given identifier.
   *
   * @param identifier the name of the constructor that is being invoked
   */
  void set constructorName(SimpleIdentifier identifier) {
    _constructorName = becomeParentOf(identifier);
  }

  @override
  Token get endToken => _argumentList.endToken;

  @override
  accept(AstVisitor visitor) => visitor.visitSuperConstructorInvocation(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_constructorName, visitor);
    safelyVisitChild(_argumentList, visitor);
  }
}

/**
 * Instances of the class `SuperExpression` represent a super expression.
 *
 * <pre>
 * superExpression ::=
 *     'super'
 * </pre>
 */
class SuperExpression extends Expression {
  /**
   * The token representing the keyword.
   */
  Token keyword;

  /**
   * Initialize a newly created super expression.
   *
   * @param keyword the token representing the keyword
   */
  SuperExpression(this.keyword);

  @override
  Token get beginToken => keyword;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(keyword);

  @override
  Token get endToken => keyword;

  @override
  int get precedence => 16;

  @override
  accept(AstVisitor visitor) => visitor.visitSuperExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `SwitchCase` represent the case in a switch statement.
 *
 * <pre>
 * switchCase ::=
 *     [SimpleIdentifier]* 'case' [Expression] ':' [Statement]*
 * </pre>
 */
class SwitchCase extends SwitchMember {
  /**
   * The expression controlling whether the statements will be executed.
   */
  Expression _expression;

  /**
   * Initialize a newly created switch case.
   *
   * @param labels the labels associated with the switch member
   * @param keyword the token representing the 'case' or 'default' keyword
   * @param expression the expression controlling whether the statements will be executed
   * @param colon the colon separating the keyword or the expression from the statements
   * @param statements the statements that will be executed if this switch member is selected
   */
  SwitchCase(List<Label> labels, Token keyword, Expression expression,
      Token colon, List<Statement> statements)
      : super(labels, keyword, colon, statements) {
    _expression = becomeParentOf(expression);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..addAll(labels)
      ..add(keyword)
      ..add(_expression)
      ..add(colon)
      ..addAll(statements);

  /**
   * Return the expression controlling whether the statements will be executed.
   *
   * @return the expression controlling whether the statements will be executed
   */
  Expression get expression => _expression;

  /**
   * Set the expression controlling whether the statements will be executed to the given expression.
   *
   * @param expression the expression controlling whether the statements will be executed
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitSwitchCase(this);

  @override
  void visitChildren(AstVisitor visitor) {
    labels.accept(visitor);
    safelyVisitChild(_expression, visitor);
    statements.accept(visitor);
  }
}

/**
 * Instances of the class `SwitchDefault` represent the default case in a switch statement.
 *
 * <pre>
 * switchDefault ::=
 *     [SimpleIdentifier]* 'default' ':' [Statement]*
 * </pre>
 */
class SwitchDefault extends SwitchMember {
  /**
   * Initialize a newly created switch default.
   *
   * @param labels the labels associated with the switch member
   * @param keyword the token representing the 'case' or 'default' keyword
   * @param colon the colon separating the keyword or the expression from the statements
   * @param statements the statements that will be executed if this switch member is selected
   */
  SwitchDefault(List<Label> labels, Token keyword, Token colon,
      List<Statement> statements)
      : super(labels, keyword, colon, statements);

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..addAll(labels)
      ..add(keyword)
      ..add(colon)
      ..addAll(statements);

  @override
  accept(AstVisitor visitor) => visitor.visitSwitchDefault(this);

  @override
  void visitChildren(AstVisitor visitor) {
    labels.accept(visitor);
    statements.accept(visitor);
  }
}

/**
 * The abstract class `SwitchMember` defines the behavior common to objects representing
 * elements within a switch statement.
 *
 * <pre>
 * switchMember ::=
 *     switchCase
 *   | switchDefault
 * </pre>
 */
abstract class SwitchMember extends AstNode {
  /**
   * The labels associated with the switch member.
   */
  NodeList<Label> _labels;

  /**
   * The token representing the 'case' or 'default' keyword.
   */
  Token keyword;

  /**
   * The colon separating the keyword or the expression from the statements.
   */
  Token colon;

  /**
   * The statements that will be executed if this switch member is selected.
   */
  NodeList<Statement> _statements;

  /**
   * Initialize a newly created switch member.
   *
   * @param labels the labels associated with the switch member
   * @param keyword the token representing the 'case' or 'default' keyword
   * @param colon the colon separating the keyword or the expression from the statements
   * @param statements the statements that will be executed if this switch member is selected
   */
  SwitchMember(List<Label> labels, this.keyword, this.colon,
      List<Statement> statements) {
    _labels = new NodeList<Label>(this, labels);
    _statements = new NodeList<Statement>(this, statements);
  }

  @override
  Token get beginToken {
    if (!_labels.isEmpty) {
      return _labels.beginToken;
    }
    return keyword;
  }

  @override
  Token get endToken {
    if (!_statements.isEmpty) {
      return _statements.endToken;
    }
    return colon;
  }

  /**
   * Return the labels associated with the switch member.
   *
   * @return the labels associated with the switch member
   */
  NodeList<Label> get labels => _labels;

  /**
   * Return the statements that will be executed if this switch member is selected.
   *
   * @return the statements that will be executed if this switch member is selected
   */
  NodeList<Statement> get statements => _statements;
}

/**
 * Instances of the class `SwitchStatement` represent a switch statement.
 *
 * <pre>
 * switchStatement ::=
 *     'switch' '(' [Expression] ')' '{' [SwitchCase]* [SwitchDefault]? '}'
 * </pre>
 */
class SwitchStatement extends Statement {
  /**
   * The token representing the 'switch' keyword.
   */
  Token keyword;

  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The expression used to determine which of the switch members will be selected.
   */
  Expression _expression;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * The left curly bracket.
   */
  Token leftBracket;

  /**
   * The switch members that can be selected by the expression.
   */
  NodeList<SwitchMember> _members;

  /**
   * The right curly bracket.
   */
  Token rightBracket;

  /**
   * Initialize a newly created switch statement.
   *
   * @param keyword the token representing the 'switch' keyword
   * @param leftParenthesis the left parenthesis
   * @param expression the expression used to determine which of the switch members will be selected
   * @param rightParenthesis the right parenthesis
   * @param leftBracket the left curly bracket
   * @param members the switch members that can be selected by the expression
   * @param rightBracket the right curly bracket
   */
  SwitchStatement(this.keyword, this.leftParenthesis, Expression expression,
      this.rightParenthesis, this.leftBracket, List<SwitchMember> members,
      this.rightBracket) {
    _expression = becomeParentOf(expression);
    _members = new NodeList<SwitchMember>(this, members);
  }

  @override
  Token get beginToken => keyword;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(leftParenthesis)
      ..add(_expression)
      ..add(rightParenthesis)
      ..add(leftBracket)
      ..addAll(_members)
      ..add(rightBracket);

  @override
  Token get endToken => rightBracket;

  /**
   * Return the expression used to determine which of the switch members will be selected.
   *
   * @return the expression used to determine which of the switch members will be selected
   */
  Expression get expression => _expression;

  /**
   * Set the expression used to determine which of the switch members will be selected to the given
   * expression.
   *
   * @param expression the expression used to determine which of the switch members will be selected
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  /**
   * Return the switch members that can be selected by the expression.
   *
   * @return the switch members that can be selected by the expression
   */
  NodeList<SwitchMember> get members => _members;

  @override
  accept(AstVisitor visitor) => visitor.visitSwitchStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
    _members.accept(visitor);
  }
}

/**
 * Instances of the class `SymbolLiteral` represent a symbol literal expression.
 *
 * <pre>
 * symbolLiteral ::=
 *     '#' (operator | (identifier ('.' identifier)*))
 * </pre>
 */
class SymbolLiteral extends Literal {
  /**
   * The token introducing the literal.
   */
  Token poundSign;

  /**
   * The components of the literal.
   */
  final List<Token> components;

  /**
   * Initialize a newly created symbol literal.
   *
   * @param poundSign the token introducing the literal
   * @param components the components of the literal
   */
  SymbolLiteral(this.poundSign, this.components);

  @override
  Token get beginToken => poundSign;

  /**
   * TODO(paulberry): untested.
   * TODO(paulberry): add "." tokens.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(poundSign)
      ..addAll(components);

  @override
  Token get endToken => components[components.length - 1];

  @override
  accept(AstVisitor visitor) => visitor.visitSymbolLiteral(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `ThisExpression` represent a this expression.
 *
 * <pre>
 * thisExpression ::=
 *     'this'
 * </pre>
 */
class ThisExpression extends Expression {
  /**
   * The token representing the keyword.
   */
  Token keyword;

  /**
   * Initialize a newly created this expression.
   *
   * @param keyword the token representing the keyword
   */
  ThisExpression(this.keyword);

  @override
  Token get beginToken => keyword;

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()..add(keyword);

  @override
  Token get endToken => keyword;

  @override
  int get precedence => 16;

  @override
  accept(AstVisitor visitor) => visitor.visitThisExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    // There are no children to visit.
  }
}

/**
 * Instances of the class `ThrowExpression` represent a throw expression.
 *
 * <pre>
 * throwExpression ::=
 *     'throw' [Expression]
 * </pre>
 */
class ThrowExpression extends Expression {
  /**
   * The token representing the 'throw' keyword.
   */
  Token keyword;

  /**
   * The expression computing the exception to be thrown.
   */
  Expression _expression;

  /**
   * Initialize a newly created throw expression.
   *
   * @param keyword the token representing the 'throw' keyword
   * @param expression the expression computing the exception to be thrown
   */
  ThrowExpression(this.keyword, Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken => keyword;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(_expression);

  @override
  Token get endToken {
    if (_expression != null) {
      return _expression.endToken;
    }
    return keyword;
  }

  /**
   * Return the expression computing the exception to be thrown.
   *
   * @return the expression computing the exception to be thrown
   */
  Expression get expression => _expression;

  /**
   * Set the expression computing the exception to be thrown to the given expression.
   *
   * @param expression the expression computing the exception to be thrown
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  int get precedence => 0;

  @override
  accept(AstVisitor visitor) => visitor.visitThrowExpression(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
  }
}

/**
 * Instances of the class `TopLevelVariableDeclaration` represent the declaration of one or
 * more top-level variables of the same type.
 *
 * <pre>
 * topLevelVariableDeclaration ::=
 *     ('final' | 'const') type? staticFinalDeclarationList ';'
 *   | variableDeclaration ';'
 * </pre>
 */
class TopLevelVariableDeclaration extends CompilationUnitMember {
  /**
   * The top-level variables being declared.
   */
  VariableDeclarationList _variableList;

  /**
   * The semicolon terminating the declaration.
   */
  Token semicolon;

  /**
   * Initialize a newly created top-level variable declaration.
   *
   * @param comment the documentation comment associated with this variable
   * @param metadata the annotations associated with this variable
   * @param variableList the top-level variables being declared
   * @param semicolon the semicolon terminating the declaration
   */
  TopLevelVariableDeclaration(Comment comment, List<Annotation> metadata,
      VariableDeclarationList variableList, this.semicolon)
      : super(comment, metadata) {
    _variableList = becomeParentOf(variableList);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(_variableList)
      ..add(semicolon);

  @override
  Element get element => null;

  @override
  Token get endToken => semicolon;

  @override
  Token get firstTokenAfterCommentAndMetadata => _variableList.beginToken;

  /**
   * Return the top-level variables being declared.
   *
   * @return the top-level variables being declared
   */
  VariableDeclarationList get variables => _variableList;

  /**
   * Set the top-level variables being declared to the given list of variables.
   *
   * @param variableList the top-level variables being declared
   */
  void set variables(VariableDeclarationList variableList) {
    _variableList = becomeParentOf(variableList);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitTopLevelVariableDeclaration(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_variableList, visitor);
  }
}

/**
 * Instances of the class `ToSourceVisitor` write a source representation of a visited AST
 * node (and all of it's children) to a writer.
 */
class ToSourceVisitor implements AstVisitor<Object> {
  /**
   * The writer to which the source is to be written.
   */
  final PrintWriter _writer;

  /**
   * Initialize a newly created visitor to write source code representing the visited nodes to the
   * given writer.
   *
   * @param writer the writer to which the source is to be written
   */
  ToSourceVisitor(this._writer);

  @override
  Object visitAdjacentStrings(AdjacentStrings node) {
    _visitNodeListWithSeparator(node.strings, " ");
    return null;
  }

  @override
  Object visitAnnotation(Annotation node) {
    _writer.print('@');
    _visitNode(node.name);
    _visitNodeWithPrefix(".", node.constructorName);
    _visitNode(node.arguments);
    return null;
  }

  @override
  Object visitArgumentList(ArgumentList node) {
    _writer.print('(');
    _visitNodeListWithSeparator(node.arguments, ", ");
    _writer.print(')');
    return null;
  }

  @override
  Object visitAsExpression(AsExpression node) {
    _visitNode(node.expression);
    _writer.print(" as ");
    _visitNode(node.type);
    return null;
  }

  @override
  Object visitAssertStatement(AssertStatement node) {
    _writer.print("assert (");
    _visitNode(node.condition);
    _writer.print(");");
    return null;
  }

  @override
  Object visitAssignmentExpression(AssignmentExpression node) {
    _visitNode(node.leftHandSide);
    _writer.print(' ');
    _writer.print(node.operator.lexeme);
    _writer.print(' ');
    _visitNode(node.rightHandSide);
    return null;
  }

  @override
  Object visitAwaitExpression(AwaitExpression node) {
    _writer.print("await ");
    _visitNode(node.expression);
    _writer.print(";");
    return null;
  }

  @override
  Object visitBinaryExpression(BinaryExpression node) {
    _visitNode(node.leftOperand);
    _writer.print(' ');
    _writer.print(node.operator.lexeme);
    _writer.print(' ');
    _visitNode(node.rightOperand);
    return null;
  }

  @override
  Object visitBlock(Block node) {
    _writer.print('{');
    _visitNodeListWithSeparator(node.statements, " ");
    _writer.print('}');
    return null;
  }

  @override
  Object visitBlockFunctionBody(BlockFunctionBody node) {
    Token keyword = node.keyword;
    if (keyword != null) {
      _writer.print(keyword.lexeme);
      if (node.star != null) {
        _writer.print('*');
      }
      _writer.print(' ');
    }
    _visitNode(node.block);
    return null;
  }

  @override
  Object visitBooleanLiteral(BooleanLiteral node) {
    _writer.print(node.literal.lexeme);
    return null;
  }

  @override
  Object visitBreakStatement(BreakStatement node) {
    _writer.print("break");
    _visitNodeWithPrefix(" ", node.label);
    _writer.print(";");
    return null;
  }

  @override
  Object visitCascadeExpression(CascadeExpression node) {
    _visitNode(node.target);
    _visitNodeList(node.cascadeSections);
    return null;
  }

  @override
  Object visitCatchClause(CatchClause node) {
    _visitNodeWithPrefix("on ", node.exceptionType);
    if (node.catchKeyword != null) {
      if (node.exceptionType != null) {
        _writer.print(' ');
      }
      _writer.print("catch (");
      _visitNode(node.exceptionParameter);
      _visitNodeWithPrefix(", ", node.stackTraceParameter);
      _writer.print(") ");
    } else {
      _writer.print(" ");
    }
    _visitNode(node.body);
    return null;
  }

  @override
  Object visitClassDeclaration(ClassDeclaration node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitTokenWithSuffix(node.abstractKeyword, " ");
    _writer.print("class ");
    _visitNode(node.name);
    _visitNode(node.typeParameters);
    _visitNodeWithPrefix(" ", node.extendsClause);
    _visitNodeWithPrefix(" ", node.withClause);
    _visitNodeWithPrefix(" ", node.implementsClause);
    _writer.print(" {");
    _visitNodeListWithSeparator(node.members, " ");
    _writer.print("}");
    return null;
  }

  @override
  Object visitClassTypeAlias(ClassTypeAlias node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    if (node.abstractKeyword != null) {
      _writer.print("abstract ");
    }
    _writer.print("class ");
    _visitNode(node.name);
    _visitNode(node.typeParameters);
    _writer.print(" = ");
    _visitNode(node.superclass);
    _visitNodeWithPrefix(" ", node.withClause);
    _visitNodeWithPrefix(" ", node.implementsClause);
    _writer.print(";");
    return null;
  }

  @override
  Object visitComment(Comment node) => null;

  @override
  Object visitCommentReference(CommentReference node) => null;

  @override
  Object visitCompilationUnit(CompilationUnit node) {
    ScriptTag scriptTag = node.scriptTag;
    NodeList<Directive> directives = node.directives;
    _visitNode(scriptTag);
    String prefix = scriptTag == null ? "" : " ";
    _visitNodeListWithSeparatorAndPrefix(prefix, directives, " ");
    prefix = scriptTag == null && directives.isEmpty ? "" : " ";
    _visitNodeListWithSeparatorAndPrefix(prefix, node.declarations, " ");
    return null;
  }

  @override
  Object visitConditionalExpression(ConditionalExpression node) {
    _visitNode(node.condition);
    _writer.print(" ? ");
    _visitNode(node.thenExpression);
    _writer.print(" : ");
    _visitNode(node.elseExpression);
    return null;
  }

  @override
  Object visitConstructorDeclaration(ConstructorDeclaration node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitTokenWithSuffix(node.externalKeyword, " ");
    _visitTokenWithSuffix(node.constKeyword, " ");
    _visitTokenWithSuffix(node.factoryKeyword, " ");
    _visitNode(node.returnType);
    _visitNodeWithPrefix(".", node.name);
    _visitNode(node.parameters);
    _visitNodeListWithSeparatorAndPrefix(" : ", node.initializers, ", ");
    _visitNodeWithPrefix(" = ", node.redirectedConstructor);
    _visitFunctionWithPrefix(" ", node.body);
    return null;
  }

  @override
  Object visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    _visitTokenWithSuffix(node.keyword, ".");
    _visitNode(node.fieldName);
    _writer.print(" = ");
    _visitNode(node.expression);
    return null;
  }

  @override
  Object visitConstructorName(ConstructorName node) {
    _visitNode(node.type);
    _visitNodeWithPrefix(".", node.name);
    return null;
  }

  @override
  Object visitContinueStatement(ContinueStatement node) {
    _writer.print("continue");
    _visitNodeWithPrefix(" ", node.label);
    _writer.print(";");
    return null;
  }

  @override
  Object visitDeclaredIdentifier(DeclaredIdentifier node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitTokenWithSuffix(node.keyword, " ");
    _visitNodeWithSuffix(node.type, " ");
    _visitNode(node.identifier);
    return null;
  }

  @override
  Object visitDefaultFormalParameter(DefaultFormalParameter node) {
    _visitNode(node.parameter);
    if (node.separator != null) {
      _writer.print(" ");
      _writer.print(node.separator.lexeme);
      _visitNodeWithPrefix(" ", node.defaultValue);
    }
    return null;
  }

  @override
  Object visitDoStatement(DoStatement node) {
    _writer.print("do ");
    _visitNode(node.body);
    _writer.print(" while (");
    _visitNode(node.condition);
    _writer.print(");");
    return null;
  }

  @override
  Object visitDoubleLiteral(DoubleLiteral node) {
    _writer.print(node.literal.lexeme);
    return null;
  }

  @override
  Object visitEmptyFunctionBody(EmptyFunctionBody node) {
    _writer.print(';');
    return null;
  }

  @override
  Object visitEmptyStatement(EmptyStatement node) {
    _writer.print(';');
    return null;
  }

  @override
  Object visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitNode(node.name);
    return null;
  }

  @override
  Object visitEnumDeclaration(EnumDeclaration node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _writer.print("enum ");
    _visitNode(node.name);
    _writer.print(" {");
    _visitNodeListWithSeparator(node.constants, ", ");
    _writer.print("}");
    return null;
  }

  @override
  Object visitExportDirective(ExportDirective node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _writer.print("export ");
    _visitNode(node.uri);
    _visitNodeListWithSeparatorAndPrefix(" ", node.combinators, " ");
    _writer.print(';');
    return null;
  }

  @override
  Object visitExpressionFunctionBody(ExpressionFunctionBody node) {
    Token keyword = node.keyword;
    if (keyword != null) {
      _writer.print(keyword.lexeme);
      _writer.print(' ');
    }
    _writer.print("=> ");
    _visitNode(node.expression);
    if (node.semicolon != null) {
      _writer.print(';');
    }
    return null;
  }

  @override
  Object visitExpressionStatement(ExpressionStatement node) {
    _visitNode(node.expression);
    _writer.print(';');
    return null;
  }

  @override
  Object visitExtendsClause(ExtendsClause node) {
    _writer.print("extends ");
    _visitNode(node.superclass);
    return null;
  }

  @override
  Object visitFieldDeclaration(FieldDeclaration node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitTokenWithSuffix(node.staticKeyword, " ");
    _visitNode(node.fields);
    _writer.print(";");
    return null;
  }

  @override
  Object visitFieldFormalParameter(FieldFormalParameter node) {
    _visitTokenWithSuffix(node.keyword, " ");
    _visitNodeWithSuffix(node.type, " ");
    _writer.print("this.");
    _visitNode(node.identifier);
    _visitNode(node.parameters);
    return null;
  }

  @override
  Object visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.loopVariable;
    if (node.awaitKeyword != null) {
      _writer.print("await ");
    }
    _writer.print("for (");
    if (loopVariable == null) {
      _visitNode(node.identifier);
    } else {
      _visitNode(loopVariable);
    }
    _writer.print(" in ");
    _visitNode(node.iterable);
    _writer.print(") ");
    _visitNode(node.body);
    return null;
  }

  @override
  Object visitFormalParameterList(FormalParameterList node) {
    String groupEnd = null;
    _writer.print('(');
    NodeList<FormalParameter> parameters = node.parameters;
    int size = parameters.length;
    for (int i = 0; i < size; i++) {
      FormalParameter parameter = parameters[i];
      if (i > 0) {
        _writer.print(", ");
      }
      if (groupEnd == null && parameter is DefaultFormalParameter) {
        if (parameter.kind == ParameterKind.NAMED) {
          groupEnd = "}";
          _writer.print('{');
        } else {
          groupEnd = "]";
          _writer.print('[');
        }
      }
      parameter.accept(this);
    }
    if (groupEnd != null) {
      _writer.print(groupEnd);
    }
    _writer.print(')');
    return null;
  }

  @override
  Object visitForStatement(ForStatement node) {
    Expression initialization = node.initialization;
    _writer.print("for (");
    if (initialization != null) {
      _visitNode(initialization);
    } else {
      _visitNode(node.variables);
    }
    _writer.print(";");
    _visitNodeWithPrefix(" ", node.condition);
    _writer.print(";");
    _visitNodeListWithSeparatorAndPrefix(" ", node.updaters, ", ");
    _writer.print(") ");
    _visitNode(node.body);
    return null;
  }

  @override
  Object visitFunctionDeclaration(FunctionDeclaration node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitNodeWithSuffix(node.returnType, " ");
    _visitTokenWithSuffix(node.propertyKeyword, " ");
    _visitNode(node.name);
    _visitNode(node.functionExpression);
    return null;
  }

  @override
  Object visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    _visitNode(node.functionDeclaration);
    return null;
  }

  @override
  Object visitFunctionExpression(FunctionExpression node) {
    _visitNode(node.parameters);
    _writer.print(' ');
    _visitNode(node.body);
    return null;
  }

  @override
  Object visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    _visitNode(node.function);
    _visitNode(node.argumentList);
    return null;
  }

  @override
  Object visitFunctionTypeAlias(FunctionTypeAlias node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _writer.print("typedef ");
    _visitNodeWithSuffix(node.returnType, " ");
    _visitNode(node.name);
    _visitNode(node.typeParameters);
    _visitNode(node.parameters);
    _writer.print(";");
    return null;
  }

  @override
  Object visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    _visitNodeWithSuffix(node.returnType, " ");
    _visitNode(node.identifier);
    _visitNode(node.parameters);
    return null;
  }

  @override
  Object visitHideCombinator(HideCombinator node) {
    _writer.print("hide ");
    _visitNodeListWithSeparator(node.hiddenNames, ", ");
    return null;
  }

  @override
  Object visitIfStatement(IfStatement node) {
    _writer.print("if (");
    _visitNode(node.condition);
    _writer.print(") ");
    _visitNode(node.thenStatement);
    _visitNodeWithPrefix(" else ", node.elseStatement);
    return null;
  }

  @override
  Object visitImplementsClause(ImplementsClause node) {
    _writer.print("implements ");
    _visitNodeListWithSeparator(node.interfaces, ", ");
    return null;
  }

  @override
  Object visitImportDirective(ImportDirective node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _writer.print("import ");
    _visitNode(node.uri);
    if (node.deferredToken != null) {
      _writer.print(" deferred");
    }
    _visitNodeWithPrefix(" as ", node.prefix);
    _visitNodeListWithSeparatorAndPrefix(" ", node.combinators, " ");
    _writer.print(';');
    return null;
  }

  @override
  Object visitIndexExpression(IndexExpression node) {
    if (node.isCascaded) {
      _writer.print("..");
    } else {
      _visitNode(node.target);
    }
    _writer.print('[');
    _visitNode(node.index);
    _writer.print(']');
    return null;
  }

  @override
  Object visitInstanceCreationExpression(InstanceCreationExpression node) {
    _visitTokenWithSuffix(node.keyword, " ");
    _visitNode(node.constructorName);
    _visitNode(node.argumentList);
    return null;
  }

  @override
  Object visitIntegerLiteral(IntegerLiteral node) {
    _writer.print(node.literal.lexeme);
    return null;
  }

  @override
  Object visitInterpolationExpression(InterpolationExpression node) {
    if (node.rightBracket != null) {
      _writer.print("\${");
      _visitNode(node.expression);
      _writer.print("}");
    } else {
      _writer.print("\$");
      _visitNode(node.expression);
    }
    return null;
  }

  @override
  Object visitInterpolationString(InterpolationString node) {
    _writer.print(node.contents.lexeme);
    return null;
  }

  @override
  Object visitIsExpression(IsExpression node) {
    _visitNode(node.expression);
    if (node.notOperator == null) {
      _writer.print(" is ");
    } else {
      _writer.print(" is! ");
    }
    _visitNode(node.type);
    return null;
  }

  @override
  Object visitLabel(Label node) {
    _visitNode(node.label);
    _writer.print(":");
    return null;
  }

  @override
  Object visitLabeledStatement(LabeledStatement node) {
    _visitNodeListWithSeparatorAndSuffix(node.labels, " ", " ");
    _visitNode(node.statement);
    return null;
  }

  @override
  Object visitLibraryDirective(LibraryDirective node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _writer.print("library ");
    _visitNode(node.name);
    _writer.print(';');
    return null;
  }

  @override
  Object visitLibraryIdentifier(LibraryIdentifier node) {
    _writer.print(node.name);
    return null;
  }

  @override
  Object visitListLiteral(ListLiteral node) {
    if (node.constKeyword != null) {
      _writer.print(node.constKeyword.lexeme);
      _writer.print(' ');
    }
    _visitNodeWithSuffix(node.typeArguments, " ");
    _writer.print("[");
    _visitNodeListWithSeparator(node.elements, ", ");
    _writer.print("]");
    return null;
  }

  @override
  Object visitMapLiteral(MapLiteral node) {
    if (node.constKeyword != null) {
      _writer.print(node.constKeyword.lexeme);
      _writer.print(' ');
    }
    _visitNodeWithSuffix(node.typeArguments, " ");
    _writer.print("{");
    _visitNodeListWithSeparator(node.entries, ", ");
    _writer.print("}");
    return null;
  }

  @override
  Object visitMapLiteralEntry(MapLiteralEntry node) {
    _visitNode(node.key);
    _writer.print(" : ");
    _visitNode(node.value);
    return null;
  }

  @override
  Object visitMethodDeclaration(MethodDeclaration node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitTokenWithSuffix(node.externalKeyword, " ");
    _visitTokenWithSuffix(node.modifierKeyword, " ");
    _visitNodeWithSuffix(node.returnType, " ");
    _visitTokenWithSuffix(node.propertyKeyword, " ");
    _visitTokenWithSuffix(node.operatorKeyword, " ");
    _visitNode(node.name);
    if (!node.isGetter) {
      _visitNode(node.parameters);
    }
    _visitFunctionWithPrefix(" ", node.body);
    return null;
  }

  @override
  Object visitMethodInvocation(MethodInvocation node) {
    if (node.isCascaded) {
      _writer.print("..");
    } else {
      _visitNodeWithSuffix(node.target, ".");
    }
    _visitNode(node.methodName);
    _visitNode(node.argumentList);
    return null;
  }

  @override
  Object visitNamedExpression(NamedExpression node) {
    _visitNode(node.name);
    _visitNodeWithPrefix(" ", node.expression);
    return null;
  }

  @override
  Object visitNativeClause(NativeClause node) {
    _writer.print("native ");
    _visitNode(node.name);
    return null;
  }

  @override
  Object visitNativeFunctionBody(NativeFunctionBody node) {
    _writer.print("native ");
    _visitNode(node.stringLiteral);
    _writer.print(';');
    return null;
  }

  @override
  Object visitNullLiteral(NullLiteral node) {
    _writer.print("null");
    return null;
  }

  @override
  Object visitParenthesizedExpression(ParenthesizedExpression node) {
    _writer.print('(');
    _visitNode(node.expression);
    _writer.print(')');
    return null;
  }

  @override
  Object visitPartDirective(PartDirective node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _writer.print("part ");
    _visitNode(node.uri);
    _writer.print(';');
    return null;
  }

  @override
  Object visitPartOfDirective(PartOfDirective node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _writer.print("part of ");
    _visitNode(node.libraryName);
    _writer.print(';');
    return null;
  }

  @override
  Object visitPostfixExpression(PostfixExpression node) {
    _visitNode(node.operand);
    _writer.print(node.operator.lexeme);
    return null;
  }

  @override
  Object visitPrefixedIdentifier(PrefixedIdentifier node) {
    _visitNode(node.prefix);
    _writer.print('.');
    _visitNode(node.identifier);
    return null;
  }

  @override
  Object visitPrefixExpression(PrefixExpression node) {
    _writer.print(node.operator.lexeme);
    _visitNode(node.operand);
    return null;
  }

  @override
  Object visitPropertyAccess(PropertyAccess node) {
    if (node.isCascaded) {
      _writer.print("..");
    } else {
      _visitNode(node.target);
      _writer.print('.');
    }
    _visitNode(node.propertyName);
    return null;
  }

  @override
  Object
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    _writer.print("this");
    _visitNodeWithPrefix(".", node.constructorName);
    _visitNode(node.argumentList);
    return null;
  }

  @override
  Object visitRethrowExpression(RethrowExpression node) {
    _writer.print("rethrow");
    return null;
  }

  @override
  Object visitReturnStatement(ReturnStatement node) {
    Expression expression = node.expression;
    if (expression == null) {
      _writer.print("return;");
    } else {
      _writer.print("return ");
      expression.accept(this);
      _writer.print(";");
    }
    return null;
  }

  @override
  Object visitScriptTag(ScriptTag node) {
    _writer.print(node.scriptTag.lexeme);
    return null;
  }

  @override
  Object visitShowCombinator(ShowCombinator node) {
    _writer.print("show ");
    _visitNodeListWithSeparator(node.shownNames, ", ");
    return null;
  }

  @override
  Object visitSimpleFormalParameter(SimpleFormalParameter node) {
    _visitTokenWithSuffix(node.keyword, " ");
    _visitNodeWithSuffix(node.type, " ");
    _visitNode(node.identifier);
    return null;
  }

  @override
  Object visitSimpleIdentifier(SimpleIdentifier node) {
    _writer.print(node.token.lexeme);
    return null;
  }

  @override
  Object visitSimpleStringLiteral(SimpleStringLiteral node) {
    _writer.print(node.literal.lexeme);
    return null;
  }

  @override
  Object visitStringInterpolation(StringInterpolation node) {
    _visitNodeList(node.elements);
    return null;
  }

  @override
  Object visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    _writer.print("super");
    _visitNodeWithPrefix(".", node.constructorName);
    _visitNode(node.argumentList);
    return null;
  }

  @override
  Object visitSuperExpression(SuperExpression node) {
    _writer.print("super");
    return null;
  }

  @override
  Object visitSwitchCase(SwitchCase node) {
    _visitNodeListWithSeparatorAndSuffix(node.labels, " ", " ");
    _writer.print("case ");
    _visitNode(node.expression);
    _writer.print(": ");
    _visitNodeListWithSeparator(node.statements, " ");
    return null;
  }

  @override
  Object visitSwitchDefault(SwitchDefault node) {
    _visitNodeListWithSeparatorAndSuffix(node.labels, " ", " ");
    _writer.print("default: ");
    _visitNodeListWithSeparator(node.statements, " ");
    return null;
  }

  @override
  Object visitSwitchStatement(SwitchStatement node) {
    _writer.print("switch (");
    _visitNode(node.expression);
    _writer.print(") {");
    _visitNodeListWithSeparator(node.members, " ");
    _writer.print("}");
    return null;
  }

  @override
  Object visitSymbolLiteral(SymbolLiteral node) {
    _writer.print("#");
    List<Token> components = node.components;
    for (int i = 0; i < components.length; i++) {
      if (i > 0) {
        _writer.print(".");
      }
      _writer.print(components[i].lexeme);
    }
    return null;
  }

  @override
  Object visitThisExpression(ThisExpression node) {
    _writer.print("this");
    return null;
  }

  @override
  Object visitThrowExpression(ThrowExpression node) {
    _writer.print("throw ");
    _visitNode(node.expression);
    return null;
  }

  @override
  Object visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    _visitNodeWithSuffix(node.variables, ";");
    return null;
  }

  @override
  Object visitTryStatement(TryStatement node) {
    _writer.print("try ");
    _visitNode(node.body);
    _visitNodeListWithSeparatorAndPrefix(" ", node.catchClauses, " ");
    _visitNodeWithPrefix(" finally ", node.finallyBlock);
    return null;
  }

  @override
  Object visitTypeArgumentList(TypeArgumentList node) {
    _writer.print('<');
    _visitNodeListWithSeparator(node.arguments, ", ");
    _writer.print('>');
    return null;
  }

  @override
  Object visitTypeName(TypeName node) {
    _visitNode(node.name);
    _visitNode(node.typeArguments);
    return null;
  }

  @override
  Object visitTypeParameter(TypeParameter node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitNode(node.name);
    _visitNodeWithPrefix(" extends ", node.bound);
    return null;
  }

  @override
  Object visitTypeParameterList(TypeParameterList node) {
    _writer.print('<');
    _visitNodeListWithSeparator(node.typeParameters, ", ");
    _writer.print('>');
    return null;
  }

  @override
  Object visitVariableDeclaration(VariableDeclaration node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitNode(node.name);
    _visitNodeWithPrefix(" = ", node.initializer);
    return null;
  }

  @override
  Object visitVariableDeclarationList(VariableDeclarationList node) {
    _visitNodeListWithSeparatorAndSuffix(node.metadata, " ", " ");
    _visitTokenWithSuffix(node.keyword, " ");
    _visitNodeWithSuffix(node.type, " ");
    _visitNodeListWithSeparator(node.variables, ", ");
    return null;
  }

  @override
  Object visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    _visitNode(node.variables);
    _writer.print(";");
    return null;
  }

  @override
  Object visitWhileStatement(WhileStatement node) {
    _writer.print("while (");
    _visitNode(node.condition);
    _writer.print(") ");
    _visitNode(node.body);
    return null;
  }

  @override
  Object visitWithClause(WithClause node) {
    _writer.print("with ");
    _visitNodeListWithSeparator(node.mixinTypes, ", ");
    return null;
  }

  @override
  Object visitYieldStatement(YieldStatement node) {
    if (node.star != null) {
      _writer.print("yield* ");
    } else {
      _writer.print("yield ");
    }
    _visitNode(node.expression);
    _writer.print(";");
    return null;
  }

  /**
   * Visit the given function body, printing the prefix before if given body is not empty.
   *
   * @param prefix the prefix to be printed if there is a node to visit
   * @param body the function body to be visited
   */
  void _visitFunctionWithPrefix(String prefix, FunctionBody body) {
    if (body is! EmptyFunctionBody) {
      _writer.print(prefix);
    }
    _visitNode(body);
  }

  /**
   * Safely visit the given node.
   *
   * @param node the node to be visited
   */
  void _visitNode(AstNode node) {
    if (node != null) {
      node.accept(this);
    }
  }

  /**
   * Print a list of nodes without any separation.
   *
   * @param nodes the nodes to be printed
   * @param separator the separator to be printed between adjacent nodes
   */
  void _visitNodeList(NodeList<AstNode> nodes) {
    _visitNodeListWithSeparator(nodes, "");
  }

  /**
   * Print a list of nodes, separated by the given separator.
   *
   * @param nodes the nodes to be printed
   * @param separator the separator to be printed between adjacent nodes
   */
  void _visitNodeListWithSeparator(NodeList<AstNode> nodes, String separator) {
    if (nodes != null) {
      int size = nodes.length;
      for (int i = 0; i < size; i++) {
        if (i > 0) {
          _writer.print(separator);
        }
        nodes[i].accept(this);
      }
    }
  }

  /**
   * Print a list of nodes, separated by the given separator.
   *
   * @param prefix the prefix to be printed if the list is not empty
   * @param nodes the nodes to be printed
   * @param separator the separator to be printed between adjacent nodes
   */
  void _visitNodeListWithSeparatorAndPrefix(String prefix,
      NodeList<AstNode> nodes, String separator) {
    if (nodes != null) {
      int size = nodes.length;
      if (size > 0) {
        _writer.print(prefix);
        for (int i = 0; i < size; i++) {
          if (i > 0) {
            _writer.print(separator);
          }
          nodes[i].accept(this);
        }
      }
    }
  }

  /**
   * Print a list of nodes, separated by the given separator.
   *
   * @param nodes the nodes to be printed
   * @param separator the separator to be printed between adjacent nodes
   * @param suffix the suffix to be printed if the list is not empty
   */
  void _visitNodeListWithSeparatorAndSuffix(NodeList<AstNode> nodes,
      String separator, String suffix) {
    if (nodes != null) {
      int size = nodes.length;
      if (size > 0) {
        for (int i = 0; i < size; i++) {
          if (i > 0) {
            _writer.print(separator);
          }
          nodes[i].accept(this);
        }
        _writer.print(suffix);
      }
    }
  }

  /**
   * Safely visit the given node, printing the prefix before the node if it is non-`null`.
   *
   * @param prefix the prefix to be printed if there is a node to visit
   * @param node the node to be visited
   */
  void _visitNodeWithPrefix(String prefix, AstNode node) {
    if (node != null) {
      _writer.print(prefix);
      node.accept(this);
    }
  }

  /**
   * Safely visit the given node, printing the suffix after the node if it is non-`null`.
   *
   * @param suffix the suffix to be printed if there is a node to visit
   * @param node the node to be visited
   */
  void _visitNodeWithSuffix(AstNode node, String suffix) {
    if (node != null) {
      node.accept(this);
      _writer.print(suffix);
    }
  }

  /**
   * Safely visit the given node, printing the suffix after the node if it is non-`null`.
   *
   * @param suffix the suffix to be printed if there is a node to visit
   * @param node the node to be visited
   */
  void _visitTokenWithSuffix(Token token, String suffix) {
    if (token != null) {
      _writer.print(token.lexeme);
      _writer.print(suffix);
    }
  }
}

/**
 * Instances of the class `TryStatement` represent a try statement.
 *
 * <pre>
 * tryStatement ::=
 *     'try' [Block] ([CatchClause]+ finallyClause? | finallyClause)
 *
 * finallyClause ::=
 *     'finally' [Block]
 * </pre>
 */
class TryStatement extends Statement {
  /**
   * The token representing the 'try' keyword.
   */
  Token tryKeyword;

  /**
   * The body of the statement.
   */
  Block _body;

  /**
   * The catch clauses contained in the try statement.
   */
  NodeList<CatchClause> _catchClauses;

  /**
   * The token representing the 'finally' keyword, or `null` if the statement does not contain
   * a finally clause.
   */
  Token finallyKeyword;

  /**
   * The finally block contained in the try statement, or `null` if the statement does not
   * contain a finally clause.
   */
  Block _finallyBlock;

  /**
   * Initialize a newly created try statement.
   *
   * @param tryKeyword the token representing the 'try' keyword
   * @param body the body of the statement
   * @param catchClauses the catch clauses contained in the try statement
   * @param finallyKeyword the token representing the 'finally' keyword
   * @param finallyBlock the finally block contained in the try statement
   */
  TryStatement(this.tryKeyword, Block body, List<CatchClause> catchClauses,
      this.finallyKeyword, Block finallyBlock) {
    _body = becomeParentOf(body);
    _catchClauses = new NodeList<CatchClause>(this, catchClauses);
    _finallyBlock = becomeParentOf(finallyBlock);
  }

  @override
  Token get beginToken => tryKeyword;

  /**
   * Return the body of the statement.
   *
   * @return the body of the statement
   */
  Block get body => _body;

  /**
   * Set the body of the statement to the given block.
   *
   * @param block the body of the statement
   */
  void set body(Block block) {
    _body = becomeParentOf(block);
  }

  /**
   * Return the catch clauses contained in the try statement.
   *
   * @return the catch clauses contained in the try statement
   */
  NodeList<CatchClause> get catchClauses => _catchClauses;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(tryKeyword)
      ..add(_body)
      ..addAll(_catchClauses)
      ..add(finallyKeyword)
      ..add(_finallyBlock);

  @override
  Token get endToken {
    if (_finallyBlock != null) {
      return _finallyBlock.endToken;
    } else if (finallyKeyword != null) {
      return finallyKeyword;
    } else if (!_catchClauses.isEmpty) {
      return _catchClauses.endToken;
    }
    return _body.endToken;
  }

  /**
   * Return the finally block contained in the try statement, or `null` if the statement does
   * not contain a finally clause.
   *
   * @return the finally block contained in the try statement
   */
  Block get finallyBlock => _finallyBlock;

  /**
   * Set the finally block contained in the try statement to the given block.
   *
   * @param block the finally block contained in the try statement
   */
  void set finallyBlock(Block block) {
    _finallyBlock = becomeParentOf(block);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitTryStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_body, visitor);
    _catchClauses.accept(visitor);
    safelyVisitChild(_finallyBlock, visitor);
  }
}

/**
 * The abstract class `TypeAlias` defines the behavior common to declarations of type aliases.
 *
 * <pre>
 * typeAlias ::=
 *     'typedef' typeAliasBody
 *
 * typeAliasBody ::=
 *     classTypeAlias
 *   | functionTypeAlias
 * </pre>
 */
abstract class TypeAlias extends CompilationUnitMember {
  /**
   * The token representing the 'typedef' keyword.
   */
  Token keyword;

  /**
   * The semicolon terminating the declaration.
   */
  Token semicolon;

  /**
   * Initialize a newly created type alias.
   *
   * @param comment the documentation comment associated with this type alias
   * @param metadata the annotations associated with this type alias
   * @param keyword the token representing the 'typedef' keyword
   * @param semicolon the semicolon terminating the declaration
   */
  TypeAlias(Comment comment, List<Annotation> metadata, this.keyword,
      this.semicolon)
      : super(comment, metadata);

  @override
  Token get endToken => semicolon;

  @override
  Token get firstTokenAfterCommentAndMetadata => keyword;
}

/**
 * Instances of the class `TypeArgumentList` represent a list of type arguments.
 *
 * <pre>
 * typeArguments ::=
 *     '<' typeName (',' typeName)* '>'
 * </pre>
 */
class TypeArgumentList extends AstNode {
  /**
   * The left bracket.
   */
  Token leftBracket;

  /**
   * The type arguments associated with the type.
   */
  NodeList<TypeName> _arguments;

  /**
   * The right bracket.
   */
  Token rightBracket;

  /**
   * Initialize a newly created list of type arguments.
   *
   * @param leftBracket the left bracket
   * @param arguments the type arguments associated with the type
   * @param rightBracket the right bracket
   */
  TypeArgumentList(this.leftBracket, List<TypeName> arguments,
      this.rightBracket) {
    _arguments = new NodeList<TypeName>(this, arguments);
  }

  /**
   * Return the type arguments associated with the type.
   *
   * @return the type arguments associated with the type
   */
  NodeList<TypeName> get arguments => _arguments;

  @override
  Token get beginToken => leftBracket;

  /**
   * TODO(paulberry): Add commas.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(leftBracket)
      ..addAll(_arguments)
      ..add(rightBracket);

  @override
  Token get endToken => rightBracket;

  @override
  accept(AstVisitor visitor) => visitor.visitTypeArgumentList(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _arguments.accept(visitor);
  }
}

/**
 * The abstract class `TypedLiteral` defines the behavior common to literals that have a type
 * associated with them.
 *
 * <pre>
 * listLiteral ::=
 *     [ListLiteral]
 *   | [MapLiteral]
 * </pre>
 */
abstract class TypedLiteral extends Literal {
  /**
   * The token representing the 'const' keyword, or `null` if the literal is not a constant.
   */
  Token constKeyword;

  /**
   * The type argument associated with this literal, or `null` if no type arguments were
   * declared.
   */
  TypeArgumentList _typeArguments;

  /**
   * Initialize a newly created typed literal.
   *
   * @param constKeyword the token representing the 'const' keyword
   * @param typeArguments the type argument associated with this literal, or `null` if no type
   *          arguments were declared
   */
  TypedLiteral(this.constKeyword, TypeArgumentList typeArguments) {
    _typeArguments = becomeParentOf(typeArguments);
  }

  /**
   * Return the type argument associated with this literal, or `null` if no type arguments
   * were declared.
   *
   * @return the type argument associated with this literal
   */
  TypeArgumentList get typeArguments => _typeArguments;

  /**
   * Set the type argument associated with this literal to the given arguments.
   *
   * @param typeArguments the type argument associated with this literal
   */
  void set typeArguments(TypeArgumentList typeArguments) {
    _typeArguments = becomeParentOf(typeArguments);
  }

  ChildEntities get _childEntities => new ChildEntities()
      ..add(constKeyword)
      ..add(_typeArguments);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_typeArguments, visitor);
  }
}

/**
 * Instances of the class `TypeName` represent the name of a type, which can optionally
 * include type arguments.
 *
 * <pre>
 * typeName ::=
 *     [Identifier] typeArguments?
 * </pre>
 */
class TypeName extends AstNode {
  /**
   * The name of the type.
   */
  Identifier _name;

  /**
   * The type arguments associated with the type, or `null` if there are no type arguments.
   */
  TypeArgumentList _typeArguments;

  /**
   * The type being named, or `null` if the AST structure has not been resolved.
   */
  DartType type;

  /**
   * Initialize a newly created type name.
   *
   * @param name the name of the type
   * @param typeArguments the type arguments associated with the type, or `null` if there are
   *          no type arguments
   */
  TypeName(Identifier name, TypeArgumentList typeArguments) {
    _name = becomeParentOf(name);
    _typeArguments = becomeParentOf(typeArguments);
  }

  @override
  Token get beginToken => _name.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_name)
      ..add(_typeArguments);

  @override
  Token get endToken {
    if (_typeArguments != null) {
      return _typeArguments.endToken;
    }
    return _name.endToken;
  }

  /**
   * Return `true` if this type is a deferred type.
   *
   * 15.1 Static Types: A type <i>T</i> is deferred iff it is of the form </i>p.T</i> where <i>p</i>
   * is a deferred prefix.
   *
   * @return `true` if this type is a deferred type
   */
  bool get isDeferred {
    Identifier identifier = name;
    if (identifier is! PrefixedIdentifier) {
      return false;
    }
    return (identifier as PrefixedIdentifier).isDeferred;
  }

  @override
  bool get isSynthetic => _name.isSynthetic && _typeArguments == null;

  /**
   * Return the name of the type.
   *
   * @return the name of the type
   */
  Identifier get name => _name;

  /**
   * Set the name of the type to the given identifier.
   *
   * @param identifier the name of the type
   */
  void set name(Identifier identifier) {
    _name = becomeParentOf(identifier);
  }

  /**
   * Return the type arguments associated with the type, or `null` if there are no type
   * arguments.
   *
   * @return the type arguments associated with the type
   */
  TypeArgumentList get typeArguments => _typeArguments;

  /**
   * Set the type arguments associated with the type to the given type arguments.
   *
   * @param typeArguments the type arguments associated with the type
   */
  void set typeArguments(TypeArgumentList typeArguments) {
    _typeArguments = becomeParentOf(typeArguments);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitTypeName(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_typeArguments, visitor);
  }
}

/**
 * Instances of the class `TypeParameter` represent a type parameter.
 *
 * <pre>
 * typeParameter ::=
 *     [SimpleIdentifier] ('extends' [TypeName])?
 * </pre>
 */
class TypeParameter extends Declaration {
  /**
   * The name of the type parameter.
   */
  SimpleIdentifier _name;

  /**
   * The token representing the 'extends' keyword, or `null` if there was no explicit upper
   * bound.
   */
  Token keyword;

  /**
   * The name of the upper bound for legal arguments, or `null` if there was no explicit upper
   * bound.
   */
  TypeName _bound;

  /**
   * Initialize a newly created type parameter.
   *
   * @param comment the documentation comment associated with the type parameter
   * @param metadata the annotations associated with the type parameter
   * @param name the name of the type parameter
   * @param keyword the token representing the 'extends' keyword
   * @param bound the name of the upper bound for legal arguments
   */
  TypeParameter(Comment comment, List<Annotation> metadata,
      SimpleIdentifier name, this.keyword, TypeName bound)
      : super(comment, metadata) {
    _name = becomeParentOf(name);
    _bound = becomeParentOf(bound);
  }

  /**
   * Return the name of the upper bound for legal arguments, or `null` if there was no
   * explicit upper bound.
   *
   * @return the name of the upper bound for legal arguments
   */
  TypeName get bound => _bound;

  /**
   * Set the name of the upper bound for legal arguments to the given type name.
   *
   * @param typeName the name of the upper bound for legal arguments
   */
  void set bound(TypeName typeName) {
    _bound = becomeParentOf(typeName);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(_name)
      ..add(keyword)
      ..add(_bound);

  @override
  TypeParameterElement get element =>
      _name != null ? (_name.staticElement as TypeParameterElement) : null;

  @override
  Token get endToken {
    if (_bound == null) {
      return _name.endToken;
    }
    return _bound.endToken;
  }

  @override
  Token get firstTokenAfterCommentAndMetadata => _name.beginToken;

  /**
   * Return the name of the type parameter.
   *
   * @return the name of the type parameter
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the type parameter to the given identifier.
   *
   * @param identifier the name of the type parameter
   */
  void set name(SimpleIdentifier identifier) {
    _name = becomeParentOf(identifier);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitTypeParameter(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_bound, visitor);
  }
}

/**
 * Instances of the class `TypeParameterList` represent type parameters within a declaration.
 *
 * <pre>
 * typeParameterList ::=
 *     '<' [TypeParameter] (',' [TypeParameter])* '>'
 * </pre>
 */
class TypeParameterList extends AstNode {
  /**
   * The left angle bracket.
   */
  final Token leftBracket;

  /**
   * The type parameters in the list.
   */
  NodeList<TypeParameter> _typeParameters;

  /**
   * The right angle bracket.
   */
  final Token rightBracket;

  /**
   * Initialize a newly created list of type parameters.
   *
   * @param leftBracket the left angle bracket
   * @param typeParameters the type parameters in the list
   * @param rightBracket the right angle bracket
   */
  TypeParameterList(this.leftBracket, List<TypeParameter> typeParameters,
      this.rightBracket) {
    _typeParameters = new NodeList<TypeParameter>(this, typeParameters);
  }

  @override
  Token get beginToken => leftBracket;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(leftBracket)
      ..addAll(_typeParameters)
      ..add(rightBracket);

  @override
  Token get endToken => rightBracket;

  /**
   * Return the type parameters for the type.
   *
   * @return the type parameters for the type
   */
  NodeList<TypeParameter> get typeParameters => _typeParameters;

  @override
  accept(AstVisitor visitor) => visitor.visitTypeParameterList(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _typeParameters.accept(visitor);
  }
}

/**
 * Instances of the class `UnifyingAstVisitor` implement an AST visitor that will recursively
 * visit all of the nodes in an AST structure (like instances of the class
 * [RecursiveAstVisitor]). In addition, every node will also be visited by using a single
 * unified [visitNode] method.
 *
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general [visitNode] method. Failure to do so will
 * cause the children of the visited node to not be visited.
 */
class UnifyingAstVisitor<R> implements AstVisitor<R> {
  @override
  R visitAdjacentStrings(AdjacentStrings node) => visitNode(node);

  @override
  R visitAnnotation(Annotation node) => visitNode(node);

  @override
  R visitArgumentList(ArgumentList node) => visitNode(node);

  @override
  R visitAsExpression(AsExpression node) => visitNode(node);

  @override
  R visitAssertStatement(AssertStatement node) => visitNode(node);

  @override
  R visitAssignmentExpression(AssignmentExpression node) => visitNode(node);

  @override
  R visitAwaitExpression(AwaitExpression node) => visitNode(node);

  @override
  R visitBinaryExpression(BinaryExpression node) => visitNode(node);

  @override
  R visitBlock(Block node) => visitNode(node);

  @override
  R visitBlockFunctionBody(BlockFunctionBody node) => visitNode(node);

  @override
  R visitBooleanLiteral(BooleanLiteral node) => visitNode(node);

  @override
  R visitBreakStatement(BreakStatement node) => visitNode(node);

  @override
  R visitCascadeExpression(CascadeExpression node) => visitNode(node);

  @override
  R visitCatchClause(CatchClause node) => visitNode(node);

  @override
  R visitClassDeclaration(ClassDeclaration node) => visitNode(node);

  @override
  R visitClassTypeAlias(ClassTypeAlias node) => visitNode(node);

  @override
  R visitComment(Comment node) => visitNode(node);

  @override
  R visitCommentReference(CommentReference node) => visitNode(node);

  @override
  R visitCompilationUnit(CompilationUnit node) => visitNode(node);

  @override
  R visitConditionalExpression(ConditionalExpression node) => visitNode(node);

  @override
  R visitConstructorDeclaration(ConstructorDeclaration node) => visitNode(node);

  @override
  R visitConstructorFieldInitializer(ConstructorFieldInitializer node) =>
      visitNode(node);

  @override
  R visitConstructorName(ConstructorName node) => visitNode(node);

  @override
  R visitContinueStatement(ContinueStatement node) => visitNode(node);

  @override
  R visitDeclaredIdentifier(DeclaredIdentifier node) => visitNode(node);

  @override
  R visitDefaultFormalParameter(DefaultFormalParameter node) => visitNode(node);

  @override
  R visitDoStatement(DoStatement node) => visitNode(node);

  @override
  R visitDoubleLiteral(DoubleLiteral node) => visitNode(node);

  @override
  R visitEmptyFunctionBody(EmptyFunctionBody node) => visitNode(node);

  @override
  R visitEmptyStatement(EmptyStatement node) => visitNode(node);

  @override
  R visitEnumConstantDeclaration(EnumConstantDeclaration node) =>
      visitNode(node);

  @override
  R visitEnumDeclaration(EnumDeclaration node) => visitNode(node);

  @override
  R visitExportDirective(ExportDirective node) => visitNode(node);

  @override
  R visitExpressionFunctionBody(ExpressionFunctionBody node) => visitNode(node);

  @override
  R visitExpressionStatement(ExpressionStatement node) => visitNode(node);

  @override
  R visitExtendsClause(ExtendsClause node) => visitNode(node);

  @override
  R visitFieldDeclaration(FieldDeclaration node) => visitNode(node);

  @override
  R visitFieldFormalParameter(FieldFormalParameter node) => visitNode(node);

  @override
  R visitForEachStatement(ForEachStatement node) => visitNode(node);

  @override
  R visitFormalParameterList(FormalParameterList node) => visitNode(node);

  @override
  R visitForStatement(ForStatement node) => visitNode(node);

  @override
  R visitFunctionDeclaration(FunctionDeclaration node) => visitNode(node);

  @override
  R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) =>
      visitNode(node);

  @override
  R visitFunctionExpression(FunctionExpression node) => visitNode(node);

  @override
  R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) =>
      visitNode(node);

  @override
  R visitFunctionTypeAlias(FunctionTypeAlias node) => visitNode(node);

  @override
  R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) =>
      visitNode(node);

  @override
  R visitHideCombinator(HideCombinator node) => visitNode(node);

  @override
  R visitIfStatement(IfStatement node) => visitNode(node);

  @override
  R visitImplementsClause(ImplementsClause node) => visitNode(node);

  @override
  R visitImportDirective(ImportDirective node) => visitNode(node);

  @override
  R visitIndexExpression(IndexExpression node) => visitNode(node);

  @override
  R visitInstanceCreationExpression(InstanceCreationExpression node) =>
      visitNode(node);

  @override
  R visitIntegerLiteral(IntegerLiteral node) => visitNode(node);

  @override
  R visitInterpolationExpression(InterpolationExpression node) =>
      visitNode(node);

  @override
  R visitInterpolationString(InterpolationString node) => visitNode(node);

  @override
  R visitIsExpression(IsExpression node) => visitNode(node);

  @override
  R visitLabel(Label node) => visitNode(node);

  @override
  R visitLabeledStatement(LabeledStatement node) => visitNode(node);

  @override
  R visitLibraryDirective(LibraryDirective node) => visitNode(node);

  @override
  R visitLibraryIdentifier(LibraryIdentifier node) => visitNode(node);

  @override
  R visitListLiteral(ListLiteral node) => visitNode(node);

  @override
  R visitMapLiteral(MapLiteral node) => visitNode(node);

  @override
  R visitMapLiteralEntry(MapLiteralEntry node) => visitNode(node);

  @override
  R visitMethodDeclaration(MethodDeclaration node) => visitNode(node);

  @override
  R visitMethodInvocation(MethodInvocation node) => visitNode(node);

  @override
  R visitNamedExpression(NamedExpression node) => visitNode(node);

  @override
  R visitNativeClause(NativeClause node) => visitNode(node);

  @override
  R visitNativeFunctionBody(NativeFunctionBody node) => visitNode(node);

  R visitNode(AstNode node) {
    node.visitChildren(this);
    return null;
  }

  @override
  R visitNullLiteral(NullLiteral node) => visitNode(node);

  @override
  R visitParenthesizedExpression(ParenthesizedExpression node) =>
      visitNode(node);

  @override
  R visitPartDirective(PartDirective node) => visitNode(node);

  @override
  R visitPartOfDirective(PartOfDirective node) => visitNode(node);

  @override
  R visitPostfixExpression(PostfixExpression node) => visitNode(node);

  @override
  R visitPrefixedIdentifier(PrefixedIdentifier node) => visitNode(node);

  @override
  R visitPrefixExpression(PrefixExpression node) => visitNode(node);

  @override
  R visitPropertyAccess(PropertyAccess node) => visitNode(node);

  @override
  R
      visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) =>
      visitNode(node);

  @override
  R visitRethrowExpression(RethrowExpression node) => visitNode(node);

  @override
  R visitReturnStatement(ReturnStatement node) => visitNode(node);

  @override
  R visitScriptTag(ScriptTag scriptTag) => visitNode(scriptTag);

  @override
  R visitShowCombinator(ShowCombinator node) => visitNode(node);

  @override
  R visitSimpleFormalParameter(SimpleFormalParameter node) => visitNode(node);

  @override
  R visitSimpleIdentifier(SimpleIdentifier node) => visitNode(node);

  @override
  R visitSimpleStringLiteral(SimpleStringLiteral node) => visitNode(node);

  @override
  R visitStringInterpolation(StringInterpolation node) => visitNode(node);

  @override
  R visitSuperConstructorInvocation(SuperConstructorInvocation node) =>
      visitNode(node);

  @override
  R visitSuperExpression(SuperExpression node) => visitNode(node);

  @override
  R visitSwitchCase(SwitchCase node) => visitNode(node);

  @override
  R visitSwitchDefault(SwitchDefault node) => visitNode(node);

  @override
  R visitSwitchStatement(SwitchStatement node) => visitNode(node);

  @override
  R visitSymbolLiteral(SymbolLiteral node) => visitNode(node);

  @override
  R visitThisExpression(ThisExpression node) => visitNode(node);

  @override
  R visitThrowExpression(ThrowExpression node) => visitNode(node);

  @override
  R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) =>
      visitNode(node);

  @override
  R visitTryStatement(TryStatement node) => visitNode(node);

  @override
  R visitTypeArgumentList(TypeArgumentList node) => visitNode(node);

  @override
  R visitTypeName(TypeName node) => visitNode(node);

  @override
  R visitTypeParameter(TypeParameter node) => visitNode(node);

  @override
  R visitTypeParameterList(TypeParameterList node) => visitNode(node);

  @override
  R visitVariableDeclaration(VariableDeclaration node) => visitNode(node);

  @override
  R visitVariableDeclarationList(VariableDeclarationList node) =>
      visitNode(node);

  @override
  R visitVariableDeclarationStatement(VariableDeclarationStatement node) =>
      visitNode(node);

  @override
  R visitWhileStatement(WhileStatement node) => visitNode(node);

  @override
  R visitWithClause(WithClause node) => visitNode(node);

  @override
  R visitYieldStatement(YieldStatement node) => visitNode(node);
}

/**
 * The abstract class `UriBasedDirective` defines the behavior common to nodes that represent
 * a directive that references a URI.
 *
 * <pre>
 * uriBasedDirective ::=
 *     [ExportDirective]
 *   | [ImportDirective]
 *   | [PartDirective]
 * </pre>
 */
abstract class UriBasedDirective extends Directive {
  /**
   * The prefix of a URI using the `dart-ext` scheme to reference a native code library.
   */
  static String _DART_EXT_SCHEME = "dart-ext:";

  /**
   * The URI referenced by this directive.
   */
  StringLiteral _uri;

  /**
   * The content of the URI.
   */
  String uriContent;

  /**
   * The source to which the URI was resolved.
   */
  Source source;

  /**
   * Initialize a newly create URI-based directive.
   *
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param uri the URI referenced by this directive
   */
  UriBasedDirective(Comment comment, List<Annotation> metadata,
      StringLiteral uri)
      : super(comment, metadata) {
    _uri = becomeParentOf(uri);
  }

  /**
   * Return the URI referenced by this directive.
   *
   * @return the URI referenced by this directive
   */
  StringLiteral get uri => _uri;

  /**
   * Set the URI referenced by this directive to the given URI.
   *
   * @param uri the URI referenced by this directive
   */
  void set uri(StringLiteral uri) {
    _uri = becomeParentOf(uri);
  }

  /**
   * Return the element associated with the URI of this directive, or `null` if the AST
   * structure has not been resolved or if the URI could not be resolved. Examples of the latter
   * case include a directive that contains an invalid URL or a URL that does not exist.
   *
   * @return the element associated with this directive
   */
  Element get uriElement;

  /**
   * Validate the given directive, but do not check for existence.
   *
   * @return a code indicating the problem if there is one, or `null` no problem
   */
  UriValidationCode validate() {
    StringLiteral uriLiteral = uri;
    if (uriLiteral is StringInterpolation) {
      return UriValidationCode.URI_WITH_INTERPOLATION;
    }
    String uriContent = this.uriContent;
    if (uriContent == null) {
      return UriValidationCode.INVALID_URI;
    }
    if (this is ImportDirective && uriContent.startsWith(_DART_EXT_SCHEME)) {
      return UriValidationCode.URI_WITH_DART_EXT_SCHEME;
    }
    try {
      parseUriWithException(Uri.encodeFull(uriContent));
    } on URISyntaxException catch (exception) {
      return UriValidationCode.INVALID_URI;
    }
    return null;
  }

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_uri, visitor);
  }
}

/**
 * Validation codes returned by [UriBasedDirective.validate].
 */
class UriValidationCode {
  static const UriValidationCode INVALID_URI =
      const UriValidationCode('INVALID_URI');

  static const UriValidationCode URI_WITH_INTERPOLATION =
      const UriValidationCode('URI_WITH_INTERPOLATION');

  static const UriValidationCode URI_WITH_DART_EXT_SCHEME =
      const UriValidationCode('URI_WITH_DART_EXT_SCHEME');

  /**
   * The name of the validation code.
   */
  final String name;

  /**
   * Initialize a newly created validation code to have the given [name].
   */
  const UriValidationCode(this.name);

  @override
  String toString() => name;
}

/**
 * Instances of the class `VariableDeclaration` represent an identifier that has an initial
 * value associated with it. Instances of this class are always children of the class
 * [VariableDeclarationList].
 *
 * <pre>
 * variableDeclaration ::=
 *     [SimpleIdentifier] ('=' [Expression])?
 * </pre>
 */
class VariableDeclaration extends Declaration {
  /**
   * The name of the variable being declared.
   */
  SimpleIdentifier _name;

  /**
   * The equal sign separating the variable name from the initial value, or `null` if the
   * initial value was not specified.
   */
  Token equals;

  /**
   * The expression used to compute the initial value for the variable, or `null` if the
   * initial value was not specified.
   */
  Expression _initializer;

  /**
   * Initialize a newly created variable declaration.
   *
   * @param comment the documentation comment associated with this declaration
   * @param metadata the annotations associated with this member
   * @param name the name of the variable being declared
   * @param equals the equal sign separating the variable name from the initial value
   * @param initializer the expression used to compute the initial value for the variable
   */
  VariableDeclaration(Comment comment, List<Annotation> metadata,
      SimpleIdentifier name, this.equals, Expression initializer)
      : super(comment, metadata) {
    _name = becomeParentOf(name);
    _initializer = becomeParentOf(initializer);
  }

  @override
  Iterable get childEntities => super._childEntities
      ..add(_name)
      ..add(equals)
      ..add(_initializer);

  /**
   * This overridden implementation of getDocumentationComment() looks in the grandparent node for
   * dartdoc comments if no documentation is specifically available on the node.
   */
  @override
  Comment get documentationComment {
    Comment comment = super.documentationComment;
    if (comment == null) {
      if (parent != null && parent.parent != null) {
        AstNode node = parent.parent;
        if (node is AnnotatedNode) {
          return node.documentationComment;
        }
      }
    }
    return comment;
  }

  @override
  VariableElement get element =>
      _name != null ? (_name.staticElement as VariableElement) : null;

  @override
  Token get endToken {
    if (_initializer != null) {
      return _initializer.endToken;
    }
    return _name.endToken;
  }

  @override
  Token get firstTokenAfterCommentAndMetadata => _name.beginToken;

  /**
   * Return the expression used to compute the initial value for the variable, or `null` if
   * the initial value was not specified.
   *
   * @return the expression used to compute the initial value for the variable
   */
  Expression get initializer => _initializer;

  /**
   * Set the expression used to compute the initial value for the variable to the given expression.
   *
   * @param initializer the expression used to compute the initial value for the variable
   */
  void set initializer(Expression initializer) {
    _initializer = becomeParentOf(initializer);
  }

  /**
   * Return `true` if this variable was declared with the 'const' modifier.
   *
   * @return `true` if this variable was declared with the 'const' modifier
   */
  bool get isConst {
    AstNode parent = this.parent;
    return parent is VariableDeclarationList && parent.isConst;
  }

  /**
   * Return `true` if this variable was declared with the 'final' modifier. Variables that are
   * declared with the 'const' modifier will return `false` even though they are implicitly
   * final.
   *
   * @return `true` if this variable was declared with the 'final' modifier
   */
  bool get isFinal {
    AstNode parent = this.parent;
    return parent is VariableDeclarationList && parent.isFinal;
  }

  /**
   * Return the name of the variable being declared.
   *
   * @return the name of the variable being declared
   */
  SimpleIdentifier get name => _name;

  /**
   * Set the name of the variable being declared to the given identifier.
   *
   * @param name the name of the variable being declared
   */
  void set name(SimpleIdentifier name) {
    _name = becomeParentOf(name);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitVariableDeclaration(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_name, visitor);
    safelyVisitChild(_initializer, visitor);
  }
}

/**
 * Instances of the class `VariableDeclarationList` represent the declaration of one or more
 * variables of the same type.
 *
 * <pre>
 * variableDeclarationList ::=
 *     finalConstVarOrType [VariableDeclaration] (',' [VariableDeclaration])*
 *
 * finalConstVarOrType ::=
 *   | 'final' [TypeName]?
 *   | 'const' [TypeName]?
 *   | 'var'
 *   | [TypeName]
 * </pre>
 */
class VariableDeclarationList extends AnnotatedNode {
  /**
   * The token representing the 'final', 'const' or 'var' keyword, or `null` if no keyword was
   * included.
   */
  Token keyword;

  /**
   * The type of the variables being declared, or `null` if no type was provided.
   */
  TypeName _type;

  /**
   * A list containing the individual variables being declared.
   */
  NodeList<VariableDeclaration> _variables;

  /**
   * Initialize a newly created variable declaration list.
   *
   * @param comment the documentation comment associated with this declaration list
   * @param metadata the annotations associated with this declaration list
   * @param keyword the token representing the 'final', 'const' or 'var' keyword
   * @param type the type of the variables being declared
   * @param variables a list containing the individual variables being declared
   */
  VariableDeclarationList(Comment comment, List<Annotation> metadata,
      this.keyword, TypeName type, List<VariableDeclaration> variables)
      : super(comment, metadata) {
    _type = becomeParentOf(type);
    _variables = new NodeList<VariableDeclaration>(this, variables);
  }

  /**
   * TODO(paulberry): include commas.
   */
  @override
  Iterable get childEntities => super._childEntities
      ..add(keyword)
      ..add(_type)
      ..addAll(_variables);

  @override
  Token get endToken => _variables.endToken;

  @override
  Token get firstTokenAfterCommentAndMetadata {
    if (keyword != null) {
      return keyword;
    } else if (_type != null) {
      return _type.beginToken;
    }
    return _variables.beginToken;
  }

  /**
   * Return `true` if the variables in this list were declared with the 'const' modifier.
   *
   * @return `true` if the variables in this list were declared with the 'const' modifier
   */
  bool get isConst =>
      keyword is KeywordToken && (keyword as KeywordToken).keyword == Keyword.CONST;

  /**
   * Return `true` if the variables in this list were declared with the 'final' modifier.
   * Variables that are declared with the 'const' modifier will return `false` even though
   * they are implicitly final.
   *
   * @return `true` if the variables in this list were declared with the 'final' modifier
   */
  bool get isFinal =>
      keyword is KeywordToken && (keyword as KeywordToken).keyword == Keyword.FINAL;

  /**
   * Return the type of the variables being declared, or `null` if no type was provided.
   *
   * @return the type of the variables being declared
   */
  TypeName get type => _type;

  /**
   * Set the type of the variables being declared to the given type name.
   *
   * @param typeName the type of the variables being declared
   */
  void set type(TypeName typeName) {
    _type = becomeParentOf(typeName);
  }

  /**
   * Return a list containing the individual variables being declared.
   *
   * @return a list containing the individual variables being declared
   */
  NodeList<VariableDeclaration> get variables => _variables;

  @override
  accept(AstVisitor visitor) => visitor.visitVariableDeclarationList(this);

  @override
  void visitChildren(AstVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_type, visitor);
    _variables.accept(visitor);
  }
}

/**
 * Instances of the class `VariableDeclarationStatement` represent a list of variables that
 * are being declared in a context where a statement is required.
 *
 * <pre>
 * variableDeclarationStatement ::=
 *     [VariableDeclarationList] ';'
 * </pre>
 */
class VariableDeclarationStatement extends Statement {
  /**
   * The variables being declared.
   */
  VariableDeclarationList _variableList;

  /**
   * The semicolon terminating the statement.
   */
  Token semicolon;

  /**
   * Initialize a newly created variable declaration statement.
   *
   * @param variableList the fields being declared
   * @param semicolon the semicolon terminating the statement
   */
  VariableDeclarationStatement(VariableDeclarationList variableList,
      this.semicolon) {
    _variableList = becomeParentOf(variableList);
  }

  @override
  Token get beginToken => _variableList.beginToken;

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(_variableList)
      ..add(semicolon);

  @override
  Token get endToken => semicolon;

  /**
   * Return the variables being declared.
   *
   * @return the variables being declared
   */
  VariableDeclarationList get variables => _variableList;

  /**
   * Set the variables being declared to the given list of variables.
   *
   * @param variableList the variables being declared
   */
  void set variables(VariableDeclarationList variableList) {
    _variableList = becomeParentOf(variableList);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitVariableDeclarationStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_variableList, visitor);
  }
}

/**
 * Instances of the class `WhileStatement` represent a while statement.
 *
 * <pre>
 * whileStatement ::=
 *     'while' '(' [Expression] ')' [Statement]
 * </pre>
 */
class WhileStatement extends Statement {
  /**
   * The token representing the 'while' keyword.
   */
  Token keyword;

  /**
   * The left parenthesis.
   */
  Token leftParenthesis;

  /**
   * The expression used to determine whether to execute the body of the loop.
   */
  Expression _condition;

  /**
   * The right parenthesis.
   */
  Token rightParenthesis;

  /**
   * The body of the loop.
   */
  Statement _body;

  /**
   * Initialize a newly created while statement.
   *
   * @param keyword the token representing the 'while' keyword
   * @param leftParenthesis the left parenthesis
   * @param condition the expression used to determine whether to execute the body of the loop
   * @param rightParenthesis the right parenthesis
   * @param body the body of the loop
   */
  WhileStatement(this.keyword, this.leftParenthesis, Expression condition,
      this.rightParenthesis, Statement body) {
    _condition = becomeParentOf(condition);
    _body = becomeParentOf(body);
  }

  @override
  Token get beginToken => keyword;

  /**
   * Return the body of the loop.
   *
   * @return the body of the loop
   */
  Statement get body => _body;

  /**
   * Set the body of the loop to the given statement.
   *
   * @param statement the body of the loop
   */
  void set body(Statement statement) {
    _body = becomeParentOf(statement);
  }

  @override
  Iterable get childEntities => new ChildEntities()
      ..add(keyword)
      ..add(leftParenthesis)
      ..add(_condition)
      ..add(rightParenthesis)
      ..add(_body);

  /**
   * Return the expression used to determine whether to execute the body of the loop.
   *
   * @return the expression used to determine whether to execute the body of the loop
   */
  Expression get condition => _condition;

  /**
   * Set the expression used to determine whether to execute the body of the loop to the given
   * expression.
   *
   * @param expression the expression used to determine whether to execute the body of the loop
   */
  void set condition(Expression expression) {
    _condition = becomeParentOf(expression);
  }

  @override
  Token get endToken => _body.endToken;

  @override
  accept(AstVisitor visitor) => visitor.visitWhileStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_condition, visitor);
    safelyVisitChild(_body, visitor);
  }
}

/**
 * Instances of the class `WithClause` represent the with clause in a class declaration.
 *
 * <pre>
 * withClause ::=
 *     'with' [TypeName] (',' [TypeName])*
 * </pre>
 */
class WithClause extends AstNode {
  /**
   * The token representing the 'with' keyword.
   */
  Token withKeyword;

  /**
   * The names of the mixins that were specified.
   */
  NodeList<TypeName> _mixinTypes;

  /**
   * Initialize a newly created with clause.
   *
   * @param withKeyword the token representing the 'with' keyword
   * @param mixinTypes the names of the mixins that were specified
   */
  WithClause(this.withKeyword, List<TypeName> mixinTypes) {
    _mixinTypes = new NodeList<TypeName>(this, mixinTypes);
  }

  @override
  Token get beginToken => withKeyword;

  /**
   * TODO(paulberry): add commas.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(withKeyword)
      ..addAll(_mixinTypes);

  @override
  Token get endToken => _mixinTypes.endToken;

  /**
   * Set the token representing the 'with' keyword to the given token.
   *
   * @param withKeyword the token representing the 'with' keyword
   */
  @deprecated
  void set mixinKeyword(Token withKeyword) {
    this.withKeyword = withKeyword;
  }

  /**
   * Return the names of the mixins that were specified.
   *
   * @return the names of the mixins that were specified
   */
  NodeList<TypeName> get mixinTypes => _mixinTypes;

  @override
  accept(AstVisitor visitor) => visitor.visitWithClause(this);

  @override
  void visitChildren(AstVisitor visitor) {
    _mixinTypes.accept(visitor);
  }
}

/**
 * Instances of the class `YieldStatement` implement a yield statement.
 */
class YieldStatement extends Statement {
  /**
   * The 'yield' keyword.
   */
  Token yieldKeyword;

  /**
   * The star optionally following the 'yield' keyword.
   */
  Token star;

  /**
   * The expression whose value will be yielded.
   */
  Expression _expression;

  /**
   * The semicolon following the expression.
   */
  Token semicolon;

  /**
   * Initialize a newly created yield expression.
   *
   * @param yieldKeyword the 'yield' keyword
   * @param star the star following the 'yield' keyword
   * @param expression the expression whose value will be yielded
   * @param semicolon the semicolon following the expression
   */
  YieldStatement(this.yieldKeyword, this.star, Expression expression,
      this.semicolon) {
    _expression = becomeParentOf(expression);
  }

  @override
  Token get beginToken {
    if (yieldKeyword != null) {
      return yieldKeyword;
    }
    return _expression.beginToken;
  }

  /**
   * TODO(paulberry): untested.
   */
  @override
  Iterable get childEntities => new ChildEntities()
      ..add(yieldKeyword)
      ..add(star)
      ..add(_expression)
      ..add(semicolon);

  @override
  Token get endToken {
    if (semicolon != null) {
      return semicolon;
    }
    return _expression.endToken;
  }

  /**
   * Return the expression whose value will be yielded.
   *
   * @return the expression whose value will be yielded
   */
  Expression get expression => _expression;

  /**
   * Set the expression whose value will be yielded to the given expression.
   *
   * @param expression the expression whose value will be yielded
   */
  void set expression(Expression expression) {
    _expression = becomeParentOf(expression);
  }

  @override
  accept(AstVisitor visitor) => visitor.visitYieldStatement(this);

  @override
  void visitChildren(AstVisitor visitor) {
    safelyVisitChild(_expression, visitor);
  }
}
