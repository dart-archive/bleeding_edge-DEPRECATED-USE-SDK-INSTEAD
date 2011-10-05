/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.IWorkingCopyProvider;
import com.google.dart.tools.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends a TreeViewer to allow more performance when showing error ticks. A
 * <code>ProblemItemMapper</code> is contained that maps all items in the tree to underlying
 * resource
 */
public class ProblemTreeViewer extends TreeViewer implements
    ResourceToItemsMapper.IContentViewerAccessor {

  protected ResourceToItemsMapper fResourceToItemsMapper;

  /*
   * @see TreeViewer#TreeViewer(Composite)
   */
  public ProblemTreeViewer(Composite parent) {
    super(parent);
    initMapper();
  }

  /*
   * @see TreeViewer#TreeViewer(Composite, int)
   */
  public ProblemTreeViewer(Composite parent, int style) {
    super(parent, style);
    initMapper();
  }

  /*
   * @see TreeViewer#TreeViewer(Tree)
   */
  public ProblemTreeViewer(Tree tree) {
    super(tree);
    initMapper();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.StructuredViewer#addFilter(org.eclipse.jface.
   * viewers.ViewerFilter)
   */
  @Override
  public void addFilter(ViewerFilter filter) {
    if (filter instanceof DartViewerFilter) {
      ((DartViewerFilter) filter).filteringStart();
    }
    super.addFilter(filter);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.dart.tools.ui.internal.viewsupport.ResourceToItemsMapper.
   * IContentViewerAccessor#doUpdateItem(org.eclipse.swt.widgets.Widget)
   */
  @Override
  public void doUpdateItem(Widget item) {
    doUpdateItem(item, item.getData(), true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.AbstractTreeViewer#isExpandable(java.lang.Object)
   */
  @Override
  public boolean isExpandable(Object parent) {
    if (hasFilters() && evaluateExpandableWithFilters(parent)) {
      // workaround for 65762
      return hasFilteredChildren(parent);
    }
    return super.isExpandable(parent);
  }

  /**
   * Public method to test if a element is filtered by the views active filters
   * 
   * @param object the element to test for
   * @param parent the parent element
   * @return return <code>true if the element is filtered</code>
   */
  public boolean isFiltered(Object object, Object parent) {
    return isFiltered(object, parent, getFilters());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.StructuredViewer#removeFilter(org.eclipse.jface
   * .viewers.ViewerFilter)
   */
  @Override
  public void removeFilter(ViewerFilter filter) {
    super.removeFilter(filter);
    if (filter instanceof DartViewerFilter) {
      ((DartViewerFilter) filter).filteringEnd();
    }
  }

  // ---------------- filter sessions ----------------------------

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.StructuredViewer#resetFilters()
   */
  @Override
  public void resetFilters() {
    endFilterSessions(getFilters());
    super.resetFilters();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.StructuredViewer#setFilters(org.eclipse.jface
   * .viewers.ViewerFilter[])
   */
  @Override
  public void setFilters(ViewerFilter[] filters) {
    ViewerFilter[] oldFilters = getFilters();
    for (int i = 0; i < filters.length; i++) {
      ViewerFilter curr = filters[i];
      if (curr instanceof DartViewerFilter && !findAndRemove(oldFilters, curr)) {
        ((DartViewerFilter) curr).filteringStart();
      }
    }
    endFilterSessions(oldFilters);
    super.setFilters(filters);
  }

  protected Object[] addAditionalProblemParents(Object[] elements) {
    return elements;
  }

  /**
   * Decides if {@link #isExpandable(Object)} should also test filters. The default behaviour is to
   * do this only for IMembers. Implementors can replace this behaviour.
   * 
   * @param parent the given element
   * @return returns if if {@link #isExpandable(Object)} should also test filters for the given
   *         element.
   */
  protected boolean evaluateExpandableWithFilters(Object parent) {
    return parent instanceof TypeMember;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.StructuredViewer#filter(java.lang.Object[])
   */
  @Override
  protected final Object[] filter(Object[] elements) {
    return filter(elements, getRoot());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.AbstractTreeViewer#getFilteredChildren(java.lang .Object)
   */
  @Override
  protected final Object[] getFilteredChildren(Object parent) {
    return filter(getRawChildren(parent), parent);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.StructuredViewer#handleDispose(org.eclipse.swt
   * .events.DisposeEvent)
   */
  @Override
  protected void handleDispose(DisposeEvent event) {
    endFilterSessions(getFilters());
    super.handleDispose(event);
  }

  /*
   * @see ContentViewer#handleLabelProviderChanged(LabelProviderChangedEvent)
   */
  @Override
  protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
    if (event instanceof ProblemsLabelChangedEvent) {
      ProblemsLabelChangedEvent e = (ProblemsLabelChangedEvent) event;
      if (!e.isMarkerChange() && canIgnoreChangesFromAnnotionModel()) {
        return;
      }
    }
    Object[] changed = addAditionalProblemParents(event.getElements());

    if (changed != null && !fResourceToItemsMapper.isEmpty()) {
      ArrayList<Object> others = new ArrayList<Object>();
      for (int i = 0; i < changed.length; i++) {
        Object curr = changed[i];
        if (curr instanceof IResource) {
          fResourceToItemsMapper.resourceChanged((IResource) curr);
        } else {
          others.add(curr);
        }
      }
      if (others.isEmpty()) {
        return;
      }
      event = new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(),
          others.toArray());
    } else {
      // we have modified the list of changed elements via add additional
// parents.
      if (event.getElements() != changed) {
        event = new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(), changed);
      }
    }
    super.handleLabelProviderChanged(event);
  }

  protected final boolean hasFilteredChildren(Object parent) {
    Object[] rawChildren = getRawChildren(parent);
    return containsNonFiltered(rawChildren, parent);
  }

  /**
   * All element filter tests must go through this method. Can be overridden by subclasses.
   * 
   * @param object the object to filter
   * @param parent the parent
   * @param filters the filters to apply
   * @return true if the element is filtered
   */
  protected boolean isFiltered(Object object, Object parent, ViewerFilter[] filters) {
    for (int i = 0; i < filters.length; i++) {
      ViewerFilter filter = filters[i];
      if (!filter.select(this, parent, object)) {
        return true;
      }
    }
    return false;
  }

  /*
   * @see StructuredViewer#mapElement(Object, Widget)
   */
  @Override
  protected void mapElement(Object element, Widget item) {
    super.mapElement(element, item);
    if (item instanceof Item) {
      fResourceToItemsMapper.addToMap(element, (Item) item);
    }
  }

  /*
   * @see StructuredViewer#unmapAllElements()
   */
  @Override
  protected void unmapAllElements() {
    fResourceToItemsMapper.clearMap();
    super.unmapAllElements();
  }

  /*
   * @see StructuredViewer#unmapElement(Object, Widget)
   */
  @Override
  protected void unmapElement(Object element, Widget item) {
    if (item instanceof Item) {
      fResourceToItemsMapper.removeFromMap(element, (Item) item);
    }
    super.unmapElement(element, item);
  }

  /**
   * Answers whether this viewer can ignore label provider changes resulting from marker changes in
   * annotation models
   * 
   * @return return <code>true</code> if annotation model marker changes can be ignored
   */
  private boolean canIgnoreChangesFromAnnotionModel() {
    Object contentProvider = getContentProvider();
    return contentProvider instanceof IWorkingCopyProvider
        && !((IWorkingCopyProvider) contentProvider).providesWorkingCopies();
  }

  private boolean containsNonFiltered(Object[] elements, Object parent) {
    if (elements.length == 0) {
      return false;
    }
    if (!hasFilters()) {
      return true;
    }
    ViewerFilter[] filters = getFilters();
    for (int i = 0; i < elements.length; i++) {
      Object object = elements[i];
      if (!isFiltered(object, parent, filters)) {
        return true;
      }
    }
    return false;
  }

  private void endFilterSessions(ViewerFilter[] filters) {
    for (int i = 0; i < filters.length; i++) {
      ViewerFilter curr = filters[i];
      if (curr instanceof DartViewerFilter) {
        ((DartViewerFilter) curr).filteringEnd();
      }
    }
  }

  private Object[] filter(Object[] elements, Object parent) {
    if (!hasFilters() || elements.length == 0) {
      return elements;
    }
    List<Object> list = new ArrayList<Object>(elements.length);
    ViewerFilter[] filters = getFilters();
    for (int i = 0; i < elements.length; i++) {
      Object object = elements[i];
      if (!isFiltered(object, parent, filters)) {
        list.add(object);
      }
    }
    return list.toArray();
  }

  private boolean findAndRemove(ViewerFilter[] filters, ViewerFilter filter) {
    for (int i = 0; i < filters.length; i++) {
      if (filters[i] == filter) {
        filters[i] = null;
        return true;
      }
    }
    return false;
  }

  private void initMapper() {
    fResourceToItemsMapper = new ResourceToItemsMapper(this);
  }
}
