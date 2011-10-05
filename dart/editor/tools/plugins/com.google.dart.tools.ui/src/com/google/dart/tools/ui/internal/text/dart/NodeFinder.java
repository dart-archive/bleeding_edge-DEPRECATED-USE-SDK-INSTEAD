/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.compiler.ast.DartContext;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.parser.DartScanner;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.GenericVisitor;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.ui.internal.text.Selection;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class NodeFinder extends GenericVisitor {

  /**
   * A visitor that maps a selection to a given DartNode. The result node is determined as follows:
   * <ul>
   * <li>first the visitor tries to find a node with the exact start and length</li>
   * <li>if no such node exists than the node that encloses the range defined by start and end is
   * returned.</li>
   * <li>if the length is zero than also nodes are considered where the node's start or end position
   * matches <code>start</code>.</li>
   * <li>otherwise <code>null</code> is returned.</li>
   * </ul>
   * 
   * @param root the root node from which the search starts
   * @param start the start offset
   * @param length the length
   * @return the result node
   */
  public static DartNode perform(DartNode root, int start, int length) {
    NodeFinder finder = new NodeFinder(start, length);
    finder.accept(root);
    DartNode result = finder.getCoveredNode();
    if (result == null || result.getStartPosition() != start || result.getLength() != length) {
      return finder.getCoveringNode();
    }
    return result;
  }

  /**
   * A visitor that maps a selection to a given DartNode. The result node is determined as follows:
   * <ul>
   * <li>first the visitor tries to find a node that is covered by <code>start</code> and
   * <code>length</code> where either <code>start</code> and <code>length</code> exactly matches the
   * node or where the text covered before and after the node only consists of white spaces or
   * comments.</li>
   * <li>if no such node exists than the node that encloses the range defined by start and end is
   * returned.</li>
   * <li>if the length is zero than also nodes are considered where the node's start or end position
   * matches <code>start</code>.</li>
   * <li>otherwise <code>null</code> is returned.</li>
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
    finder.accept(root);
    DartNode result = finder.getCoveredNode();
    if (result == null) {
      return null;
    }
    Selection selection = Selection.createFromStartLength(start, length);
    if (selection.covers(result)) {
      Buffer buffer = source.getBuffer();
      if (buffer != null) {
        String src = buffer.getText(start, length);
        DartScanner scanner = new DartScanner(src);
        Token token = scanner.next();
        if (token != Token.EOS) {
          int tStart = scanner.getTokenLocation().getBegin().getPos();
          if (tStart == result.getStartPosition() - start) {
            int idx = tStart + result.getLength();
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

  public NodeFinder(int offset, int length) {
    fStart = offset;
    fEnd = offset + length;
  }

  /**
   * Returns the covered node. If more than one nodes are covered by the selection, the returned
   * node is first covered node found in a top-down traversal of the AST
   * 
   * @return DartNode
   */
  public DartNode getCoveredNode() {
    return fCoveredNode;
  }

  /**
   * Returns the covering node. If more than one nodes are covering the selection, the returned node
   * is last covering node found in a top-down traversal of the AST
   * 
   * @return DartNode
   */
  public DartNode getCoveringNode() {
    return fCoveringNode;
  }

  @Override
  protected boolean visitNode(DartNode node, DartContext context) {
    int nodeStart = node.getStartPosition();
    int nodeEnd = nodeStart + node.getLength();
    if (nodeEnd < fStart || fEnd < nodeStart) {
      return false;
    }
    if (nodeStart <= fStart && fEnd <= nodeEnd) {
      fCoveringNode = node;
    }
    if (fStart <= nodeStart && nodeEnd <= fEnd) {
      if (fCoveringNode == node) { // nodeStart == fStart && nodeEnd == fEnd
        fCoveredNode = node;
        return true; // look further for node with same length as parent
      } else if (fCoveredNode == null) { // no better found
        fCoveredNode = node;
      }
      return false;
    }
    return true;
  }

}
