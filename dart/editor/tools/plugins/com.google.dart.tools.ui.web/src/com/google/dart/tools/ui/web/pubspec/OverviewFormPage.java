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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.generator.DartIdentifierUtil;
import com.google.dart.tools.core.pub.IModelListener;
import com.google.dart.tools.core.pub.PubspecModel;
import com.google.dart.tools.core.pub.RunPubJob;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;
import com.google.dart.tools.ui.actions.RunPubAction;
import com.google.dart.tools.ui.actions.RunPublishAction;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;
import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * The forms page for the Pubspec Editor
 */
public class OverviewFormPage extends FormPage implements IModelListener {

  private static String NAME_MESSAGE_KEY = "nameMessage";
  private static String VERSION_MESSAGE_KEY = "versionMessage";
  private static String SDK_VERSION_MESSAGE_KEY = "sdkVersionMessage";

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
  private Text sdkVersionText;

  private boolean editable;

  private Text documentationText;

  public OverviewFormPage(FormEditor editor) {
    super(editor, "overview", "Overview");
    editable = ((PubspecEditor) editor).isEditable();
    block = new DependenciesMasterBlock(this, editable);
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
  }

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
    Composite bottom = toolkit.createComposite(scrolledForm.getBody());
    bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    createInfoSection(top, scrolledForm, toolkit);
    Composite right = toolkit.createComposite(top);
    GridLayoutFactory.fillDefaults().applyTo(right);
    GridData griData = new GridData(SWT.FILL, SWT.TOP, false, false);
    griData.widthHint = 350;
    right.setLayoutData(griData);
    if (editable) {
      createActionsSection(right);
    }
    createExploreSection(right);
    block.createContent(form, bottom);
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

  private void createActionsSection(Composite composite) {
    Section section = toolkit.createSection(composite, Section.TITLE_BAR);
    GridData sectionLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    section.setLayoutData(sectionLayoutData);
    section.setText("Pub Actions");
    Composite client = toolkit.createComposite(section);
    client.setLayout(new TableWrapLayout());
    section.setClient(client);

    Composite links = new Composite(client, SWT.NONE);

    GridLayoutFactory.fillDefaults().spacing(15, 5).applyTo(links);

    ImageHyperlink saveActionText = toolkit.createImageHyperlink(links, SWT.NONE);
    saveActionText.setText("Run pub get");
    saveActionText.setImage(DartWebPlugin.getImage("pubspec.png"));
    saveActionText.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        if (getEditor().isDirty()) {
          getEditor().doSave(new NullProgressMonitor());
          if (!DartCore.getPlugin().isAutoRunPubEnabled()) {
            runPub();
          }
        } else {
          runPub();
        }
      }
    });

    ImageHyperlink deployActionText = toolkit.createImageHyperlink(links, SWT.NONE);
    deployActionText.setText("Run pub build");
    deployActionText.setImage(DartWebPlugin.getImage("package_obj.gif"));
    deployActionText.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        RunPubAction pubAction = RunPublishAction.createPubDeployAction(getSite().getWorkbenchWindow());
        pubAction.run();
      }
    });

    ImageHyperlink publishActionText = toolkit.createImageHyperlink(links, SWT.NONE);
    publishActionText.setText("Publish on pub.dartlang.org...");
    publishActionText.setImage(DartWebPlugin.getImage("export.gif"));
    publishActionText.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        RunPublishAction pubAction = RunPublishAction.createPubPublishAction(getSite().getWorkbenchWindow());
        pubAction.run();
      }
    });

  }

  private void createExploreSection(Composite top) {
    Section section = toolkit.createSection(top, Section.TITLE_BAR);

    GridData sectionLayoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
    section.setLayoutData(sectionLayoutData);
    section.setText("Explore");
    Composite client = toolkit.createComposite(section);
    client.setLayout(new TableWrapLayout());
    section.setClient(client);

    Composite links = new Composite(client, SWT.NONE);

    GridLayoutFactory.fillDefaults().spacing(15, 5).applyTo(links);

    Hyperlink link = toolkit.createHyperlink(client, "Show packages on pub.dartlang.org", SWT.NONE);
    link.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        try {
          getSite().getPage().showView("com.google.dart.tools.ui.view.packages");
        } catch (PartInitException exception) {
          DartWebPlugin.logError(exception);
        }
      }
    });
    createExternalLink(
        links,
        "View Pubspec documentation",
        "http://pub.dartlang.org/doc/pubspec.html");
    createExternalLink(links, "View Semantic versioning documentation", "http://semver.org/");

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

    GridData sectionLayoutData = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
    sectionLayoutData.horizontalSpan = 1;
    section.setLayoutData(sectionLayoutData);
    section.setRedraw(true);

    section.setText("General Information");

    Composite client = toolkit.createComposite(section);
    GridLayoutFactory.swtDefaults().spacing(5, 5).numColumns(2).margins(0, 0).applyTo(client);

    section.setClient(client);
    infoSectionPart = new SectionPart(section);
    form.addPart(infoSectionPart);

    Label nameLabel = toolkit.createLabel(client, "Name:");
    nameLabel.setToolTipText("A unique name to identify this package. The name should be a valid Dart identifier.");
    nameText = toolkit.createText(client, "", SWT.SINGLE | SWT.BORDER);
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
    authorText = toolkit.createText(client, "", SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.SCROLL_LINE);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).hint(200, SWT.DEFAULT).applyTo(
        authorText);
    authorText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setAuthor(authorText.getText().trim());
        setTextDirty();
      }
    });
    authorText.addTraverseListener(new TraverseListener() {
      @Override
      public void keyTraversed(TraverseEvent e) {
        if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
          e.doit = true;
        }
      }
    });

    Label versionLabel = toolkit.createLabel(client, "Version:");
    versionLabel.setToolTipText("A version number is three numbers separated by dots, like 0.2.43. "
        + "It can also have a build (+hotfix.oopsie) or pre-release (-alpha.12) suffix.");
    versionText = toolkit.createText(client, "", SWT.SINGLE | SWT.BORDER);
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
    homepageText = toolkit.createText(client, "", SWT.SINGLE | SWT.BORDER);
    homepageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    homepageText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setHomepage(homepageText.getText().trim());
        setTextDirty();
      }
    });

    Label documentationLabel = toolkit.createLabel(client, "Documentation: ");
    documentationLabel.setToolTipText("URL for the site that hosts documentation separate from the main homepage for this package.");
    documentationText = toolkit.createText(client, "", SWT.SINGLE | SWT.BORDER);
    documentationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    documentationText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setDocumentation(documentationText.getText().trim());
        setTextDirty();
      }
    });

    Label sdkVersionLabel = toolkit.createLabel(client, "SDK version:");
    sdkVersionLabel.setToolTipText("Set SDK version contraints for this package");
    sdkVersionText = toolkit.createText(client, "", SWT.SINGLE | SWT.BORDER);
    sdkVersionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    sdkVersionText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (validateVersionConstriants(sdkVersionText.getText().trim())) {
          model.setSdkVersion(sdkVersionText.getText().trim());
        }
        setTextDirty();
      }
    });

    Label descriptionLabel = toolkit.createLabel(client, "Description:");
    descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    descriptionLabel.setToolTipText("A description about this package");
    description = toolkit.createText(client, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
    toolkit.adapt(description, true, true);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd.widthHint = 200;
    gd.heightHint = 50;
    description.setLayoutData(gd);
    description.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        model.setDescription(description.getText().trim());
        setTextDirty();
      }
    });
    ignoreModify = true;
    updateInfoSection();
    if (model != null && model.isErrorOnParse()) {
      MessageDialog.openError(
          null,
          "Pubspec Editor",
          "Looks like the pubspec.yaml has invalid syntax or is corrupted.\nSwitch to the Source tab to fix.");
    }

    infoSectionPart.getSection().setEnabled(editable);
  }

  private void runPub() {
    IEditorInput input = getEditorInput();
    if (input instanceof IFileEditorInput) {
      IFile file = ((IFileEditorInput) input).getFile();
      if (file != null) {
        new RunPubJob(file.getParent(), RunPubJob.INSTALL_COMMAND, false).schedule();
      }
    }
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
      sdkVersionText.setText(model.getSdkVersion());
      documentationText.setText(model.getDocumentation());
    }
    if (ignoreModify) {
      ignoreModify = false;
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

  private boolean validateVersionConstriants(String version) {
    boolean isValid = PubYamlUtils.isValidVersionConstraintString(version);

    if (isValid) {
      getManagedForm().getMessageManager().removeMessage(SDK_VERSION_MESSAGE_KEY, sdkVersionText);
    } else {
      getManagedForm().getMessageManager().addMessage(
          SDK_VERSION_MESSAGE_KEY,
          "The SDK version constriant does not have the correct format as in '1.0.0', '<1.5.0', \n'>=2.0.0 <3.0.0', or it contains invalid characters",
          null,
          IMessageProvider.ERROR,
          sdkVersionText);
    }
    return isValid;
  }

}
