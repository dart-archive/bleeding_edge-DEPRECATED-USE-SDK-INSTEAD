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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartElementComparator;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;
import com.google.dart.tools.ui.actions.DartSearchActionGroup;
import com.google.dart.tools.ui.actions.OpenViewActionGroup;
import com.google.dart.tools.ui.actions.RefactorActionGroup;
import com.google.dart.tools.ui.internal.actions.AbstractToggleLinkingAction;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.IProductConstants;
import com.google.dart.tools.ui.internal.text.ProductProperties;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.internal.viewsupport.ColoredViewersManager;
import com.google.dart.tools.ui.internal.viewsupport.SourcePositionComparator;
import com.google.dart.tools.ui.internal.viewsupport.StatusBarUpdater;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * The content outline page of the Dart editor. The viewer implements a proprietary update mechanism
 * based on model deltas. It does not react to domain changes. Publishes its context menu under
 * <code>DartToolsPlugin.getDefault().getPluginId() + &quot;.outline&quot;</code>.
 */
public class DartOutlinePage extends Page implements IContentOutlinePage, IAdaptable,
    IPostSelectionProvider {

  /**
   * This action toggles whether this Java Outline page links its selection to the active editor.
   */
  public class ToggleLinkingAction extends AbstractToggleLinkingAction {
    public ToggleLinkingAction() {
      boolean isLinkingEnabled = PreferenceConstants.getPreferenceStore().getBoolean(
          PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE);
      setChecked(isLinkingEnabled);
      fOpenAndLinkWithEditorHelper.setLinkWithEditor(isLinkingEnabled);
    }

    @Override
    public void run() {
      final boolean isChecked = isChecked();
      PreferenceConstants.getPreferenceStore().setValue(
          PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE,
          isChecked);
      if (isChecked && fEditor != null) {
        fEditor.synchronizeOutlinePage(fEditor.computeHighlightRangeSourceReference(), false);
      }
      fOpenAndLinkWithEditorHelper.setLinkWithEditor(isChecked);
    }
  }

  /**
   * The tree viewer used for displaying the outline.
   * 
   * @see TreeViewer
   */
  protected class DartOutlineViewer extends TreeViewer {

    /**
     * The maximum number of top-level items in a tree that will be fully expanded. Above this size
     * the tree is not expanded. The value was chosen to completely fill the outline on a large
     * screen.
     */
    private static final int MAX_AUTOEXPAND_SIZE = 200;

    /**
     * Indicates an item which has been reused. At the point of its reuse it has been expanded. This
     * field is used to communicate between <code>internalExpandToLevel</code> and
     * <code>reuseTreeItem</code>.
     */
//    private Item fReusedExpandedItem;
    private boolean fReorderedMembers;
    private boolean fForceFireSelectionChanged;

    public DartOutlineViewer(Tree tree) {
      super(tree);
      setAutoExpandLevel(ALL_LEVELS);
      setUseHashlookup(true);
    }

    /*
     * @see org.eclipse.jface.viewers.AbstractTreeViewer#isExpandable(java.lang.Object )
     */
    @Override
    public boolean isExpandable(Object element) {
      if (hasFilters()) {
        return getFilteredChildren(element).length > 0;
      }
      return super.isExpandable(element);
    }

    /**
     * Investigates the given element change event and if affected incrementally updates the Java
     * outline.
     * 
     * @param delta the Java element delta used to reconcile the Java outline
     */
    public void reconcile(DartElementDelta delta) {
      fReorderedMembers = false;
      fForceFireSelectionChanged = false;
      if (getComparator() == null) {
        if (fTopLevelTypeOnly && delta.getElement() instanceof Type
            && (delta.getKind() & DartElementDelta.ADDED) != 0) {
          refresh(true);

        } else {
          Widget w = findItem(fInput);
          if (w != null && !w.isDisposed()) {
            update(w, delta);
          }
          if (fForceFireSelectionChanged) {
            fireSelectionChanged(new SelectionChangedEvent(
                getSite().getSelectionProvider(),
                this.getSelection()));
          }
          if (fReorderedMembers) {
            refresh(false);
            fReorderedMembers = false;
          }
        }
      } else {
        // just for now
        refresh(true);
      }
    }

    @Override
    protected void createChildren(final Widget widget) {
      super.createChildren(widget);
      if (widget instanceof Control && getItemCount((Control) widget) > MAX_AUTOEXPAND_SIZE) {
        setAutoExpandLevel(0);
      }
    }

    protected boolean filtered(DartElement parent, DartElement child) {
      Object[] result = new Object[] {child};
      ViewerFilter[] filters = getFilters();
      for (int i = 0; i < filters.length; i++) {
        result = filters[i].filter(this, parent, result);
        if (result.length == 0) {
          return true;
        }
      }

      return false;
    }

    protected SourceRange getSourceRange(DartElement element) throws DartModelException {
      if (element instanceof SourceReference) {
        return ((SourceReference) element).getSourceRange();
      }
      if (element instanceof TypeMember) {
        return ((TypeMember) element).getNameRange();
      }
      return null;
    }

    /*
     * @see ContentViewer#handleLabelProviderChanged(LabelProviderChangedEvent)
     */
    @Override
    protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
      Object input = getInput();
      if (event instanceof ProblemsLabelChangedEvent) {
        ProblemsLabelChangedEvent e = (ProblemsLabelChangedEvent) event;
        if (e.isMarkerChange() && input instanceof CompilationUnit) {
          return; // marker changes can be ignored
        }
      }
      // look if the underlying resource changed
      Object[] changed = event.getElements();
      if (changed != null) {
        IResource resource = getUnderlyingResource();
        if (resource != null) {
          for (int i = 0; i < changed.length; i++) {
            if (changed[i] != null && changed[i].equals(resource)) {
              // change event to a full refresh
              event = new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource());
              break;
            }
          }
        }
      }
      super.handleLabelProviderChanged(event);
    }

    /*
     * @see TreeViewer#internalExpandToLevel
     */
    @Override
    protected void internalExpandToLevel(Widget node, int level) {
      if (node instanceof Item) {
        Item i = (Item) node;
        if (i.getData() instanceof DartElement) {
//          DartElement je = (DartElement) i.getData();
          DartX.todo(); // looking for synthetic model items (lib,app?)
//          if (je.getElementType() == DartElement.IMPORT_CONTAINER
//              || isInnerType(je)) {
//            if (i != fReusedExpandedItem) {
//              setExpanded(i, false);
//              return;
//            }
//          }
        }
      }
      super.internalExpandToLevel(node, level);
    }

    protected boolean mustUpdateParent(DartElementDelta delta, DartElement element) {
      if (element instanceof Method) {
        if ((delta.getKind() & DartElementDelta.ADDED) != 0) {
          return false;
        }
        return "main".equals(element.getElementName()); //$NON-NLS-1$
      }
      return false;
    }

    protected boolean overlaps(SourceRange range, int start, int end) {
      return start <= (range.getOffset() + range.getLength() - 1) && range.getOffset() <= end;
    }

    protected void reuseTreeItem(Item item, Object element) {

      // remove children
      Item[] c = getChildren(item);
      if (c != null && c.length > 0) {

//        if (getExpanded(item)) {
//          fReusedExpandedItem = item;
//        }

        for (int k = 0; k < c.length; k++) {
          if (c[k].getData() != null) {
            disassociate(c[k]);
          }
          c[k].dispose();
        }
      }

      updateItem(item, element);
      updatePlus(item, element);
      internalExpandToLevel(item, ALL_LEVELS);

//      fReusedExpandedItem = null;
      fForceFireSelectionChanged = true;
    }

    protected void update(Widget w, DartElementDelta delta) {

      Item item;

      DartElement parent = delta.getElement();
      DartElementDelta[] affected = delta.getAffectedChildren();
      Item[] children = getChildren(w);

      boolean doUpdateParent = false;
      boolean doUpdateParentsPlus = false;

      Vector<Item> deletions = new Vector<Item>();
      Vector<DartElementDelta> additions = new Vector<DartElementDelta>();

      for (int i = 0; i < affected.length; i++) {
        DartElementDelta affectedDelta = affected[i];
        DartElement affectedElement = affectedDelta.getElement();
        int status = affected[i].getKind();

        // find tree item with affected element
        int j;
        for (j = 0; j < children.length; j++) {
          if (affectedElement.equals(children[j].getData())) {
            break;
          }
        }

        if (j == children.length) {
          // remove from collapsed parent
          if ((status & DartElementDelta.REMOVED) != 0) {
            doUpdateParentsPlus = true;
            continue;
          }
          // addition
          if ((status & DartElementDelta.CHANGED) != 0
//              && (affectedDelta.getFlags() & DartElementDelta.F_MODIFIERS) != 0
              && !filtered(parent, affectedElement)) {
            additions.addElement(affectedDelta);
          }
          continue;
        }

        item = children[j];

        // removed
        if ((status & DartElementDelta.REMOVED) != 0) {
          deletions.addElement(item);
          doUpdateParent = doUpdateParent || mustUpdateParent(affectedDelta, affectedElement);

          // changed
        } else if ((status & DartElementDelta.CHANGED) != 0) {
          int change = affectedDelta.getFlags();
          doUpdateParent = doUpdateParent || mustUpdateParent(affectedDelta, affectedElement);

//          if ((change & DartElementDelta.F_MODIFIERS) != 0) {
//            if (filtered(parent, affectedElement))
//              deletions.addElement(item);
//            else
//              updateItem(item, affectedElement);
//          }

          if ((change & DartElementDelta.F_CONTENT) != 0) {
            updateItem(item, affectedElement);
          }

//          if ((change & DartElementDelta.F_CATEGORIES) != 0)
//            updateItem(item, affectedElement);

          if ((change & DartElementDelta.F_CHILDREN) != 0) {
            update(item, affectedDelta);
          }

          if ((change & DartElementDelta.F_REORDER) != 0) {
            fReorderedMembers = true;
          }
        }
      }

      // find all elements to add
      DartElementDelta[] add = delta.getAddedChildren();
      if (additions.size() > 0) {
        DartElementDelta[] tmp = new DartElementDelta[add.length + additions.size()];
        System.arraycopy(add, 0, tmp, 0, add.length);
        for (int i = 0; i < additions.size(); i++) {
          tmp[i + add.length] = additions.elementAt(i);
        }
        add = tmp;
      }

      // add at the right position
      go2 : for (int i = 0; i < add.length; i++) {

        try {

          DartElement e = add[i].getElement();
          if (filtered(parent, e)) {
            continue go2;
          }

          doUpdateParent = doUpdateParent || mustUpdateParent(add[i], e);
          SourceRange rng = getSourceRange(e);
          int start = rng.getOffset();
          int end = start + rng.getLength() - 1;
          int nameOffset = Integer.MAX_VALUE;
          if (e instanceof Field) {
            SourceRange nameRange = ((Field) e).getNameRange();
            if (nameRange != null) {
              nameOffset = nameRange.getOffset();
            }
          }

          Item last = null;
          item = null;
          children = getChildren(w);

          for (int j = 0; j < children.length; j++) {
            item = children[j];
            DartElement r = (DartElement) item.getData();

            if (r == null) {
              // parent node collapsed and not be opened before -> do nothing
              continue go2;
            }

            try {
              rng = getSourceRange(r);

              // multi-field declarations always start at
              // the same offset. They also have the same
              // end offset if the field sequence is terminated
              // with a semicolon. If not, the source range
              // ends behind the identifier / initializer
              // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=51851
              boolean multiFieldDeclaration = r.getElementType() == DartElement.FIELD
                  && e.getElementType() == DartElement.FIELD && rng.getOffset() == start;

              // elements are inserted by occurrence
              // however, multi-field declarations have
              // equal source ranges offsets, therefore we
              // compare name-range offsets.
              boolean multiFieldOrderBefore = false;
              if (multiFieldDeclaration) {
                if (r instanceof Field) {
                  SourceRange nameRange = ((Field) r).getNameRange();
                  if (nameRange != null) {
                    if (nameRange.getOffset() > nameOffset) {
                      multiFieldOrderBefore = true;
                    }
                  }
                }
              }

              if (!multiFieldDeclaration && overlaps(rng, start, end)) {

                // be tolerant if the delta is not correct, or if
                // the tree has been updated other than by a delta
                reuseTreeItem(item, e);
                continue go2;

              } else if (multiFieldOrderBefore || rng.getOffset() > start) {

                if (last != null && deletions.contains(last)) {
                  // reuse item
                  deletions.removeElement(last);
                  reuseTreeItem(last, e);
                } else {
                  // nothing to reuse
                  createTreeItem(w, e, j);
                }
                continue go2;
              }

            } catch (DartModelException x) {
              // stumbled over deleted element
            }

            last = item;
          }

          // add at the end of the list
          if (last != null && deletions.contains(last)) {
            // reuse item
            deletions.removeElement(last);
            reuseTreeItem(last, e);
          } else {
            // nothing to reuse
            createTreeItem(w, e, -1);
          }

        } catch (DartModelException x) {
          // the element to be added is not present -> don't add it
        }
      }

      // remove items which haven't been reused
      Enumeration<Item> e = deletions.elements();
      while (e.hasMoreElements()) {
        item = e.nextElement();
        disassociate(item);
        item.dispose();
      }

      if (doUpdateParent) {
        updateItem(w, delta.getElement());
      }
      if (!doUpdateParent && doUpdateParentsPlus && w instanceof Item) {
        updatePlus((Item) w, delta.getElement());
      }
    }

    protected void updateTreeFont() {
      Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
      Font oldFont = getTree().getFont();
      Font font = SWTUtil.changeFontSize(oldFont, newFont);
      getTree().setFont(font);
    }

    private IResource getUnderlyingResource() {
      Object input = getInput();
      if (input instanceof CompilationUnit) {
        CompilationUnit cu = (CompilationUnit) input;
        cu = cu.getPrimary();
        return cu.getResource();
      }
      return null;
    }

  }

  /**
   * The element change listener of the java outline viewer.
   * 
   * @see IElementChangedListener
   */
  protected class ElementChangedListener implements
      com.google.dart.tools.core.model.ElementChangedListener {

    @Override
    public void elementChanged(final ElementChangedEvent e) {

      if (getControl() == null) {
        return;
      }

      Display d = getControl().getDisplay();
      if (d != null) {
        d.asyncExec(new Runnable() {
          @Override
          public void run() {
            CompilationUnit cu = (CompilationUnit) fInput;
            DartElement base = cu;
            if (fTopLevelTypeOnly) {
              try {
                if (cu.getTypes().length > 0) {
                  if (fOutlineViewer != null) {
                    fOutlineViewer.refresh(true);
                  }
                  return;
                }
              } catch (DartModelException ex) {
                // ignore it
              }
            }
            DartElementDelta delta = findElement(base, e.getDelta());
            if (delta != null && fOutlineViewer != null) {
              fOutlineViewer.reconcile(delta);
            }
          }
        });
      }
    }

    protected DartElementDelta findElement(DartElement unit, DartElementDelta delta) {

      if (delta == null || unit == null) {
        return null;
      }

      DartElement element = delta.getElement();

      if (unit.equals(element)) {
        if (isPossibleStructuralChange(delta)) {
          return delta;
        }
        return null;
      }

      if (element.getElementType() >= DartElement.COMPILATION_UNIT) {
        return null;
      }

      DartElementDelta[] children = delta.getAffectedChildren();
      if (children == null || children.length == 0) {
        return null;
      }

      for (int i = 0; i < children.length; i++) {
        DartElementDelta d = findElement(unit, children[i]);
        if (d != null) {
          return d;
        }
      }

      return null;
    }

    private boolean isPossibleStructuralChange(DartElementDelta cuDelta) {
      if (cuDelta.getKind() != DartElementDelta.CHANGED) {
        return true; // add or remove
      }
      int flags = cuDelta.getFlags();
      if ((flags & DartElementDelta.F_CHILDREN) != 0) {
        return true;
      }
      return (flags & (DartElementDelta.F_CONTENT | DartElementDelta.F_FINE_GRAINED)) == DartElementDelta.F_CONTENT;
    }
  }

//  class ClassOnlyAction extends Action {
//
//    public ClassOnlyAction() {
//      super();
//      PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
//          DartHelpContextIds.GO_INTO_TOP_LEVEL_TYPE_ACTION);
//      setText(DartEditorMessages.JavaOutlinePage_GoIntoTopLevelType_label);
//      setToolTipText(DartEditorMessages.JavaOutlinePage_GoIntoTopLevelType_tooltip);
//      setDescription(DartEditorMessages.JavaOutlinePage_GoIntoTopLevelType_description);
//      JavaPluginImages.setLocalImageDescriptors(this,
//          "gointo_toplevel_type.gif"); //$NON-NLS-1$
//
//      IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
//      boolean showclass = preferenceStore.getBoolean("GoIntoTopLevelTypeAction.isChecked"); //$NON-NLS-1$
//      setTopLevelTypeOnly(showclass);
//    }
//
//    /*
//     * @see org.eclipse.jface.action.Action#run()
//     */
//    public void run() {
//      setTopLevelTypeOnly(!fTopLevelTypeOnly);
//    }
//
//    private void setTopLevelTypeOnly(boolean show) {
//      fTopLevelTypeOnly = show;
//      setChecked(show);
//      fOutlineViewer.refresh(false);
//
//      IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
//      preferenceStore.setValue("GoIntoTopLevelTypeAction.isChecked", show); //$NON-NLS-1$
//    }
//  }

  class LexicalSortingAction extends Action {

    private final DartElementComparator fComparator = new DartElementComparator();
    private final SourcePositionComparator fSourcePositonComparator = new SourcePositionComparator();

    public LexicalSortingAction() {
      super();
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          this,
          DartHelpContextIds.LEXICAL_SORTING_OUTLINE_ACTION);
      setText(DartEditorMessages.JavaOutlinePage_Sort_label);
      DartPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$
      setToolTipText(DartEditorMessages.JavaOutlinePage_Sort_tooltip);
      setDescription(DartEditorMessages.JavaOutlinePage_Sort_description);

      boolean checked = DartToolsPlugin.getDefault().getPreferenceStore().getBoolean(
          "LexicalSortingAction.isChecked"); //$NON-NLS-1$
      valueChanged(checked, false);
    }

    @Override
    public void run() {
      valueChanged(isChecked(), true);
    }

    private void valueChanged(final boolean on, boolean store) {
      setChecked(on);
      BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
        @Override
        public void run() {
          if (on) {
            fOutlineViewer.setComparator(fComparator);
          } else {
            fOutlineViewer.setComparator(fSourcePositonComparator);
          }
        }
      });

      if (store) {
        DartToolsPlugin.getDefault().getPreferenceStore().setValue(
            "LexicalSortingAction.isChecked", on); //$NON-NLS-1$
      }
    }
  }

  static class NoClassElement extends WorkbenchAdapter implements IAdaptable {
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class clas) {
      if (clas == IWorkbenchAdapter.class) {
        return this;
      }
      return null;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return DartEditorMessages.JavaOutlinePage_error_NoTopLevelType;
    }
  }

  private class CollapseAllAction extends Action {
    DartOutlineViewer fJavaOutlineViewer;

    CollapseAllAction(DartOutlineViewer viewer) {
      super("Collapse All"); //$NON-NLS-1$
      setDescription("Collapse All"); //$NON-NLS-1$
      setToolTipText("Collapse All"); //$NON-NLS-1$
      DartPluginImages.setLocalImageDescriptors(this, "collapseall.gif"); //$NON-NLS-1$

      fJavaOutlineViewer = viewer;
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          this,
          DartHelpContextIds.COLLAPSE_ALL_ACTION);
    }

    @Override
    public void run() {
      fJavaOutlineViewer.collapseAll();
    }
  }

  /**
   * Empty selection provider.
   */
  private static final class EmptySelectionProvider implements ISelectionProvider {
    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
    }

    @Override
    public ISelection getSelection() {
      return StructuredSelection.EMPTY;
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    }

    @Override
    public void setSelection(ISelection selection) {
    }
  }

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (fOutlineViewer != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          fOutlineViewer.updateTreeFont();
        }
      }
    }
  }

  static Object[] NO_CHILDREN = new Object[0];

  /** A flag to show contents of top level type only */
  private boolean fTopLevelTypeOnly;

  private DartElement fInput;
  private final String fContextMenuID;
  private Menu fMenu;
  private DartOutlineViewer fOutlineViewer;
  private DartEditor fEditor;

  //private MemberFilterActionGroup fMemberFilterActionGroup;

  private ListenerList fSelectionChangedListeners = new ListenerList(ListenerList.IDENTITY);
  private ListenerList fPostSelectionChangedListeners = new ListenerList(ListenerList.IDENTITY);
  private final Hashtable<String, IAction> fActions = new Hashtable<String, IAction>();

  private final TogglePresentationAction fTogglePresentation;

  //private ToggleLinkingAction fToggleLinkingAction;

  private CompositeActionGroup fActionGroups;

  private IPropertyChangeListener fPropertyChangeListener;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private OpenAndLinkWithEditorHelper fOpenAndLinkWithEditorHelper;

  /**
   * Custom filter action group.
   */
//  private CustomFiltersActionGroup fCustomFiltersActionGroup;
  /**
   * Category filter action group.
   */
//  private CategoryFilterActionGroup fCategoryFilterActionGroup;

  public DartOutlinePage(String contextMenuID, DartEditor editor) {
    super();

    Assert.isNotNull(editor);

    fContextMenuID = contextMenuID;
    fEditor = editor;

    fTogglePresentation = new TogglePresentationAction();
    fTogglePresentation.setEditor(editor);

    fPropertyChangeListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        doPropertyChange(event);
      }
    };
    DartToolsPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
        fPropertyChangeListener);
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
  }

  /*
   * @see org.eclipse.jface.text.IPostSelectionProvider#addPostSelectionChangedListener
   * (org.eclipse.jface.viewers.ISelectionChangedListener)
   */
  @Override
  public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
    if (fOutlineViewer != null) {
      fOutlineViewer.addPostSelectionChangedListener(listener);
    } else {
      fPostSelectionChangedListeners.add(listener);
    }
  }

  /*
   * @see ISelectionProvider#addSelectionChangedListener(ISelectionChangedListener)
   */
  @Override
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    if (fOutlineViewer != null) {
      fOutlineViewer.addSelectionChangedListener(listener);
    } else {
      fSelectionChangedListeners.add(listener);
    }
  }

  /*
   * @see IPage#createControl
   */
  @Override
  public void createControl(Composite parent) {

    Tree tree = new Tree(parent, SWT.MULTI);

//    AppearanceAwareLabelProvider lprovider = new AppearanceAwareLabelProvider(
//        AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | DartElementLabels.F_APP_TYPE_SIGNATURE
//            | DartElementLabels.ALL_CATEGORY, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);

    fOutlineViewer = new DartOutlineViewer(tree);
    ColoredViewersManager.install(fOutlineViewer);
    initDragAndDrop();
    fOutlineViewer.setContentProvider(new LibraryExplorerContentProvider(true));
    fOutlineViewer.setLabelProvider(new DartElementLabelProvider());
    fOutlineViewer.updateTreeFont();

    Object[] listeners = fSelectionChangedListeners.getListeners();
    for (int i = 0; i < listeners.length; i++) {
      fSelectionChangedListeners.remove(listeners[i]);
      fOutlineViewer.addSelectionChangedListener((ISelectionChangedListener) listeners[i]);
    }

    listeners = fPostSelectionChangedListeners.getListeners();
    for (int i = 0; i < listeners.length; i++) {
      fPostSelectionChangedListeners.remove(listeners[i]);
      fOutlineViewer.addPostSelectionChangedListener((ISelectionChangedListener) listeners[i]);
    }

    MenuManager manager = new MenuManager(fContextMenuID, fContextMenuID);
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager m) {
        contextMenuAboutToShow(m);
      }
    });
    fMenu = manager.createContextMenu(tree);
    tree.setMenu(fMenu);

    IPageSite site = getSite();
    //site.registerContextMenu(DartToolsPlugin.getPluginId() + ".outline", manager, fOutlineViewer); //$NON-NLS-1$

    updateSelectionProvider(site);

    // we must create the groups after we have set the selection provider to the
    // site

    fActionGroups = new CompositeActionGroup(new ActionGroup[] {
        new OpenViewActionGroup(this), new RefactorActionGroup(this),
        new DartSearchActionGroup(this)});

    // register global actions
    IActionBars actionBars = site.getActionBars();
    actionBars.setGlobalActionHandler(
        ITextEditorActionConstants.UNDO,
        fEditor.getAction(ITextEditorActionConstants.UNDO));
    actionBars.setGlobalActionHandler(
        ITextEditorActionConstants.REDO,
        fEditor.getAction(ITextEditorActionConstants.REDO));

    IAction action = fEditor.getAction(ITextEditorActionConstants.NEXT);
    actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, action);
    actionBars.setGlobalActionHandler(ITextEditorActionConstants.NEXT, action);
    action = fEditor.getAction(ITextEditorActionConstants.PREVIOUS);
    actionBars.setGlobalActionHandler(
        ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION,
        action);
    actionBars.setGlobalActionHandler(ITextEditorActionConstants.PREVIOUS, action);

    actionBars.setGlobalActionHandler(
        ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY,
        fTogglePresentation);

    DartX.todo();
    fActionGroups.fillActionBars(actionBars);

    IStatusLineManager statusLineManager = actionBars.getStatusLineManager();
    if (statusLineManager != null) {
      StatusBarUpdater updater = new StatusBarUpdater(statusLineManager);
      fOutlineViewer.addPostSelectionChangedListener(updater);
    }
    // Custom filter group
    DartX.todo();
//    fCustomFiltersActionGroup = new CustomFiltersActionGroup(
//        "com.google.dart.tools.ui.JavaOutlinePage", fOutlineViewer); //$NON-NLS-1$

    fOpenAndLinkWithEditorHelper = new OpenAndLinkWithEditorHelper(fOutlineViewer) {

      @Override
      protected void activate(ISelection selection) {
        fEditor.doSelectionChanged(selection);
        getSite().getPage().activate(fEditor);
      }

      @Override
      protected void linkToEditor(ISelection selection) {
        fEditor.doSelectionChanged(selection);

      }

      @Override
      protected void open(ISelection selection, boolean activate) {
        fEditor.doSelectionChanged(selection);
        if (activate) {
          getSite().getPage().activate(fEditor);
        }
      }

    };

    registerToolbarActions(actionBars);

    fOutlineViewer.setInput(fInput);
  }

  @Override
  public void dispose() {

    if (fEditor == null) {
      return;
    }

//    if (fMemberFilterActionGroup != null) {
//      fMemberFilterActionGroup.dispose();
//      fMemberFilterActionGroup = null;
//    }

    DartX.todo();
//    if (fCategoryFilterActionGroup != null) {
//      fCategoryFilterActionGroup.dispose();
//      fCategoryFilterActionGroup = null;
//    }
//
//    if (fCustomFiltersActionGroup != null) {
//      fCustomFiltersActionGroup.dispose();
//      fCustomFiltersActionGroup = null;
//    }

    fEditor.outlinePageClosed();
    fEditor = null;

    fSelectionChangedListeners.clear();
    fSelectionChangedListeners = null;

    fPostSelectionChangedListeners.clear();
    fPostSelectionChangedListeners = null;

    if (fPropertyChangeListener != null) {
      DartToolsPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(
          fPropertyChangeListener);
      fPropertyChangeListener = null;
    }
    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
      fontPropertyChangeListener = null;
    }

    if (fMenu != null && !fMenu.isDisposed()) {
      fMenu.dispose();
      fMenu = null;
    }

    if (fActionGroups != null) {
      fActionGroups.dispose();
    }

    fTogglePresentation.setEditor(null);

    fOutlineViewer = null;

    super.dispose();
  }

  public IAction getAction(String actionID) {
    Assert.isNotNull(actionID);
    return fActions.get(actionID);
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {
    if (key == IShowInSource.class) {
      return getShowInSource();
    }
    if (key == IShowInTargetList.class) {
      return new IShowInTargetList() {
        @Override
        public String[] getShowInTargetIds() {
          String explorerViewID = ProductProperties.getProperty(IProductConstants.PERSPECTIVE_EXPLORER_VIEW);
          // make sure the specified view ID is known
          if (PlatformUI.getWorkbench().getViewRegistry().find(explorerViewID) == null) {
            explorerViewID = IPageLayout.ID_PROJECT_EXPLORER;
          }
          return new String[] {explorerViewID};

        }

      };
    }
    if (key == IShowInTarget.class) {
      return getShowInTarget();
    }

    return null;
  }

  @Override
  public Control getControl() {
    if (fOutlineViewer != null) {
      return fOutlineViewer.getControl();
    }
    return null;
  }

  /*
   * @see ISelectionProvider#getSelection()
   */
  @Override
  public ISelection getSelection() {
    if (fOutlineViewer == null) {
      return StructuredSelection.EMPTY;
    }
    return fOutlineViewer.getSelection();
  }

  /*
   * (non-Javadoc) Method declared on Page
   */
  @Override
  public void init(IPageSite pageSite) {
    super.init(pageSite);
  }

  /*
   * @see org.eclipse.jface.text.IPostSelectionProvider# removePostSelectionChangedListener
   * (org.eclipse.jface.viewers.ISelectionChangedListener)
   */
  @Override
  public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
    if (fOutlineViewer != null) {
      fOutlineViewer.removePostSelectionChangedListener(listener);
    } else {
      fPostSelectionChangedListeners.remove(listener);
    }
  }

  /*
   * @see ISelectionProvider#removeSelectionChangedListener(ISelectionChangedListener )
   */
  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    if (fOutlineViewer != null) {
      fOutlineViewer.removeSelectionChangedListener(listener);
    } else {
      fSelectionChangedListeners.remove(listener);
    }
  }

  public void select(SourceReference reference) {
    if (fOutlineViewer != null) {

      ISelection s = fOutlineViewer.getSelection();
      if (s instanceof IStructuredSelection) {
        IStructuredSelection ss = (IStructuredSelection) s;
        List<?> elements = ss.toList();
        if (!elements.contains(reference)) {
          s = (reference == null ? StructuredSelection.EMPTY : new StructuredSelection(reference));
          fOutlineViewer.setSelection(s, true);
        }
      }
    }
  }

  public void setAction(String actionID, IAction action) {
    Assert.isNotNull(actionID);
    if (action == null) {
      fActions.remove(actionID);
    } else {
      fActions.put(actionID, action);
    }
  }

  /*
   * @see Page#setFocus()
   */
  @Override
  public void setFocus() {
    if (fOutlineViewer != null) {
      fOutlineViewer.getControl().setFocus();
    }
  }

  public void setInput(DartElement inputElement) {
    fInput = inputElement;
    if (fOutlineViewer != null) {
      fOutlineViewer.setInput(fInput);
      updateSelectionProvider(getSite());
    }
    DartX.todo();
//    if (fCategoryFilterActionGroup != null)
//      fCategoryFilterActionGroup.setInput(new DartElement[]{fInput});
  }

  /*
   * @see ISelectionProvider#setSelection(ISelection)
   */
  @Override
  public void setSelection(ISelection selection) {
    if (fOutlineViewer != null) {
      fOutlineViewer.setSelection(selection);
    }
  }

  /**
   * Convenience method to add the action installed under the given actionID to the specified group
   * of the menu.
   * 
   * @param menu the menu manager
   * @param group the group to which to add the action
   * @param actionID the ID of the new action
   */
  protected void addAction(IMenuManager menu, String group, String actionID) {
    IAction action = getAction(actionID);
    if (action != null) {
      if (action instanceof IUpdate) {
        ((IUpdate) action).update();
      }

      if (action.isEnabled()) {
        IMenuManager subMenu = menu.findMenuUsingPath(group);
        if (subMenu != null) {
          subMenu.add(action);
        } else {
          menu.appendToGroup(group, action);
        }
      }
    }
  }

  protected void contextMenuAboutToShow(IMenuManager menu) {

    DartToolsPlugin.createStandardGroups(menu);

    IStructuredSelection selection = (IStructuredSelection) getSelection();
    fActionGroups.setContext(new ActionContext(selection));
    fActionGroups.fillContextMenu(menu);
  }

  /**
   * Returns the <code>JavaOutlineViewer</code> of this view.
   * 
   * @return the {@link DartOutlineViewer}
   */
  protected final DartOutlineViewer getOutlineViewer() {
    return fOutlineViewer;
  }

  /**
   * Returns the <code>IShowInSource</code> for this view.
   * 
   * @return the {@link IShowInSource}
   */
  protected IShowInSource getShowInSource() {
    return new IShowInSource() {
      @Override
      public ShowInContext getShowInContext() {
        return new ShowInContext(null, getSite().getSelectionProvider().getSelection());
      }
    };
  }

  /**
   * Returns the <code>IShowInTarget</code> for this view.
   * 
   * @return the {@link IShowInTarget}
   */
  protected IShowInTarget getShowInTarget() {
    return new IShowInTarget() {
      @Override
      public boolean show(ShowInContext context) {
        ISelection sel = context.getSelection();
        if (sel instanceof ITextSelection) {
          ITextSelection tsel = (ITextSelection) sel;
          int offset = tsel.getOffset();
          DartElement element = fEditor.getElementAt(offset);
          if (element != null) {
            setSelection(new StructuredSelection(element));
            return true;
          }
        } else if (sel instanceof IStructuredSelection) {
          setSelection(sel);
          return true;
        }
        return false;
      }
    };
  }

  /**
   * Returns whether only the contents of the top level type is to be shown.
   * 
   * @return <code>true</code> if only the contents of the top level type is to be shown.
   */
  protected final boolean isTopLevelTypeOnly() {
    return fTopLevelTypeOnly;
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    if (fOutlineViewer != null) {
      DartX.todo();
//      if (MembersOrderPreferenceCache.isMemberOrderProperty(event.getProperty())) {
      fOutlineViewer.refresh(false);
//      }
    }
  }

  private void initDragAndDrop() {
    DartX.todo();
//    int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
//    Transfer[] transfers = new Transfer[]{LocalSelectionTransfer.getInstance()};
//
//    // Drop Adapter
//    TransferDropTargetListener[] dropListeners = new TransferDropTargetListener[]{new SelectionTransferDropAdapter(
//        fOutlineViewer)};
//    fOutlineViewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers,
//        new DelegatingDropAdapter(dropListeners));
//
//    // Drag Adapter
//    TransferDragSourceListener[] dragListeners = new TransferDragSourceListener[]{new SelectionTransferDragAdapter(
//        fOutlineViewer)};
//    fOutlineViewer.addDragSupport(ops, transfers, new JdtViewerDragAdapter(
//        fOutlineViewer, dragListeners));
  }

//  /**
//   * Checks whether a given Java element is an inner type.
//   * 
//   * @param element the java element
//   * @return <code>true</code> iff the given element is an inner type
//   */
//  private boolean isInnerType(DartElement element) {
//    if (element != null && element.getElementType() == DartElement.TYPE) {
//      Type type = (Type) element;
//      try {
//        return type.isMember();
//      } catch (DartModelException e) {
//        DartElement parent = type.getParent();
//        if (parent != null) {
//          int parentElementType = parent.getElementType();
//          return (parentElementType != DartElement.JAVASCRIPT_UNIT && parentElementType != DartElement.CLASS_FILE);
//        }
//      }
//    }
//    return false;
//  }

  private void registerToolbarActions(IActionBars actionBars) {
    IToolBarManager toolBarManager = actionBars.getToolBarManager();
    toolBarManager.add(new LexicalSortingAction());

//    fMemberFilterActionGroup = new MemberFilterActionGroup(fOutlineViewer,
//        "com.google.dart.tools.ui.JavaOutlinePage"); //$NON-NLS-1$
//    fMemberFilterActionGroup.contributeToToolBar(toolBarManager);

    DartX.todo();
//    fCustomFiltersActionGroup.fillActionBars(actionBars);

//    IMenuManager viewMenuManager = actionBars.getMenuManager();
//    viewMenuManager.add(new Separator("EndFilterGroup")); //$NON-NLS-1$
//
//    fToggleLinkingAction = new ToggleLinkingAction();
//    viewMenuManager.add(new ClassOnlyAction());
//    viewMenuManager.add(fToggleLinkingAction);
    new ToggleLinkingAction().run(); // initialize to enable linking; not visible to users
    toolBarManager.add(new CollapseAllAction(getOutlineViewer()));

    DartX.todo();
//    fCategoryFilterActionGroup = new CategoryFilterActionGroup(fOutlineViewer,
//        "com.google.dart.tools.ui.JavaOutlinePage", new DartElement[]{fInput}); //$NON-NLS-1$
//    fCategoryFilterActionGroup.contributeToViewMenu(viewMenuManager);
  }

  /*
   *
   */
  private void updateSelectionProvider(IPageSite site) {
    ISelectionProvider provider = fOutlineViewer;
    if (fInput != null) {
      CompilationUnit cu = fInput.getAncestor(CompilationUnit.class);
      if (cu != null && !DartModelUtil.isPrimary(cu)) {
        provider = new EmptySelectionProvider();
      }
    }
    site.setSelectionProvider(provider);
  }
}
