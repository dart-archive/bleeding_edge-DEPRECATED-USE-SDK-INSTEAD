// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library computer.hover;

import 'package:analysis_server/src/protocol.dart' show HoverInformation;
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/element.dart';


/**
 * Converts [str] from a Dart Doc string with slashes and stars to a plain text
 * representation of the comment.
 */
String _removeDartDocDelimiters(String str) {
  if (str == null) {
    return null;
  }
  // remove /** */
  if (str.startsWith('/**')) {
    str = str.substring(3);
  }
  if (str.endsWith("*/")) {
    str = str.substring(0, str.length - 2);
  }
  str = str.trim();
  // remove leading '* '
  List<String> lines = str.split('\n');
  StringBuffer sb = new StringBuffer();
  bool firstLine = true;
  for (String line in lines) {
    line = line.trim();
    if (line.startsWith("*")) {
      line = line.substring(1);
      if (line.startsWith(" ")) {
        line = line.substring(1);
      }
    } else if (line.startsWith("///")) {
      line = line.substring(3);
      if (line.startsWith(" ")) {
        line = line.substring(1);
      }
    }
    if (!firstLine) {
      sb.write('\n');
    }
    firstLine = false;
    sb.write(line);
  }
  str = sb.toString();
  // done
  return str;
}


/**
 * A computer for the hover at the specified offset of a Dart [CompilationUnit].
 */
class DartUnitHoverComputer {
  final CompilationUnit _unit;
  final int _offset;

  DartUnitHoverComputer(this._unit, this._offset);

  /**
   * Returns the computed hover, maybe `null`.
   */
  HoverInformation compute() {
    AstNode node = new NodeLocator.con1(_offset).searchWithin(_unit);
    if (node == null) {
      return null;
    }
    if (node.parent is TypeName &&
        node.parent.parent is ConstructorName &&
        node.parent.parent.parent is InstanceCreationExpression) {
      node = node.parent.parent.parent;
    }
    if (node.parent is ConstructorName &&
        node.parent.parent is InstanceCreationExpression) {
      node = node.parent.parent;
    }
    if (node is Expression) {
      Expression expression = node;
      HoverInformation hover =
          new HoverInformation(expression.offset, expression.length);
      // element
      Element element = ElementLocator.locateWithOffset(expression, _offset);
      if (element != null) {
        // variable, if synthetic accessor
        if (element is PropertyAccessorElement) {
          PropertyAccessorElement accessor = element;
          if (accessor.isSynthetic) {
            element = accessor.variable;
          }
        }
        // description
        hover.elementDescription = element.toString();
        hover.elementKind = element.kind.displayName;
        // library
        LibraryElement library = element.library;
        if (library != null) {
          hover.containingLibraryName = library.name;
          hover.containingLibraryPath = library.source.fullName;
        }
        // documentation
        String dartDoc = element.computeDocumentationComment();
        dartDoc = _removeDartDocDelimiters(dartDoc);
        hover.dartdoc = dartDoc;
      }
      // parameter
      hover.parameter = _safeToString(expression.bestParameterElement);
      // types
      hover.staticType = _safeToString(expression.staticType);
      hover.propagatedType = _safeToString(expression.propagatedType);
      // done
      return hover;
    }
    // not an expression
    return null;
  }

  static _safeToString(obj) => obj != null ? obj.toString() : null;
}
