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
package com.google.dart.tools.ui.web.pubspec;

import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * Details page for dependencies, shows all the details of the package
 */
public class DependencyDetailsPage extends AbstractFormPart implements IDetailsPage {

  private static String EMPTY_STRING = "";

  private DependencyObject input;

  private Text nameText;
  private Text versionText;
  private Text pathText;
  private Text gitrefText;
  private Button pubButton;
  private Button gitButton;

  private boolean ignoreModify = false;

  @Override
  public void createContents(Composite parent) {
    TableWrapLayout layout = new TableWrapLayout();
    parent.setLayout(layout);

    FormToolkit toolkit = getManagedForm().getToolkit();
    Section section = toolkit.createSection(parent, Section.DESCRIPTION | Section.TITLE_BAR);
    section.setText("Dependency Details");

    TableWrapData td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
    td.grabHorizontal = true;
    section.setLayoutData(td);
    Composite client = toolkit.createComposite(section);
    GridLayout glayout = new GridLayout();
    glayout.marginWidth = glayout.marginHeight = 0;
    glayout.numColumns = 3;
    client.setLayout(glayout);

    GridData gd;
    toolkit.createLabel(client, "Name:");
    nameText = toolkit.createText(client, "", SWT.SINGLE); //$NON-NLS-1$
    gd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
    nameText.setLayoutData(gd);
    nameText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (input != null) {
          input.setName(nameText.getText());
          setTextDirty();
        }
      }
    });

    toolkit.createLabel(client, "Source:");
    pubButton = toolkit.createButton(client, "pub.dartlang.org", SWT.RADIO);
    pubButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    pubButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (pubButton.getSelection()) {
          updateModelandSourceFields(false);
        } else {
          updateModelandSourceFields(true);
        }
        setTextDirty();
      }
    });
    toolkit.createLabel(client, "");
    gitButton = toolkit.createButton(client, "Git repository", SWT.RADIO);
    gitButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    gitButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (gitButton.getSelection()) {
          updateModelandSourceFields(true);
        } else {
          updateModelandSourceFields(false);
        }
        setTextDirty();
      }
    });
    toolkit.createLabel(client, "");
    toolkit.createLabel(client, "Path:");
    pathText = toolkit.createText(client, "", SWT.SINGLE); //$NON-NLS-1$
    gd = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
    pathText.setLayoutData(gd);
    pathText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (input != null) {
          input.setPath(pathText.getText());
          setTextDirty();
        }
      }
    });
    toolkit.createLabel(client, "");
    toolkit.createLabel(client, "Git ref:");
    gitrefText = toolkit.createText(client, "", SWT.SINGLE);
    gitrefText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
    gitrefText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (input != null) {
          input.setGitRef(gitrefText.getText());
          setTextDirty();
        }
      }
    });
    toolkit.createLabel(client, "Version: ");
    versionText = toolkit.createText(client, "", SWT.SINGLE); //$NON-NLS-1$
    gd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
    versionText.setLayoutData(gd);
    versionText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (input != null) {
          input.setVersion(versionText.getText());
          setTextDirty();
        }
      }
    });

    toolkit.createLabel(client, "");

    StringBuffer buf = new StringBuffer();
    buf.append("<form>");
    buf.append("<p>");
    buf.append("Examples: ");
    buf.append("</p>");
    buf.append("<li bindent=\"20\">any</li>");
    buf.append("<li bindent=\"20\">1.0.0.</li>");
    buf.append("<li bindent=\"20\">&gt;=2.0.0 &lt;3.0.0</li>");
    buf.append("<p>");
    buf.append("<a href=\"http://pub.dartlang.org/doc/pubspec.html#version-constraints\">what are version constraints?</a>");
    buf.append("</p>");
    buf.append("</form>");

    FormText info = toolkit.createFormText(client, true);
    gd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
    info.setLayoutData(gd);
    info.setText(buf.toString(), true, true);
    info.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        ExternalBrowserUtil.openInExternalBrowser((String) e.getHref());
      }
    });
    toolkit.paintBordersFor(section);
    section.setClient(client);
  }

  @Override
  public void refresh() {
    super.refresh();
    update();
  }

  @Override
  public void selectionChanged(IFormPart part, ISelection selection) {
    IStructuredSelection ssel = (IStructuredSelection) selection;
    if (ssel.size() == 1) {
      input = (DependencyObject) ssel.getFirstElement();
    } else {
      input = null;
    }
    ignoreModify = true;
    update();
    ignoreModify = false;

  }

  @Override
  public void setFocus() {
    nameText.setFocus();
  }

  private void setTextDirty() {
    if (!ignoreModify) {
      if (input.getModel() != null) {
        input.getModel().setDirty(true);
      }
      markDirty();
    }
  }

  private void update() {
    nameText.setText(input != null && input.getName() != null ? input.getName() : EMPTY_STRING);
    versionText.setText(input != null && input.getVersion() != null ? input.getVersion()
        : EMPTY_STRING);
    if (input != null && input.isGitDependency()) {
      gitButton.setSelection(true);
      pubButton.setSelection(false);
      updateModelandSourceFields(true);
      pathText.setText(input != null && input.getPath() != null ? input.getPath() : EMPTY_STRING);
      gitrefText.setText(input != null && input.getGitRef() != null ? input.getGitRef()
          : EMPTY_STRING);
    } else {
      pubButton.setSelection(true);
      gitButton.setSelection(false);
      pathText.setText(EMPTY_STRING);
      gitrefText.setText(EMPTY_STRING);
      updateModelandSourceFields(false);
    }
  }

  private void updateModelandSourceFields(boolean value) {
    if (input != null) {
      input.setGitDependency(value);
    }
    pathText.setEnabled(value);
    gitrefText.setEnabled(value);
  }

}
