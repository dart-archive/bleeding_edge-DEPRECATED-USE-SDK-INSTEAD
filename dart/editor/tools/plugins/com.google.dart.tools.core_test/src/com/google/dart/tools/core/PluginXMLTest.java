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
package com.google.dart.tools.core;

import junit.framework.TestCase;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.team.core.Team;

public class PluginXMLTest extends TestCase {
  public void test_pluginxml_builder() {
    assertNotNull(Platform.getExtensionRegistry().getExtension(
        ResourcesPlugin.PI_RESOURCES,
        ResourcesPlugin.PT_BUILDERS,
        DartCore.DART_BUILDER_ID));
  }

  public void test_pluginxml_contentType_sourceFile() {
    IContentTypeManager manager = Platform.getContentTypeManager();
    IContentType type = manager.getContentType(DartCore.DART_SOURCE_CONTENT_TYPE);
    assertNotNull(type);
  }

  public void test_pluginxml_fileType_dart() {
    assertEquals(Team.TEXT, Team.getFileContentManager().getTypeForExtension("dart"));
  }

  public void test_pluginxml_projectNature() {
    assertNotNull(ResourcesPlugin.getWorkspace().getNatureDescriptor(DartCore.DART_PROJECT_NATURE));
  }
}
