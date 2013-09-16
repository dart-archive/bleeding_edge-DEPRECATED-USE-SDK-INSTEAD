/*****************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ****************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.ConfigureColumns;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.INavigationLocationProvider;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.part.MultiPageSelectionProvider;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.xml.core.internal.provisional.IXMLPreferenceNames;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeLabelProvider;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLMultiPageEditorPart extends MultiPageEditorPart implements
    INavigationLocationProvider, IPersistableEditor {

  /**
   * Internal part activation listener, copied from AbstractTextEditor
   */
  class ActivationListener implements IPartListener, IWindowListener {

    /**
     * The maximum number of children the root nodes can have for the design page to auto expand the
     * root nodes
     */
    private static final int MAX_NUM_CHILD_NODES_FOR_AUTO_EXPAND = 500;

    /** Cache of the active workbench part. */
    private IWorkbenchPart fActivePart;
    /** Indicates whether activation handling is currently be done. */
    private boolean fIsHandlingActivation = false;
    /**
     * The part service.
     * 
     * @since 3.1
     */
    private IPartService fPartService;

    /**
     * Creates this activation listener.
     * 
     * @param partService the part service on which to add the part listener
     * @since 3.1
     */
    public ActivationListener(IPartService partService) {
      fPartService = partService;
      fPartService.addPartListener(this);
      PlatformUI.getWorkbench().addWindowListener(this);
    }

    /**
     * Disposes this activation listener.
     * 
     * @since 3.1
     */
    public void dispose() {
      fPartService.removePartListener(this);
      PlatformUI.getWorkbench().removeWindowListener(this);
      fPartService = null;
    }

    /*
     * @see IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
     */
    public void partActivated(IWorkbenchPart part) {
      fActivePart = part;
      handleActivation();
    }

    /*
     * @see IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
     */
    public void partBroughtToTop(IWorkbenchPart part) {
      // do nothing
    }

    /*
     * @see IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
     */
    public void partClosed(IWorkbenchPart part) {
      // do nothing
    }

    /*
     * @see IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
     */
    public void partDeactivated(IWorkbenchPart part) {
      fActivePart = null;
    }

    /*
     * @see IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
     */
    public void partOpened(IWorkbenchPart part) {
      if (fDesignViewer instanceof AbstractTreeViewer) {
        IDocument document = getDocument();
        if (document != null) {
          IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
              document);
          try {
            if (model instanceof IDOMModel) {
              IDOMDocument modelDocument = ((IDOMModel) model).getDocument();
              NodeList rootChildren = modelDocument.getChildNodes();

              boolean tooManyChildren = (rootChildren.getLength() > MAX_NUM_CHILD_NODES_FOR_AUTO_EXPAND);
              /*
               * For each root (there should really only be one real root but there are also could
               * be empty text regions and doc type at the root level) determine if it has to many
               * children or not to auto expand
               */
              for (int i = 0; i < rootChildren.getLength() && !tooManyChildren; ++i) {
                tooManyChildren = (rootChildren.item(i).getChildNodes().getLength() > MAX_NUM_CHILD_NODES_FOR_AUTO_EXPAND);
              }

              /*
               * if root node does not have to many children then auto expand the root node
               */
              if (!tooManyChildren) {
                ((AbstractTreeViewer) fDesignViewer).expandToLevel(2);
              }
            }
          } finally {
            if (model != null) {
              model.releaseFromRead();
            }
          }
        }
      }
    }

    /**
     * Handles the activation triggering a element state check in the editor.
     */
    void handleActivation() {
      if (fIsHandlingActivation || (getTextEditor() == null)) {
        return;
      }

      if (fActivePart == XMLMultiPageEditorPart.this) {
        fIsHandlingActivation = true;
        try {
          getTextEditor().safelySanityCheckState(getEditorInput());
        } finally {
          fIsHandlingActivation = false;
        }
      }
    }

    /*
     * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
     * 
     * @since 3.1
     */
    public void windowActivated(IWorkbenchWindow window) {
      if (window == getEditorSite().getWorkbenchWindow()) {
        /*
         * Workaround for problem described in http://dev.eclipse.org/bugs/show_bug.cgi?id=11731
         * Will be removed when SWT has solved the problem.
         */
        window.getShell().getDisplay().asyncExec(new Runnable() {
          public void run() {
            handleActivation();
          }
        });
      }
    }

    /*
     * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
     * 
     * @since 3.1
     */
    public void windowDeactivated(IWorkbenchWindow window) {
      // do nothing
    }

    /*
     * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
     * 
     * @since 3.1
     */
    public void windowClosed(IWorkbenchWindow window) {
      // do nothing
    }

    /*
     * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
     * 
     * @since 3.1
     */
    public void windowOpened(IWorkbenchWindow window) {
      // do nothing
    }
  }

  /**
   * Listens for selection from the source page, applying it to the design viewer.
   */
  private class TextEditorPostSelectionAdapter extends UIJob implements ISelectionChangedListener {
    boolean forcePostSelection = false;
    ISelection selection = null;

    public TextEditorPostSelectionAdapter() {
      super(getTitle());
      setUser(true);
    }

    public IStatus runInUIThread(IProgressMonitor monitor) {
      if (selection != null) {
        fDesignViewer.getSelectionProvider().setSelection(selection);
      }
      return Status.OK_STATUS;
    }

    public void selectionChanged(SelectionChangedEvent event) {
      if ((fDesignViewer != null)
          && ((getActivePage() != fDesignPageIndex) || !XMLMultiPageEditorPart.this.equals(getSite().getPage().getActivePart()))) {
        if (forcePostSelection) {
          selection = event.getSelection();
          schedule(200);
        } else {
          fDesignViewer.getSelectionProvider().setSelection(event.getSelection());
        }
      }
    }
  }

  private class PageInitializationData {
    IConfigurationElement fElement;
    String fPropertyName;
    Object fData;

    PageInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
      super();
      fElement = cfig;
      fPropertyName = propertyName;
      fData = data;
    }

    void sendInitializationData(IExecutableExtension executableExtension) {
      try {
        executableExtension.setInitializationData(fElement, fPropertyName, fData);
      } catch (CoreException e) {
        Logger.logException(e);
      }
    }
  }

  /**
   * Internal IPropertyListener on the source page
   */
  class PropertyListener implements IPropertyListener {
    public void propertyChanged(Object source, int propId) {
      switch (propId) {
      // had to implement input changed "listener" so that
      // StructuredTextEditor could tell it containing editor that
      // the input has change, when a 'resource moved' event is
      // found.
        case IEditorPart.PROP_INPUT: {
          if (source == getTextEditor() && fDesignViewer instanceof XMLTableTreeViewer) {
            IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
                getDocument());
            try {
              if (model instanceof IDOMModel) {
                IDOMDocument modelDocument = ((IDOMModel) model).getDocument();
                Object designInput = ((XMLTableTreeViewer) fDesignViewer).getInput();
                if (modelDocument != designInput)
                  setInput(getTextEditor().getEditorInput());
              }
            } finally {
              if (model != null)
                model.releaseFromRead();
            }
          }
        }
        case IEditorPart.PROP_DIRTY: {
          if (source == getTextEditor()) {
            if (getTextEditor().getEditorInput() != getEditorInput()) {
              setInput(getTextEditor().getEditorInput());
              /*
               * title should always change when input changes. create runnable for following post
               * call
               */
              Runnable runnable = new Runnable() {
                public void run() {
                  _firePropertyChange(IWorkbenchPart.PROP_TITLE);
                }
              };
              /*
               * Update is just to post things on the display queue (thread). We have to do this to
               * get the dirty property to get updated after other things on the queue are executed.
               */
              ((Control) getTextEditor().getAdapter(Control.class)).getDisplay().asyncExec(runnable);
            }
          }
          break;
        }
        case IWorkbenchPart.PROP_TITLE: {
          // update the input if the title is changed
          if (source == getTextEditor()) {
            if (getTextEditor().getEditorInput() != getEditorInput()) {
              setInput(getTextEditor().getEditorInput());
            }
          }
          break;
        }
        default: {
          // propagate changes. Is this needed? Answer: Yes.
          if (source == getTextEditor()) {
            _firePropertyChange(propId);
          }
          break;
        }
      }

    }
  }

  class TextInputListener implements ITextInputListener {
    public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
      // do nothing
    }

    public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
      if ((fDesignViewer != null) && (newInput != null)) {
        fDesignViewer.setDocument(newInput);
      }
    }
  }

  class StatusLineLabelProvider extends JFaceNodeLabelProvider {
    public StatusLineLabelProvider() {
    }

    public String getText(Object element) {
      if (element == null)
        return null;

      Node node = (Node) element;
      if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
        return getText(((Attr) node).getOwnerElement());
      }

      StringBuffer s = new StringBuffer();
      if (node.getNodeType() != Node.DOCUMENT_NODE) {
        while (node != null && node instanceof INodeNotifier) {
          INodeNotifier notifier = (INodeNotifier) node;
          if (node.getNodeType() != Node.DOCUMENT_NODE) {
            IJFaceNodeAdapter adapter = (IJFaceNodeAdapter) notifier.getAdapterFor(IJFaceNodeAdapter.class);
            if (adapter != null) {
              s.insert(0, adapter.getLabelText(node));
            }
          }
          node = node.getParentNode();
          if (node != null && node.getNodeType() != Node.DOCUMENT_NODE)
            s.insert(0, IPath.SEPARATOR);
        }
      }
      return s.toString();
    }

    public Image getImage(Object element) {
      if (element == null)
        return null;

      Node node = (Node) element;
      if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
        return getImage(((Attr) node).getOwnerElement());
      }
      return super.getImage(element);
    }
  }

  /** The design page index. */
  private int fDesignPageIndex;

  /** The design viewer */
  IDesignViewer fDesignViewer;
  /** Any container for the design viewer */
  private Composite fDesignContainer;

  private ActivationListener fActivationListener;

  IPropertyListener fPropertyListener = null;

  /** The source page index. */
  int fSourcePageIndex;

  /** The text editor. */
  private StructuredTextEditor fTextEditor;

  private TextEditorPostSelectionAdapter fTextEditorSelectionListener;

  private ILabelProvider fStatusLineLabelProvider;

  private PageInitializationData fPageInitializer;

  private ToolBarManager fToolbarManager;
  private ToolBarManager fEditorManager;

  /** Context menu manager */
  private MenuManager fMenuManager;

  private boolean fAllocateToolbar = true;

  private TextInputListener fTextInputListener;

  /**
   * StructuredTextMultiPageEditorPart constructor comment.
   */
  public XMLMultiPageEditorPart() {
    super();
    fStatusLineLabelProvider = new StatusLineLabelProvider();
  }

  /*
   * This method is just to make firePropertyChanged accessible from some (anonomous) inner classes.
   */
  void _firePropertyChange(int property) {
    super.firePropertyChange(property);
  }

  /**
   * Adds the source page of the multi-page editor.
   */
  private void addSourcePage() throws PartInitException {
    fSourcePageIndex = addPage(fTextEditor, getEditorInput());
    setPageText(fSourcePageIndex, XMLEditorMessages.XMLMultiPageEditorPart_0);

    firePropertyChange(PROP_TITLE);

    // Changes to the Text Viewer's document instance should also
    // force an
    // input refresh
    fTextInputListener = new TextInputListener();
    fTextEditor.getTextViewer().addTextInputListener(fTextInputListener);
  }

  /**
   * Connects the design viewer with the viewer selection manager. Should be done after
   * createSourcePage() is done because we need to get the ViewerSelectionManager from the
   * TextEditor. setModel is also done here because getModel() needs to reference the TextEditor.
   */
  private void connectDesignPage() {
    if (fDesignViewer != null) {
      fDesignViewer.setDocument(getDocument());
    }

    /*
     * Connect selection from the Design page to the selection provider for the
     * XMLMultiPageEditorPart so that selection changes in the Design page will propagate across the
     * workbench
     */
    if (fDesignViewer.getSelectionProvider() instanceof IPostSelectionProvider) {
      ((IPostSelectionProvider) fDesignViewer.getSelectionProvider()).addPostSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          if (getActivePage() != fSourcePageIndex) {
            ((MultiPageSelectionProvider) getSite().getSelectionProvider()).firePostSelectionChanged(event);
          }
        }
      });
    }
    fDesignViewer.getSelectionProvider().addSelectionChangedListener(
        new ISelectionChangedListener() {
          public void selectionChanged(SelectionChangedEvent event) {
            if (getActivePage() != fSourcePageIndex) {
              ((MultiPageSelectionProvider) getSite().getSelectionProvider()).fireSelectionChanged(event);
            }
          }
        });

    fDesignViewer.getSelectionProvider().addSelectionChangedListener(
        new ISelectionChangedListener() {
          public void selectionChanged(SelectionChangedEvent event) {
            if (getActivePage() != fSourcePageIndex) {
              try {
                updateStatusLine(event.getSelection());
              } catch (Exception exception) {
                Logger.logException(exception);
              }
            }
          }
        });

    /*
     * Handle double-click in the Design page by selecting the corresponding amount of text in the
     * Source page.
     * 
     * Warning: This implies more knowledge of the design viewer's underlying Control than expressed
     * in the IDesignViewer interface
     */
    fDesignViewer.getControl().addListener(SWT.MouseDoubleClick, new Listener() {
      public void handleEvent(Event event) {
        ISelection selection = fDesignViewer.getSelectionProvider().getSelection();
        int start = -1;
        int length = -1;
        if (selection instanceof IStructuredSelection) {
          /*
           * selection goes from the start of the first object to the end of the last
           */
          IStructuredSelection structuredSelection = (IStructuredSelection) selection;
          Object o = structuredSelection.getFirstElement();
          Object o2 = null;
          if (structuredSelection.size() > 1) {
            o2 = structuredSelection.toArray()[structuredSelection.size() - 1];
          } else {
            o2 = o;
          }
          if (o instanceof IndexedRegion) {
            start = ((IndexedRegion) o).getStartOffset();
            length = ((IndexedRegion) o2).getEndOffset() - start;
          } else if (o2 instanceof ITextRegion) {
            start = ((ITextRegion) o).getStart();
            length = ((ITextRegion) o2).getEnd() - start;
          }
        } else if (selection instanceof ITextSelection) {
          start = ((ITextSelection) selection).getOffset();
          length = ((ITextSelection) selection).getLength();
        }
        if ((start > -1) && (length > -1)) {
          getTextEditor().selectAndReveal(start, length);
        }
      }
    });

    /*
     * Connect selection from the Source page to the selection provider of the Design page so that
     * selection in the Source page will drive selection in the Design page. Prefer post selection.
     */
    ISelectionProvider provider = getTextEditor().getSelectionProvider();
    if (fTextEditorSelectionListener == null) {
      fTextEditorSelectionListener = new TextEditorPostSelectionAdapter();
    }
    if (provider instanceof IPostSelectionProvider) {
      fTextEditorSelectionListener.forcePostSelection = false;
      ((IPostSelectionProvider) provider).addPostSelectionChangedListener(fTextEditorSelectionListener);
    } else {
      fTextEditorSelectionListener.forcePostSelection = true;
      provider.addSelectionChangedListener(fTextEditorSelectionListener);
    }
  }

  /**
   * Create and Add the Design Page using a registered factory
   */
  private void createAndAddDesignPage() {
    IDesignViewer designViewer = createDesignPage();

    fDesignViewer = designViewer;
    // note: By adding the design page as a Control instead of an
    // IEditorPart, page switches will indicate
    // a "null" active editor when the design page is made active
    if (fDesignContainer != null)
      fDesignPageIndex = addPage(fDesignContainer);
    else
      fDesignPageIndex = addPage(designViewer.getControl());

    setPageText(fDesignPageIndex, designViewer.getTitle());
  }

  protected IDesignViewer createDesignPage() {
    Composite container = getDesignContainer(getContainer());

    XMLTableTreeViewer tableTreeViewer = new XMLTableTreeViewer(container);
    tableTreeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    // Set the default info-pop for XML design viewer.
    XMLUIPlugin.getInstance().getWorkbench().getHelpSystem().setHelp(tableTreeViewer.getControl(),
        XMLTableTreeHelpContextIds.XML_DESIGN_VIEW_HELPID);

    // Toolbar wasn't allocated
    if (fToolbarManager != null && fEditorManager != null) {
      addToolBarActions(tableTreeViewer);
      addEditorActions(tableTreeViewer);
    }
    return tableTreeViewer;
  }

  protected Composite getDesignContainer(Composite defaultContainer) {
    Composite container = defaultContainer;
    // create a container to hold the toolbar if it should be created
    if (fAllocateToolbar) {
      container = new Composite(defaultContainer, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.marginHeight = 0;
      layout.verticalSpacing = 0;
      layout.marginWidth = 0;
      container.setLayout(layout);

      Composite toolbarContainer = new Composite(container, SWT.NONE);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.verticalSpacing = 0;
      layout.marginWidth = 0;
      layout.horizontalSpacing = 0;
      layout.numColumns = 2;
      toolbarContainer.setLayout(layout);
      toolbarContainer.setLayoutData(new GridData(GridData.END, GridData.VERTICAL_ALIGN_BEGINNING,
          true, false));

      ToolBar tb = new ToolBar(toolbarContainer, SWT.FLAT);
      fToolbarManager = new ToolBarManager(tb);
      tb.setLayoutData(new GridData(GridData.END, GridData.VERTICAL_ALIGN_BEGINNING, true, false));
      tb = new ToolBar(toolbarContainer, SWT.FLAT);
      fEditorManager = new ToolBarManager(tb);
      tb.setLayoutData(new GridData(GridData.END, GridData.VERTICAL_ALIGN_BEGINNING, true, false));
      fDesignContainer = container;
    }
    return container;
  }

  private class EditorActions extends Action {
    private ToolBar fToolbar;

    public EditorActions(ToolBar toolbar) {
      fToolbar = toolbar;
    }

    public ImageDescriptor getImageDescriptor() {
      return XMLEditorPluginImageHelper.getInstance().getImageDescriptor(
          XMLEditorPluginImageHelper.EDITOR_MENU);
    }

    public String getToolTipText() {
      return XMLEditorMessages.EditorMenu_tooltip;
    }

    public void run() {
      Menu menu = fMenuManager.createContextMenu(fDesignContainer);
      Point size = fToolbar.getSize();
      Point location = fToolbar.toDisplay(0, size.y);
      menu.setLocation(location.x, location.y);
      menu.setVisible(true);
    }
  }

  private void addToolBarActions(IDesignViewer viewer) {
    if (viewer instanceof AbstractTreeViewer) {
      ViewerExpandCollapseAction expand = new ViewerExpandCollapseAction(true);
      ViewerExpandCollapseAction collapse = new ViewerExpandCollapseAction(false);
      fToolbarManager.add(expand);
      fToolbarManager.add(collapse);
      fToolbarManager.update(true);

      expand.setViewer((AbstractTreeViewer) viewer);
      collapse.setViewer((AbstractTreeViewer) viewer);
    }
  }

  private void addEditorActions(IDesignViewer viewer) {
    if (viewer instanceof AbstractTreeViewer) {
      final Tree tree = (Tree) ((AbstractTreeViewer) viewer).getControl();
      fMenuManager = new MenuManager();
      fMenuManager.add(new Action(XMLEditorMessages.ConfigureColumns_label) {
        public void run() {
          ConfigureColumns.forTree(tree, new SameShellProvider(tree));
        }
      });
      getSite().registerContextMenu(
          "org.eclipse.wst.xml.ui.editor", fMenuManager, getSite().getSelectionProvider()); //$NON-NLS-1$
      fMenuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

      fEditorManager.add(new EditorActions(fEditorManager.getControl()));
      fEditorManager.update(true);
    }
  }

  /**
   * Creates the pages of this multi-page editor.
   * <p>
   * Subclasses of <code>MultiPageEditor</code> must implement this method.
   * </p>
   */
  protected void createPages() {
    try {
      // source page MUST be created before design page, now
      createSourcePage();

      createAndAddDesignPage();
      addSourcePage();
      connectDesignPage();

      // set the active editor in the action bar contributor first
      // before setactivepage calls action bar contributor's
      // setactivepage (bug141013 - remove when bug151488 is fixed)
      IEditorActionBarContributor contributor = getEditorSite().getActionBarContributor();
      if (contributor instanceof MultiPageEditorActionBarContributor) {
        ((MultiPageEditorActionBarContributor) contributor).setActiveEditor(this);
      }

      int activePageIndex = getPreferenceStore().getInt(
          getEditorSite().getId() + "." + IXMLPreferenceNames.LAST_ACTIVE_PAGE); //$NON-NLS-1$;
      if ((activePageIndex >= 0) && (activePageIndex < getPageCount())) {
        setActivePage(activePageIndex);
      } else {
        setActivePage(fSourcePageIndex);
      }
    } catch (PartInitException e) {
      Logger.logException(e);
      throw new RuntimeException(e);
    }
  }

  /**
   * @see org.eclipse.ui.part.MultiPageEditorPart#createSite(org.eclipse.ui.IEditorPart)
   */
  protected IEditorSite createSite(IEditorPart editor) {
    IEditorSite site = null;
    if (editor == fTextEditor) {
      site = new MultiPageEditorSite(this, editor) {
        /**
         * @see org.eclipse.ui.part.MultiPageEditorSite#getActionBarContributor()
         */
        public IEditorActionBarContributor getActionBarContributor() {
          IEditorActionBarContributor contributor = super.getActionBarContributor();
          IEditorActionBarContributor multiContributor = XMLMultiPageEditorPart.this.getEditorSite().getActionBarContributor();
          if (multiContributor instanceof XMLMultiPageEditorActionBarContributor) {
            contributor = ((XMLMultiPageEditorActionBarContributor) multiContributor).sourceViewerActionContributor;
          }
          return contributor;
        }

        public String getId() {
          // sets this id so nested editor is considered xml source
          // page
          return ContentTypeIdForXML.ContentTypeID_XML + ".source"; //$NON-NLS-1$;
        }
      };
    } else {
      site = super.createSite(editor);
    }
    return site;
  }

  /**
   * Creates the source page of the multi-page editor.
   */
  protected void createSourcePage() throws PartInitException {
    /*
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=134301 - XML editor does not remember font
     * settings
     * 
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#getSymbolicFontName()
     */
    fPageInitializer.sendInitializationData(fTextEditor);

    if (fPropertyListener == null) {
      fPropertyListener = new PropertyListener();
    }
    fTextEditor.addPropertyListener(fPropertyListener);
  }

  /**
   * Method createTextEditor.
   * 
   * @return StructuredTextEditor
   */
  private StructuredTextEditor createTextEditor() {
    return new StructuredTextEditor();
  }

  private void disconnectDesignPage() {
    if (fDesignViewer != null) {
      fDesignViewer.setDocument(null);
    }
  }

  public void dispose() {
    Logger.trace("Source Editor", "XMLMultiPageEditorPart::dispose entry"); //$NON-NLS-1$ //$NON-NLS-2$

    if (fTextInputListener != null) {
      fTextEditor.getTextViewer().removeTextInputListener(fTextInputListener);
      fTextInputListener = null;
    }
    disconnectDesignPage();
    fDesignViewer = null;

    if (fActivationListener != null) {
      fActivationListener.dispose();
      fActivationListener = null;
    }

    if (fMenuManager != null) {
      fMenuManager.removeAll();
      fMenuManager.dispose();
      fMenuManager = null;
    }

    if (fPropertyListener != null) {
      fTextEditor.removePropertyListener(fPropertyListener);
      fPropertyListener = null;
    }

    if (fEditorManager != null) {
      fEditorManager.dispose();
      fEditorManager = null;
    }
    if (fToolbarManager != null) {
      fToolbarManager.dispose();
      fToolbarManager = null;
    }
    // moved to last when added window ... seems like
    // we'd be in danger of losing some data, like site,
    // or something.
    super.dispose();

    fTextEditor = null;
    fPageInitializer = null;

    Logger.trace("Source Editor", "StructuredTextMultiPageEditorPart::dispose exit"); //$NON-NLS-1$ //$NON-NLS-2$

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void doSave(IProgressMonitor monitor) {
    fTextEditor.doSave(monitor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#doSaveAs()
   */
  public void doSaveAs() {
    fTextEditor.doSaveAs();
    /*
     * Update the design viewer since the editor input would have changed to the new file.
     */
    if (fDesignViewer != null) {
      fDesignViewer.setDocument(getDocument());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class key) {
    Object result = null;

    // we extend superclass, not override it, so allow it first
    // chance to satisfy request.
    result = super.getAdapter(key);

    if (result == null) {
      if (key == IDesignViewer.class) {
        result = fDesignViewer;

      } else if (key.equals(IGotoMarker.class)) {
        result = new IGotoMarker() {
          public void gotoMarker(IMarker marker) {
            XMLMultiPageEditorPart.this.gotoMarker(marker);
          }
        };
      } else {
        /*
         * DMW: I'm bullet-proofing this because its been reported (on very early versions) a null
         * pointer sometimes happens here on startup, when an editor has been left open when
         * workbench shutdown.
         */
        if (fTextEditor != null) {
          result = fTextEditor.getAdapter(key);
        }
      }
    }
    return result;
  }

  private IDocument getDocument() {
    IDocument document = null;
    if (fTextEditor != null) {
      final IDocumentProvider provider = fTextEditor.getDocumentProvider();
      if (provider != null) {
        document = provider.getDocument(fTextEditor.getEditorInput());
      }
    }
    return document;
  }

  private IPreferenceStore getPreferenceStore() {
    return XMLUIPlugin.getDefault().getPreferenceStore();
  }

  StructuredTextEditor getTextEditor() {
    return fTextEditor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#getTitle()
   */
  public String getTitle() {
    String title = null;
    if (getTextEditor() == null) {
      if (getEditorInput() != null) {
        title = getEditorInput().getName();
      }
    } else {
      title = getTextEditor().getTitle();
    }
    if (title == null) {
      title = getPartName();
    }
    return title;
  }

  void gotoMarker(IMarker marker) {
    setActivePage(fSourcePageIndex);
    IDE.gotoMarker(fTextEditor, marker);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
   */
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    try {
      super.init(site, input);
      fTextEditor = createTextEditor();
      fTextEditor.setEditorPart(this);
      // we want to listen for our own activation
      fActivationListener = new ActivationListener(site.getWorkbenchWindow().getPartService());
    } catch (Exception e) {
      Logger.logException("exception initializing " + getClass().getName(), e); //$NON-NLS-1$
    }
    setPartName(input.getName());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
   */
  public boolean isSaveAsAllowed() {
    return (fTextEditor != null) && fTextEditor.isSaveAsAllowed();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
   */
  public boolean isSaveOnCloseNeeded() {
    // overriding super class since it does a lowly isDirty!
    if (fTextEditor != null) {
      return fTextEditor.isSaveOnCloseNeeded();
    }
    return isDirty();
  }

  /**
   * Prevents the creation of the in-editor toolbar, if called before createPageContainer() during
   * editor initialization.
   */
  protected final void noToolbar() {
    fAllocateToolbar = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.part.MultiPageEditorPart#pageChange(int)
   */
  protected void pageChange(int newPageIndex) {
    if (newPageIndex == fSourcePageIndex) {
      ISelectionProvider provider = fDesignViewer.getSelectionProvider();
      if (provider != null) {
        getTextEditor().getSelectionProvider().setSelection(provider.getSelection());
      }
    }
    super.pageChange(newPageIndex);
    saveLastActivePageIndex(newPageIndex);

    if (newPageIndex == fDesignPageIndex) {
      // design page isn't an IEditorPart, therefore we have to send
      // selection changes ourselves
      ISelectionProvider selectionProvider = fDesignViewer.getSelectionProvider();
      if (selectionProvider != null) {
        SelectionChangedEvent event = new SelectionChangedEvent(selectionProvider,
            selectionProvider.getSelection());
        ((MultiPageSelectionProvider) getSite().getSelectionProvider()).fireSelectionChanged(event);
        ((MultiPageSelectionProvider) getSite().getSelectionProvider()).firePostSelectionChanged(event);
      }
    }
  }

  private void saveLastActivePageIndex(int newPageIndex) {
    // save the last active page index to preference manager
    getPreferenceStore().setValue(
        getEditorSite().getId() + "." + IXMLPreferenceNames.LAST_ACTIVE_PAGE, newPageIndex); //$NON-NLS-1$
  }

  public void setFocus() {
    super.setFocus();
    Control control = fDesignViewer.getControl();
    control.setFocus();
    // 271382 - Focus not set properly after activating XML editor
    control.forceFocus();
  }

  public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
    super.setInitializationData(cfig, propertyName, data);
    fPageInitializer = new PageInitializationData(cfig, propertyName, data);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
   */
  protected void setInput(IEditorInput input) {
    /*
     * If driven from the Source page, it's "model" may not be up to date with (or even exist for)
     * the input just yet. Later notification from the TextViewer could set us straight, although
     * it's not guaranteed to happen after the model has been created.
     */
    super.setInput(input);
    if (fDesignViewer != null) {
      fDesignViewer.setDocument(getDocument());
    }
    setPartName(input.getName());
  }

  void updateStatusLine(ISelection selection) {
    IStatusLineManager statusLineManager = getEditorSite().getActionBars().getStatusLineManager();
    if (fStatusLineLabelProvider != null && statusLineManager != null) {
      String text = null;
      Image image = null;
      if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
        Object firstElement = ((IStructuredSelection) selection).getFirstElement();
        if (firstElement != null) {
          text = fStatusLineLabelProvider.getText(firstElement);
          image = fStatusLineLabelProvider.getImage((firstElement));
        }
      }
      if (image == null) {
        statusLineManager.setMessage(text);
      } else {
        statusLineManager.setMessage(image, text);
      }
    }
  }

  public INavigationLocation createEmptyNavigationLocation() {
    if (getActivePage() == fDesignPageIndex) {
      return new DesignPageNavigationLocation(this, fDesignViewer, false);
    }
    // Makes sure that the text editor is returned
    return fTextEditor.createEmptyNavigationLocation();
  }

  public INavigationLocation createNavigationLocation() {
    if (getActivePage() == fDesignPageIndex) {
      return new DesignPageNavigationLocation(this, fDesignViewer, true);
    }
    // Makes sure that the text editor is returned
    return fTextEditor.createNavigationLocation();
  }

  public void saveState(IMemento memento) {
    if (fTextEditor != null) {
      fTextEditor.saveState(memento);
    }
  }

  public void restoreState(IMemento memento) {
    if (fTextEditor != null) {
      fTextEditor.restoreState(memento);
    }
  }
}
