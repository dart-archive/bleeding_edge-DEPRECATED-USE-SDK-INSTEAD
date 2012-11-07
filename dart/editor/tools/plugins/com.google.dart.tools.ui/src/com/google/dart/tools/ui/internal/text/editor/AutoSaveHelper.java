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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.collect.MapMaker;
import com.google.common.io.Files;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.util.PartListenerAdapter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper for auto-saving {@link ITextEditor}s.
 */
public class AutoSaveHelper {
  private static class CloseTask extends Task {
    private final IFile file;

    public CloseTask(IFile file) {
      this.file = file;
    }

    @Override
    void execute() throws Exception {
      instance.pathMapCloseFile(file);
    }
  }

  private static class EditorInfo implements Serializable {
    private final String content;
    private final int selectionStart;
    private final int selectionLength;

    public EditorInfo(String content, int selectionStart, int selectionLength) {
      this.content = content;
      this.selectionStart = selectionStart;
      this.selectionLength = selectionLength;
    }
  }
  private static class OpenTask extends Task {
    private final IFile file;

    public OpenTask(IFile file) {
      this.file = file;
    }

    @Override
    void execute() throws Exception {
      instance.pathMapOpenFile(file);
    }
  }

  private static class SaveTask extends Task {
    private final IFile file;
    private final EditorInfo info;

    public SaveTask(IFile file, EditorInfo info) {
      this.file = file;
      this.info = info;
    }

    @Override
    void execute() throws Exception {
      String path = getStringPath(file);
      Integer id = instance.pathMap.get(path);
      if (id != null) {
        editorInfoWrite(id, info);
      }
    }

  }

  private abstract static class Task {
    abstract void execute() throws Exception;
  }

  private static final AutoSaveHelper instance = new AutoSaveHelper();

  public static void reconciled(IEditorInput input, ISourceViewer viewer) {
    IFile file = getInputFile(input);
    if (file != null && viewer != null) {
      IDocument document = viewer.getDocument();
      if (document != null) {
        String content = document.get();
        Point selection = DartUI.getSelectionRange(viewer);
        EditorInfo info = new EditorInfo(content, selection.x, selection.y);
        instance.taskQueue.add(new SaveTask(file, info));
      }
    }
  }

  /**
   * Starts Thread to run main loop. Should be called only one time.
   */
  public static void start() {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        // if workbench is still starting, reschedule this Runnable
        if (workbench.isStarting()) {
          Display.getDefault().asyncExec(this);
          return;
        }
        // OK, workbench started, editors opened.
        // Now we can restore content of editors.
        instance.start(workbench);
      }
    });
  }

  /**
   * Deletes {@link EditorInfo} into file with name "id".
   */
  private static void editorInfoDelete(Integer id) {
    File saveFolder = getSaveFolder();
    File saveFile = new File(saveFolder, id.toString());
    if (saveFile.exists()) {
      saveFile.delete();
    }
  }

  /**
   * @return the {@link EditorInfo} from file with given "id", may be <code>null</code> if file does
   *         not exist.
   */
  private static EditorInfo editorInfoRead(Integer id) throws Exception {
    File saveFolder = getSaveFolder();
    File saveFile = new File(saveFolder, id.toString());
    if (saveFile.exists()) {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile));
      try {
        return (EditorInfo) ois.readObject();
      } finally {
        ois.close();
      }
    }
    return null;
  }

  /**
   * Writes given {@link EditorInfo} into file with name "id".
   */
  private static void editorInfoWrite(Integer id, EditorInfo info) throws Exception {
    File saveFolder = getSaveFolder();
    File saveFile = new File(saveFolder, id.toString());
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile));
    try {
      oos.writeObject(info);
    } finally {
      oos.close();
    }
  }

  /**
   * @return the input {@link IFile}, may be <code>null</code>.
   */
  private static IFile getInputFile(IEditorInput input) {
    if (input instanceof IFileEditorInput) {
      return ((IFileEditorInput) input).getFile();
    }
    return null;
  }

  /**
   * @return the input {@link IFile}, may be <code>null</code>.
   */
  private static IFile getInputFile(IWorkbenchPart part) {
    if (part instanceof ITextEditor) {
      ITextEditor editor = (ITextEditor) part;
      IEditorInput input = editor.getEditorInput();
      return getInputFile(input);
    }
    return null;
  }

  /**
   * @return the "auto save" folder in plugin state location.
   */
  private static File getSaveFolder() {
    IPath stateLocation = DartToolsPlugin.getDefault().getStateLocation();
    return stateLocation.append("autoSave").toFile();
  }

  /**
   * @return the {@link String} presentation of {@link IFile} path, good to use as argument for
   *         {@link IWorkspaceRoot#getFile(IPath)}.
   */
  private static String getStringPath(IFile file) {
    return file.getFullPath().toPortableString();
  }

  private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<Task>();

  private final AtomicInteger lastId = new AtomicInteger();

  private final Map<String, Integer> pathMap = new MapMaker().makeMap();

  /**
   * Removes given {@link IFile} from the list of opened files.
   */
  private void pathMapCloseFile(IFile file) throws Exception {
    String path = getStringPath(file);
    Integer id = pathMap.remove(path);
    if (id != null) {
      pathMapWrite();
      editorInfoDelete(id);
    }
  }

  /**
   * We remove "autoSave" folder on normal shutdown, we don't need to restore anything.
   */
  private void pathMapDelete() {
    try {
      File saveFolder = getSaveFolder();
      Files.deleteRecursively(saveFolder);
    } catch (Throwable e) {
    }
  }

  private File pathMapGetFile() {
    File saveFolder = getSaveFolder();
    return new File(saveFolder, "map");
  }

  /**
   * Remembers in {@link #pathMap} that given {@link IFile} was opened.
   */
  private void pathMapOpenFile(IFile file) throws Exception {
    String path = getStringPath(file);
    if (!pathMap.containsKey(path)) {
      int id = lastId.getAndIncrement();
      pathMap.put(path, id);
      pathMapWrite();
    }
  }

  /**
   * Writes {@link #pathMap}.
   */
  private void pathMapWrite() throws Exception {
    File saveFile = pathMapGetFile();
    saveFile.getParentFile().mkdirs();
    saveFile.delete();
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile));
    try {
      oos.writeObject(pathMap);
    } finally {
      oos.close();
    }
  }

  private void restoreEditors(IWorkbenchPage activePage) {
    try {
      File mapFile = pathMapGetFile();
      if (mapFile.exists()) {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mapFile));
        try {
          @SuppressWarnings("unchecked")
          Map<String, Integer> map = (Map<String, Integer>) ois.readObject();
          IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
          for (Entry<String, Integer> entry : map.entrySet()) {
            String pathStr = entry.getKey();
            Integer id = entry.getValue();
            try {
              EditorInfo info = editorInfoRead(id);
              if (info != null) {
                IFile file = workspaceRoot.getFile(new Path(pathStr));
                if (file.exists()) {
                  IEditorPart newEditor = IDE.openEditor(activePage, file);
                  if (newEditor instanceof AbstractTextEditor) {
                    ISourceViewer viewer = ReflectionUtils.invokeMethod(
                        newEditor,
                        "getSourceViewer()");
                    viewer.getDocument().set(info.content);
                    viewer.setSelectedRange(info.selectionStart, info.selectionLength);
                  }
                }
              }
            } catch (Throwable e) {
              DartToolsPlugin.log(e);
            }
          }
        } finally {
          ois.close();
        }
      }
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }

  /**
   * Main loop of {@link AutoSaveHelper}, executes {@link Task}.
   */
  private void runTasksLoop() {
    while (true) {
      try {
        Task task = taskQueue.take();
        try {
          task.execute();
        } catch (Throwable e) {
          DartToolsPlugin.log(e);
        }
      } catch (InterruptedException e) {
      }
    }
  }

  /**
   * Start {@link AutoSaveHelper} with given started {@link IWorkbench}.
   */
  private void start(IWorkbench workbench) {
    IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
    restoreEditors(activePage);
    // remember all open editors
    {
      IEditorReference[] editorReferences = activePage.getEditorReferences();
      for (IEditorReference reference : editorReferences) {
        try {
          IEditorInput input = reference.getEditorInput();
          IFile file = getInputFile(input);
          if (file != null) {
            taskQueue.add(new OpenTask(file));
          }
        } catch (Throwable e) {
          DartToolsPlugin.log(e);
        }
      }
    }
    // delete all saved data
    pathMapDelete();
    // add shutdown listener
    workbench.addWorkbenchListener(new IWorkbenchListener() {
      @Override
      public void postShutdown(IWorkbench workbench) {
        pathMapDelete();
      }

      @Override
      public boolean preShutdown(IWorkbench workbench, boolean forced) {
        return true;
      }
    });
    // add part open/close listener
    activePage.addPartListener(new PartListenerAdapter() {
      @Override
      public void partClosed(IWorkbenchPart part) {
        IFile file = getInputFile(part);
        if (file != null) {
          taskQueue.add(new CloseTask(file));
        }
      }

      @Override
      public void partOpened(IWorkbenchPart part) {
        IFile file = getInputFile(part);
        if (file != null) {
          taskQueue.add(new OpenTask(file));
        }
      }
    });
    // run tasks loop
    {
      Thread thread = new Thread() {
        @Override
        public void run() {
          instance.runTasksLoop();
        }
      };
      thread.setDaemon(true);
      thread.start();
    }
  }
}
