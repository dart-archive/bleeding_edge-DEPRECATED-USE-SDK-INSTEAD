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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.model.DartIgnoreEvent;
import com.google.dart.tools.core.model.DartIgnoreListener;
import com.google.dart.tools.core.pub.IPubUpdateListener;
import com.google.dart.tools.core.pub.PubManager;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.ProblemsLabelDecorator;
import com.google.dart.tools.ui.actions.CleanFoldersAction;
import com.google.dart.tools.ui.actions.CopyFilePathAction;
import com.google.dart.tools.ui.actions.DeleteAction;
import com.google.dart.tools.ui.actions.NewAppFromPackageAction;
import com.google.dart.tools.ui.actions.OpenAsTextAction;
import com.google.dart.tools.ui.actions.OpenExternalDartdocAction;
import com.google.dart.tools.ui.actions.OpenNewFileWizardAction;
import com.google.dart.tools.ui.actions.OpenNewFolderWizardAction;
import com.google.dart.tools.ui.actions.RunPubAction;
import com.google.dart.tools.ui.actions.ShowInFinderAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.CollapseAllAction;
import com.google.dart.tools.ui.internal.formatter.DartFormatter.FormatFileAction;
import com.google.dart.tools.ui.internal.handlers.OpenFolderHandler;
import com.google.dart.tools.ui.internal.projects.HideProjectAction;
import com.google.dart.tools.ui.internal.projects.OpenNewApplicationWizardAction;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import static com.google.dart.tools.core.DartCore.isDartLikeFileName;
import static com.google.dart.tools.core.DartCore.isHtmlLikeFileName;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.MoveResourceAction;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * File-oriented view for navigating Dart projects.
 */
@SuppressWarnings("deprecation")
public class FilesView extends ViewPart implements ISetSelectionTarget {

  private class OpenPubDocs extends Action {

    public OpenPubDocs() {
      setText("Find packages to include");
    }

    @Override
    public void run() {
      try {
        getSite().getPage().showView("com.google.dart.tools.ui.view.packages");
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      }
    }
  }

  private class PubUpdateListener implements IPubUpdateListener {
    @Override
    public void packagesUpdated(final IContainer container) {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          if (treeViewer != null) {
            IResource resource = container.findMember(DartCore.PACKAGES_DIRECTORY_NAME);
            if (resource != null && !treeViewer.getControl().isDisposed()) {
              treeViewer.refresh(resource);
            }
          }
        }
      });
    }

    @Override
    public void pubCacheChanged(final Map<String, Object> added) {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          if (treeViewer != null) {
            ResourceContentProvider provider = (ResourceContentProvider) treeViewer.getContentProvider();
            if (provider != null) {
              InstalledPackagesNode node = provider.getPackagesNode();
              node.updatePackages(added);
              treeViewer.refresh(node);
            }
          }
        }
      });
    }

  }

  public static final String VIEW_ID = "com.google.dart.tools.ui.FileExplorer"; // from plugin.xml

  private static final List<String> NON_TEXT_FILE_EXTENSIONS = Arrays.asList(DartCore.IMAGE_FILE_EXTENSIONS);

  /**
   * A constant for the Link with Editor memento.
   */
  private static final String LINK_WITH_EDITOR_ID = "linkWithEditor";

  //persistence tags
  private static final String TAG_ELEMENT = "element"; //$NON-NLS-1$
  private static final String TAG_EXPANDED = "expanded"; //$NON-NLS-1$
  private static final String TAG_PATH = "path"; //$NON-NLS-1$
  private static final String TAG_SELECTION = "selection"; //$NON-NLS-1$

  private static boolean allElementsAreProjects(IStructuredSelection selection) {
    for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
      Object selectedElement = iterator.next();
      if (!(selectedElement instanceof IProject)) {
        return false;
      }
    }
    return true;
  }

  private static boolean allElementsAreResources(IStructuredSelection selection) {
    for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
      Object selectedElement = iterator.next();
      if (!(selectedElement instanceof IResource)) {
        return false;
      }
    }
    return true;
  }

  private TreeViewer treeViewer;
  private IMemento memento;
  private LinkWithEditorAction linkWithEditorAction;
  private MoveResourceAction moveAction;
  private PropertyDialogAction propertyDialogAction;
  private RenameResourceAction renameAction;
//  private CleanUpAction cleanUpAction;
  private DeleteAction deleteAction;
  private OpenNewFileWizardAction createFileAction;
  private OpenNewFolderWizardAction createFolderAction;
  private OpenNewApplicationWizardAction createApplicationAction;

  private IgnoreResourceAction ignoreResourceAction;

  private EnableDartBuilderAction enableBuilderAction;

  private CopyFilePathAction copyFilePathAction;

  private FormatFileAction formatFileAction;

  private HideProjectAction hideContainerAction;
  private UndoRedoActionGroup undoRedoActionGroup;
  private RunPubAction pubUpdateAction;
  private RunPubAction pubInstallAction;
  private RunPubAction pubInstallOfflineAction;

  private RunPubAction pubDeployAction;

  private NewAppFromPackageAction copyPackageAction;
  private CleanFoldersAction cleanFoldersAction;
  private IPreferenceStore preferences;

  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  private IPubUpdateListener pubUpdateListener = new PubUpdateListener();

//  private RefreshAction refreshAction;

  private CopyAction copyAction;

  private PasteAction pasteAction;

  private Clipboard clipboard;

  private ResourceLabelProvider resourceLabelProvider;

  /**
   * Used to refresh view content when ignores are updated.
   */
  private DartIgnoreListener dartIgnoreListener;

  private OpenAsTextAction openAsTextAction;

  private OpenExternalDartdocAction browseDartDocAction;

  private ResourceContentProvider resourceContentProvider;

  @Override
  public void createPartControl(Composite parent) {
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    treeViewer = new TreeViewer(parent);
    treeViewer.setUseHashlookup(true);
    resourceContentProvider = new ResourceContentProvider();
    treeViewer.setContentProvider(resourceContentProvider);
    resourceLabelProvider = ResourceLabelProvider.createInstance();
    treeViewer.setLabelProvider(new DecoratingStyledCellLabelProvider(
        resourceLabelProvider,
        new ProblemsLabelDecorator(),
        null));
    treeViewer.setComparator(new FilesViewerComparator());
    treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        handleDoubleClick(event);
      }
    });
    treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
    treeViewer.getTree().setBackgroundMode(SWT.INHERIT_FORCE);
    treeViewer.getTree().addListener(SWT.EraseItem, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SWTUtil.eraseSelection(event, treeViewer.getTree(), getPreferences());
      }
    });

    initDragAndDrop();

    getSite().setSelectionProvider(treeViewer);

    makeActions();

    fillInToolbar(getViewSite().getActionBars().getToolBarManager());
    fillInActionBars();

    // Create the TreeViewer's context menu.
    createContextMenu();

    parent.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        linkWithEditorAction.syncSelectionToEditor();
      }
    });

    SWTUtil.bindJFaceResourcesFontToControl(treeViewer.getTree());
    getPreferences().addPropertyChangeListener(propertyChangeListener);
    updateColors();
    PubManager.getInstance().addListener(pubUpdateListener);

    restoreState();
  }

  @Override
  public void dispose() {
    if (linkWithEditorAction != null) {
      linkWithEditorAction.dispose();
    }
    if (undoRedoActionGroup != null) {
      undoRedoActionGroup.dispose();
    }
    if (copyFilePathAction != null) {
      treeViewer.removeSelectionChangedListener(copyFilePathAction);
    }

    if (clipboard != null) {
      clipboard.dispose();
    }

    if (dartIgnoreListener != null) {
      DartCore.removeIgnoreListener(dartIgnoreListener);
    }
    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }
    if (propertyDialogAction != null) {
      treeViewer.removeSelectionChangedListener(propertyDialogAction);
    }
    if (formatFileAction != null) {
      treeViewer.removeSelectionChangedListener(formatFileAction);
    }

    if (pubUpdateListener != null) {
      PubManager.getInstance().removeListener(pubUpdateListener);
    }

    resourceLabelProvider.dispose();

    super.dispose();
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);

    this.memento = memento;

    dartIgnoreListener = new DartIgnoreListener() {
      @Override
      public void ignoresChanged(DartIgnoreEvent event) {
        Display.getDefault().asyncExec(new Runnable() {
          @Override
          public void run() {
            if (!treeViewer.getControl().isDisposed()) {
              treeViewer.refresh();
            }
          }
        });
      }
    };

    DartCore.addIgnoreListener(dartIgnoreListener);
  }

  @Override
  public void saveState(IMemento memento) {
    memento.putBoolean(LINK_WITH_EDITOR_ID, linkWithEditorAction.getLinkWithEditor());

    //save expanded elements
    Object expandedElements[] = treeViewer.getVisibleExpandedElements();
    if (expandedElements.length > 0) {
      IMemento expandedMem = memento.createChild(TAG_EXPANDED);
      for (Object element : expandedElements) {
        if (element instanceof IResource) {
          IMemento elementMem = expandedMem.createChild(TAG_ELEMENT);
          elementMem.putString(TAG_PATH, ((IResource) element).getFullPath().toString());
        }
      }
    }

    //save selection
    Object elements[] = ((IStructuredSelection) treeViewer.getSelection()).toArray();
    if (elements.length > 0) {
      IMemento selectionMem = memento.createChild(TAG_SELECTION);
      for (Object element : elements) {
        if (element instanceof IResource) {
          IMemento elementMem = selectionMem.createChild(TAG_ELEMENT);
          elementMem.putString(TAG_PATH, ((IResource) element).getFullPath().toString());
        }
      }
    }
  }

  @Override
  public void selectReveal(ISelection selection) {
    treeViewer.setSelection(selection, true);
  }

  @Override
  public void setFocus() {
    treeViewer.getTree().setFocus();
  }

  protected void createContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager manager) {
        fillContextMenu(manager);
      }
    });
    Menu menu = menuMgr.createContextMenu(treeViewer.getTree());
    treeViewer.getTree().setMenu(menu);
    getSite().registerContextMenu(menuMgr, treeViewer);
  }

  protected void fillContextMenu(IMenuManager manager) {
    IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();

    // New File/ New Folder 

    if (allElementsAreResources(selection)) {
      manager.add(createFileAction);

    }
    if (selection.size() == 0 || selection.getFirstElement() instanceof IContainer) {
      manager.add(createFolderAction);
    }

    if (selection.size() == 0) {
      manager.add(createApplicationAction);
    }

    // OPEN GROUP

    if (manager.getItems().length > 0) {
      manager.add(new Separator());
    }

    if (selection.size() == 0) {
      manager.add(OpenFolderHandler.createCommandAction(getSite().getWorkbenchWindow()));
    }

    // Close folder action (aka Remove from Editor)
    if (!selection.isEmpty() && allElementsAreResources(selection)) {
      // Remove, iff non-empty selection, all elements are IResources
      if (allElementsAreProjects(selection)) {
        manager.add(hideContainerAction);
      }
    }

    boolean isPackagesDir = isPackagesDir(selection);
    boolean isFolder = isFolder(selection);

    // EDIT GROUP

    if (!selection.isEmpty() && allElementsAreResources(selection)) {

      manager.add(new Separator());

      if (!isFolder) {
        manager.add(copyAction);
      }

      // Copy File Path iff single element and is an IResource

      if (selection.size() == 1) {
        manager.add(copyFilePathAction);
      }

      if (!isFolder) {
        manager.add(pasteAction);
      }

      if (selection.size() == 1 && selection.getFirstElement() instanceof IFile) {
        String extension = FileUtilities.getExtension(((IResource) (selection.getFirstElement())).getName());
        if (!NON_TEXT_FILE_EXTENSIONS.contains(extension) && !Extensions.DART.equals(extension)) {
          manager.add(openAsTextAction);
        }
      }

      manager.add(new Separator());
//      manager.add(refreshAction);
      // reanalyze
      if (!selection.isEmpty() && allElementsAreProjects(selection)) {
        manager.add(cleanFoldersAction);
      }

      // REFACTOR GROUP

      manager.add(new Separator());

      if (selection.size() == 1) {

        if (!isPackagesDir && !isPubFile(selection.getFirstElement())) {
          manager.add(renameAction);
          manager.add(moveAction);
        }

      }

      if (!isPackagesDir) {

        // Only show this if it's enabled (if it's not the WST one will be there instead)
        if (formatFileAction.isEnabled()) {
          manager.add(new Separator());
          manager.add(formatFileAction);
        }

//        manager.add(cleanUpAction);
        manager.add(new Separator());

        boolean analysisTargets = true;
        for (Object elem : selection.toList()) {
          if (!isAnalyzable(elem)) {
            analysisTargets = false;
            break;
          }
        }

        if (analysisTargets) {
          ignoreResourceAction.updateLabel();
          manager.add(ignoreResourceAction);
        }

        if (enableBuilderAction.shouldBeEnabled()) {
          enableBuilderAction.updateLabel();
          manager.add(enableBuilderAction);
        }
      }

      manager.add(new Separator());
      manager.add(deleteAction);
      manager.add(new Separator());
    }

    manager.add(new Separator("additions"));

    if (selection.size() == 1 && selection.getFirstElement() instanceof IFile
        && isPubSpecFile(selection.getFirstElement())) {
      manager.add(pubInstallAction);
      manager.add(pubInstallOfflineAction);
      manager.add(pubUpdateAction);
      manager.add(pubDeployAction);
    }

    if (isPackagesDir) {
      manager.add(new Separator());
      manager.add(new OpenPubDocs());
    }

    if (allElementsAreResources(selection)) {
      manager.add(new Separator());
      manager.add(ShowInFinderAction.getInstance(null));
      manager.add(propertyDialogAction);
    }

    // Dart SDK

    if (selection.size() == 1 && !(selection.getFirstElement() instanceof IResource)
        && isInDartSdkNode(selection.getFirstElement())) {
      manager.add(browseDartDocAction);
    }

    if (selection.size() == 1 && selection.getFirstElement() instanceof DartPackageNode) {
      String name = ((DartPackageNode) selection.getFirstElement()).getLabel();
      copyPackageAction.setText(NLS.bind(FilesViewMessages.NewApplicationFromPackage_label, name));
      manager.add(copyPackageAction);
    }
  }

  protected void fillInToolbar(IToolBarManager toolbar) {
    // Link with Editor
    linkWithEditorAction = new LinkWithEditorAction(getViewSite().getPage(), treeViewer);

    if (memento != null && memento.getBoolean(LINK_WITH_EDITOR_ID) != null) {
      linkWithEditorAction.setLinkWithEditor(memento.getBoolean(LINK_WITH_EDITOR_ID).booleanValue());
    } else {
      linkWithEditorAction.setLinkWithEditor(true);
    }

    toolbar.add(linkWithEditorAction);

    // Collapse All
    toolbar.add(new CollapseAllAction(treeViewer));
  }

  protected void handleDoubleClick(DoubleClickEvent event) {

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("FilesView.handleDoubleClick");

    try {

      IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();

      instrumentation.record(selection);

      for (Object element : selection.toArray()) {
        if (treeViewer.isExpandable(element)) {
          treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element));
        }

        if (element instanceof IFile) {
          try {
            IFile file = (IFile) element;
            instrumentation.data("FileName", file.getName());

            String editorId = IDE.getEditorDescriptor(file).getId();
            boolean isTooComplex = false;
            if (DartUI.ID_CU_EDITOR.equals(editorId)) {
              // Gracefully degrade by opening a simpler text editor on too complex files.
              if (DartUI.isTooComplexDartFile(file)) {
                isTooComplex = true;
                instrumentation.metric("isTooComplexDartFile", true);
                editorId = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
              }
            }
            IEditorPart editor = getViewSite().getPage().openEditor(
                new FileEditorInput(file),
                editorId);
            if (isTooComplex) {
              DartUI.showTooComplexDartFileWarning(editor);
            }
          } catch (PartInitException e) {
            DartToolsPlugin.log(e);
          }
        } else if (element instanceof IFileStore) {
          try {

            IFileInfo info = ((IFileStore) element).fetchInfo();
            instrumentation.data("FileStoreName", info.getName());

            if (!info.isDirectory()) {
              IDE.openEditorOnFileStore(getViewSite().getPage(), (IFileStore) element);
            }

          } catch (PartInitException e) {
            DartToolsPlugin.log(e);
          }
        }
      }
    } catch (RuntimeException e) {
      instrumentation.metric("Exception", e.getClass().toString());
      instrumentation.data("Exception", e.toString());
      throw e;
    } finally {
      instrumentation.log();

    }
  }

  protected void restoreState() {
    if (memento == null) {
      return;
    }

    IContainer container = ResourcesPlugin.getWorkspace().getRoot();
    //restore expansion
    IMemento childMem = memento.getChild(TAG_EXPANDED);
    if (childMem != null) {
      List<Object> elements = new ArrayList<Object>();
      for (IMemento mem : childMem.getChildren(TAG_ELEMENT)) {
        Object element = container.findMember(mem.getString(TAG_PATH));
        if (element != null) {
          elements.add(element);
        }
      }
      treeViewer.setExpandedElements(elements.toArray());
    }
    //restore selection
    childMem = memento.getChild(TAG_SELECTION);
    if (childMem != null) {
      ArrayList<Object> list = new ArrayList<Object>();
      for (IMemento mem : childMem.getChildren(TAG_ELEMENT)) {
        Object element = container.findMember(mem.getString(TAG_PATH));
        if (element != null) {
          list.add(element);
        }
      }
      treeViewer.setSelection(new StructuredSelection(list));
    }
  }

  protected void updateColors() {
    SWTUtil.setColors(getViewer().getTree(), getPreferences());
  }

  Shell getShell() {
    return getSite().getShell();
  }

  TreeViewer getViewer() {
    return treeViewer;
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
    treeViewer.refresh(false);
  }

  private void fillInActionBars() {

    IActionBars actionBars = getViewSite().getActionBars();
    IUndoContext workspaceContext = (IUndoContext) ResourcesPlugin.getWorkspace().getAdapter(
        IUndoContext.class);
    undoRedoActionGroup = new UndoRedoActionGroup(getViewSite(), workspaceContext, true);
    undoRedoActionGroup.fillActionBars(actionBars);

    actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
    actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);
//    actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

  }

  private IPreferenceStore getPreferences() {
    return preferences;
  }

  private void initDragAndDrop() {
    int ops = DND.DROP_COPY | DND.DROP_MOVE;// | DND.DROP_LINK;
    Transfer[] transfers = new Transfer[] {
        LocalSelectionTransfer.getInstance(), ResourceTransfer.getInstance(),
        FileTransfer.getInstance(), PluginTransfer.getInstance()};
    treeViewer.addDragSupport(ops, transfers, new FilesViewDragAdapter(treeViewer));
    FilesViewDropAdapter adapter = new FilesViewDropAdapter(treeViewer);
    adapter.setFeedbackEnabled(true);
    treeViewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, adapter);
  }

  private boolean isAnalyzable(Object obj) {
    if (obj instanceof IContainer) {
      return true;
    }
    if (obj instanceof IFile) {
      String name = ((IFile) obj).getName();
      return isDartLikeFileName(name) || isHtmlLikeFileName(name);
    }
    return false;
  }

  private boolean isFolder(IStructuredSelection selection) {
    if (selection.isEmpty()) {
      return false;
    }

    Object resource = selection.getFirstElement();
    return resource instanceof IFolder;

  }

  private boolean isInDartSdkNode(Object selection) {
    while (selection != null) {
      if (selection instanceof DartLibraryNode || selection instanceof DartSdkNode) {
        return true;
      } else {
        selection = resourceContentProvider.getParent(selection);
      }
    }
    return false;

  }

  private boolean isPackagesDir(IStructuredSelection selection) {
    if (isFolder(selection)) {
      Object resource = selection.getFirstElement();
      return DartCore.isPackagesDirectory((IFolder) resource);
    }

    return false;
  }

  private boolean isPubFile(Object file) {
    if (!(file instanceof IResource)) {
      return false;
    }
    return isPubSpecFile(file)
        || ((IResource) file).getName().equals(DartCore.PUBSPEC_LOCK_FILE_NAME);
  }

  private boolean isPubSpecFile(Object file) {
    return ((IResource) file).getName().equals(DartCore.PUBSPEC_FILE_NAME);
  }

  private void makeActions() {
    createFileAction = new OpenNewFileWizardAction(getSite().getWorkbenchWindow());
    treeViewer.addSelectionChangedListener(createFileAction);
    createFolderAction = new OpenNewFolderWizardAction(getSite().getWorkbenchWindow());
    treeViewer.addSelectionChangedListener(createFolderAction);
    createApplicationAction = new OpenNewApplicationWizardAction();
    renameAction = new RenameResourceAction(getShell(), treeViewer.getTree()) {
      @Override
      public void run() {
        if (!RefactoringUtils.waitReadyForRefactoring2()) {
          return;
        }
        super.run();
      }
    };
    treeViewer.addSelectionChangedListener(renameAction);
//    cleanUpAction = new CleanUpAction(getViewSite());
//    treeViewer.addSelectionChangedListener(cleanUpAction);
    moveAction = new MoveResourceAction(getShell());
    treeViewer.addSelectionChangedListener(moveAction);

    formatFileAction = new FormatFileAction(getViewSite());
    formatFileAction.setEnabled(false); //selection events will update
    treeViewer.addSelectionChangedListener(formatFileAction);

    propertyDialogAction = new PropertyDialogAction(getViewSite(), treeViewer);
    propertyDialogAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);
    propertyDialogAction.setEnabled(false); //selection events will update
    treeViewer.addSelectionChangedListener(propertyDialogAction);

    ignoreResourceAction = new IgnoreResourceAction(getShell());
    treeViewer.addSelectionChangedListener(ignoreResourceAction);

    enableBuilderAction = new EnableDartBuilderAction(getShell());
    treeViewer.addSelectionChangedListener(enableBuilderAction);

    clipboard = new Clipboard(getShell().getDisplay());

    pasteAction = new PasteAction(getShell(), clipboard);
    treeViewer.addSelectionChangedListener(pasteAction);

    copyAction = new CopyAction(getShell(), clipboard, pasteAction);
    copyAction.setEnabled(false); //selection events will update
    treeViewer.addSelectionChangedListener(copyAction);

//    refreshAction = new RefreshAction(this);
//    treeViewer.addSelectionChangedListener(refreshAction);

    deleteAction = new DeleteAction(getSite());
    deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
        ISharedImages.IMG_TOOL_DELETE));
    treeViewer.addSelectionChangedListener(deleteAction);

    hideContainerAction = new HideProjectAction(getSite());
    treeViewer.addSelectionChangedListener(hideContainerAction);

    cleanFoldersAction = new CleanFoldersAction(getSite());
    treeViewer.addSelectionChangedListener(cleanFoldersAction);

    copyFilePathAction = new CopyFilePathAction(getSite());
    treeViewer.addSelectionChangedListener(copyFilePathAction);

    openAsTextAction = new OpenAsTextAction(getSite().getPage());
    treeViewer.addSelectionChangedListener(openAsTextAction);

    pubUpdateAction = RunPubAction.createPubUpdateAction(getSite().getWorkbenchWindow());
    pubInstallAction = RunPubAction.createPubInstallAction(getSite().getWorkbenchWindow());
    pubInstallOfflineAction = RunPubAction.createPubInstallOfflineAction(getSite().getWorkbenchWindow());
    pubDeployAction = RunPubAction.createPubDeployAction(getSite().getWorkbenchWindow());

    copyPackageAction = new NewAppFromPackageAction(getSite());

    browseDartDocAction = new OpenExternalDartdocAction(getSite()) {
      @Override
      protected boolean isValidSelection(
          com.google.dart.tools.ui.internal.text.editor.DartSelection selection) {
        return true;
      }
    };
    treeViewer.addSelectionChangedListener(browseDartDocAction);
  }
}
