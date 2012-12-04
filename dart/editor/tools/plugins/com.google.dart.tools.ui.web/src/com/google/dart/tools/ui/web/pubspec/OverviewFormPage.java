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
import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * The forms page for the Pubspec Editor
 */
public class OverviewFormPage extends FormPage implements IModelListener {

  private DependenciesMasterBlock block;
  private Text nameText;
  private Text authorText;
  private Text versionText;
  private Text homepageText;
  private Text description;
  private SectionPart infoSectionPart;
  private FormToolkit toolkit;
  private IManagedForm form;

  private PubspecModel model;

  private boolean ignoreModify = false;

  public OverviewFormPage(FormEditor editor) {
    super(editor, "overview", "Overview");
    block = new DependenciesMasterBlock(this);
    model = ((PubspecEditor) this.getEditor()).getModel();
  }

  @Override
  public void modelChanged(Object[] objects, String type) {
    if (type.equals(IModelListener.REFRESH)) {
      ignoreModify = true;
    }
    updateInfoSection();
    ignoreModify = false;
  }

  @Override
  protected void createFormContent(final IManagedForm managedForm) {
    this.form = managedForm;
    final ScrolledForm scrolledForm = form.getForm();
    toolkit = form.getToolkit();
    scrolledForm.setText("Pubspec Details");
    scrolledForm.setImage(DartWebPlugin.getImage("pubspec.png"));
    toolkit.decorateFormHeading(scrolledForm.getForm());
    scrolledForm.getBody().setLayout(new GridLayout());

    Composite top = toolkit.createComposite(scrolledForm.getBody());
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.numColumns = 2;
    top.setLayout(layout);
    top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    createInfoSection(top, scrolledForm, toolkit);
    createExploreSection(top);
    block.createContent(form);
    model.addModelListener(this);

  }

  private void createExploreSection(Composite top) {
    Section section = toolkit.createSection(top, Section.TITLE_BAR);//| Section.DESCRIPTION);

    GridData sectionLayoutData = new GridData(SWT.FILL, SWT.TOP, false, false);
    sectionLayoutData.widthHint = 350;
    section.setLayoutData(sectionLayoutData);
    section.setText("Explore");
    Composite client = toolkit.createComposite(section);
    client.setLayout(new TableWrapLayout());
    section.setClient(client);

    Composite links = new Composite(client, SWT.NONE);

    GridLayoutFactory.fillDefaults().spacing(15, 5).applyTo(links);

    createExternalLink(
        links,
        "Show packages on pub.dartlang.org",
        "http://pub.dartlang.org/packages");
    createExternalLink(
        links,
        "View Pubspec documentation",
        "http://pub.dartlang.org/doc/pubspec.html");

  }

  private void createExternalLink(Composite client, String text, final String href) {

    Hyperlink link = toolkit.createHyperlink(client, text, SWT.NONE);
    link.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        ExternalBrowserUtil.openInExternalBrowser(href);
      }
    });

  }

  private void createInfoSection(Composite parent, final ScrolledForm scrolledForm,
      FormToolkit toolkit) {
    Section section = toolkit.createSection(parent, Section.TITLE_BAR);

    GridData sectionLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    sectionLayoutData.horizontalSpan = 1;
    section.setLayoutData(sectionLayoutData);
    section.setRedraw(true);

    section.setText("General Information");

    Composite client = toolkit.createComposite(section);
    GridLayoutFactory.swtDefaults().spacing(5, 5).numColumns(2).margins(0, 0).applyTo(client);

    section.setClient(client);
    toolkit.paintBordersFor(client);
    infoSectionPart = new SectionPart(section);
    form.addPart(infoSectionPart);

    toolkit.createLabel(client, "Name:");
    nameText = toolkit.createText(client, "", SWT.SINGLE);
    nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    nameText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setName(nameText.getText().trim());
        setTextDirty();
      }
    });

    toolkit.createLabel(client, "Author:");
    authorText = toolkit.createText(client, "", SWT.SINGLE);
    authorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(client, "Version:");
    versionText = toolkit.createText(client, "", SWT.SINGLE);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(client, "Homepage: ");
    homepageText = toolkit.createText(client, "", SWT.SINGLE);
    homepageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label label = toolkit.createLabel(client, "Description:");
    label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    description = toolkit.createText(client, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    toolkit.adapt(description, true, true);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd.heightHint = 50;
    description.setLayoutData(gd);

    updateInfoSection();

    authorText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setAuthor(authorText.getText().trim());
        setTextDirty();
      }
    });
    versionText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setVersion(versionText.getText().trim());
        setTextDirty();
      }
    });
    homepageText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setHomepage(homepageText.getText().trim());
        setTextDirty();
      }
    });
    description.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setDescription(description.getText().trim());
        setTextDirty();
      }
    });
  }

  private void setTextDirty() {
    if (!ignoreModify) {
      model.setDirty(true);
      infoSectionPart.markDirty();
    }
  }

  private void updateInfoSection() {
    nameText.setText(model.getName());
    versionText.setText(model.getVersion());
    description.setText(model.getDescription());
    homepageText.setText(model.getHomepage());
    authorText.setText(model.getAuthor());
  }

}
