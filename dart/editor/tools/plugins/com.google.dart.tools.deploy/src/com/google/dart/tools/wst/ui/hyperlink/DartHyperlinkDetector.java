/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.wst.ui.hyperlink;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.actions.InstrumentedSelectionDispatchAction;
import com.google.dart.tools.ui.internal.text.editor.DartElementHyperlink;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.wst.ui.DartReconcilerManager;
import com.google.dart.tools.wst.ui.EmbeddedDartReconcilerHook;
import com.google.dart.tools.wst.ui.StructuredTextViewerConfigurationDart;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;

@SuppressWarnings("restriction")
public class DartHyperlinkDetector extends AbstractHyperlinkDetector {

  private static class Opener extends InstrumentedSelectionDispatchAction {
    private Element element;
    private IStructuredDocumentRegion region;
    private ITextViewer viewer;

    Opener(Element element, IStructuredDocumentRegion region, ITextViewer viewer) {
      super(Workbench.getInstance().getActiveWorkbenchWindow());
      this.element = element;
      this.region = region;
      this.viewer = viewer;
    }

    @Override
    public void run(IStructuredSelection selection) {
      try {
        if (element.getContext().exists(element.getSource())) {
          DartUI.openInEditor(element);
        } else {
          // If the source is undefined we assume it is part of the script currently in the viewer.
          // This is valid because even with multiple scripts, each script is its own library.
          // There is no way to reference elements in a script from anywhere else.
          int elementOffset = element.getNameOffset() + region.getStartOffset();
          int elementLength = element.getDisplayName().length();
          viewer.setSelectedRange(elementOffset, elementLength);
          viewer.revealRange(elementOffset, elementLength);
        }
      } catch (Throwable e) {
        ExceptionHandler.handle(e, getText(), "Exception during open.");
      }
    }
  }

  private IStructuredDocumentRegion partition;

  public DartHyperlinkDetector() {
    super();
  }

  @Override
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    // Adapted from DartElementHyperlinkDetector.internalDetectHyperlinks()
    if (region == null) {
      return null;
    }

    CompilationUnit cu = getCompilationUnit(textViewer, region);
    if (cu == null) {
      return null;
    }

    int offset = region.getOffset() - partition.getStartOffset();

    AstNode node = new NodeLocator(offset, offset + region.getLength()).searchWithin(cu);
    if (node == null || node instanceof com.google.dart.engine.ast.CompilationUnit
        || node instanceof Directive || node instanceof Declaration
        || node instanceof InstanceCreationExpression || node instanceof PrefixExpression
        || node instanceof PostfixExpression || node instanceof ConditionalExpression) {
      return null;
    }

    Element element = ElementLocator.locateWithOffset(node, offset);
    if (element != null) {
      IRegion wordRegion = getWordRegion(node);
      return new IHyperlink[] {new DartElementHyperlink(element, wordRegion, new Opener(
          element,
          partition,
          textViewer))};
    }

    return null;
  }

  private CompilationUnit getCompilationUnit(ITextViewer textViewer, IRegion region) {
    int offset = region.getOffset();
    IDocument document = textViewer.getDocument();
    try {
      ITypedRegion part = document.getPartition(offset);
      if (!StructuredTextViewerConfigurationDart.DART_SCRIPT_PARTITION_NAME.equals(part.getType())) {
        return null;
      }
    } catch (BadLocationException ex) {
      return null;
    }
    partition = ((IStructuredDocument) document).getRegionAtCharacterOffset(offset);
    EmbeddedDartReconcilerHook reconciler = DartReconcilerManager.getInstance().reconcilerFor(
        document);
    if (waitForResolution(reconciler, partition.getStartOffset(), partition.getLength())) {
      CompilationUnit unit = reconciler.getResolvedUnit(
          partition.getStartOffset(),
          partition.getLength(),
          document);
      return unit;
    }
    return null;
  }

  private Region getWordRegion(AstNode node) {
    // Adapted from DartElementHyperlinkDetector.getWordRegion()
    if (node instanceof BinaryExpression) {
      Token operator = ((BinaryExpression) node).getOperator();
      return new Region(operator.getOffset() + partition.getStartOffset(), operator.getLength());
    }
    return new Region(node.getOffset() + partition.getStartOffset(), node.getLength());
  }

  private boolean waitForResolution(final EmbeddedDartReconcilerHook reconciler, final int offset,
      final int length) {
    if (reconciler.isResolved(offset, length)) {
      return true;
    }
    // Kick off resolution.
    new Thread() {
      @Override
      public void run() {
        reconciler.validate(new Region(offset, length), null, null);
      }
    }.start();
    // But don't wait forever for it to finish, since this runs on the UI thread.
    // Adapted from DartContentAssistInvocationContext.waitAssistContext()
    long endTime = System.currentTimeMillis() + 500;
    while (System.currentTimeMillis() < endTime) {
      if (reconciler.isResolved(offset, length)) {
        return true;
      }
      ExecutionUtils.sleep(5);
    }
    return false;
  }
}
