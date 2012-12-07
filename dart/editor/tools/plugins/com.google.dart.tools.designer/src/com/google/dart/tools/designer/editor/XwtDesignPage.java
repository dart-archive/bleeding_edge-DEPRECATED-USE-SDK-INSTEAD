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

import com.google.dart.tools.designer.model.XmlObjectInfo;

import org.eclipse.wb.core.model.broadcast.BroadcastSupport;

/**
 * {@link XmlDesignPage} for Dart HTML.
 * 
 * @author scheglov_ke
 * @coverage XWT.editor
 */
public final class XwtDesignPage extends XmlDesignPage {
  @Override
  protected XmlObjectInfo parse() throws Exception {
    XmlObjectInfo root = new XmlObjectInfo() {
      {
        setBroadcastSupport(new BroadcastSupport());
      }
    };
    return root;
    // TODO(scheglov)
//    XwtParser parser = new XwtParser(m_file, m_document);
//    return parser.parse();
  }
}
