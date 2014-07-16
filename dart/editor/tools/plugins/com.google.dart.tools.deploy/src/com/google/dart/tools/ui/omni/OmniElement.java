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
package com.google.dart.tools.ui.omni;

import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.omni.util.CamelUtil;

import org.eclipse.jface.resource.ImageDescriptor;

public abstract class OmniElement {

  protected static final String separator = " - "; //$NON-NLS-1$

  private static final int[][] EMPTY_INDICES = new int[0][0];
  private final OmniProposalProvider provider;

  private boolean duplicate;
  protected int detailOffset = -1;

  /**
   * Create an element.
   * 
   * @param provider the proposal provider
   */
  public OmniElement(OmniProposalProvider provider) {
    super();
    this.provider = provider;
  }

  /**
   * Executes the associated action for this element.
   * 
   * @param text the current string in the search box
   */
  public void execute(String text) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(this.getClass());

    instrumentation.data("text", text);
    try {
      doExecute(text, instrumentation);
    } finally {
      instrumentation.log();
    }

  }

  /**
   * Returns a detailed label (used for disambiguating duplicate matches)
   */
  public String getDetailedLabel() {
    return getLabel();
  }

  /**
   * Get the offset of the detail portion of this element's label (if there is one). The default
   * value is <code>-1</code>.
   * 
   * @return the detail offset, or <code>-1</code> if there is none.
   */
  public int getDetailOffset() {
    return detailOffset;
  }

  /**
   * Returns the id for this element. The id has to be unique within the OmniProposalProvider that
   * provided this element.
   * 
   * @return the id
   */
  public abstract String getId();

  /**
   * Returns the image descriptor for this element.
   * 
   * @return an image descriptor, or null if no image is available
   */
  public abstract ImageDescriptor getImageDescriptor();

  /**
   * Returns an in info label, that used with the detailed label can further disambiguate duplicate
   * matches. In the UI this label is used in the "info" area at the bottom of the omni box.
   */
  public String getInfoLabel() {
    return "";
  }

  /**
   * Returns the label to be displayed to the user.
   * 
   * @return the label
   */
  public abstract String getLabel();

  /**
   * Returns the text for matching. (This may be a substring of what is returned by
   * {@link OmniElement#getLabel()}.)
   * 
   * @return the text for matching
   */
  public String getMatchText() {
    return getLabel();
  }

  /**
   * Get the offset of the match text into the full label.
   * 
   * @return match text offset
   */
  public int getMatchTextOffset() {
    //default is 0, assuming the entire label should be matched
    return 0;
  }

  /**
   * @return Returns a storable version of this element
   */
  public OmniElement getMemento() {
    return this;
  }

  /**
   * @return Returns the provider.
   */
  public OmniProposalProvider getProvider() {
    return provider;
  }

  /**
   * Return the label to be used for sorting and matching elements.
   * 
   * @return the sort label
   */
  public String getSortLabel() {
    return getLabel();
  }

  /**
   * Check if this element has been marked a duplicate (requiring disambiguation in the UI).
   */
  public boolean isDuplicate() {
    return duplicate;
  }

  /**
   * Tests for equivalency ("equalness" without the contract of {@link Object#equals(Object)}.
   * 
   * @param that the element to test against
   * @return <code>true</code> if this element is the "same as" or "equivalent to" the other,
   *         <code>false</code> otherwise
   */
  public boolean isSameAs(OmniElement that) {
    if (that == null) {
      return false;
    }
    if (this == that) {
      return true;
    }
    if (this.getProvider() == that.getProvider()) {
      return this.getLabel() == that.getLabel();
    }
    return false;
  }

  public OmniEntry match(String filter, OmniProposalProvider providerForMatching) {
    String sortLabel = getMatchText();
    filter = filter.toLowerCase();
    int index = sortLabel.toLowerCase().indexOf(filter);
    if (index != -1) {
      index += getMatchTextOffset();
      return new OmniEntry(this, providerForMatching, new int[][] {{
          index, index + filter.length() - 1}}, EMPTY_INDICES);
    }

    //TODO (pquitslund): we are no longer combining provider and labels into filters 
    //with an eye towards more formal query support
    //String combinedLabel = (providerForMatching.getName() + " " + getLabel()); //$NON-NLS-1$
    String combinedLabel = (getMatchText()); //$NON-NLS-1$
    index = combinedLabel.toLowerCase().indexOf(filter);

    if (index != -1) {
      int lengthOfElementMatch = index + filter.length() - providerForMatching.getName().length()
          - 1;
      if (lengthOfElementMatch > 0) {
        return new OmniEntry(
            this,
            providerForMatching,
            new int[][] {{0, lengthOfElementMatch - 1}},
            new int[][] {{index, index + filter.length() - 1}});
      }
      return new OmniEntry(this, providerForMatching, EMPTY_INDICES, new int[][] {{
          index, index + filter.length() - 1}});
    }
    String camelCase = CamelUtil.getCamelCase(sortLabel);
    index = camelCase.indexOf(filter);
    if (index != -1) {
      int[][] indices = CamelUtil.getCamelCaseIndices(sortLabel, index, filter.length());
      return new OmniEntry(this, providerForMatching, indices, EMPTY_INDICES);
    }
    String combinedCamelCase = CamelUtil.getCamelCase(combinedLabel);
    index = combinedCamelCase.indexOf(filter);
    if (index != -1) {
      String providerCamelCase = CamelUtil.getCamelCase(providerForMatching.getName());
      int lengthOfElementMatch = index + filter.length() - providerCamelCase.length();
      if (lengthOfElementMatch > 0) {
        return new OmniEntry(this, providerForMatching, CamelUtil.getCamelCaseIndices(
            sortLabel,
            0,
            lengthOfElementMatch), CamelUtil.getCamelCaseIndices(
            providerForMatching.getName(),
            index,
            filter.length() - lengthOfElementMatch));
      }
      return new OmniEntry(this, providerForMatching, EMPTY_INDICES, CamelUtil.getCamelCaseIndices(
          providerForMatching.getName(),
          index,
          filter.length()));
    }
    return null;
  }

  /**
   * Flag this element as a duplicate requiring disambiguation in the UI.
   */
  public void setIsDuplicate(boolean isDuplicate) {
    this.duplicate = isDuplicate;
  }

  protected abstract void doExecute(String text, UIInstrumentationBuilder instrumentation);

}
