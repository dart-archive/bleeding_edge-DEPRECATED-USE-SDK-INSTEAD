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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.type.Type;
import com.google.dart.tools.core.dom.visitor.ChildVisitor;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.utilities.ast.DartAstUtilities;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;
import com.google.dart.tools.core.utilities.performance.PerformanceManager;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.OpenAction;
import com.google.dart.tools.ui.actions.SelectionDispatchAction;
import com.google.dart.tools.ui.internal.text.functions.DartWordFinder;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Dart element hyperlink detector.
 */
public class DartElementHyperlinkDetector extends AbstractHyperlinkDetector {
  /**
   * Visits an AST looking for a node between the given start and end offsets that could be a
   * hyperlink candidate.
   * 
   * @throws HyperlinkCandidateFoundException once a candidate has been found this is thrown to
   *           short cut any further visiting
   */
  private class HyperlinkCandidateVisitor extends DartNodeTraverser<Void> {
    /**
     * Used to cancel visiting after a candidate has been found.
     */
    private class HyperlinkCandidateFoundException extends RuntimeException {
      private static final long serialVersionUID = 1L;
    }

    /**
     * The found candidate or <code>null</code> if there is none.
     */
    private DartElement foundCandidate;

    /**
     * The region within the candidate that needs to be highlighted, or <code>null</code> if the
     * candidate can be used to determine the region.
     */
    private IRegion candidateRegion;

    /**
     * End offset where the candidate should end before.
     */
    private final int endOffset;

    /**
     * Start offset where the candidate should start after.
     */
    private final int startOffset;

    /**
     * The compilation unit containing the element to be found.
     */
    private final CompilationUnit compilationUnit;

    /**
     * A visitor that will visit all of the children of the node being visited.
     */
    private ChildVisitor<Void> childVisitor = new ChildVisitor<Void>(this);

    /**
     * @param input the compilation unit containing the element to be found
     * @param start start offset where the candidate should start after
     * @param end end offset where the candidate should end before
     */
    protected HyperlinkCandidateVisitor(CompilationUnit input, int start, int end) {
      this.compilationUnit = input;
      this.startOffset = start;
      this.endOffset = end;
    }

    /**
     * Determine whether the given node is a hyperlink candidate based on the start and end offsets.
     * 
     * @param node the node being tested
     * @throws HyperlinkCandidateFoundException
     */
    @Override
    public Void visitIdentifier(DartIdentifier node) {
      if (foundCandidate == null) {
        int start = node.getSourceStart();
        int end = start + node.getSourceLength();
        if (start >= startOffset && end <= endOffset) {
          Element targetSymbol = null;
          if (node instanceof DartIdentifier) {
            targetSymbol = node.getTargetSymbol();
            if (targetSymbol == null) {
              DartNode parent = node.getParent();
              if (parent instanceof DartTypeNode) {
                Type type = DartAstUtilities.getType((DartTypeNode) parent);
                if (type != null) {
                  targetSymbol = type.getElement();
                }
              } else if (parent instanceof DartMethodInvocation) {
                DartMethodInvocation invocation = (DartMethodInvocation) parent;
                if (node == invocation.getFunctionName()) {
                  targetSymbol = (Element) invocation.getTargetSymbol();
                }
              }
            }
          }
          if (targetSymbol == null) {
            foundCandidate = null;
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
                    foundCandidate = BindingUtils.getDartElement(compilationUnit.getLibrary(),
                        containingType.getSymbol());
                    candidateRegion = new Region(parameterName.getSourceStart(),
                        parameterName.getSourceLength());
                  } else {
                    foundCandidate = null;
                  }
                } else {
                  foundCandidate = BindingUtils.getDartElement(compilationUnit.getLibrary(),
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
                  foundCandidate = BindingUtils.getDartElement(compilationUnit.getLibrary(),
                      containingType.getSymbol());
                  candidateRegion = new Region(variableName.getSourceStart(),
                      variableName.getSourceLength());
                } else {
                  foundCandidate = null;
                }
              } else {
                foundCandidate = null;
              }
            } else {
              foundCandidate = BindingUtils.getDartElement(compilationUnit.getLibrary(),
                  targetSymbol);
              if (foundCandidate instanceof SourceReference) {
                try {
                  SourceRange range = ((SourceReference) foundCandidate).getNameRange();
                  candidateRegion = new Region(range.getOffset(), range.getLength());
                } catch (DartModelException exception) {
                  // Ignored
                }
              }
            }
          }
          throw new HyperlinkCandidateFoundException();
        }
      }
      return null;
    }

    @Override
    public Void visitNode(DartNode node) {
      node.accept(childVisitor);
      return null;
    }
  }

  /**
   * The id of the detect hyperlinks operation.
   */
  private static final String DETECT_LINKS_ID = DartToolsPlugin.PLUGIN_ID + ".hyperlinkDetection";

  @Override
  public final IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    PerformanceManager.Timer timer = PerformanceManager.getInstance().start(DETECT_LINKS_ID);
    try {
      return internalDetectHyperlinks(textViewer, region, canShowMultipleHyperlinks);
    } finally {
      timer.end();
    }
  }

  private IHyperlink[] internalDetectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    ITextEditor textEditor = (ITextEditor) getAdapter(ITextEditor.class);
    if (region == null || !(textEditor instanceof DartEditor)) {
      return null;
    }

    IAction openAction = textEditor.getAction("OpenEditor"); //$NON-NLS-1$
    if (!(openAction instanceof SelectionDispatchAction)) {
      return null;
    }

    int offset = region.getOffset();

    CompilationUnit input = (CompilationUnit) EditorUtility.getEditorInputJavaElement(textEditor,
        false);
    if (input == null) {
      return null;
    }

    // get the possibly hyperlink word region
    IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
    IRegion wordRegion = DartWordFinder.findWord(document, offset);
    if (wordRegion == null) {
      return null;
    }
    //
    // Search the AST for the word region to determine if it is a candidate for a link.
    //
    DartEditor dartEditor = (DartEditor) textEditor;
    DartUnit ast = dartEditor.getAST();
    if (ast != null) {
      int start = wordRegion.getOffset();
      int end = start + wordRegion.getLength();
      final HyperlinkCandidateVisitor visitor = new HyperlinkCandidateVisitor(input, start, end);
      try {
        ast.accept(visitor);
      } catch (HyperlinkCandidateVisitor.HyperlinkCandidateFoundException e) {
        // just means the visiting has been cut off early
      }
      if (visitor.foundCandidate != null) {
        if (visitor.candidateRegion != null) {
          return new IHyperlink[] {new DartElementHyperlink(visitor.foundCandidate, wordRegion,
              new OpenAction(dartEditor) {
                @Override
                protected void selectInEditor(IEditorPart part, DartElement element) {
                  EditorUtility.revealInEditor(part, visitor.candidateRegion.getOffset(),
                      visitor.candidateRegion.getLength());
                }
              })};
        }
        return new IHyperlink[] {new DartElementHyperlink(visitor.foundCandidate, wordRegion,
            (SelectionDispatchAction) openAction)};
      }
    }

    return null;
  }
}
