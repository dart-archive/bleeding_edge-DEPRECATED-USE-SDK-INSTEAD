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
import com.google.dart.tools.ui.web.html.HtmlEditor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.editor.DesignComposite;
import org.eclipse.wb.internal.core.preferences.IPreferenceConstants;
import org.eclipse.wb.internal.core.utils.execution.ExecutionUtils;
import org.eclipse.wb.internal.core.utils.execution.RunnableEx;
import org.eclipse.wb.internal.core.utils.external.ExternalFactoriesHelper;
import org.eclipse.wb.internal.core.utils.ui.GridDataFactory;
import org.eclipse.wb.internal.core.utils.ui.GridLayoutFactory;
import org.eclipse.wb.internal.core.utils.ui.TabFolderDecorator;
import org.eclipse.wb.internal.core.utils.ui.UiUtils;
import org.eclipse.wb.internal.core.views.IDesignCompositeProvider;

import java.util.List;

/**
 * Editor for any XML based UI.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public abstract class AbstractXmlEditor extends MultiPageEditorPart implements
    IDesignCompositeProvider {
  private static final String CONTEXT_ID = "com.google.dart.tools.designer.editorScope";
  protected HtmlEditor m_xmlEditor;
  private SourcePage m_sourcePage;
  private XmlDesignPage m_designPage;
  private final List<IXmlEditorPage> m_additionalPages = Lists.newArrayList();
  private IXmlEditorPage m_activePage;
//  private String m_cleanSource;
  private Control m_partControl;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public AbstractXmlEditor() {
    DesignerPlugin.configurePreEditor();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Life cycle
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void dispose() {
    // dispose pages
    m_sourcePage.dispose();
    m_designPage.dispose();
    for (IXmlEditorPage page : m_additionalPages) {
      page.dispose();
    }
    // clear page references (sometimes top level editor leaks)
    {
      m_activePage = null;
      m_sourcePage = null;
      m_designPage = null;
      m_additionalPages.clear();
    }
    // continue
    super.dispose();
  }

  @Override
  protected void setInput(IEditorInput input) {
    super.setInput(input);
    initializeTitle(input);
  }

  private void initializeTitle(IEditorInput input) {
    if (input != null) {
      String title = input.getName();
      setPartName(title);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // EditorPart
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
    if (!(editorInput instanceof IFileEditorInput)) {
      throw new PartInitException("Invalid Input: Must be IFileEditorInput");
    }
    super.init(site, editorInput);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the {@link IDocument} from XML editor.
   */
  public final IDocument getDocument() {
    return m_xmlEditor.getDocument();
  }

  /**
   * @return the {@link SourcePage} which is used as "Source" page.
   */
  public final SourcePage getSourcePage() {
    return m_sourcePage;
  }

  /**
   * @return the {@link XmlDesignPage} which is used as "Design" page.
   */
  public final XmlDesignPage getDesignPage() {
    return m_designPage;
  }

  /**
   * @return the top level {@link Control} of this {@link AbstractXmlEditor} part.
   */
  public Control getPartControl() {
    return m_partControl;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IAdaptable
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter) {
    if (ITextEditor.class.isAssignableFrom(adapter)) {
      return m_xmlEditor;
    }
    return super.getAdapter(adapter);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IDesignCompositeProvider
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public DesignComposite getDesignComposite() {
    return m_designPage.getDesignComposite();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Save
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void doSave(IProgressMonitor monitor) {
//    rememberSourceContent();
    m_xmlEditor.doSave(monitor);
    if (m_splitRefreshStrategy.shouldOnSave()) {
      m_designPage.updateGEF();
    }
  }

  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  @Override
  public void doSaveAs() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return <code>true</code> "Source" page should be first.
   */
  private boolean isSourceFirst() {
    int layout = DesignerPlugin.getPreferences().getInt(IPreferenceConstants.P_EDITOR_LAYOUT);
    return layout == IPreferenceConstants.V_EDITOR_LAYOUT_PAGES_SOURCE
        || layout == IPreferenceConstants.V_EDITOR_LAYOUT_SPLIT_HORIZONTAL_SOURCE
        || layout == IPreferenceConstants.V_EDITOR_LAYOUT_SPLIT_VERTICAL_SOURCE;
  }

  /**
   * @return <code>true</code> if "page mode".
   */
  private boolean isPagesMode() {
    int layout = DesignerPlugin.getPreferences().getInt(IPreferenceConstants.P_EDITOR_LAYOUT);
    return layout == IPreferenceConstants.V_EDITOR_LAYOUT_PAGES_SOURCE
        || layout == IPreferenceConstants.V_EDITOR_LAYOUT_PAGES_DESIGN;
  }

  /**
   * @return <code>true</code> "split mode".
   */
  private boolean isSplitMode() {
    int layout = DesignerPlugin.getPreferences().getInt(IPreferenceConstants.P_EDITOR_LAYOUT);
    return layout == IPreferenceConstants.V_EDITOR_LAYOUT_SPLIT_HORIZONTAL_DESIGN
        || layout == IPreferenceConstants.V_EDITOR_LAYOUT_SPLIT_HORIZONTAL_SOURCE
        || layout == IPreferenceConstants.V_EDITOR_LAYOUT_SPLIT_VERTICAL_DESIGN
        || layout == IPreferenceConstants.V_EDITOR_LAYOUT_SPLIT_VERTICAL_SOURCE;
  }

  /**
   * @return <code>true</code> horizontal "split mode".
   */
  private boolean isSplitModeHorizontal() {
    int layout = DesignerPlugin.getPreferences().getInt(IPreferenceConstants.P_EDITOR_LAYOUT);
    return layout == IPreferenceConstants.V_EDITOR_LAYOUT_SPLIT_HORIZONTAL_DESIGN
        || layout == IPreferenceConstants.V_EDITOR_LAYOUT_SPLIT_HORIZONTAL_SOURCE;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Pages
  //
  ////////////////////////////////////////////////////////////////////////////
  private Composite m_splitSourceContainer;
  private SashForm m_splitSashForm;
  private final IRefreshStrategy m_splitRefreshStrategy = new IRefreshStrategy() {
    @Override
    public boolean shouldImmediately() {
      return false;
    }

    @Override
    public boolean shouldWithDelay() {
      return getPreferenceDelay() > 0;
    }

    @Override
    public boolean shouldOnSave() {
      return getPreferenceDelay() <= 0;
    }

    @Override
    public int getDelay() {
      int delay = getPreferenceDelay();
      return Math.max(delay, 250);
    }

    private int getPreferenceDelay() {
      IPreferenceStore preferences = DesignerPlugin.getPreferences();
      return preferences.getInt(IPreferenceConstants.P_EDITOR_LAYOUT_SYNC_DELAY);
    }
  };

  @Override
  protected Composite createPageContainer(Composite parent) {
    m_partControl = parent;
    parent = super.createPageContainer(parent);
    if (isPagesMode()) {
      return parent;
    } else {
      int style = isSplitModeHorizontal() ? SWT.HORIZONTAL : SWT.VERTICAL;
      m_splitSashForm = new SashForm(parent, style);
      return m_splitSashForm;
    }
  }

  @Override
  protected void createPages() {
    // decorate CTabFolder
    {
      CTabFolder tabFolder = (CTabFolder) getContainer();
      TabFolderDecorator.decorate(this, tabFolder);
    }
    // do create pages
    if (isSourceFirst()) {
      createPageXml();
      createPageDesign(1);
      m_sourcePage.setPageIndex(0);
      m_designPage.setPageIndex(1);
    } else {
      createPageXml();
      createPageDesign(0);
      m_designPage.getControl().moveAbove(null);
      m_sourcePage.setPageIndex(1);
      m_designPage.setPageIndex(0);
    }
    createAdditionalPages();
    // activate page
    activateEditorContext();
    // tweak for "split mode"
    if (isSplitMode()) {
      createPageSplit();
    }
  }

  /**
   * Updates UI for "split mode".
   */
  private void createPageSplit() {
    trackSourceActivation();
    // prepare Composite for "Source" control
    {
      m_splitSourceContainer = new Composite(m_splitSashForm, SWT.NONE);
      // layout and separator
      if (isSplitModeHorizontal()) {
        GridLayoutFactory.create(m_splitSourceContainer).columns(2).noMargins().noSpacing();
        {
          LineControl separator = new LineControl(m_splitSourceContainer, SWT.VERTICAL);
          GridDataFactory.create(separator).grabV().fillV();
        }
      } else {
        GridLayoutFactory.create(m_splitSourceContainer).columns(1).noMargins().noSpacing();
        {
          LineControl separator = new LineControl(m_splitSourceContainer, SWT.HORIZONTAL);
          GridDataFactory.create(separator).grabH().fillH();
        }
      }
      // sash
      m_splitSashForm.setWeights(new int[] {60, 40});
    }
    // move "Source" control to "sash"
    int sourceIndex = m_sourcePage.getPageIndex();
    Control control = getControl(sourceIndex);
    {
      control.setParent(m_splitSourceContainer);
      control.setVisible(true);
      GridDataFactory.create(control).grab().fill();
    }
    // if "Source" first, then move it
    if (isSourceFirst()) {
      control.moveAbove(null);
      m_splitSourceContainer.moveAbove(null);
    }
    // remove "Source" tab
    {
      CTabFolder tabFolder = (CTabFolder) getContainer();
      // dispose "Source" item
      CTabItem item = tabFolder.getItem(sourceIndex);
      item.dispose();
      // show "Design" tab
      tabFolder.setSelection(0);
      // if only one tab, don't show tabs
      if (tabFolder.getItems().length == 1) {
        tabFolder.setTabHeight(0);
      }
    }
    // "Design" is always active
    {
      m_designPage.setActive(true);
      m_designPage.forceDocumentListener();
      m_designPage.setRefreshStrategy(m_splitRefreshStrategy);
      m_designPage.setActive(false);
    }
    // activate "Source"
    pageChange(m_sourcePage.getPageIndex());
    m_xmlEditor.setFocus();
  }

  private void trackSourceActivation() {
    final Display display = Display.getDefault();
    display.addFilter(SWT.MouseDown, new Listener() {
      @Override
      public void handleEvent(Event event) {
        // editor was disposed
        if (m_sourcePage == null) {
          display.removeFilter(SWT.MouseDown, this);
          return;
        }
        // track location in "source" or "design"
        Composite sourceControl = (Composite) m_sourcePage.getControl();
        Composite designControl = getContainer();
        if (UiUtils.isChildOf(sourceControl, event.widget)) {
          int pageIndex = m_sourcePage.getPageIndex();
          pageChange(pageIndex);
          m_designPage.setRefreshStrategy(m_splitRefreshStrategy);
        }
        if (UiUtils.isChildOf(designControl, event.widget)) {
          int pageIndex = m_designPage.getPageIndex();
          pageChange(pageIndex);
          m_designPage.setRefreshStrategy(IRefreshStrategy.IMMEDIATELY);
        }
      }
    });
  }

  @Override
  public int getActivePage() {
    int pageIndex = super.getActivePage();
    if (isSplitMode() && m_sourcePage != null) {
      int sourcePageIndex = m_sourcePage.getPageIndex();
      if (m_activePage == m_sourcePage) {
        return sourcePageIndex;
      }
      if (pageIndex >= sourcePageIndex) {
        pageIndex++;
      }
    }
    return pageIndex;
  }

  @Override
  protected int getPageCount() {
    int pageCount = super.getPageCount();
    if (isSplitMode() && m_sourcePage != null) {
      boolean isSourceExtracted = !UiUtils.isChildOf(getContainer(), m_sourcePage.getControl());
      if (isSourceExtracted) {
        pageCount++;
      }
    }
    return pageCount;
  }

  @Override
  protected Control getControl(int pageIndex) {
    if (isSplitMode() && m_sourcePage != null) {
      int sourcePageIndex = m_sourcePage.getPageIndex();
      if (pageIndex == sourcePageIndex) {
        return m_sourcePage.getControl();
      }
      if (pageIndex > sourcePageIndex) {
        pageIndex--;
      }
    }
    return super.getControl(pageIndex);
  }

  @Override
  protected IEditorPart getEditor(int pageIndex) {
    if (isSplitMode() && m_sourcePage != null) {
      int sourcePageIndex = m_sourcePage.getPageIndex();
      if (pageIndex == sourcePageIndex) {
        return m_xmlEditor;
      }
      if (pageIndex > sourcePageIndex) {
        pageIndex--;
      }
    }
    return super.getEditor(pageIndex);
  }

  /**
   * Activates context of our XML editor.
   */
  private void activateEditorContext() {
    IContextService contextService = (IContextService) getSite().getService(IContextService.class);
    if (contextService != null) {
      contextService.activateContext(CONTEXT_ID);
    }
  }

  /**
   * Creates "XML Source" page of multi-page editor.
   */
  private void createPageXml() {
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        m_xmlEditor = createEditorXml();
        int pageIndex = addPage(m_xmlEditor, getEditorInput());
        Control control = getControl(pageIndex);
        // create XML page
        m_sourcePage = new SourcePage(m_xmlEditor, control);
        m_sourcePage.initialize(AbstractXmlEditor.this);
        m_sourcePage.setPageIndex(pageIndex);
        // configure page tab
        setPageText(pageIndex, m_sourcePage.getName());
        setPageImage(pageIndex, m_sourcePage.getImage());
//        // track changes and update dirty flag
//        trackDirty();
      }
    });
  }

  /**
   * @return the {@link ITextEditor} to use as HTML editor.
   */
  protected HtmlEditor createEditorXml() {
    return new HtmlEditor();
  }

  /**
   * Creates "Design" page of multi-page editor.
   */
  private void createPageDesign(int pageIndex) {
    m_designPage = createDesignPage();
    addPage(pageIndex, m_designPage);
  }

  /**
   * Create additional pages.
   */
  private void createAdditionalPages() {
    List<IXmlEditorPageFactory> factories = ExternalFactoriesHelper.getElementsInstances(
        IXmlEditorPageFactory.class,
        "org.eclipse.wb.core.xml.XMLEditorPageFactories",
        "factory");
    for (IXmlEditorPageFactory factory : factories) {
      factory.createPages(this, m_additionalPages);
    }
    // initialize created pages
    for (IXmlEditorPage page : m_additionalPages) {
      page.initialize(this);
      addPage(page);
    }
  }

  /**
   * Add {@link IXmlEditorPage} page to this editor.
   */
  private void addPage(IXmlEditorPage page) {
    int index = getPageCount();
    addPage(index, page);
  }

  /**
   * Add {@link IXmlEditorPage} page to this editor.
   */
  private void addPage(int pageIndex, IXmlEditorPage page) {
    page.initialize(this);
    // create/add control
    Control control = page.createControl(getContainer());
    addPage(pageIndex, control);
    page.setPageIndex(pageIndex);
    // presentation
    setPageText(pageIndex, page.getName());
    setPageImage(pageIndex, page.getImage());
  }

  /**
   * @return the {@link XmlDesignPage} to be used as "Design" page.
   */
  protected abstract XmlDesignPage createDesignPage();

//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // Dirty flag tracking
//  //
//  ////////////////////////////////////////////////////////////////////////////
//  /**
//   * WST SSE has bug with undo/redo and "dirty" flag. So, we need to track "dirty" flag manually.
//   * <p>
//   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=138100
//   */
//  private void trackDirty() {
//    rememberSourceContent();
//    getDocument().addDocumentListener(new IDocumentListener() {
//      @Override
//      public void documentChanged(DocumentEvent event) {
//        firePropertyChange(PROP_DIRTY);
//      }
//
//      @Override
//      public void documentAboutToBeChanged(DocumentEvent event) {
//      }
//    });
//  }
//
//  private void rememberSourceContent() {
//    m_cleanSource = getDocument().get();
//  }
//
//  @Override
//  public boolean isDirty() {
//    return !getDocument().get().equals(m_cleanSource);
//  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Page access
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void pageChange(int pageIndex) {
    // tweak page index
    if (isSplitMode() && pageIndex >= m_sourcePage.getPageIndex()) {
      String callerMethodName = Thread.currentThread().getStackTrace()[2].getMethodName();
      boolean isUserPageChangeRequest = callerMethodName.equals("widgetSelected");
      if (isUserPageChangeRequest) {
        pageIndex++;
      }
    }
    // prepare new active page
    IXmlEditorPage activePage = m_activePage;
    if (pageIndex == m_sourcePage.getPageIndex()) {
      activePage = m_sourcePage;
    } else if (pageIndex == m_designPage.getPageIndex()) {
      activePage = m_designPage;
    } else {
      for (IXmlEditorPage page : m_additionalPages) {
        if (pageIndex == page.getPageIndex()) {
          activePage = page;
          break;
        }
      }
    }
    // do nothing, if this page is already active
    // else this will cause loosing "..." button clicking in properties table
    if (activePage == m_activePage) {
      return;
    }
    // deactivate active page
    if (m_activePage != null) {
      m_activePage.setActive(false);
      m_activePage = null;
    }
    // activate new active page
    m_activePage = activePage;
    // We use "fake" item because we can not override private getItem() method.
    // Currently (20110623) this item is not used for anything useful for WindowBuilder.
    {
      CTabItem fakeItem = isSplitMode() ? new CTabItem((CTabFolder) getContainer(), SWT.NONE)
          : null;
      try {
        super.pageChange(pageIndex);
      } finally {
        if (fakeItem != null) {
          fakeItem.dispose();
        }
      }
    }
    // activate new active page
    m_activePage = activePage;
    if (m_activePage != null) {
      m_activePage.setActive(true);
    }
  }

  /**
   * Switches between "Source" and "Design" pages.
   */
  public void switchSourceDesign() {
    if (m_activePage == m_sourcePage) {
      setActivePage(m_designPage);
    } else {
      setActivePage(m_sourcePage);
    }
  }

  /**
   * Shows "XML Source" page.
   */
  public void showSource() {
    if (m_activePage != m_sourcePage) {
      setActivePage(m_sourcePage);
    }
  }

  /**
   * Shows "Design" page.
   */
  public void showDesign() {
    if (m_activePage != m_designPage) {
      setActivePage(m_designPage);
    }
  }

  /**
   * Shows given {@link IXmlEditorPage}.
   */
  private void setActivePage(IXmlEditorPage page) {
    int pageIndex = page.getPageIndex();
    if (isPagesMode()) {
      setActivePage(pageIndex);
    } else {
      pageChange(pageIndex);
    }
    page.getControl().setFocus();
  }

  /**
   * Moves cursor to given position in "XML Source" editor.
   */
  public void showSourcePosition(final int position) {
    ExecutionUtils.runLogLater(new RunnableEx() {
      @Override
      public void run() throws Exception {
        m_xmlEditor.selectAndReveal(position, 0);
      }
    });
  }
}
