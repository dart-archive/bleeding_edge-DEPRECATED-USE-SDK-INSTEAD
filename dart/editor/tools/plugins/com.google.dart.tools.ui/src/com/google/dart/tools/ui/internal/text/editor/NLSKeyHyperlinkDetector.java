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

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.AccessorClassReference;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.NLSHintHelper;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * NLS hyperlink detector.
 */
public class NLSKeyHyperlinkDetector extends AbstractHyperlinkDetector {

  /*
   * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org
   * .eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
   */
  @Override
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    ITextEditor textEditor = (ITextEditor) getAdapter(ITextEditor.class);
    if (region == null || textEditor == null || canShowMultipleHyperlinks) {
      return null;
    }

    IEditorSite site = textEditor.getEditorSite();
    if (site == null) {
      return null;
    }

    DartElement javaElement = getInputJavaElement(textEditor);
    if (javaElement == null) {
      return null;
    }

    DartUnit ast = DartToolsPlugin.getDefault().getASTProvider().getAST(javaElement,
        ASTProvider.WAIT_NO, null);
    if (ast == null) {
      return null;
    }

    DartNode node = NodeFinder.perform(ast, region.getOffset(), 1);
    if (!(node instanceof DartStringLiteral) && !(node instanceof DartIdentifier)) {
      return null;
    }

//    if (node.getLocationInParent() == QualifiedName.QUALIFIER_PROPERTY)
//      return null;

    IRegion nlsKeyRegion = new Region(node.getStartPosition(), node.getLength());
    AccessorClassReference ref = NLSHintHelper.getAccessorClassReference(ast, nlsKeyRegion);
    if (ref == null) {
      return null;
    }
    String keyName = null;
    if (node instanceof DartStringLiteral) {
      keyName = ((DartStringLiteral) node).getValue();
    } else {
      keyName = ((DartIdentifier) node).getTargetName();
    }
    if (keyName != null) {
      return new IHyperlink[] {new NLSKeyHyperlink(nlsKeyRegion, keyName, ref, textEditor)};
    }

    return null;
  }

  private DartElement getInputJavaElement(ITextEditor editor) {
    IEditorInput editorInput = editor.getEditorInput();

    if (editor instanceof CompilationUnitEditor) {
      return DartToolsPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput);
    }

    return null;
  }

}
