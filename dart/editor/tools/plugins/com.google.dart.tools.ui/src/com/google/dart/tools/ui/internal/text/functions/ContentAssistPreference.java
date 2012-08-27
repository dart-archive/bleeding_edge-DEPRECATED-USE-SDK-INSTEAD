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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProcessor;
import com.google.dart.tools.ui.text.DartTextTools;
import com.google.dart.tools.ui.text.IColorManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public class ContentAssistPreference {

  /** Preference key for content assist auto activation */
  private final static String AUTOACTIVATION = PreferenceConstants.CODEASSIST_AUTOACTIVATION;
  /** Preference key for content assist auto activation delay */
  private final static String AUTOACTIVATION_DELAY = PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY;
  /** Preference key for content assist proposal color */
  private final static String PROPOSALS_FOREGROUND = PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND;
  /** Preference key for content assist proposal color */
  private final static String PROPOSALS_BACKGROUND = PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND;
  /** Preference key for content assist parameters color */
  private final static String PARAMETERS_FOREGROUND = PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND;
  /** Preference key for content assist parameters color */
  private final static String PARAMETERS_BACKGROUND = PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND;
  /** Preference key for content assist auto insert */
  private final static String AUTOINSERT = PreferenceConstants.CODEASSIST_AUTOINSERT;

  /** Preference key for java content assist auto activation triggers */
  private final static String AUTOACTIVATION_TRIGGERS_JAVA = PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA;
  /** Preference key for jsdoc content assist auto activation triggers */
  @SuppressWarnings("unused")
  private final static String AUTOACTIVATION_TRIGGERS_JAVADOC = PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC;

  /** Preference key for visibility of proposals */
  private final static String SHOW_VISIBLE_PROPOSALS = PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS;
  /** Preference key for case sensitivity of proposals */
  private final static String CASE_SENSITIVITY = PreferenceConstants.CODEASSIST_CASE_SENSITIVITY;
  /** Preference key for adding imports on code assist */
  /** Preference key for filling argument names on method completion */
  private static final String FILL_METHOD_ARGUMENTS = PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES;
  /** Preference key for prefix completion. */
  private static final String PREFIX_COMPLETION = PreferenceConstants.CODEASSIST_PREFIX_COMPLETION;

  /**
   * Changes the configuration of the given content assistant according to the given property change
   * event and the given preference store.
   */
  public static void changeConfiguration(ContentAssistant assistant, IPreferenceStore store,
      PropertyChangeEvent event) {

    String p = event.getProperty();

    if (AUTOACTIVATION.equals(p)) {
      boolean enabled = store.getBoolean(AUTOACTIVATION);
      assistant.enableAutoActivation(enabled);
    } else if (AUTOACTIVATION_DELAY.equals(p)) {
      int delay = store.getInt(AUTOACTIVATION_DELAY);
      assistant.setAutoActivationDelay(delay);
    } else if (PROPOSALS_FOREGROUND.equals(p)) {
      Color c = getColor(store, PROPOSALS_FOREGROUND);
      assistant.setProposalSelectorForeground(c);
    } else if (PROPOSALS_BACKGROUND.equals(p)) {
      Color c = getColor(store, PROPOSALS_BACKGROUND);
      assistant.setProposalSelectorBackground(c);
    } else if (PARAMETERS_FOREGROUND.equals(p)) {
      Color c = getColor(store, PARAMETERS_FOREGROUND);
      assistant.setContextInformationPopupForeground(c);
      assistant.setContextSelectorForeground(c);
    } else if (PARAMETERS_BACKGROUND.equals(p)) {
      Color c = getColor(store, PARAMETERS_BACKGROUND);
      assistant.setContextInformationPopupBackground(c);
      assistant.setContextSelectorBackground(c);
    } else if (AUTOINSERT.equals(p)) {
      boolean enabled = store.getBoolean(AUTOINSERT);
      assistant.enableAutoInsert(enabled);
    } else if (PREFIX_COMPLETION.equals(p)) {
      boolean enabled = store.getBoolean(PREFIX_COMPLETION);
      assistant.enablePrefixCompletion(enabled);
    }

    changeJavaProcessor(assistant, store, p);
    changeJavaDocProcessor(assistant, store, p);
  }

  /**
   * Configure the given content assistant from the given store.
   */
  public static void configure(ContentAssistant assistant, IPreferenceStore store) {

    DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();
    IColorManager manager = textTools.getColorManager();

    boolean enabled = store.getBoolean(AUTOACTIVATION);
    assistant.enableAutoActivation(enabled);

    int delay = store.getInt(AUTOACTIVATION_DELAY);
    assistant.setAutoActivationDelay(delay);

    Color c = getColor(store, PROPOSALS_FOREGROUND, manager);
    assistant.setProposalSelectorForeground(c);

    c = getColor(store, PROPOSALS_BACKGROUND, manager);
    assistant.setProposalSelectorBackground(c);

    c = getColor(store, PARAMETERS_FOREGROUND, manager);
    assistant.setContextInformationPopupForeground(c);
    assistant.setContextSelectorForeground(c);

    c = getColor(store, PARAMETERS_BACKGROUND, manager);
    assistant.setContextInformationPopupBackground(c);
    assistant.setContextSelectorBackground(c);

    enabled = store.getBoolean(AUTOINSERT);
    assistant.enableAutoInsert(enabled);

    enabled = store.getBoolean(PREFIX_COMPLETION);
    assistant.enablePrefixCompletion(enabled);

    configureJavaProcessor(assistant, store);
    DartX.todo("dartdoc");
    // configureJavaDocProcessor(assistant, store);
  }

  public static boolean fillArgumentsOnMethodCompletion(IPreferenceStore store) {
    return store.getBoolean(FILL_METHOD_ARGUMENTS);
  }

  private static void changeJavaDocProcessor(ContentAssistant assistant, IPreferenceStore store,
      String key) {
    DartX.todo();
//    JavadocCompletionProcessor jdcp = getJavaDocProcessor(assistant);
//    if (jdcp == null)
//      return;
//
//    if (AUTOACTIVATION_TRIGGERS_JAVADOC.equals(key)) {
//      String triggers = store.getString(AUTOACTIVATION_TRIGGERS_JAVADOC);
//      if (triggers != null)
//        jdcp.setCompletionProposalAutoActivationCharacters(triggers.toCharArray());
//    } else if (CASE_SENSITIVITY.equals(key)) {
//      boolean enabled = store.getBoolean(CASE_SENSITIVITY);
//      jdcp.restrictProposalsToMatchingCases(enabled);
//    }
  }

  private static void changeJavaProcessor(ContentAssistant assistant, IPreferenceStore store,
      String key) {
    DartCompletionProcessor jcp = getJavaProcessor(assistant);
    if (jcp == null) {
      return;
    }

    if (AUTOACTIVATION_TRIGGERS_JAVA.equals(key)) {
      String triggers = store.getString(AUTOACTIVATION_TRIGGERS_JAVA);
      if (triggers != null) {
        jcp.setCompletionProposalAutoActivationCharacters(triggers.toCharArray());
      }
    } else if (SHOW_VISIBLE_PROPOSALS.equals(key)) {
      boolean enabled = store.getBoolean(SHOW_VISIBLE_PROPOSALS);
      jcp.restrictProposalsToVisibility(enabled);
    } else if (CASE_SENSITIVITY.equals(key)) {
      boolean enabled = store.getBoolean(CASE_SENSITIVITY);
      jcp.restrictProposalsToMatchingCases(enabled);
    }
  }

  @SuppressWarnings("unused")
  private static void configureJavaDocProcessor(ContentAssistant assistant, IPreferenceStore store) {
    DartX.todo();
//    JavadocCompletionProcessor jdcp = getJavaDocProcessor(assistant);
//    if (jdcp == null)
//      return;
//
//    String triggers = store.getString(AUTOACTIVATION_TRIGGERS_JAVADOC);
//    if (triggers != null)
//      jdcp.setCompletionProposalAutoActivationCharacters(triggers.toCharArray());
//
//    boolean enabled = store.getBoolean(CASE_SENSITIVITY);
//    jdcp.restrictProposalsToMatchingCases(enabled);
  }

  private static void configureJavaProcessor(ContentAssistant assistant, IPreferenceStore store) {
    DartCompletionProcessor jcp = getJavaProcessor(assistant);
    if (jcp == null) {
      return;
    }

    String triggers = store.getString(AUTOACTIVATION_TRIGGERS_JAVA);
    if (triggers != null) {
      jcp.setCompletionProposalAutoActivationCharacters(triggers.toCharArray());
    }

    boolean enabled = store.getBoolean(SHOW_VISIBLE_PROPOSALS);
    jcp.restrictProposalsToVisibility(enabled);

    enabled = store.getBoolean(CASE_SENSITIVITY);
    jcp.restrictProposalsToMatchingCases(enabled);
  }

  private static Color getColor(IPreferenceStore store, String key) {
    DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();
    return getColor(store, key, textTools.getColorManager());
  }

  private static Color getColor(IPreferenceStore store, String key, IColorManager manager) {
    RGB rgb = PreferenceConverter.getColor(store, key);
    return manager.getColor(rgb);
  }

//  private static JavadocCompletionProcessor getJavaDocProcessor(
//      ContentAssistant assistant) {
//    IContentAssistProcessor p = assistant.getContentAssistProcessor(DartPartitions.DART_DOC);
//    if (p instanceof JavadocCompletionProcessor)
//      return (JavadocCompletionProcessor) p;
//    return null;
//  }

  private static DartCompletionProcessor getJavaProcessor(ContentAssistant assistant) {
    IContentAssistProcessor p = assistant.getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
    if (p instanceof DartCompletionProcessor) {
      return (DartCompletionProcessor) p;
    }
    return null;
  }
}
