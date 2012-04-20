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
package com.google.dart.tools.ui.internal.intro;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * A "fake" editor for showing intro content to first time users.
 */
public class IntroEditor extends EditorPart {
  public static final String ID = "com.google.dart.tools.ui.intro.editor"; //$NON-NLS-1$

  public static final IEditorInput INPUT = new IEditorInput() {

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
      return IntroMessages.IntroEditor_name;
    }

    @Override
    public IPersistableElement getPersistable() {
      return null;
    }

    @Override
    public String getToolTipText() {
      return IntroMessages.IntroEditor_tooltip;
    }
  };

  /**
   * Reads the existing text file with give name in the package of {@link IntroEditor}.
   */
  private static String readTemplate(String name) throws IOException {
    InputStream welcomeStream = IntroEditor.class.getResourceAsStream(name);
    try {
      return CharStreams.toString(new InputStreamReader(welcomeStream));
    } finally {
      Closeables.closeQuietly(welcomeStream);
    }
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
  }

  @Override
  public void doSaveAs() {
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
  }

  private Composite createIntroContent(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    composite.setLayout(new GridLayout());
    toolkit.adapt(composite);
    // use Browser
    try {
      final List<SampleDescription> descriptions = SampleDescriptionHelper.getDescriptions();
      // prepare HTML
      String html;
      {
        String sampleTemplate = readTemplate("sample-template.html"); //$NON-NLS-1$
        // prepare samples
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < descriptions.size(); i++) {
          SampleDescription description = descriptions.get(i);
          String sampleHtml = sampleTemplate.replace("${id}", Integer.toString(i)); //$NON-NLS-1$
          sampleHtml = sampleHtml.replace("${name}", description.name); //$NON-NLS-1$
          sampleHtml = sampleHtml.replace("${logo}", description.logo.toURI().toString()); //$NON-NLS-1$
          sampleHtml = sampleHtml.replace("${description}", description.description); //$NON-NLS-1$
          sb.append(sampleHtml);
        }
        // apply "samples" into template
        String welcomeTemplate = readTemplate("welcome-template.html"); //$NON-NLS-1$
        html = welcomeTemplate.replace("${samples}", sb.toString()); //$NON-NLS-1$
      }
      // create Browser
      Browser browser = new Browser(composite, SWT.NONE);
      browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
      browser.setText(html);
      // open links in external browser
      browser.addLocationListener(new LocationAdapter() {
        @Override
        public void changing(LocationEvent event) {
          event.doit = false;
          Program.launch(event.location);
        }
      });
      // register JavaScript function
      new BrowserFunction(browser, "openSample") { //$NON-NLS-1$
        @Override
        public Object function(Object[] arguments) {
          String indexString = (String) arguments[0];
          int index = Integer.parseInt(indexString);
          SampleDescription description = descriptions.get(index);
          openSample(new File(description.directory, description.file));
          return null;
        }
      };
    } catch (Throwable e) {
      DartCore.logError(e);
    }
    // done
    return composite;
  }

  private File getDirectory(File file) {
    IPath path = new Path(file.getAbsolutePath());
    String[] segments = path.segments();
    int i;
    for (i = 0; i < segments.length; i++) {
      if (segments[i].equals("samples")) {
        break;
      }
    }
    // get directory to depth samples + 1
    Path p = (Path) path.removeLastSegments((segments.length - i) - 2);
    return new File(p.toString());
  }

  private void openInEditor(final File file) {
    //performed async to ensure that resource change events have been processed before the editor
    //opens (needed for proper linking w/editor)
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          EditorUtility.openInEditor(ResourceUtil.getFile(file));
        } catch (Throwable e) {
          DartCore.logError(e);
        }
      }
    });
  }

  private void openSample(File file) {
    try {
      File dir = getDirectory(file);
      // TODO(keertip): pass in a real progress monitor
      IProjectUtilities.createOrOpenProject(dir, new NullProgressMonitor());
      openInEditor(file);
    } catch (Throwable e) {
      DartCore.logError(e);
    }
  }
}
