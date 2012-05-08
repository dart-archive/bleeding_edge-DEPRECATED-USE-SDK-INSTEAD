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
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.internal.ui.ISearchHelpContextIds;
import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.IContextMenuConstants;
import com.google.dart.tools.search.ui.IQueryListener;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.ISearchResultPage;
import com.google.dart.tools.search.ui.ISearchResultViewPart;
import com.google.dart.tools.search.ui.NewSearchUI;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.part.PageSwitcher;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class SearchView extends PageBookView implements ISearchResultViewPart, IQueryListener {

  static class DummyPart implements IWorkbenchPart {
    private int fLastActivation = 0;

    @Override
    public void addPropertyListener(IPropertyListener listener) {/* dummy */
    }

    @Override
    public void createPartControl(Composite parent) {/* dummy */
    }

    @Override
    public void dispose() {/* dummy */
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
      return null;
    }

    public int getLastActivation() {
      return fLastActivation;
    }

    @Override
    public IWorkbenchPartSite getSite() {
      return null;
    }

    @Override
    public String getTitle() {
      return null;
    }

    @Override
    public Image getTitleImage() {
      return null;
    }

    @Override
    public String getTitleToolTip() {
      return null;
    }

    @Override
    public void removePropertyListener(IPropertyListener listener) {/* dummy */
    }

    @Override
    public void setFocus() {/* dummy */
    }

    public void setLastActivation(int lastActivation) {
      fLastActivation = lastActivation;
    }
  }
  static class EmptySearchView extends Page implements ISearchResultPage {
    private Composite fControl;
    private String fId;

    @Override
    public void createControl(Composite parent) {
      Color background = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(new GridLayout(1, false));

      composite.setBackground(background);

      Link link = new Link(composite, SWT.NONE);
      link.setText(SearchMessages.SearchView_empty_search_label);
      link.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
      link.setBackground(background);

      fControl = composite;
    }

    @Override
    public Control getControl() {
      return fControl;
    }

    @Override
    public String getID() {
      return fId;
    }

    @Override
    public String getLabel() {
      return ""; //$NON-NLS-1$
    }

    @Override
    public Object getUIState() {
      // empty implementation
      return null;
    }

    @Override
    public void init(IPageSite pageSite) {
      super.init(pageSite);
      getSite().setSelectionProvider(null);
    }

    @Override
    public void restoreState(IMemento memento) {
      // do nothing
    }

    @Override
    public void saveState(IMemento memento) {
      // do nothing

    }

    @Override
    public void setFocus() {
      if (fControl != null) {
        fControl.setFocus();
      }
    }

    @Override
    public void setID(String id) {
      fId = id;
    }

    @Override
    public void setInput(ISearchResult search, Object viewState) {
      // do nothing
    }

    @Override
    public void setViewPart(ISearchResultViewPart part) {
      // do nothing
    }
  }

  private static final String MEMENTO_TYPE = "view"; //$NON-NLS-1$
  private static final String MEMENTO_KEY_LAST_ACTIVATION = "com.google.dart.tools.search.lastActivation"; //$NON-NLS-1$
  private static final String MEMENTO_KEY_RESTORE = "com.google.dart.tools.search.restore"; //$NON-NLS-1$

  /**
   * Creates the groups and separators for the search view's context menu
   * 
   * @param menu the context menu
   */
  public static void createContextMenuGroups(IMenuManager menu) {
    menu.add(new Separator(IContextMenuConstants.GROUP_NEW));
    menu.add(new GroupMarker(IContextMenuConstants.GROUP_SHOW));
    menu.add(new GroupMarker(IContextMenuConstants.GROUP_OPEN));
    menu.add(new GroupMarker(IContextMenuConstants.GROUP_REMOVE_MATCHES));
    menu.add(new Separator(IContextMenuConstants.GROUP_EDIT));
    menu.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
    menu.add(new GroupMarker(IContextMenuConstants.GROUP_GENERATE));
    menu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
    menu.add(new Separator(IContextMenuConstants.GROUP_BUILD));
    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    menu.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));
    menu.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
  }

  /**
   * Creates the groups and separators for the search view's tool bar
   * 
   * @param toolbar the toolbar
   */
  public static void createToolBarGroups(IToolBarManager toolbar) {
    toolbar.add(new Separator(IContextMenuConstants.GROUP_NEW));
    toolbar.add(new GroupMarker(IContextMenuConstants.GROUP_SHOW));
    toolbar.add(new GroupMarker(IContextMenuConstants.GROUP_OPEN));
    toolbar.add(new Separator(IContextMenuConstants.GROUP_BUILD));
    toolbar.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
    toolbar.add(new Separator(IContextMenuConstants.GROUP_EDIT));
    toolbar.add(new GroupMarker(IContextMenuConstants.GROUP_REMOVE_MATCHES));
    toolbar.add(new GroupMarker(IContextMenuConstants.GROUP_GENERATE));
    toolbar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    toolbar.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));
    toolbar.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
    toolbar.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
  }

  private HashMap<DummyPart, IPageBookViewPage> fPartsToPages;
  private HashMap<IPageBookViewPage, DummyPart> fPagesToParts;
  private HashMap<ISearchResult, Object> fSearchViewStates;
  private SearchPageRegistry fSearchViewPageService;
  private SearchHistoryDropDownAction fSearchesDropDownAction;
  private ISearchResult fCurrentSearch;

  private DummyPart fDefaultPart;
  private SearchAgainAction fSearchAgainAction;
  private CancelSearchAction fCancelAction;

  private UndoRedoActionGroup fUndoRedoActionGroup;
  private IMemento fPageState;

  private int fActivationCount = 0;

  private String fDefaultPartName;

  private Composite fPageContent;

  private Link fDescription;

  private Composite fDescriptionComposite;

  public SearchView() {
    super();
    fPartsToPages = new HashMap<DummyPart, IPageBookViewPage>();
    fPagesToParts = new HashMap<IPageBookViewPage, DummyPart>();
    fSearchViewPageService = new SearchPageRegistry();
    fSearchViewStates = new HashMap<ISearchResult, Object>();
  }

  @Override
  public void createPartControl(Composite parent) {
    createActions();

    fPageContent = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    fPageContent.setLayout(layout);

    fDescriptionComposite = null;

    super.createPartControl(fPageContent);
    getPageBook().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    fDefaultPartName = getPartName();
    initializeToolBar();
    InternalSearchUI.getInstance().getSearchManager().addQueryListener(this);
    initializePageSwitcher();

    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, ISearchHelpContextIds.New_SEARCH_VIEW);
    restorePageFromMemento();

    showLatestSearch();
  }

  @Override
  public void dispose() {
    if (fUndoRedoActionGroup != null) {
      fUndoRedoActionGroup.dispose();
    }
    InternalSearchUI.getInstance().getSearchViewManager().searchViewClosed(this);
    InternalSearchUI.getInstance().getSearchManager().removeQueryListener(this);
    super.dispose();
  }

  @Override
  public void fillContextMenu(IMenuManager menuManager) {
    ISearchResult result = getCurrentSearchResult();
    if (result != null) {
      menuManager.appendToGroup(IContextMenuConstants.GROUP_SEARCH, fSearchAgainAction);
    }
  }

  @Override
  public ISearchResultPage getActivePage() {
    IPage page = getCurrentPage();
    if (page instanceof ISearchResultPage) {
      return (ISearchResultPage) page;
    }
    return null;
  }

  public ISearchResult getCurrentSearchResult() {
    return fCurrentSearch;
  }

  public IWorkbenchSiteProgressService getProgressService() {
    IWorkbenchSiteProgressService service = null;
    Object siteService = getSite().getAdapter(IWorkbenchSiteProgressService.class);
    if (siteService != null) {
      service = (IWorkbenchSiteProgressService) siteService;
    }
    return service;
  }

  /**
   * @return the search result page registry
   */
  public SearchPageRegistry getSearchPageRegistry() {
    return fSearchViewPageService;
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);
    fPageState = memento;
  }

  @Override
  public void partActivated(IWorkbenchPart part) {
    super.partActivated(part);
    if (part == this) {
      InternalSearchUI.getInstance().getSearchViewManager().searchViewActivated(this);
    }
  }

  @Override
  public void queryAdded(ISearchQuery query) {
    fSearchesDropDownAction.updateEnablement();
  }

  @Override
  public void queryFinished(ISearchQuery query) {
    if (fCurrentSearch != null && fCurrentSearch.equals(query.getSearchResult())) {
      updateCancelAction();
    }
  }

  @Override
  public void queryRemoved(ISearchQuery query) {
    InternalSearchUI.getInstance().cancelSearch(query);
    if (query.getSearchResult().equals(fCurrentSearch)) {
      showSearchResult(null);
      partActivated(fDefaultPart);
    }
    fSearchViewStates.remove(query.getSearchResult());
    fSearchesDropDownAction.disposeMenu();
    fSearchesDropDownAction.updateEnablement();
  }

  @Override
  public void queryStarting(ISearchQuery query) {
    if (fCurrentSearch != null && fCurrentSearch.equals(query.getSearchResult())) {
      updateCancelAction();
    }
  }

  @Override
  public void saveState(IMemento memento) {
    for (Iterator<Entry<IPageBookViewPage, DummyPart>> iter = fPagesToParts.entrySet().iterator(); iter.hasNext();) {
      Entry<IPageBookViewPage, DummyPart> entry = iter.next();
      ISearchResultPage page = (ISearchResultPage) entry.getKey();
      DummyPart part = entry.getValue();

      IMemento child = memento.createChild(MEMENTO_TYPE, page.getID());
      page.saveState(child);
      child.putInteger(MEMENTO_KEY_LAST_ACTIVATION, part.getLastActivation());
    }
  }

  /*
   * TODO workaround for focus problem. Clarify focus behavior.
   * 
   * @see org.eclipse.ui.IWorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    IPage currentPage = getCurrentPage();
    if (currentPage != null) {
      currentPage.setFocus();
    } else {
      super.setFocus();
    }
  }

  @Override
  public void showBusy(boolean busy) {
    super.showBusy(busy);
    if (!busy) {
      getProgressService().warnOfContentChange();
    }
  }

  public void showEmptySearchPage(String pageId) {
    ISearchResultPage newPage = fSearchViewPageService.findPageForPageId(pageId, true);
    internalShowSearchPage(newPage, null);
  }

  public void showSearchResult(ISearchResult search) {
    ISearchResultPage newPage = null;
    if (search != null) {
      newPage = fSearchViewPageService.findPageForSearchResult(search, true);
      if (newPage == null) {
        String format = SearchMessages.SearchView_error_noResultPage;
        String message = MessageFormat.format(format, new Object[] {search.getClass().getName()});
        SearchPlugin.log(new Status(IStatus.ERROR, SearchPlugin.getID(), 0, message, null));
        return;
      }
    }
    internalShowSearchPage(newPage, search);
  }

  @Override
  public void updateLabel() {
    ISearchResultPage page = getActivePage();
    String label = ""; //$NON-NLS-1$
    if (page != null) {
      label = LegacyActionTools.escapeMnemonics(page.getLabel());
    }
    if (!fPageContent.isDisposed()) {
      if (label.length() == 0) {
        if (fDescriptionComposite != null) {
          fDescriptionComposite.dispose();
          fDescriptionComposite = null;
          fPageContent.layout();
        }
      } else {
        if (fDescriptionComposite == null) {
          fDescriptionComposite = new Composite(fPageContent, SWT.NONE);
          fDescriptionComposite.moveAbove(null);

          GridLayout layout = new GridLayout();
          layout.marginHeight = 0;
          layout.marginWidth = 0;
          layout.verticalSpacing = 0;
          fDescriptionComposite.setLayout(layout);
          fDescriptionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

          fDescription = new Link(fDescriptionComposite, SWT.NONE);
          GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
          gridData.horizontalIndent = 5;
          fDescription.setLayoutData(gridData);
          fDescription.setText(label);

          Label separator = new Label(fDescriptionComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
          separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
          fPageContent.layout();
        } else {
          fDescription.setText(label);
        }
      }
    }
  }

  public void updatePartName() {
    if (fDefaultPartName != null) {
      // mstodo not yet enabled.
//			String partName= null;
//			ISearchResultPage page= getActivePage();
//			if (page != null && isPinned()) {
//				partName= getSearchPageRegistry().findLabelForPageId(page.getID());
//			}
//			setPartName(partName != null ? partName : fDefaultPartName);
    }
  }

  @Override
  protected IPage createDefaultPage(PageBook book) {
    IPageBookViewPage page = new EmptySearchView();
    page.createControl(book);
    initPage(page);
    DummyPart part = new DummyPart();
    fPartsToPages.put(part, page);
    fPagesToParts.put(page, part);
    fDefaultPart = part;
    return page;
  }

  @Override
  protected PageRec doCreatePage(IWorkbenchPart part) {
    IPageBookViewPage page = fPartsToPages.get(part);
    initPage(page);
    page.createControl(getPageBook());
    PageRec rec = new PageRec(part, page);
    return rec;
  }

  @Override
  protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
    IPage page = pageRecord.page;
    page.dispose();
    pageRecord.dispose();

    // empty cross-reference cache
    fPartsToPages.remove(part);
  }

  // Methods related to saving page state. -------------------------------------------

  @Override
  protected IWorkbenchPart getBootstrapPart() {
    return null;
  }

  @Override
  protected void initPage(IPageBookViewPage page) {
    super.initPage(page);
    IActionBars actionBars = page.getSite().getActionBars();
    actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), fSearchAgainAction);
    actionBars.updateActionBars();

    fUndoRedoActionGroup.fillActionBars(actionBars);

    ISearchResultPage srPage = (ISearchResultPage) page;
    IMemento memento = null;
    if (fPageState != null) {
      IMemento[] mementos = fPageState.getChildren(MEMENTO_TYPE);
      for (int i = 0; i < mementos.length; i++) {
        if (mementos[i].getID().equals(srPage.getID())) {
          memento = mementos[i];
          break;
        }
      }
    }
    srPage.restoreState(memento);
  }

  @Override
  protected boolean isImportant(IWorkbenchPart part) {
    return part instanceof DummyPart;
  }

  private void createActions() {
    fSearchesDropDownAction = new SearchHistoryDropDownAction(this);
    fSearchesDropDownAction.updateEnablement();
    fSearchAgainAction = new SearchAgainAction(this);
    fSearchAgainAction.setEnabled(false);
    fSearchAgainAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
    fCancelAction = new CancelSearchAction(this);
    fCancelAction.setEnabled(false);

    IUndoContext workspaceContext = (IUndoContext) ResourcesPlugin.getWorkspace().getAdapter(
        IUndoContext.class);
    fUndoRedoActionGroup = new UndoRedoActionGroup(getViewSite(), workspaceContext, true);
  }

  private void initializePageSwitcher() {
    new PageSwitcher(this) {
      @Override
      public void activatePage(Object page) {
        ISearchResult searchResult = ((ISearchQuery) page).getSearchResult();
        InternalSearchUI.getInstance().showSearchResult(SearchView.this, searchResult, false);
      }

      @Override
      public ImageDescriptor getImageDescriptor(Object page) {
        ISearchResult searchResult = ((ISearchQuery) page).getSearchResult();
        return searchResult.getImageDescriptor();
      }

      @Override
      public String getName(Object page) {
        ISearchResult searchResult = ((ISearchQuery) page).getSearchResult();
        return searchResult.getLabel();
      }

      @Override
      public Object[] getPages() {
        return NewSearchUI.getQueries();
      }
    };
  }

  private void initializeToolBar() {
    IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
    createToolBarGroups(tbm);
    tbm.appendToGroup(IContextMenuConstants.GROUP_SEARCH, fSearchAgainAction);
    tbm.appendToGroup(IContextMenuConstants.GROUP_SEARCH, fCancelAction);
    tbm.appendToGroup(IContextMenuConstants.GROUP_SEARCH, fSearchesDropDownAction);
    getViewSite().getActionBars().updateActionBars();
  }

  private void internalShowSearchPage(ISearchResultPage page, ISearchResult search) {
    // detach the previous page.
    ISearchResultPage currentPage = (ISearchResultPage) getCurrentPage();
    if (fCurrentSearch != null && currentPage != null) {
      fSearchViewStates.put(fCurrentSearch, currentPage.getUIState());
      currentPage.setInput(null, null);
    }

    fCurrentSearch = search;

    if (page != null) {
      if (page != currentPage) {
        DummyPart part = fPagesToParts.get(page);
        if (part == null) {
          part = new DummyPart();
          fPagesToParts.put(page, part);
          fPartsToPages.put(part, page);
          page.setViewPart(this);
        }
        part.setLastActivation(++fActivationCount);
        partActivated(part);
      }

      // connect to the new pages
      Object uiState = search != null ? fSearchViewStates.get(search) : null;
      page.setInput(search, uiState);
    }
    updatePartName();
    updateLabel();
    updateCancelAction();

    updateHelpContextID(page);

  }

  private void restorePageFromMemento() {
    if (fPageState != null) {
      int bestActivation = -1;
      IMemento restorePageMemento = null;
      IMemento[] children = fPageState.getChildren(MEMENTO_TYPE);
      for (int i = 0; i < children.length; i++) {
        IMemento pageMemento = children[i];
        if (pageMemento.getString(MEMENTO_KEY_RESTORE) != null) {
          Integer lastActivation = pageMemento.getInteger(MEMENTO_KEY_LAST_ACTIVATION);
          if (lastActivation != null && lastActivation.intValue() > bestActivation) {
            bestActivation = lastActivation.intValue();
            restorePageMemento = pageMemento;
          }
        }
      }
      if (restorePageMemento != null) {
        showEmptySearchPage(restorePageMemento.getID());
      }
    }
  }

  private void showLatestSearch() {
    ISearchQuery[] queries = InternalSearchUI.getInstance().getSearchManager().getQueries();
    if (queries.length > 0) {
      showSearchResult(queries[0].getSearchResult());
    }
  }

  private void updateCancelAction() {
    ISearchResult result = getCurrentSearchResult();
    boolean queryRunning = false;
    if (result != null) {
      queryRunning = InternalSearchUI.getInstance().isQueryRunning(result.getQuery());
    }
    fCancelAction.setEnabled(queryRunning);
    fSearchAgainAction.setEnabled(!queryRunning && result != null && result.getQuery().canRerun());
  }

  private void updateHelpContextID(ISearchResultPage page) {
    String helpContextId = null;
    String pageId = null;

    if (page != null) {
      pageId = page.getID();
    }

    if (pageId != null) {
      helpContextId = fSearchViewPageService.getHelpContextId(pageId);
    }

    if (helpContextId == null) {
      helpContextId = ISearchHelpContextIds.New_SEARCH_VIEW;
    }

    PlatformUI.getWorkbench().getHelpSystem().setHelp(fPageContent.getParent(), helpContextId);
  }
}
