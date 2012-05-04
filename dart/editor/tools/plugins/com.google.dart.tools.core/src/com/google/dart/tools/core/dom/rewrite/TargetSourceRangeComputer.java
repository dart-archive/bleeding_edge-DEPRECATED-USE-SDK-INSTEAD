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
package com.google.dart.tools.core.dom.rewrite;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;

/**
 * Instances of the class <code>TargetSourceRangeComputer</code> compute adjusted source ranges for
 * AST nodes that are being replaced or deleted.
 * <p>
 * For example, a refactoring like inline method may choose to replace calls to the method but leave
 * intact any comments immediately preceding the calls. On the other hand, a refactoring like
 * extract method may choose to extract not only the nodes for the selected code but also any
 * comments preceding or following them.
 * <p>
 * Clients should subclass if they need to influence the the source range to be affected when
 * replacing or deleting a particular node. An instance of the subclass should be registered with
 * {@link ASTRewrite#setTargetSourceRangeComputer(TargetSourceRangeComputer)}. During a call to
 * {@link ASTRewrite#rewriteAST(org.eclipse.jface.text.IDocument, java.util.Map)} , the
 * {@link #computeSourceRange(ASTNode)} method on this object will be used to compute the source
 * range for a node being deleted or replaced.
 */
public class TargetSourceRangeComputer {
  /**
   * Reified source range. Instances are &quot;value&quot; object (cannot be modified).
   */
  public static final class SourceRange {
    /**
     * 0-based character index, or <code>-1</code> if no source position information is known.
     */
    private int startPosition;

    /**
     * (possibly 0) length, or <code>0</code> if no source position information is known.
     */
    private int length;

    /**
     * Creates a new source range.
     * 
     * @param startPosition the 0-based character index, or <code>-1</code> if no source position
     *          information is known
     * @param length the (possibly 0) length, or <code>0</code> if no source position information is
     *          known
     */
    public SourceRange(int startPosition, int length) {
      this.startPosition = startPosition;
      this.length = length;
    }

    /**
     * Return the source length.
     * 
     * @return a (possibly 0) length, or <code>0</code> if no source position information is known
     */
    public int getLength() {
      return length;
    }

    /**
     * Return the start position.
     * 
     * @return the 0-based character index, or <code>-1</code> if no source position information is
     *         known
     */
    public int getStartPosition() {
      return startPosition;
    }
  }

  /**
   * Creates a new target source range computer.
   */
  public TargetSourceRangeComputer() {
    // do nothing
  }

  /**
   * Return the target source range of the given node. Unlike {@link ASTNode#getStartPosition()} and
   * {@link ASTNode#getLength()}, the extended source range may include comments and whitespace
   * immediately before or after the normal source range for the node.
   * <p>
   * The returned source ranges must satisfy the following conditions:
   * <dl>
   * <li>no two source ranges in an AST may be overlapping</li>
   * <li>a source range of a parent node must fully cover the source ranges of its children</li>
   * </dl>
   * <p>
   * The default implementation uses {@link CompilationUnit#getExtendedStartPosition(ASTNode)} and
   * {@link CompilationUnit#getExtendedLength(ASTNode)} to compute the target source range. Clients
   * may override or extend this method to expand or contract the source range of the given node.
   * The resulting source range must cover at least the original source range of the node.
   * 
   * @param node the node with a known source range in the compilation unit being rewritten
   * @return the exact source range in the compilation unit being rewritten that should be replaced
   *         (or deleted)
   */
  public SourceRange computeSourceRange(DartNode node) {
    DartCore.notYetImplemented();
    // DartNode root= node.getRoot();
    // if (root instanceof DartUnit) {
    // DartUnit cu= (DartUnit) root;
    // return new SourceRange(cu.getExtendedStartPosition(node),
    // cu.getExtendedLength(node));
    // }
    return new SourceRange(node.getSourceInfo().getOffset(), node.getSourceInfo().getLength());
  }
}
