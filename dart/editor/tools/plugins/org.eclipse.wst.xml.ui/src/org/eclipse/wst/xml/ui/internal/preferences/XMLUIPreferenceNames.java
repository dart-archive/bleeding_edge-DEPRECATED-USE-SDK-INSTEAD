/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.preferences;

/**
 * Preference keys for XML UI
 */
public class XMLUIPreferenceNames {

  public final static String SUGGESTION_STRATEGY_VALUE_LAX = "Lax"; //$NON-NLS-1$
  public final static String SUGGESTION_STRATEGY_VALUE_STRICT = "Strict"; //$NON-NLS-1$

  /**
   * A named preference that controls time before code assist gets auto activated.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String AUTO_PROPOSE_DELAY = "autoProposeDelay";//$NON-NLS-1$

  /**
   * A named preference that controls if code assist gets auto activated.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String AUTO_PROPOSE = getAutoProposeKey();

  private static String getAutoProposeKey() {
    return "autoPropose";//$NON-NLS-1$
  }

  /**
   * A named preference that holds the characters that auto activate code assist.
   * <p>
   * Value is of type <code>String</code>. All characters that trigger auto code assist.
   * </p>
   */
  public static final String AUTO_PROPOSE_CODE = getAutoProposeCodeKey();

  private static String getAutoProposeCodeKey() {
    return "autoProposeCode";//$NON-NLS-1$
  }

  /**
   * The key to store customized templates.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String TEMPLATES_KEY = getTemplatesKey();

  private static String getTemplatesKey() {
    return "org.eclipse.wst.sse.ui.custom_templates"; //$NON-NLS-1$
  }

  /**
   * A named preference that controls whether or grammar should be inferred or not.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String USE_INFERRED_GRAMMAR = getUseInferredGrammarKey();

  public static class OUTLINE_BEHAVIOR {
    public static final String ELEMENT_NODE = "outline-behavior.element-node"; //$NON-NLS-1$
    public static final String ATTRIBUTE_NODE = "outline-behavior.attribute-node"; //$NON-NLS-1$
    public static final String TEXT_NODE = "outline-behavior.text-node"; //$NON-NLS-1$
    public static final String CDATA_SECTION_NODE = "outline-behavior.cdata-section-node"; //$NON-NLS-1$
    public static final String ENTITY_REFERENCE_NODE = "outline-behavior.entity-reference-node"; //$NON-NLS-1$
    public static final String ENTITY_NODE = "outline-behavior.entity-node"; //$NON-NLS-1$
    public static final String PROCESSING_INSTRUCTION_NODE = "outline-behavior.instruction-node"; //$NON-NLS-1$
    public static final String COMMENT_NODE = "outline-behavior.comment-node"; //$NON-NLS-1$
    public static final String DOCUMENT_NODE = "outline-behavior.document-node"; //$NON-NLS-1$
    public static final String DOCUMENT_TYPE_NODE = "outline-behavior.document-type-node"; //$NON-NLS-1$
    public static final String DOCUMENT_FRAGMENT_NODE = "outline-behavior.document-fragment-node"; //$NON-NLS-1$
    public static final String NOTATION_NODE = "outline-behavior.notation-node"; //$NON-NLS-1$
  }

  private static String getUseInferredGrammarKey() {
    return "useInferredGrammar"; //$NON-NLS-1$
  }

  /**
   * A named preference that holds the characters that auto activate code assist.
   * <p>
   * Value is of type <code>String</code>. All characters that trigger auto code assist.
   * </p>
   */
  public static final String SUGGESTION_STRATEGY = getSuggestionStrategeyKey();

  private static String getSuggestionStrategeyKey() {
    return "suggestionStrategy";//$NON-NLS-1$
  }

  /**
   * The key to store the last template name used in new DTD file wizard. Template name is stored
   * instead of template id because user-created templates do not have template ids.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String NEW_FILE_TEMPLATE_NAME = "newFileTemplateName"; //$NON-NLS-1$

  /**
   * The initial template ID to be used in the new XML file wizard. In the absence of
   * {@link NEW_FILE_TEMPLATE_NAME}, this ID is used to find a template name
   */
  public static final String NEW_FILE_TEMPLATE_ID = "newFileTemplateId"; //$NON-NLS-1$

  /**
   * The key to store the option for auto-completing comments while typing.
   * <p>
   * Value is of type <code>boolean</code>.
   * </p>
   */
  public static final String TYPING_COMPLETE_COMMENTS = "completeComments"; //$NON-NLS-1$

  /**
   * The key to store the option for auto-completing end-tags after entering <code>&lt;/</code>
   * <p>
   * Value is of type <code>boolean</code>.
   * </p>
   */
  public static final String TYPING_COMPLETE_END_TAGS = "completeEndTags"; //$NON-NLS-1$

  /**
   * The key to store the option for auto-completing the element after entering <code>&gt;</code>
   * <p>
   * Value is of type <code>boolean</code>.
   * </p>
   */
  public static final String TYPING_COMPLETE_ELEMENTS = "completeElements"; //$NON-NLS-1$

  /**
   * The key to store the option for removing an end-tag if the start tag is converted to an
   * empty-tag.
   * <p>
   * Value is of type <code>boolean</code>.
   * </p>
   */
  public static final String TYPING_REMOVE_END_TAGS = "removeEndTags"; //$NON-NLS-1$

  /**
   * The key to store the option for auto-completing strings (" and ') while typing.
   * <p>
   * Value is of type <code>boolean</code>.
   * </p>
   */
  public static final String TYPING_CLOSE_STRINGS = "closeStrings"; //$NON-NLS-1$

  /**
   * The key to store the option for auto-completing brackets ([ and () while typing.
   * <p>
   * Value is of type <code>boolean</code>.
   * </p>
   */
  public static final String TYPING_CLOSE_BRACKETS = "closeBrackets"; //$NON-NLS-1$

  /**
   * <p>
   * preference key used for saving which categories should not display on the default page
   * </p>
   * <p>
   * Value is of type {@link String} consisting of
   * <tt>org.eclipse.wst.sse.ui.completionProposal/proposalCategory/@id</tt>s separated by the null
   * character (<tt>\0</tt>), ordered is ignored
   * </p>
   */
  public static final String CONTENT_ASSIST_DO_NOT_DISPLAY_ON_DEFAULT_PAGE = "xml_content_assist_display_on_default_page"; //$NON-NLS-1$

  /**
   * <p>
   * preference key used for saving which categories should not display on their own page
   * </p>
   * <p>
   * Value is of type {@link String} consisting of
   * <tt>org.eclipse.wst.sse.ui.completionProposal/proposalCategory/@id</tt>s separated by the null
   * character (<tt>\0</tt>), order is ignored
   * </p>
   */
  public static final String CONTENT_ASSIST_DO_NOT_DISPLAY_ON_OWN_PAGE = "xml_content_assist_display_on_own_page"; //$NON-NLS-1$

  /**
   * <p>
   * preference key for saving the sort order of the categories when displaying them on their own
   * page
   * </p>
   * <p>
   * Value is of type {@link String} consisting of
   * <tt>org.eclipse.wst.sse.ui.completionProposal/proposalCategory/@id</tt>s separated by the null
   * character (<tt>\0</tt>) in the desired sort order.
   * </p>
   */
  public static final String CONTENT_ASSIST_OWN_PAGE_SORT_ORDER = "xml_content_assist_own_page_sort_order"; //$NON-NLS-1$

  /**
   * <p>
   * preference key for saving the sort order of the categories when displaying them on the default
   * page
   * </p>
   * <p>
   * Value is of type {@link String} consisting of
   * <tt>org.eclipse.wst.sse.ui.completionProposal/proposalCategory/@id</tt>s separated by the null
   * character (<tt>\0</tt>) in the desired sort order.
   * </p>
   */
  public static final String CONTENT_ASSIST_DEFAULT_PAGE_SORT_ORDER = "xml_content_assist_default_page_sort_order"; //$NON-NLS-1$

  /**
   * <p>
   * preference key to store the option for auto insertion of single suggestions
   * </p>
   * <p>
   * Value is of type <code>boolean</code>
   * </p>
   */
  public static final String INSERT_SINGLE_SUGGESTION = "insertSingleSuggestion"; //$NON-NLS-1$
}
