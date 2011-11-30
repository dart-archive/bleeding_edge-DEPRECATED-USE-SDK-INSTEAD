// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class MessageKind {
  final String template;
  const MessageKind(this.template);

  static final GENERIC = const MessageKind('#{1}');

  static final NOT_ASSIGNABLE = const MessageKind(
      '#{2} is not assignable to #{1}');
  static final VOID_EXPRESSION = const MessageKind(
      'expression does not yield a value');
  static final VOID_VARIABLE = const MessageKind(
      'variable cannot be of type void');
  static final RETURN_VALUE_IN_VOID = const MessageKind(
      'cannot return value from void function');
  static final RETURN_NOTHING = const MessageKind(
      'value of type #{1} expected');
  static final MISSING_ARGUMENT = const MessageKind(
      'missing argument');
  static final ADDITIONAL_ARGUMENT = const MessageKind(
      'additional argument');

  static final CANNOT_RESOLVE = const MessageKind(
      'cannot resolve #{1}');
  static final CANNOT_RESOLVE_TYPE = const MessageKind(
      'cannot resolve type #{1}');
  static final DUPLICATE_DEFINITION = const MessageKind(
      'duplicate definition of #{1}');
  static final NOT_A_TYPE = const MessageKind(
      '#{1} is not a type');

  toString() => template;
}

class Message {
  final kind;
  final List arguments;
  String message;

  Message(this.kind, this.arguments);

  String toString() {
    if (message === null) {
      message = kind.template;
      int position = 1;
      for (var argument in arguments) {
        message = message.replaceAll('#{${position++}}',
                                     argument.toString());
      }
    }
    return message;
  }

  bool operator==(other) {
    if (other is !Message) return false;
    return (kind == other.kind) && (toString() == other.toString());
  }
}

class TypeWarning {
  final Message message;
  TypeWarning.message(this.message);
  TypeWarning(MessageKind kind, List<Type> arguments)
    : message = new Message(kind, arguments);
  String toString() => message.toString();
}

class ResolutionError {
  final Message message;
  ResolutionError.message(this.message);
  ResolutionError(MessageKind kind, List<Type> arguments)
    : message = new Message(kind, arguments);
  String toString() => message.toString();
}

class ResolutionWarning {
  final Message message;
  ResolutionWarning.message(this.message);
  ResolutionWarning(MessageKind kind, List<Type> arguments)
    : message = new Message(kind, arguments);
  String toString() => message.toString();
}
