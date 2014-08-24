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
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
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

  private Region getWordRegion(AstNode node) {
    if (node instanceof BinaryExpression) {
      Token operator = ((BinaryExpression) node).getOperator();
      return new Region(operator.getOffset(), operator.getLength());
    }
    return new Region(node.getOffset(), node.getLength());
  }

  private IHyperlink[] internalDetectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    int offset = region.getOffset();

    DartEditor editor = (DartEditor) getAdapter(ITextEditor.class);
    if (region == null || !(editor instanceof DartEditor)) {
      return null;
    }

    IAction openAction = editor.getAction("OpenEditor"); //$NON-NLS-1$
    if (!(openAction instanceof InstrumentedSelectionDispatchAction)) {
      return null;
    }

    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      String file = editor.getInputFilePath();
      if (file != null) {
        NavigationRegion[] navigationRegions = DartCore.getAnalysisServerData().getNavigation(file);
        for (NavigationRegion navigationRegion : navigationRegions) {
          if (navigationRegion.containsInclusive(offset)) {
            return new IHyperlink[] {new DartNavigationRegionHyperlink_NEW(
                editor.getInputResourceFile(),
                navigationRegion)};
          }
        }
      }
    } else {
      // Get the associated CU
      CompilationUnit cu = editor.getInputUnit();
      if (cu == null) {
        return null;
      }

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
        return new IHyperlink[] {new DartElementHyperlink_OLD(element, wordRegion, new OpenAction(
            editor))};
      }
    }

    return null;

  }

}
