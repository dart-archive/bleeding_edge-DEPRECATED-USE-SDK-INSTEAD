/*
 * Copyright (c) 2012, the Dart project authors.
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

import com.google.common.base.Objects;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPartOfDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.ElementKind;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.resolver.NodeElement;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Instances of the class <code>DartElementLocator</code> locate the {@link DartElement Dart
 * element(s)} associated with a source range, given the AST structure built from the source.
 */
public class DartElementLocator extends ASTVisitor<Void> {
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
  private int startOffset;

  /**
   * The end offset of the range used to identify the element.
   */
  private int endOffset;

  /**
   * A flag indicating whether elements should be returned for declaration sites as well as for
   * reference sites.
   */
  private final boolean includeDeclarations;

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
   * The resolved element that was found that corresponds to the given source range, or <code>null
   * </code> if there is no such element.
   */
  private Element resolvedElement;

  /**
   * Constructor for the case when you know not just offset in source, but resolved {@link Element},
   * so not need to visit AST, etc - just convert to the {@link DartElement}.
   */
  public DartElementLocator(CompilationUnit input, Element targetSymbol) {
    this(input, 0, 0, false);
    findElementFor(targetSymbol);
  }

  /**
   * Initialize a newly created locator to locate one or more {@link DartElement Dart elements} by
   * locating the node within the given compilation unit that corresponds to the given offset in the
   * source.
   * 
   * @param input the compilation unit containing the element to be found
   * @param offset the offset used to identify the element
   */
  public DartElementLocator(CompilationUnit input, int offset) {
    this(input, offset, offset, false);
  }

  /**
   * Initialize a newly created locator to locate one or more {@link DartElement Dart elements} by
   * locating the node within the given compilation unit that corresponds to the given range of
   * characters in the source.
   * 
   * @param input the compilation unit containing the element to be found
   * @param offset the offset used to identify the element
   * @param includeDeclarations <code>true</code> if elements should be returned for declaration
   *          sites as well as for reference sites
   */
  public DartElementLocator(CompilationUnit input, int offset, boolean includeDeclarations) {
    this(input, offset, offset, includeDeclarations);
  }

  /**
   * Initialize a newly created locator to locate one or more {@link DartElement Dart elements} by
   * locating the node within the given compilation unit that corresponds to the given range of
   * characters in the source.
   * 
   * @param input the compilation unit containing the element to be found
   * @param start the start offset of the range used to identify the element
   * @param end the end offset of the range used to identify the element
   */
  public DartElementLocator(CompilationUnit input, int start, int end) {
    this(input, start, end, false);
  }

  /**
   * Initialize a newly created locator to locate one or more {@link DartElement Dart elements} by
   * locating the node within the given compilation unit that corresponds to the given range of
   * characters in the source.
   * 
   * @param input the compilation unit containing the element to be found
   * @param start the start offset of the range used to identify the element
   * @param end the end offset of the range used to identify the element
   * @param includeDeclarations <code>true</code> if elements should be returned for declaration
   *          sites as well as for reference sites
   */
  public DartElementLocator(CompilationUnit input, int start, int end, boolean includeDeclarations) {
    this.compilationUnit = input;
    this.startOffset = start;
    this.endOffset = end;
    this.includeDeclarations = includeDeclarations;
  }

  /**
   * Return the region within the element's compilation unit that needs to be highlighted, or
   * <code>null
   * </code> if either there is no element or if the element can be used to determine the region.
   * 
   * @return the region within the element's compilation unit that needs to be highlighted
   */
  public IRegion getCandidateRegion() {
    return candidateRegion;
  }

  /**
   * Return the element that was found that corresponds to the given source range, or
   * <code>null</code> if there is no such element.
   * 
   * @return the element that was found
   */
  public DartElement getFoundElement() {
    return foundElement;
  }

  /**
   * Return the resolved element that was found that corresponds to the given source range, or
   * <code>null</code> if there is no such element.
   * 
   * @return the element that was found
   */
  public Element getResolvedElement() {
    return resolvedElement;
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
      node.accept(this);
    } catch (DartElementFoundException exception) {
      // A node with the right source position was found.
    } catch (Exception exception) {
      DartCore.logInformation("Unable to locate element at offset (" + startOffset + " - "
          + endOffset + ") in " + compilationUnit.getElementName(), exception);
      return null;
    }
    return foundElement;
  }

  @Override
  public Void visitArrayAccess(DartArrayAccess node) {
    super.visitArrayAccess(node);
    if (foundElement == null) {
      int start = node.getSourceInfo().getOffset();
      int end = start + node.getSourceInfo().getLength();
      if (start <= startOffset && endOffset <= end) {
        DartExpression target = node.getTarget();
        wordRegion = new Region(target.getSourceInfo().getOffset()
            + target.getSourceInfo().getLength(), end);
        Element targetElement = node.getElement();
        findElementFor(targetElement);
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitBinaryExpression(DartBinaryExpression node) {
    super.visitBinaryExpression(node);
    if (foundElement == null) {
      int start = node.getSourceInfo().getOffset();
      int end = start + node.getSourceInfo().getLength();
      if (start <= startOffset && endOffset <= end) {
        DartExpression leftOperand = node.getArg1();
        DartExpression rightOperand = node.getArg2();
        wordRegion = computeOperatorRegion(leftOperand.getSourceInfo().getOffset()
            + leftOperand.getSourceInfo().getLength(), rightOperand.getSourceInfo().getOffset() - 1);
        Element targetElement = node.getElement();
        findElementFor(targetElement);
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitIdentifier(DartIdentifier node) {
    if (foundElement == null) {
      int start = node.getSourceInfo().getOffset();
      int length = node.getSourceInfo().getLength();
      int end = start + length;
      if (start <= startOffset && endOffset <= end) {
        wordRegion = new Region(start, length);
        Element targetElement = DartAstUtilities.getElement(node, includeDeclarations);
        if (targetElement == null) {
          foundElement = null;
        } else {
          if (targetElement instanceof VariableElement) {
            VariableElement variableElement = (VariableElement) targetElement;
            resolvedElement = variableElement;
            if (variableElement.getKind() == ElementKind.PARAMETER
                || variableElement.getKind() == ElementKind.VARIABLE) {
              foundElement = BindingUtils.getDartElement(
                  compilationUnit.getLibrary(),
                  variableElement);
              candidateRegion = new Region(
                  variableElement.getNameLocation().getOffset(),
                  variableElement.getNameLocation().getLength());
            } else {
              foundElement = null;
            }
          } else {
            findElementFor(targetElement);
            // Import prefix is resolved into LibraryElement, so it is correct that corresponding
            // DartElement is DartLibrary, but this is not what we (and user) wants, because
            // it loses information. We want DartImport, it gives both DartLibrary and prefix.
            if (foundElement instanceof DartLibrary) {
              try {
                DartImport[] imports = compilationUnit.getLibrary().getImports();
                for (DartImport imprt : imports) {
                  if (Objects.equal(imprt.getLibrary(), foundElement)
                      && Objects.equal(imprt.getPrefix(), node.getName())) {
                    foundElement = imprt;
                    SourceRange range = imprt.getNameRange();
                    candidateRegion = new Region(range.getOffset(), range.getLength());
                  }
                }
              } catch (DartModelException e) {
                DartCore.logError("Cannot resolve import " + foundElement.getElementName(), e);
              }
            }
          }
        }
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitImportDirective(DartImportDirective node) {
    DartLibrary library = compilationUnit.getLibrary();
    try {
      if (Objects.equal(compilationUnit, library.getDefiningCompilationUnit())) {
        DartImport[] imports = library.getImports();
        for (DartImport imprt : imports) {
          // on URI of library - return defining Unit of imported Library
          SourceRange uriRange = imprt.getUriRange();
          if (SourceRangeUtils.contains(uriRange, startOffset)) {
            resolvedElement = null;
            foundElement = imprt.getLibrary().getDefiningCompilationUnit();
            wordRegion = new Region(uriRange.getOffset(), uriRange.getLength());
            candidateRegion = new Region(0, 0);
            throw new DartElementFoundException();
          }
          // on #import directive - return DartImport element
          SourceRange sourceRange = imprt.getSourceRange();
          if (SourceRangeUtils.contains(sourceRange, startOffset)) {
            resolvedElement = null;
            foundElement = imprt;
            wordRegion = new Region(sourceRange.getOffset(), sourceRange.getLength());
            candidateRegion = new Region(sourceRange.getOffset(), sourceRange.getLength());
            throw new DartElementFoundException();
          }
        }
      }
    } catch (DartModelException e) {
      DartCore.logError("Cannot access imports of " + library.getElementName(), e);
    }
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitNode(DartNode node) {
    try {
      node.visitChildren(this);
    } catch (DartElementFoundException exception) {
      throw exception;
    } catch (Exception exception) {
      // Ignore the exception and proceed in order to visit the rest of the structure.
      DartCore.logInformation(
          "Exception caught while traversing an AST structure. Please report to the dartc team.",
          exception);
    }
    return null;
  }

  @Override
  public Void visitPartOfDirective(DartPartOfDirective node) {
    int start = node.getSourceInfo().getOffset();
    int end = node.getSourceInfo().getEnd();
    if (start <= startOffset && endOffset <= end) {
      if (node.getName() != null) {
        Element targetElement = node.getName().getElement();
        findElementFor(targetElement);
        if (foundElement instanceof DartLibrary) {
          try {
            foundElement = ((DartLibrary) foundElement).getDefiningCompilationUnit();
            candidateRegion = new Region(0, 0);
          } catch (DartModelException exception) {
          }
        }
      }
    }
    return super.visitPartOfDirective(node);
  }

  @Override
  public Void visitStringLiteral(DartStringLiteral node) {
    if (foundElement == null) {
      int start = node.getSourceInfo().getOffset();
      int length = node.getSourceInfo().getLength();
      int end = start + length;
      if (end == 0) {
        return null;
      }
      if (start <= startOffset && end >= endOffset) {
        wordRegion = computeInternalStringRegion(start, length);
        DartNode parent = node.getParent();
        if (parent instanceof DartSourceDirective
            && ((DartSourceDirective) parent).getSourceUri() == node) {
          //resolvedElement = ((DartSourceDirective) parent).getElement();
          DartLibrary library = compilationUnit.getLibrary();
          String fileName = getFileName(library, node.getValue());
          CompilationUnit sourcedUnit = library.getCompilationUnit(fileName);
          if (sourcedUnit != null && sourcedUnit.exists()) {
            foundElement = sourcedUnit;
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
      int start = node.getSourceInfo().getOffset();
      int end = start + node.getSourceInfo().getLength();
      if (start <= startOffset && endOffset <= end) {
        DartExpression operand = node.getArg();
        wordRegion = computeOperatorRegion(start, operand.getSourceInfo().getOffset() - 1);
        Element targetSymbol = node.getElement();
        findElementFor(targetSymbol);
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    super.visitUnqualifiedInvocation(node);
    if (foundElement == null) {
      int start = node.getSourceInfo().getOffset();
      int length = node.getSourceInfo().getLength();
      int end = start + length;
      if (start <= startOffset && endOffset <= end) {
        wordRegion = new Region(start, length);
        NodeElement invocationElement = node.getElement();
        if (invocationElement != null && node.getTarget() != null
            && node.getTarget().getElement() != invocationElement) {
          findElementFor(invocationElement);
        }
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
      definingLibrary = BindingUtils.getDartElement(
          compilationUnit.getLibrary(),
          definingLibraryElement);
    }
    if (definingLibrary == null) {
      definingLibrary = compilationUnit.getLibrary();
    }
    resolvedElement = targetSymbol;
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

  /**
   * Extract the file name from the given URI. Return the file name that was extracted, or the
   * original URI if the format of the URI could not be recognized.
   * 
   * @param library the library that the URI might be relative to
   * @param uri the string representation of the URI
   * @return the file name that was extracted
   */
  private String getFileName(DartLibrary library, String uri) {
    int index = uri.lastIndexOf('/');
    if (index >= 0) {
      return uri.substring(index + 1);
    }
    return uri;
  }

}
