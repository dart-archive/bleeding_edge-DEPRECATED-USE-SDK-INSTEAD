/*******************************************************************************
 * Copyright (c) 2010 Standards for Technology in Automotive Retail and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver (STAR) - initial API and
 * implementation David Carver (Intalio) - generalize the implementation for contribution to eclipse
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.views.annotations;

import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.taginfo.MarkupTagInfoProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.w3c.dom.Node;

/**
 * Provides a view similar to the JavaDoc view that will display any documentation that resides in
 * the content model for an XML document.
 * 
 * @author dcarver
 */
public class XMLAnnotationsView extends ViewPart implements ISelectionListener {

  public static final String ID = "org.eclipse.wst.xml.ui.view.annotations.XMLAnnotationsView"; //$NON-NLS-1$

  private StyledText styledtext;

  private String xmlDoc = XMLUIMessages.Documentation_view_default_msg;

  private HTMLTextPresenter presenter;

  private final TextPresentation presentation = new TextPresentation();

  private IStructuredSelection currentSelection;
  private CMNode cmNode;

  public XMLAnnotationsView() {
  }

  public void init(IViewSite site) throws PartInitException {
    super.init(site);
    getSite().getPage().addPostSelectionListener(this);
  }

  /**
   * Create contents of the view part.
   * 
   * @param parent
   */
  public void createPartControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new FillLayout());
    presenter = new HTMLTextPresenter(false);
    createStyledTextWidget(container);
    doStyledTextInput();
  }

  private void createStyledTextWidget(Composite container) {
    styledtext = new StyledText(container, SWT.V_SCROLL | SWT.H_SCROLL);
    styledtext.setBackground(getColor(SWT.COLOR_INFO_BACKGROUND));
    styledtext.setEditable(false);
    styledtext.setBounds(container.getBounds());
    styledtext.addControlListener(new ControlAdapter() {
      /*
       * @see org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse
       * .swt.events.ControlEvent)
       */
      public void controlResized(ControlEvent e) {
        doStyledTextInput();
      }
    });
  }

  private void doStyledTextInput() {
    presentation.clear();
    Rectangle size = styledtext.getClientArea();
    int width = size.width;
    int height = size.height;
    if (width == 0) {
      width = 200;
    }

    if (height == 0) {
      height = 400;
    }

    String msg = presenter.updatePresentation(styledtext, xmlDoc, presentation, width, height);
    if (msg != null) {
      styledtext.setText(msg);
      TextPresentation.applyTextPresentation(presentation, styledtext);
    }
  }

  public void setFocus() {

  }

  public void dispose() {
    styledtext = null;
    getSite().getPage().removePostSelectionListener(this);
    cmNode = null;
    super.dispose();
  }

  public void selectionChanged(IWorkbenchPart part, ISelection selection) {

    IEditorPart edPart = getSite().getPage().getActiveEditor();
    if (part.equals(edPart)) {
      if (selection instanceof IStructuredSelection) {
        currentSelection = (IStructuredSelection) selection;
        if (!selection.isEmpty() && (currentSelection.getFirstElement() instanceof Node)) {
          Node node = (Node) currentSelection.getFirstElement();
          ModelQuery mq = ModelQueryUtil.getModelQuery(node.getOwnerDocument());
          if (mq != null) {
            cmNode = mq.getCMNode(node);
            MarkupTagInfoProvider tagInfo = new MarkupTagInfoProvider();
            xmlDoc = tagInfo.getInfo(cmNode);
          } else {
            xmlDoc = XMLUIMessages.Documentation_view_default_msg;
          }
          doStyledTextInput();
        }
      }
    }
  }

  private Color getColor(int colorID) {
    Display display = Display.getCurrent();
    return display.getSystemColor(colorID);
  }

}
