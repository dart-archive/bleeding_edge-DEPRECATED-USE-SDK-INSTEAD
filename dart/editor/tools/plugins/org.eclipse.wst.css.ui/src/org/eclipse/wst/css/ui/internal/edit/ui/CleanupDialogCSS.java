/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.edit.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.css.core.internal.cleanup.CSSCleanupStrategy;
import org.eclipse.wst.css.core.internal.cleanup.CSSCleanupStrategyImpl;
import org.eclipse.wst.css.ui.internal.CSSUIMessages;
import org.eclipse.wst.css.ui.internal.editor.IHelpContextIds;

public class CleanupDialogCSS extends Dialog implements SelectionListener {

  private boolean embeddedCSS;
  protected Button fRadioButtonIdentCaseAsis;
  protected Button fRadioButtonIdentCaseLower;
  protected Button fRadioButtonIdentCaseUpper;
  protected Button fRadioButtonPropNameCaseAsis;
  protected Button fRadioButtonPropNameCaseLower;
  protected Button fRadioButtonPropNameCaseUpper;
  protected Button fRadioButtonPropValueCaseAsis;
  protected Button fRadioButtonPropValueCaseLower;
  protected Button fRadioButtonPropValueCaseUpper;
  protected Button fRadioButtonSelectorTagCaseAsis;
  protected Button fRadioButtonSelectorTagCaseLower;
  protected Button fRadioButtonSelectorTagCaseUpper;
  protected Button fRadioButtonSelectorIdCaseAsis;
  protected Button fRadioButtonSelectorIdCaseLower;
  protected Button fRadioButtonSelectorIdCaseUpper;
  protected Button fRadioButtonSelectorClassCaseAsis;
  protected Button fRadioButtonSelectorClassCaseLower;
  protected Button fRadioButtonSelectorClassCaseUpper;
  protected Button fCheckBoxQuoteValues;
  protected Button fCheckBoxFormatSource;

  /**
   * CSSCleanupDialog constructor comment.
   * 
   * @param parentShell org.eclipse.swt.widgets.Shell
   */
  public CleanupDialogCSS(Shell parentShell) {
    super(parentShell);
  }

  /**
   * @return org.eclipse.swt.widgets.Control
   * @param parent org.eclipse.swt.widgets.Composite
   */
  public Control createDialogArea(Composite parent) {
    if (isEmbeddedCSS())
      getShell().setText(CSSUIMessages.CSS_Cleanup_UI_);
    else
      getShell().setText(CSSUIMessages.Cleanup_UI_);

    Composite panel = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.makeColumnsEqualWidth = true;
    panel.setLayout(layout);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(panel, IHelpContextIds.CSS_CLEANUP_HELPID);

    // Convert ident case
    // ACC: Group radio buttons together so associated label is read
    //		Label identCaseLabel = new Label(panel, SWT.NULL);
    //		identCaseLabel.setText(ResourceHandler.getString("Identifier_case__UI_"));
    // //$NON-NLS-1$ = "Identifier case:"
    //		Canvas identCase = new Canvas(panel, SWT.NULL);
    Group identCase = new Group(panel, SWT.NULL);
    identCase.setText(CSSUIMessages.Identifier_case__UI_);
    GridLayout hLayout = new GridLayout();
    hLayout.numColumns = 3;
    identCase.setLayout(hLayout);
    fRadioButtonIdentCaseAsis = new Button(identCase, SWT.RADIO);
    fRadioButtonIdentCaseAsis.setText(CSSUIMessages.As_is_UI_);
    fRadioButtonIdentCaseAsis.addSelectionListener(this);
    fRadioButtonIdentCaseLower = new Button(identCase, SWT.RADIO);
    fRadioButtonIdentCaseLower.setText(CSSUIMessages.Lower_UI_);
    fRadioButtonIdentCaseLower.addSelectionListener(this);
    fRadioButtonIdentCaseUpper = new Button(identCase, SWT.RADIO);
    fRadioButtonIdentCaseUpper.setText(CSSUIMessages.Upper_UI_);
    fRadioButtonIdentCaseUpper.addSelectionListener(this);

    // Convert property name case
    // ACC: Group radio buttons together so associated label is read
    //		Label propNameCaseLabel = new Label(panel, SWT.NULL);
    //		propNameCaseLabel.setText(ResourceHandler.getString("Property_name_case__UI_"));
    // //$NON-NLS-1$ = "Property name case:"
    //		Canvas propNameCase = new Canvas(panel, SWT.NULL);
    Group propNameCase = new Group(panel, SWT.NULL);
    propNameCase.setText(CSSUIMessages.Property_name_case__UI_);
    hLayout = new GridLayout();
    hLayout.numColumns = 3;
    propNameCase.setLayout(hLayout);
    fRadioButtonPropNameCaseAsis = new Button(propNameCase, SWT.RADIO);
    fRadioButtonPropNameCaseAsis.setText(CSSUIMessages.As_is_UI_);
    fRadioButtonPropNameCaseAsis.addSelectionListener(this);
    fRadioButtonPropNameCaseLower = new Button(propNameCase, SWT.RADIO);
    fRadioButtonPropNameCaseLower.setText(CSSUIMessages.Lower_UI_);
    fRadioButtonPropNameCaseLower.addSelectionListener(this);
    fRadioButtonPropNameCaseUpper = new Button(propNameCase, SWT.RADIO);
    fRadioButtonPropNameCaseUpper.setText(CSSUIMessages.Upper_UI_);
    fRadioButtonPropNameCaseUpper.addSelectionListener(this);

    // Convert property Value case
    // ACC: Group radio buttons together so associated label is read
    //		Label propValueCaseLabel = new Label(panel, SWT.NULL);
    //		propValueCaseLabel.setText(ResourceHandler.getString("Property_value_case__UI_"));
    // //$NON-NLS-1$ = "Property value case:"
    //		Canvas propValueCase = new Canvas(panel, SWT.NULL);
    Group propValueCase = new Group(panel, SWT.NULL);
    propValueCase.setText(CSSUIMessages.Property_value_case__UI_);
    hLayout = new GridLayout();
    hLayout.numColumns = 3;
    propValueCase.setLayout(hLayout);
    fRadioButtonPropValueCaseAsis = new Button(propValueCase, SWT.RADIO);
    fRadioButtonPropValueCaseAsis.setText(CSSUIMessages.As_is_UI_);
    fRadioButtonPropValueCaseAsis.addSelectionListener(this);
    fRadioButtonPropValueCaseLower = new Button(propValueCase, SWT.RADIO);
    fRadioButtonPropValueCaseLower.setText(CSSUIMessages.Lower_UI_);
    fRadioButtonPropValueCaseLower.addSelectionListener(this);
    fRadioButtonPropValueCaseUpper = new Button(propValueCase, SWT.RADIO);
    fRadioButtonPropValueCaseUpper.setText(CSSUIMessages.Upper_UI_);
    fRadioButtonPropValueCaseUpper.addSelectionListener(this);

    if (!isEmbeddedCSS()) {
      // Convert selector tag case
      // ACC: Group radio buttons together so associated label is read
      //			Label selectorTagCaseLabel = new Label(panel, SWT.NULL);
      //			selectorTagCaseLabel.setText(ResourceHandler.getString("Selector_tag_name_case__UI_"));
      // //$NON-NLS-1$ = "Selector tag name case:"
      //			Canvas selectorTagCase = new Canvas(panel, SWT.NULL);
      Group selectorTagCase = new Group(panel, SWT.NULL);
      selectorTagCase.setText(CSSUIMessages.Selector_tag_name_case__UI_);
      hLayout = new GridLayout();
      hLayout.numColumns = 3;
      selectorTagCase.setLayout(hLayout);
      fRadioButtonSelectorTagCaseAsis = new Button(selectorTagCase, SWT.RADIO);
      fRadioButtonSelectorTagCaseAsis.setText(CSSUIMessages.As_is_UI_);
      fRadioButtonSelectorTagCaseAsis.addSelectionListener(this);
      fRadioButtonSelectorTagCaseLower = new Button(selectorTagCase, SWT.RADIO);
      fRadioButtonSelectorTagCaseLower.setText(CSSUIMessages.Lower_UI_);
      fRadioButtonSelectorTagCaseLower.addSelectionListener(this);
      fRadioButtonSelectorTagCaseUpper = new Button(selectorTagCase, SWT.RADIO);
      fRadioButtonSelectorTagCaseUpper.setText(CSSUIMessages.Upper_UI_);
      fRadioButtonSelectorTagCaseUpper.addSelectionListener(this);

      Group selectorIdCase = new Group(panel, SWT.NULL);
      selectorIdCase.setText(CSSUIMessages.ID_Selector_Case__UI_);
      hLayout = new GridLayout();
      hLayout.numColumns = 3;
      selectorIdCase.setLayout(hLayout);
      fRadioButtonSelectorIdCaseAsis = new Button(selectorIdCase, SWT.RADIO);
      fRadioButtonSelectorIdCaseAsis.setText(CSSUIMessages.As_is_UI_);
      fRadioButtonSelectorIdCaseAsis.addSelectionListener(this);
      fRadioButtonSelectorIdCaseLower = new Button(selectorIdCase, SWT.RADIO);
      fRadioButtonSelectorIdCaseLower.setText(CSSUIMessages.Lower_UI_);
      fRadioButtonSelectorIdCaseLower.addSelectionListener(this);
      fRadioButtonSelectorIdCaseUpper = new Button(selectorIdCase, SWT.RADIO);
      fRadioButtonSelectorIdCaseUpper.setText(CSSUIMessages.Upper_UI_);
      fRadioButtonSelectorIdCaseUpper.addSelectionListener(this);

      Group selectorClassCase = new Group(panel, SWT.NULL);
      selectorClassCase.setText(CSSUIMessages.Class_Selector_Case__UI_);
      hLayout = new GridLayout();
      hLayout.numColumns = 3;
      selectorClassCase.setLayout(hLayout);
      fRadioButtonSelectorClassCaseAsis = new Button(selectorClassCase, SWT.RADIO);
      fRadioButtonSelectorClassCaseAsis.setText(CSSUIMessages.As_is_UI_);
      fRadioButtonSelectorClassCaseAsis.addSelectionListener(this);
      fRadioButtonSelectorClassCaseLower = new Button(selectorClassCase, SWT.RADIO);
      fRadioButtonSelectorClassCaseLower.setText(CSSUIMessages.Lower_UI_);
      fRadioButtonSelectorClassCaseLower.addSelectionListener(this);
      fRadioButtonSelectorClassCaseUpper = new Button(selectorClassCase, SWT.RADIO);
      fRadioButtonSelectorClassCaseUpper.setText(CSSUIMessages.Upper_UI_);
      fRadioButtonSelectorClassCaseUpper.addSelectionListener(this);
    }

    // Quote attribute values
    fCheckBoxQuoteValues = new Button(panel, SWT.CHECK);
    fCheckBoxQuoteValues.setText(CSSUIMessages.Quote_values_UI_);
    fCheckBoxQuoteValues.addSelectionListener(this);

    if (!isEmbeddedCSS()) {
      // Format source
      fCheckBoxFormatSource = new Button(panel, SWT.CHECK);
      fCheckBoxFormatSource.setText(CSSUIMessages.Format_source_UI_);
      fCheckBoxFormatSource.addSelectionListener(this);
    }

    setCleanupOptions();

    return panel;
  }

  /**
   * Insert the method's description here.
   * 
   * @return boolean
   */
  public boolean isEmbeddedCSS() {
    return embeddedCSS;
  }

  /**
	 *  
	 */
  protected void okPressed() {
    updateCleanupOptions();
    super.okPressed();
  }

  /**
	 *  
	 */
  protected void setCleanupOptions() {
    CSSCleanupStrategy stgy = CSSCleanupStrategyImpl.getInstance();

    if (fRadioButtonIdentCaseAsis != null) {
      if (stgy.getIdentCase() == CSSCleanupStrategy.UPPER)
        fRadioButtonIdentCaseUpper.setSelection(true);
      else if (stgy.getIdentCase() == CSSCleanupStrategy.LOWER)
        fRadioButtonIdentCaseLower.setSelection(true);
      else
        fRadioButtonIdentCaseAsis.setSelection(true);
    }

    if (fRadioButtonPropNameCaseAsis != null) {
      if (stgy.getPropNameCase() == CSSCleanupStrategy.UPPER)
        fRadioButtonPropNameCaseUpper.setSelection(true);
      else if (stgy.getPropNameCase() == CSSCleanupStrategy.LOWER)
        fRadioButtonPropNameCaseLower.setSelection(true);
      else
        fRadioButtonPropNameCaseAsis.setSelection(true);
    }

    if (fRadioButtonPropValueCaseAsis != null) {
      if (stgy.getPropValueCase() == CSSCleanupStrategy.UPPER)
        fRadioButtonPropValueCaseUpper.setSelection(true);
      else if (stgy.getPropValueCase() == CSSCleanupStrategy.LOWER)
        fRadioButtonPropValueCaseLower.setSelection(true);
      else
        fRadioButtonPropValueCaseAsis.setSelection(true);
    }

    if (fRadioButtonSelectorTagCaseAsis != null) {
      if (stgy.getSelectorTagCase() == CSSCleanupStrategy.UPPER)
        fRadioButtonSelectorTagCaseUpper.setSelection(true);
      else if (stgy.getSelectorTagCase() == CSSCleanupStrategy.LOWER)
        fRadioButtonSelectorTagCaseLower.setSelection(true);
      else
        fRadioButtonSelectorTagCaseAsis.setSelection(true);
    }

    if (fRadioButtonSelectorIdCaseAsis != null) {
      if (stgy.getIdSelectorCase() == CSSCleanupStrategy.UPPER)
        fRadioButtonSelectorIdCaseUpper.setSelection(true);
      else if (stgy.getIdSelectorCase() == CSSCleanupStrategy.LOWER)
        fRadioButtonSelectorIdCaseLower.setSelection(true);
      else
        fRadioButtonSelectorIdCaseAsis.setSelection(true);
    }

    if (fRadioButtonSelectorClassCaseAsis != null) {
      if (stgy.getClassSelectorCase() == CSSCleanupStrategy.UPPER)
        fRadioButtonSelectorClassCaseUpper.setSelection(true);
      else if (stgy.getClassSelectorCase() == CSSCleanupStrategy.LOWER)
        fRadioButtonSelectorClassCaseLower.setSelection(true);
      else
        fRadioButtonSelectorClassCaseAsis.setSelection(true);
    }

    if (fCheckBoxQuoteValues != null)
      fCheckBoxQuoteValues.setSelection(stgy.isQuoteValues());

    if (fCheckBoxFormatSource != null)
      fCheckBoxFormatSource.setSelection(stgy.isFormatSource());

  }

  /**
   * Insert the method's description here.
   * 
   * @param newEmbeddedCSS boolean
   */
  public void setEmbeddedCSS(boolean newEmbeddedCSS) {
    embeddedCSS = newEmbeddedCSS;
  }

  /**
	 *  
	 */
  protected void updateCleanupOptions() {
    CSSCleanupStrategy stgy = CSSCleanupStrategyImpl.getInstance();

    if (fRadioButtonIdentCaseAsis != null) {
      if (fRadioButtonIdentCaseUpper.getSelection())
        stgy.setIdentCase(CSSCleanupStrategy.UPPER);
      else if (fRadioButtonIdentCaseLower.getSelection())
        stgy.setIdentCase(CSSCleanupStrategy.LOWER);
      else
        stgy.setIdentCase(CSSCleanupStrategy.ASIS);
    }

    if (fRadioButtonPropNameCaseAsis != null) {
      if (fRadioButtonPropNameCaseUpper.getSelection())
        stgy.setPropNameCase(CSSCleanupStrategy.UPPER);
      else if (fRadioButtonPropNameCaseLower.getSelection())
        stgy.setPropNameCase(CSSCleanupStrategy.LOWER);
      else
        stgy.setPropNameCase(CSSCleanupStrategy.ASIS);
    }

    if (fRadioButtonPropValueCaseAsis != null) {
      if (fRadioButtonPropValueCaseUpper.getSelection())
        stgy.setPropValueCase(CSSCleanupStrategy.UPPER);
      else if (fRadioButtonPropValueCaseLower.getSelection())
        stgy.setPropValueCase(CSSCleanupStrategy.LOWER);
      else
        stgy.setPropValueCase(CSSCleanupStrategy.ASIS);
    }

    if (fRadioButtonSelectorTagCaseAsis != null) {
      if (fRadioButtonSelectorTagCaseUpper.getSelection())
        stgy.setSelectorTagCase(CSSCleanupStrategy.UPPER);
      else if (fRadioButtonSelectorTagCaseLower.getSelection())
        stgy.setSelectorTagCase(CSSCleanupStrategy.LOWER);
      else
        stgy.setSelectorTagCase(CSSCleanupStrategy.ASIS);
    }

    if (fRadioButtonSelectorIdCaseAsis != null) {
      if (fRadioButtonSelectorIdCaseUpper.getSelection())
        stgy.setIdSelectorCase(CSSCleanupStrategy.UPPER);
      else if (fRadioButtonSelectorIdCaseLower.getSelection())
        stgy.setIdSelectorCase(CSSCleanupStrategy.LOWER);
      else
        stgy.setIdSelectorCase(CSSCleanupStrategy.ASIS);
    }

    if (fRadioButtonSelectorClassCaseAsis != null) {
      if (fRadioButtonSelectorClassCaseUpper.getSelection())
        stgy.setClassSelectorCase(CSSCleanupStrategy.UPPER);
      else if (fRadioButtonSelectorClassCaseLower.getSelection())
        stgy.setClassSelectorCase(CSSCleanupStrategy.LOWER);
      else
        stgy.setClassSelectorCase(CSSCleanupStrategy.ASIS);
    }

    if (fCheckBoxQuoteValues != null)
      stgy.setQuoteValues(fCheckBoxQuoteValues.getSelection());

    if (fCheckBoxFormatSource != null)
      stgy.setFormatSource(fCheckBoxFormatSource.getSelection());

    // save these values to preferences
    ((CSSCleanupStrategyImpl) stgy).saveOptions();
  }

  public void widgetSelected(SelectionEvent e) {
    boolean okEnabled = fCheckBoxFormatSource.getSelection()
        || fCheckBoxQuoteValues.getSelection()
        || ((fRadioButtonIdentCaseLower != null && fRadioButtonIdentCaseLower.getSelection()) || (fRadioButtonIdentCaseUpper != null && fRadioButtonIdentCaseUpper.getSelection()))
        || ((fRadioButtonPropNameCaseLower != null && fRadioButtonPropNameCaseLower.getSelection()) || (fRadioButtonPropNameCaseUpper != null && fRadioButtonPropNameCaseUpper.getSelection()))
        || ((fRadioButtonPropValueCaseLower != null && fRadioButtonPropValueCaseLower.getSelection()) || (fRadioButtonPropValueCaseUpper != null && fRadioButtonPropValueCaseUpper.getSelection()))
        || ((fRadioButtonSelectorTagCaseLower != null && fRadioButtonSelectorTagCaseLower.getSelection()) || (fRadioButtonSelectorTagCaseUpper != null && fRadioButtonSelectorTagCaseUpper.getSelection()))
        || ((fRadioButtonSelectorIdCaseLower != null && fRadioButtonSelectorIdCaseLower.getSelection()) || (fRadioButtonSelectorIdCaseUpper != null && fRadioButtonSelectorIdCaseUpper.getSelection()))
        || ((fRadioButtonSelectorClassCaseLower != null && fRadioButtonSelectorClassCaseLower.getSelection()) || (fRadioButtonSelectorClassCaseUpper != null && fRadioButtonSelectorClassCaseUpper.getSelection()));
    getButton(OK).setEnabled(okEnabled);

  }

  public void widgetDefaultSelected(SelectionEvent e) {
    widgetSelected(e);

  }
}
