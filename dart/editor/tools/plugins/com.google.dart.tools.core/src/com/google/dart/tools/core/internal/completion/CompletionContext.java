package com.google.dart.tools.core.internal.completion;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.source.Source;

// temporary definition
public class CompletionContext implements AssistContext {

  private CompilationUnit compilationUnit;
  private int selectionOffset;
  private int selectionLength;

  public CompletionContext(CompilationUnit compilationUnit, int selectionOffset, int selectionLength) {
    this.compilationUnit = compilationUnit;
    this.selectionOffset = selectionOffset;
    this.selectionLength = selectionLength;
  }

  @Override
  public CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  @Override
  public ASTNode getCoveredNode() {
    NodeLocator loc = new NodeLocator(selectionOffset, selectionOffset);
    ASTNode node = loc.searchWithin(compilationUnit);
    return node;
  }

  @Override
  public ASTNode getCoveringNode() {
    NodeLocator loc = new NodeLocator(selectionOffset, selectionOffset + selectionLength);
    ASTNode node = loc.searchWithin(compilationUnit);
    return node;
  }

  @Override
  public int getSelectionLength() {
    return selectionLength;
  }

  @Override
  public int getSelectionOffset() {
    return selectionOffset;
  }

  @Override
  public Source getSource() {
    return compilationUnit.getElement().getSource();
  }

}
