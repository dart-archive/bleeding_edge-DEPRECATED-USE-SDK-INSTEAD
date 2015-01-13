// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Utility function shared between different parts of analyzer2dart.

library analyzer2dart.util;

import 'package:analyzer/analyzer.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:compiler/src/universe/universe.dart';
import 'package:compiler/src/io/source_file.dart';

Selector createSelectorFromMethodInvocation(ArgumentList node,
                                            String name) {
  int arity = 0;
  List<String> namedArguments = <String>[];
  for (Expression argument in node.arguments) {
    if (argument is NamedExpression) {
      namedArguments.add(argument.name.label.name);
    } else {
      arity++;
    }
  }
  return new Selector.call(name, null, arity, namedArguments);
}

/// Prints [message] together with source code pointed to by [node] from
/// [source].
void reportSourceMessage(Source source, AstNode node, String message) {
  SourceFile sourceFile =
      new StringSourceFile(source.fullName, source.contents.data);

  print(sourceFile.getLocationMessage(message, node.offset, node.end));
}
