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

import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.ExternalDartProject;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.problem.CategorizedProblem;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.SourceFileElement;
import com.google.dart.tools.core.problem.Problem;
import com.google.dart.tools.core.problem.ProblemRequestor;
import com.google.dart.tools.core.utilities.net.URIUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.Logger;
import com.google.dart.tools.ui.internal.text.DartStatusConstants;
import com.google.dart.tools.ui.internal.text.correction.DartCorrectionProcessor;
import com.google.dart.tools.ui.internal.text.dart.IProblemRequestorExtension;
import com.google.dart.tools.ui.internal.text.editor.saveparticipant.IPostSaveListener;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.quickassist.IQuickFixableAnnotation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.jface.text.source.ImageUtilities;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.ForwardingDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CompilationUnitDocumentProvider extends TextFileDocumentProvider implements
    ICompilationUnitDocumentProvider {

  /**
   * Annotation representing a <code>Problem</code>.
   */
  static public class ProblemAnnotation extends Annotation implements IJavaAnnotation,
      IAnnotationPresentation, IQuickFixableAnnotation {

    public static final String SPELLING_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.spelling"; //$NON-NLS-1$

    // XXX: To be fully correct these constants should be non-static
    /**
     * The layer in which task problem annotations are located.
     */
    @SuppressWarnings("unused")
    private static final int TASK_LAYER;
    /**
     * The layer in which info problem annotations are located.
     */
    private static final int INFO_LAYER;
    /**
     * The layer in which warning problem annotations representing are located.
     */
    private static final int WARNING_LAYER;
    /**
     * The layer in which error problem annotations representing are located.
     */
    private static final int ERROR_LAYER;

    static {
      AnnotationPreferenceLookup lookup = EditorsUI.getAnnotationPreferenceLookup();
      TASK_LAYER = computeLayer("org.eclipse.ui.workbench.texteditor.task", lookup); //$NON-NLS-1$
      INFO_LAYER = computeLayer("com.google.dart.tools.ui.info", lookup); //$NON-NLS-1$
      WARNING_LAYER = computeLayer("com.google.dart.tools.ui.warning", lookup); //$NON-NLS-1$
      ERROR_LAYER = computeLayer("com.google.dart.tools.ui.error", lookup); //$NON-NLS-1$
    }

    private static Image fgQuickFixImage;

    private static Image fgQuickFixErrorImage;
    private static Image fgTaskImage;
    private static Image fgInfoImage;
    private static Image fgWarningImage;
    private static Image fgErrorImage;
    private static boolean fgImagesInitialized = false;

    private static int computeLayer(String annotationType, AnnotationPreferenceLookup lookup) {
      Annotation annotation = new Annotation(annotationType, false, null);
      AnnotationPreference preference = lookup.getAnnotationPreference(annotation);
      if (preference != null) {
        return preference.getPresentationLayer() + 1;
      } else {
        return IAnnotationAccessExtension.DEFAULT_LAYER + 1;
      }
    }

    private CompilationUnit fCompilationUnit;
    private List<IJavaAnnotation> fOverlaids;
    private Problem fProblem;
    private Image fImage;
    private boolean fImageInitialized = false;
    private int fLayer = IAnnotationAccessExtension.DEFAULT_LAYER;
    private boolean fIsQuickFixable;
    private boolean fIsQuickFixableStateSet = false;

    public ProblemAnnotation(Problem problem, CompilationUnit cu) {

      fProblem = problem;
      fCompilationUnit = cu;

      // TODO(scheglov) restore when we will implement this feature
//      if (DartSpellingReconcileStrategy.SPELLING_PROBLEM_ID == fProblem.getID()) {
//        setType(SPELLING_ANNOTATION_TYPE);
//        fLayer = WARNING_LAYER;
//        DartX.todo("Task");
//        // } else if (Problem.Task == fProblem.getID()) {
//        // setType(DartMarkerAnnotation.TASK_ANNOTATION_TYPE);
//        // fLayer = TASK_LAYER;
//      } else 
      if (fProblem.isWarning()) {
        setType(DartMarkerAnnotation.WARNING_ANNOTATION_TYPE);
        fLayer = WARNING_LAYER;
      } else if (fProblem.isError()) {
        setType(DartMarkerAnnotation.ERROR_ANNOTATION_TYPE);
        fLayer = ERROR_LAYER;
      } else {
        setType(DartMarkerAnnotation.INFO_ANNOTATION_TYPE);
        fLayer = INFO_LAYER;
      }
    }

    @Override
    public void addOverlaid(IJavaAnnotation annotation) {
      if (fOverlaids == null) {
        fOverlaids = new ArrayList<IJavaAnnotation>(1);
      }
      fOverlaids.add(annotation);
    }

    @Override
    public String[] getArguments() {
      return isProblem() ? fProblem.getArguments() : null;
    }

    @Override
    public CompilationUnit getCompilationUnit() {
      return fCompilationUnit;
    }

    @Override
    public ErrorCode getId() {
      return fProblem.getID();
    }

    public Image getImage(Display display) {
      initializeImages();
      return fImage;
    }

    @Override
    public int getLayer() {
      return fLayer;
    }

    @Override
    public String getMarkerType() {
      if (fProblem instanceof CategorizedProblem) {
        return ((CategorizedProblem) fProblem).getMarkerType();
      }
      return null;
    }

    @Override
    public Iterator<IJavaAnnotation> getOverlaidIterator() {
      if (fOverlaids != null) {
        return fOverlaids.iterator();
      }
      return null;
    }

    @Override
    public IJavaAnnotation getOverlay() {
      return null;
    }

    @Override
    public String getText() {
      return fProblem.getMessage();
    }

    @Override
    public boolean hasOverlay() {
      return false;
    }

    @Override
    public boolean isProblem() {
      String type = getType();
      return DartMarkerAnnotation.INFO_ANNOTATION_TYPE.equals(type)
          || DartMarkerAnnotation.WARNING_ANNOTATION_TYPE.equals(type)
          || DartMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(type)
          || SPELLING_ANNOTATION_TYPE.equals(type);
    }

    @Override
    public boolean isQuickFixable() {
      Assert.isTrue(isQuickFixableStateSet());
      return fIsQuickFixable;
    }

    @Override
    public boolean isQuickFixableStateSet() {
      return fIsQuickFixableStateSet;
    }

    @Override
    public void paint(GC gc, Canvas canvas, Rectangle r) {
      initializeImage();
      if (fImage != null) {
        ImageUtilities.drawImage(fImage, gc, canvas, r, SWT.CENTER, SWT.TOP);
      }
    }

    @Override
    public void removeOverlaid(IJavaAnnotation annotation) {
      if (fOverlaids != null) {
        fOverlaids.remove(annotation);
        if (fOverlaids.size() == 0) {
          fOverlaids = null;
        }
      }
    }

    @Override
    public void setQuickFixable(boolean state) {
      fIsQuickFixable = state;
      fIsQuickFixableStateSet = true;
    }

    private boolean indicateQuixFixableProblems() {
      return PreferenceConstants.getPreferenceStore().getBoolean(
          PreferenceConstants.EDITOR_CORRECTION_INDICATION);
    }

    private void initializeImage() {
      // http://bugs.eclipse.org/bugs/show_bug.cgi?id=18936
      if (!fImageInitialized) {
        initializeImages();
        DartX.todo("quickfix");
        if (!isQuickFixableStateSet()) {
          setQuickFixable(isProblem() && indicateQuixFixableProblems()
              && DartCorrectionProcessor.hasCorrections(this)); // no light bulb for tasks
        }
        if (isQuickFixable()) {
          if (DartMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(getType())) {
            fImage = fgQuickFixErrorImage;
          } else {
            fImage = fgQuickFixImage;
          }
        } else {
          String type = getType();
          if (DartMarkerAnnotation.TASK_ANNOTATION_TYPE.equals(type)) {
            fImage = fgTaskImage;
          } else if (DartMarkerAnnotation.INFO_ANNOTATION_TYPE.equals(type)) {
            fImage = fgInfoImage;
          } else if (DartMarkerAnnotation.WARNING_ANNOTATION_TYPE.equals(type)) {
            fImage = fgWarningImage;
          } else if (DartMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(type)) {
            fImage = fgErrorImage;
          }
        }
        fImageInitialized = true;
      }
    }

    private void initializeImages() {
      if (fgImagesInitialized) {
        return;
      }

      fgQuickFixImage = DartPluginImages.get(DartPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
      fgQuickFixErrorImage = DartPluginImages.get(DartPluginImages.IMG_OBJS_FIXABLE_ERROR);

      ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
      fgTaskImage = sharedImages.getImage(SharedImages.IMG_OBJS_TASK_TSK);
      fgInfoImage = DartToolsPlugin.getImage("icons/full/misc/info2.png");
      fgWarningImage = sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
      fgErrorImage = sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);

      fgImagesInitialized = true;
    }
  }

  /**
   * Annotation model dealing with java marker annotations and temporary problems. Also acts as
   * problem requester for its compilation unit. Initially inactive. Must explicitly be activated.
   */
  protected static class CompilationUnitAnnotationModel extends ResourceMarkerAnnotationModel
      implements ProblemRequestor, IProblemRequestorExtension {

    private static class ProblemRequestorState {
      boolean fInsideReportingSequence = false;
      List<Problem> fReportedProblems;
    }

    private ThreadLocal<ProblemRequestorState> fProblemRequestorState = new ThreadLocal<ProblemRequestorState>();
    private int fStateCount = 0;

    private CompilationUnit fCompilationUnit;
    private List<IJavaAnnotation> fGeneratedAnnotations = new ArrayList<IJavaAnnotation>();
    private IProgressMonitor fProgressMonitor;
    private boolean fIsActive = false;
    private boolean fIsHandlingTemporaryProblems;

    private ReverseMap fReverseMap = new ReverseMap();
    private List<IJavaAnnotation> fPreviouslyOverlaid = null;
    private List<IJavaAnnotation> fCurrentlyOverlaid = new ArrayList<IJavaAnnotation>();
    private Thread fActiveThread;

    public CompilationUnitAnnotationModel(IResource resource) {
      super(resource);
    }

    @Override
    public void acceptProblem(Problem problem) {
      // TODO(scheglov) restore when we will implement this feature
      if (fIsHandlingTemporaryProblems
      /*|| problem.getID() == DartSpellingReconcileStrategy.SPELLING_PROBLEM_ID*/) {
        ProblemRequestorState state = fProblemRequestorState.get();
        if (state != null) {
          state.fReportedProblems.add(problem);
        }
      }
    }

    @Override
    public void beginReporting() {
      ProblemRequestorState state = fProblemRequestorState.get();
      if (state == null) {
        internalBeginReporting(false);
      }
    }

    @Override
    public void beginReportingSequence() {
      ProblemRequestorState state = fProblemRequestorState.get();
      if (state == null) {
        internalBeginReporting(true);
      }
    }

    @Override
    public void endReporting() {
      ProblemRequestorState state = fProblemRequestorState.get();
      if (state != null && !state.fInsideReportingSequence) {
        internalEndReporting(state);
      }
    }

    @Override
    public void endReportingSequence() {
      ProblemRequestorState state = fProblemRequestorState.get();
      if (state != null && state.fInsideReportingSequence) {
        internalEndReporting(state);
      }
    }

    @Override
    public synchronized boolean isActive() {
      return fIsActive && fActiveThread == Thread.currentThread();
    }

    public void setCompilationUnit(CompilationUnit unit) {
      fCompilationUnit = unit;
    }

    @Override
    public synchronized void setIsActive(boolean isActive) {
      Assert.isLegal(!isActive || Display.getCurrent() == null); // must not be enabled from UI threads
      fIsActive = isActive;
      if (fIsActive) {
        fActiveThread = Thread.currentThread();
      } else {
        fActiveThread = null;
      }
    }

    @Override
    public void setIsHandlingTemporaryProblems(boolean enable) {
      if (fIsHandlingTemporaryProblems != enable) {
        fIsHandlingTemporaryProblems = enable;
        if (fIsHandlingTemporaryProblems) {
          startCollectingProblems();
        } else {
          stopCollectingProblems();
        }
      }

    }

    @Override
    public void setProgressMonitor(IProgressMonitor monitor) {
      fProgressMonitor = monitor;
    }

    @Override
    protected void addAnnotation(Annotation annotation, Position position, boolean fireModelChanged)
        throws BadLocationException {
      super.addAnnotation(annotation, position, fireModelChanged);

      synchronized (getLockObject()) {
        Object cached = fReverseMap.get(position);
        if (cached == null) {
          fReverseMap.put(position, annotation);
        } else if (cached instanceof List) {
          @SuppressWarnings("unchecked")
          List<Object> list = (List<Object>) cached;
          list.add(annotation);
        } else if (cached instanceof Annotation) {
          List<Object> list = new ArrayList<Object>(2);
          list.add(cached);
          list.add(annotation);
          fReverseMap.put(position, list);
        }
      }
    }

    @Override
    protected AnnotationModelEvent createAnnotationModelEvent() {
      return new CompilationUnitAnnotationModelEvent(this, getResource());
    }

    @Override
    protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
//      String markerType = MarkerUtilities.getMarkerType(marker);
//      if (markerType != null && markerType.startsWith(DartMarkerAnnotation.JAVA_MARKER_TYPE_PREFIX)) {
//        return new DartMarkerAnnotation(marker);
//      }
      // TODO(scheglov)
      if (DartMarkerAnnotation.isJavaAnnotation(marker)) {
        return new DartMarkerAnnotation(marker);
      }
      return super.createMarkerAnnotation(marker);
    }

    protected Position createPositionFromProblem(Problem problem) {
      int start = problem.getSourceStart();
      if (start < 0) {
        return null;
      }

      int length = problem.getSourceEnd() - problem.getSourceStart();
      if (length < 0) {
        return null;
      }

      return new Position(start, length);
    }

    @Override
    protected void removeAllAnnotations(boolean fireModelChanged) {
      super.removeAllAnnotations(fireModelChanged);
      synchronized (getLockObject()) {
        fReverseMap.clear();
      }
    }

    @Override
    protected void removeAnnotation(Annotation annotation, boolean fireModelChanged) {
      Position position = getPosition(annotation);
      synchronized (getLockObject()) {
        Object cached = fReverseMap.get(position);
        if (cached instanceof List) {
          @SuppressWarnings("unchecked")
          List<Object> list = (List<Object>) cached;
          list.remove(annotation);
          if (list.size() == 1) {
            fReverseMap.put(position, list.get(0));
            list.clear();
          }
        } else if (cached instanceof Annotation) {
          fReverseMap.remove(position);
        }
      }
      super.removeAnnotation(annotation, fireModelChanged);
    }

    private Object getAnnotations(Position position) {
      synchronized (getLockObject()) {
        return fReverseMap.get(position);
      }
    }

    /**
     * Sets up the infrastructure necessary for problem reporting.
     * 
     * @param insideReportingSequence <code>true</code> if this method call is issued from inside a
     *          reporting sequence
     */
    private void internalBeginReporting(boolean insideReportingSequence) {
      if (fCompilationUnit != null /*
                                    * &&
                                    * fCompilationUnit.getJavaProject().isOnClasspath(fCompilationUnit
                                    * )
                                    */) {
        ProblemRequestorState state = new ProblemRequestorState();
        state.fInsideReportingSequence = insideReportingSequence;
        state.fReportedProblems = new ArrayList<Problem>();
        synchronized (getLockObject()) {
          fProblemRequestorState.set(state);
          ++fStateCount;
        }
      }
    }

    private void internalEndReporting(ProblemRequestorState state) {
      int stateCount = 0;
      synchronized (getLockObject()) {
        --fStateCount;
        stateCount = fStateCount;
        fProblemRequestorState.set(null);
      }

      if (stateCount == 0) {
        reportProblems(state.fReportedProblems);
      }
    }

    private void overlayMarkers(Position position, ProblemAnnotation problemAnnotation) {
      Object value = getAnnotations(position);
      if (value instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;
        for (Iterator<Object> e = list.iterator(); e.hasNext();) {
          setOverlay(e.next(), problemAnnotation);
        }
      } else {
        setOverlay(value, problemAnnotation);
      }
    }

    private void removeMarkerOverlays(boolean isCanceled) {
      if (isCanceled) {
        fCurrentlyOverlaid.addAll(fPreviouslyOverlaid);
      } else if (fPreviouslyOverlaid != null) {
        Iterator<IJavaAnnotation> e = fPreviouslyOverlaid.iterator();
        while (e.hasNext()) {
          DartMarkerAnnotation annotation = (DartMarkerAnnotation) e.next();
          annotation.setOverlay(null);
        }
      }
    }

    /**
     * Signals the end of problem reporting.
     * 
     * @param reportedProblems the problems to report
     */
    private void reportProblems(List<Problem> reportedProblems) {
      if (fProgressMonitor != null && fProgressMonitor.isCanceled()) {
        return;
      }

      boolean temporaryProblemsChanged = false;

      synchronized (getLockObject()) {

        boolean isCanceled = false;

        fPreviouslyOverlaid = fCurrentlyOverlaid;
        fCurrentlyOverlaid = new ArrayList<IJavaAnnotation>();

        if (fGeneratedAnnotations.size() > 0) {
          temporaryProblemsChanged = true;
          removeAnnotations(fGeneratedAnnotations, false, true);
          fGeneratedAnnotations.clear();
        }

        if (reportedProblems != null && reportedProblems.size() > 0) {

          Iterator<Problem> e = reportedProblems.iterator();
          while (e.hasNext()) {

            if (fProgressMonitor != null && fProgressMonitor.isCanceled()) {
              isCanceled = true;
              break;
            }

            Problem problem = e.next();
            Position position = createPositionFromProblem(problem);
            if (position != null) {

              try {
                ProblemAnnotation annotation = new ProblemAnnotation(problem, fCompilationUnit);
                overlayMarkers(position, annotation);
                addAnnotation(annotation, position, false);
                fGeneratedAnnotations.add(annotation);

                temporaryProblemsChanged = true;
              } catch (BadLocationException x) {
                // ignore invalid position
              }
            }
          }
        }

        removeMarkerOverlays(isCanceled);
        fPreviouslyOverlaid = null;
      }

      if (temporaryProblemsChanged) {
        fireModelChanged();
      }
    }

    /**
     * Overlays value with problem annotation.
     * 
     * @param value the value
     * @param problemAnnotation
     */
    private void setOverlay(Object value, ProblemAnnotation problemAnnotation) {
      if (value instanceof DartMarkerAnnotation) {
        DartMarkerAnnotation annotation = (DartMarkerAnnotation) value;
        if (annotation.isProblem()) {
          annotation.setOverlay(problemAnnotation);
          fPreviouslyOverlaid.remove(annotation);
          fCurrentlyOverlaid.add(annotation);
        }
      } else {
      }
    }

    /**
     * Tells this annotation model to collect temporary problems from now on.
     */
    private void startCollectingProblems() {
      fGeneratedAnnotations.clear();
    }

    /**
     * Tells this annotation model to no longer collect temporary problems.
     */
    private void stopCollectingProblems() {
      if (fGeneratedAnnotations != null) {
        removeAnnotations(fGeneratedAnnotations, true, true);
      }
      fGeneratedAnnotations.clear();
    }
  }

  /**
   * Bundle of all required informations to allow working copy management.
   */
  static protected class CompilationUnitInfo extends FileInfo {
    public CompilationUnit fCopy;
  }

  protected static class GlobalAnnotationModelListener implements IAnnotationModelListener,
      IAnnotationModelListenerExtension {

    private ListenerList fListenerList;

    public GlobalAnnotationModelListener() {
      fListenerList = new ListenerList(ListenerList.IDENTITY);
    }

    public void addListener(IAnnotationModelListener listener) {
      fListenerList.add(listener);
    }

    @Override
    public void modelChanged(AnnotationModelEvent event) {
      Object[] listeners = fListenerList.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        Object curr = listeners[i];
        if (curr instanceof IAnnotationModelListenerExtension) {
          ((IAnnotationModelListenerExtension) curr).modelChanged(event);
        }
      }
    }

    @Override
    public void modelChanged(IAnnotationModel model) {
      Object[] listeners = fListenerList.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        ((IAnnotationModelListener) listeners[i]).modelChanged(model);
      }
    }

    public void removeListener(IAnnotationModelListener listener) {
      fListenerList.remove(listener);
    }
  }

  /**
   * Internal structure for mapping positions to some value. The reason for this specific structure
   * is that positions can change over time. Thus a lookup is based on value and not on hash value.
   */
  protected static class ReverseMap {

    static class Entry {
      Position fPosition;
      Object fValue;
    }

    private List<Entry> fList = new ArrayList<Entry>(2);
    private int fAnchor = 0;

    public ReverseMap() {
    }

    public void clear() {
      fList.clear();
    }

    public Object get(Position position) {

      Entry entry;

      // behind anchor
      int length = fList.size();
      for (int i = fAnchor; i < length; i++) {
        entry = fList.get(i);
        if (entry.fPosition.equals(position)) {
          fAnchor = i;
          return entry.fValue;
        }
      }

      // before anchor
      for (int i = 0; i < fAnchor; i++) {
        entry = fList.get(i);
        if (entry.fPosition.equals(position)) {
          fAnchor = i;
          return entry.fValue;
        }
      }

      return null;
    }

    public void put(Position position, Object value) {
      int index = getIndex(position);
      if (index == -1) {
        Entry entry = new Entry();
        entry.fPosition = position;
        entry.fValue = value;
        fList.add(entry);
      } else {
        Entry entry = fList.get(index);
        entry.fValue = value;
      }
    }

    public void remove(Position position) {
      int index = getIndex(position);
      if (index > -1) {
        fList.remove(index);
      }
    }

    private int getIndex(Position position) {
      Entry entry;
      int length = fList.size();
      for (int i = 0; i < length; i++) {
        entry = fList.get(i);
        if (entry.fPosition.equals(position)) {
          return i;
        }
      }
      return -1;
    }
  }

  /** Preference key for temporary problems */
  private final static String HANDLE_TEMPORARY_PROBLEMS = PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS;

  /** Indicates whether the save has been initialized by this provider */
  private boolean fIsAboutToSave = false;
  /** The save policy used by this provider */
  private ISavePolicy fSavePolicy;
  /** Internal property changed listener */
  private IPropertyChangeListener fPropertyListener;
  /** Annotation model listener added to all created CU annotation models */
  private GlobalAnnotationModelListener fGlobalAnnotationModelListener;
  /**
   * Element information of all connected elements with a fake CU but no file info.
   */
  private final Map<Object, CompilationUnitInfo> fFakeCUMapForMissingInfo = new HashMap<Object, CompilationUnitInfo>();

  /**
   * Constructor
   */
  public CompilationUnitDocumentProvider() {

    IDocumentProvider provider = new TextFileDocumentProvider();
    provider = new ForwardingDocumentProvider(
        DartPartitions.DART_PARTITIONING,
        new DartDocumentSetupParticipant(),
        provider);
    setParentDocumentProvider(provider);

    fGlobalAnnotationModelListener = new GlobalAnnotationModelListener();
    fPropertyListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (HANDLE_TEMPORARY_PROBLEMS.equals(event.getProperty())) {
          enableHandlingTemporaryProblems();
        }
      }
    };
    DartToolsPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
  }

  @Override
  public void addGlobalAnnotationModelListener(IAnnotationModelListener listener) {
    fGlobalAnnotationModelListener.addListener(listener);
  }

  @Override
  public void connect(Object element) throws CoreException {
    super.connect(element);
    if (getFileInfo(element) != null) {
      return;
    }

    CompilationUnitInfo info = fFakeCUMapForMissingInfo.get(element);
    if (info == null) {
      CompilationUnit cu = createFakeCompiltationUnit(element, true);
      if (cu == null) {
        return;
      }
      info = new CompilationUnitInfo();
      info.fCopy = cu;
      info.fElement = element;
      if (element instanceof IStorageEditorInput) {
        IStorage storage = ((IStorageEditorInput) element).getStorage();
        info.fModel = new StorageMarkerAnnotationModel(
            ResourcesPlugin.getWorkspace().getRoot(),
            storage.getName());
      } else {
        info.fModel = new AnnotationModel();
      }
      fFakeCUMapForMissingInfo.put(element, info);
    }
    info.fCount++;
  }

  @Override
  public ILineTracker createLineTracker(Object element) {
    return new DefaultLineTracker();
  }

  @Override
  public void disconnect(Object element) {
    CompilationUnitInfo info = fFakeCUMapForMissingInfo.get(element);
    if (info != null) {
      if (info.fCount == 1) {
        fFakeCUMapForMissingInfo.remove(element);
        info.fModel = null;
        // Destroy and unregister fake working copy
        try {
          info.fCopy.discardWorkingCopy();
        } catch (DartModelException ex) {
          handleCoreException(ex, ex.getMessage());
        }
      } else {
        info.fCount--;
      }
    }
    super.disconnect(element);
  }

  @Override
  public IAnnotationModel getAnnotationModel(Object element) {
    IAnnotationModel model = super.getAnnotationModel(element);
    if (model != null) {
      return model;
    }

    FileInfo info = fFakeCUMapForMissingInfo.get(element);
    if (info != null) {
      if (info.fModel != null) {
        return info.fModel;
      }
      if (info.fTextFileBuffer != null) {
        return info.fTextFileBuffer.getAnnotationModel();
      }
    }

    return null;
  }

  @Override
  public CompilationUnit getWorkingCopy(Object element) {
    FileInfo fileInfo = getFileInfo(element);
    if (fileInfo instanceof CompilationUnitInfo) {
      CompilationUnitInfo info = (CompilationUnitInfo) fileInfo;
      return info.fCopy;
    }
    CompilationUnitInfo cuInfo = fFakeCUMapForMissingInfo.get(element);
    if (cuInfo != null) {
      return cuInfo.fCopy;
    }

    return null;
  }

  @Override
  public boolean isModifiable(Object element) {
    if (element instanceof ExternalCompilationUnitEditorInput) {
      ExternalCompilationUnitEditorInput input = (ExternalCompilationUnitEditorInput) element;
      return input.isModifiable();
    } else if (element instanceof FileStoreEditorInput) {
      return false;
    } else {
      return super.isModifiable(element);
    }
  }

  @Override
  public boolean isReadOnly(Object element) {
    if (element instanceof ExternalCompilationUnitEditorInput) {
      ExternalCompilationUnitEditorInput input = (ExternalCompilationUnitEditorInput) element;
      return !input.isModifiable();
    } else if (element instanceof FileStoreEditorInput) {
      return true;
    } else {
      return super.isReadOnly(element);
    }
  }

  @Override
  public void removeGlobalAnnotationModelListener(IAnnotationModelListener listener) {
    fGlobalAnnotationModelListener.removeListener(listener);
  }

  @Override
  public void saveDocumentContent(IProgressMonitor monitor, Object element, IDocument document,
      boolean overwrite) throws CoreException {
    if (!fIsAboutToSave) {
      return;
    }
    super.saveDocument(monitor, element, document, overwrite);
  }

  @Override
  public void setSavePolicy(ISavePolicy savePolicy) {
    fSavePolicy = savePolicy;
  }

  @Override
  public void shutdown() {
    DartToolsPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(
        fPropertyListener);
    Iterator<?> e = getConnectedElementsIterator();
    while (e.hasNext()) {
      disconnect(e.next());
    }
    fFakeCUMapForMissingInfo.clear();
  }

  protected void commitWorkingCopy(IProgressMonitor monitor, Object element,
      final CompilationUnitInfo info, boolean overwrite) throws CoreException {

    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    monitor.beginTask("", 120); //$NON-NLS-1$

    try {
      final IProgressMonitor subMonitor1 = getSubProgressMonitor(monitor, 50);

      try {
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable ex) {
            IStatus status = new Status(
                IStatus.ERROR,
                DartUI.ID_PLUGIN,
                IStatus.OK,
                "Error in Dart Core during reconcile while saving", ex); //$NON-NLS-1$
            DartToolsPlugin.getDefault().getLog().log(status);
          }

          @Override
          public void run() {
            try {
              DartX.todo();
              ((CompilationUnitImpl) info.fCopy).reconcile(false, subMonitor1);
            } catch (DartModelException ex) {
              handleException(ex);
            } catch (OperationCanceledException ex) {
              // do not log this
            }
          }
        });
      } finally {
        subMonitor1.done();
      }

      IDocument document = info.fTextFileBuffer.getDocument();
      DartX.todo();
      boolean isSynchronized = true;
//      IResource resource = info.fCopy.getResource();
//
//      Assert.isTrue(resource instanceof IFile);
//
//      boolean isSynchronized = resource.isSynchronized(IResource.DEPTH_ZERO);
//
//      /*
//       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=98327 Make sure file gets
//       * save in commit() if the underlying file has been deleted
//       */
//      if (!isSynchronized && isDeleted(element))
//        info.fTextFileBuffer.setDirty(true);
//
//      if (!resource.exists()) {
//        // underlying resource has been deleted, just recreate file, ignore the
//        // rest
//        IProgressMonitor subMonitor2 = getSubProgressMonitor(monitor, 70);
//        try {
//          createFileFromDocument(subMonitor2, (IFile) resource, document);
//        } finally {
//          subMonitor2.done();
//        }
//        return;
//      }

      if (fSavePolicy != null) {
        fSavePolicy.preSave(info.fCopy);
      }

      IProgressMonitor subMonitor3 = getSubProgressMonitor(monitor, 50);
      try {
        fIsAboutToSave = true;
        info.fCopy.commitWorkingCopy(isSynchronized || overwrite, subMonitor3);
        notifyPostSaveListeners(info.fCopy, info, getSubProgressMonitor(monitor, 20));
      } catch (CoreException x) {
        // inform about the failure
        fireElementStateChangeFailed(element);
        throw x;
      } catch (RuntimeException x) {
        // inform about the failure
        fireElementStateChangeFailed(element);
        throw x;
      } finally {
        fIsAboutToSave = false;
        subMonitor3.done();
      }

      // If here, the dirty state of the editor will change to "not dirty".
      // Thus, the state changing flag will be reset.
      if (info.fModel instanceof AbstractMarkerAnnotationModel) {
        AbstractMarkerAnnotationModel model = (AbstractMarkerAnnotationModel) info.fModel;
        model.updateMarkers(document);
      }

      if (fSavePolicy != null) {
        CompilationUnit unit = fSavePolicy.postSave(info.fCopy);
        if (unit != null && info.fModel instanceof AbstractMarkerAnnotationModel) {
          IResource r = unit.getResource();
          IMarker[] markers = r.findMarkers(IMarker.MARKER, true, IResource.DEPTH_ZERO);
          if (markers != null && markers.length > 0) {
            AbstractMarkerAnnotationModel model = (AbstractMarkerAnnotationModel) info.fModel;
            for (int i = 0; i < markers.length; i++) {
              model.updateMarker(document, markers[i], null);
            }
          }
        }
      }
    } finally {
      monitor.done();
    }
  }

  @Override
  protected IAnnotationModel createAnnotationModel(IFile file) {
    return new CompilationUnitAnnotationModel(file);
  }

  /**
   * Creates a compilation unit from the given file.
   * 
   * @param file the file from which to create the compilation unit
   * @return the fake compilation unit
   */
  protected CompilationUnit createCompilationUnit(IFile file) {
    Object element = DartCore.create(file);
    if (element instanceof CompilationUnit) {
      return (CompilationUnit) element;
    }
    return null;
  }

  @Override
  protected FileInfo createEmptyFileInfo() {
    return new CompilationUnitInfo();
  }

  @Override
  @SuppressWarnings("deprecation")
  protected FileInfo createFileInfo(Object element) throws CoreException {
    CompilationUnit original = null;
    if (element instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) element;
      original = createCompilationUnit(input.getFile());
      if (original == null) {
        return null;
      }
    }

    FileInfo info = super.createFileInfo(element);
    if (!(info instanceof CompilationUnitInfo)) {
      return null;
    }

    if (original == null) {
      original = findCompilationUnit(element);
    }
    if (original == null) {
      original = createFakeCompiltationUnit(element, false);
    }
    if (original == null) {
      return null;
    }

    CompilationUnitInfo cuInfo = (CompilationUnitInfo) info;
    setUpSynchronization(cuInfo);

    ProblemRequestor requestor = cuInfo.fModel instanceof ProblemRequestor
        ? (ProblemRequestor) cuInfo.fModel : null;
    if (requestor instanceof IProblemRequestorExtension) {
      IProblemRequestorExtension extension = (IProblemRequestorExtension) requestor;
      extension.setIsActive(false);
      extension.setIsHandlingTemporaryProblems(isHandlingTemporaryProblems());
    }

    /*
     * Use the deprecated method to ensure that our problem requestor is used; it is the only way to
     * have as-you-type IProblems from reconciling appear in the annotation model.
     */
    if (DartModelUtil.isPrimary(original)) {
      original.becomeWorkingCopy(requestor, getProgressMonitor());
    }
    cuInfo.fCopy = original;

    if (cuInfo.fModel instanceof CompilationUnitAnnotationModel) {
      CompilationUnitAnnotationModel model = (CompilationUnitAnnotationModel) cuInfo.fModel;
      model.setCompilationUnit(cuInfo.fCopy);
    }

    if (cuInfo.fModel != null) {
      cuInfo.fModel.addAnnotationModelListener(fGlobalAnnotationModelListener);
    }

    return cuInfo;
  }

  @Override
  protected DocumentProviderOperation createSaveOperation(final Object element,
      final IDocument document, final boolean overwrite) throws CoreException {
    final FileInfo info = getFileInfo(element);
    if (info instanceof CompilationUnitInfo) {

      // Delegate handling of non-primary CUs
      CompilationUnit cu = ((CompilationUnitInfo) info).fCopy;
      if (cu != null && !DartModelUtil.isPrimary(cu)) {
        return super.createSaveOperation(element, document, overwrite);
      }

      if (info.fTextFileBuffer.getDocument() != document) {
        // the info exists, but not for the given document
        // -> saveAs was executed with a target that is already open
        // in another editor
        // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=85519
        Status status = new Status(
            IStatus.WARNING,
            EditorsUI.PLUGIN_ID,
            IStatus.ERROR,
            DartEditorMessages.CompilationUnitDocumentProvider_saveAsTargetOpenInEditor,
            null);
        throw new CoreException(status);
      }

      return new DocumentProviderOperation() {
        @Override
        public ISchedulingRule getSchedulingRule() {
          if (info.fElement instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) info.fElement).getFile();
            return computeSchedulingRule(file);
          } else {
            return null;
          }
        }

        @Override
        protected void execute(IProgressMonitor monitor) throws CoreException {
          commitWorkingCopy(monitor, element, (CompilationUnitInfo) info, overwrite);
        }
      };
    }

    return null;
  }

  @Override
  protected void disposeFileInfo(Object element, FileInfo info) {
    if (info instanceof CompilationUnitInfo) {
      CompilationUnitInfo cuInfo = (CompilationUnitInfo) info;

      try {
        cuInfo.fCopy.discardWorkingCopy();
      } catch (DartModelException x) {
        handleCoreException(x, x.getMessage());
      }

      if (cuInfo.fModel != null) {
        cuInfo.fModel.removeAnnotationModelListener(fGlobalAnnotationModelListener);
      }
    }
    super.disposeFileInfo(element, info);
  }

  /**
   * Switches the state of problem acceptance according to the value in the preference store.
   */
  protected void enableHandlingTemporaryProblems() {
    boolean enable = isHandlingTemporaryProblems();
    for (Iterator<?> iter = getFileInfosIterator(); iter.hasNext();) {
      FileInfo info = (FileInfo) iter.next();
      if (info.fModel instanceof IProblemRequestorExtension) {
        IProblemRequestorExtension extension = (IProblemRequestorExtension) info.fModel;
        extension.setIsHandlingTemporaryProblems(enable);
      }
    }
  }

  /**
   * Returns the preference whether handling temporary problems is enabled.
   * 
   * @return <code>true</code> if temporary problems are handled
   */
  protected boolean isHandlingTemporaryProblems() {
    IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
    return store.getBoolean(HANDLE_TEMPORARY_PROBLEMS);
  }

  /**
   * Notify post save listeners.
   * <p>
   * <strong>Note:</strong> Post save listeners are not allowed to save the file and they must not
   * assumed to be called in the UI thread i.e. if they open a dialog they must ensure it ends up in
   * the UI thread.
   * </p>
   * 
   * @param unit the compilation unit
   * @param info compilation unit info
   * @param monitor the progress monitor
   * @throws CoreException
   * @see IPostSaveListener
   */
  protected void notifyPostSaveListeners(final CompilationUnit unit,
      final CompilationUnitInfo info, final IProgressMonitor monitor) throws CoreException {
    final Buffer buffer = unit.getBuffer();
    IPostSaveListener[] listeners = DartToolsPlugin.getDefault().getSaveParticipantRegistry().getEnabledPostSaveListeners(
        unit.getDartProject().getProject());

    String message = DartEditorMessages.CompilationUnitDocumentProvider_error_saveParticipantProblem;
    final MultiStatus errorStatus = new MultiStatus(
        DartUI.ID_PLUGIN,
        DartStatusConstants.EDITOR_POST_SAVE_NOTIFICATION,
        message,
        null);

    monitor.beginTask(
        DartEditorMessages.CompilationUnitDocumentProvider_progressNotifyingSaveParticipants,
        listeners.length * 5);
    try {
      for (int i = 0; i < listeners.length; i++) {
        final IPostSaveListener listener = listeners[i];
        final String participantName = listener.getName();
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable ex) {
            String msg = Messages.format(
                "The save participant ''{0}'' caused an exception: {1}", new String[] {listener.getId(), ex.toString()}); //$NON-NLS-1$
            DartToolsPlugin.log(new Status(
                IStatus.ERROR,
                DartUI.ID_PLUGIN,
                DartStatusConstants.EDITOR_POST_SAVE_NOTIFICATION,
                msg,
                null));

            msg = Messages.format(
                DartEditorMessages.CompilationUnitDocumentProvider_error_saveParticipantFailed,
                new String[] {participantName, ex.toString()});
            errorStatus.add(new Status(
                IStatus.ERROR,
                DartUI.ID_PLUGIN,
                DartStatusConstants.EDITOR_POST_SAVE_NOTIFICATION,
                msg,
                null));

            // Revert the changes
            if (info != null && buffer.hasUnsavedChanges()) {
              try {
                info.fTextFileBuffer.revert(getSubProgressMonitor(monitor, 1));
              } catch (CoreException e) {
                msg = Messages.format(
                    "Error on revert after failure of save participant ''{0}''.", participantName); //$NON-NLS-1$
                IStatus status = new Status(
                    IStatus.ERROR,
                    DartUI.ID_PLUGIN,
                    DartStatusConstants.EDITOR_POST_SAVE_NOTIFICATION,
                    msg,
                    ex);
                DartToolsPlugin.getDefault().getLog().log(status);
              }

              if (info.fModel instanceof AbstractMarkerAnnotationModel) {
                AbstractMarkerAnnotationModel markerModel = (AbstractMarkerAnnotationModel) info.fModel;
                markerModel.resetMarkers();
              }
            }

            // XXX: Work in progress 'Save As' case
            // else if (buffer.hasUnsavedChanges()) {
            // try {
            // buffer.save(getSubProgressMonitor(monitor, 1), true);
            // } catch (DartModelException e) {
            //								message= Messages.format("Error reverting changes after failure of save participant ''{0}''.", participantName); //$NON-NLS-1$
            // IStatus status= new Status(IStatus.ERROR, DartUI.ID_PLUGIN,
            // IStatus.OK, message, ex);
            // DartToolsPlugin.getDefault().getLog().log(status);
            // }
            // }
          }

          @Override
          public void run() {
            try {
              long stamp = unit.getResource().getModificationStamp();

              listener.saved(unit, getSubProgressMonitor(monitor, 4));

              if (stamp != unit.getResource().getModificationStamp()) {
                String msg = Messages.format(
                    DartEditorMessages.CompilationUnitDocumentProvider_error_saveParticipantSavedFile,
                    participantName);
                errorStatus.add(new Status(
                    IStatus.ERROR,
                    DartUI.ID_PLUGIN,
                    DartStatusConstants.EDITOR_POST_SAVE_NOTIFICATION,
                    msg,
                    null));
              }

              if (buffer.hasUnsavedChanges()) {
                buffer.save(getSubProgressMonitor(monitor, 1), true);
              }

            } catch (CoreException ex) {
              handleException(ex);
            } finally {
              monitor.worked(1);
            }
          }
        });
      }
    } finally {
      monitor.done();
      if (!errorStatus.isOK()) {
        throw new CoreException(errorStatus);
      }
    }
  }

  /**
   * Creates a fake compilation unit.
   * 
   * @param editorInput the storage editor input
   * @param setContents tells whether to read and set the contents to the new CU
   * @return the fake compilation unit
   */
  private CompilationUnit createFakeCompilationUnit(IStorageEditorInput editorInput,
      boolean setContents) {
    try {
      final IStorage storage = editorInput.getStorage();
      final IPath storagePath = storage.getFullPath();
      if (storage.getName() == null || storagePath == null) {
        if (storagePath == null) {
          Logger.log(Logger.INFO, "Unsupported editor input: null path"); //$NON-NLS-1$
        } else if (storage.getName() == null) {
          Logger.log(Logger.INFO, "Unsupported editor input: no name"); //$NON-NLS-1$
        }
        return null;
      }

      final IPath documentPath;
      if (storage instanceof IFileState) {
        documentPath = storagePath.append(Long.toString(((IFileState) storage).getModificationTime()));
      } else {
        documentPath = storagePath;
      }

      WorkingCopyOwner woc = new WorkingCopyOwner() {
        @Override
        public Buffer createBuffer(SourceFileElement<?> workingCopy) {
          return new DocumentAdapter(workingCopy, documentPath);
        }
      };

      // IIncludePathEntry[] cpEntries = null;
      // DartProject jp = findJavaProject(storagePath);
      // if (jp != null)
      // cpEntries = jp.getResolvedIncludepath(true);
      //
      // if (cpEntries == null || cpEntries.length == 0)
      // cpEntries = new
      // IIncludePathEntry[]{JavaRuntime.getDefaultJREContainerEntry()};

      final CompilationUnit cu = woc.newWorkingCopy(storage.getName(), getProgressMonitor());
      if (setContents) {
        int READER_CHUNK_SIZE = 2048;
        int BUFFER_SIZE = 8 * READER_CHUNK_SIZE;

        String charsetName = null;
        if (storage instanceof IEncodedStorage) {
          charsetName = ((IEncodedStorage) storage).getCharset();
        }
        if (charsetName == null) {
          charsetName = getDefaultEncoding();
        }

        Reader in = null;
        InputStream contents = storage.getContents();
        try {
          in = new BufferedReader(new InputStreamReader(contents, charsetName));
          StringBuffer buffer = new StringBuffer(BUFFER_SIZE);
          char[] readBuffer = new char[READER_CHUNK_SIZE];
          int n;
          n = in.read(readBuffer);
          while (n > 0) {
            buffer.append(readBuffer, 0, n);
            n = in.read(readBuffer);
          }
          cu.getBuffer().setContents(buffer.toString());
        } catch (IOException e) {
          DartToolsPlugin.log(e);
          return null;
        } catch (Exception e) {
          DartX.todo("this is to work around parser bugs");
          DartToolsPlugin.log(e);
          return null;
        } finally {
          try {
            if (in != null) {
              in.close();
            } else {
              contents.close();
            }
          } catch (IOException x) {
          }
        }
      }

      if (!isModifiable(editorInput)) {
        DartModelUtil.reconcile(cu);
      }

      return cu;
    } catch (CoreException ex) {
      DartToolsPlugin.log(ex.getStatus());
      return null;
    }
  }

  /**
   * Creates a fake compilation unit.
   * 
   * @param editorInput the URI editor input
   * @return the fake compilation unit
   */
  private CompilationUnit createFakeCompilationUnit(IURIEditorInput editorInput) {
    try {
      final URI uri = editorInput.getURI();
      final IFileStore fileStore = EFS.getStore(uri);
      final IPath path = URIUtil.toPath(uri);
      if (fileStore.getName() == null || path == null) {
        return null;
      }

      WorkingCopyOwner woc = new WorkingCopyOwner() {
        @Override
        public Buffer createBuffer(SourceFileElement<?> workingCopy) {
          return new DocumentAdapter(workingCopy, path);
        }
      };

      // IIncludePathEntry[] cpEntries = null;
      // DartProject jp = findJavaProject(path);
      // if (jp != null)
      // cpEntries = jp.getResolvedIncludepath(true);
      //
      // if (cpEntries == null || cpEntries.length == 0)
      // cpEntries = new
      // IIncludePathEntry[]{JavaRuntime.getDefaultJREContainerEntry()};

      final CompilationUnitImpl cu = createWorkingCopy(uri, fileStore.getName(), woc);

      if (!isModifiable(editorInput)) {
        DartModelUtil.reconcile(cu);
      }

      return cu;
    } catch (CoreException ex) {
      return null;
    }
  }

  /**
   * Creates a fake compilation unit.
   * 
   * @param element the element
   * @param setContents tells whether to read and set the contents to the new CU
   * @return the fake compilation unit
   */
  private CompilationUnit createFakeCompiltationUnit(Object element, boolean setContents) {
    if (element instanceof IStorageEditorInput) {
      return createFakeCompilationUnit((IStorageEditorInput) element, setContents);
    } else if (element instanceof IURIEditorInput) {
      return createFakeCompilationUnit((IURIEditorInput) element);
    }
    return null;
  }

/**
   * Create a new working copy for the file being edited. This is a replacement for the method
   * {@link WorkingCopyOwner#newWorkingCopy(String, IProgressMonitor) that allows us to use the URI
   * of the file being edited rather than synthesizing one from the current working directory and
   * the file name (which produces invalid URI's).
   * 
   * @param uri the URI of the file being edited
   * @param fileName the name of the file being edited
   * @param owner the working copy owner to be associated with the working copy
   * @return the working copy that was created
   * @throws DartModelException
   */
  private CompilationUnitImpl createWorkingCopy(URI uri, String fileName, WorkingCopyOwner owner)
      throws DartModelException {
    ExternalDartProject project = new ExternalDartProject();
    IFile libraryFile = project.getProject().getFile(fileName);
    LibrarySource sourceFile = new UrlLibrarySource(
        uri,
        PackageLibraryManagerProvider.getPackageLibraryManager());
    DartLibraryImpl parent = new DartLibraryImpl(project, libraryFile, sourceFile);
    final CompilationUnitImpl cu = new CompilationUnitImpl(parent, libraryFile, owner);
    cu.becomeWorkingCopy(owner.getProblemRequestor(cu), getProgressMonitor());
    return cu;
  }

  /**
   * Search through all of the bundled libraries to find a compilation unit matching the given
   * input.
   * 
   * @param editorInput the input for which a compilation unit is being sought
   * @return the compilation unit that was found, or <code>null</code> if no compilation unit could
   *         be found
   */
  private CompilationUnit findCompilationUnit(IURIEditorInput editorInput) {
    if (editorInput instanceof ExternalCompilationUnitEditorInput) {
      return ((ExternalCompilationUnitEditorInput) editorInput).getCompilationUnit();
    }
    try {
      URI uri = editorInput.getURI();
      if (uri == null) {
        return null;
      }
      for (DartLibrary library : DartModelManager.getInstance().getDartModel().getBundledLibraries()) {
        for (CompilationUnit unit : library.getCompilationUnits()) {
          URI unitUri = URIUtilities.safelyResolveDartUri(unit.getSourceRef().getUri());
          if (uri.equals(unitUri)) {
            return unit;
          }
        }
      }
    } catch (DartModelException exception) {
      // Could not get the bundled libraries, so fall through to return null.
    }
    return null;
  }

  /**
   * Search through all of the bundled libraries to find a compilation unit matching the given
   * input.
   * 
   * @param editorInput the input for which a compilation unit is being sought
   * @return the compilation unit that was found, or <code>null</code> if no compilation unit could
   *         be found
   */
  private CompilationUnit findCompilationUnit(Object editorInput/* , boolean setContents */) {
//    if (element instanceof IStorageEditorInput) {
//      return findCompiltationUnit((IStorageEditorInput) element, setContents);
//    } else
    if (editorInput instanceof IURIEditorInput) {
      return findCompilationUnit((IURIEditorInput) editorInput);
    }
    return null;
  }

  /**
   * Fuzzy search for Java project in the workspace that matches the given path.
   * 
   * @param path the path to match
   * @return the matching Java project or <code>null</code>
   */
  @SuppressWarnings("unused")
  private DartProject findJavaProject(IPath path) {
    if (path == null) {
      return null;
    }

    String[] pathSegments = path.segments();
    DartModel model = DartCore.create(DartToolsPlugin.getWorkspace().getRoot());
    DartProject[] projects;
    try {
      projects = model.getDartProjects();
    } catch (DartModelException e) {
      return null; // ignore - use default JRE
    }
    for (int i = 0; i < projects.length; i++) {
      IPath projectPath = projects[i].getProject().getFullPath();
      String projectSegment = projectPath.segments()[0];
      for (int j = 0; j < pathSegments.length; j++) {
        if (projectSegment.equals(pathSegments[j])) {
          return projects[i];
        }
      }
    }
    return null;
  }

  /**
   * Creates and returns a new sub-progress monitor for the given parent monitor.
   * 
   * @param monitor the parent progress monitor
   * @param ticks the number of work ticks allocated from the parent monitor
   * @return the new sub-progress monitor
   */
  private IProgressMonitor getSubProgressMonitor(IProgressMonitor monitor, int ticks) {
    if (monitor != null) {
      return new SubProgressMonitor(
          monitor,
          ticks,
          SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
    }

    return new NullProgressMonitor();
  }

}
