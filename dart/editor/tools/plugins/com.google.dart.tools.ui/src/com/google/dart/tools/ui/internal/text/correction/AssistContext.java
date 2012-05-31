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
package com.google.dart.tools.ui.internal.text.correction;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.ASTProvider;
import com.google.dart.tools.ui.text.dart.IInvocationContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.TextInvocationContext;
import org.eclipse.ui.IEditorPart;

public class AssistContext extends TextInvocationContext implements IInvocationContext {

  private final CompilationUnit fCompilationUnit;
  private final IEditorPart fEditor;

  private DartUnit fASTRoot;
//  private final SharedASTProvider.WAIT_FLAG fWaitFlag;
//  private final ASTProvider.WAIT_FLAG fWaitFlag;
  /**
   * The cached node finder, can be null.
   */
  private NodeFinder fNodeFinder;

  /*
   * Constructor for CorrectionContext.
   */
  public AssistContext(CompilationUnit cu, int offset, int length) {
    this(cu, null, offset, length);
  }

  public AssistContext(CompilationUnit cu, ISourceViewer sourceViewer, IEditorPart editor,
      int offset, int length) {
    this(cu, sourceViewer, editor, offset, length, ASTProvider.WAIT_YES);
  }

  public AssistContext(CompilationUnit cu, ISourceViewer sourceViewer, int offset, int length) {
    this(cu, sourceViewer, null, offset, length);
  }

  public AssistContext(CompilationUnit cu, ISourceViewer sourceViewer, int offset, int length,
      ASTProvider.WAIT_FLAG waitFlag) {
    this(cu, sourceViewer, null, offset, length, waitFlag);
  }

  private AssistContext(CompilationUnit cu, ISourceViewer sourceViewer, IEditorPart editor,
      int offset, int length, ASTProvider.WAIT_FLAG waitFlag) {
    super(sourceViewer, offset, length);
    Assert.isLegal(cu != null);
    Assert.isLegal(waitFlag != null);
    fCompilationUnit = cu;
    fEditor = editor;
//    fWaitFlag = waitFlag;
  }

  @Override
  public DartUnit getASTRoot() {
    if (fASTRoot == null) {
//      fASTRoot = SharedASTProvider.getAST(fCompilationUnit, fWaitFlag, null);
      try {
        // TODO(scheglov) remove SAVE when compilation of WORKING-COPY will be fixed
        fCompilationUnit.getBuffer().save(null, true);
        fASTRoot = DartCompilerUtilities.resolveUnit(fCompilationUnit);
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
        return null;
      }
      // TODO(scheglov) don't know if needed
//      if (fASTRoot == null) {
//        // see bug 63554
//        fASTRoot = ASTResolving.createQuickFixAST(fCompilationUnit, null);
//      }
    }
    return fASTRoot;
  }

  /**
   * Returns the compilation unit.
   * 
   * @return an <code>CompilationUnit</code>
   */
  @Override
  public CompilationUnit getCompilationUnit() {
    return fCompilationUnit;
  }

  @Override
  public DartNode getCoveredNode() {
    if (fNodeFinder == null) {
      fNodeFinder = NodeFinder.find(getASTRoot(), getOffset(), getLength());
    }
    return fNodeFinder.getCoveredNode();
  }

  @Override
  public DartNode getCoveringNode() {
    if (fNodeFinder == null) {
      fNodeFinder = NodeFinder.find(getASTRoot(), getOffset(), getLength());
    }
    return fNodeFinder.getCoveringNode();
  }

  /**
   * Returns the editor or <code>null</code> if none.
   * 
   * @return an <code>IEditorPart</code> or <code>null</code> if none
   * @since 3.5
   */
  public IEditorPart getEditor() {
    return fEditor;
  }

  /**
   * Returns the length.
   */
  @Override
  public int getSelectionLength() {
    return Math.max(getLength(), 0);
  }

  /**
   * Returns the offset.
   */
  @Override
  public int getSelectionOffset() {
    return getOffset();
  }

  public void setASTRoot(DartUnit root) {
    fASTRoot = root;
  }

}
