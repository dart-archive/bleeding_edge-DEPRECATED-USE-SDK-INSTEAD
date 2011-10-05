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
package com.google.dart.tools.ui.internal.libraryview;

import com.google.dart.tools.core.internal.refactoring.util.RefactoringUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.SelectionDispatchAction;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.ResourceTransfer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An action to copy some set of selected contents onto the OS clipboard.
 * <p>
 * Originally copied out of
 * <code>org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction</code>. Note that
 * "CopyAction" refers to an action which knows ahead of time where a resource is being copied from
 * and to, inside of an Eclipse workspace, think "Move...". The copy, or CTRL-C, is known in Eclipse
 * as "CopyToClipboardAction".
 * <p>
 * TODO currently the elements are copied as a resource location or file name, but not as a
 * DartElement. See incomplete methods
 * {@link ClipboardCopier#createDataArray(IResource[], DartElement[], String[], String, TypedSource[])}
 * and
 * {@link ClipboardCopier#createDataTypeArray(IResource[], DartElement[], String[], TypedSource[])}.
 */
public final class CopyToClipboardAction extends SelectionDispatchAction {

  private static class ClipboardCopier {

    /**
     * OS specific line separator.
     */
    private static String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

    private static void addFileName(Set<String> fileNames, IResource resource) {
      if (resource == null) {
        return;
      }
      IPath location = resource.getLocation();
      if (location != null) {
        fileNames.add(location.toOSString());
      } else {
        // not a file system path. skip file.
      }
    }

    private static void addFileNames(Set<String> fileNames, IResource[] resources) {
      for (int i = 0; i < resources.length; i++) {
        addFileName(fileNames, resources[i]);
      }
    }

    private static Object[] createDataArray(IResource[] resources, DartElement[] dartElements,
        String[] fileNames, String names, TypedSource[] typedSources) {
      List<Object> result = new ArrayList<Object>(4);
      if (resources.length != 0) {
        result.add(resources);
      }
      // TODO see comment in type javadoc, need to implement this case
//      if (dartElements.length != 0) {
//        result.add(dartElements);
//      }
      if (fileNames.length != 0) {
        result.add(fileNames);
      }
      // TODO see comment in type javadoc, need to implement this case
//      if (typedSources.length != 0) {
//        result.add(typedSources);
//      }
      result.add(names);
      return result.toArray();
    }

    private static Transfer[] createDataTypeArray(IResource[] resources,
        DartElement[] dartElements, String[] fileNames, TypedSource[] typedSources) {
      List<ByteArrayTransfer> result = new ArrayList<ByteArrayTransfer>(4);
      if (resources.length != 0) {
        result.add(ResourceTransfer.getInstance());
      }
      // TODO see comment in type javadoc, need to implement this case
//      if (dartElements.length != 0) {
//      result.add(JavaElementTransfer.getInstance());
//      }
      if (fileNames.length != 0) {
        result.add(FileTransfer.getInstance());
      }
      // TODO see comment in type javadoc, need to implement this case
//      if (typedSources.length != 0) {
//        result.add(TypedSourceTransfer.getInstance());
//      }
      result.add(TextTransfer.getInstance());
      return result.toArray(new Transfer[result.size()]);
    }

    private static ILabelProvider createLabelProvider() {
      return new DartElementLabelProvider();
    }

    private static DartElement[] getCompilationUnits(DartElement[] dartElements) {
      List<?> cus = RefactoringUtils.getElementsOfType(dartElements, CompilationUnit.class);
      return cus.toArray(new CompilationUnit[cus.size()]);
    }

    private final boolean autoRepeatOnFailure;

    private final IResource[] resources;

    private final DartElement[] dartElements;

    private final Shell shell;

    private final ILabelProvider labelProvider;

    private ClipboardCopier(IResource[] resources, DartElement[] dartElements, Shell shell,
        boolean autoRepeatOnFailure) {
      Assert.isNotNull(resources);
      Assert.isNotNull(dartElements);
      Assert.isNotNull(shell);
      this.resources = resources;
      this.dartElements = dartElements;
      this.shell = shell;
      this.labelProvider = createLabelProvider();
      this.autoRepeatOnFailure = autoRepeatOnFailure;
    }

    public void copyToClipboard(Clipboard clipboard) throws CoreException {
      StringBuffer namesBuf = new StringBuffer();
      int resourceCount = resources.length + dartElements.length;

      //Set<String> fileNames
      Set<String> fileNames = new HashSet<String>(resourceCount);
      processResources(fileNames, namesBuf);
      processDartElements(fileNames, namesBuf);

      // TODO future improvement, this catches more selected resources:
//      IType[] mainTypes = ReorgUtils.getMainTypes(dartElements);
//      ICompilationUnit[] cusOfMainTypes = ReorgUtils.getCompilationUnits(mainTypes);
//      IResource[] resourcesOfMainTypes = ReorgUtils.getResources(cusOfMainTypes);
//      addFileNames(fileNames, resourcesOfMainTypes);

      IResource[] cuResources = RefactoringUtils.getResources(getCompilationUnits(dartElements));
      addFileNames(fileNames, cuResources);

      IResource[] resourcesForClipboard = resources;
      // TODO future improvement, this catches more selected resources:
      //CCPUtils.union(resources,
      //CCPUtils.union(cuResources, resourcesOfMainTypes));
      DartElement[] dartElementsForClipboard = dartElements;
      // TODO future improvement, this catches more selected resources:
      // DartElement[] dartElementsForClipboard = CCPUtils.union(dartElements, cusOfMainTypes);

      TypedSource[] typedSources = TypedSource.createTypedSources(dartElementsForClipboard);
      String[] fileNameArray = fileNames.toArray(new String[fileNames.size()]);
      copyToClipboard(resourcesForClipboard, fileNameArray, namesBuf.toString(),
          dartElementsForClipboard, typedSources, 0, clipboard);
    }

    private void copyToClipboard(IResource[] resources, String[] fileNames, String names,
        DartElement[] dartElements, TypedSource[] typedSources, int repeat, Clipboard clipboard) {
      final int repeat_max_count = 10;
      try {
        clipboard.setContents(
            createDataArray(resources, dartElements, fileNames, names, typedSources),
            createDataTypeArray(resources, dartElements, fileNames, typedSources));
      } catch (SWTError e) {
        if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD || repeat >= repeat_max_count) {
          throw e;
        }
        if (autoRepeatOnFailure) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e1) {
            // do nothing.
          }
        }
        if (autoRepeatOnFailure
            || MessageDialog.openQuestion(shell, CCPMessages.CopyToClipboardAction_problem,
                CCPMessages.CopyToClipboardAction_acessProblem)) {
          copyToClipboard(resources, fileNames, names, dartElements, typedSources, repeat + 1,
              clipboard);
        }
      }
    }

    private String getName(DartElement element) {
      return TextProcessor.deprocess(labelProvider.getText(element));
    }

    private String getName(IResource resource) {
      return TextProcessor.deprocess(labelProvider.getText(resource));
    }

    private void processDartElements(Set<String> fileNames, StringBuffer namesBuf) {
      for (int i = 0; i < dartElements.length; i++) {
        DartElement element = dartElements[i];
        switch (element.getElementType()) {
          case DartElement.DART_PROJECT:
          case DartElement.LIBRARY:
          case DartElement.DART_LIBRARY_FOLDER:
          case DartElement.COMPILATION_UNIT:
            addFileName(fileNames, RefactoringUtils.getResource(element));
            break;
          default:
            break;
        }

        if (namesBuf.length() > 0) {
          namesBuf.append(LINE_SEPARATOR);
        }
        namesBuf.append(getName(element));
      }
    }

    private void processResources(Set<String> fileNames, StringBuffer namesBuf) {
      for (int i = 0; i < resources.length; i++) {
        IResource resource = resources[i];
        addFileName(fileNames, resource);

        if (namesBuf.length() > 0) {
          namesBuf.append(LINE_SEPARATOR);
        }
        namesBuf.append(getName(resource));
      }
    }
  }

  private static class CopyToClipboardEnablementPolicy {
    private static boolean canCopyToClipboard(DartElement element) {
      return element != null && element.exists();
    }

    private static boolean canCopyToClipboard(IResource resource) {
      return resource != null && resource.exists() && !resource.isPhantom()
          && resource.getType() != IResource.ROOT;
    }

    private final IResource[] resources;

    private final DartElement[] dartElements;

    public CopyToClipboardEnablementPolicy(IResource[] resources, DartElement[] dartElements) {
      Assert.isNotNull(resources);
      Assert.isNotNull(dartElements);
      this.resources = resources;
      this.dartElements = dartElements;
    }

    public boolean canEnable() {
      if (resources.length + dartElements.length == 0) {
        return false;
      }
      if (hasProjects() && hasNonProjects()) {
        return false;
      }
      if (!canCopyAllToClipboard()) {
        return false;
      }
//      if (!new ParentChecker(resources, dartElements).haveCommonParent()) {
//        return false;
//      }
      return true;
    }

    private boolean canCopyAllToClipboard() {
      for (int i = 0; i < resources.length; i++) {
        if (!canCopyToClipboard(resources[i])) {
          return false;
        }
      }
      for (int i = 0; i < dartElements.length; i++) {
        if (!canCopyToClipboard(dartElements[i])) {
          return false;
        }
      }
      return true;
    }

    private boolean hasNonProjects() {
      for (int i = 0; i < resources.length; i++) {
        if (!(resources[i] instanceof IProject)) {
          return true;
        }
      }
      for (int i = 0; i < dartElements.length; i++) {
        if (!(dartElements[i] instanceof DartProject)) {
          return true;
        }
      }
      return false;
    }

    private boolean hasProjects() {
      for (int i = 0; i < resources.length; i++) {
        if (resources[i] instanceof IProject) {
          return true;
        }
      }
      for (int i = 0; i < dartElements.length; i++) {
        if (dartElements[i] instanceof DartProject) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * The id of this action.
   */
  public static final String ID = DartToolsPlugin.PLUGIN_ID + ".CopyAction"; //$NON-NLS-1$

  private static ISharedImages getWorkbenchSharedImages() {
    return DartToolsPlugin.getDefault().getWorkbench().getSharedImages();
  }

  private boolean autoRepeatOnFailure;

  private final Clipboard clipboard;

  public CopyToClipboardAction(IWorkbenchSite site) {
    this(site, null);
  }

  public CopyToClipboardAction(IWorkbenchSite site, Clipboard clipboard) {
    super(site);
    setId(ID);
    setText(CCPMessages.CopyToClipboardAction_text);
    setDescription(CCPMessages.CopyToClipboardAction_description);
    this.clipboard = clipboard;
    ISharedImages workbenchImages = getWorkbenchSharedImages();
    setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
    setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
    setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
    update(getSelection());
  }

  @Override
  public void run(IStructuredSelection selection) {
    try {
      List<?> elements = selection.toList();
      IResource[] resources = RefactoringUtils.getResources(elements);
      DartElement[] dartElements = RefactoringUtils.getDartElements(elements);
      if (elements.size() == resources.length + dartElements.length
          && canEnable(resources, dartElements)) {
        doRun(resources, dartElements);
      }
    } catch (CoreException e) {
      ExceptionHandler.handle(e, getShell(), CCPMessages.CopyToClipboardAction_error,
          CCPMessages.CopyToClipboardAction_internalError);
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    List<?> elements = selection.toList();
    IResource[] resources = RefactoringUtils.getResources(elements);
    DartElement[] dartElements = RefactoringUtils.getDartElements(elements);
    if (elements.size() != resources.length + dartElements.length) {
      setEnabled(false);
    } else {
      setEnabled(canEnable(resources, dartElements));
    }
  }

  public void setAutoRepeatOnFailure(boolean autorepeatOnFailure) {
    this.autoRepeatOnFailure = autorepeatOnFailure;
  }

  private boolean canEnable(IResource[] resources, DartElement[] dartElements) {
    return new CopyToClipboardEnablementPolicy(resources, dartElements).canEnable();
  }

  private void doRun(IResource[] resources, DartElement[] dartElements) throws CoreException {
    ClipboardCopier copier = new ClipboardCopier(resources, dartElements, getShell(),
        autoRepeatOnFailure);

    if (clipboard != null) {
      copier.copyToClipboard(clipboard);
    } else {
      Clipboard clipboard = new Clipboard(getShell().getDisplay());
      try {
        copier.copyToClipboard(clipboard);
      } finally {
        clipboard.dispose();
      }
    }
  }
}
