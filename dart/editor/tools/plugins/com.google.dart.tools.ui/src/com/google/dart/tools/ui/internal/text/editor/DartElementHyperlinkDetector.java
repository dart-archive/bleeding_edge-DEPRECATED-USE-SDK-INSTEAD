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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.tools.core.utilities.performance.PerformanceManager;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.InstrumentedSelectionDispatchAction;
import com.google.dart.tools.ui.actions.OpenAction;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Dart element hyperlink detector.
 */
public class DartElementHyperlinkDetector extends AbstractHyperlinkDetector {
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
    DartEditor editor = (DartEditor) getAdapter(ITextEditor.class);
    if (region == null || !(editor instanceof DartEditor)) {
      return null;
    }

    IAction openAction = editor.getAction("OpenEditor"); //$NON-NLS-1$
    if (!(openAction instanceof InstrumentedSelectionDispatchAction)) {
      return null;
    }

    com.google.dart.engine.ast.CompilationUnit cu = editor.getInputUnit();

    int offset = region.getOffset();

    ASTNode node = new NodeLocator(offset, offset + region.getLength()).searchWithin(cu);
    if (node == null || node instanceof com.google.dart.engine.ast.CompilationUnit
        || node instanceof Directive || node instanceof ClassDeclaration) {
      return null;
    }

    Element element = ElementLocator.locate(node);

    if (element != null) {
      IRegion wordRegion = new Region(node.getOffset(), node.getLength());
      return new IHyperlink[] {new DartElementHyperlink(element, wordRegion, new OpenAction(editor))};
    }

    return null;

  }

}
