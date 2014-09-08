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
package com.google.dart.tools.ui.theme.preferences;

import com.google.dart.tools.deploy.Activator;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSourceViewer;
import com.google.dart.tools.ui.theme.ColorTheme;
import com.google.dart.tools.ui.theme.ColorThemeManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

// TODO(messick): Export strings.

/**
 * The preference page for managing color themes.
 * 
 * @see com.github.eclipsecolortheme.preferences.ColorThemePreferencePage
 */
@SuppressWarnings("restriction")
public class ThemePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private static final String SAMPLE_CODE_FILE_NAME = "DartSample.dart";

  public static IPreferenceStore globalPreferences() {
    return /*Activator*/DartToolsPlugin.getDefault().getPreferenceStore();
  }

  private static String loadPreviewContentFromFile(String filename) {
    String line;
    String separator = System.getProperty("line.separator"); //$NON-NLS-1$
    StringBuffer buffer = new StringBuffer(512);
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(
          ThemePreferencePage.class.getResourceAsStream(filename)));
      while ((line = reader.readLine()) != null) {
        buffer.append(line);
        buffer.append(separator);
      }
    } catch (IOException io) {
      Activator.logError(io);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
    return buffer.toString();
  }

  private static String readFile(File file) throws IOException {
    Reader in = new BufferedReader(new FileReader(file));
    StringBuilder sb = new StringBuilder();
    try {
      char[] chars = new char[1 << 11];
      int length;
      while ((length = in.read(chars)) > 0) {
        sb.append(chars, 0, length);
      }
    } finally {
      in.close();
    }
    return sb.toString();
  }

  private static void setLinkTarget(Link link, final String target) {
    link.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        Program.launch(target);
      }
    });
  }

  private static String viewerCode() {
    String content = loadPreviewContentFromFile(SAMPLE_CODE_FILE_NAME);
    return content;
  }

  private ColorThemeManager colorThemeManager = new ColorThemeManager();
  private Composite container;
  private List themeSelectionList;
  private Composite themeSelection;
  private Composite themeDetails;
  private SourceViewer previewViewer;
  private Label authorLabel;
  private Link websiteLink;
  private TemporaryProject project;
  private IFile unit;
  private CompilationUnitEditor editor;
  private DartSourceViewer sourceViewer;
  private WorkbenchPage page;
  private boolean updating;

  /**
   * Creates a new color theme preference page.
   */
  public ThemePreferencePage() {
    setPreferenceStore(globalPreferences());
    updating = false;
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  @Override
  public boolean performCancel() {
    while (updating) {
      waitALittle();
    }
    colorThemeManager.undoPreview();

    try {
      if (editor != null) {
        editor.close(false);
      }
      if (project != null) {
        project.dispose();
      }
    } catch (CoreException ex) {
      Activator.logError(ex);
    }
    return super.performCancel();
  }

  @Override
  public boolean performOk() {
    while (updating) {
      waitALittle();
    }
    try {
      if (editor != null) {
        String selectedThemeName = themeSelectionList.getSelection()[0];
        getPreferenceStore().setValue("colorTheme", selectedThemeName); // $NON-NLS-1$
        colorThemeManager.applyTheme(selectedThemeName);
        editor.close(false);
      }
      if (project != null) {
        project.dispose();
      }
    } catch (PartInitException e) {
      Activator.logError(e);
    } catch (CoreException ex) {
      Activator.logError(ex);
    }

    return super.performOk();
  }

  @Override
  protected void contributeButtons(Composite parent) {
    ((GridLayout) parent.getLayout()).numColumns++;

    Button button = new Button(parent, SWT.NONE);
    button.setText("&Import a theme...");
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        FileDialog dialog = new FileDialog(getShell());
        String file = dialog.open();
        if (file == null) {
          return;
        }
        ColorTheme theme;
        try {
          String content = readFile(new File(file));
          theme = colorThemeManager.saveTheme(content);
        } catch (IOException e) {
          theme = null;
        }
        if (theme != null) {
          reloadThemeSelectionList();
          themeSelectionList.setSelection(new String[] {theme.getName()});
          updateDetails(theme);
        } else {
          MessageBox box = new MessageBox(getShell(), SWT.OK);
          box.setText("Theme not imported");
          box.setMessage("This is not a valid theme file.");
          box.open();
        }
      }
    });
  }

  @Override
  protected Control createContents(Composite parent) {
    container = new Composite(parent, SWT.NONE);
    GridData gridData = new GridData();
    GridLayout containerLayout = new GridLayout(1, true);
    containerLayout.marginWidth = 0;
    container.setLayout(containerLayout);

    gridData = new GridData(GridData.FILL_BOTH);
    themeSelection = new Composite(container, SWT.NONE);
    GridLayout themeSelectionLayout = new GridLayout(2, false);
    themeSelectionLayout.marginWidth = 0;
    themeSelectionLayout.marginHeight = 0;
    themeSelection.setLayout(themeSelectionLayout);
    themeSelection.setLayoutData(gridData);

    gridData = new GridData(GridData.FILL_VERTICAL);
    gridData.minimumWidth = 120;
    themeSelectionList = new List(themeSelection, SWT.BORDER | SWT.V_SCROLL);
    themeSelectionList.setLayoutData(gridData);
    fillThemeSelectionList();

    gridData = new GridData(GridData.FILL_BOTH);
    gridData.verticalAlignment = SWT.TOP;
    GridLayout themeDetailsLayout = new GridLayout(1, true);
    themeDetailsLayout.marginWidth = 0;
    themeDetailsLayout.marginHeight = 0;
    themeDetails = new Composite(themeSelection, SWT.NONE);

    createPreviewer(themeDetails);

    themeDetails.setLayoutData(gridData);
    themeDetails.setLayout(themeDetailsLayout);

    authorLabel = new Label(themeDetails, SWT.NONE);
    GridDataFactory.swtDefaults().grab(true, false).applyTo(authorLabel);
    websiteLink = new Link(themeDetails, SWT.NONE);
    GridDataFactory.swtDefaults().grab(true, false).applyTo(websiteLink);

    themeSelectionList.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        updateDetails(colorThemeManager.getTheme(themeSelectionList.getSelection()[0]));
      }
    });

    String activeThemeName = getPreferenceStore().getString("colorTheme"); // $NON-NLS-1$
    if (colorThemeManager.getTheme(activeThemeName) == null) {
      activeThemeName = ColorThemeManager.DEFAULT_THEME_NAME;
    }
    themeSelectionList.setSelection(new String[] {activeThemeName});
    updateDetails(colorThemeManager.getTheme(activeThemeName));

    // TODO(messick): Need to think about including this web site.
    Link ectLink = new Link(container, SWT.NONE);
    ectLink.setText("Download more themes or create your own on "
        + "<a>eclipsecolorthemes.org</a>.");
    setLinkTarget(ectLink, "http://eclipsecolorthemes.org");
    return container;
  }

  @Override
  protected void performDefaults() {
    getPreferenceStore().setToDefault("colorTheme"); // $NON-NLS-1$
    colorThemeManager.clearImportedThemes();
    reloadThemeSelectionList();
    if (editor != null) {
      String selectedThemeName = themeSelectionList.getSelection()[0];
      getPreferenceStore().setValue("colorTheme", selectedThemeName); // $NON-NLS-1$
      colorThemeManager.applyTheme(selectedThemeName);
    }
    super.performDefaults();
  }

  private void createPreviewer(Composite parent) {
    setup();
    Composite previewComp = new Composite(parent, SWT.NONE);
    previewComp.setEnabled(false); // After re-parenting, it is too inconsistent to allow mouse clicks
    GridLayout layout = new GridLayout();
    layout.marginHeight = layout.marginWidth = 0;
    previewComp.setLayout(layout);
    previewComp.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label label = new Label(previewComp, SWT.NONE);
    label.setText("Code Editor");
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    previewViewer = getSourceViewer(previewComp); //new SourceViewer(previewComp, null, SWT.BORDER | SWT.V_SCROLL /*| SWT.H_SCROLL */);
    previewViewer.getTextWidget().setSelection(173, 210); // TODO(messick): This is fragile.
    previewViewer.setEditable(false);

    Control control = previewViewer.getControl();
    GridData controlData = new GridData(GridData.FILL_BOTH);
    control.setLayoutData(controlData);
  }

  private void fillThemeSelectionList() {
    Set<ColorTheme> themes = colorThemeManager.getThemes();
    java.util.List<String> themeNames = new LinkedList<String>();
    for (ColorTheme theme : themes) {
      themeNames.add(theme.getName());
    }
    Collections.sort(themeNames, String.CASE_INSENSITIVE_ORDER);
    themeNames.remove(ColorThemeManager.DEFAULT_THEME_NAME);
    themeNames.add(0, ColorThemeManager.DEFAULT_THEME_NAME);
    themeSelectionList.setItems(themeNames.toArray(new String[themeNames.size()]));
  }

  private SourceViewer getSourceViewer(Composite parent) {
    (sourceViewer.getTextWidget()).setParent(parent);
    return sourceViewer;
  }

  private void reloadThemeSelectionList() {
    themeSelectionList.removeAll();
    fillThemeSelectionList();
    themeSelectionList.setSelection(new String[] {ColorThemeManager.DEFAULT_THEME_NAME});
    updateDetails(null);
    container.pack();
  }

  private void setup() {
    Exception caughtException = null;
    String sampleCode = viewerCode();
    try {
      page = (WorkbenchPage) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      project = new TemporaryProject();
      String name = "Temp.dart";
      unit = project.setUnitContent(name, sampleCode);
      editor = (CompilationUnitEditor) IDE.openEditor(
          PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
          unit);
      project.getProject().setHidden(true);
      name = editor.getPartName();
      IEditorReference ref = null;
      IEditorReference[] eds = page.getEditorManager().getEditors();
      for (IEditorReference r : eds) {
        if (r.getPartName() == name) { // intentional identity check
          ref = r;
          break;
        }
      }
      if (ref != null) {
        page.hideEditor(ref);
      }
      IPreferenceStore store = colorThemeManager.createCombinedPreferenceStore();
      editor.setPreferences(store);
      sourceViewer = ReflectionUtils.invokeMethod(editor, "getSourceViewer()");
      sourceViewer.setPreferenceStore(store);
    } catch (CoreException ex) {
      caughtException = ex;
    } catch (IOException ex) {
      caughtException = ex;
    }
    if (caughtException != null) {
      Activator.logError(caughtException);
    }
  }

  private void updateDetails(ColorTheme theme) {
    if (editor == null) {
      return;
    }
    if (theme == null) {
      // TODO(messick): Fix this awkward UX
      themeDetails.setVisible(false);
    } else {
      try {
        updating = true;
        authorLabel.setText("Created by " + theme.getAuthor());
        String website = theme.getWebsite();
        if (website == null || website.length() == 0) {
          websiteLink.setVisible(false);
        } else {
          websiteLink.setText("<a>" + website + "</a>"); // $NON-NLS-1$ // $NON-NLS-2$
          for (Listener listener : websiteLink.getListeners(SWT.Selection)) {
            websiteLink.removeListener(SWT.Selection, listener);
          }
          setLinkTarget(websiteLink, website);
          websiteLink.setVisible(true);
        }
        themeDetails.setVisible(true);
        colorThemeManager.previewTheme(theme.getName());
        editor.reconciled(true, new NullProgressMonitor());
        authorLabel.pack();
        websiteLink.pack();
      } finally {
        updating = false;
      }
    }
  }

  private void waitALittle() {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // ignore it
    }
  }
}
