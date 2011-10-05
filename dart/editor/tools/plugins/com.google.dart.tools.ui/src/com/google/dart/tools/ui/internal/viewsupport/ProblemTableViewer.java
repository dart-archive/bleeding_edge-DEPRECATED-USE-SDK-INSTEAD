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

import com.google.dart.tools.ui.IWorkingCopyProvider;
import com.google.dart.tools.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

import java.util.ArrayList;

/**
 * Extends a TableViewer to allow more performance when showing error ticks. A
 * <code>ProblemItemMapper</code> is contained that maps all items in the tree to underlying
 * resource
 */
public class ProblemTableViewer extends TableViewer implements
    ResourceToItemsMapper.IContentViewerAccessor {

  protected ResourceToItemsMapper fResourceToItemsMapper;

  /**
   * Constructor for ProblemTableViewer.
   * 
   * @param parent
   */
  public ProblemTableViewer(Composite parent) {
    super(parent);
    initMapper();
  }

  /**
   * Constructor for ProblemTableViewer.
   * 
   * @param parent
   * @param style
   */
  public ProblemTableViewer(Composite parent, int style) {
    super(parent, style);
    initMapper();
  }

  /**
   * Constructor for ProblemTableViewer.
   * 
   * @param table
   */
  public ProblemTableViewer(Table table) {
    super(table);
    initMapper();
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

    Object[] changed = event.getElements();
    if (changed != null && !fResourceToItemsMapper.isEmpty()) {
      ArrayList<Object> others = new ArrayList<Object>(changed.length);
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
    }
    super.handleLabelProviderChanged(event);
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
   */
  private boolean canIgnoreChangesFromAnnotionModel() {
    Object contentProvider = getContentProvider();
    return contentProvider instanceof IWorkingCopyProvider
        && !((IWorkingCopyProvider) contentProvider).providesWorkingCopies();
  }

  private void initMapper() {
    fResourceToItemsMapper = new ResourceToItemsMapper(this);
  }
}
