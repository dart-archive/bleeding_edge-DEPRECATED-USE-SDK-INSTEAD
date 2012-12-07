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

import com.google.dart.tools.designer.editor.actions.DesignPageActions;
import com.google.dart.tools.designer.editor.palette.DesignerPalette;
import com.google.dart.tools.designer.model.XmlObjectInfo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wb.core.controls.flyout.FlyoutControlComposite;
import org.eclipse.wb.core.model.ObjectInfo;
import org.eclipse.wb.gef.core.ICommandExceptionHandler;
import org.eclipse.wb.gef.core.IEditPartViewer;
import org.eclipse.wb.internal.core.editor.DesignComposite;
import org.eclipse.wb.internal.core.editor.actions.SelectSupport;
import org.eclipse.wb.internal.core.utils.Debug;

/**
 * {@link DesignComposite} for XML.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public class XmlDesignComposite extends DesignComposite {
  private DesignPageActions m_pageActions;
  private XmlDesignToolbarHelper m_toolbarHelper;
  private DesignerPalette m_designerPalette;
  private XmlObjectInfo m_rootObject;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public XmlDesignComposite(Composite parent, int style, IEditorPart editorPart,
      ICommandExceptionHandler exceptionHandler) {
    super(parent, style, editorPart, exceptionHandler);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Creation of UI
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void createDesignActions() {
    IEditPartViewer treeViewer = m_componentsComposite.getTreeViewer();
    m_pageActions = new DesignPageActions(m_editorPart, treeViewer);
    m_viewer.setContextMenu(new DesignContextMenuProvider(m_viewer, m_pageActions));
    // install dispose listener
    addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        m_pageActions.dispose();
      }
    });
  }

  @Override
  protected void createDesignToolbarHelper() {
    m_toolbarHelper = new XmlDesignToolbarHelper(m_toolBar);
    m_toolbarHelper.initialize(m_pageActions, m_viewer);
    m_toolbarHelper.fill();
  }

  @Override
  protected void createPalette(FlyoutControlComposite gefComposite) {
    m_designerPalette = new DesignerPalette(gefComposite.getFlyoutParent(), SWT.NONE, true);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Life cycle
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Notifies that "Design" page was activated.
   */
  public void onActivate() {
    m_pageActions.installActions();
  }

  /**
   * Notifies that "Design" page was deactivated.
   */
  public void onDeActivate() {
    m_pageActions.uninstallActions();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Design access
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void refresh(ObjectInfo rootObject, IProgressMonitor monitor) {
    m_rootObject = (XmlObjectInfo) rootObject;
    // refresh viewer's
    {
      monitor.subTask("Updating GEF viewer...");
      monitor.worked(1);
      m_viewer.setInput(m_rootObject);
      m_viewer.getControl().setDrawCached(false);
    }
    {
      monitor.subTask("Updating Property composite...");
      monitor.worked(1);
      m_componentsComposite.setInput(m_viewer, m_rootObject);
    }
    {
      long start = System.currentTimeMillis();
      monitor.subTask("Loading palette...");
      monitor.worked(1);
      // TODO(scheglov)
//      {
//        String toolkitId = m_rootObject.getDescription().getToolkit().getId();
//        m_designerPalette.setInput(m_viewer, m_rootObject, toolkitId);
//      }
      Debug.println("palette: " + (System.currentTimeMillis() - start));
    }
    // configure helpers
    m_pageActions.setRoot(m_rootObject);
    m_toolbarHelper.setRoot(m_rootObject);
    m_viewersComposite.setRoot(m_rootObject);
    new SelectSupport(rootObject, m_viewer, m_componentsComposite.getTreeViewer());
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Structure/Palette reparenting access
  //
  ////////////////////////////////////////////////////////////////////////////
  private IExtractableControl m_extractablePalette;

  @Override
  public IExtractableControl getExtractablePalette() {
    if (m_extractablePalette == null) {
      m_extractablePalette = new ExtractableControl(m_designerPalette.getControl(), this);
    }
    return m_extractablePalette;
  }
}
