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
package com.google.dart.tools.search.ui.text;

import com.google.dart.tools.search.internal.ui.CopyToClipboardAction;
import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.internal.ui.SearchPluginImages;
import com.google.dart.tools.search.internal.ui.SelectAllAction;
import com.google.dart.tools.search.internal.ui.text.EditorOpener;
import com.google.dart.tools.search.ui.IContextMenuConstants;
import com.google.dart.tools.search.ui.IQueryListener;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.ISearchResultListener;
import com.google.dart.tools.search.ui.ISearchResultPage;
import com.google.dart.tools.search.ui.ISearchResultViewPart;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.SearchResultEvent;
import com.google.dart.tools.search2.internal.ui.InternalSearchUI;
import com.google.dart.tools.search2.internal.ui.SearchMessages;
import com.google.dart.tools.search2.internal.ui.SearchView;
import com.google.dart.tools.search2.internal.ui.basic.views.CollapseAllAction;
import com.google.dart.tools.search2.internal.ui.basic.views.ExpandAllAction;
import com.google.dart.tools.search2.internal.ui.basic.views.INavigate;
import com.google.dart.tools.search2.internal.ui.basic.views.SetLayoutAction;
import com.google.dart.tools.search2.internal.ui.basic.views.ShowNextResultAction;
import com.google.dart.tools.search2.internal.ui.basic.views.ShowPreviousResultAction;
import com.google.dart.tools.search2.internal.ui.basic.views.TableViewerNavigator;
import com.google.dart.tools.search2.internal.ui.basic.views.TreeViewerNavigator;
import com.google.dart.tools.search2.internal.ui.text.AnnotationManagers;
import com.google.dart.tools.search2.internal.ui.text.PositionTracker;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.progress.UIJob;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An abstract base implementation for classes showing <code>AbstractTextSearchResult</code>
 * instances. This class assumes that the input element set via
 * {@link AbstractTextSearchViewPage#setInput(ISearchResult,Object)} is a subclass of
 * {@link AbstractTextSearchResult}. This result page supports a tree and/or a table presentation of
 * search results. Subclasses can determine which presentations they want to support at construction
 * time by passing the appropriate flags. Subclasses must customize the viewers for each
 * presentation with a label provider and a content provider. <br>
 * Changes in the search result are handled by updating the viewer in the
 * <code>elementsChanged()</code> and <code>clear()</code> methods.
 */
public abstract class AbstractTextSearchViewPage extends Page implements ISearchResultPage {
  private class SelectionProviderAdapter implements ISelectionProvider, ISelectionChangedListener {
    private ArrayList<ISelectionChangedListener> fListeners = new ArrayList<ISelectionChangedListener>(
        5);

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
      fListeners.add(listener);
    }

    @Override
    public ISelection getSelection() {
      return fViewer.getSelection();
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
      fListeners.remove(listener);
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      // forward to my listeners
      SelectionChangedEvent wrappedEvent = new SelectionChangedEvent(this, event.getSelection());
      for (Iterator<ISelectionChangedListener> listeners = fListeners.iterator(); listeners.hasNext();) {
        ISelectionChangedListener listener = listeners.next();
        listener.selectionChanged(wrappedEvent);
      }
    }

    @Override
    public void setSelection(ISelection selection) {
      fViewer.setSelection(selection);
    }

  }

  private class UpdateUIJob extends UIJob {

    public UpdateUIJob() {
      super(SearchMessages.AbstractTextSearchViewPage_update_job_name);
      setSystem(true);
    }

    /*
     * Undocumented for testing only. Used to find UpdateUIJobs.
     */
    @Override
    public boolean belongsTo(Object family) {
      return family == AbstractTextSearchViewPage.this;
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor) {
      Control control = getControl();
      if (control == null || control.isDisposed()) {
        // disposed the control while the UI was posted.
        return Status.OK_STATUS;
      }
      runBatchedClear();
      runBatchedUpdates();
      if (hasMoreUpdates() || isQueryRunning()) {
        schedule(500);
      } else {
        fIsUIUpdateScheduled = false;
        turnOnDecoration();
        updateBusyLabel();
        updateActionStates();
        if (fScheduleEnsureSelection) {
          fScheduleEnsureSelection = false;
          AbstractTextSearchResult result = getInput();
          if (result != null && fViewer.getSelection().isEmpty()) {
            navigateNext(true);
          }
        }
      }
      fViewPart.updateLabel();
      return Status.OK_STATUS;
    }

  }

  private volatile boolean fIsUIUpdateScheduled = false;

  private volatile boolean fScheduleEnsureSelection = false;
  private static final String KEY_LAYOUT = "com.google.dart.tools.search.resultpage.layout"; //$NON-NLS-1$
  /**
   * An empty array.
   */
  protected static final Match[] EMPTY_MATCH_ARRAY = new Match[0];

  private StructuredViewer fViewer;

  private Composite fViewerContainer;
  private Control fBusyLabel;
  private PageBook fPagebook;
  private boolean fIsBusyShown;
  private ISearchResultViewPart fViewPart;
  private Set<Object> fBatchedUpdates;
  private boolean fBatchedClearAll;
  private ISearchResultListener fListener;

  private IQueryListener fQueryListener;
  private MenuManager fMenu;
  private AbstractTextSearchResult fInput;
  // Actions
  private CopyToClipboardAction fCopyToClipboardAction;
  private Action fShowNextAction;
  private Action fShowPreviousAction;
  private ExpandAllAction fExpandAllAction;

  private CollapseAllAction fCollapseAllAction;
  private SetLayoutAction fFlatAction;

  private SetLayoutAction fHierarchicalAction;
  private int fCurrentLayout;
  private int fCurrentMatchIndex = 0;
  private String fId;
  private final int fSupportedLayouts;
  private SelectionProviderAdapter fViewerAdapter;
  private SelectAllAction fSelectAllAction;
  private Integer fElementLimit;

  /**
   * The editor opener.
   */
  private EditorOpener fEditorOpener = new EditorOpener();

  /**
   * Flag (<code>value 1</code>) denoting flat list layout.
   */
  public static final int FLAG_LAYOUT_FLAT = 1;

  /**
   * Flag (<code>value 2</code>) denoting tree layout.
   */
  public static final int FLAG_LAYOUT_TREE = 2;

  /**
   * Constructs this page with the default layout flags.
   * 
   * @see AbstractTextSearchViewPage#AbstractTextSearchViewPage(int)
   */
  protected AbstractTextSearchViewPage() {
    this(FLAG_LAYOUT_FLAT | FLAG_LAYOUT_TREE);
  }

  /**
   * This constructor must be passed a combination of layout flags combined with bitwise or. At
   * least one flag must be passed in (i.e. 0 is not a permitted value).
   * 
   * @param supportedLayouts flags determining which layout options this page supports. Must not be
   *          0
   * @see #FLAG_LAYOUT_FLAT
   * @see #FLAG_LAYOUT_TREE
   */
  protected AbstractTextSearchViewPage(int supportedLayouts) {
    fSupportedLayouts = supportedLayouts;
    initLayout();
    fShowNextAction = new ShowNextResultAction(this);
    fShowPreviousAction = new ShowPreviousResultAction(this);
    fCopyToClipboardAction = new CopyToClipboardAction();
    if ((supportedLayouts & FLAG_LAYOUT_TREE) != 0) {
      fExpandAllAction = new ExpandAllAction();
      fCollapseAllAction = new CollapseAllAction();
    }

    fSelectAllAction = new SelectAllAction();
    createLayoutActions();
    fBatchedUpdates = new HashSet<Object>();
    fBatchedClearAll = false;

    fListener = new ISearchResultListener() {
      @Override
      public void searchResultChanged(SearchResultEvent e) {
        handleSearchResultChanged(e);
      }
    };
    fElementLimit = null;
  }

  @Override
  public void createControl(Composite parent) {
    fQueryListener = createQueryListener();
    fMenu = new MenuManager("#PopUp"); //$NON-NLS-1$
    fMenu.setRemoveAllWhenShown(true);
    fMenu.setParent(getSite().getActionBars().getMenuManager());
    fMenu.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager mgr) {
        SearchView.createContextMenuGroups(mgr);
        fillContextMenu(mgr);
        fViewPart.fillContextMenu(mgr);
      }
    });
    fPagebook = new PageBook(parent, SWT.NULL);
    fPagebook.setLayoutData(new GridData(GridData.FILL_BOTH));
    fBusyLabel = createBusyControl();
    fViewerContainer = new Composite(fPagebook, SWT.NULL);
    fViewerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
    fViewerContainer.setSize(100, 100);
    fViewerContainer.setLayout(new FillLayout());

    fViewerAdapter = new SelectionProviderAdapter();
    getSite().setSelectionProvider(fViewerAdapter);
    // Register menu
    getSite().registerContextMenu(fViewPart.getViewSite().getId(), fMenu, fViewerAdapter);

    createViewer(fViewerContainer, fCurrentLayout);
    showBusyLabel(fIsBusyShown);
    NewSearchUI.addQueryListener(fQueryListener);

  }

  @Override
  public void dispose() {
    AbstractTextSearchResult oldSearch = getInput();
    if (oldSearch != null) {
      AnnotationManagers.removeSearchResult(getSite().getWorkbenchWindow(), oldSearch);
    }
    super.dispose();
    NewSearchUI.removeQueryListener(fQueryListener);
  }

  @Override
  public Control getControl() {
    return fPagebook;
  }

  /**
   * Returns the currently selected match.
   * 
   * @return the selected match or <code>null</code> if none are selected
   */
  public Match getCurrentMatch() {
    Object element = getFirstSelectedElement();
    if (element != null) {
      Match[] matches = getDisplayedMatches(element);
      if (fCurrentMatchIndex >= 0 && fCurrentMatchIndex < matches.length) {
        return matches[fCurrentMatchIndex];
      }
    }
    return null;
  }

  /**
   * Returns the current location of the match. This takes possible modifications of the file into
   * account. Therefore the result may differ from the position information that can be obtained
   * directly off the match.
   * 
   * @param match the match to get the position for.
   * @return the current position of the match.
   */
  public IRegion getCurrentMatchLocation(Match match) {
    PositionTracker tracker = InternalSearchUI.getInstance().getPositionTracker();

    int offset, length;
    Position pos = tracker.getCurrentPosition(match);
    if (pos == null) {
      offset = match.getOffset();
      length = match.getLength();
    } else {
      offset = pos.getOffset();
      length = pos.getLength();
    }
    return new Region(offset, length);
  }

  /**
   * Returns the number of matches that are currently displayed for the given element. If
   * {@link AbstractTextSearchResult#getActiveMatchFilters()} is not null, only matches are returned
   * that are not filtered by the match filters. Any action operating on the visible matches in the
   * search result page should use this method to get the match count for a search result (instead
   * of asking the search result directly).
   * 
   * @param element The element to get the matches for
   * @return The number of matches displayed for the given element. If the current input of this
   *         page is <code>null</code>, 0 is returned
   * @see AbstractTextSearchResult#getMatchCount(Object)
   */
  public int getDisplayedMatchCount(Object element) {
    AbstractTextSearchResult result = getInput();
    if (result == null) {
      return 0;
    }
    if (result.getActiveMatchFilters() == null) {
      return result.getMatchCount(element);
    }

    int count = 0;
    Match[] matches = result.getMatches(element);
    for (int i = 0; i < matches.length; i++) {
      if (!matches[i].isFiltered()) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns the matches that are currently displayed for the given element. If
   * {@link AbstractTextSearchResult#getActiveMatchFilters()} is not null, only matches are returned
   * that are not filtered by the match filters. If
   * {@link AbstractTextSearchResult#getActiveMatchFilters()} is null all matches of the given
   * element are returned. Any action operating on the visible matches in the search result page
   * should use this method to get the matches for a search result (instead of asking the search
   * result directly).
   * 
   * @param element The element to get the matches for
   * @return The matches displayed for the given element. If the current input of this page is
   *         <code>null</code>, an empty array is returned
   * @see AbstractTextSearchResult#getMatches(Object)
   */
  public Match[] getDisplayedMatches(Object element) {
    AbstractTextSearchResult result = getInput();
    if (result == null) {
      return EMPTY_MATCH_ARRAY;
    }
    Match[] matches = result.getMatches(element);
    if (result.getActiveMatchFilters() == null) {
      return matches;
    }

    int count = 0;
    for (int i = 0; i < matches.length; i++) {
      if (matches[i].isFiltered()) {
        matches[i] = null;
      } else {
        count++;
      }
    }
    if (count == matches.length) {
      return matches;
    }

    Match[] filteredMatches = new Match[count];
    for (int i = 0, k = 0; i < matches.length; i++) {
      if (matches[i] != null) {
        filteredMatches[k++] = matches[i];
      }
    }
    return filteredMatches;
  }

  /**
   * Gets the maximal number of top level elements to be shown in a viewer. <code>null</code> means
   * the view page does not limit the elements and will not provide UI to configure it. If a
   * non-null value is set, configuration UI will be provided. The limit value must be a positive
   * number or <code>-1</code> to not limit top level element.
   * 
   * @return returns the element limit. Valid values are:
   *         <dl>
   *         <li><code>null</code> to not limit and not provide configuration UI (default value)</li>
   *         <li><code>-1</code> to not limit and provide configuration UI</li>
   *         <li><code>positive integer</code> to limit by the given value and provide configuration
   *         UI</li>
   *         </dl>
   */
  public Integer getElementLimit() {
    return fElementLimit;
  }

  @Override
  public String getID() {
    return fId;
  }

  /**
   * Returns the currently shown result.
   * 
   * @return the previously set result or <code>null</code>
   * @see AbstractTextSearchViewPage#setInput(ISearchResult, Object)
   */
  public AbstractTextSearchResult getInput() {
    return fInput;
  }

  @Override
  public String getLabel() {
    AbstractTextSearchResult result = getInput();
    if (result == null) {
      return ""; //$NON-NLS-1$
    }
    return result.getLabel();
  }

  /**
   * Return the layout this page is currently using.
   * 
   * @return the layout this page is currently using
   * @see #FLAG_LAYOUT_FLAT
   * @see #FLAG_LAYOUT_TREE
   */
  public int getLayout() {
    return fCurrentLayout;
  }

  @Override
  public Object getUIState() {
    return fViewer.getSelection();
  }

  /**
   * Selects the element corresponding to the next match and shows the match in an editor. Note that
   * this will cycle back to the first match after the last match.
   */
  public void gotoNextMatch() {
    gotoNextMatch(false);
  }

  /**
   * Selects the element corresponding to the previous match and shows the match in an editor. Note
   * that this will cycle back to the last match after the first match.
   */
  public void gotoPreviousMatch() {
    gotoPreviousMatch(false);
  }

  @Override
  public void init(IPageSite pageSite) {
    super.init(pageSite);
    initActionDefinitionIDs();
    pageSite.getActionBars().updateActionBars();
  }

  /**
   * Note: this is internal API and should not be called from clients outside of the search plug-in.
   * <p>
   * Removes the currently selected match. Does nothing if no match is selected.
   * </p>
   * 
   * @noreference This method is not intended to be referenced by clients.
   */
  public void internalRemoveSelected() {
    AbstractTextSearchResult result = getInput();
    if (result == null) {
      return;
    }
    StructuredViewer viewer = getViewer();
    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

    HashSet<Match> set = new HashSet<Match>();
    if (viewer instanceof TreeViewer) {
      ITreeContentProvider cp = (ITreeContentProvider) viewer.getContentProvider();
      collectAllMatchesBelow(result, set, cp, selection.toArray());
    } else {
      collectAllMatches(set, selection.toArray());
    }
    navigateNext(true);

    Match[] matches = new Match[set.size()];
    set.toArray(matches);
    result.removeMatches(matches);
  }

  /**
   * Determines whether a certain layout is supported by this search result page.
   * 
   * @param layout the layout to test for
   * @return whether the given layout is supported or not
   * @see AbstractTextSearchViewPage#AbstractTextSearchViewPage(int)
   */
  public boolean isLayoutSupported(int layout) {
    return (layout & fSupportedLayouts) == layout;
  }

  @Override
  public void restoreState(IMemento memento) {
    if (countBits(fSupportedLayouts) > 1) {
      try {
        fCurrentLayout = getSettings().getInt(KEY_LAYOUT);
        // workaround because the saved value may be 0
        if (fCurrentLayout == 0) {
          initLayout();
        }
      } catch (NumberFormatException e) {
        // ignore, signals no value stored.
      }
      if (memento != null) {
        Integer layout = memento.getInteger(KEY_LAYOUT);
        if (layout != null) {
          fCurrentLayout = layout.intValue();
          // workaround because the saved value may be 0
          if (fCurrentLayout == 0) {
            initLayout();
          }
        }
      }
    }
  }

  @Override
  public void saveState(IMemento memento) {
    if (countBits(fSupportedLayouts) > 1) {
      memento.putInteger(KEY_LAYOUT, fCurrentLayout);
    }
  }

  /**
   * Sets the maximal number of top level elements to be shown in a viewer. If <code>null</code> is
   * set, the view page does not support to limit the elements and will not provide UI to configure
   * it. If a non-null value is set, configuration UI will be provided. The limit value must be a
   * positive number or <code>-1</code> to not limit top level element. If enabled, the element
   * limit has to be enforced by the content provider that is implemented by the client. The view
   * page just manages the value and configuration.
   * 
   * @param limit the element limit. Valid values are:
   *          <dl>
   *          <li><code>null</code> to not limit and not provide configuration UI</li>
   *          <li><code>-1</code> to not limit and provide configuration UI</li>
   *          <li><code>positive integer</code> to limit by the given value and provide
   *          configuration UI</li>
   *          </dl>
   */
  public void setElementLimit(Integer limit) {
    fElementLimit = limit;

    if (fViewer != null) {
      fViewer.refresh();
    }
    if (fViewPart != null) {
      fViewPart.updateLabel();
    }
  }

  @Override
  public void setFocus() {
    Control control = fViewer.getControl();
    if (control != null && !control.isDisposed()) {
      control.setFocus();
    }
  }

  @Override
  public void setID(String id) {
    fId = id;
  }

  @Override
  public void setInput(ISearchResult newSearch, Object viewState) {
    if (newSearch != null && !(newSearch instanceof AbstractTextSearchResult)) {
      return; // ignore
    }

    AbstractTextSearchResult oldSearch = fInput;
    if (oldSearch != null) {
      disconnectViewer();
      oldSearch.removeListener(fListener);
      AnnotationManagers.removeSearchResult(getSite().getWorkbenchWindow(), oldSearch);
    }
    fInput = (AbstractTextSearchResult) newSearch;

    if (fInput != null) {
      AnnotationManagers.addSearchResult(getSite().getWorkbenchWindow(), fInput);

      fInput.addListener(fListener);
      connectViewer(fInput);
      if (viewState instanceof ISelection) {
        fViewer.setSelection((ISelection) viewState, true);
      } else {
        navigateNext(true);
      }

      updateBusyLabel();
      turnOffDecoration();
      scheduleUIUpdate();

    } else {
      getViewPart().updateLabel();
    }
  }

  /**
   * Sets the layout of this search result page. The layout must be on of
   * <code>FLAG_LAYOUT_FLAT</code> or <code>FLAG_LAYOUT_TREE</code> and it must be one of the values
   * passed during construction of this search result page.
   * 
   * @param layout the new layout
   * @see AbstractTextSearchViewPage#isLayoutSupported(int)
   */
  public void setLayout(int layout) {
    Assert.isTrue(countBits(layout) == 1);
    Assert.isTrue(isLayoutSupported(layout));
    if (countBits(fSupportedLayouts) < 2) {
      return;
    }
    if (fCurrentLayout == layout) {
      return;
    }
    fCurrentLayout = layout;
    ISelection selection = fViewer.getSelection();
    disconnectViewer();
    disposeViewer();
    createViewer(fViewerContainer, layout);
    fViewerContainer.layout(true);
    connectViewer(fInput);
    fViewer.setSelection(selection, true);
    getSettings().put(KEY_LAYOUT, layout);
    getViewPart().updateLabel();
  }

  /**
   * Sets the view part
   * 
   * @param part View part to set
   */
  @Override
  public void setViewPart(ISearchResultViewPart part) {
    fViewPart = part;
  }

  /**
   * Determines whether the provided selection can be used to remove matches from the result.
   * 
   * @param selection the selection to test
   * @return returns <code>true</code> if the elements in the current selection can be removed.
   */
  protected boolean canRemoveMatchesWith(ISelection selection) {
    return !selection.isEmpty();
  }

  /**
   * This method is called whenever all elements have been removed from the shown
   * <code>AbstractSearchResult</code>. This method is guaranteed to be called in the UI thread.
   * Note that this notification is asynchronous. i.e. further changes may have occurred by the time
   * this method is called. They will be described in a future call.
   */
  protected abstract void clear();

  /**
   * Configures the given viewer. Implementers have to set at least a content provider and a label
   * provider. This method may be called if the page was constructed with the flag
   * <code>FLAG_LAYOUT_FLAT</code>.
   * 
   * @param viewer the viewer to be configured
   */
  protected abstract void configureTableViewer(TableViewer viewer);

  /**
   * Configures the given viewer. Implementers have to set at least a content provider and a label
   * provider. This method may be called if the page was constructed with the flag
   * <code>FLAG_LAYOUT_TREE</code>.
   * 
   * @param viewer the viewer to be configured
   */
  protected abstract void configureTreeViewer(TreeViewer viewer);

  /**
   * Creates the table viewer to be shown on this page. Clients may override this method.
   * 
   * @param parent the parent widget
   * @return returns a newly created <code>TableViewer</code>
   */
  protected TableViewer createTableViewer(Composite parent) {
    return new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
  }

  /**
   * Creates the tree viewer to be shown on this page. Clients may override this method.
   * 
   * @param parent the parent widget
   * @return returns a newly created <code>TreeViewer</code>.
   */
  protected TreeViewer createTreeViewer(Composite parent) {
    return new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
  }

  /**
   * This method is called whenever the set of matches for the given elements changes. This method
   * is guaranteed to be called in the UI thread. Note that this notification is asynchronous. i.e.
   * further changes may have occurred by the time this method is called. They will be described in
   * a future call.
   * <p>
   * The changed elements are evaluated by {@link #evaluateChangedElements(Match[], Set)}.
   * </p>
   * 
   * @param objects array of objects that has to be refreshed
   */
  protected abstract void elementsChanged(Object[] objects);

  /**
   * Evaluates the elements to that are later passed to {@link #elementsChanged(Object[])}. By
   * default the element to change are the elements received by ({@link Match#getElement()}). Client
   * implementations can modify this behavior.
   * 
   * @param matches the matches that were added or removed
   * @param changedElements the set that collects the elements to change. Clients should only add
   *          elements to the set.
   */
  protected void evaluateChangedElements(Match[] matches, Set<Object> changedElements) {
    for (int i = 0; i < matches.length; i++) {
      changedElements.add(matches[i].getElement());
    }
  }

  /**
   * Fills the context menu for this page. Subclasses may override this method.
   * 
   * @param mgr the menu manager representing the context menu
   */
  protected void fillContextMenu(IMenuManager mgr) {

    if (getMatchCount() > 0) {
      mgr.appendToGroup(IContextMenuConstants.GROUP_SHOW, fShowNextAction);
      mgr.appendToGroup(IContextMenuConstants.GROUP_SHOW, fShowPreviousAction);
    }
    if (!getViewer().getSelection().isEmpty()) {
      mgr.appendToGroup(IContextMenuConstants.GROUP_EDIT, fCopyToClipboardAction);
    }
    if (getMatchCount() > 0) {
      if (getLayout() == FLAG_LAYOUT_TREE) {
        mgr.appendToGroup(IContextMenuConstants.GROUP_SHOW, fExpandAllAction);
      }
    }
  }

  /**
   * Fills the toolbar contribution for this page. Subclasses may override this method.
   * 
   * @param tbm the tool bar manager representing the view's toolbar
   */
  protected void fillToolbar(IToolBarManager tbm) {
    tbm.appendToGroup(IContextMenuConstants.GROUP_OPEN, fShowNextAction);
    tbm.appendToGroup(IContextMenuConstants.GROUP_OPEN, fShowPreviousAction);
    IActionBars actionBars = getSite().getActionBars();
    if (actionBars != null) {
      actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), fShowNextAction);
      actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), fShowPreviousAction);
      actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyToClipboardAction);
      actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
    }
    if (getLayout() == FLAG_LAYOUT_TREE) {
      tbm.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, fExpandAllAction);
      tbm.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, fCollapseAllAction);
    }
  }

  /**
   * Returns a dialog settings object for this search result page. There will be one dialog settings
   * object per search result page id.
   * 
   * @return the dialog settings for this search result page
   * @see AbstractTextSearchViewPage#getID()
   */
  protected IDialogSettings getSettings() {
    IDialogSettings parent = SearchPlugin.getDefault().getDialogSettings();
    IDialogSettings settings = parent.getSection(getID());
    if (settings == null) {
      settings = parent.addNewSection(getID());
    }
    return settings;
  }

  /**
   * Returns the viewer currently used in this page.
   * 
   * @return the currently used viewer or <code>null</code> if none has been created yet.
   */
  protected StructuredViewer getViewer() {
    return fViewer;
  }

  /**
   * Returns the view part set with <code>setViewPart(ISearchResultViewPart)</code>.
   * 
   * @return The view part or <code>null</code> if the view part hasn't been set yet (or set to
   *         null).
   */
  protected ISearchResultViewPart getViewPart() {
    return fViewPart;
  }

  /**
   * <p>
   * This method is called when the search page gets an 'open' event from its underlying viewer (for
   * example on double click). The default implementation will open the first match on any element
   * that has matches. If the element to be opened is an inner node in the tree layout, the node
   * will be expanded if it's collapsed and vice versa. Subclasses are allowed to override this
   * method.
   * </p>
   * 
   * @param event the event sent for the currently shown viewer
   * @see IOpenListener
   */
  protected void handleOpen(OpenEvent event) {
    Viewer viewer = event.getViewer();
    boolean hasCurrentMatch = showCurrentMatch(OpenStrategy.activateOnOpen());
    ISelection sel = event.getSelection();
    if (viewer instanceof TreeViewer && sel instanceof IStructuredSelection) {
      IStructuredSelection selection = (IStructuredSelection) sel;
      TreeViewer tv = (TreeViewer) getViewer();
      Object element = selection.getFirstElement();
      if (element != null) {
        if (!hasCurrentMatch && getDisplayedMatchCount(element) > 0) {
          gotoNextMatch(OpenStrategy.activateOnOpen());
        } else {
          tv.setExpandedState(element, !tv.getExpandedState(element));
        }
      }
      return;
    } else if (!hasCurrentMatch) {
      gotoNextMatch(OpenStrategy.activateOnOpen());
    }
  }

  /**
   * Handles a search result event for the current search result.
   * 
   * @param e the event to handle
   */
  protected void handleSearchResultChanged(final SearchResultEvent e) {
    if (e instanceof MatchEvent) {
      postUpdate(((MatchEvent) e).getMatches());
    } else if (e instanceof RemoveAllEvent) {
      postClear();
    } else if (e instanceof FilterUpdateEvent) {
      postUpdate(((FilterUpdateEvent) e).getUpdatedMatches());
    }
  }

  /**
   * Opens an editor on the given file resource.
   * <p>
   * If the page already has an editor open on the target object then that editor is brought to
   * front; otherwise, a new editor is opened. If <code>activate == true</code> the editor will be
   * activated.
   * <p>
   * 
   * @param page the workbench page in which the editor will be opened
   * @param file the file to open
   * @param activate if <code>true</code> the editor will be activated
   * @return an open editor or <code>null</code> if an external editor was opened
   * @throws PartInitException if the editor could not be initialized
   * @see org.eclipse.ui.IWorkbenchPage#openEditor(IEditorInput, String, boolean)
   */
  protected final IEditorPart open(IWorkbenchPage page, IFile file, boolean activate)
      throws PartInitException {
    return fEditorOpener.open(page, file, activate);
  }

  /**
   * Opens an editor on the given file resource and tries to select the given offset and length.
   * <p>
   * If the page already has an editor open on the target object then that editor is brought to
   * front; otherwise, a new editor is opened. If <code>activate == true</code> the editor will be
   * activated.
   * <p>
   * 
   * @param page the workbench page in which the editor will be opened
   * @param file the file to open
   * @param offset the offset to select in the editor
   * @param length the length to select in the editor
   * @param activate if <code>true</code> the editor will be activated
   * @return an open editor or <code>null</code> if an external editor was opened
   * @throws PartInitException if the editor could not be initialized
   * @see org.eclipse.ui.IWorkbenchPage#openEditor(IEditorInput, String, boolean)
   */
  protected final IEditorPart openAndSelect(IWorkbenchPage page, IFile file, int offset,
      int length, boolean activate) throws PartInitException {
    return fEditorOpener.openAndSelect(page, file, offset, length, activate);
  }

  /**
   * Posts a UI update to make sure an element is selected.
   */
  protected void postEnsureSelection() {
    fScheduleEnsureSelection = true;
    scheduleUIUpdate();
  }

  /**
   * Opens an editor on the given element and selects the given range of text. If a search results
   * implements a <code>IFileMatchAdapter</code>, match locations will be tracked and the current
   * match range will be passed into this method.
   * 
   * @param match the match to show
   * @param currentOffset the current start offset of the match
   * @param currentLength the current length of the selection
   * @throws PartInitException if an editor can't be opened
   * @see org.eclipse.core.filebuffers.ITextFileBufferManager
   * @see IFileMatchAdapter
   * @deprecated Use {@link #showMatch(Match, int, int, boolean)} instead
   */
  @Deprecated
  protected void showMatch(Match match, int currentOffset, int currentLength)
      throws PartInitException {
  }

  /**
   * Opens an editor on the given element and selects the given range of text. If a search results
   * implements a <code>IFileMatchAdapter</code>, match locations will be tracked and the current
   * match range will be passed into this method. If the <code>activate</code> parameter is
   * <code>true</code> the opened editor should have be activated. Otherwise the focus should not be
   * changed.
   * 
   * @param match the match to show
   * @param currentOffset the current start offset of the match
   * @param currentLength the current length of the selection
   * @param activate whether to activate the editor.
   * @throws PartInitException if an editor can't be opened
   * @see org.eclipse.core.filebuffers.ITextFileBufferManager
   * @see IFileMatchAdapter
   */
  protected void showMatch(Match match, int offset, int length, boolean activate)
      throws PartInitException {
    IWorkbenchPage page = getSite().getPage();
    fEditorOpener.openAndSelect(page, match, offset, length, activate);
  }

  protected void updateActionStates() {
    boolean matches = getMatchCount() > 0;
    fShowNextAction.setEnabled(matches);
    fShowPreviousAction.setEnabled(matches);
    fExpandAllAction.setEnabled(matches);
    fCollapseAllAction.setEnabled(matches);
  }

  private void asyncExec(final Runnable runnable) {
    final Control control = getControl();
    if (control != null && !control.isDisposed()) {
      Display currentDisplay = Display.getCurrent();
      if (currentDisplay == null || !currentDisplay.equals(control.getDisplay())) {
        // meaning we're not executing on the display thread of the
        // control
        control.getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            if (!control.isDisposed()) {
              runnable.run();
            }
          }
        });
      } else {
        runnable.run();
      }
    }
  }

  private void collectAllMatches(HashSet<Match> set, Object[] elements) {
    for (int j = 0; j < elements.length; j++) {
      Match[] matches = getDisplayedMatches(elements[j]);
      for (int i = 0; i < matches.length; i++) {
        set.add(matches[i]);
      }
    }
  }

  private void collectAllMatchesBelow(AbstractTextSearchResult result, Set<Match> set,
      ITreeContentProvider cp, Object[] elements) {
    for (int j = 0; j < elements.length; j++) {
      Match[] matches = getDisplayedMatches(elements[j]);
      for (int i = 0; i < matches.length; i++) {
        set.add(matches[i]);
      }
      Object[] children = cp.getChildren(elements[j]);
      collectAllMatchesBelow(result, set, cp, children);
    }
  }

  private void connectViewer(AbstractTextSearchResult search) {
    fViewer.setInput(search);
  }

  private int countBits(int layoutFlags) {
    int bitCount = 0;
    for (int i = 0; i < 32; i++) {
      if (layoutFlags % 2 == 1) {
        bitCount++;
      }
      layoutFlags >>= 1;
    }
    return bitCount;
  }

  private Control createBusyControl() {
    Table busyLabel = new Table(fPagebook, SWT.NONE);
    TableItem item = new TableItem(busyLabel, SWT.NONE);
    item.setText(SearchMessages.AbstractTextSearchViewPage_searching_label);
    busyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    return busyLabel;
  }

  private void createLayoutActions() {
    if (countBits(fSupportedLayouts) > 1) {
      fFlatAction = new SetLayoutAction(
          this,
          SearchMessages.AbstractTextSearchViewPage_flat_layout_label,
          SearchMessages.AbstractTextSearchViewPage_flat_layout_tooltip,
          FLAG_LAYOUT_FLAT);
      fHierarchicalAction = new SetLayoutAction(
          this,
          SearchMessages.AbstractTextSearchViewPage_hierarchical_layout_label,
          SearchMessages.AbstractTextSearchViewPage_hierarchical_layout_tooltip,
          FLAG_LAYOUT_TREE);
      SearchPluginImages.setImageDescriptors(
          fFlatAction,
          SearchPluginImages.T_LCL,
          SearchPluginImages.IMG_LCL_SEARCH_FLAT_LAYOUT);
      SearchPluginImages.setImageDescriptors(
          fHierarchicalAction,
          SearchPluginImages.T_LCL,
          SearchPluginImages.IMG_LCL_SEARCH_HIERARCHICAL_LAYOUT);
    }
  }

  private IQueryListener createQueryListener() {
    return new IQueryListener() {
      @Override
      public void queryAdded(ISearchQuery query) {
        // ignore
      }

      @Override
      public void queryFinished(final ISearchQuery query) {
        // handle the end of the query in the UIUpdateJob, as ui updates
        // may not be finished here.
        postEnsureSelection();
      }

      @Override
      public void queryRemoved(ISearchQuery query) {
        // ignore
      }

      @Override
      public void queryStarting(final ISearchQuery query) {
        final Runnable runnable1 = new Runnable() {
          @Override
          public void run() {
            updateBusyLabel();
            AbstractTextSearchResult result = getInput();

            if (result == null || !result.getQuery().equals(query)) {
              return;
            }
            turnOffDecoration();
            scheduleUIUpdate();
          }

        };
        asyncExec(runnable1);
      }
    };
  }

  private void createViewer(Composite parent, int layout) {
    if ((layout & FLAG_LAYOUT_FLAT) != 0) {
      TableViewer viewer = createTableViewer(parent);
      fViewer = viewer;
      configureTableViewer(viewer);
    } else if ((layout & FLAG_LAYOUT_TREE) != 0) {
      TreeViewer viewer = createTreeViewer(parent);
      fViewer = viewer;
      configureTreeViewer(viewer);
      fCollapseAllAction.setViewer(viewer);
      fExpandAllAction.setViewer(viewer);
    }

    fCopyToClipboardAction.setViewer(fViewer);
    fSelectAllAction.setViewer(fViewer);

    IToolBarManager tbm = getSite().getActionBars().getToolBarManager();
    tbm.removeAll();
    SearchView.createToolBarGroups(tbm);
    fillToolbar(tbm);
    tbm.update(false);

    new OpenAndLinkWithEditorHelper(fViewer) {

      @Override
      protected void activate(ISelection selection) {
        final int currentMode = OpenStrategy.getOpenMethod();
        try {
          OpenStrategy.setOpenMethod(OpenStrategy.DOUBLE_CLICK);
          handleOpen(new OpenEvent(fViewer, selection));
        } finally {
          OpenStrategy.setOpenMethod(currentMode);
        }
      }

      @Override
      protected void linkToEditor(ISelection selection) {
        // not supported by this part
      }

      @Override
      protected void open(ISelection selection, boolean activate) {
        handleOpen(new OpenEvent(fViewer, selection));
      }

    };

    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        fCurrentMatchIndex = -1;
      }
    });

    fViewer.addSelectionChangedListener(fViewerAdapter);

    Menu menu = fMenu.createContextMenu(fViewer.getControl());
    fViewer.getControl().setMenu(menu);

    updateLayoutActions();
    getViewPart().updateLabel();
  }

  private void disconnectViewer() {
    fViewer.setInput(null);
  }

  private void disposeViewer() {
    fViewer.removeSelectionChangedListener(fViewerAdapter);
    fViewer.getControl().dispose();
    fViewer = null;
  }

  private Object getFirstSelectedElement() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    if (selection.size() > 0) {
      return selection.getFirstElement();
    }
    return null;
  }

  private int getMatchCount() {
    AbstractTextSearchResult result = getInput();
    if (result == null) {
      return 0;
    }
    return result.getMatchCount();
  }

  private void gotoNextMatch(boolean activateEditor) {
    fCurrentMatchIndex++;
    Match nextMatch = getCurrentMatch();
    if (nextMatch == null) {
      navigateNext(true);
      fCurrentMatchIndex = 0;
    }
    showCurrentMatch(activateEditor);
  }

  private void gotoPreviousMatch(boolean activateEditor) {
    fCurrentMatchIndex--;
    Match nextMatch = getCurrentMatch();
    if (nextMatch == null) {
      navigateNext(false);
      fCurrentMatchIndex = getDisplayedMatchCount(getFirstSelectedElement()) - 1;
    }
    showCurrentMatch(activateEditor);
  }

  private synchronized boolean hasMoreUpdates() {
    return fBatchedClearAll || fBatchedUpdates.size() > 0;
  }

  private void initActionDefinitionIDs() {
    fCopyToClipboardAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
// filtered out by a greedy activity
//    fShowNextAction.setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_NEXT);
//    fShowPreviousAction.setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_PREVIOUS);
    fSelectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);
  }

  private void initLayout() {
    if (supportsTreeLayout()) {
      fCurrentLayout = FLAG_LAYOUT_TREE;
    } else {
      fCurrentLayout = FLAG_LAYOUT_FLAT;
    }
  }

  private boolean isQueryRunning() {
    AbstractTextSearchResult result = getInput();
    if (result != null) {
      return NewSearchUI.isQueryRunning(result.getQuery());
    }
    return false;
  }

  private void navigateNext(boolean forward) {
    INavigate navigator = null;
    if (fViewer instanceof TableViewer) {
      navigator = new TableViewerNavigator((TableViewer) fViewer);
    } else {
      navigator = new TreeViewerNavigator(this, (TreeViewer) fViewer);
    }
    navigator.navigateNext(forward);
  }

  private synchronized void postClear() {
    fBatchedClearAll = true;
    fBatchedUpdates.clear();
    scheduleUIUpdate();
  }

  private synchronized void postUpdate(Match[] matches) {
    evaluateChangedElements(matches, fBatchedUpdates);
    scheduleUIUpdate();
  }

  private void runBatchedClear() {
    synchronized (this) {
      if (!fBatchedClearAll) {
        return;
      }
      fBatchedClearAll = false;
      updateBusyLabel();
    }
    getViewPart().updateLabel();
    clear();
  }

  private synchronized void runBatchedUpdates() {
    elementsChanged(fBatchedUpdates.toArray());
    fBatchedUpdates.clear();
    updateBusyLabel();
  }

  private synchronized void scheduleUIUpdate() {
    if (!fIsUIUpdateScheduled) {
      fIsUIUpdateScheduled = true;
      new UpdateUIJob().schedule();
    }
  }

  private void showBusyLabel(boolean shouldShowBusy) {
    if (shouldShowBusy) {
      fPagebook.showPage(fBusyLabel);
    } else {
      fPagebook.showPage(fViewerContainer);
    }
  }

  private boolean showCurrentMatch(boolean activateEditor) {
    Match currentMatch = getCurrentMatch();
    if (currentMatch != null) {
      showMatch(currentMatch, activateEditor);
      return true;
    }
    return false;
  }

  private void showMatch(final Match match, final boolean activateEditor) {
    ISafeRunnable runnable = new ISafeRunnable() {
      @Override
      public void handleException(Throwable exception) {
        if (exception instanceof PartInitException) {
          PartInitException pie = (PartInitException) exception;
          ErrorDialog.openError(
              getSite().getShell(),
              SearchMessages.DefaultSearchViewPage_show_match,
              SearchMessages.DefaultSearchViewPage_error_no_editor,
              pie.getStatus());
        }
      }

      @Override
      public void run() throws Exception {
        IRegion location = getCurrentMatchLocation(match);
        showMatch(match, location.getOffset(), location.getLength(), activateEditor);
      }
    };
    SafeRunner.run(runnable);
  }

  private boolean supportsTreeLayout() {
    return isLayoutSupported(FLAG_LAYOUT_TREE);
  }

  private void turnOffDecoration() {
    IBaseLabelProvider lp = fViewer.getLabelProvider();
    if (lp instanceof DecoratingLabelProvider) {
      ((DecoratingLabelProvider) lp).setLabelDecorator(null);
    }
  }

  private void turnOnDecoration() {
    IBaseLabelProvider lp = fViewer.getLabelProvider();
    if (lp instanceof DecoratingLabelProvider) {
      ((DecoratingLabelProvider) lp).setLabelDecorator(PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());

    }
  }

  private void updateBusyLabel() {
    AbstractTextSearchResult result = getInput();
    boolean shouldShowBusy = result != null && NewSearchUI.isQueryRunning(result.getQuery())
        && result.getMatchCount() == 0;
    if (shouldShowBusy == fIsBusyShown) {
      return;
    }
    fIsBusyShown = shouldShowBusy;
    showBusyLabel(fIsBusyShown);
  }

  private void updateLayoutActions() {
    if (fFlatAction != null) {
      fFlatAction.setChecked(fCurrentLayout == fFlatAction.getLayout());
    }
    if (fHierarchicalAction != null) {
      fHierarchicalAction.setChecked(fCurrentLayout == fHierarchicalAction.getLayout());
    }
  }

}
