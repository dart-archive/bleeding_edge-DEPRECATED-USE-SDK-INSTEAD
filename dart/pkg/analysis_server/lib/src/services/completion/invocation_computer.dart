// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library services.completion.computer.dart.invocation;

import 'dart:async';

import 'package:analysis_server/src/services/completion/dart_completion_manager.dart';
import 'package:analysis_server/src/services/completion/optype.dart';
import 'package:analysis_server/src/services/completion/suggestion_builder.dart';
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/element.dart';

import '../../protocol_server.dart' show CompletionSuggestionKind;

/**
 * A computer for calculating invocation / access suggestions
 * `completion.getSuggestions` request results.
 */
class InvocationComputer extends DartCompletionComputer {
  SuggestionBuilder builder;

  @override
  bool computeFast(DartCompletionRequest request) {
    OpType optype = request.optype;
    if (optype.includeInvocationSuggestions) {
      builder = request.node.accept(new _InvocationAstVisitor(request));
      if (builder != null) {
        return builder.computeFast(request.node);
      }
    }

    return true;
  }

  @override
  Future<bool> computeFull(DartCompletionRequest request) {
    if (builder != null) {
      return builder.computeFull(request.node);
    }
    return new Future.value(false);
  }
}

class _ExpressionSuggestionBuilder implements SuggestionBuilder {
  final DartCompletionRequest request;

  _ExpressionSuggestionBuilder(this.request);

  @override
  bool computeFast(AstNode node) {
    return false;
  }

  @override
  Future<bool> computeFull(AstNode node) {
    if (node is SimpleIdentifier) {
      node = node.parent;
    }
    if (node is MethodInvocation) {
      node = (node as MethodInvocation).realTarget;
    } else if (node is PropertyAccess) {
      node = (node as PropertyAccess).realTarget;
    }
    if (node is Identifier && node.bestElement is ClassElement) {
      node.bestElement.accept(
          new _PrefixedIdentifierSuggestionBuilder(request));
      return new Future.value(true);
    }
    if (node is Expression) {
      InterfaceTypeSuggestionBuilder.suggestionsFor(request, node.bestType);
      return new Future.value(true);
    }
    return new Future.value(false);
  }
}

/**
 * An [AstNode] vistor for determining which suggestion builder
 * should be used to build invocation/access suggestions.
 */
class _InvocationAstVisitor extends GeneralizingAstVisitor<SuggestionBuilder> {
  final DartCompletionRequest request;

  _InvocationAstVisitor(this.request);

  @override
  visitConstructorName(ConstructorName node) {
    // some PrefixedIdentifier nodes are transformed into
    // ConstructorName nodes during the resolution process.
    return new _PrefixedIdentifierSuggestionBuilder(request);
  }

  @override
  SuggestionBuilder visitMethodInvocation(MethodInvocation node) {
    return new _ExpressionSuggestionBuilder(request);
  }

  @override
  SuggestionBuilder visitNode(AstNode node) {
    return null;
  }

  @override
  SuggestionBuilder visitPrefixedIdentifier(PrefixedIdentifier node) {
    // some PrefixedIdentifier nodes are transformed into
    // ConstructorName nodes during the resolution process.
    return new _PrefixedIdentifierSuggestionBuilder(request);
  }

  @override
  SuggestionBuilder visitPropertyAccess(PropertyAccess node) {
    return new _ExpressionSuggestionBuilder(request);
  }

  @override
  SuggestionBuilder visitSimpleIdentifier(SimpleIdentifier node) {
    return node.parent.accept(this);
  }
}

/**
 * An [Element] visitor for determining the appropriate invocation/access
 * suggestions based upon the element for which the completion is requested.
 */
class _PrefixedIdentifierSuggestionBuilder extends
    GeneralizingElementVisitor<Future<bool>> implements SuggestionBuilder {

  final DartCompletionRequest request;

  _PrefixedIdentifierSuggestionBuilder(this.request);

  @override
  bool computeFast(AstNode node) {
    return false;
  }

  @override
  Future<bool> computeFull(AstNode node) {
    if (node is SimpleIdentifier) {
      node = node.parent;
    }
    if (node is ConstructorName) {
      // some PrefixedIdentifier nodes are transformed into
      // ConstructorName nodes during the resolution process.
      return new NamedConstructorSuggestionBuilder(request).computeFull(node);
    }
    if (node is PrefixedIdentifier) {
      SimpleIdentifier prefix = node.prefix;
      if (prefix != null) {
        Element element = prefix.bestElement;
        DartType type = prefix.bestType;
        if (element is! ClassElement && type != null && !type.isDynamic) {
          InterfaceTypeSuggestionBuilder.suggestionsFor(
              request,
              type);
          return new Future.value(true);
        } else if (element != null) {
          return element.accept(this);
        }
      }
    }
    return new Future.value(false);
  }

  @override
  Future<bool> visitClassElement(ClassElement element) {
    if (element != null) {
      InterfaceType type = element.type;
      if (type != null) {
        StaticClassElementSuggestionBuilder.suggestionsFor(
            request,
            type.element);
      }
    }
    return new Future.value(false);
  }

  @override
  Future<bool> visitElement(Element element) {
    return new Future.value(false);
  }

  @override
  Future<bool> visitPrefixElement(PrefixElement element) {
    //TODO (danrubel) reimplement to use prefixElement.importedLibraries
    // once that accessor is implemented and available in Dart
    bool modified = false;
    // Find the import directive with the given prefix
    request.unit.directives.forEach((Directive directive) {
      if (directive is ImportDirective) {
        if (directive.prefix != null) {
          if (directive.prefix.name == element.name) {
            // Suggest elements from the imported library
            LibraryElement library = directive.uriElement;
            LibraryElementSuggestionBuilder.suggestionsFor(
                request,
                CompletionSuggestionKind.INVOCATION,
                library);
            modified = true;
          }
        }
      }
    });
    return new Future.value(modified);
  }

  @override
  Future<bool> visitPropertyAccessorElement(PropertyAccessorElement element) {
    if (element != null) {
      PropertyInducingElement elemVar = element.variable;
      if (elemVar != null) {
        InterfaceTypeSuggestionBuilder.suggestionsFor(request, elemVar.type);
      }
      return new Future.value(true);
    }
    return new Future.value(false);
  }

  @override
  Future<bool> visitVariableElement(VariableElement element) {
    InterfaceTypeSuggestionBuilder.suggestionsFor(request, element.type);
    return new Future.value(true);
  }
}
