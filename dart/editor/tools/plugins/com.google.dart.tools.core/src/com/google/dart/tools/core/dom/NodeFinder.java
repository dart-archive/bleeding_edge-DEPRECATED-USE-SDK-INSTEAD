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
package com.google.dart.tools.core.dom;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.parser.DartScanner;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class NodeFinder extends ASTVisitor<Void> {
  public static NodeFinder find(DartNode root, int start, int length) {
    NodeFinder finder = new NodeFinder(start, length);
    root.accept(finder);
    return finder;
  }

  /**
   * A visitor that maps a selection to a given DartNode. The result node is determined as follows:
   * <ul>
   * <li>First the visitor tries to find a node with the exact start and length</li>
   * <li>If no such node exists then the node that encloses the range defined by start and end is
   * returned.</li>
   * <li>If the length is zero then also nodes are considered where the node's start or end position
   * matches <code>start</code>.</li>
   * <li>Otherwise <code>null</code> is returned.</li>
   * </ul>
   * 
   * @param root the root node from which the search starts
   * @param start the start offset
   * @param length the length
   * @return the result node
   */
  public static DartNode perform(DartNode root, int start, int length) {
    NodeFinder finder = find(root, start, length);
    return finder.selectNode();
  }

  /**
   * A visitor that maps a selection to a given DartNode. The result node is determined as follows:
   * <ul>
   * <li>First the visitor tries to find a node that is covered by <code>start</code> and
   * <code>length</code> where either <code>start</code> and <code>length</code> exactly matches the
   * node or where the text covered before and after the node only consists of white spaces or
   * comments.</li>
   * <li>If no such node exists then the node that encloses the range defined by start and end is
   * returned.</li>
   * <li>If the length is zero then also nodes are considered where the node's start or end position
   * matches <code>start</code>.</li>
   * <li>Otherwise <code>null</code> is returned.</li>
   * </ul>
   * 
   * @param root the root node from which the search starts
   * @param start the start offset
   * @param length the length
   * @param source the source of the compilation unit
   * @return the result node
   * @throws DartModelException if an error occurs in the Java model
   */
  public static DartNode perform(DartNode root, int start, int length, CompilationUnit source)
      throws DartModelException {
    NodeFinder finder = new NodeFinder(start, length);
    root.accept(finder);
    DartNode result = finder.getCoveredNode();
    if (result == null) {
      return null;
    }
    int nodeStart = result.getSourceInfo().getOffset();
    int nodeEnd = result.getSourceInfo().getEnd();
    if (start <= nodeStart && nodeEnd <= start + length) {
      Buffer buffer = source.getBuffer();
      if (buffer != null) {
        String src = buffer.getText(start, length);
        DartScanner scanner = new DartScanner(src);
        Token token = scanner.next();
        if (token != Token.EOS) {
          int tStart = scanner.getTokenLocation().getBegin();
          if (tStart == result.getSourceInfo().getOffset() - start) {
            int idx = tStart + result.getSourceInfo().getLength();
            String nsrc = src.substring(idx, idx + length - 1);
            scanner = new DartScanner(nsrc);
            token = scanner.next();
            if (token == Token.EOS) {
              return result;
            }
          }
        }
      }
    }
    return finder.getCoveringNode();
  }

  public static DartNode perform(DartNode root, SourceRange range) {
    return perform(root, range.getOffset(), range.getLength());
  }

  private int fStart;

  private int fEnd;
  private DartNode fCoveringNode;
  private DartNode fCoveredNode;
  private DartMethodDefinition method;
  private DartMethodDefinition enclosingMethod;
  private DartClass classDef;
  private DartClass enclosingClass;
  private DartField enclosingField;

  private DartField field;

  public NodeFinder(int offset, int length) {
    fStart = offset;
    fEnd = offset + length;
  }

  /**
   * Returns the covered node. If more than one node is covered by the selection, the returned node
   * is the first covered node found in a top-down traversal of the AST.
   */
  public DartNode getCoveredNode() {
    return fCoveredNode;
  }

  /**
   * Returns the covering node. If more than one node is covering the selection, the returned node
   * is the last covering node found in a top-down traversal of the AST.
   */
  public DartNode getCoveringNode() {
    // In DartMethodDefinition nodes getName() and getFunction() have intersecting source ranges,
    // but are not parent and child.
    // So, here we check for this specific case to choose getName().
    if (fCoveringNode != null && fCoveringNode.getParent() instanceof DartMethodDefinition) {
      DartMethodDefinition methodDefinition = (DartMethodDefinition) fCoveringNode.getParent();
      if (fCoveringNode == methodDefinition.getFunction()) {
        DartExpression name = methodDefinition.getName();
        if (name.getSourceInfo().getOffset() <= fStart && fEnd <= name.getSourceInfo().getEnd()) {
          return name;
        }
      }
    }
    return fCoveringNode;
  }

  public DartClass getEnclosingClass() {
    return enclosingClass;
  }

  public DartField getEnclosingField() {
    return enclosingField;
  }

  public DartMethodDefinition getEnclosingMethod() {
    return enclosingMethod;
  }

  public DartNode selectNode() {
    DartNode result = getCoveredNode();
    if (result == null || result.getSourceInfo().getOffset() != fStart
        || result.getSourceInfo().getLength() != fEnd - fStart) {
      return getCoveringNode();
    }
    return result;
  }

  @Override
  public Void visitClass(DartClass node) {
    classDef = node;
    visitNode(node);
    classDef = null;
    return null;
  }

  @Override
  public Void visitField(DartField node) {
    field = node;
    visitNode(node);
    field = null;
    return null;
  }

  @Override
  public Void visitMethodDefinition(DartMethodDefinition node) {
    method = node;
    visitNode(node);
    method = null;
    return null;
  }

  @Override
  public Void visitNode(DartNode node) {
    int nodeStart = node.getSourceInfo().getOffset();
    int nodeEnd = node.getSourceInfo().getEnd();
    if (nodeEnd < fStart || fEnd < nodeStart) {
      if (nodeEnd == -2) {
        // TODO Remove this workaround for a parser bug: no source positions set
        node.visitChildren(this);
      }
      return null;
    }
    // There is problem in DartMethodDefinition and DartFunction.
    // DartFunction starts at same offset as method, but does not include name.
    if (node instanceof DartFunction && node.getParent() == enclosingMethod
        && enclosingMethod != null) {
      DartNode n = fCoveringNode;
      while (n != null) {
        if (n == enclosingMethod.getName()) {
          node.visitChildren(this);
          return null;
        }
        n = n.getParent();
      }
    }
    if (nodeStart <= fStart && fEnd <= nodeEnd) {
      fCoveringNode = node;
      enclosingMethod = method;
      enclosingField = field;
      enclosingClass = classDef;
    }
    if (fStart <= nodeStart && nodeEnd <= fEnd) {
      if (fCoveringNode == node) {
        // nodeStart == fStart && nodeEnd == fEnd
        fCoveredNode = node;
        enclosingMethod = method;
        enclosingField = field;
        enclosingClass = classDef;
        // look further for node with same length as parent
        node.visitChildren(this);
      } else if (fCoveredNode == null) {
        // no better found
        fCoveredNode = node;
        enclosingMethod = method;
        enclosingField = field;
        enclosingClass = classDef;
      }
      return null;
    }
    node.visitChildren(this);
    return null;
  }
}
