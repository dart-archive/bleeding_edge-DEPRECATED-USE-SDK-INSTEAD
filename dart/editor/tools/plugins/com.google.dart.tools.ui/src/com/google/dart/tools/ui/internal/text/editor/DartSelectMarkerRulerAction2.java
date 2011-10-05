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

import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.VerticalRulerEvent;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.SelectAnnotationRulerAction;

import java.util.ResourceBundle;

/**
 * A special select marker ruler action which activates quick fix if clicked on a quick fixable
 * problem.
 */
public class DartSelectMarkerRulerAction2 extends SelectAnnotationRulerAction {

  public DartSelectMarkerRulerAction2(ResourceBundle bundle, String prefix, ITextEditor editor) {
    super(bundle, prefix, editor);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IJavaHelpContextIds.JAVA_SELECT_MARKER_RULER_ACTION);
  }

  /*
   * @see org.eclipse.ui.texteditor.IVerticalRulerListener#annotationDefaultSelected
   * (org.eclipse.ui.texteditor.VerticalRulerEvent)
   */
  @Override
  public void annotationDefaultSelected(VerticalRulerEvent event) {
    Annotation annotation = event.getSelectedAnnotation();
    IAnnotationModel model = getAnnotationModel();

    if (isOverrideIndicator(annotation)) {
      ((OverrideIndicatorManager.OverrideIndicator) annotation).open();
      return;
    }

    if (isBreakpoint(annotation)) {
      triggerAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK);
    }

    Position position = model.getPosition(annotation);
    if (position == null) {
      return;
    }

    if (isQuickFixTarget(annotation)) {
      ITextOperationTarget operation = (ITextOperationTarget) getTextEditor().getAdapter(
          ITextOperationTarget.class);
      final int opCode = ISourceViewer.QUICK_ASSIST;
      if (operation != null && operation.canDoOperation(opCode)) {
        getTextEditor().selectAndReveal(position.getOffset(), position.getLength());
        operation.doOperation(opCode);
        return;
      }
    }

    // default:
    super.annotationDefaultSelected(event);
  }

  /**
   * Checks whether the given annotation is a breakpoint annotation.
   * 
   * @param annotation
   * @return <code>true</code> if the annotation is a breakpoint annotation
   */
  private boolean isBreakpoint(Annotation annotation) {
    DartX.todo();
    return annotation.getType().equals("org.eclipse.debug.core.breakpoint") /*|| annotation.getType().equals(JavaExpandHover.NO_BREAKPOINT_ANNOTATION)*/; //$NON-NLS-1$
  }

  /**
   * Tells whether the given annotation is an override annotation.
   * 
   * @param annotation the annotation
   * @return <code>true</code> iff the annotation is an override annotation
   */
  private boolean isOverrideIndicator(Annotation annotation) {
    return annotation instanceof OverrideIndicatorManager.OverrideIndicator;
  }

  private boolean isQuickFixTarget(Annotation a) {
    DartX.todo();
    return false;
//    return JavaCorrectionProcessor.hasCorrections(a)
//        || a instanceof AssistAnnotation;
  }

  private void triggerAction(String actionID) {
    IAction action = getTextEditor().getAction(actionID);
    if (action != null) {
      if (action instanceof IUpdate) {
        ((IUpdate) action).update();
      }
      // hack to propagate line change
      if (action instanceof ISelectionListener) {
        ((ISelectionListener) action).selectionChanged(null, null);
      }
      if (action.isEnabled()) {
        action.run();
      }
    }
  }

}
