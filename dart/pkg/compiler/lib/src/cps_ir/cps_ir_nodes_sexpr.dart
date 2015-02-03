// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dart2js.ir_nodes_sexpr;

import '../constants/values.dart';
import '../util/util.dart';
import 'cps_ir_nodes.dart';

/// A [Decorator] is a function used by [SExpressionStringifier] to augment the
/// output produced for a node.  It can be provided to the constructor.
typedef String Decorator(Node node, String s);

/// Generate a Lisp-like S-expression representation of an IR node as a string.
class SExpressionStringifier extends Visitor<String> with Indentation {
  final _Namer namer = new _Namer();

  String newValueName(Primitive node) => namer.nameValue(node);
  String newContinuationName(Continuation node) => namer.nameContinuation(node);
  Decorator decorator;

  SExpressionStringifier([this.decorator]) {
    if (this.decorator == null) {
      this.decorator = (Node node, String s) => s;
    }
  }

  String access(Reference<Definition> r) {
    return decorator(r.definition, namer.getName(r.definition));
  }

  String visitParameter(Parameter node) {
    return namer.nameParameter(node);
  }

  String visitMutableVariable(MutableVariable node) {
    return namer.nameMutableVariable(node);
  }

  /// Main entry point for creating a [String] from a [Node].  All recursive
  /// calls must go through this method.
  String visit(Node node) {
    String s = super.visit(node);
    return decorator(node, s);
  }

  String visitFunctionDefinition(FunctionDefinition node) {
    String name = node.element.name;
    namer.setReturnContinuation(node.body.returnContinuation);
    String parameters = node.parameters.map(visit).join(' ');
    String body = indentBlock(() => visit(node.body.body));
    return '$indentation(FunctionDefinition $name ($parameters) return\n'
        '$body)';
  }

  String visitFieldDefinition(FieldDefinition node) {
    String name = node.element.name;
    if (node.hasInitializer) {
      namer.setReturnContinuation(node.body.returnContinuation);
      String body = indentBlock(() => visit(node.body.body));
      return '$indentation(FieldDefinition $name () return\n'
             '$body)';
    } else {
      return '$indentation(FieldDefinition $name)';
    }
  }

  String visitLetPrim(LetPrim node) {
    String name = newValueName(node.primitive);
    String value = visit(node.primitive);
    String body = indentBlock(() => visit(node.body));
    return '$indentation(LetPrim ($name $value)\n$body)';
  }

  String visitLetCont(LetCont node) {
    String conts;
    bool first = true;
    for (Continuation continuation in node.continuations) {
      String name = newContinuationName(continuation);
      if (continuation.isRecursive) name = 'rec $name';
      // TODO(karlklose): this should be changed to `.map(visit).join(' ')`  and
      // should recurse to [visit].  Currently we can't do that, because the
      // unstringifier_test produces [LetConts] with dummy arguments on them.
      String parameters = continuation.parameters
          .map((p) => '${decorator(p, newValueName(p))}')
          .join(' ');
      String body =
          indentBlock(() => indentBlock(() => visit(continuation.body)));
      if (first) {
        first = false;
        conts = '($name ($parameters)\n$body)';
      } else {
        // Each subsequent line is indented additional spaces to align it
        // with the previous continuation.
        String indent = '$indentation${' ' * '(LetCont ('.length}';
        conts = '$conts\n$indent($name ($parameters)\n$body)';
      }
    }
    String body = indentBlock(() => visit(node.body));
    return '$indentation($LetCont ($conts)\n$body)';
  }

  String visitLetMutable(LetMutable node) {
    String name = visit(node.variable);
    String value = access(node.value);
    String body = indentBlock(() => visit(node.body));
    return '$indentation(LetMutable ($name $value)\n$body)';
  }

  String formatArguments(Invoke node) {
    int positionalArgumentCount = node.selector.positionalArgumentCount;
    List<String> args = new List<String>();
    args.addAll(
        node.arguments.getRange(0, positionalArgumentCount).map(access));
    for (int i = 0; i < node.selector.namedArgumentCount; ++i) {
      String name = node.selector.namedArguments[i];
      String arg = access(node.arguments[positionalArgumentCount + i]);
      args.add("($name: $arg)");
    }
    return '(${args.join(' ')})';
  }

  String visitInvokeStatic(InvokeStatic node) {
    String name = node.target.name;
    String cont = access(node.continuation);
    String args = formatArguments(node);
    return '$indentation(InvokeStatic $name $args $cont)';
  }

  String visitInvokeMethod(InvokeMethod node) {
    String name = node.selector.name;
    String rcv = access(node.receiver);
    String cont = access(node.continuation);
    String args = formatArguments(node);
    return '$indentation(InvokeMethod $rcv $name $args $cont)';
  }

  String visitInvokeMethodDirectly(InvokeMethodDirectly node) {
    String receiver = access(node.receiver);
    String name = node.selector.name;
    String cont = access(node.continuation);
    String args = formatArguments(node);
    return '$indentation(InvokeMethodDirectly $receiver $name $args $cont)';
  }

  String visitInvokeConstructor(InvokeConstructor node) {
    String callName;
    if (node.target.name.isEmpty) {
      callName = '${node.type}';
    } else {
      callName = '${node.type}.${node.target.name}';
    }
    String cont = access(node.continuation);
    String args = formatArguments(node);
    return '$indentation(InvokeConstructor $callName $args $cont)';
  }

  String visitConcatenateStrings(ConcatenateStrings node) {
    String cont = access(node.continuation);
    String args = node.arguments.map(access).join(' ');
    return '$indentation(ConcatenateStrings ($args) $cont)';
  }

  String visitInvokeContinuation(InvokeContinuation node) {
    String name = access(node.continuation);
    if (node.isRecursive) name = 'rec $name';
    String args = node.arguments.map(access).join(' ');
    return '$indentation($InvokeContinuation $name ($args))';
  }

  String visitBranch(Branch node) {
    String condition = visit(node.condition);
    String trueCont = access(node.trueContinuation);
    String falseCont = access(node.falseContinuation);
    return '$indentation(Branch $condition $trueCont $falseCont)';
  }

  String visitConstant(Constant node) {
    String value =
        node.expression.value.accept(new ConstantStringifier(), null);
    return '(Constant $value)';
  }

  String visitThis(This node) {
    return '(This)';
  }

  String visitReifyTypeVar(ReifyTypeVar node) {
    return '$indentation(ReifyTypeVar ${node.typeVariable.name})';
  }

  String visitCreateFunction(CreateFunction node) {
    String function =
        indentBlock(() => indentBlock(() => visit(node.definition)));
    return '(CreateFunction\n$function)';
  }

  String visitContinuation(Continuation node) {
    // Continuations are visited directly in visitLetCont.
    return '(Unexpected Continuation)';
  }

  String visitGetMutableVariable(GetMutableVariable node) {
    return '(GetMutableVariable ${access(node.variable)})';
  }

  String visitSetMutableVariable(SetMutableVariable node) {
    String value = access(node.value);
    String body = indentBlock(() => visit(node.body));
    return '$indentation(SetMutableVariable ${access(node.variable)} '
           '$value\n$body)';
  }

  String visitTypeOperator(TypeOperator node) {
    String receiver = access(node.receiver);
    String cont = access(node.continuation);
    String operator = node.isTypeTest ? 'is' : 'as';
    return '$indentation(TypeOperator $operator $receiver ${node.type} $cont)';
  }

  String visitLiteralList(LiteralList node) {
    String values = node.values.map(access).join(' ');
    return '(LiteralList ($values))';
  }

  String visitLiteralMap(LiteralMap node) {
    String keys = node.entries.map((e) => access(e.key)).join(' ');
    String values = node.entries.map((e) => access(e.value)).join(' ');
    return '(LiteralMap ($keys) ($values))';
  }

  String visitDeclareFunction(DeclareFunction node) {
    String name = visit(node.variable);
    String function = indentBlock(() => visit(node.definition));
    String body = indentBlock(() => visit(node.body));
    return '$indentation(DeclareFunction $name =\n'
           '$function in\n'
           '$body)';
  }

  String visitIsTrue(IsTrue node) {
    String value = access(node.value);
    return '(IsTrue $value)';
  }

  String visitSetField(SetField node) {
    String object = access(node.object);
    String field = node.field.name;
    String value = access(node.value);
    String body = indentBlock(() => visit(node.body));
    return '$indentation(SetField $object $field $value)\n$body';
  }

  String visitGetField(GetField node) {
    String object = access(node.object);
    String field = node.field.name;
    return '(GetField $object $field)';
  }

  String visitCreateBox(CreateBox node) {
    return '(CreateBox)';
  }

  String visitCreateInstance(CreateInstance node) {
    String className = node.classElement.name;
    String arguments = node.arguments.map(access).join(' ');
    return '(CreateInstance $className ($arguments))';
  }

  String visitIdentical(Identical node) {
    String left = access(node.left);
    String right = access(node.right);
    return '(Identical $left $right)';
  }

  String visitInterceptor(Interceptor node) {
    return '(Interceptor ${access(node.input)})';
  }
}

class ConstantStringifier extends ConstantValueVisitor<String, Null> {
  // Some of these methods are unimplemented because we haven't had a need
  // to print such constants.  When printing is implemented, the corresponding
  // parsing support should be added to SExpressionUnstringifier.parseConstant
  // in the dart2js tests (currently in the file
  // tests/compiler/dart2js/backend_dart/sexpr_unstringifier.dart).

  String _failWith(ConstantValue constant) {
    throw 'Stringification not supported for ${constant.toStructuredString()}';
  }

  String visitFunction(FunctionConstantValue constant, _) {
    return _failWith(constant);
  }

  String visitNull(NullConstantValue constant, _) {
    return '(Null)';
  }

  String visitInt(IntConstantValue constant, _) {
    return '(Int ${constant.unparse()})';
  }

  String visitDouble(DoubleConstantValue constant, _) {
    return '(Double ${constant.unparse()})';
  }

  String visitBool(BoolConstantValue constant, _) {
    return '(Bool ${constant.unparse()})';
  }

  String visitString(StringConstantValue constant, _) {
    return '(String ${constant.unparse()})';
  }

  String visitList(ListConstantValue constant, _) {
    return _failWith(constant);
  }

  String visitMap(MapConstantValue constant, _) {
    return _failWith(constant);
  }

  String visitConstructed(ConstructedConstantValue constant, _) {
    return _failWith(constant);
  }

  String visitType(TypeConstantValue constant, _) {
    return _failWith(constant);
  }

  String visitInterceptor(InterceptorConstantValue constant, _) {
    return _failWith(constant);
  }

  String visitDummy(DummyConstantValue constant, _) {
    return _failWith(constant);
  }

  String visitDeferred(DeferredConstantValue constant, _) {
    return _failWith(constant);
  }
}

class _Namer {
  final Map<Node, String> _names = <Node, String>{};
  int _valueCounter = 0;
  int _continuationCounter = 0;

  String nameParameter(Parameter parameter) {
    assert(!_names.containsKey(parameter));
    return _names[parameter] = parameter.hint.name;
  }

  String nameMutableVariable(MutableVariable variable) {
    assert(!_names.containsKey(variable));
    return _names[variable] = variable.hint.name;
  }

  String nameContinuation(Continuation node) {
    assert(!_names.containsKey(node));
    return _names[node] = 'k${_continuationCounter++}';
  }

  String nameValue(Primitive node) {
    assert(!_names.containsKey(node));
    return _names[node] = 'v${_valueCounter++}';
  }

  void setReturnContinuation(Continuation node) {
    assert(!_names.containsKey(node) || _names[node] == 'return');
    _names[node] = 'return';
  }

  String getName(Node node) {
    assert(_names.containsKey(node));
    return _names[node];
  }
}
