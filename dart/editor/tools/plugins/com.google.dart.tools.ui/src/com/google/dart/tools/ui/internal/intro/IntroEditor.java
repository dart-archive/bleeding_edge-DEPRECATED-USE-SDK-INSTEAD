/*
 * Copyright 2011 Google Inc.
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
package com.google.dart.tools.ui.internal.intro;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.EditorPart;

/**
 * A "fake" editor for showing intro content to first time users.
 */
public class IntroEditor extends EditorPart {
  public static final String ID = "com.google.dart.tools.ui.intro.editor";

  /*
   * TODO (pquitslund): string content should be externalized.
   */

  public static IEditorInput getInput() {
    return new IEditorInput() {

      @Override
      public boolean exists() {
        return false;
      }

      @Override
      public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        return null;
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return null;
      }

      @Override
      public String getName() {
        return "Welcome";
      }

      @Override
      public IPersistableElement getPersistable() {
        return null;
      }

      @Override
      public String getToolTipText() {
        return "Welcome to Dart!";
      }
    };
  }

  private static String bold(String str) {
    return "<span font=\"header\">" + str + "</span>";
  }

  private static String img(String imgName) {
    return " <img href=\"" + imgName + "\"/> ";
  }

  private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  public IntroEditor() {
  }

  @Override
  public void createPartControl(Composite parent) {
    createIntroContent(parent);
  }

  @Override
  public void dispose() {
    toolkit.dispose();
    super.dispose();
  }

  @Override
  public void doSave(IProgressMonitor monitor) {
    //no-op
  }

  @Override
  public void doSaveAs() {
    //no-op
  }

  @Override
  public void init(IEditorSite site, IEditorInput input) {
    setSite(site);
    setInput(input);
    setTitleToolTip(input.getToolTipText());
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  @Override
  public void setFocus() {
    //do nothing on focus gain
  }

  private Composite createIntroContent(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    TableWrapLayout layout = new TableWrapLayout();
    layout.verticalSpacing = 0;
    layout.horizontalSpacing = 0;
    layout.bottomMargin = 0;
    layout.topMargin = 20;
    layout.rightMargin = 0;
    layout.leftMargin = 20;
    composite.setLayout(layout);
    toolkit.adapt(composite);

    FormText formText = toolkit.createFormText(composite, true);
    formText.setLayoutData(GridDataFactory.fillDefaults().grab(true, true));

    StringBuffer buf = new StringBuffer();
    buf.append("<form>");
    buf.append("<p>");
    buf.append("<span color=\"header\" font=\"header\">" + "Getting started is easy!</span>");
    buf.append("</p>");
    buf.append("<p>" + bold("1. Click ") + img("new_lib_image") + bold("to create an application.")
        + "</p>");
    buf.append("<p>" + bold("2. Look around.  Click ") + img("run_image") + bold(" to run.")
        + "</p>");
    buf.append("<li style=\"text\" bindent=\"20\" indent=\"20\">"
        + "<span color=\"header\">(compiles to JavaScript and runs in Chrome)</span></li>");
    buf.append("<p>" + bold("3. Have fun.  Write awesome code.") + "</p>");
    buf.append("</form>");

    formText.setWhitespaceNormalized(true);
    TableWrapData twd_formText = new TableWrapData(TableWrapData.FILL);
    twd_formText.grabHorizontal = true;
    formText.setLayoutData(twd_formText);
    formText.setImage("new_lib_image",
        DartToolsPlugin.getImage("icons/full/dart16/library_new.png"));
    formText.setImage("run_image", DartToolsPlugin.getImage("icons/full/dart16/run_client.png"));

    formText.setColor("header", toolkit.getColors().getColor(IFormColors.TITLE));
    formText.setFont("header", JFaceResources.getHeaderFont());

    formText.setText(buf.toString(), true, false);
    return composite;
  }
}
