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
package com.google.dart.tools.designer.editor;

import com.google.common.collect.Lists;
import com.google.dart.tools.designer.model.EditorContextCommitListener;
import com.google.dart.tools.designer.model.XmlObjectInfo;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.core.model.broadcast.ObjectEventListener;
import org.eclipse.wb.internal.core.editor.DesignPageSite;
import org.eclipse.wb.internal.core.editor.ObjectPathHelper;
import org.eclipse.wb.internal.core.editor.structure.components.IComponentsTree;

import java.util.List;
import java.util.Map;

/**
 * Manager for handling undo/redo modifications in XML {@link IDocument}.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public final class UndoManager {
  private final XmlDesignPage m_designPage;
  private final IDocument m_document;
  private String m_currentSource;
  private String m_currentDump;
  private IComponentsTree m_componentsTree;
  private ISelectionProvider m_selectionProvider;
  private ITreeContentProvider m_componentsProvider;
  private ObjectPathHelper m_objectPathHelper;
  @SuppressWarnings("unchecked")
  private final Map<String, int[][]> m_dumpToSelection = new LRUMap(32);
  @SuppressWarnings("unchecked")
  private final Map<String, int[][]> m_dumpToExpanded = new LRUMap(32);

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public UndoManager(XmlDesignPage designPage, IDocument document) {
    m_designPage = designPage;
    m_document = document;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  private boolean m_active;
  private XmlObjectInfo m_rootObject;

  /**
   * Activates {@link UndoManager}, so it starts listening for document changes.
   */
  public void activate() {
    if (!m_active) {
      m_active = true;
      if (!StringUtils.equals(m_currentSource, m_document.get())) {
        refreshDesignerEditor();
      }
      addDocumentListener();
    }
  }

  /**
   * Deactivates {@link UndoManager}, so it stops listening for document changes.
   */
  public void deactivate() {
    m_active = false;
    removeDocumentListener();
  }

  /**
   * Sets the new root {@link XmlObjectInfo} in editor.
   */
  public void setRoot(XmlObjectInfo rootObject) {
    m_rootObject = rootObject;
    rootObject.addBroadcastListener(m_commitListener);
    rootObject.addBroadcastListener(m_refreshListener);
    // get components tree
    {
      DesignPageSite site = DesignPageSite.Helper.getSite(rootObject);
      m_componentsTree = site.getComponentTree();
      m_componentsProvider = m_componentsTree.getContentProvider();
      m_selectionProvider = m_componentsTree.getSelectionProvider();
      m_objectPathHelper = new ObjectPathHelper(m_componentsProvider);
      // listen for tree expansion state
      m_componentsTree.setExpandListener(new Runnable() {
        @Override
        public void run() {
          Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
              rememberState();
            }
          });
        }
      });
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IDocument listener
  //
  ////////////////////////////////////////////////////////////////////////////
  private int m_bufferChangeCount = 0;
  private IRefreshStrategy m_refreshStrategy = IRefreshStrategy.IMMEDIATELY;
  private final IDocumentListener m_documentListener = new IDocumentListener() {
    @Override
    public void documentChanged(DocumentEvent event) {
      if (event.getText() != null || event.getLength() != 0) {
        scheduleRefresh_onBufferChange();
      }
    }

    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    private void scheduleRefresh_onBufferChange() {
      final int bufferChangeCount = ++m_bufferChangeCount;
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (bufferChangeCount == m_bufferChangeCount) {
            refreshDesignerEditor();
          }
        }
      };
      if (m_refreshStrategy.shouldImmediately()) {
        runnable.run();
      } else if (m_refreshStrategy.shouldWithDelay()) {
        int delay = m_refreshStrategy.getDelay();
        Display.getDefault().timerExec(delay, runnable);
      }
    }
  };

  void setRefreshStrategy(IRefreshStrategy refreshStrategy) {
    m_refreshStrategy = refreshStrategy;
  }

  /**
   * Adds {@link IDocumentListener}.
   */
  private void addDocumentListener() {
    m_document.addDocumentListener(m_documentListener);
  }

  /**
   * Removes {@link IDocumentListener}.
   */
  private void removeDocumentListener() {
    m_document.removeDocumentListener(m_documentListener);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Refresh listener
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Remove IDocument listeners during commit.
   */
  private final EditorContextCommitListener m_commitListener = new EditorContextCommitListener() {
    @Override
    public void aboutToCommit() {
      removeDocumentListener();
    }

    @Override
    public void doneCommit() {
      addDocumentListener();
    }
  };
  /**
   * Remember new state after refresh.
   */
  private final ObjectEventListener m_refreshListener = new ObjectEventListener() {
    @Override
    public void refreshBeforeCreate() throws Exception {
      removeSelectionListener();
    }

    @Override
    public void refreshed2() throws Exception {
      addSelectionListener();
      rememberSource();
      rememberDump();
      rememberState();
    }
  };
  ////////////////////////////////////////////////////////////////////////////
  //
  // Selection listener
  //
  ////////////////////////////////////////////////////////////////////////////
  private final ISelectionChangedListener m_selectionListener = new ISelectionChangedListener() {
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      rememberState();
    }
  };

  /**
   * Adds {@link ISelectionChangedListener}.
   */
  private void addSelectionListener() {
    m_selectionProvider.addSelectionChangedListener(m_selectionListener);
  }

  /**
   * Removes {@link ISelectionChangedListener}
   */
  private void removeSelectionListener() {
    if (m_selectionProvider != null) {
      m_selectionProvider.removeSelectionChangedListener(m_selectionListener);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Refresh
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Informs {@link DesignPage} that buffer was changed and reparse required.
   */
  void refreshDesignerEditor() {
    rememberSource();
    // refresh viewer
    removeSelectionListener();
    if (m_designPage.internal_refreshGEF()) {
      addSelectionListener();
      // restore state
      rememberDump();
      restoreState();
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // State
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Remembers current source.
   */
  private void rememberSource() {
    m_currentSource = m_document.get();
  }

  /**
   * Remembers current dump.
   */
  private void rememberDump() {
    m_currentDump = ObjectPathHelper.getObjectsDump(m_rootObject, 0);
  }

  /**
   * Remembers selected/expanded elements for current source.
   */
  private void rememberState() {
    // selection
    {
      // prepare selected objects
      Object[] selectedObjects;
      {
        IStructuredSelection structuredSelection = (IStructuredSelection) m_selectionProvider.getSelection();
        selectedObjects = structuredSelection.toArray();
      }
      // remember
      int[][] paths = m_objectPathHelper.getObjectsPaths(selectedObjects);
      //m_sourceToSelection.put(m_currentSource, paths);
      m_dumpToSelection.put(m_currentDump, paths);
    }
    // expanded
    {
      Object[] expandedObjects = m_componentsTree.getExpandedElements();
      int[][] paths = m_objectPathHelper.getObjectsPaths(expandedObjects);
      m_dumpToExpanded.put(m_currentDump, paths);
    }
  }

  /**
   * Tries to restore selected/expanded elements for current source.
   */
  private void restoreState() {
    restoreSelection();
    restoreExpanded();
  }

  /**
   * Tries to restore selected elements for current source.
   */
  private void restoreSelection() {
    // prepare selection
    int[][] paths = m_dumpToSelection.get(m_currentDump);
    // do restore
    if (paths != null) {
      removeSelectionListener();
      try {
        Object[] objects = m_objectPathHelper.getObjectsForPaths(paths);
        m_selectionProvider.setSelection(new StructuredSelection(objects));
      } finally {
        addSelectionListener();
      }
    }
  }

  /**
   * Tries to restore expanded elements for current dump.
   */
  private void restoreExpanded() {
    int[][] paths = m_dumpToExpanded.get(m_currentDump);
    // do restore
    if (paths != null) {
      Object[] objects = m_objectPathHelper.getObjectsForPaths(paths);
      m_componentsTree.setExpandedElements(objects);
    }
    // if no expanded element, perform default expanding
    if (m_componentsTree.getExpandedElements().length == 0) {
      List<Object> expandedElements = Lists.newArrayList();
      Object element = m_rootObject;
      while (true) {
        expandedElements.add(element);
        Object[] children = m_componentsProvider.getChildren(element);
        if (children.length != 1) {
          break;
        }
        element = children[0];
      }
      m_componentsTree.setExpandedElements(expandedElements.toArray());
    }
  }
}
