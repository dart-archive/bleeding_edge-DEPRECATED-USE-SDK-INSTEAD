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

import com.google.dart.tools.core.pub.DependencyObject;
import com.google.dart.tools.core.pub.DependencyObject.Type;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;
import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
  private static String[] sourceList = {"git", "pub.dartlang.org"};
  private static String VERSION_CONTSTRAINTS_KEY = "versionConstraints";

  private DependencyObject input;

  private Text nameText;
  private Text versionText;
  private Text pathText;
  private Text gitrefText;
  private Combo sourceCombo;
  private Label pathLabel;
  private Label gitrefLabel;

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

    toolkit.createLabel(client, "Version: ");
    versionText = toolkit.createText(client, "", SWT.SINGLE); //$NON-NLS-1$
    gd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
    versionText.setLayoutData(gd);
    versionText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (input != null) {
          if (validateVersionConstriants(versionText.getText())) {
            input.setVersion(versionText.getText());
          }
          setTextDirty();
        }
      }
    });

    toolkit.createLabel(client, "");
    Label examplesLabel = toolkit.createLabel(client, "e.g. any, 1.0.0 ...");
    examplesLabel.setFont(DartWebPlugin.getPlugin().getItalicFont(examplesLabel.getFont()));
    StringBuffer buf = new StringBuffer();
    buf.append("<form>");
    buf.append("<p>");
    buf.append("<a href=\"http://pub.dartlang.org/doc/pubspec.html#version-constraints\">what are version constraints?</a>");
    buf.append("</p>");
    buf.append("</form>");

    FormText info = toolkit.createFormText(client, true);
    gd = new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1);
    info.setLayoutData(gd);
    info.setText(buf.toString(), true, true);
    info.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        ExternalBrowserUtil.openInExternalBrowser((String) e.getHref());
      }
    });

    toolkit.createLabel(client, "Source:");
    sourceCombo = new Combo(client, SWT.READ_ONLY | SWT.BORDER);
    toolkit.adapt(sourceCombo, true, false);
    sourceCombo.setItems(sourceList);
    sourceCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (sourceCombo.getSelectionIndex() == 0) {
          updateModelandSourceFields(true, Type.GIT);
        } else {
          updateModelandSourceFields(false, Type.HOSTED);
        }
        setTextDirty();
      }
    });

    toolkit.createLabel(client, "");
    pathLabel = toolkit.createLabel(client, "Git path:");
    pathText = toolkit.createText(client, "", SWT.SINGLE); //$NON-NLS-1$
    pathText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    pathText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (input != null) {
          input.setPath(pathText.getText());
          setTextDirty();
        }
      }
    });

    gitrefLabel = toolkit.createLabel(client, "Git ref:");
    gitrefText = toolkit.createText(client, "", SWT.SINGLE);
    gitrefText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    gitrefText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (input != null) {
          input.setGitRef(gitrefText.getText());
          setTextDirty();
        }
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
    if (input != null) {
      nameText.setText(input.getName() != null ? input.getName() : EMPTY_STRING);
      versionText.setText(input.getVersion() != null ? input.getVersion() : EMPTY_STRING);
      if (input.getType().equals(Type.GIT)) {
        sourceCombo.select(0);
        updateModelandSourceFields(true, Type.GIT);
        pathText.setText(input.getPath() != null ? input.getPath() : EMPTY_STRING);
        gitrefText.setText(input.getGitRef() != null ? input.getGitRef() : EMPTY_STRING);
      } else if (input.getType().equals(Type.HOSTED)) {
        sourceCombo.select(1);
        pathText.setText(EMPTY_STRING);
        gitrefText.setText(EMPTY_STRING);
        updateModelandSourceFields(false, Type.HOSTED);
      }
    }
  }

  private void updateModelandSourceFields(boolean value, Type type) {
    if (input != null) {
      input.setType(type);
    }
    pathLabel.setVisible(value);
    pathText.setVisible(value);
    gitrefLabel.setVisible(value);
    gitrefText.setVisible(value);
  }

  private boolean validateVersionConstriants(String version) {
    boolean isValid = true;

    if (!version.equals("any") && !version.isEmpty()) {
      String[] versions = version.split(" ");
      if (versions.length > 2) {
        isValid = false;
      } else {
        for (String ver : versions) {
          if (!PubYamlUtils.isValidVersionConstraint(ver)) {
            isValid = false;
          }
        }
      }
    }
    if (isValid) {
      getManagedForm().getMessageManager().removeMessage(VERSION_CONTSTRAINTS_KEY, versionText);
    } else {
      getManagedForm().getMessageManager().addMessage(
          VERSION_CONTSTRAINTS_KEY,
          "The version constriant does not have the correct format as in '1.0.0', '<1.5.0', \n'>=2.0.0 <3.0.0', or it contains invalid characters",
          null,
          IMessageProvider.ERROR,
          versionText);
    }
    return isValid;
  }

}
