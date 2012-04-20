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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.ProblemsLabelDecorator;
import com.google.dart.tools.ui.internal.viewsupport.AppearanceAwareLabelProvider;
import com.google.dart.tools.ui.search.MatchPresentation;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Search label provider.
 */
public abstract class SearchLabelProvider extends AppearanceAwareLabelProvider implements
    IStyledLabelProvider {

  public static final String PROPERTY_MATCH_COUNT = "com.google.dart.tools.search.matchCount"; //$NON-NLS-1$

  protected static final int DEFAULT_SEARCH_IMAGEFLAGS = DEFAULT_IMAGEFLAGS;
  protected static final long DEFAULT_SEARCH_TEXTFLAGS = (DEFAULT_TEXTFLAGS | DartElementLabels.P_COMPRESSED)
      & ~DartElementLabels.M_APP_RETURNTYPE;

  private static final String EMPHASIZE_POTENTIAL_MATCHES = "com.google.dart.tools.search.potentialMatch.emphasize"; //$NON-NLS-1$
  private static final String POTENTIAL_MATCH_FG_COLOR = "com.google.dart.tools.search.potentialMatch.fgColor"; //$NON-NLS-1$

  protected DartSearchResultPage page;
  private Map<MatchPresentation, ILabelProvider> labelProviderMap;

  private Color potentialMatchFgColor;

  private ScopedPreferenceStore searchPreferences;
  private IPropertyChangeListener searchPropertyListener;

  public SearchLabelProvider(DartSearchResultPage page) {
    super(DEFAULT_SEARCH_TEXTFLAGS, DEFAULT_SEARCH_IMAGEFLAGS);
    addLabelDecorator(new ProblemsLabelDecorator(null));

    this.page = page;
    labelProviderMap = new HashMap<MatchPresentation, ILabelProvider>(5);

    searchPreferences = new ScopedPreferenceStore(InstanceScope.INSTANCE, NewSearchUI.PLUGIN_ID);
    searchPropertyListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        doSearchPropertyChange(event);
      }
    };
    searchPreferences.addPropertyChangeListener(searchPropertyListener);
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    super.addListener(listener);
    for (Iterator<ILabelProvider> labelProviders = labelProviderMap.values().iterator(); labelProviders.hasNext();) {
      ILabelProvider labelProvider = labelProviders.next();
      labelProvider.addListener(listener);
    }
  }

  @Override
  public void dispose() {
    if (potentialMatchFgColor != null) {
      potentialMatchFgColor.dispose();
      potentialMatchFgColor = null;
    }
    searchPreferences.removePropertyChangeListener(searchPropertyListener);
    for (Iterator<ILabelProvider> labelProviders = labelProviderMap.values().iterator(); labelProviders.hasNext();) {
      ILabelProvider labelProvider = labelProviders.next();
      labelProvider.dispose();
    }

    searchPreferences = null;
    searchPropertyListener = null;
    labelProviderMap.clear();

    super.dispose();
  }

  @Override
  public Color getForeground(Object element) {
    if (arePotentialMatchesEmphasized()) {
      if (getNumberOfPotentialMatches(element) > 0) {
        return getForegroundColor();
      }
    }
    return super.getForeground(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    StyledString string = DartElementLabels.getStyledTextLabel(element,
        (evaluateTextFlags(element) | DartElementLabels.COLORIZE));
    if (string.length() == 0 && (element instanceof IStorage)) {
      string = new StyledString(fStorageLabelProvider.getText(element));
    }
    String decorated = decorateText(string.getString(), element);
    if (decorated != null) {
      return StyledCellLabelProvider.styleDecoratedString(decorated,
          StyledString.DECORATIONS_STYLER, string);
    }
    return string;
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    if (PROPERTY_MATCH_COUNT.equals(property)) {
      return true;
    }
    return getLabelProvider(element).isLabelProperty(element, property);
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    super.removeListener(listener);
    for (Iterator<ILabelProvider> labelProviders = labelProviderMap.values().iterator(); labelProviders.hasNext();) {
      ILabelProvider labelProvider = labelProviders.next();
      labelProvider.removeListener(listener);
    }
  }

  protected final StyledString getColoredLabelWithCounts(Object element, StyledString coloredName) {
    String name = coloredName.getString();
    String decorated = getLabelWithCounts(element, name);
    if (decorated.length() > name.length()) {
      StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.COUNTER_STYLER,
          coloredName);
    }
    return coloredName;
  }

  protected final String getLabelWithCounts(Object element, String elementName) {
    int matchCount = page.getDisplayedMatchCount(element);
    int potentialCount = getNumberOfPotentialMatches(element);

    if (matchCount < 2) {
      if (matchCount == 1 && hasChildren(element)) {
        if (potentialCount > 0) {
          return Messages.format(SearchMessages.SearchLabelProvider_potential_singular, elementName);
        }
        return Messages.format(SearchMessages.SearchLabelProvider_exact_singular, elementName);
      }
      if (potentialCount > 0) {
        return Messages.format(SearchMessages.SearchLabelProvider_potential_noCount, elementName);
      }
      return Messages.format(SearchMessages.SearchLabelProvider_exact_noCount, elementName);
    } else {
      int exactCount = matchCount - potentialCount;

      if (potentialCount > 0 && exactCount > 0) {
        String[] args = new String[] {
            elementName, String.valueOf(matchCount), String.valueOf(exactCount),
            String.valueOf(potentialCount)};
        return Messages.format(SearchMessages.SearchLabelProvider_exact_and_potential_plural, args);
      } else if (exactCount == 0) {
        String[] args = new String[] {elementName, String.valueOf(matchCount)};
        return Messages.format(SearchMessages.SearchLabelProvider_potential_plural, args);
      }
      String[] args = new String[] {elementName, String.valueOf(matchCount)};
      return Messages.format(SearchMessages.SearchLabelProvider_exact_plural, args);
    }
  }

  protected final int getNumberOfPotentialMatches(Object element) {
    int res = 0;
    AbstractTextSearchResult result = page.getInput();
    if (result != null) {
      Match[] matches = result.getMatches(element);
      for (int i = 0; i < matches.length; i++) {
        if ((matches[i]) instanceof DartElementMatch) {
          //TODO (pquitslund): fix this accuracy check
//          if (((DartElementMatch)matches[i]).getAccuracy() == SearchMatch.A_INACCURATE)
          res++;
        }
      }
    }
    return res;
  }

  protected Image getParticipantImage(Object element) {
    ILabelProvider lp = getLabelProvider(element);
    if (lp == null) {
      return null;
    }
    return lp.getImage(element);
  }

  protected String getParticipantText(Object element) {
    ILabelProvider labelProvider = getLabelProvider(element);
    if (labelProvider != null) {
      return labelProvider.getText(element);
    }
    return ""; //$NON-NLS-1$

  }

  protected StyledString getStyledParticipantText(Object element) {
    ILabelProvider labelProvider = getLabelProvider(element);
    if (labelProvider instanceof IStyledLabelProvider) {
      return ((IStyledLabelProvider) labelProvider).getStyledText(element);
    }
    if (labelProvider != null) {
      return new StyledString(labelProvider.getText(element));
    }
    return new StyledString();
  }

  /**
   * Returns <code>true</code> if the given element has children
   * 
   * @param elem the element
   * @return returns <code>true</code> if the given element has children
   */
  protected boolean hasChildren(Object elem) {
    return false;
  }

  final void doSearchPropertyChange(PropertyChangeEvent event) {
    if (potentialMatchFgColor == null) {
      return;
    }
    if (POTENTIAL_MATCH_FG_COLOR.equals(event.getProperty())
        || EMPHASIZE_POTENTIAL_MATCHES.equals(event.getProperty())) {
      potentialMatchFgColor.dispose();
      potentialMatchFgColor = null;
      LabelProviderChangedEvent lpEvent = new LabelProviderChangedEvent(SearchLabelProvider.this,
          null); // refresh all
      fireLabelProviderChanged(lpEvent);
    }
  }

  private boolean arePotentialMatchesEmphasized() {
    return searchPreferences.getBoolean(EMPHASIZE_POTENTIAL_MATCHES);
  }

  private Color getForegroundColor() {
    if (potentialMatchFgColor == null) {
      potentialMatchFgColor = new Color(DartToolsPlugin.getActiveWorkbenchShell().getDisplay(),
          getPotentialMatchForegroundColor());
    }
    return potentialMatchFgColor;
  }

  private ILabelProvider getLabelProvider(Object element) {
    AbstractTextSearchResult input = page.getInput();
    if (!(input instanceof DartSearchResult)) {
      return null;
    }

    MatchPresentation participant = ((DartSearchResult) input).getSearchParticpant(element);
    if (participant == null) {
      return null;
    }

    ILabelProvider lp = labelProviderMap.get(participant);
    if (lp == null) {
      lp = participant.createLabelProvider();
      labelProviderMap.put(participant, lp);

      Object[] listeners = fListeners.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        lp.addListener((ILabelProviderListener) listeners[i]);
      }
    }
    return lp;
  }

  private RGB getPotentialMatchForegroundColor() {
    return PreferenceConverter.getColor(searchPreferences, POTENTIAL_MATCH_FG_COLOR);
  }
}
