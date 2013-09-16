/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation David Carver - bug 212330 - migrate to org.eclipse.ui.menus extension point
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.CMDocumentLoader;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.InferredGrammarBuildingCMDocumentLoader;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.ui.internal.util.SharedXMLEditorPluginImageHelper;
import org.w3c.dom.Document;

/**
 * 
 */
public class XMLTableTreeActionBarContributor implements IDesignViewerActionBarContributor {

  protected IEditorPart editorPart;
  protected final static String DESIGN_VIEWER_SEPARATOR_1_ID = "sed.tabletree.separator.1"; //$NON-NLS-1$
  protected final static String DESIGN_VIEWER_SEPARATOR_2_ID = "sed.tabletree.separator.2"; //$NON-NLS-1$
  protected final static String VALIDATE_XML_ID = "sed.tabletree.validateXML"; //$NON-NLS-1$
  protected final static String RELOAD_GRAMMAR_ID = "sed.tabletree.reloadGrammar"; //$NON-NLS-1$
  protected final static String TOGGLE_EDIT_MODE_ID = "sed.tabletree.toggleEditMode"; //$NON-NLS-1$
  protected final static String EXPAND_ALL_ID = "sed.tabletree.expandAll"; //$NON-NLS-1$
  protected final static String COLLAPSE_ALL_ID = "sed.tabletree.collapseAll"; //$NON-NLS-1$

  protected ToggleEditModeAction toggleAction;
  protected ReloadGrammarAction reloadGrammarAction;
  // protected ValidateXMLAction validateXMLAction;
  protected ViewerExpandCollapseAction expandAction;
  protected ViewerExpandCollapseAction collapseAction;
  protected ViewerExpandCollapseAction xmlMenuExpandAction;
  protected ViewerExpandCollapseAction xmlMenuCollapseAction;
  private IActionBars actionBars;

  public XMLTableTreeActionBarContributor() {
  }

  protected void removeContributions(IContributionManager manager) {
    /*
     * try { doRemove(manager, DESIGN_VIEWER_SEPARATOR_1_ID); doRemove(manager,
     * DESIGN_VIEWER_SEPARATOR_2_ID); doRemove(manager, VALIDATE_XML_ID); doRemove(manager,
     * RELOAD_GRAMMAR_ID); doRemove(manager, TOGGLE_EDIT_MODE_ID); doRemove(manager, EXPAND_ALL_ID);
     * doRemove(manager, COLLAPSE_ALL_ID); } catch (Exception e) { }
     */
  }

  protected void doRemove(IContributionManager manager, String id) {
    /*
     * try { if (manager.find(id) != null) { manager.remove(id); } } catch (Exception e) { }
     */}

  public void init(IActionBars bars, IWorkbenchPage page) {
    init(bars);
  }

  public void init(IActionBars bars) {
    this.actionBars = bars;
//		IToolBarManager tbm = bars.getToolBarManager();

    /*
     * IMenuManager xmlMenu =
     * bars.getMenuManager().findMenuUsingPath("org.eclipse.core.runtime.xml.design.xmlmenu");
     * //$NON-NLS-1$
     * 
     * if (xmlMenu == null) { xmlMenu = new
     * MenuManager(XMLEditorMessages.XMLTableTreeActionBarContributor_0,
     * "org.eclipse.core.runtime.xml.design.xmlmenu"); //$NON-NLS-1$ // For RCP usage if
     * (bars.getMenuManager().find(IWorkbenchActionConstants.M_WINDOW) != null) {
     * bars.getMenuManager().insertBefore(IWorkbenchActionConstants.M_WINDOW, xmlMenu); } } else {
     * removeContributions(xmlMenu); }
     * 
     * tbm.add(new Separator("DESIGN_VIEWER_SEPARATOR_1_ID")); //$NON-NLS-1$
     */
    // ToggleEditModeAction
    //           
    /*
     * toggleAction = new ToggleEditModeAction(); toggleAction.setId(TOGGLE_EDIT_MODE_ID);
     * xmlMenu.add(toggleAction); tbm.add(toggleAction);
     */
    // ReloadGrammarAction
    //
    /*
     * reloadGrammarAction = new ReloadGrammarAction();
     * reloadGrammarAction.setId(RELOAD_GRAMMAR_ID); tbm.add(reloadGrammarAction);
     * xmlMenu.add(reloadGrammarAction);
     * 
     * xmlMenu.add(new Separator());
     */
    // ExpandCollapseAction
    //
    /*
     * xmlMenuExpandAction = new ViewerExpandCollapseAction(true);
     * xmlMenuExpandAction.setId(EXPAND_ALL_ID);
     * xmlMenuExpandAction.setText(XMLEditorMessages.XMLTableTreeActionBarContributor_1);
     * xmlMenu.add(xmlMenuExpandAction);
     * 
     * xmlMenuCollapseAction = new ViewerExpandCollapseAction(false);
     * xmlMenuCollapseAction.setId(COLLAPSE_ALL_ID); xmlMenuCollapseAction.setId(EXPAND_ALL_ID);
     * xmlMenuCollapseAction.setText(XMLEditorMessages.XMLTableTreeActionBarContributor_2);
     * xmlMenu.add(xmlMenuCollapseAction);
     */
  }

  protected void addActionWithId(IMenuManager menuManager, Action action, String id) {
    action.setId(id);
    menuManager.add(action);
  }

  public void initViewerSpecificContributions(IActionBars bars) {
    /*
     * IToolBarManager tbm = bars.getToolBarManager(); tbm.add(new
     * Separator(DESIGN_VIEWER_SEPARATOR_2_ID));
     * 
     * expandAction = new ViewerExpandCollapseAction(true); expandAction.setId(EXPAND_ALL_ID);
     * tbm.add(expandAction);
     * 
     * collapseAction = new ViewerExpandCollapseAction(false);
     * collapseAction.setId(COLLAPSE_ALL_ID); tbm.add(collapseAction);
     */
  }

  public void setViewerSpecificContributionsEnabled(boolean enabled) {
    /*
     * if (expandAction != null) { expandAction.setEnabled(enabled);
     * xmlMenuExpandAction.setEnabled(enabled); }
     * 
     * if (collapseAction != null) { collapseAction.setEnabled(enabled);
     * xmlMenuCollapseAction.setEnabled(enabled); }
     */
  }

  public void setActiveEditor(IEditorPart targetEditor) {
    editorPart = targetEditor;

//		IStructuredModel model = getModelForEditorPart(targetEditor);
    /*
     * reloadGrammarAction.setModel(model);
     * toggleAction.setModelQuery(ModelQueryUtil.getModelQuery(model));
     * 
     * XMLTableTreeViewer tableTreeViewer = getTableTreeViewerForEditorPart(editorPart); if
     * (tableTreeViewer != null) { expandAction.setViewer(tableTreeViewer);
     * collapseAction.setViewer(tableTreeViewer);
     * 
     * xmlMenuExpandAction.setViewer(tableTreeViewer);
     * xmlMenuCollapseAction.setViewer(tableTreeViewer); }
     */
    ITextEditor textEditor = null;
    if (editorPart instanceof XMLMultiPageEditorPart) {
      IWorkbenchPartSite site = editorPart.getSite();
      if (site instanceof IEditorSite) {
        textEditor = ((XMLMultiPageEditorPart) editorPart).getTextEditor();
      }
    }
    actionBars.setGlobalActionHandler(ITextEditorActionConstants.UNDO,
        getAction(textEditor, ITextEditorActionConstants.UNDO));
    actionBars.setGlobalActionHandler(ITextEditorActionConstants.REDO,
        getAction(textEditor, ITextEditorActionConstants.REDO));

    // TODO... uncomment this and investigate NPE
    //
    // add the cut/copy/paste for text fields
    // ActionHandlerPlugin.connectPart(editorPart);
  }

  protected final IAction getAction(ITextEditor editor, String actionId) {
    return (editor == null ? null : editor.getAction(actionId));
  }

  /**
   * @deprecated - not to be used
   */
  protected IStructuredModel getModelForEditorPart(IEditorPart targetEditor) {
    IStructuredModel result = null;
    if (editorPart instanceof XMLMultiPageEditorPart) {
      StructuredTextEditor textEditor = ((XMLMultiPageEditorPart) targetEditor).getTextEditor();
      result = (textEditor != null) ? textEditor.getModel() : null;
    }
    return result;
  }

  /**
   * @deprecated - not to be used
   */
  protected XMLTableTreeViewer getTableTreeViewerForEditorPart(IEditorPart targetEditor) {
    XMLTableTreeViewer result = null;
    Object object = targetEditor.getAdapter(IDesignViewer.class);
    if (object instanceof XMLTableTreeViewer) {
      result = (XMLTableTreeViewer) object;
    }
    return result;
  }

  /**
	 * 
	 */
  public class ToggleEditModeAction extends Action {
    protected ImageDescriptor onImage = SharedXMLEditorPluginImageHelper.getImageDescriptor(SharedXMLEditorPluginImageHelper.IMG_ETOOL_CONSTRAINON);
    protected ImageDescriptor offImage = SharedXMLEditorPluginImageHelper.getImageDescriptor(SharedXMLEditorPluginImageHelper.IMG_ETOOL_CONSTRAINOFF);
    protected ModelQuery modelQuery;

    public ToggleEditModeAction() {
      setAppearanceForEditMode(ModelQuery.EDIT_MODE_CONSTRAINED_STRICT);
    }

    public void run() {
      if (modelQuery != null) {
        int newState = getNextState(modelQuery.getEditMode());
        modelQuery.setEditMode(newState);
        setAppearanceForEditMode(newState);
      }
    }

    public void setModelQuery(ModelQuery newModelQuery) {
      modelQuery = newModelQuery;
      if (modelQuery != null) {
        setAppearanceForEditMode(modelQuery.getEditMode());
      }
    }

    public void setAppearanceForEditMode(int editMode) {
      if (editMode == ModelQuery.EDIT_MODE_CONSTRAINED_STRICT) {
        setToolTipText(XMLEditorMessages.XMLTableTreeActionBarContributor_3);
        setText(XMLEditorMessages.XMLTableTreeActionBarContributor_4);
        setImageDescriptor(onImage);
      } else {
        setToolTipText(XMLEditorMessages.XMLTableTreeActionBarContributor_5);
        setText(XMLEditorMessages.XMLTableTreeActionBarContributor_6);
        setImageDescriptor(offImage);
      }
    }

    public int getNextState(int editMode) {
      int result = -1;
      if (editMode == ModelQuery.EDIT_MODE_CONSTRAINED_STRICT) {
        result = ModelQuery.EDIT_MODE_UNCONSTRAINED;
      } else {
        result = ModelQuery.EDIT_MODE_CONSTRAINED_STRICT;
      }
      return result;
    }
  }

  /**
	 * 
	 */
  public class ReloadGrammarAction extends Action {
    protected IStructuredModel model;

    public ReloadGrammarAction() {
      setDisabledImageDescriptor(SharedXMLEditorPluginImageHelper.getImageDescriptor(SharedXMLEditorPluginImageHelper.IMG_DTOOL_RLDGRMR));
      setImageDescriptor(SharedXMLEditorPluginImageHelper.getImageDescriptor(SharedXMLEditorPluginImageHelper.IMG_ETOOL_RLDGRMR));
      setToolTipText(XMLEditorMessages.XMLTableTreeActionBarContributor_7);
      setText(XMLEditorMessages.XMLTableTreeActionBarContributor_8);
    }

    public void setModel(IStructuredModel newModel) {
      this.model = newModel;
    }

    public void run() {
      if (model != null) {
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(model);
        Document document = ((IDOMModel) model).getDocument();
        if ((modelQuery != null) && (modelQuery.getCMDocumentManager() != null)) {
          modelQuery.getCMDocumentManager().getCMDocumentCache().clear();
          // TODO... need to figure out how to access the
          // DOMObserver via ModelQuery
          // ...why?
          CMDocumentLoader loader = new InferredGrammarBuildingCMDocumentLoader(document,
              modelQuery);
          loader.loadCMDocuments();
        }
      }
    }
  }

  /**
   * @see org.eclipse.ui.IEditorActionBarContributor#dispose()
   */
  public void dispose() {
    setActiveEditor(null);
  }
}
