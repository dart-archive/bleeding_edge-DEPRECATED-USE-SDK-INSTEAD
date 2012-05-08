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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.internal.ui.Messages;
import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.ui.IContextMenuConstants;
import com.google.dart.tools.search.ui.ISearchResultViewPart;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.views.navigator.NavigatorDragAdapter;

import java.io.File;
import java.util.Set;

@SuppressWarnings("deprecation")
public class FileSearchPage extends AbstractTextSearchViewPage implements IAdaptable {

  public static class DecoratorIgnoringViewerSorter extends ViewerComparator {
    private final ILabelProvider fLabelProvider;

    public DecoratorIgnoringViewerSorter(ILabelProvider labelProvider) {
      fLabelProvider = labelProvider;
    }

    @Override
    public int category(Object element) {
      if (element instanceof IContainer) {
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

      if (e1 instanceof LineElement && e2 instanceof LineElement) {
        LineElement m1 = (LineElement) e1;
        LineElement m2 = (LineElement) e2;
        return m1.getOffset() - m2.getOffset();
      }

      String name1 = fLabelProvider.getText(e1);
      String name2 = fLabelProvider.getText(e2);
      if (name1 == null) {
        name1 = "";//$NON-NLS-1$
      }
      if (name2 == null) {
        name2 = "";//$NON-NLS-1$
      }
      return getComparator().compare(name1, name2);
    }
  }

  private static final String KEY_SORTING = "com.google.dart.tools.search.resultpage.sorting"; //$NON-NLS-1$
  private static final String KEY_LIMIT = "com.google.dart.tools.search.resultpage.limit"; //$NON-NLS-1$

  private static final int DEFAULT_ELEMENT_LIMIT = 1000;

  private ActionGroup fActionGroup;
  private IFileSearchContentProvider fContentProvider;
  private int fCurrentSortOrder;
  private SortAction fSortByNameAction;
  private SortAction fSortByPathAction;

  public FileSearchPage() {
    fSortByNameAction = new SortAction(SearchMessages.FileSearchPage_sort_name_label, this,
        FileLabelProvider.SHOW_LABEL_PATH);
    fSortByPathAction = new SortAction(SearchMessages.FileSearchPage_sort_path_label, this,
        FileLabelProvider.SHOW_PATH_LABEL);

    setElementLimit(new Integer(DEFAULT_ELEMENT_LIMIT));
  }

  @Override
  public void dispose() {
    fActionGroup.dispose();
    super.dispose();
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    return null;
  }

  @Override
  public int getDisplayedMatchCount(Object element) {
    if (showLineMatches()) {
      if (element instanceof LineElement) {
        LineElement lineEntry = (LineElement) element;
        return lineEntry.getNumberOfMatches(getInput());
      }
      return 0;
    }
    return super.getDisplayedMatchCount(element);
  }

  @Override
  public Match[] getDisplayedMatches(Object element) {
    if (showLineMatches()) {
      if (element instanceof LineElement) {
        LineElement lineEntry = (LineElement) element;
        return lineEntry.getMatches(getInput());
      }
      return new Match[0];
    }
    return super.getDisplayedMatches(element);
  }

  @Override
  public String getLabel() {
    String label = super.getLabel();
    StructuredViewer viewer = getViewer();
    if (viewer instanceof TableViewer) {
      TableViewer tv = (TableViewer) viewer;

      AbstractTextSearchResult result = getInput();
      if (result != null) {
        int itemCount = ((IStructuredContentProvider) tv.getContentProvider()).getElements(getInput()).length;
        if (showLineMatches()) {
          int matchCount = getInput().getMatchCount();
          if (itemCount < matchCount) {
            return Messages.format(SearchMessages.FileSearchPage_limited_format_matches,
                new Object[] {label, new Integer(itemCount), new Integer(matchCount)});
          }
        } else {
          int fileCount = getInput().getElements().length;
          if (itemCount < fileCount) {
            return Messages.format(SearchMessages.FileSearchPage_limited_format_files,
                new Object[] {label, new Integer(itemCount), new Integer(fileCount)});
          }
        }
      }
    }
    return label;
  }

  @Override
  public StructuredViewer getViewer() {
    return super.getViewer();
  }

  @Override
  public void restoreState(IMemento memento) {
    super.restoreState(memento);
    try {
      fCurrentSortOrder = getSettings().getInt(KEY_SORTING);
    } catch (NumberFormatException e) {
      fCurrentSortOrder = fSortByNameAction.getSortOrder();
    }
    int elementLimit = DEFAULT_ELEMENT_LIMIT;
    try {
      elementLimit = getSettings().getInt(KEY_LIMIT);
    } catch (NumberFormatException e) {
    }
    if (memento != null) {
      Integer value = memento.getInteger(KEY_SORTING);
      if (value != null) {
        fCurrentSortOrder = value.intValue();
      }

      value = memento.getInteger(KEY_LIMIT);
      if (value != null) {
        elementLimit = value.intValue();
      }
    }
    setElementLimit(new Integer(elementLimit));
  }

  @Override
  public void saveState(IMemento memento) {
    super.saveState(memento);
    memento.putInteger(KEY_SORTING, fCurrentSortOrder);
    memento.putInteger(KEY_LIMIT, getElementLimit().intValue());
  }

  @Override
  public void setElementLimit(Integer elementLimit) {
    super.setElementLimit(elementLimit);
    int limit = elementLimit.intValue();
    getSettings().put(KEY_LIMIT, limit);
  }

  public void setSortOrder(int sortOrder) {
    fCurrentSortOrder = sortOrder;
    DecoratingFileSearchLabelProvider lpWrapper = (DecoratingFileSearchLabelProvider) getViewer().getLabelProvider();
    ((FileLabelProvider) lpWrapper.getStyledStringProvider()).setOrder(sortOrder);
    getViewer().refresh();
    getSettings().put(KEY_SORTING, fCurrentSortOrder);
  }

  @Override
  public void setViewPart(ISearchResultViewPart part) {
    super.setViewPart(part);
    fActionGroup = new NewTextSearchActionGroup(part);
  }

  @Override
  protected void clear() {
    if (fContentProvider != null) {
      fContentProvider.clear();
    }
  }

  @Override
  protected void configureTableViewer(TableViewer viewer) {
    viewer.setUseHashlookup(true);
    FileLabelProvider innerLabelProvider = new FileLabelProvider(this, fCurrentSortOrder);
    viewer.setLabelProvider(new DecoratingFileSearchLabelProvider(innerLabelProvider));
    viewer.setContentProvider(new FileTableContentProvider(this));
    viewer.setComparator(new DecoratorIgnoringViewerSorter(innerLabelProvider));
    fContentProvider = (IFileSearchContentProvider) viewer.getContentProvider();
    addDragAdapters(viewer);
  }

  @Override
  protected void configureTreeViewer(TreeViewer viewer) {
    viewer.setUseHashlookup(true);
    FileLabelProvider innerLabelProvider = new FileLabelProvider(this, FileLabelProvider.SHOW_LABEL);
    viewer.setLabelProvider(new DecoratingFileSearchLabelProvider(innerLabelProvider));
    viewer.setContentProvider(new FileTreeContentProvider(this, viewer));
    viewer.setComparator(new DecoratorIgnoringViewerSorter(innerLabelProvider));
    fContentProvider = (IFileSearchContentProvider) viewer.getContentProvider();
    addDragAdapters(viewer);
  }

  @Override
  protected void elementsChanged(Object[] objects) {
    if (fContentProvider != null) {
      fContentProvider.elementsChanged(objects);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  protected void evaluateChangedElements(Match[] matches, Set changedElements) {
    if (showLineMatches()) {
      for (int i = 0; i < matches.length; i++) {
        changedElements.add(((FileResourceMatch) matches[i]).getLineElement());
      }
    } else {
      super.evaluateChangedElements(matches, changedElements);
    }
  }

  @Override
  protected void fillContextMenu(IMenuManager mgr) {
    super.fillContextMenu(mgr);
    addSortActions(mgr);
    fActionGroup.setContext(new ActionContext(getSite().getSelectionProvider().getSelection()));
    fActionGroup.fillContextMenu(mgr);
//refactoring support		
//		FileSearchQuery query= (FileSearchQuery) getInput().getQuery();
//		if (query.getSearchString().length() > 0) {
//			IStructuredSelection selection= (IStructuredSelection) getViewer().getSelection();
//			if (!selection.isEmpty()) {
//				ReplaceAction replaceSelection= new ReplaceAction(getSite().getShell(), (FileSearchResult)getInput(), selection.toArray());
//				replaceSelection.setText(SearchMessages.ReplaceAction_label_selected);
//				mgr.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, replaceSelection);
//
//			}
//			ReplaceAction replaceAll= new ReplaceAction(getSite().getShell(), (FileSearchResult)getInput(), null);
//			replaceAll.setText(SearchMessages.ReplaceAction_label_all);
//			mgr.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, replaceAll);
//		}
  }

  @Override
  protected void handleOpen(OpenEvent event) {
    if (showLineMatches()) {
      Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
      if (firstElement instanceof IFile) {
        if (getDisplayedMatchCount(firstElement) == 0) {
          try {
            open(getSite().getPage(), (IFile) firstElement, false);
          } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(),
                SearchMessages.FileSearchPage_open_file_dialog_title,
                SearchMessages.FileSearchPage_open_file_failed, e.getStatus());
          }
          return;
        }
      }
      if (firstElement instanceof File) {
        IFileStore file = EFS.getLocalFileSystem().fromLocalFile((File) firstElement);
        try {
          IDE.openEditorOnFileStore(getSite().getPage(), file);
        } catch (PartInitException e) {
          ErrorDialog.openError(getSite().getShell(),
              SearchMessages.FileSearchPage_open_file_dialog_title,
              SearchMessages.FileSearchPage_open_file_failed, e.getStatus());
        }
      }
    }
    super.handleOpen(event);
  }

  private void addDragAdapters(StructuredViewer viewer) {
    Transfer[] transfers = new Transfer[] {ResourceTransfer.getInstance()};
    int ops = DND.DROP_COPY | DND.DROP_LINK;
    viewer.addDragSupport(ops, transfers, new NavigatorDragAdapter(viewer));
  }

  private void addSortActions(IMenuManager mgr) {
    if (getLayout() != FLAG_LAYOUT_FLAT) {
      return;
    }
    MenuManager sortMenu = new MenuManager(SearchMessages.FileSearchPage_sort_by_label);
    sortMenu.add(fSortByNameAction);
    sortMenu.add(fSortByPathAction);

    fSortByNameAction.setChecked(fCurrentSortOrder == fSortByNameAction.getSortOrder());
    fSortByPathAction.setChecked(fCurrentSortOrder == fSortByPathAction.getSortOrder());

    mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortMenu);
  }

  private boolean showLineMatches() {
    //TODO(pquitslund): line match presentation disabled
    return false;

//    AbstractTextSearchResult input = getInput();
//    return getLayout() == FLAG_LAYOUT_TREE && input != null
//        && !((FileSearchQuery) input.getQuery()).isFileNameSearch();
  }

}
