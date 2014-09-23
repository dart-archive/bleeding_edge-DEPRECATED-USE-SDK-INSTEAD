/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.analysis.model.ResolvedHtmlEvent;
import com.google.dart.tools.core.analysis.model.ResourceMap;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

/**
 * Instances of {@code AnalysisWorker} perform analysis by repeatedly calling
 * {@link AnalysisContext#performAnalysisTask()} and update both the index and the error markers
 * based upon the analysis results.
 * 
 * @coverage dart.tools.core.builder
 */
public class AnalysisWorker {

  /**
   * Internal implementation of events broadcast by the worker.
   */
  private abstract class AbstractEvent implements AnalysisEvent {
    private final AnalysisContext context;
    private final ResourceMap resourceMap;
    Source source;
    IResource resource;

    public AbstractEvent(AnalysisContext context, ResourceMap resourceMap) {
      this.context = context;
      this.resourceMap = resourceMap;
    }

    @Override
    public AnalysisContext getContext() {
      return context;
    }

    @Override
    public ContextManager getContextManager() {
      return contextManager;
    }

    @Override
    public ResourceMap getResourceMap() {
      return resourceMap;
    }
  }

  /**
   * Internal implementation of events broadcast by the worker.
   */
  private class Event extends AbstractEvent implements ResolvedEvent {
    CompilationUnit unit;

    public Event(AnalysisContext context, ResourceMap resourceMap) {
      super(context, resourceMap);
    }

    @Override
    public IResource getResource() {
      return resource;
    }

    @Override
    public Source getSource() {
      return source;
    }

    @Override
    public CompilationUnit getUnit() {
      return unit;
    }
  }

  /**
   * Internal implementation of events broadcast by the worker.
   */
  private class HtmlEvent extends AbstractEvent implements ResolvedHtmlEvent {
    HtmlUnit unit;

    public HtmlEvent(AnalysisContext context, ResourceMap resourceMap) {
      super(context, resourceMap);
    }

    @Override
    public IResource getResource() {
      return resource;
    }

    @Override
    public Source getSource() {
      return source;
    }

    @Override
    public HtmlUnit getUnit() {
      return unit;
    }
  }

  /**
   * The {@link AnalysisContext} cache size for a context when background analysis is not being
   * performed on that context.
   */
  private static final int IDLE_CACHE_SIZE = AnalysisOptionsImpl.DEFAULT_CACHE_SIZE;

  /**
   * The {@link AnalysisContext} cache size for a context when background analysis is being
   * performed on that context.
   */
  private static int WORKING_CACHE_SIZE = computeWorkingCacheSize();

  private static final int WORKING_CACHE_SIZE_DEFAULT = IDLE_CACHE_SIZE * 2;
  private static final int WORKING_CACHE_256_MEMORY = 450 * 1024 * 1024;
  private static final int WORKING_CACHE_256_SIZE = 256;
  private static final int WORKING_CACHE_512_MEMORY = 900 * 1024 * 1024;
  private static final int WORKING_CACHE_512_SIZE = 512;

  /**
   * Objects to be notified when each compilation unit has been resolved. Contents of this array
   * will not change, but the array itself may be replaced. Synchronize against
   * {@link #allListenersLock} before accessing this field.
   */
  private static AnalysisListener[] allListeners = new AnalysisListener[] {};

  /**
   * Synchronize against {@code #allListenersLock} before accessing {@link #allListeners}
   */
  private static final Object allListenersLock = new Object();

  /**
   * Add a listener to be notified when compilation units are resolved
   * 
   * @param listener the listener
   */
  public static void addListener(AnalysisListener listener) {
    if (listener == null) {
      return;
    }
    synchronized (allListenersLock) {
      for (AnalysisListener each : allListeners) {
        if (listener == each) {
          return;
        }
      }
      int oldLen = allListeners.length;
      AnalysisListener[] newListeners = new AnalysisListener[oldLen + 1];
      System.arraycopy(allListeners, 0, newListeners, 0, oldLen);
      newListeners[oldLen] = listener;
      allListeners = newListeners;
    }
  }

  /**
   * Ensure that a worker is at the front of the queue to update the analysis for the context.
   * 
   * @param manager the manager containing the context to be analyzed (not {@code null})
   * @param context the context to be analyzed (not {@code null})
   * @see #performAnalysis(AnalysisManager)
   */
  public static void performAnalysisInBackground(ContextManager manager, AnalysisContext context) {
    AnalysisManager.getInstance().performAnalysisInBackground(manager, context);
  }

  /**
   * Remove a listener from the list of objects to be notified.
   * 
   * @param listener the listener to be removed
   */
  public static void removeListener(AnalysisListener listener) {
    synchronized (allListenersLock) {
      for (int index = 0; index < allListeners.length; index++) {
        if (listener == allListeners[index]) {
          int oldLen = allListeners.length;
          AnalysisListener[] newListeners = new AnalysisListener[oldLen - 1];
          System.arraycopy(allListeners, 0, newListeners, 0, index);
          System.arraycopy(allListeners, index + 1, newListeners, index, oldLen - index - 1);
          allListeners = newListeners;
          return;
        }
      }
    }
  }

  /**
   * Wait for any scheduled background analysis to complete or for the specified duration to elapse.
   * 
   * @param milliseconds the number of milliseconds to wait
   * @return {@code true} if the background analysis has completed, else {@code false}
   */
  public static boolean waitForBackgroundAnalysis(long milliseconds) {
    return AnalysisManager.getInstance().waitForBackgroundAnalysis(milliseconds);
  }

  /**
   * Returns what cache size to use for working {@link AnalysisContext}, currently depending on
   * maximum heap size.
   */
  private static int computeWorkingCacheSize() {
    long maxMemory = Runtime.getRuntime().maxMemory();
    if (maxMemory > WORKING_CACHE_512_MEMORY) {
      return WORKING_CACHE_512_SIZE;
    } else if (maxMemory > WORKING_CACHE_256_MEMORY) {
      return WORKING_CACHE_256_SIZE;
    }
    return WORKING_CACHE_SIZE_DEFAULT;
  }

  /**
   * The context manager containing the source for this context (not {@code null}).
   */
  protected final ContextManager contextManager;

  /**
   * An object used to synchronously access the {@link #context} field.
   */
  private final Object lock = new Object();

  /**
   * The analysis context on which analysis is performed or {@code null} if either the analysis is
   * stopped or complete. Synchronize against {@link #lock} before accessing this field.
   */
  private AnalysisContext context;

  /**
   * The marker manager used to translate errors into Eclipse markers (not {@code null}).
   */
  private final AnalysisMarkerManager markerManager;

  /**
   * The project manager used to obtain the index to be updated and used to notify others when
   * analysis is complete (not {@code null}).
   */
  private final ProjectManager projectManager;

  /**
   * Contains information about the compilation unit that was resolved.
   */
  private final Event event;

  /**
   * Contains information about the HTML unit that was resolved.
   */
  private final HtmlEvent htmlEvent;

  /**
   * Flag to prevent log from being saturated with exceptions.
   */
  private static boolean exceptionLogged = false;

  /**
   * Construct a new instance for performing analysis which updates the
   * {@link ProjectManager#getIndex() default index} and uses the
   * {@link AnalysisMarkerManager#getInstance() default marker manager} to translate errors into
   * Eclipse markers.
   * 
   * @param manager the manager containing sources for the specified context (not {@code null})
   * @param context the context used to perform the analysis (not {@code null})
   */
  public AnalysisWorker(ContextManager manager, AnalysisContext context) {
    this(
        manager,
        context,
        DartCore.getProjectManager(),
        DartCore.getProjectManager().getResourceMap(context),
        AnalysisMarkerManager.getInstance());
  }

  /**
   * Construct a new instance for performing analysis.
   * 
   * @param contextManager manager containing sources for the specified context (not {@code null})
   * @param context the context used to perform the analysis (not {@code null})
   * @param projectManager used to obtain the index to be updated and notified others when analysis
   *          is complete (not {@code null})
   * @param resourceMap the resource map for the given context
   * @param markerManager used to translate errors into Eclipse markers (not {@code null})
   */
  public AnalysisWorker(ContextManager contextManager, AnalysisContext context,
      ProjectManager projectManager, ResourceMap resourceMap, AnalysisMarkerManager markerManager) {
    this.contextManager = contextManager;
    this.context = context;
    this.projectManager = projectManager;
    this.markerManager = markerManager;
    this.contextManager.addWorker(this);
    this.event = new Event(context, resourceMap);
    this.htmlEvent = new HtmlEvent(context, resourceMap);
  }

  /**
   * Answer the context being processed by the receiver.
   * 
   * @return the context or {@code null} if processing has been stopped or is complete
   */
  public AnalysisContext getContext() {
    synchronized (lock) {
      return context;
    }
  }

  /**
   * Perform analysis by repeatedly calling {@link AnalysisContext#performAnalysisTask()} and update
   * both the index and the error markers based upon the analysis results.
   * 
   * @param manager the {@link AnalysisManager} or {@code null} if is performed without a manager
   */
  public void performAnalysis(AnalysisManager manager) {

    // Check if project exists
    if (contextManager == null || contextManager.getResource() == null
        || !contextManager.getResource().exists()) {
      return;
    }

    // Check for a valid context and SDK
    DartSdk sdk;
    synchronized (lock) {
      if (context == null) {
        return;
      }
      sdk = context.getSourceFactory().getDartSdk();
    }
    boolean hasSdk = sdk != DartSdkManager.NONE;
    markerManager.queueHasDartSdk(contextManager.getResource(), hasSdk);
    if (!hasSdk) {
      return;
    }

    // Check if the context has been set to null indicating that analysis should stop
    AnalysisContext context;
    synchronized (lock) {
      if (this.context == null) {
        return;
      }
      context = this.context;
    }
    setCacheSize(context, WORKING_CACHE_SIZE);

    boolean analysisComplete = false;
    while (true) {

      // Check if the context has been set to null indicating that analysis should stop
      synchronized (lock) {
        if (this.context == null) {
          break;
        }
      }

      // Exit if no more analysis to be performed (changes == null)
      AnalysisResult result;
      try {
        result = context.performAnalysisTask();
      } catch (RuntimeException e) {
        DartCore.logError("Analysis Failed: " + contextManager, e);
        break;
      }
      ChangeNotice[] changes = result.getChangeNotices();
      if (changes == null) {
        analysisComplete = true;
        break;
      }

      // Process changes and allow subclasses to check results
      processChanges(context, changes);
      checkResults(context);
    }

    setCacheSize(context, IDLE_CACHE_SIZE);
    stop();
    markerManager.done();

    // Notify others that analysis is complete
    if (analysisComplete) {
      notifyComplete();
    }
  }

  /**
   * Queue this worker to have {@link #performAnalysis(AnalysisManager)} called in a background job.
   * 
   * @see #performAnalysisInBackground(Project, AnalysisContext)
   */
  public void performAnalysisInBackground() {
    AnalysisManager.getInstance().addWorker(this);
  }

  /**
   * Signal the receiver to stop analysis.
   */
  public void stop() {
    synchronized (lock) {
      context = null;
    }
    contextManager.removeWorker(this);
  }

  /**
   * Subclasses may override this method to call various "get" methods on the context looking to see
   * if information it needs is cached.
   * 
   * @param context the analysis context being processed (not {@code null})
   */
  protected void checkResults(AnalysisContext context) {
  }

  /**
   * Notify those interested that the analysis is complete.
   */
  private void notifyComplete() {
    if (contextManager instanceof Project) {
      projectManager.projectAnalyzed((Project) contextManager);
    }
    AnalysisListener[] currentListeners;
    synchronized (allListenersLock) {
      currentListeners = allListeners;
    }
    for (AnalysisListener listener : currentListeners) {
      try {
        listener.complete(event);
      } catch (Exception e) {
        if (!exceptionLogged) {
          // Log at most one exception so as not to flood the log
          exceptionLogged = true;
          DartCore.logError("Exception notifying listener that analysis is complete", e);
        }
      }
    }
  }

  /**
   * Notify those interested that a compilation unit has been resolved.
   * 
   * @param context the analysis context containing the unit that was resolved (not {@code null})
   * @param unit the unit that was resolved (not {@code null})
   * @param source the source of the unit that was resolved (not {@code null})
   * @param resource the resource of the unit that was resolved or {@code null} if outside the
   *          workspace
   */
  private void notifyResolved(AnalysisContext context, CompilationUnit unit, Source source,
      IResource resource) {
    AnalysisListener[] currentListeners;
    synchronized (allListenersLock) {
      currentListeners = allListeners;
    }
    event.unit = unit;
    event.source = source;
    event.resource = resource;
    for (AnalysisListener listener : currentListeners) {
      try {
        listener.resolved(event);
      } catch (Exception e) {
        if (!exceptionLogged) {
          // Log at most one exception so as not to flood the log
          exceptionLogged = true;
          DartCore.logError("Exception notifying listener of resolved unit: " + source, e);
        }
      }
    }
  }

  /**
   * Notify those interested that a HTML unit has been resolved.
   * 
   * @param context the analysis context containing the unit that was resolved (not {@code null})
   * @param unit the unit that was resolved (not {@code null})
   * @param source the source of the unit that was resolved (not {@code null})
   * @param resource the resource of the unit that was resolved or {@code null} if outside the
   *          workspace
   */
  private void notifyResolved(AnalysisContext context, HtmlUnit unit, Source source,
      IResource resource) {
    AnalysisListener[] currentListeners;
    synchronized (allListenersLock) {
      currentListeners = allListeners;
    }
    htmlEvent.unit = unit;
    htmlEvent.source = source;
    htmlEvent.resource = resource;
    for (AnalysisListener listener : currentListeners) {
      try {
        listener.resolvedHtml(htmlEvent);
      } catch (Exception e) {
        if (!exceptionLogged) {
          // Log at most one exception so as not to flood the log
          exceptionLogged = true;
          DartCore.logError("Exception notifying listener of resolved HTML unit: " + source, e);
        }
      }
    }
  }

  /**
   * Update both the index and the error markers based upon the analysis results.
   * 
   * @param context the analysis context containing the unit that was resolved (not {@code null})
   * @param changes the changes to be processed (not {@code null})
   */
  private void processChanges(AnalysisContext context, ChangeNotice[] changes) {
    for (ChangeNotice change : changes) {
      Source source = change.getSource();
      IResource res = contextManager.getResource(source);

      // If errors are available, then queue the errors to be translated to markers
      AnalysisError[] errors = change.getErrors();
      if (errors != null) {
        if (res == null) {
          // TODO (danrubel): log unmatched sources once context 
          // only returns errors for added sources
          // DartCore.logError("Failed to determine resource for: " + source);
        } else {
          IPath location = res.getLocation();
          if (location != null && !DartCore.isContainedInPackages(location.toFile())) {
            LineInfo lineInfo = change.getLineInfo();
            if (lineInfo == null) {
              // Sometimes this happens in UI tests, but we don't know what error.
              @SuppressWarnings("resource")
              PrintStringWriter writer = new PrintStringWriter();
              writer.print("Missing line information for: ");
              writer.println(source);
              for (AnalysisError error : errors) {
                writer.print("Error: ");
                writer.print(error.getErrorCode());
                writer.print(" ");
                writer.print(error.getSource());
                writer.print(" ");
                writer.print(error.getOffset());
                writer.print(" ");
                writer.print(error.getLength());
                writer.print(" ");
                writer.println(error.getMessage());
              }
              DartCore.logInformation(writer.toString());
            } else {
              markerManager.queueErrors(res, lineInfo, errors);
            }
          }
        }
      }

      // If there is a resolved unit, then then notify others such as indexer
      CompilationUnit unit = change.getCompilationUnit();
      if (unit != null) {
        notifyResolved(context, unit, source, res);
      }

      // If there is a resolved HTML unit, then then notify others such as indexer
      HtmlUnit htmlUnit = change.getHtmlUnit();
      if (htmlUnit != null) {
        notifyResolved(context, htmlUnit, source, res);
      }
    }
  }

  private void setCacheSize(AnalysisContext context, int cacheSize) {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
    options.setCacheSize(cacheSize);
    context.setAnalysisOptions(options);
  }
}
