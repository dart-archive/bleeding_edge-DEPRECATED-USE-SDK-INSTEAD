/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 */
public final class ProjectTemplateStore {

  private static final String KEY = "com.google.dart.tools.ui.config.custom_code_templates"; //$NON-NLS-1$

  public static boolean hasProjectSpecificTempates(IProject project) {
    String pref = new ProjectScope(project).getNode(DartUI.ID_PLUGIN).get(KEY, null);
    if (pref != null && pref.trim().length() > 0) {
      Reader input = new StringReader(pref);
      TemplateReaderWriter reader = new TemplateReaderWriter();
      TemplatePersistenceData[] datas;
      try {
        datas = reader.read(input);
        return datas.length > 0;
      } catch (IOException e) {
        // ignore
      }
    }
    return false;
  }

  private final TemplateStore fInstanceStore;

  private final TemplateStore fProjectStore;

  public ProjectTemplateStore(IProject project) {
    fInstanceStore = DartToolsPlugin.getDefault().getCodeTemplateStore();
    if (project == null) {
      fProjectStore = null;
    } else {
      final ScopedPreferenceStore projectSettings = new ScopedPreferenceStore(new ProjectScope(
          project), DartUI.ID_PLUGIN);
      fProjectStore = new TemplateStore(projectSettings, KEY) {
        /*
         * Make sure we keep the id of added code templates - add removes it in the usual add()
         * method
         */
        @Override
        public void add(TemplatePersistenceData data) {
          internalAdd(data);
        }

        @Override
        public void save() throws IOException {

          StringWriter output = new StringWriter();
          TemplateReaderWriter writer = new TemplateReaderWriter();
          writer.save(getTemplateData(false), output);

          projectSettings.setValue(KEY, output.toString());
          projectSettings.save();
        }
      };
    }
  }

  public Template findTemplateById(String id) {
    Template template = null;
    if (fProjectStore != null) {
      template = fProjectStore.findTemplateById(id);
    }
    if (template == null) {
      template = fInstanceStore.findTemplateById(id);
    }

    return template;
  }

  public TemplatePersistenceData[] getTemplateData() {
    if (fProjectStore != null) {
      return fProjectStore.getTemplateData(true);
    } else {
      return fInstanceStore.getTemplateData(true);
    }
  }

  public boolean isProjectSpecific(String id) {
    if (id == null) {
      return false;
    }

    if (fProjectStore == null) {
      return false;
    }

    return fProjectStore.findTemplateById(id) != null;
  }

  public void load() throws IOException {
    if (fProjectStore != null) {
      fProjectStore.load();

      Set<String> datas = new HashSet<String>();
      TemplatePersistenceData[] data = fProjectStore.getTemplateData(false);
      for (int i = 0; i < data.length; i++) {
        datas.add(data[i].getId());
      }

      data = fInstanceStore.getTemplateData(false);
      for (int i = 0; i < data.length; i++) {
        TemplatePersistenceData orig = data[i];
        if (!datas.contains(orig.getId())) {
          TemplatePersistenceData copy = new TemplatePersistenceData(new Template(
              orig.getTemplate()), orig.isEnabled(), orig.getId());
          fProjectStore.add(copy);
          copy.setDeleted(true);
        }
      }
    }
  }

  public void restoreDefaults() {
    if (fProjectStore == null) {
      fInstanceStore.restoreDefaults();
    } else {
      fProjectStore.restoreDefaults();
    }
  }

  public void revertChanges() throws IOException {
    if (fProjectStore != null) {
      // nothing to do
    } else {
      fInstanceStore.load();
    }
  }

  public void save() throws IOException {
    if (fProjectStore == null) {
      fInstanceStore.save();
    } else {
      fProjectStore.save();
    }
  }

  public void setProjectSpecific(String id, boolean projectSpecific) {
    Assert.isNotNull(fProjectStore);

    TemplatePersistenceData data = fProjectStore.getTemplateData(id);
    if (data == null) {
      return; // does not exist
    } else {
      data.setDeleted(!projectSpecific);
    }
  }
}
