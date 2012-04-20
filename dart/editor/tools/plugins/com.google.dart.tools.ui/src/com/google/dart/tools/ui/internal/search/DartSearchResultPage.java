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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.search.ui.ISearchResultViewPart;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.text.editor.CompositeActionGroup;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.internal.viewsupport.DecoratingDartLabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.ProblemTableViewer;
import com.google.dart.tools.ui.internal.viewsupport.ProblemTreeViewer;
import com.google.dart.tools.ui.search.MatchPresentation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.HashMap;

/**
 * A search result page for displaying Dart element search matches.
 */
public class DartSearchResultPage extends AbstractTextSearchViewPage implements IAdaptable {

  private static class DecoratorIgnoringViewerSorter extends ViewerComparator {

    private final ILabelProvider labelProvider;

    public DecoratorIgnoringViewerSorter(ILabelProvider labelProvider) {
      this.labelProvider = labelProvider;
    }

    @Override
    public int category(Object element) {
      if (element instanceof DartElement || element instanceof IResource) {
        return 1;
      }
      return 2;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      int cat1 = category(e1);
      int cat2 = category(e2);

      if (cat1 != cat2) {
        return cat1 - cat2;
      }
      String name1 = labelProvider.getText(e1);
      String name2 = labelProvider.getText(e2);
      if (name1 == null) {
        name1 = "";//$NON-NLS-1$
      }
      if (name2 == null) {
        name2 = "";//$NON-NLS-1$
      }
      return getComparator().compare(name1, name2);
    }
  }

  private class GroupAction extends Action {
    private int fGrouping;
    private DartSearchResultPage fPage;

    public GroupAction(String label, String tooltip, DartSearchResultPage page, int grouping) {
      super(label);
      setToolTipText(tooltip);
      fPage = page;
      fGrouping = grouping;
    }

    public int getGrouping() {
      return fGrouping;
    }

    @Override
    public void run() {
      fPage.setGrouping(fGrouping);
    }
  }

  private class NewSearchViewActionGroup extends CompositeActionGroup {

    public NewSearchViewActionGroup(IViewPart part) {
//TODO (pquitslund): implement action groups
//      Assert.isNotNull(part);
//      OpenViewActionGroup openViewActionGroup;
//      setGroups(new ActionGroup[]{
//        new OpenEditorActionGroup(part),
//        openViewActionGroup= new OpenViewActionGroup(part),
//        new GenerateActionGroup(part),
//        new RefactorActionGroup(part),
//        new JavaSearchActionGroup(part)
//      });
//      openViewActionGroup.containsShowInMenu(false);
    }
  }

  private class PostfixLabelProvider extends SearchLabelProvider {
    private ITreeContentProvider fContentProvider;

    public PostfixLabelProvider(DartSearchResultPage page) {
      super(page);
      fContentProvider = new LevelTreeContentProvider.FastDartElementProvider();
    }

    @Override
    public Image getImage(Object element) {
      Image image = super.getImage(element);
      if (image != null) {
        return image;
      }
      return getParticipantImage(element);
    }

    @Override
    public StyledString getStyledText(Object element) {
      StyledString styledString = getColoredLabelWithCounts(element, internalGetStyledText(element));
      styledString.append(getQualification(element), StyledString.QUALIFIER_STYLER);
      return styledString;
    }

    @Override
    public String getText(Object element) {
      String labelWithCounts = getLabelWithCounts(element, internalGetText(element));
      return labelWithCounts + getQualification(element);
    }

    @Override
    protected boolean hasChildren(Object element) {
      ITreeContentProvider contentProvider = (ITreeContentProvider) page.getViewer().getContentProvider();
      return contentProvider.hasChildren(element);
    }

    private String getQualification(Object element) {
      StringBuffer res = new StringBuffer();

      ITreeContentProvider provider = (ITreeContentProvider) page.getViewer().getContentProvider();
      Object visibleParent = provider.getParent(element);
      Object realParent = fContentProvider.getParent(element);
      Object lastElement = element;
      while (realParent != null && !(realParent instanceof DartModel)
          && !realParent.equals(visibleParent)) {
        if (!isSameInformation(realParent, lastElement)) {
          res.append(DartElementLabels.CONCAT_STRING).append(internalGetText(realParent));
        }
        lastElement = realParent;
        realParent = fContentProvider.getParent(realParent);
      }
      return res.toString();
    }

    private StyledString internalGetStyledText(Object element) {
      StyledString text = super.getStyledText(element);
      if (text != null && text.length() > 0) {
        return text;
      }
      return getStyledParticipantText(element);
    }

    private String internalGetText(Object element) {
      String text = super.getText(element);
      if (text != null && text.length() > 0) {
        return text;
      }
      return getParticipantText(element);
    }

    private boolean isSameInformation(Object realParent, Object lastElement) {
      if (lastElement instanceof Type) {
        Type type = (Type) lastElement;
        if (realParent instanceof DartLibrary) {
          if (type.getLibrary().equals(realParent)) {
            return true;
          }
        } else if (realParent instanceof CompilationUnit) {
          if (type.getCompilationUnit().equals(realParent)) {
            return true;
          }
        }
      }
      return false;
    }

  }

  private class SortAction extends Action {
    private DartSearchResultPage fPage;
    private int fSortOrder;

    public SortAction(String label, DartSearchResultPage page, int sortOrder) {
      super(label);
      fPage = page;
      fSortOrder = sortOrder;
    }

    public int getSortOrder() {
      return fSortOrder;
    }

    @Override
    public void run() {
      BusyIndicator.showWhile(fPage.getViewer().getControl().getDisplay(), new Runnable() {
        @Override
        public void run() {
          fPage.setSortOrder(fSortOrder);
        }
      });
    }
  }

  public static final IShowInTargetList SHOW_IN_TARGET_LIST = new IShowInTargetList() {
    @Override
    public String[] getShowInTargetIds() {
      return SHOW_IN_TARGETS;
    }
  };
  private static final int DEFAULT_ELEMENT_LIMIT = 1000;
  private static final String FALSE = "FALSE"; //$NON-NLS-1$
  private static final String KEY_GROUPING = "org.eclipse.jdt.search.resultpage.grouping"; //$NON-NLS-1$
  private static final String KEY_LIMIT = "org.eclipse.jdt.search.resultpage.limit"; //$NON-NLS-1$

  private static final String KEY_LIMIT_ENABLED = "org.eclipse.jdt.search.resultpage.limit_enabled"; //$NON-NLS-1$
  private static final String KEY_SORTING = "org.eclipse.jdt.search.resultpage.sorting"; //$NON-NLS-1$

  private static final String[] SHOW_IN_TARGETS = new String[] {DartUI.ID_LIBRARIES
  /* , DartUI.ID_RES_NAV */};

  private static final String TRUE = "TRUE"; //$NON-NLS-1$
  private NewSearchViewActionGroup actionGroup;
  private DartSearchContentProvider contentProvider;
  private int currentGrouping;
  private int currentSortOrder;

  private DartSearchEditorOpener fEditorOpener = new DartSearchEditorOpener();
//  private GroupAction fGroupFileAction;
//  private GroupAction fGroupPackageAction;
//  private GroupAction fGroupProjectAction;

//  private SelectionDispatchAction fCopyQualifiedNameAction;

//  private GroupAction fGroupTypeAction;

//  private SortAction fSortByNameAction;
//
//  private SortAction fSortByParentName;
//  private SortAction fSortByPathAction;

  private SortingLabelProvider fSortingLabelProvider;

  public DartSearchResultPage() {
//    fCopyQualifiedNameAction= null;

//    initSortActions();
//    initGroupingActions();
    setElementLimit(new Integer(DEFAULT_ELEMENT_LIMIT));
  }

  @Override
  public void dispose() {
    actionGroup.dispose();
    super.dispose();
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    if (IShowInTargetList.class.equals(adapter)) {
      return SHOW_IN_TARGET_LIST;
    }
    return null;
  }

  @Override
  public String getLabel() {
    String label = super.getLabel();
    AbstractTextSearchResult input = getInput();
    if (input != null && input.getActiveMatchFilters() != null
        && input.getActiveMatchFilters().length > 0) {
      if (isQueryRunning()) {
        String message = SearchMessages.DartSearchResultPage_filtered_message;
        return Messages.format(message, new Object[] {label});

      } else {
        int filteredOut = input.getMatchCount() - getFilteredMatchCount();
        String message = SearchMessages.DartSearchResultPage_filteredWithCount_message;
        return Messages.format(message, new Object[] {label, String.valueOf(filteredOut)});
      }
    }
    return label;
  }

  @Override
  public void init(IPageSite site) {
    super.init(site);
//    IMenuManager menuManager = site.getActionBars().getMenuManager();
//    menuManager.insertBefore(IContextMenuConstants.GROUP_PROPERTIES, new Separator(GROUP_FILTERING));
    actionGroup.fillActionBars(site.getActionBars());
//    menuManager.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, new Action(
//        SearchMessages.JavaSearchResultPage_preferences_label) {
//      @Override
//      public void run() {
//        String pageId = "org.eclipse.search.preferences.SearchPreferencePage"; //$NON-NLS-1$
//        PreferencesUtil.createPreferenceDialogOn(DartToolsPlugin.getActiveWorkbenchShell(), pageId,
//            null, null).open();
//      }
//    });
  }

  @Override
  public void restoreState(IMemento memento) {
    super.restoreState(memento);

    int sortOrder = SortingLabelProvider.SHOW_ELEMENT_CONTAINER;
    int grouping = LevelTreeContentProvider.LEVEL_PACKAGE;
    int elementLimit = DEFAULT_ELEMENT_LIMIT;

    try {
      sortOrder = getSettings().getInt(KEY_SORTING);
    } catch (NumberFormatException e) {
    }
    try {
      grouping = getSettings().getInt(KEY_GROUPING);
    } catch (NumberFormatException e) {
    }
    if (FALSE.equals(getSettings().get(KEY_LIMIT_ENABLED))) {
      elementLimit = -1;
    } else {
      try {
        elementLimit = getSettings().getInt(KEY_LIMIT);
      } catch (NumberFormatException e) {
      }
    }
    if (memento != null) {
      Integer value = memento.getInteger(KEY_GROUPING);
      if (value != null) {
        grouping = value.intValue();
      }
      value = memento.getInteger(KEY_SORTING);
      if (value != null) {
        sortOrder = value.intValue();
      }
      boolean limitElements = !FALSE.equals(memento.getString(KEY_LIMIT_ENABLED));
      value = memento.getInteger(KEY_LIMIT);
      if (value != null) {
        elementLimit = limitElements ? value.intValue() : -1;
      }
    }

    currentGrouping = grouping;
    currentSortOrder = sortOrder;
    setElementLimit(new Integer(elementLimit));
  }

//  private SelectionDispatchAction getCopyQualifiedNameAction() {
//    if (fCopyQualifiedNameAction == null) {
//      fCopyQualifiedNameAction= new CopyQualifiedNameAction(getSite());
//      fCopyQualifiedNameAction.setActionDefinitionId(CopyQualifiedNameAction.ACTION_DEFINITION_ID);
//    }
//    return fCopyQualifiedNameAction;
//  }

  @Override
  public void saveState(IMemento memento) {
    super.saveState(memento);
    memento.putInteger(KEY_GROUPING, currentGrouping);
    memento.putInteger(KEY_SORTING, currentSortOrder);
    int limit = getElementLimit().intValue();
    if (limit != -1) {
      memento.putString(KEY_LIMIT_ENABLED, TRUE);
    } else {
      memento.putString(KEY_LIMIT_ENABLED, FALSE);
    }
    memento.putInteger(KEY_LIMIT, limit);
  }

  @Override
  public void setElementLimit(Integer elementLimit) {
    super.setElementLimit(elementLimit);
    int limit = elementLimit.intValue();
    getSettings().put(KEY_LIMIT, limit);
    getSettings().put(KEY_LIMIT_ENABLED, limit != -1 ? TRUE : FALSE);
  }

  @Override
  public void setViewPart(ISearchResultViewPart part) {
    super.setViewPart(part);
    actionGroup = new NewSearchViewActionGroup(part);
  }

  @Override
  public void showMatch(Match match, int offset, int length, boolean activate)
      throws PartInitException {
    IEditorPart editor = fEditorOpener.openMatch(match);

    if (editor != null && activate) {
      editor.getEditorSite().getPage().activate(editor);
    }
    Object element = match.getElement();
    if (editor instanceof ITextEditor) {
      ITextEditor textEditor = (ITextEditor) editor;
      textEditor.selectAndReveal(offset, length);
    } else if (editor != null) {
      if (element instanceof IFile) {
        IFile file = (IFile) element;
        showWithMarker(editor, file, offset, length);
      }
    } else if (getInput() instanceof DartSearchResult) {
      DartSearchResult result = (DartSearchResult) getInput();
      MatchPresentation participant = result.getSearchParticpant(element);
      if (participant != null) {
        participant.showMatch(match, offset, length, activate);
      }
    }
  }

  @Override
  protected void clear() {
    if (contentProvider != null) {
      contentProvider.clear();
    }
  }

  @Override
  protected void configureTableViewer(TableViewer viewer) {
    viewer.setUseHashlookup(true);
    fSortingLabelProvider = new SortingLabelProvider(this);
    viewer.setLabelProvider(new DecoratingDartLabelProvider(fSortingLabelProvider, false));
    contentProvider = new DartSearchTableContentProvider(this);
    viewer.setContentProvider(contentProvider);
    viewer.setComparator(new DecoratorIgnoringViewerSorter(fSortingLabelProvider));
    setSortOrder(currentSortOrder);
    addDragAdapters(viewer);
  }

  @Override
  protected void configureTreeViewer(TreeViewer viewer) {
    PostfixLabelProvider postfixLabelProvider = new PostfixLabelProvider(this);
    viewer.setUseHashlookup(true);
    viewer.setComparator(new DecoratorIgnoringViewerSorter(postfixLabelProvider));
    viewer.setLabelProvider(new DecoratingDartLabelProvider(postfixLabelProvider, false));
    contentProvider = new LevelTreeContentProvider(this, currentGrouping);
    viewer.setContentProvider(contentProvider);
    addDragAdapters(viewer);
  }

  @Override
  protected TableViewer createTableViewer(Composite parent) {
    return new ProblemTableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
  }

  @Override
  protected TreeViewer createTreeViewer(Composite parent) {
    return new ProblemTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
  }

  @Override
  protected void elementsChanged(Object[] objects) {
    if (contentProvider != null) {
      contentProvider.elementsChanged(objects);
    }
  }

  @Override
  protected void fillContextMenu(IMenuManager mgr) {
    super.fillContextMenu(mgr);
//    addSortActions(mgr);

//    mgr.appendToGroup(IContextMenuConstants.GROUP_EDIT, getCopyQualifiedNameAction());

    actionGroup.setContext(new ActionContext(getSite().getSelectionProvider().getSelection()));
    actionGroup.fillContextMenu(mgr);
  }

  @Override
  protected void fillToolbar(IToolBarManager tbm) {
    super.fillToolbar(tbm);

//    IActionBars actionBars = getSite().getActionBars();
//    if (actionBars != null) {
//      actionBars.setGlobalActionHandler(CopyQualifiedNameAction.ACTION_HANDLER_ID, getCopyQualifiedNameAction());
//    }

//    if (getLayout() != FLAG_LAYOUT_FLAT) {
//      addGroupActions(tbm);
//    }
  }

  @Override
  protected StructuredViewer getViewer() {
    // override so that it's visible in the package.
    return super.getViewer();
  }

  @Override
  protected void handleOpen(OpenEvent event) {
    Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
    if (firstElement instanceof CompilationUnit || firstElement instanceof Type
        || firstElement instanceof TypeMember) {
      if (getDisplayedMatchCount(firstElement) == 0) {
        try {
          fEditorOpener.openElement(firstElement);
        } catch (CoreException e) {
          ExceptionHandler.handle(e, getSite().getShell(),
              SearchMessages.DartSearchResultPage_open_editor_error_title,
              SearchMessages.DartSearchResultPage_open_editor_error_message);
        }
        return;
      }
    }
    super.handleOpen(event);
  }

  /**
   * Precondition here: the viewer must be showing a tree with a LevelContentProvider.
   * 
   * @param grouping the grouping which must be one of the <code>LEVEL_*</code> constants from
   *          {@link LevelTreeContentProvider}
   */
  void setGrouping(int grouping) {
    currentGrouping = grouping;
    StructuredViewer viewer = getViewer();
    LevelTreeContentProvider cp = (LevelTreeContentProvider) viewer.getContentProvider();
    cp.setLevel(grouping);
//    updateGroupingActions();
    getSettings().put(KEY_GROUPING, currentGrouping);
    getViewPart().updateLabel();
  }

  void setSortOrder(int order) {
    if (fSortingLabelProvider != null) {
      currentSortOrder = order;
      StructuredViewer viewer = getViewer();
      //viewer.getControl().setRedraw(false);
      fSortingLabelProvider.setOrder(order);
      //viewer.getControl().setRedraw(true);
      viewer.refresh();
      getSettings().put(KEY_SORTING, currentSortOrder);
    }
  }

  private void addDragAdapters(StructuredViewer viewer) {
    //TODO (pquitslund): add dnd support
//    Transfer[] transfers= new Transfer[] { LocalSelectionTransfer.getInstance(), ResourceTransfer.getInstance() };
//    int ops= DND.DROP_COPY | DND.DROP_LINK;
//
//    JdtViewerDragAdapter dragAdapter= new JdtViewerDragAdapter(viewer);
//    dragAdapter.addDragSourceListener(new SelectionTransferDragAdapter(viewer));
//    dragAdapter.addDragSourceListener(new EditorInputTransferDragAdapter(viewer));
//    dragAdapter.addDragSourceListener(new ResourceTransferDragAdapter(viewer));
//    viewer.addDragSupport(ops, transfers, dragAdapter);
  }

//  private void addGroupActions(IToolBarManager mgr) {
//    mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, new Separator(GROUP_GROUPING));
//    mgr.appendToGroup(GROUP_GROUPING, fGroupProjectAction);
//    mgr.appendToGroup(GROUP_GROUPING, fGroupPackageAction);
//    mgr.appendToGroup(GROUP_GROUPING, fGroupFileAction);
//    mgr.appendToGroup(GROUP_GROUPING, fGroupTypeAction);
//
//    updateGroupingActions();
//  }

//  private void addSortActions(IMenuManager mgr) {
//    if (getLayout() != FLAG_LAYOUT_FLAT) {
//      return;
//    }
//    MenuManager sortMenu = new MenuManager(SearchMessages.JavaSearchResultPage_sortBylabel);
//    sortMenu.add(fSortByNameAction);
//    sortMenu.add(fSortByPathAction);
//    sortMenu.add(fSortByParentName);
//
//    fSortByNameAction.setChecked(fCurrentSortOrder == fSortByNameAction.getSortOrder());
//    fSortByPathAction.setChecked(fCurrentSortOrder == fSortByPathAction.getSortOrder());
//    fSortByParentName.setChecked(fCurrentSortOrder == fSortByParentName.getSortOrder());
//
//    mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortMenu);
//  }

  private int getFilteredMatchCount() {
    StructuredViewer viewer = getViewer();
    if (viewer instanceof TreeViewer) {
      ITreeContentProvider tp = (ITreeContentProvider) viewer.getContentProvider();
      return getMatchCount(tp, getRootElements((TreeViewer) getViewer()));
    } else {
      return getMatchCount((TableViewer) viewer);
    }
  }

  private int getMatchCount(ITreeContentProvider cp, Object[] elements) {
    int count = 0;
    for (int j = 0; j < elements.length; j++) {
      count += getDisplayedMatchCount(elements[j]);
      Object[] children = cp.getChildren(elements[j]);
      count += getMatchCount(cp, children);
    }
    return count;
  }

  private int getMatchCount(TableViewer viewer) {
    Object[] elements = getRootElements(viewer);
    int count = 0;
    for (int i = 0; i < elements.length; i++) {
      count += getDisplayedMatchCount(elements[i]);
    }
    return count;
  }

  private Object[] getRootElements(TableViewer viewer) {
    Table t = viewer.getTable();
    Item[] roots = t.getItems();
    Object[] elements = new Object[roots.length];
    for (int i = 0; i < elements.length; i++) {
      elements[i] = roots[i].getData();
    }
    return elements;
  }

  private Object[] getRootElements(TreeViewer viewer) {
    Tree t = viewer.getTree();
    Item[] roots = t.getItems();
    Object[] elements = new Object[roots.length];
    for (int i = 0; i < elements.length; i++) {
      elements[i] = roots[i].getData();
    }
    return elements;
  }

//  private void initGroupingActions() {
//    fGroupProjectAction = new GroupAction(SearchMessages.JavaSearchResultPage_groupby_project,
//        SearchMessages.JavaSearchResultPage_groupby_project_tooltip, this,
//        LevelTreeContentProvider.LEVEL_PROJECT);
//    DartPluginImages.setLocalImageDescriptors(fGroupProjectAction, "prj_mode.gif"); //$NON-NLS-1$
//    fGroupPackageAction = new GroupAction(SearchMessages.JavaSearchResultPage_groupby_package,
//        SearchMessages.JavaSearchResultPage_groupby_package_tooltip, this,
//        LevelTreeContentProvider.LEVEL_PACKAGE);
//    DartPluginImages.setLocalImageDescriptors(fGroupPackageAction, "package_mode.gif"); //$NON-NLS-1$
//    fGroupFileAction = new GroupAction(SearchMessages.JavaSearchResultPage_groupby_file,
//        SearchMessages.JavaSearchResultPage_groupby_file_tooltip, this,
//        LevelTreeContentProvider.LEVEL_FILE);
//    DartPluginImages.setLocalImageDescriptors(fGroupFileAction, "file_mode.gif"); //$NON-NLS-1$
//    fGroupTypeAction = new GroupAction(SearchMessages.JavaSearchResultPage_groupby_type,
//        SearchMessages.JavaSearchResultPage_groupby_type_tooltip, this,
//        LevelTreeContentProvider.LEVEL_TYPE);
//    DartPluginImages.setLocalImageDescriptors(fGroupTypeAction, "type_mode.gif"); //$NON-NLS-1$
//  }

//  private void initSortActions() {
//    fSortByNameAction = new SortAction(SearchMessages.JavaSearchResultPage_sortByName, this,
//        SortingLabelProvider.SHOW_ELEMENT_CONTAINER);
//    fSortByPathAction = new SortAction(SearchMessages.JavaSearchResultPage_sortByPath, this,
//        SortingLabelProvider.SHOW_PATH);
//    fSortByParentName = new SortAction(SearchMessages.JavaSearchResultPage_sortByParentName, this,
//        SortingLabelProvider.SHOW_CONTAINER_ELEMENT);
//  }

  private boolean isQueryRunning() {
    AbstractTextSearchResult result = getInput();
    if (result != null) {
      return NewSearchUI.isQueryRunning(result.getQuery());
    }
    return false;
  }

  private void showWithMarker(IEditorPart editor, IFile file, int offset, int length)
      throws PartInitException {
    try {
      IMarker marker = file.createMarker(NewSearchUI.SEARCH_MARKER);
      HashMap<String, Integer> attributes = new HashMap<String, Integer>(4);
      attributes.put(IMarker.CHAR_START, new Integer(offset));
      attributes.put(IMarker.CHAR_END, new Integer(offset + length));
      marker.setAttributes(attributes);
      IDE.gotoMarker(editor, marker);
      marker.delete();
    } catch (CoreException e) {
      throw new PartInitException(SearchMessages.DartSearchResultPage_error_marker, e);
    }
  }

//  private void updateGroupingActions() {
//    fGroupProjectAction.setChecked(fCurrentGrouping == LevelTreeContentProvider.LEVEL_PROJECT);
//    fGroupPackageAction.setChecked(fCurrentGrouping == LevelTreeContentProvider.LEVEL_PACKAGE);
//    fGroupFileAction.setChecked(fCurrentGrouping == LevelTreeContentProvider.LEVEL_FILE);
//    fGroupTypeAction.setChecked(fCurrentGrouping == LevelTreeContentProvider.LEVEL_TYPE);
//  }
}
