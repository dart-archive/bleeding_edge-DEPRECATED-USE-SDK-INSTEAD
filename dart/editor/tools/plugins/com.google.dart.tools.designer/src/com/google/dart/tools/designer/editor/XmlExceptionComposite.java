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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.internal.core.editor.errors.ExceptionComposite;
import org.eclipse.wb.internal.core.editor.errors.report2.IReportEntry;
import org.eclipse.wb.internal.core.editor.errors.report2.StringFileReportEntry;
import org.eclipse.wb.internal.core.editor.errors.report2.ZipFileErrorReport;

/**
 * Implementation for XML.
 * 
 * @author mitin_aa
 * @coverage XML.editor
 */
public final class XmlExceptionComposite extends ExceptionComposite {
  private IFile m_file;
  private IDocument m_document;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public XmlExceptionComposite(Composite parent, int style) {
    super(parent, style);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // ExceptionComposite
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected ZipFileErrorReport getZipFileErrorReport() {
    IProject project = m_file.getProject();
    return new ZipFileErrorReport(getScreenshotImage(), project, getSourceFileReport(
        m_file,
        m_document));
  }

  @Override
  protected void doShowSource(int sourcePosition) {
    // TODO(scheglov)
//    SwitchAction.showSource(sourcePosition);
  }

  @Override
  protected void doRefresh() {
    // TODO(scheglov)
//    new RefreshAction().run();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Error Report related.
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the report info containing actual source of editing XML.
   */
  private static IReportEntry getSourceFileReport(IFile file, IDocument document) {
    try {
      return new StringFileReportEntry(file.getName(), document.get());
    } catch (Throwable e) {
      // ignore, just send nothing
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the {@link Throwable} to display with additional information which may be included into
   * problem report.
   * 
   * @param e the {@link Throwable} to display.
   * @param screenshot the {@link Image} of entire shell just before error. Can be <code>null</code>
   *          in case of parse error when no screenshot needed.
   * @param file the IFile instance of editing document.
   * @param document the currently editing document, may be modified.
   */
  public void setException(Throwable e, Image screenshot, IFile file, IDocument document) {
    m_file = file;
    m_document = document;
    setException0(e, screenshot);
  }
}
