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
package com.google.dart.tools.ui.text;

import com.google.dart.tools.ui.internal.text.dart.DartCodeScanner;
import com.google.dart.tools.ui.internal.text.dartdoc.DartDocScanner;
import com.google.dart.tools.ui.internal.text.functions.DartColorManager;
import com.google.dart.tools.ui.internal.text.functions.DartCommentScanner;
import com.google.dart.tools.ui.internal.text.functions.DartMultilineStringScanner;
import com.google.dart.tools.ui.internal.text.functions.FastDartPartitionScanner;
import com.google.dart.tools.ui.internal.text.functions.SingleTokenDartScanner;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Tools required to configure a Dart text viewer. The color manager and all scanners are
 * singletons.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
@SuppressWarnings("deprecation")
public class DartTextTools {

  /**
   * This tools' preference listener.
   */
  private class PreferenceListener implements IPropertyChangeListener,
      Preferences.IPropertyChangeListener {
    @Override
    public void propertyChange(Preferences.PropertyChangeEvent event) {
      adaptToPreferenceChange(new PropertyChangeEvent(
          event.getSource(),
          event.getProperty(),
          event.getOldValue(),
          event.getNewValue()));
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      adaptToPreferenceChange(event);
    }
  }

  /**
   * Array with legal content types.
   */
  private final static String[] LEGAL_CONTENT_TYPES = new String[] {
      DartPartitions.DART_DOC, DartPartitions.DART_MULTI_LINE_COMMENT,
      DartPartitions.DART_SINGLE_LINE_COMMENT, DartPartitions.DART_SINGLE_LINE_DOC,
      DartPartitions.DART_STRING, DartPartitions.DART_MULTI_LINE_STRING};

  /** The color manager. */
  private DartColorManager colorManager;
  /** The Dart source code scanner. */
  private DartCodeScanner codeScanner;
  /** The Dart multi-line comment scanner. */
  private DartCommentScanner multilineCommentScanner;
  /** The Dart single-line comment scanner. */
  private DartCommentScanner singlelineCommentScanner;
  /** The Dart string scanner. */
  private SingleTokenDartScanner stringScanner;
  /** The Dart multi-line scanner */
  private DartMultilineStringScanner multilineStringScanner;
  /** The Doc scanner. */
  private DartDocScanner dartDocScanner;
  /** The preference store. */
  private IPreferenceStore preferenceStore;
  /**
   * The core preference store.
   */
  private Preferences corePreferenceStore;
  /** The preference change listener */
  private PreferenceListener preferenceListener = new PreferenceListener();

  /**
   * Creates a new JavaScript text tools collection.
   * 
   * @param store the preference store to initialize the text tools. The text tool instance installs
   *          a listener on the passed preference store to adapt itself to changes in the preference
   *          store. In general <code>PreferenceConstants.
   * 			getPreferenceStore()</code> should be used to initialize the text tools.
   * @see com.google.dart.tools.ui.PreferenceConstants#getPreferenceStore()
   */
  public DartTextTools(IPreferenceStore store) {
    this(store, null, true);
  }

  /**
   * Creates a new JavaScript text tools collection.
   * 
   * @param store the preference store to initialize the text tools. The text tool instance installs
   *          a listener on the passed preference store to adapt itself to changes in the preference
   *          store. In general <code>PreferenceConstants.
   * 			getPreferenceStore()</code> should be used to initialize the text tools.
   * @param autoDisposeOnDisplayDispose if <code>true</code> the color manager automatically
   *          disposes all managed colors when the current display gets disposed and all calls to
   *          {@link org.eclipse.jface.text.source.ISharedTextColors#dispose()} are ignored.
   * @see com.google.dart.tools.ui.PreferenceConstants#getPreferenceStore()
   */
  public DartTextTools(IPreferenceStore store, boolean autoDisposeOnDisplayDispose) {
    this(store, null, autoDisposeOnDisplayDispose);
  }

  /**
   * Creates a new JavaScript text tools collection.
   * 
   * @param store the preference store to initialize the text tools. The text tool instance installs
   *          a listener on the passed preference store to adapt itself to changes in the preference
   *          store. In general <code>PreferenceConstants.
   * 			getPreferenceStore()</code> should be used to initialize the text tools.
   * @param coreStore optional preference store to initialize the text tools. The text tool instance
   *          installs a listener on the passed preference store to adapt itself to changes in the
   *          preference store.
   * @see com.google.dart.tools.ui.PreferenceConstants#getPreferenceStore()
   */
  public DartTextTools(IPreferenceStore store, Preferences coreStore) {
    this(store, coreStore, true);
  }

  /**
   * Creates a new JavaScript text tools collection.
   * 
   * @param store the preference store to initialize the text tools. The text tool instance installs
   *          a listener on the passed preference store to adapt itself to changes in the preference
   *          store. In general <code>PreferenceConstants.
   * 			getPreferenceStore()</code> should be used to initialize the text tools.
   * @param coreStore optional preference store to initialize the text tools. The text tool instance
   *          installs a listener on the passed preference store to adapt itself to changes in the
   *          preference store.
   * @param autoDisposeOnDisplayDispose if <code>true</code> the color manager automatically
   *          disposes all managed colors when the current display gets disposed and all calls to
   *          {@link org.eclipse.jface.text.source.ISharedTextColors#dispose()} are ignored.
   * @see com.google.dart.tools.ui.PreferenceConstants#getPreferenceStore()
   */
  public DartTextTools(IPreferenceStore store, Preferences coreStore,
      boolean autoDisposeOnDisplayDispose) {
    colorManager = new DartColorManager(autoDisposeOnDisplayDispose);
    codeScanner = new DartCodeScanner(colorManager, store);

    preferenceStore = store;
    preferenceStore.addPropertyChangeListener(preferenceListener);

    corePreferenceStore = coreStore;
    if (corePreferenceStore != null) {
      corePreferenceStore.addPropertyChangeListener(preferenceListener);
    }

    multilineCommentScanner = new DartCommentScanner(
        colorManager,
        store,
        coreStore,
        IDartColorConstants.JAVA_MULTI_LINE_COMMENT);
    singlelineCommentScanner = new DartCommentScanner(
        colorManager,
        store,
        coreStore,
        IDartColorConstants.JAVA_SINGLE_LINE_COMMENT);
    stringScanner = new SingleTokenDartScanner(colorManager, store, IDartColorConstants.JAVA_STRING);
    multilineStringScanner = new DartMultilineStringScanner(
        colorManager,
        store,
        IDartColorConstants.DART_MULTI_LINE_STRING);
    dartDocScanner = new DartDocScanner(colorManager, store, coreStore);
  }

  /**
   * Factory method for creating a Java-specific document partitioner using this object's partitions
   * scanner. This method is a convenience method.
   * 
   * @return a newly created JavaScript document partitioner
   */
  public IDocumentPartitioner createDocumentPartitioner() {
    return new FastPartitioner(getPartitionScanner(), LEGAL_CONTENT_TYPES);
  }

  /**
   * Disposes all the individual tools of this tools collection.
   */
  public void dispose() {

    codeScanner = null;
    multilineCommentScanner = null;
    singlelineCommentScanner = null;
    stringScanner = null;
    dartDocScanner = null;

    if (colorManager != null) {
      colorManager.dispose();
      colorManager = null;
    }

    if (preferenceStore != null) {
      preferenceStore.removePropertyChangeListener(preferenceListener);
      preferenceStore = null;

      if (corePreferenceStore != null) {
        corePreferenceStore.removePropertyChangeListener(preferenceListener);
        corePreferenceStore = null;
      }

      preferenceListener = null;
    }
  }

  /**
   * Returns the color manager which is used to manage any Java-specific colors needed for such
   * things like syntax highlighting.
   * <p>
   * Clients which are only interested in the color manager of the JavaScript UI plug-in should use
   * {@link com.google.dart.tools.ui.DartUI#getColorManager()}.
   * </p>
   * 
   * @return the color manager to be used for JavaScript text viewers
   * @see com.google.dart.tools.ui.DartUI#getColorManager()
   */
  public IColorManager getColorManager() {
    return colorManager;
  }

  /**
   * Returns a scanner which is configured to scan Java-specific partitions, which are multi-line
   * comments, Javadoc comments, and regular JavaScript source code.
   * 
   * @return a JavaScript partition scanner
   */
  public IPartitionTokenScanner getPartitionScanner() {
    return new FastDartPartitionScanner();
  }

  /**
   * Sets up the JavaScript document partitioner for the given document for the default
   * partitioning.
   * 
   * @param document the document to be set up
   */
  public void setupJavaDocumentPartitioner(IDocument document) {
    setupDartDocumentPartitioner(document, IDocumentExtension3.DEFAULT_PARTITIONING);
  }

  /**
   * Sets up the JavaScript document partitioner for the given document for the given partitioning.
   * 
   * @param document the document to be set up
   * @param partitioning the document partitioning
   */
  public void setupDartDocumentPartitioner(IDocument document, String partitioning) {
    IDocumentPartitioner partitioner = createDocumentPartitioner();
    if (document instanceof IDocumentExtension3) {
      IDocumentExtension3 extension3 = (IDocumentExtension3) document;
      extension3.setDocumentPartitioner(partitioning, partitioner);
    } else {
      document.setDocumentPartitioner(partitioner);
    }
    partitioner.connect(document);
  }

  /**
   * Adapts the behavior of the contained components to the change encoded in the given event.
   * 
   * @param event the event to which to adapt
   * @deprecated As of 3.0, no replacement
   */
  @Deprecated
  protected void adaptToPreferenceChange(PropertyChangeEvent event) {
    if (codeScanner.affectsBehavior(event)) {
      codeScanner.adaptToPreferenceChange(event);
    }
    if (multilineCommentScanner.affectsBehavior(event)) {
      multilineCommentScanner.adaptToPreferenceChange(event);
    }
    if (singlelineCommentScanner.affectsBehavior(event)) {
      singlelineCommentScanner.adaptToPreferenceChange(event);
    }
    if (stringScanner.affectsBehavior(event)) {
      stringScanner.adaptToPreferenceChange(event);
    }
    if (multilineStringScanner.affectsBehavior(event)) {
      multilineStringScanner.adaptToPreferenceChange(event);
    }
    if (dartDocScanner.affectsBehavior(event)) {
      dartDocScanner.adaptToPreferenceChange(event);
    }
  }

  /**
   * Returns this text tool's core preference store.
   * 
   * @return the core preference store
   */
  protected Preferences getCorePreferenceStore() {
    return corePreferenceStore;
  }

  /**
   * Returns this text tool's preference store.
   * 
   * @return the preference store
   */
  protected IPreferenceStore getPreferenceStore() {
    return preferenceStore;
  }
}
