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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.text.Selection;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;

/**
 * A special text selection that gives access to the resolved and enclosing element.
 */
public class DartTextSelection extends TextSelection {

  private DartElement fElement;
  private DartElement[] fResolvedElements;

  private boolean fEnclosingElementRequested;
  private DartElement fEnclosingElement;

  private boolean fPartialASTRequested;
  private DartUnit fPartialAST;

  private boolean fNodesRequested;
  private DartNode[] fSelectedNodes;
  private DartNode fCoveringNode;

  private boolean fInMethodBodyRequested;
  private boolean fInMethodBody;

  private boolean fInClassInitializerRequested;
  private boolean fInClassInitializer;

  private boolean fInVariableInitializerRequested;
  private boolean fInVariableInitializer;

  /**
   * Creates a new text selection at the given offset and length.
   */
  public DartTextSelection(DartElement element, IDocument document, int offset, int length) {
    super(document, offset, length);
    fElement = element;
  }

  public DartNode resolveCoveringNode() {
    if (fNodesRequested) {
      return fCoveringNode;
    }
    resolveSelectedNodes();
    return fCoveringNode;
  }

  /**
   * Resolves the <code>DartElement</code>s at the current offset. Returns an empty array if the
   * string under the offset doesn't resolve to a <code>DartElement</code>.
   * 
   * @return the resolved java elements at the current offset
   * @throws DartModelException passed from the underlying code resolve API
   */
  public DartElement[] resolveElementAtOffset() throws DartModelException {
    if (fResolvedElements != null) {
      return fResolvedElements;
    }
    // long start= System.currentTimeMillis();
    fResolvedElements = SelectionConverter.codeResolve(fElement, this);
    // System.out.println("Time resolving element: " +
    // (System.currentTimeMillis() - start));
    return fResolvedElements;
  }

  public DartElement resolveEnclosingElement() throws DartModelException {
    if (fEnclosingElementRequested) {
      return fEnclosingElement;
    }
    fEnclosingElementRequested = true;
    fEnclosingElement = SelectionConverter.resolveEnclosingElement(fElement, this);
    return fEnclosingElement;
  }

  public boolean resolveInClassInitializer() {
    DartX.todo();
    return false;
//    if (fInClassInitializerRequested)
//      return fInClassInitializer;
//    fInClassInitializerRequested = true;
//    resolveSelectedNodes();
//    DartNode node = getStartNode();
//    if (node == null) {
//      fInClassInitializer = true;
//    } else {
//      while (node != null) {
//        int nodeType = node.getNodeType();
//        if (node instanceof AbstractTypeDeclaration) {
//          fInClassInitializer = false;
//          break;
//        } else if (nodeType == DartNode.ANONYMOUS_CLASS_DECLARATION) {
//          fInClassInitializer = false;
//          break;
//        } else if (nodeType == DartNode.INITIALIZER) {
//          fInClassInitializer = true;
//          break;
//        }
//        node = node.getParent();
//      }
//    }
//    return fInClassInitializer;
  }

  public boolean resolveInMethodBody() {
    if (fInMethodBodyRequested) {
      return fInMethodBody;
    }
    fInMethodBodyRequested = true;
    resolveSelectedNodes();
    DartNode node = getStartNode();
    if (node == null) {
      fInMethodBody = true;
    } else {
      while (node != null) {
        if (node instanceof DartBlock && node.getParent() instanceof DartFunction
            && node.getParent().getParent() instanceof DartMethodDefinition) {
          fInMethodBody = true;
          break;
        }
        node = node.getParent();
      }
    }
    return fInMethodBody;
  }

  public boolean resolveInVariableInitializer() {
    if (fInVariableInitializerRequested) {
      return fInVariableInitializer;
    }
    fInVariableInitializerRequested = true;
    resolveSelectedNodes();
    DartNode node = getStartNode();
    DartNode last = null;

    DartX.todo();

    while (node != null) {
      if (node instanceof DartClass) {
        fInVariableInitializer = false;
        break;
        // TODO(devoncarew): commenting out to fix compilation errors
//      } else if (node instanceof DartFieldDefinition
//          && ((DartFieldDefinition) node).getValue() == last) {
//        fInVariableInitializer = true;
//        break;
      } else if (node instanceof DartVariable && ((DartVariable) node).getValue() == last) {
        fInVariableInitializer = true;
        break;
      }
      last = node;
      node = node.getParent();
    }
    return fInVariableInitializer;
  }

  public DartUnit resolvePartialAstAtOffset() {
    if (fPartialASTRequested) {
      return fPartialAST;
    }
    fPartialASTRequested = true;
    if (!(fElement instanceof CompilationUnit)) {
      return null;
    }
    // long start= System.currentTimeMillis();
    fPartialAST = DartToolsPlugin.getDefault().getASTProvider().getAST(fElement,
        ASTProvider.WAIT_YES, null);
    // System.out.println("Time requesting partial AST: " +
    // (System.currentTimeMillis() - start));
    return fPartialAST;
  }

  public DartNode[] resolveSelectedNodes() {
    if (fNodesRequested) {
      return fSelectedNodes;
    }
    fNodesRequested = true;
    DartUnit root = resolvePartialAstAtOffset();
    if (root == null) {
      return null;
    }
    Selection ds = Selection.createFromStartLength(getOffset(), getLength());
    SelectionAnalyzer analyzer = new SelectionAnalyzer(ds, false);
    root.accept(analyzer);
    fSelectedNodes = analyzer.getSelectedNodes();
    fCoveringNode = analyzer.getLastCoveringNode();
    return fSelectedNodes;
  }

  private DartNode getStartNode() {
    if (fSelectedNodes != null && fSelectedNodes.length > 0) {
      return fSelectedNodes[0];
    } else {
      return fCoveringNode;
    }
  }
}
