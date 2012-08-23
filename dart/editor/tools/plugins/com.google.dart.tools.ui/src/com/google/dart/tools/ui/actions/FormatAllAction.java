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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.actions.WorkbenchRunnableAdapter;
import com.google.dart.tools.ui.internal.dialogs.OptionalMessageDialog;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.comment.CommentFormattingContext;
import com.google.dart.tools.ui.internal.text.comment.CommentFormattingStrategy;
import com.google.dart.tools.ui.internal.text.dart.DartFormattingStrategy;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.internal.util.Resources;
import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Formats the code of the compilation units contained in the selection.
 * <p>
 * The action is applicable to selections containing elements of type <code>CompilationUnit</code>,
 * <code>DartLibrary </code>, and <code>DartProject</code>.
 * </p>
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class FormatAllAction extends SelectionDispatchAction {
  /*
   * (non-Javadoc) Class implements IObjectActionDelegate
   */
  public static class ObjectDelegate implements IObjectActionDelegate {
    private FormatAllAction fAction;

    @Override
    public void run(IAction action) {
      fAction.run();
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
      if (fAction == null) {
        action.setEnabled(false);
      }
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
      fAction = new FormatAllAction(targetPart.getSite());
    }
  }

  private static Map<Object, Object> getFomatterSettings(DartProject project) {
    return new HashMap<Object, Object>(project.getOptions(true));
  }

  private DocumentRewriteSession fRewriteSession;

  /**
   * Creates a new <code>FormatAllAction</code>. The action requires that the selection provided by
   * the site's selection provider is of type <code>
   * org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the site providing context information for this action
   */
  public FormatAllAction(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.FormatAllAction_label);
    setToolTipText(ActionMessages.FormatAllAction_tooltip);
    setDescription(ActionMessages.FormatAllAction_description);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.FORMAT_ALL);
  }

  /*
   * (non-Javadoc) Method declared on SelectionDispatchAction.
   */
  @Override
  public void run(IStructuredSelection selection) {
    CompilationUnit[] cus = getCompilationUnits(selection);
    if (cus.length == 0) {
      MessageDialog.openInformation(
          getShell(),
          ActionMessages.FormatAllAction_EmptySelection_title,
          ActionMessages.FormatAllAction_EmptySelection_description);
      return;
    }
    try {
      if (cus.length == 1) {
        DartUI.openInEditor(cus[0]);
      } else {
        int returnCode = OptionalMessageDialog.open("FormatAll", //$NON-NLS-1$
            getShell(),
            ActionMessages.FormatAllAction_noundo_title,
            null,
            ActionMessages.FormatAllAction_noundo_message,
            MessageDialog.WARNING,
            new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL},
            0);
        if (returnCode != OptionalMessageDialog.NOT_SHOWN && returnCode != Window.OK) {
          return;
        }
      }
    } catch (CoreException e) {
      ExceptionHandler.handle(
          e,
          getShell(),
          ActionMessages.FormatAllAction_error_title,
          ActionMessages.FormatAllAction_error_message);
    }
    runOnMultiple(cus);
  }

  /*
   * (non-Javadoc) Method declared on SelectionDispatchAction.
   */
  @Override
  public void run(ITextSelection selection) {
  }

  /**
   * Perform format all on the given compilation units.
   * 
   * @param cus The compilation units to format.
   */
  public void runOnMultiple(final CompilationUnit[] cus) {
    try {
      final MultiStatus status = new MultiStatus(
          DartUI.ID_PLUGIN,
          IStatus.OK,
          ActionMessages.FormatAllAction_status_description,
          null);

      IStatus valEditStatus = Resources.makeCommittable(getResources(cus), getShell());
      if (valEditStatus.matches(IStatus.CANCEL)) {
        return;
      }
      status.merge(valEditStatus);
      if (!status.matches(IStatus.ERROR)) {
        PlatformUI.getWorkbench().getProgressService().run(
            true,
            true,
            new WorkbenchRunnableAdapter(new IWorkspaceRunnable() {
              @Override
              public void run(IProgressMonitor monitor) {
                doRunOnMultiple(cus, status, monitor);
              }
            })); // workspace lock
      }
      if (!status.isOK()) {
        String title = ActionMessages.FormatAllAction_multi_status_title;
        ErrorDialog.openError(getShell(), title, null, status);
      }
    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(
          e,
          getShell(),
          ActionMessages.FormatAllAction_error_title,
          ActionMessages.FormatAllAction_error_message);
    } catch (InterruptedException e) {
      // Canceled by user
    }
  }

  /*
   * (non-Javadoc) Method declared on SelectionDispatchAction.
   */
  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(isEnabled(selection));
  }

  /*
   * (non-Javadoc) Method declared on SelectionDispatchAction.
   */
  @Override
  public void selectionChanged(ITextSelection selection) {
    // do nothing
  }

//  private void collectCompilationUnits(IPackageFragment pack, Collection result)
//      throws JavaScriptModelException {
//    result.addAll(Arrays.asList(pack.getJavaScriptUnits()));
//  }
//
//  private void collectCompilationUnits(IPackageFragmentRoot root,
//      Collection result) throws JavaScriptModelException {
//    if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
//      IJavaScriptElement[] children = root.getChildren();
//      for (int i = 0; i < children.length; i++) {
//        collectCompilationUnits((IPackageFragment) children[i], result);
//      }
//    }
//  }

  private void doFormat(IDocument document, Map<Object, Object> options) {
    final IFormattingContext context = new CommentFormattingContext();
    try {
      context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, options);
      context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.valueOf(true));

      final MultiPassContentFormatter formatter = new MultiPassContentFormatter(
          DartPartitions.DART_PARTITIONING,
          IDocument.DEFAULT_CONTENT_TYPE);

      formatter.setMasterStrategy(new DartFormattingStrategy());
      formatter.setSlaveStrategy(new CommentFormattingStrategy(), DartPartitions.DART_DOC);
      formatter.setSlaveStrategy(
          new CommentFormattingStrategy(),
          DartPartitions.DART_SINGLE_LINE_DOC);
      formatter.setSlaveStrategy(
          new CommentFormattingStrategy(),
          DartPartitions.DART_SINGLE_LINE_COMMENT);
      formatter.setSlaveStrategy(
          new CommentFormattingStrategy(),
          DartPartitions.DART_MULTI_LINE_COMMENT);

      try {
        startSequentialRewriteMode(document);
        formatter.format(document, context);
      } finally {
        stopSequentialRewriteMode(document);
      }
    } finally {
      context.dispose();
    }
  }

  private void doRunOnMultiple(CompilationUnit[] cus, MultiStatus status, IProgressMonitor monitor)
      throws OperationCanceledException {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }
    monitor.setTaskName(ActionMessages.FormatAllAction_operation_description);

    monitor.beginTask("", cus.length * 4); //$NON-NLS-1$
    try {
      Map<Object, Object> lastOptions = null;
      DartProject lastProject = null;

      for (int i = 0; i < cus.length; i++) {
        CompilationUnit cu = cus[i];
        IPath path = cu.getPath();
        if (lastProject == null || !lastProject.equals(cu.getDartProject())) {
          lastProject = cu.getDartProject();
          lastOptions = getFomatterSettings(lastProject);
        }
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        if (cu.getResource().getResourceAttributes().isReadOnly()) {
          String message = Messages.format(
              ActionMessages.FormatAllAction_read_only_skipped,
              path.toString());
          status.add(new Status(IStatus.WARNING, DartUI.ID_PLUGIN, IStatus.WARNING, message, null));
          continue;
        }

        ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
        try {
          try {
            manager.connect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));

            monitor.subTask(path.makeRelative().toString());
            ITextFileBuffer fileBuffer = manager.getTextFileBuffer(path, LocationKind.IFILE);

            formatCompilationUnit(fileBuffer, lastOptions);

            if (fileBuffer.isDirty() && !fileBuffer.isShared()) {
              fileBuffer.commit(new SubProgressMonitor(monitor, 2), false);
            } else {
              monitor.worked(2);
            }
          } finally {
            manager.disconnect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
          }
        } catch (CoreException e) {
          String message = Messages.format(
              ActionMessages.FormatAllAction_problem_accessing,
              new String[] {path.toString(), e.getLocalizedMessage()});
          status.add(new Status(IStatus.WARNING, DartUI.ID_PLUGIN, IStatus.WARNING, message, e));
        }
      }
    } finally {
      monitor.done();
    }
  }

  private void formatCompilationUnit(final ITextFileBuffer fileBuffer,
      final Map<Object, Object> options) {
    if (fileBuffer.isShared()) {
      getShell().getDisplay().syncExec(new Runnable() {
        @Override
        public void run() {
          doFormat(fileBuffer.getDocument(), options);
        }
      });
    } else {
      doFormat(fileBuffer.getDocument(), options); // run in context thread
    }
  }

  private CompilationUnit[] getCompilationUnits(IStructuredSelection selection) {
    HashSet<DartElement> result = new HashSet<DartElement>();
    Object[] selected = selection.toArray();
    for (int i = 0; i < selected.length; i++) {
      try {
        if (selected[i] instanceof DartElement) {
          DartElement elem = (DartElement) selected[i];
          if (elem.exists()) {
            switch (elem.getElementType()) {
              case DartElement.TYPE:
                if (elem.getParent().getElementType() == DartElement.COMPILATION_UNIT) {
                  result.add(elem.getParent());
                }
                break;
              case DartElement.COMPILATION_UNIT:
                result.add(elem);
                break;
              case DartElement.LIBRARY:
//                collectCompilationUnits((DartLibrary) elem, result);
                break;
              case DartElement.DART_PROJECT:
                DartElement[] roots = ((DartProject) elem).getChildren();
                for (int k = 0; k < roots.length; k++) {
//                  collectCompilationUnits(roots[k], result);
                }
                break;
            }
          }
        }
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
      }
    }
    return result.toArray(new CompilationUnit[result.size()]);
  }

  private IResource[] getResources(CompilationUnit[] cus) {
    IResource[] res = new IResource[cus.length];
    for (int i = 0; i < res.length; i++) {
      res[i] = cus[i].getResource();
    }
    return res;
  }

  private boolean isEnabled(IStructuredSelection selection) {
    Object[] selected = selection.toArray();
    for (int i = 0; i < selected.length; i++) {
      if (selected[i] instanceof DartElement) {
        DartElement elem = (DartElement) selected[i];
        if (elem.exists()) {
          switch (elem.getElementType()) {
            case DartElement.TYPE:
              // for browsing perspective
              return elem.getParent().getElementType() == DartElement.COMPILATION_UNIT;
            case DartElement.COMPILATION_UNIT:
              return true;
            case DartElement.LIBRARY:
              return true;
            case DartElement.DART_PROJECT:
              return true;
          }
        }
      }
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  private void startSequentialRewriteMode(IDocument document) {
    if (document instanceof IDocumentExtension4) {
      IDocumentExtension4 extension = (IDocumentExtension4) document;
      fRewriteSession = extension.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
    } else if (document instanceof IDocumentExtension) {
      IDocumentExtension extension = (IDocumentExtension) document;
      extension.startSequentialRewrite(false);
    }
  }

  @SuppressWarnings("deprecation")
  private void stopSequentialRewriteMode(IDocument document) {
    if (document instanceof IDocumentExtension4) {
      IDocumentExtension4 extension = (IDocumentExtension4) document;
      extension.stopRewriteSession(fRewriteSession);
    } else if (document instanceof IDocumentExtension) {
      IDocumentExtension extension = (IDocumentExtension) document;
      extension.stopSequentialRewrite();
    }
  }

}
