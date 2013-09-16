/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.views.contentoutline;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.wst.sse.ui.internal.filter.OutlineCustomFiltersDialog;
import org.eclipse.wst.sse.ui.internal.filter.OutlineFilterDescriptor;
import org.eclipse.wst.sse.ui.internal.filter.OutlineNamePatternFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Action group to add the filter action to a view part's tool bar menu.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ContentOutlineFilterProcessor {

  private static final String SEPARATOR = ","; //$NON-NLS-1$
  private final String TAG_USER_DEFINED_PATTERNS_ENABLED = "userDefinedPatternsEnabled"; //$NON-NLS-1$
  private final String TAG_USER_DEFINED_PATTERNS = "userDefinedPatterns"; //$NON-NLS-1$

  private static class FilterItem {
    boolean enabled;
    boolean previouslyEnabled;
    OutlineFilterDescriptor descriptor;
    String id;

    private ViewerFilter filterInstance = null;

    public FilterItem(OutlineFilterDescriptor descriptor) {
      this.descriptor = descriptor;
      this.id = descriptor.getId();
      this.previouslyEnabled = false;
      this.enabled = descriptor.isEnabled();
    }

    public ViewerFilter getFilterInstance() {
      if (filterInstance == null) {
        filterInstance = descriptor.createViewerFilter();
      }
      return filterInstance;

    }
  }

  private final StructuredViewer fViewer;
  private final OutlineNamePatternFilter fPatternFilter;

  private boolean fUserDefinedPatternsEnabled;
  private String[] fUserDefinedPatterns;

  private String[] fPreviousPatterns;

  private final Map fFilterItems;

  private final String fTargetId;
  private IPreferenceStore fStore;

  /**
   * Creates a new <code>CustomFilterAction</code>.
   * 
   * @param store the preference Store
   * @param ownerId the id of this action group's owner
   * @param viewer the viewer to be filtered
   */
  public ContentOutlineFilterProcessor(IPreferenceStore store, String ownerId,
      StructuredViewer viewer) {
    Assert.isNotNull(ownerId);
    Assert.isNotNull(viewer);
    fStore = store;
    fTargetId = ownerId;
    fViewer = viewer;
    fPatternFilter = new OutlineNamePatternFilter();

    fUserDefinedPatterns = new String[0];
    fUserDefinedPatternsEnabled = false;
    fPreviousPatterns = new String[0];

    fFilterItems = new HashMap();
    OutlineFilterDescriptor[] filterDescriptors = OutlineFilterDescriptor.getFilterDescriptors(fTargetId);
    for (int i = 0; i < filterDescriptors.length; i++) {
      FilterItem item = new FilterItem(filterDescriptors[i]);
      fFilterItems.put(item.id, item);

    }

    initializeWithViewDefaults();

    updateViewerFilters();

  }

  /*
   * @see org.eclipse.jface.action.IContributionItem#isDynamic()
   */
  public boolean isDynamic() {
    return true;
  }

  /**
   * Returns a list of currently enabled filters. The filter is identified by its id.
   * <p>
   * This method is for internal use only and should not be called by clients outside of JDT/UI.
   * </p>
   * 
   * @return a list of currently enabled filters
   * @noreference This method is not intended to be referenced by clients.
   */
  public String[] internalGetEnabledFilterIds() {
    ArrayList enabledFilterIds = new ArrayList();
    for (Iterator iterator = fFilterItems.values().iterator(); iterator.hasNext();) {
      FilterItem item = (FilterItem) iterator.next();
      if (item.enabled) {
        enabledFilterIds.add(item.id);
      }
    }
    return (String[]) enabledFilterIds.toArray(new String[enabledFilterIds.size()]);
  }

  private void setEnabledFilterIds(String[] enabledIds) {
    // set all to false
    fUserDefinedPatternsEnabled = false;
    for (Iterator iterator = fFilterItems.values().iterator(); iterator.hasNext();) {
      FilterItem item = (FilterItem) iterator.next();
      item.enabled = false;
    }
    // set enabled to true
    for (int i = 0; i < enabledIds.length; i++) {
      FilterItem item = (FilterItem) fFilterItems.get(enabledIds[i]);
      if (item != null) {
        item.enabled = true;
      }
      if (fPatternFilter.getClass().getName().equals(enabledIds[i]))
        fUserDefinedPatternsEnabled = true;
    }
  }

  private void setUserDefinedPatterns(String[] patterns) {
    fUserDefinedPatterns = patterns;
  }

  private boolean areUserDefinedPatternsEnabled() {
    return fUserDefinedPatternsEnabled;
  }

  private void setUserDefinedPatternsEnabled(boolean state) {
    fUserDefinedPatternsEnabled = state;
  }

  // ---------- viewer filter handling ----------

  private boolean updateViewerFilters() {
    ViewerFilter[] installedFilters = fViewer.getFilters();
    ArrayList viewerFilters = new ArrayList(installedFilters.length);

    HashSet patterns = new HashSet();

    boolean hasChange = false;
    boolean patternChange = false;

    for (Iterator iterator = fFilterItems.values().iterator(); iterator.hasNext();) {
      FilterItem item = (FilterItem) iterator.next();
      if (item.descriptor.isCustomFilter()) {
        if (item.enabled != item.previouslyEnabled) {
          hasChange = true;
        }
        if (item.enabled) {
          ViewerFilter filter = item.getFilterInstance(); // only
          // create
          // when
          // changed
          if (filter != null) {
            viewerFilters.add(filter);
          }
        }
      } else if (item.descriptor.isPatternFilter()) {
        if (item.enabled) {
          patterns.add(item.descriptor.getPattern());
        }
        patternChange |= (item.enabled != item.previouslyEnabled);
      }
      item.previouslyEnabled = item.enabled;
    }

    if (areUserDefinedPatternsEnabled()) {
      for (int i = 0; i < fUserDefinedPatterns.length; i++) {
        patterns.add(fUserDefinedPatterns[i]);
      }
    }
    if (!patternChange) { // no pattern change so far, test if the user
      // patterns made a difference
      patternChange = hasChanges(patterns, fPreviousPatterns);
    }

    fPreviousPatterns = (String[]) patterns.toArray(new String[patterns.size()]);
    if (patternChange || hasChange) {
      fPatternFilter.setPatterns(fPreviousPatterns);
      if (patterns.isEmpty()) {
        viewerFilters.remove(fPatternFilter);
      } else if (!viewerFilters.contains(fPatternFilter)) {
        boolean contains = false;
        for (int i = 0; i < viewerFilters.size(); i++) {
          if (viewerFilters.get(i) instanceof OutlineNamePatternFilter) {
            OutlineNamePatternFilter filter = (OutlineNamePatternFilter) viewerFilters.get(i);
            String[] a1 = filter.getPatterns();
            String[] a2 = fPatternFilter.getPatterns();
            if (a1[0].equals(a2[0]))
              contains = true;
            else {
              viewerFilters.remove(i);
            }
            break;
          }
        }
        if (!contains)
          viewerFilters.add(fPatternFilter);
      }
      hasChange = true;
    }
    if (hasChange) {
      fViewer.setFilters((ViewerFilter[]) viewerFilters.toArray(new ViewerFilter[viewerFilters.size()])); // will
      // refresh
    }
    return hasChange;
  }

  private boolean hasChanges(HashSet patterns, String[] oldPatterns) {
    HashSet copy = (HashSet) patterns.clone();
    for (int i = 0; i < oldPatterns.length; i++) {
      boolean found = copy.remove(oldPatterns[i]);
      if (!found)
        return true;
    }
    return !copy.isEmpty();
  }

  // ---------- view kind/defaults persistency ----------

  private void initializeWithViewDefaults() {
    // get default values for view

    fUserDefinedPatternsEnabled = fStore.getBoolean(getPreferenceKey(TAG_USER_DEFINED_PATTERNS_ENABLED));
    setUserDefinedPatterns(OutlineCustomFiltersDialog.convertFromString(
        fStore.getString(getPreferenceKey(TAG_USER_DEFINED_PATTERNS)), SEPARATOR));

    for (Iterator iterator = fFilterItems.values().iterator(); iterator.hasNext();) {
      FilterItem item = (FilterItem) iterator.next();
      String id = item.id;
      // set default to value from plugin contributions (fixes
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=73991 ):
      fStore.setDefault(id, item.descriptor.isEnabled());
      item.enabled = fStore.getBoolean(id);
    }

  }

  private void storeViewDefaults() {
    // get default values for view

    fStore.setValue(getPreferenceKey(TAG_USER_DEFINED_PATTERNS_ENABLED),
        fUserDefinedPatternsEnabled);
    fStore.setValue(getPreferenceKey(TAG_USER_DEFINED_PATTERNS),
        OutlineCustomFiltersDialog.convertToString(fUserDefinedPatterns, SEPARATOR));

    boolean fFilterSelected = false;
    for (Iterator iterator = fFilterItems.values().iterator(); iterator.hasNext();) {
      FilterItem item = (FilterItem) iterator.next();
      fStore.setValue(item.id, item.enabled);
      if (item.enabled)
        fFilterSelected = true;
    }

    fStore.setValue(fTargetId, fUserDefinedPatternsEnabled || fFilterSelected);

  }

  private String getPreferenceKey(String tag) {
    return "CustomFiltersActionGroup." + fTargetId + '.' + tag; //$NON-NLS-1$
  }

  public void openDialog() {
    OutlineCustomFiltersDialog dialog = new OutlineCustomFiltersDialog(
        fViewer.getControl().getShell(), fTargetId, areUserDefinedPatternsEnabled(),
        fUserDefinedPatterns, internalGetEnabledFilterIds());

    if (dialog.open() == Window.OK) {

      setEnabledFilterIds(dialog.getEnabledFilterIds());
      setUserDefinedPatternsEnabled(dialog.areUserDefinedPatternsEnabled());
      setUserDefinedPatterns(dialog.getUserDefinedPatterns());
      storeViewDefaults();

      updateViewerFilters();
    } else {
      storeViewDefaults();
    }
  }
}
