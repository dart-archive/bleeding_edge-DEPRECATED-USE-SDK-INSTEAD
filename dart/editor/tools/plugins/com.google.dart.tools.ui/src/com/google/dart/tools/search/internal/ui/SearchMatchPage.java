/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.search.internal.ui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.text.editor.NewDartElementLabelProvider;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.progress.UIJob;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract {@link SearchPage} for displaying {@link SearchMatch}s.
 */
public abstract class SearchMatchPage extends SearchPage {
  /**
   * Item in search results tree.
   */
  private static class ResultItem {
    private final Element element;
    private final List<SourceRange> sourceRanges = Lists.newArrayList();
    private final List<ResultItem> children = Lists.newArrayList();
    private ResultItem parent;
    private int numMatches;

    public ResultItem(Element element, SourceRange sourceRange) {
      this.element = element;
      if (sourceRange != null) {
        sourceRanges.add(sourceRange);
      }
      numMatches = sourceRange != null ? 1 : 0;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ResultItem)) {
        return false;
      }
      return ObjectUtils.equals(((ResultItem) obj).element, element);
    }

    @Override
    public int hashCode() {
      return element != null ? element.hashCode() : 0;
    }

    public void merge(ResultItem item) {
      numMatches += item.numMatches;
      sourceRanges.addAll(item.sourceRanges);
    }

    void addChild(ResultItem child) {
      if (child.parent == null) {
        child.parent = this;
        children.add(child);
      }
    }
  }
  /**
   * {@link ITreeContentProvider} for {@link ResultItem}s.
   */
  private static class SearchContentProvider implements ITreeContentProvider {
    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      ResultItem item = (ResultItem) parentElement;
      List<ResultItem> rootChildren = item.children;
      return rootChildren.toArray(new ResultItem[rootChildren.size()]);
    }

    @Override
    public Object[] getElements(Object inputElement) {
      return getChildren(inputElement);
    }

    @Override
    public Object getParent(Object element) {
      return ((ResultItem) element).parent;
    }

    @Override
    public boolean hasChildren(Object element) {
      return getChildren(element).length != 0;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }

  /**
   * {@link ILabelProvider} for {@link ResultItem}s.
   */
  private static class SearchLabelProvider extends NewDartElementLabelProvider {
    @Override
    public Image getImage(Object elem) {
      ResultItem item = (ResultItem) elem;
      return super.getImage(item.element);
    }

    @Override
    public StyledString getStyledText(Object elem) {
      ResultItem item = (ResultItem) elem;
      StyledString styledText = super.getStyledText(item.element);
      if (item.numMatches == 1) {
        styledText.append(" (1 match)", StyledString.COUNTER_STYLER);
      } else if (item.numMatches > 1) {
        styledText.append(" (" + item.numMatches + " matches)", StyledString.COUNTER_STYLER);
      }
      return styledText;
    }
  }

  private static final ITreeContentProvider CONTENT_PROVIDER = new SearchContentProvider();
  private static final IBaseLabelProvider LABEL_PROVIDER = new DelegatingStyledCellLabelProvider(
      new SearchLabelProvider());

  /**
   * Adds new {@link ResultItem} to the tree.
   */
  private static ResultItem addResultItem(Map<Element, ResultItem> itemMap, ResultItem child) {
    // put child
    Element childElement = child.element;
    {
      ResultItem existingChild = itemMap.get(childElement);
      if (existingChild == null) {
        itemMap.put(childElement, child);
      } else {
        existingChild.merge(child);
        child = existingChild;
      }
    }
    // bind child to parent
    if (childElement != null) {
      Element parentElement = childElement.getEnclosingElement();
      ResultItem parent = new ResultItem(parentElement, null);
      parent = addResultItem(itemMap, parent);
      parent.addChild(child);
    }
    // done
    return child;
  }

  /**
   * Builds {@link ResultItem} tree out of the given {@link SearchMatch}s.
   */
  private static ResultItem buildResultItemTree(List<SearchMatch> matches) {
    ResultItem rootItem = new ResultItem(null, null);
    Map<Element, ResultItem> itemMap = Maps.newHashMap();
    itemMap.put(null, rootItem);
    for (SearchMatch match : matches) {
      Element element = getResultItemElement(match.getElement());
      ResultItem child = new ResultItem(element, match.getSourceRange());
      addResultItem(itemMap, child);
    }
    calculateNumMatches(rootItem);
    return rootItem;
  }

  /**
   * Recursively calculates {@link ResultItem#numMatches} fields.
   */
  private static int calculateNumMatches(ResultItem item) {
    int result = item.sourceRanges.size();
    for (ResultItem child : item.children) {
      result += calculateNumMatches(child);
    }
    item.numMatches = result;
    return result;
  }

  /**
   * @return the {@link SourceRange} with minimal offset.
   */
  private static SourceRange findFirst(List<SourceRange> sourceRanges) {
    SourceRange result = null;
    for (SourceRange sourceRange : sourceRanges) {
      if (result == null || result.getOffset() > sourceRange.getOffset()) {
        result = sourceRange;
      }
    }
    return result;
  }

  /**
   * @return the {@link Element} to use as enclosing in {@link ResultItem} tree.
   */
  private static Element getResultItemElement(Element element) {
    while (element != null) {
      Element executable = element.getAncestor(ExecutableElement.class);
      if (executable == null) {
        break;
      }
      element = executable;
    }
    return element;
  }

  private IAction removeAction = new Action() {
    {
      setToolTipText("Remove Selected Matches");
      DartPluginImages.setLocalImageDescriptors(this, "search_rem.gif");
    }

    @Override
    public void run() {
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
      for (Iterator<?> iter = selection.toList().iterator(); iter.hasNext();) {
        ResultItem item = (ResultItem) iter.next();
        while (item != null && item.element != null) {
          ResultItem parent = item.parent;
          parent.children.remove(item);
          if (!parent.children.isEmpty()) {
            break;
          }
          item = parent;
        }
      }
      calculateNumMatches(rootItem);
      // update viewer
      viewer.refresh();
      // update markers
      addMarkers();
    }
  };

  private IAction removeAllAction = new Action() {
    {
      setToolTipText("Remove All Matches");
      DartPluginImages.setLocalImageDescriptors(this, "search_remall.gif");
    }

    @Override
    public void run() {
      searchView.showPage(null);
    }
  };

  private IAction refreshAction = new Action() {
    {
      setToolTipText("Refresh the Current Search");
      DartPluginImages.setLocalImageDescriptors(this, "refresh.gif");
    }

    @Override
    public void run() {
      refresh();
    }
  };

  private IAction expandAllAction = new Action() {
    {
      setToolTipText("Expand All");
      DartPluginImages.setLocalImageDescriptors(this, "expandall.gif");
    }

    @Override
    public void run() {
      viewer.expandAll();
    }
  };

  private IAction collapseAllAction = new Action() {
    {
      setToolTipText("Collapse All");
      DartPluginImages.setLocalImageDescriptors(this, "collapseall.gif");
    }

    @Override
    public void run() {
      viewer.collapseAll();
    }
  };
  private final SearchView searchView;
  private final String taskName;
  private final Set<IResource> markerResources = Sets.newHashSet();
  private TreeViewer viewer;
  private ResultItem rootItem;

  public SearchMatchPage(SearchView searchView, String taskName) {
    this.searchView = searchView;
    this.taskName = taskName;
  }

  @Override
  public void createControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
    viewer.setContentProvider(CONTENT_PROVIDER);
    viewer.setLabelProvider(LABEL_PROVIDER);
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        ISelection selection = event.getSelection();
        openSelectedElement(selection);
      }
    });
    SearchView.updateColors(viewer.getControl());
  }

  @Override
  public void dispose() {
    super.dispose();
    removeMarkers();
  }

  @Override
  public Control getControl() {
    return viewer.getControl();
  }

  @Override
  public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
      IStatusLineManager statusLineManager) {
    toolBarManager.add(removeAction);
    toolBarManager.add(removeAllAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(expandAllAction);
    toolBarManager.add(collapseAllAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(refreshAction);
  }

  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  @Override
  public void show() {
    refresh();
  }

  /**
   * Runs a {@link SearchEngine} request.
   * 
   * @return the {@link SearchMatch}s to display.
   */
  protected abstract List<SearchMatch> runQuery();

  /**
   * Adds markers for all {@link ResultItem}s starting from {@link #rootItem}.
   */
  private void addMarkers() {
    try {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          removeMarkers();
          markerResources.clear();
          addMarkers(rootItem);
        }
      }, null);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }

  /**
   * Adds markers for the given {@link ResultItem} and its children.
   */
  private void addMarkers(ResultItem item) throws CoreException {
    // add marker if leaf
    if (!item.sourceRanges.isEmpty()) {
      Source source = item.element.getSource();
      IResource resource = DartCore.getProjectManager().getResource(source);
      if (resource != null && resource.exists()) {
        markerResources.add(resource);
        try {
          List<SourceRange> sourceRanges = item.sourceRanges;
          for (SourceRange sourceRange : sourceRanges) {
            IMarker marker = resource.createMarker(SearchView.SEARCH_MARKER);
            marker.setAttribute(IMarker.CHAR_START, sourceRange.getOffset());
            marker.setAttribute(IMarker.CHAR_END, sourceRange.getEnd());
          }
        } catch (Throwable e) {
        }
      }
    }
    // process children
    for (ResultItem child : item.children) {
      addMarkers(child);
    }
  }

  /**
   * Analyzes each {@link ResultItem} and expends it in {@link #viewer} only if it has not too much
   * children. So, user will see enough information, but not too much.
   */
  private void expandWhileSmallNumberOfChildren(List<ResultItem> items) {
    for (ResultItem item : items) {
      if (item.children.size() <= 5) {
        viewer.setExpandedState(item, true);
        expandWhileSmallNumberOfChildren(item.children);
      }
    }
  }

  /**
   * Opens selected {@link ResultItem} in the editor.
   */
  private void openSelectedElement(ISelection selection) {
    // need IStructuredSelection
    if (!(selection instanceof IStructuredSelection)) {
      return;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    // only single element
    if (structuredSelection.size() != 1) {
      return;
    }
    Object firstElement = structuredSelection.getFirstElement();
    // should be ResultItem
    if (!(firstElement instanceof ResultItem)) {
      return;
    }
    ResultItem item = (ResultItem) firstElement;
    Element element = item.element;
    // prepare SourceRange to reveal
    SourceRange sourceRange;
    if (!item.sourceRanges.isEmpty()) {
      sourceRange = findFirst(item.sourceRanges);
    } else if (element instanceof CompilationUnitElement || element instanceof LibraryElement) {
      sourceRange = null;
    } else {
      sourceRange = SourceRangeFactory.rangeElementName(element);
    }
    // show Element and SourceRange
    try {
      IEditorPart editor = DartUI.openInEditor(element);
      if (sourceRange != null) {
        EditorUtility.revealInEditor(editor, sourceRange);
      }
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Search", "Exception during open.");
    }
  }

  /**
   * Runs background {@link Job} to fetch {@link SearchMatch}s and then displays them in the
   * {@link #viewer}.
   */
  private void refresh() {
    try {
      new Job(taskName) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          List<SearchMatch> matches = runQuery();
          rootItem = buildResultItemTree(matches);
          // add markers
          addMarkers();
          // schedule UI update
          new UIJob("Displaying search results...") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
              Object[] expandedElements = viewer.getExpandedElements();
              viewer.setInput(rootItem);
              viewer.setExpandedElements(expandedElements);
              expandWhileSmallNumberOfChildren(rootItem.children);
              return Status.OK_STATUS;
            }
          }.schedule();
          // done
          return Status.OK_STATUS;
        }
      }.schedule();
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Search", "Exception during search.");
    }
  }

  /**
   * Removes all search markers from {@link #markerResources}.
   */
  private void removeMarkers() {
    try {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          for (IResource resource : markerResources) {
            if (resource.exists()) {
              try {
                resource.deleteMarkers(SearchView.SEARCH_MARKER, false, IResource.DEPTH_ZERO);
              } catch (Throwable e) {
              }
            }
          }
        }
      }, null);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }
}
