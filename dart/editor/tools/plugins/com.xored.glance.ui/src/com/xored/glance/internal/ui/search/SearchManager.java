/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.internal.ui.panels.SearchPanelManager;
import com.xored.glance.internal.ui.panels.SearchStatusLine;
import com.xored.glance.internal.ui.preferences.IPreferenceConstants;
import com.xored.glance.internal.ui.sources.ISourceProviderListener;
import com.xored.glance.internal.ui.sources.TextSourceMaker;
import com.xored.glance.internal.ui.sources.TextSourceManager;
import com.xored.glance.ui.panels.ISearchPanel;
import com.xored.glance.ui.panels.ISearchPanelListener;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.sources.SourceSelection;
import com.xored.glance.ui.utils.UITextSource;

/**
 * @author Yuri Strot
 */
public class SearchManager {

  /** Searching from the current position */
  public static final int FIND_HERE = 0;
  /** Searching next occurrence of the string */
  public static final int FIND_NEXT = 1;
  /** Searching previous occurrence of the string */
  public static final int FIND_PREVIOUS = 2;

  public static SearchManager getIntance() {
    if (manager == null) {
      manager = new SearchManager();
    }
    return manager;
  }

  public boolean activate() {
    final TextSourceMaker source = TextSourceManager.getInstance().getSource();
    if (update(source, true)) {
      forceFocus();
      return true;
    }
    return false;
  }

  public void startup() {
    PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {
      public void windowOpened(final IWorkbenchWindow window) {
        setStatusLine(window, true);
      }

      public void windowDeactivated(final IWorkbenchWindow window) {
      }

      public void windowClosed(final IWorkbenchWindow window) {
      }

      public void windowActivated(final IWorkbenchWindow window) {
      }
    });
    for (final IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
      setStatusLine(window, true);
    }
  }

  public void setStatusLine(final IWorkbenchWindow window, final boolean open) {
    final ISearchPanel panel = SearchStatusLine.getSearchLine(window);
    if (!open) {
      panel.closePanel();
      return;
    }
    panels.add(panel);
    final SearchPanelListener listener = new SearchPanelListener(panel);
    panel.addPanelListener(listener);
    panelToListener.put(panel, listener);
    updateSourceListener();

    final TextSourceMaker source = TextSourceManager.getInstance().getSource();
    if (source != null && source.getControl() != null && panel.isApplicable(source.getControl())) {
      this.panel = panel;
      rule = panel.getRule();
      if (setDescription(source))
        updateEnabling();
    }
  }

  public boolean isInWindow(final IWorkbenchWindow window) {
    for (final ISearchPanel panel : panels) {
      if (panel instanceof SearchStatusLine) {
        final SearchStatusLine sl = ((SearchStatusLine) panel);
        if (sl.getWindow() == window) {
          return true;
        }
      }
    }
    return false;
  }

  public void findNext() {
    if (panel != null) {
      panel.findNext();
    }
  }

  public void findPrevious() {
    if (panel != null) {
      panel.findPrevious();
    }
  }

  public void clearHistory() {
    if (panel != null) {
      panel.clearHistory();
    }
  }

  public void sourceFocus() {
    ITextSource source = getSource();
    if (source instanceof UITextSource) {
      UITextSource uiTextSource = (UITextSource) source;
      if (uiTextSource.getControl() != null) {
        uiTextSource.getControl().setFocus();
      }
    }
  }

  public void close() {
    if (panel != null) {
      panel.closePanel();
    }
  }

  public ITextSource getSource() {
    return source;
  }

  private boolean update(final TextSourceMaker source, final boolean openNewPanel) {
    updatePanel(source, openNewPanel);
    if (panel != null) {
      rule = panel.getRule();
      updateSourceListener();
      if (setDescription(source))
        updateEnabling();
      return true;
    }
    return false;
  }

  private void updatePanel(final TextSourceMaker source, final boolean openNewPanel) {
    final Control control = source.getControl();
    if (panel != null) {
      if (panel.isApplicable(control))
        return;
      if (this.source != null) {
        this.source.dispose();
        this.source = null;
      }
      creator = null;
      panel = null;
    }
    for (final ISearchPanel panel : panels) {
      if (panel.isApplicable(control)) {
        this.panel = panel;
        break;
      }
    }
    if (panel == null && openNewPanel) {
      panel = SearchPanelManager.getInstance().getPanel(control);
      if (panel != null) {
        panels.add(panel);
        final SearchPanelListener listener = new SearchPanelListener(panel);
        panel.addPanelListener(listener);
        panelToListener.put(panel, listener);
      }
    }
  }

  private void forceFocus() {
    String text = "";
    if (source != null) {
      final SourceSelection selection = source.getSelection();
      if (selection != null) {
        text = selection.getBlock().getText();
        final int offset = selection.getOffset();
        final int length = selection.getLength();
        if (offset + length <= text.length())
          text = text.substring(offset, offset + length);
        else
          text = "";
      }
    }
    panel.setFocus(text);
  }

  private void find(final SearchRule rule, final int type) {
    this.type = type;
    if (rule != null) {
      final boolean textEquals = rule.isTextEquals(this.rule);
      final boolean settingsEqual = rule.isSettingsEqual(this.rule);
      if (textEquals) {
        if (settingsEqual) {
          updateSelection();
        } else {
          updateSearch(rule, false);
        }
      } else {
        updateSearch(rule, true);
      }
    } else {
      updateSearch(rule, false);
    }
    this.rule = rule;
  }

  protected void dispose(final ISearchPanel panel) {
    panels.remove(panel);
    final SearchPanelListener listener = panelToListener.remove(panel);
    panel.removePanelListener(listener);
    if (panel == this.panel) {
      this.panel = null;
      if (source != null) {
        source.dispose();
        source = null;
      }
      if (creator != null) {
        final Control control = creator.getControl();
        if (control != null && !control.isDisposed())
          control.forceFocus();
        creator = null;
      }
      rule = null;
    }
    if (panels.size() == 0) {
      if (engine != null) {
        engine.exit();
        engine = null;
      }
      if (sourceListener != null) {
        TextSourceManager.getInstance().removeSourceProviderListener(sourceListener);
        sourceListener = null;
      }
    } else {
      updateSourceListener();
    }
  }

  protected void updateSearch(final SearchRule rule, final boolean paused) {
    getSearchEngine().setRule(rule);
  }

  protected void updateSelection() {
    if (type == FIND_NEXT) {
      getSearchEngine().selectNext();
    } else if (type == FIND_PREVIOUS) {
      getSearchEngine().selectPrev();
    }
  }

  protected boolean setDescription(final TextSourceMaker descriptor) {
    // ignore panel controls
    if (descriptor != null && panel != null && panel.getControl() != null) {
      if (isParent(panel.getControl(), descriptor.getControl()))
        return false;
    }
    // ignore the same source
    if (descriptor == null) {
      if (this.creator == null)
        return false;
    } else if (descriptor.equals(this.creator)) {
      return false;
    }
    if (source != null) {
      source.dispose();
      source = null;
      this.creator = null;
    }
    if (descriptor != null && descriptor.isValid()) {
      this.creator = descriptor;
      source = new UITextSource(descriptor.create(), descriptor.getControl());
      getSearchEngine().setSource(rule, source, true);
      source.init();
      updateIndexingState();
    }
    return true;
  }

  private void updateIndexingState() {
    if (monitor != null) {
      monitor.setCanceled(true);
      monitor = null;
    }

    final boolean indexRequired = source != null && !source.isDisposed()
        && source.isIndexRequired();
    if (GlancePlugin.getDefault().getPreferenceStore().getBoolean(
        IPreferenceConstants.PANEL_AUTO_INDEXING)
        && indexRequired) {
      index();
    } else {
      panel.setIndexingState(indexRequired ? ISearchPanel.INDEXING_STATE_INITIAL
          : ISearchPanel.INDEXING_STATE_DISABLE);
    }
  }

  public void index() {
    if (panel != null && source != null && !source.isDisposed()) {
      monitor = new SearchProgressMonitor(panel);
      new Thread() {
        @Override
        public void run() {
          panel.setIndexingState(ISearchPanel.INDEXING_STATE_IN_PROGRESS);
          if (source != null) {
            source.index(monitor);
          }
        }
      }.start();
    }
  }

  private IProgressMonitor monitor;

  protected void updateEnabling() {
    if (panel != null) {
      panel.setEnabled(source != null);
      if (source == null) {
        panel.setIndexingState(ISearchPanel.INDEXING_STATE_FINISHED);
      }
    }
  }

  protected boolean isParent(final Control parent, Control child) {
    while (child != null) {
      if (child.equals(parent))
        return true;
      child = child.getParent();
    }
    return false;
  }

  private SearchEngine getSearchEngine() {
    if (engine == null) {
      engine = new SearchEngine(searchListener);
      engine.start();
    }
    return engine;
  }

  private void updateSourceListener() {
    if (sourceListener == null) {
      sourceListener = new SourceListener();
      TextSourceManager.getInstance().addSourceProviderListener(sourceListener);
    }
  }

  private class SearchListener implements ISearchListener {

    public void allFound(final Match[] matches) {
      if (panel != null)
        panel.allFound(matches);
    }

    public void finished() {
      if (panel != null)
        panel.finished();
    }

    public void firstFound(final Match match) {
      if (panel != null)
        panel.firstFound(match);
    }
  }

  private class SourceListener implements ISourceProviderListener {
    public void sourceChanged(final TextSourceMaker source) {
      update(source, false);
    }
  }

  private class SearchPanelListener implements ISearchPanelListener {

    public SearchPanelListener(final ISearchPanel panel) {
      this.panel = panel;
    }

    protected boolean isCurrent() {
      return panel.equals(SearchManager.this.panel);
    }

    public void ruleChanged(final SearchRule rule) {
      if (isCurrent())
        find(rule, SearchManager.FIND_HERE);
    }

    public void findNext() {
      if (isCurrent())
        find(rule, SearchManager.FIND_NEXT);
    }

    public void findPrevious() {
      if (isCurrent())
        find(rule, SearchManager.FIND_PREVIOUS);
    }

    public void close() {
      dispose(panel);
    }

    public void indexCanceled() {
      if (monitor != null) {
        monitor.setCanceled(true);
        monitor = null;
      }
    }

    private final ISearchPanel panel;

  }

  private static SearchManager manager;

  private SearchManager() {
    panels = new ArrayList<ISearchPanel>();
    searchListener = new SearchListener();
    panelToListener = new HashMap<ISearchPanel, SearchPanelListener>();
  }

  private final SearchListener searchListener;
  private SourceListener sourceListener;
  private final Map<ISearchPanel, SearchPanelListener> panelToListener;
  private SearchEngine engine;
  private final List<ISearchPanel> panels;
  private ISearchPanel panel;
  private ITextSource source;
  private TextSourceMaker creator;

  private int type;
  private SearchRule rule;

}
