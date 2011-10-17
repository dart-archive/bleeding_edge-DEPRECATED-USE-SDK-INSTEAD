/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.utilities.ast;

import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartResourceDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.type.Type;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.visitor.ChildVisitor;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartResource;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import java.net.URI;

/**
 * Instances of the class <code>DartElementLocator</code> locate the {@link DartElement Dart
 * element(s)} associated with a source range, given the AST structure built from the source.
 */
public class DartElementLocator extends DartNodeTraverser<Void> {
  /**
   * Instances of the class <code>DartElementFoundException</code> are used to cancel visiting after
   * an element has been found.
   */
  private class DartElementFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  /**
   * The compilation unit containing the element to be found.
   */
  private final CompilationUnit compilationUnit;

  /**
   * The start offset of the range used to identify the element.
   */
  private final int startOffset;

  /**
   * The end offset of the range used to identify the element.
   */
  private final int endOffset;

  /**
   * The element that was found that corresponds to the given source range, or <code>null</code> if
   * there is no such element.
   */
  private DartElement foundElement;

  /**
   * The region within the compilation unit that is associated with the element that was found, or
   * <code>null</code> if no element was found.
   */
  private IRegion wordRegion;

  /**
   * The region within the element's compilation unit that needs to be highlighted, or
   * <code>null</code> if there is no element.
   */
  private IRegion candidateRegion;

  /**
   * A visitor that will visit all of the children of the node being visited.
   */
  private ChildVisitor<Void> childVisitor = new ChildVisitor<Void>(this);

  /**
   * Initialize a newly create locator to locate one or more {@link DartElement Dart elements} by
   * locating the node within the given compilation unit that corresponds to the given range of
   * characters in the source.
   * 
   * @param input the compilation unit containing the element to be found
   * @param start the start offset of the range used to identify the element
   * @param end the end offset of the range used to identify the element
   */
  public DartElementLocator(CompilationUnit input, int start, int end) {
    this.compilationUnit = input;
    this.startOffset = start;
    this.endOffset = end;
  }

  /**
   * Return the region within the element's compilation unit that needs to be highlighted, or
   * <code>null</code> if either there is no element or if the element can be used to determine the
   * region.
   * 
   * @return the region within the element's compilation unit that needs to be highlighted
   */
  public IRegion getCandidateRegion() {
    return candidateRegion;
  }

  /**
   * Return element that was found that corresponds to the given source range, or <code>null</code>
   * if there is no such element.
   * 
   * @return the element that was found
   */
  public DartElement getFoundElement() {
    return foundElement;
  }

  /**
   * Return the region within the compilation unit that is associated with the element that was
   * found, or <code>null</code> if no element was found.
   * 
   * @return the region within the compilation unit that is associated with the element that was
   *         found
   */
  public IRegion getWordRegion() {
    return wordRegion;
  }

  /**
   * Search within the given AST node for an identifier representing a {@link DartElement Dart
   * element} in the specified source range. Return the element that was found, or <code>null</code>
   * if no element was found.
   * 
   * @param node the AST node within which to search
   * @return the element that was found
   */
  public DartElement searchWithin(DartNode node) {
    try {
      node.accept(childVisitor);
    } catch (DartElementFoundException exception) {
      // A node with the right source position was found.
    }
    return foundElement;
  }

  @Override
  public Void visitArrayAccess(DartArrayAccess node) {
    super.visitArrayAccess(node);
    if (foundElement == null) {
      int start = node.getSourceStart();
      int end = start + node.getSourceLength();
      if (start <= startOffset && endOffset <= end) {
        DartExpression target = node.getTarget();
        wordRegion = new Region(target.getSourceStart() + target.getSourceLength(), end);
        Element targetSymbol = node.getReferencedElement();
        findElementFor(targetSymbol);
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitBinaryExpression(DartBinaryExpression node) {
    super.visitBinaryExpression(node);
    if (foundElement == null) {
      int start = node.getSourceStart();
      int end = start + node.getSourceLength();
      if (start <= startOffset && endOffset <= end) {
        DartExpression leftOperand = node.getArg1();
        DartExpression rightOperand = node.getArg2();
        wordRegion = computeOperatorRegion(
            leftOperand.getSourceStart() + leftOperand.getSourceLength(),
            rightOperand.getSourceStart() - 1);
        Element targetSymbol = node.getReferencedElement();
        findElementFor(targetSymbol);
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitIdentifier(DartIdentifier node) {
    if (foundElement == null) {
      int start = node.getSourceStart();
      int length = node.getSourceLength();
      int end = start + length;
      if (start <= startOffset && endOffset <= end) {
        wordRegion = new Region(start, length);
        Element targetSymbol = node.getReferencedElement();
        if (targetSymbol == null) {
          DartNode parent = node.getParent();
          if (parent instanceof DartTypeNode) {
            DartNode grandparent = parent.getParent();
            if (grandparent instanceof DartNewExpression) {
              targetSymbol = ((DartNewExpression) grandparent).getReferencedElement();
            }
            if (targetSymbol == null) {
              Type type = DartAstUtilities.getType((DartTypeNode) parent);
              if (type != null) {
                targetSymbol = type.getElement();
              }
            }
          } else if (parent instanceof DartMethodInvocation) {
            DartMethodInvocation invocation = (DartMethodInvocation) parent;
            if (node == invocation.getFunctionName()) {
              targetSymbol = invocation.getReferencedElement();
            }
          } else if (parent instanceof DartPropertyAccess) {
            DartPropertyAccess access = (DartPropertyAccess) parent;
            if (node == access.getName()) {
              targetSymbol = access.getReferencedElement();
            }
          } else if (parent instanceof DartUnqualifiedInvocation) {
            DartUnqualifiedInvocation invocation = (DartUnqualifiedInvocation) parent;
            if (node == invocation.getTarget()) {
              targetSymbol = invocation.getReferencedElement();
            }
          }
          if (targetSymbol == null) {
            targetSymbol = node.getTargetSymbol();
          }
        }
        if (targetSymbol == null) {
          foundElement = null;
        } else {
          if (targetSymbol instanceof VariableElement) {
            DartNode variableNode = ((VariableElement) targetSymbol).getNode();
            if (variableNode instanceof DartParameter) {
              DartParameter parameter = (DartParameter) variableNode;
              DartMethodDefinition method = DartAstUtilities.getEnclosingNodeOfType(
                  DartMethodDefinition.class, parameter);
              if (method == null) {
                DartClass containingType = DartAstUtilities.getEnclosingDartClass(variableNode);
                if (containingType != null) {
                  DartExpression parameterName = parameter.getName();
                  foundElement = BindingUtils.getDartElement(compilationUnit.getLibrary(),
                      containingType.getSymbol());
                  candidateRegion = new Region(parameterName.getSourceStart(),
                      parameterName.getSourceLength());
                } else {
                  foundElement = null;
                }
              } else {
                foundElement = BindingUtils.getDartElement(compilationUnit.getLibrary(),
                    method.getSymbol());
                DartExpression parameterName = parameter.getName();
                candidateRegion = new Region(parameterName.getSourceStart(),
                    parameterName.getSourceLength());
              }
            } else if (variableNode instanceof DartVariable) {
              DartVariable variable = (DartVariable) variableNode;
              DartClass containingType = DartAstUtilities.getEnclosingDartClass(variableNode);
              if (containingType != null) {
                DartIdentifier variableName = variable.getName();
                foundElement = BindingUtils.getDartElement(compilationUnit.getLibrary(),
                    containingType.getSymbol());
                candidateRegion = new Region(variableName.getSourceStart(),
                    variableName.getSourceLength());
              } else {
                foundElement = null;
              }
            } else {
              foundElement = null;
            }
          } else {
            findElementFor(targetSymbol);
          }
        }
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitNode(DartNode node) {
    node.accept(childVisitor);
    return null;
  }

  @Override
  public Void visitStringLiteral(DartStringLiteral node) {
    if (foundElement == null) {
      int start = node.getSourceStart();
      int length = node.getSourceLength();
      int end = start + length;
      if (start <= startOffset && end >= endOffset) {
        wordRegion = computeInternalStringRegion(start, length);
        DartNode parent = node.getParent();
        if (parent instanceof DartImportDirective
            && ((DartImportDirective) parent).getLibraryUri() == node) {
          DartLibrary library = compilationUnit.getLibrary();
          String libraryName = node.getValue();
          if (libraryName.startsWith("dart:")) {
            try {
              for (DartLibrary bundledLibrary : DartModelManager.getInstance().getDartModel().getBundledLibraries()) {
                if (bundledLibrary.getElementName().equals(libraryName)) {
                  foundElement = bundledLibrary.getDefiningCompilationUnit();
                  throw new DartElementFoundException();
                }
              }
            } catch (DartModelException exception) {
              DartCore.logError("Cannot access bundled libraries", exception);
            }
          } else {
            try {
              for (DartLibrary importedLibrary : library.getImportedLibraries()) {
                CompilationUnit importedUnit = importedLibrary.getCompilationUnit(libraryName);
                if (importedUnit != null && importedUnit.exists()) {
                  foundElement = importedUnit;
                  throw new DartElementFoundException();
                }
              }
            } catch (DartModelException exception) {
              DartCore.logError("Cannot access libraries imported by " + library.getElementName(),
                  exception);
            }
          }
        } else if (parent instanceof DartSourceDirective
            && ((DartSourceDirective) parent).getSourceUri() == node) {
          DartLibrary library = compilationUnit.getLibrary();
          CompilationUnit sourcedUnit = library.getCompilationUnit(node.getValue());
          if (sourcedUnit != null && sourcedUnit.exists()) {
            foundElement = sourcedUnit;
          }
        } else if (parent instanceof DartResourceDirective
            && ((DartResourceDirective) parent).getResourceUri() == node) {
          DartLibrary library = compilationUnit.getLibrary();
          try {
            URI resourceUri = compilationUnit.getSourceRef().getLibrary().getSourceFor(
                node.getValue()).getUri();
            DartResource resource = library.getResource(resourceUri);
            if (resource != null && resource.exists()) {
              foundElement = resource;
            }
          } catch (DartModelException exception) {
            foundElement = null;
          }
        }
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitUnaryExpression(DartUnaryExpression node) {
    super.visitUnaryExpression(node);
    if (foundElement == null) {
      int start = node.getSourceStart();
      int end = start + node.getSourceLength();
      if (start <= startOffset && endOffset <= end) {
        DartExpression operand = node.getArg();
        wordRegion = computeOperatorRegion(start, operand.getSourceStart() - 1);
        Element targetSymbol = node.getReferencedElement();
        findElementFor(targetSymbol);
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  /**
   * Compute a region that represents the portion of the string literal between the opening and
   * closing quotes.
   * 
   * @param nodeStart the index of the first character of the string literal
   * @param nodeLength the length of the string literal (including quotes)
   * @return the region that was computed
   */
  private IRegion computeInternalStringRegion(int nodeStart, int nodeLength) {
    int start = nodeStart;
    int end = nodeStart + nodeLength - 1;
    try {
      String source = compilationUnit.getBuffer().getContents();
      if (source.charAt(start) == '@') {
        start++;
      }
      if (source.charAt(start) == '\'') {
        while (source.charAt(start) == '\'') {
          start++;
        }
        while (source.charAt(end) == '\'') {
          end--;
        }
      } else {
        while (source.charAt(start) == '"') {
          start++;
        }
        while (source.charAt(end) == '"') {
          end--;
        }
      }
    } catch (DartModelException exception) {
    }
    if (start >= end) {
      return new Region(nodeStart, nodeLength);
    }
    return new Region(start, end - start + 1);
  }

  /**
   * Compute a region representing the portion of the source containing a binary operator.
   * 
   * @param left the index of the first character to the right of the left operand
   * @param right the index of the first character to the left of the right operand
   * @return the region that was computed
   */
  private IRegion computeOperatorRegion(int left, int right) {
    int start = left;
    int end = right;
    try {
      String source = compilationUnit.getBuffer().getContents();
      // TODO(brianwilkerson) This doesn't handle comments that occur between left and right, but
      // should.
      while (Character.isWhitespace(source.charAt(start))) {
        start++;
      }
      while (Character.isWhitespace(source.charAt(end))) {
        end--;
      }
    } catch (DartModelException exception) {
    }
    if (start > end) {
      return new Region(left, right - left + 1);
    }
    return new Region(start, end - start + 1);
  }

  /**
   * Given a compiler element representing some portion of the code base, set {@link #foundElement}
   * to the editor model element that corresponds to it.
   * 
   * @param targetSymbol the compiler element representing some portion of the code base
   */
  private void findElementFor(Element targetSymbol) {
    if (targetSymbol == null) {
      return;
    }
    LibraryElement definingLibraryElement = BindingUtils.getLibrary(targetSymbol);
    DartLibrary definingLibrary = null;
    if (definingLibraryElement != null) {
      definingLibrary = BindingUtils.getDartElement(compilationUnit.getLibrary(),
          definingLibraryElement);
    }
    if (definingLibrary == null) {
      definingLibrary = compilationUnit.getLibrary();
    }
    foundElement = BindingUtils.getDartElement(definingLibrary, targetSymbol);
    if (foundElement instanceof SourceReference) {
      try {
        SourceRange range = ((SourceReference) foundElement).getNameRange();
        candidateRegion = new Region(range.getOffset(), range.getLength());
      } catch (DartModelException exception) {
        // Ignored
      }
    }
  }
}
