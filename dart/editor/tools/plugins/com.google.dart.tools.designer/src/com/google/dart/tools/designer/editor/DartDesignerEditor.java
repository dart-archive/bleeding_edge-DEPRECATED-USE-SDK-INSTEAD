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

public class DartDesignerEditor extends AbstractXmlEditor {

  @Override
  protected XmlDesignPage createDesignPage() {
    return new XwtDesignPage();
  }

//  private StyledText textWidget;
//  private ImageCanvas imageCanvas;
//  private IFile file;
//
//  @Override
//  public void createPartControl(Composite parent) {
//    GridLayoutFactory.create(parent).noMargins().columns(2).equalColumns();
//    {
//      textWidget = new StyledText(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
//      GridDataFactory.create(textWidget).grab().fill();
//      textWidget.addModifyListener(new ModifyListener() {
//        @Override
//        public void modifyText(ModifyEvent e) {
//          renderHtml();
//        }
//      });
//    }
//    {
//      imageCanvas = new ImageCanvas(parent, SWT.NONE);
//      GridDataFactory.create(imageCanvas).grab().fill();
//    }
//    if (file != null) {
//      try {
//        String content = IOUtils2.readString(file);
//        textWidget.setText(content);
//      } catch (Throwable e) {
//      }
//    }
//    renderHtml();
//  }
//
//  @Override
//  public void doSave(IProgressMonitor monitor) {
//  }
//
//  @Override
//  public void doSaveAs() {
//  }
//
//  @Override
//  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
//    setSite(site);
//    setInput(input);
//    if (input instanceof IFileEditorInput) {
//      file = ((IFileEditorInput) input).getFile();
//    }
//  }
//
//  @Override
//  public boolean isDirty() {
//    return false;
//  }
//
//  @Override
//  public boolean isSaveAsAllowed() {
//    return false;
//  }
//
//  @Override
//  public void setFocus() {
//  }
//
//  private void renderHtml() {
//    String content = textWidget.getText();
//    Image image = HtmlRenderHelper.renderImage(content);
//    imageCanvas.setImage(image);
//  }

}
