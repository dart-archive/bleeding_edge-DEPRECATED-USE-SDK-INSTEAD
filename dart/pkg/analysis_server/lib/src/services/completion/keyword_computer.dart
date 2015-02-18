// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library services.completion.computer.dart.keyword;

import 'dart:async';

import 'package:analysis_server/src/protocol.dart';
import 'package:analysis_server/src/services/completion/dart_completion_manager.dart';
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/scanner.dart';

/**
 * A computer for calculating `completion.getSuggestions` request results
 * for the local library in which the completion is requested.
 */
class KeywordComputer extends DartCompletionComputer {

  @override
  bool computeFast(DartCompletionRequest request) {
    request.node.accept(new _KeywordVisitor(request));
    return true;
  }

  @override
  Future<bool> computeFull(DartCompletionRequest request) {
    return new Future.value(false);
  }
}

/**
 * A vistor for generating keyword suggestions.
 */
class _KeywordVisitor extends GeneralizingAstVisitor {
  final DartCompletionRequest request;

  /**
   * The identifier visited or `null` if not visited.
   */
  SimpleIdentifier identifier;

  _KeywordVisitor(this.request);

  @override
  visitBlock(Block node) {
    if (_isInClassMemberBody(node)) {
      _addSuggestions(
          [
              Keyword.ASSERT,
              Keyword.CASE,
              Keyword.CONTINUE,
              Keyword.DO,
              Keyword.FINAL,
              Keyword.FOR,
              Keyword.IF,
              Keyword.NEW,
              Keyword.RETHROW,
              Keyword.RETURN,
              Keyword.SUPER,
              Keyword.SWITCH,
              Keyword.THIS,
              Keyword.THROW,
              Keyword.TRY,
              Keyword.VAR,
              Keyword.VOID,
              Keyword.WHILE]);
    } else {
      _addSuggestions(
          [
              Keyword.ASSERT,
              Keyword.CASE,
              Keyword.CONTINUE,
              Keyword.DO,
              Keyword.FINAL,
              Keyword.FOR,
              Keyword.IF,
              Keyword.NEW,
              Keyword.RETHROW,
              Keyword.RETURN,
              Keyword.SWITCH,
              Keyword.THROW,
              Keyword.TRY,
              Keyword.VAR,
              Keyword.VOID,
              Keyword.WHILE]);
    }
  }

  @override
  visitClassDeclaration(ClassDeclaration node) {
    // Don't suggest class name
    if (node.name == identifier) {
      return;
    }
    // Inside the class declaration { }
    if (request.offset > node.leftBracket.offset) {
      _addSuggestions(
          [
              Keyword.CONST,
              Keyword.DYNAMIC,
              Keyword.FACTORY,
              Keyword.FINAL,
              Keyword.GET,
              Keyword.OPERATOR,
              Keyword.SET,
              Keyword.STATIC,
              Keyword.VAR,
              Keyword.VOID]);
      return;
    }
    _addClassDeclarationKeywords(node);
  }

  @override
  visitCompilationUnit(CompilationUnit node) {
    Directive firstDirective;
    int endOfDirectives = 0;
    if (node.directives.length > 0) {
      firstDirective = node.directives[0];
      endOfDirectives = node.directives.last.end - 1;
    }
    int startOfDeclarations = node.end;
    if (node.declarations.length > 0) {
      startOfDeclarations = node.declarations[0].offset;
      // If the first token is a simple identifier
      // and cursor position in within that first token
      // then consider cursor to be before the first declaration
      Token token = node.declarations[0].firstTokenAfterCommentAndMetadata;
      if (token.offset <= request.offset && request.offset <= token.end) {
        startOfDeclarations = token.end;
      }
    }

    // Simplistic check for library as first directive
    if (firstDirective is! LibraryDirective) {
      if (firstDirective != null) {
        if (request.offset <= firstDirective.offset) {
          _addSuggestions([Keyword.LIBRARY], DART_RELEVANCE_HIGH);
        }
      } else {
        if (request.offset <= startOfDeclarations) {
          _addSuggestions([Keyword.LIBRARY], DART_RELEVANCE_HIGH);
        }
      }
    }
    if (request.offset <= startOfDeclarations) {
      _addSuggestions(
          [Keyword.EXPORT, Keyword.IMPORT, Keyword.PART],
          DART_RELEVANCE_HIGH);
    }
    if (request.offset >= endOfDirectives) {
      _addSuggestions(
          [
              Keyword.ABSTRACT,
              Keyword.CLASS,
              Keyword.CONST,
              Keyword.FINAL,
              Keyword.TYPEDEF,
              Keyword.VAR],
          DART_RELEVANCE_HIGH);
    }
  }

  @override
  visitNode(AstNode node) {
    if (_isOffsetAfterNode(node)) {
      node.parent.accept(this);
    }
  }

  visitSimpleIdentifier(SimpleIdentifier node) {
    identifier = node;
    node.parent.accept(this);
  }

  void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    if (identifier != null && node.beginToken == identifier.beginToken) {
      AstNode unit = node.parent;
      if (unit is CompilationUnit) {
        CompilationUnitMember previous;
        for (CompilationUnitMember member in unit.declarations) {
          if (member == node && previous is ClassDeclaration) {
            if (previous.endToken.isSynthetic) {
              // Partial keywords (simple identifirs) that are part of a
              // class declaration can be parsed
              // as a TypeName in a TopLevelVariableDeclaration
              _addClassDeclarationKeywords(previous);
              return;
            }
          }
          previous = member;
        }
        // Partial keywords (simple identifiers) can be parsed
        // as a TypeName in a TopLevelVariableDeclaration
        unit.accept(this);
      }
    }
  }

  void visitTypeName(TypeName node) {
    node.parent.accept(this);
  }

  void visitVariableDeclarationList(VariableDeclarationList node) {
    node.parent.accept(this);
  }

  void _addClassDeclarationKeywords(ClassDeclaration node) {
    // Very simplistic suggestion because analyzer will warn if
    // the extends / with / implements keywords are out of order
    if (node.extendsClause == null) {
      _addSuggestion(Keyword.EXTENDS, DART_RELEVANCE_HIGH);
    } else if (node.withClause == null) {
      _addSuggestion(Keyword.WITH, DART_RELEVANCE_HIGH);
    }
    if (node.implementsClause == null) {
      _addSuggestion(Keyword.IMPLEMENTS, DART_RELEVANCE_HIGH);
    }
  }

  void _addSuggestion(Keyword keyword, [int relevance =
      DART_RELEVANCE_DEFAULT]) {
    String completion = keyword.syntax;
    request.suggestions.add(
        new CompletionSuggestion(
            CompletionSuggestionKind.KEYWORD,
            relevance,
            completion,
            completion.length,
            0,
            false,
            false));
  }

  void _addSuggestions(List<Keyword> keywords, [int relevance =
      DART_RELEVANCE_KEYWORD]) {
    keywords.forEach((Keyword keyword) {
      _addSuggestion(keyword, relevance);
    });
  }

  bool _isOffsetAfterNode(AstNode node) {
    if (request.offset == node.end) {
      Token token = node.endToken;
      if (token != null && !token.isSynthetic) {
        if (token.lexeme == ';' || token.lexeme == '}') {
          return true;
        }
      }
    }
    return false;
  }

  static bool _isInClassMemberBody(AstNode node) {
    while (true) {
      AstNode body = node.getAncestor((n) => n is FunctionBody);
      if (body == null) {
        return false;
      }
      AstNode parent = body.parent;
      if (parent is ConstructorDeclaration || parent is MethodDeclaration) {
        return true;
      }
      node = parent;
    }
  }
}
