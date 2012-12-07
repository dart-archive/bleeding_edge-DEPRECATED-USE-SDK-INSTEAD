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

import java.util.List;

/**
 * This interface is contributed via extension point and used by {@link AbstractXmlEditor} for
 * creating {@link IXmlEditorPage} pages for Designer multi page XML editor.
 * 
 * @author lobas_av
 * @coverage XML.editor
 */
public interface IXmlEditorPageFactory {
  /**
   * Create {@link IXmlEditorPage} pages for given {@link AbstractXmlEditor} editor.
   */
  void createPages(AbstractXmlEditor editor, List<IXmlEditorPage> pages);
}
