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

package com.google.dart.tools.ui.actions;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.problem.Problem;
import com.google.dart.tools.internal.corext.codemanipulation.OrganizeImportsOperation;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.actions.WorkbenchRunnableAdapter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import java.lang.reflect.InvocationTargetException;

/**
 * Organizes the import directives in a compilation unit
 */
public class OrganizeImportsAction extends SelectionDispatchAction {

  private static CompilationUnit getCompilationUnit(CompilationUnitEditor editor) {
    if (editor != null) {
      DartElement element = DartUI.getEditorInputDartElement(editor.getEditorInput());
      if (!(element instanceof CompilationUnit)) {
        return null;
      }
      return (CompilationUnit) element;
    }

    return null;
  }

  private CompilationUnitEditor editor;

  /** <code>true</code> if the query dialog is showing. */
  private boolean isQueryShowing = false;

  public OrganizeImportsAction(CompilationUnitEditor editor) {
    super(editor.getEditorSite());
    setText(ActionMessages.OrganizeImportsAction_label);
    PlatformUI.getWorkbench()
        .getHelpSystem().setHelp(this, DartHelpContextIds.ORGANIZE_IMPORTS_ACTION);
    this.editor = editor;
  }

  public OrganizeImportsAction(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.OrganizeImportsAction_label);
    PlatformUI.getWorkbench()
        .getHelpSystem().setHelp(this, DartHelpContextIds.ORGANIZE_IMPORTS_ACTION);
  }

  public void run(CompilationUnit cu) {

    CompilationUnitEditor cuEditor = null;
    if (editor != null) {
      cuEditor = editor;

      //TODO(keertip): organize imports from within editor -> editor has focus
//      if (!ElementValidator.check(cu, getShell(), ActionMessages.OrganizeImportsAction_error_title, true))
//        return;
//    } else {
      IEditorPart openEditor = EditorUtility.isOpenInEditor(cu);
      if (!(openEditor instanceof CompilationUnitEditor)) {
//        fCleanUpDelegate.run(new StructuredSelection(cu));
        return;
      }

      editor = (CompilationUnitEditor) openEditor;
      //TODO(keertip): organize imports from package explorer -> editor does not have focus
//      if (!ElementValidator.check(cu, getShell(), ActionMessages.OrganizeImportsAction_error_title, false))
//        return;
    }

    Assert.isNotNull(editor);
    // TODO(keertip): check if editable
//    if (!ActionUtil.isEditable(editor, getShell(), cu))
//      return;

    DartUnit astRoot = editor.getAST();

    OrganizeImportsOperation op = new OrganizeImportsOperation(
        cu,
        astRoot,
        !cu.isWorkingCopy(),
        false); //createChooseImportQuery(editor));

    IRewriteTarget target = (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);
    if (target != null) {
      target.beginCompoundChange();
    }

    IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
    IRunnableContext context = getSite().getWorkbenchWindow();
    if (context == null) {
      context = progressService;
    }
    IEditingSupport helper = createViewerHelper();
    try {
      registerHelper(helper, editor);
      progressService.runInUI(
          context,
          new WorkbenchRunnableAdapter(op, op.getScheduleRule()),
          op.getScheduleRule());
      Problem parseError = op.getParseError();
      if (parseError != null) {
        String message = NLS.bind(
            ActionMessages.OrganizeImportsAction_single_error_parse,
            parseError.getMessage());
        MessageDialog.openInformation(
            getShell(),
            ActionMessages.OrganizeImportsAction_error_title,
            message);
        if (parseError.getSourceStart() != -1) {
          editor.selectAndReveal(parseError.getSourceStart(), parseError.getSourceEnd()
              - parseError.getSourceStart() + 1);
        }
      } else {
        setStatusBarMessage(getOrganizeInfo(op), editor);
      }
    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(
          e,
          getShell(),
          ActionMessages.OrganizeImportsAction_error_title,
          ActionMessages.OrganizeImportsAction_error_message);
    } catch (InterruptedException e) {
    } finally {
      deregisterHelper(helper, editor);
      if (target != null) {
        target.endCompoundChange();
      }
    }

  }

  @Override
  public void run(DartTextSelection selection) {
    DartElement element = null;
    try {
      element = selection.resolveEnclosingElement();
    } catch (DartModelException e) {

      e.printStackTrace();
    }
    if (element != null && element instanceof CompilationUnit) {
      run((CompilationUnit) element);
    }
    super.run(selection);
  }

  @Override
  public void run(IStructuredSelection selection) {
    Object object = selection.getFirstElement();
    if (object != null && object instanceof CompilationUnit) {
      run((CompilationUnit) object);
    } else {
      super.run(selection);
    }
  }

  @Override
  public void run(ITextSelection selection) {
    CompilationUnit cu = getCompilationUnit(editor);
    if (cu != null) {
      run(cu);
    }
  }

  private IEditingSupport createViewerHelper() {
    return new IEditingSupport() {
        @Override
      public boolean isOriginator(DocumentEvent event, IRegion subjectRegion) {
        return true; // assume true, since we only register while we are active
      }

        @Override
      public boolean ownsFocusShell() {
        return isQueryShowing;
      }

    };
  }

  private void deregisterHelper(IEditingSupport helper, CompilationUnitEditor editor) {
    ISourceViewer viewer = editor.getViewer();
    if (viewer instanceof IEditingSupportRegistry) {
      IEditingSupportRegistry registry = (IEditingSupportRegistry) viewer;
      registry.unregister(helper);
    }
  }

  private String getOrganizeInfo(OrganizeImportsOperation op) {
    int nImportsAdded = op.getNumberOfImportsAdded();
    if (nImportsAdded >= 0) {
      if (nImportsAdded == 1) {
        return ActionMessages.OrganizeImportsAction_summary_added_singular;
      } else {
        return NLS.bind(
            ActionMessages.OrganizeImportsAction_summary_added_plural,
            String.valueOf(nImportsAdded));
      }
    } else {
      if (nImportsAdded == -1) {
        return ActionMessages.OrganizeImportsAction_summary_removed_singular;
      } else {
        return NLS.bind(
            ActionMessages.OrganizeImportsAction_summary_removed_plural,
            String.valueOf(-nImportsAdded));
      }
    }
  }

  private void registerHelper(IEditingSupport helper, CompilationUnitEditor editor) {
    ISourceViewer viewer = editor.getViewer();
    if (viewer instanceof IEditingSupportRegistry) {
      IEditingSupportRegistry registry = (IEditingSupportRegistry) viewer;
      registry.register(helper);
    }
  }

  private void setStatusBarMessage(String message, CompilationUnitEditor editor) {
    IStatusLineManager manager = editor.getEditorSite().getActionBars().getStatusLineManager();
    manager.setMessage(message);
  }

}
