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

import com.google.dart.tools.core.generator.DartIdentifierUtil;
import com.google.dart.tools.core.pub.IModelListener;
import com.google.dart.tools.core.pub.PubspecModel;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;
import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
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

  private static String NAME_MESSAGE_KEY = "nameMessage";
  private static String VERSION_MESSAGE_KEY = "versionMessage";

  private Control lastFocusControl;

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
    lastFocusControl = null;
  }

  /**
   * Add focus listeners to the specified composite and its children that track the last control to
   * have focus before a page change or the editor lost focus
   */
  public void addLastFocusListeners(Composite composite) {
    Control[] controls = composite.getChildren();
    for (int i = 0; i < controls.length; i++) {
      Control control = controls[i];
      // Add a focus listener if the control is any one of the below types
      if ((control instanceof Text) || (control instanceof Button) || (control instanceof Combo)
          || (control instanceof Table) || (control instanceof Hyperlink)
          || (control instanceof List)) {
        addLastFocusListener(control);
      }
      if (control instanceof Composite) {
        // Recursively add focus listeners to this composites children
        addLastFocusListeners((Composite) control);
      }
    }
  }

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    // Dynamically add focus listeners to all the forms children in order
    // to track the last focus control
    IManagedForm managedForm = getManagedForm();
    if (managedForm != null) {
      addLastFocusListeners(managedForm.getForm());
    }
  }

  public Control getLastFocusControl() {
    return lastFocusControl;
  }

  @Override
  public void modelChanged(Object[] objects, String type) {
    if (type.equals(IModelListener.REFRESH)) {
      ignoreModify = true;
    }
    updateInfoSection();
    ignoreModify = false;
  }

  /**
   * @param control
   */
  public void setLastFocusControl(Control control) {
    lastFocusControl = control;
  }

  /**
   * Set the focus on the last control to have focus before a page change or the editor lost focus.
   */
  public void updateFormSelection() {
    if ((lastFocusControl != null) && (lastFocusControl.isDisposed() == false)) {
      Control lastControl = lastFocusControl;
      // Set focus on the control
      lastControl.forceFocus();
      // If the control is a Text widget, select its contents
      if (lastControl instanceof Text) {
        Text text = (Text) lastControl;
        text.setSelection(0, text.getText().length());
      }
    } else {
      if (model.getDependecies().length > 0) {
        block.getViewer().setSelection(new StructuredSelection(model.getDependecies()[0]));
        block.getViewer().getTable().forceFocus();
      }
      setFocus();
    }
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

  /**
   * Add a focus listener to the specified control that tracks the last control to have focus on
   * this page. When focus is gained by this control, it registers itself as the last control to
   * have focus. The last control to have focus is stored in order to be restored after a page
   * change or editor loses focus.
   */
  private void addLastFocusListener(final Control control) {
    control.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        // NO-OP
      }

      @Override
      public void focusLost(FocusEvent e) {
        lastFocusControl = control;
      }
    });
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
    createExternalLink(links, "View Semantic versioning information", "http://semver.org/");

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

    Label nameLabel = toolkit.createLabel(client, "Name:");
    nameLabel.setToolTipText("A unique name to identify this package. The name should be a valid Dart identifier.");
    nameText = toolkit.createText(client, "", SWT.SINGLE);
    nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    nameText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (validateName(nameText.getText().trim())) {
          model.setName(nameText.getText().trim());
        }
        setTextDirty();
      }
    });

    Label authorLabel = toolkit.createLabel(client, "Author:");
    authorLabel.setToolTipText("Name(s) of the author(s) of this package. Email address can also be included.");
    authorText = toolkit.createText(client, "", SWT.SINGLE);
    authorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    authorText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setAuthor(authorText.getText().trim());
        setTextDirty();
      }
    });

    Label versionLabel = toolkit.createLabel(client, "Version:");
    versionLabel.setToolTipText("A version number is three numbers separated by dots, like 0.2.43. "
        + "It can also have a build (+hotfix.oopsie) or pre-release (-alpha.12) suffix.");
    versionText = toolkit.createText(client, "", SWT.SINGLE);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    versionText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (validateVersion(versionText.getText().trim())) {
          model.setVersion(versionText.getText().trim());
        }
        setTextDirty();
      }
    });

    Label homepageLabel = toolkit.createLabel(client, "Homepage: ");
    homepageLabel.setToolTipText("The homepage is the URL pointing to the website for this package.");
    homepageText = toolkit.createText(client, "", SWT.SINGLE);
    homepageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    homepageText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setHomepage(homepageText.getText().trim());
        setTextDirty();
      }
    });

    Label descriptionLabel = toolkit.createLabel(client, "Description:");
    descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    descriptionLabel.setToolTipText("A description about this package");
    description = toolkit.createText(client, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    toolkit.adapt(description, true, true);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd.heightHint = 50;
    description.setLayoutData(gd);
    description.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setDescription(description.getText().trim());
        setTextDirty();
      }
    });

    updateInfoSection();

  }

  private void setTextDirty() {
    if (!ignoreModify) {
      model.setDirty(true);
      infoSectionPart.markDirty();
    }
  }

  private void updateInfoSection() {
    if (model != null) {
      nameText.setText(model.getName());
      versionText.setText(model.getVersion());
      description.setText(model.getDescription());
      homepageText.setText(model.getHomepage());
      authorText.setText(model.getAuthor());
    }
  }

  private boolean validateName(String name) {
    IStatus status = DartIdentifierUtil.validateIdentifier(name);
    if (status == Status.OK_STATUS) {
      form.getMessageManager().removeMessage(NAME_MESSAGE_KEY, nameText);
      return true;
    }
    if (status.getSeverity() == Status.ERROR) {
      form.getMessageManager().addMessage(
          NAME_MESSAGE_KEY,
          "The name must be all lowercase, start with an alphabetic character, '_' or '$' and include only [a-z0-9_].",
          null,
          IMessageProvider.ERROR,
          nameText);
    }

    return false;
  }

  private boolean validateVersion(String version) {
    if (version.isEmpty() || version.matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION)) {
      form.getMessageManager().removeMessage(VERSION_MESSAGE_KEY, versionText);
      return true;
    }
    form.getMessageManager().addMessage(
        VERSION_MESSAGE_KEY,
        "The specified version does not have the correct format (major.minor.patch), or contains invalid characters.",
        null,
        IMessageProvider.ERROR,
        versionText);
    return false;
  }
}
