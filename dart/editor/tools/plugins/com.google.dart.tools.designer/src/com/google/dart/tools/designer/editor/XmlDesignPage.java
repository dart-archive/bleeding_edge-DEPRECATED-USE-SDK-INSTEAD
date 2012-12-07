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

import com.google.common.collect.Maps;
import com.google.dart.tools.designer.DartDesignerPlugin;
import com.google.dart.tools.designer.model.XmlObjectInfo;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wb.core.controls.PageBook;
import org.eclipse.wb.core.editor.DesignerState;
import org.eclipse.wb.core.editor.IDesignPageSite;
import org.eclipse.wb.core.model.broadcast.EditorActivatedListener;
import org.eclipse.wb.core.model.broadcast.EditorActivatedRequest;
import org.eclipse.wb.gef.core.ICommandExceptionHandler;
import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.EnvironmentUtils;
import org.eclipse.wb.internal.core.editor.DesignComposite;
import org.eclipse.wb.internal.core.editor.DesignPageSite;
import org.eclipse.wb.internal.core.editor.structure.PartListenerAdapter;
import org.eclipse.wb.internal.core.utils.Debug;
import org.eclipse.wb.internal.core.utils.exception.DesignerExceptionUtils;
import org.eclipse.wb.internal.core.utils.execution.ExecutionUtils;
import org.eclipse.wb.internal.core.utils.execution.RunnableEx;
import org.eclipse.wb.internal.core.utils.reflect.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * {@link XmlEditorPage} for XML.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public abstract class XmlDesignPage extends XmlEditorPage {
  protected IFile m_file;
  protected IDocument m_document;
  protected Composite m_composite;
  private PageBook m_pageBook;
  private XmlDesignComposite m_designComposite;
  private final Map<Class<?>, Composite> m_errorCompositesMap = Maps.newHashMap();
  private UndoManager m_undoManager;
  protected XmlObjectInfo m_rootObject;
  private DesignerState m_designerState = DesignerState.Undefined;
  private boolean m_forceDocumentListener;
  ////////////////////////////////////////////////////////////////////////////
  //
  // Life cycle
  //
  ////////////////////////////////////////////////////////////////////////////
  private final IPartListener m_partListener = new PartListenerAdapter() {
    @Override
    public void partActivated(IWorkbenchPart part) {
      if (part == m_editor) {
        ExecutionUtils.runAsync(new RunnableEx() {
          @Override
          public void run() throws Exception {
            // TODO(scheglov)
//            GlobalStateXml.activate(m_rootObject);
            if (m_active) {
              checkDependenciesOnDesignPageActivation();
            }
          }
        });
      }
    }
  };

  @Override
  public void initialize(AbstractXmlEditor editor) {
    super.initialize(editor);
    m_file = ((IFileEditorInput) editor.getEditorInput()).getFile();
    m_document = editor.getDocument();
    m_undoManager = new UndoManager(this, m_document);
    m_editor.getEditorSite().getPage().addPartListener(m_partListener);
  }

  @Override
  public void dispose() {
    super.dispose();
    m_undoManager.deactivate();
    m_editor.getEditorSite().getPage().removePartListener(m_partListener);
    disposeAll(true);
  }

  /**
   * Disposes design and model.
   */
  private void disposeAll(final boolean force) {
    // dispose design
    if (!m_composite.isDisposed()) {
      dispose_beforePresentation();
      m_designComposite.disposeDesign();
    }
    // dispose model
    if (m_rootObject != null) {
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          m_rootObject.refresh_dispose();
          m_rootObject.getBroadcastObject().dispose();
          disposeContext(force);
          // TODO(scheglov)
//          GlobalStateXml.deactivate(m_rootObject);
        }
      });
      m_rootObject = null;
    }
  }

  /**
   * Sends notification that presentation will be disposed.
   */
  private void dispose_beforePresentation() {
    if (m_rootObject != null) {
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          m_rootObject.getBroadcastObject().dispose_beforePresentation();
        }
      });
    }
  }

  /**
   * Disposes {@link EditorContext} of current hierarchy.
   * <p>
   * It is not guarantied that hierarchy exists, may be parsing was failed.
   * 
   * @param force is <code>true</code> if user closes editor or explicitly requests re-parsing.
   */
  protected void disposeContext(boolean force) {
    if (m_rootObject != null) {
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          // TODO(scheglov)
//          m_rootObject.getContext().dispose();
        }
      });
    }
  }

  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if (active) {
      m_undoManager.activate();
      m_designComposite.onActivate();
      checkDependenciesOnDesignPageActivation();
    } else {
      if (!m_forceDocumentListener) {
        m_undoManager.deactivate();
      }
      m_designComposite.onDeActivate();
    }
  }

  /**
   * This editor and its "Design" page are activated. Check if some external dependencies are
   * changed so that reparse or refresh should be performed.
   */
  private void checkDependenciesOnDesignPageActivation() {
    if (m_rootObject != null) {
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          EditorActivatedRequest request = new EditorActivatedRequest();
          m_rootObject.getBroadcast(EditorActivatedListener.class).invoke(request);
          if (request.isReparseRequested()) {
            refreshGEF();
          } else if (request.isRefreshRequested()) {
            m_rootObject.refresh();
          }
        }
      });
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the internal {@link DesignComposite}.
   */
  public final DesignComposite getDesignComposite() {
    return m_designComposite;
  }

  /**
   * @return the current {@link DesignerState} of editor.
   */
  public DesignerState getDesignerState() {
    return m_designerState;
  }

  /**
   * @return the {@link UndoManager} of this editor.
   */
  public UndoManager getUndoManager() {
    return m_undoManager;
  }

  /**
   * Ensure that page always listens for {@link IDocument} changes, even if it is not active. We
   * need this for "split mode", when updates on "Source" page should cause delayed UI refresh.
   */
  public void forceDocumentListener() {
    m_forceDocumentListener = true;
  }

  /**
   * Sets {@link IRefreshStrategy} to respond to {@link IDocument} changes.
   */
  public void setRefreshStrategy(IRefreshStrategy refreshStrategy) {
    m_undoManager.setRefreshStrategy(refreshStrategy);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Control
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public Control createControl(Composite parent) {
    m_composite = new Composite(parent, SWT.NONE);
    m_composite.setLayout(new FillLayout());
    // page book
    m_pageBook = new PageBook(m_composite, SWT.NONE);
    // design composite
    ICommandExceptionHandler exceptionHandler = new ICommandExceptionHandler() {
      @Override
      public void handleException(Throwable exception) {
        handleDesignException(exception);
      }
    };
    m_designComposite = createDesignComposite(m_pageBook, exceptionHandler);
    // show "design" initially
    m_pageBook.showPage(m_designComposite);
    return m_composite;
  }

  /**
   * @return the toolkit specific {@link XmlDesignComposite} instance.
   */
  protected XmlDesignComposite createDesignComposite(Composite parent,
      ICommandExceptionHandler exceptionHandler) {
    return new XmlDesignComposite(parent, SWT.NONE, m_editor, exceptionHandler);
  }

  @Override
  public Control getControl() {
    return m_composite;
  }

  /**
   * Creates and caches the composites for displaying some error/warning messages.
   */
  @SuppressWarnings("unchecked")
  private <T extends Composite> T getErrorComposite(Class<T> compositeClass) throws Exception {
    T composite = (T) m_errorCompositesMap.get(compositeClass);
    if (composite == null) {
      Constructor<T> constructor = compositeClass.getConstructor(Composite.class, int.class);
      composite = constructor.newInstance(m_pageBook, SWT.NONE);
      m_errorCompositesMap.put(compositeClass, composite);
    }
    return composite;
  }

  /**
   * Handles any exception happened on "Design" page, such as exceptions in GEF commands, property
   * table, components tree.
   */
  private void handleDesignException(Throwable e) {
    // at first, try to make post-mortem screenshot
    Image screenshot;
    try {
      screenshot = DesignerExceptionUtils.makeScreenshot();
    } catch (Throwable ex) {
      screenshot = null;
    }
    // dispose current state to prevent any further exceptions
    disposeAll(true);
    // show exception
    if (EnvironmentUtils.isTestingTime()) {
      e.printStackTrace();
    }
    showExceptionOnDesignPane(e, screenshot);
  }

  /**
   * Makes this page disabled (during refresh) and again enabled.
   */
  private void setEnabled(boolean enabled) {
    m_composite.setRedraw(enabled);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public String getName() {
    return "Design";
  }

  @Override
  public Image getImage() {
    return DartDesignerPlugin.getImage("editor_page_design.png");
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Render
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return <code>true</code> if parsing operation is slow, so should be performed with progress.
   */
  protected boolean shouldShowProgress() {
    return false;
  }

  /**
   * Performs toolkit specific parsing.
   */
  protected abstract XmlObjectInfo parse() throws Exception;

  /**
   * Disposes context and {@link #updateGEF()}.
   */
  public void refreshGEF() {
    disposeContext(true);
    updateGEF();
  }

  /**
   * Parses XML and displays it in GEF.
   */
  void updateGEF() {
    m_undoManager.refreshDesignerEditor();
  }

  /**
   * Parses {@link ICompilationUnit} and displays it in GEF.
   * 
   * @return <code>true</code> if parsing was successful.
   */
  boolean internal_refreshGEF() {
    // if "split mode", then try to parse, but expect that if may fail
    if (m_forceDocumentListener) {
      m_designComposite.setEnabled(false);
      try {
        parse();
      } catch (Throwable e) {
        return false;
      }
      m_designComposite.setEnabled(true);
    }
    // OK, do real parsing
    setEnabled(false);
    try {
      m_designerState = DesignerState.Parsing;
      disposeAll(false);
      // do parse
      if (shouldShowProgress()) {
        internal_refreshGEF_withProgress();
      } else {
        internal_refreshGEF(new NullProgressMonitor());
      }
      // success, show Design
      m_pageBook.showPage(m_designComposite);
      m_designerState = DesignerState.Successful;
      return true;
    } catch (Throwable e) {
      // show exception in editor
      showExceptionOnDesignPane(e, null);
      // failure
      return false;
    } finally {
      setEnabled(true);
    }
  }

  private void internal_refreshGEF_withProgress() throws Exception {
    final Display display = Display.getCurrent();
    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      @Override
      public void run(final IProgressMonitor monitor) {
        monitor.beginTask("Opening Design page.", 6);
        //
        try {
          DesignPageSite.setProgressMonitor(monitor);
          display.syncExec(new Runnable() {
            @Override
            public void run() {
              try {
                internal_refreshGEF(monitor);
              } catch (Throwable e) {
                ReflectionUtils.propagate(e);
              }
            }
          });
        } catch (Throwable e) {
          ReflectionUtils.propagate(e);
        } finally {
          DesignPageSite.setProgressMonitor(null);
        }
        // done progress monitor
        monitor.subTask(null);
        ExecutionUtils.waitEventLoop(100);
        monitor.done();
      }
    };
    try {
      new ProgressMonitorDialog(DesignerPlugin.getShell()).run(false, false, runnable);
    } catch (InvocationTargetException e) {
      ReflectionUtils.propagate(e.getCause());
    } catch (Throwable e) {
      ReflectionUtils.propagate(e);
    }
  }

  private void internal_refreshGEF(IProgressMonitor monitor) throws Exception {
    monitor.subTask("Initializing...");
    monitor.worked(1);
    // do parse
    {
      long start = System.currentTimeMillis();
      monitor.subTask("Parsing...");
      Debug.print("Parsing...");
      m_rootObject = parse();
      monitor.worked(1);
      Debug.println("done: " + (System.currentTimeMillis() - start));
    }
    // refresh model (create GUI)
    {
      long start = System.currentTimeMillis();
      monitor.subTask("Refreshing...");
      m_rootObject.refresh();
      monitor.worked(1);
      Debug.println("refresh: " + (System.currentTimeMillis() - start));
    }
    // site
    installDesignPageSite();
    // refresh design
    m_designComposite.refresh(m_rootObject, monitor);
    // configure helpers
    m_undoManager.setRoot(m_rootObject);
  }

  private void installDesignPageSite() {
    IDesignPageSite designPageSite = new DesignPageSite() {
      @Override
      public void showSourcePosition(int position) {
        m_editor.showSourcePosition(position);
      }

      @Override
      public void openSourcePosition(int position) {
        m_editor.showSourcePosition(position);
        m_editor.showSource();
      }

      @Override
      public void handleException(Throwable e) {
        handleDesignException(e);
      }

      @Override
      public void reparse() {
        refreshGEF();
      }
    };
    DesignPageSite.Helper.setSite(m_rootObject, designPageSite);
  }

  /**
   * Displays the error information on Design Pane.
   * 
   * @param e the {@link Throwable} to display.
   * @param screenshot the {@link Image} of entire shell just before error. Can be <code>null</code>
   *          in case of parse error when no screenshot needed.
   */
  private void showExceptionOnDesignPane(Throwable e, Image screenshot) {
    m_designerState = DesignerState.Error;
    // dispose context, because it may be already allocated some resources before parsing failed
    disposeContext(true);
    // show Throwable
    try {
      e = DesignerExceptionUtils.rewriteException(e);
      if (DesignerExceptionUtils.isWarning(e)) {
        XmlWarningComposite composite = getErrorComposite(XmlWarningComposite.class);
        composite.setException(e);
        m_pageBook.showPage(composite);
      } else {
        DesignerPlugin.log(e);
        XmlExceptionComposite composite = getErrorComposite(XmlExceptionComposite.class);
        composite.setException(e, screenshot, m_file, m_document);
        m_pageBook.showPage(composite);
      }
    } catch (Throwable ex) {
      // ignore, prevent error while showing the error
    }
  }
}
