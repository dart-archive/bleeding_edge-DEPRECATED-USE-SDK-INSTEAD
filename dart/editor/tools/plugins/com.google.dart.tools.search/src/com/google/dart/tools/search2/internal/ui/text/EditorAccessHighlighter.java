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
package com.google.dart.tools.search2.internal.ui.text;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.text.ISearchEditorAccess;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.search2.internal.ui.InternalSearchUI;
import com.google.dart.tools.search2.internal.ui.SearchMessages;

import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class EditorAccessHighlighter extends Highlighter {
  private ISearchEditorAccess fEditorAcess;
  private Map<Match, Annotation> fMatchesToAnnotations;

  public EditorAccessHighlighter(ISearchEditorAccess editorAccess) {
    fEditorAcess = editorAccess;
    fMatchesToAnnotations = new HashMap<Match, Annotation>();
  }

  @Override
  public void addHighlights(Match[] matches) {
    Map<IAnnotationModel, HashMap<Annotation, Position>> mapsByAnnotationModel = new HashMap<IAnnotationModel, HashMap<Annotation, Position>>();
    for (int i = 0; i < matches.length; i++) {
      int offset = matches[i].getOffset();
      int length = matches[i].getLength();
      if (offset >= 0 && length >= 0) {
        try {
          Position position = createPosition(matches[i]);
          if (position != null) {
            Map<Annotation, Position> map = getMap(mapsByAnnotationModel, matches[i]);
            if (map != null) {
              Annotation annotation = matches[i].isFiltered() ? new Annotation(
                  SearchPlugin.FILTERED_SEARCH_ANNOTATION_TYPE,
                  true,
                  null) : new Annotation(SearchPlugin.SEARCH_ANNOTATION_TYPE, true, null);
              fMatchesToAnnotations.put(matches[i], annotation);
              map.put(annotation, position);
            }
          }
        } catch (BadLocationException e) {
          SearchPlugin.log(new Status(
              IStatus.ERROR,
              SearchPlugin.getID(),
              0,
              SearchMessages.EditorAccessHighlighter_error_badLocation,
              e));
        }
      }
    }
    for (Iterator<IAnnotationModel> maps = mapsByAnnotationModel.keySet().iterator(); maps.hasNext();) {
      IAnnotationModel model = maps.next();
      Map<Annotation, Position> positionMap = mapsByAnnotationModel.get(model);
      addAnnotations(model, positionMap);
    }

  }

  @Override
  public void removeAll() {
    Set<Match> matchSet = fMatchesToAnnotations.keySet();
    Match[] matches = new Match[matchSet.size()];
    removeHighlights(matchSet.toArray(matches));
  }

  @Override
  public void removeHighlights(Match[] matches) {
    Map<IAnnotationModel, HashSet<Annotation>> setsByAnnotationModel = new HashMap<IAnnotationModel, HashSet<Annotation>>();
    for (int i = 0; i < matches.length; i++) {
      Annotation annotation = fMatchesToAnnotations.remove(matches[i]);
      if (annotation != null) {
        Set<Annotation> annotations = getSet(setsByAnnotationModel, matches[i]);
        if (annotations != null) {
          annotations.add(annotation);
        }
      }
    }

    for (Iterator<IAnnotationModel> maps = setsByAnnotationModel.keySet().iterator(); maps.hasNext();) {
      IAnnotationModel model = maps.next();
      Set<Annotation> set = setsByAnnotationModel.get(model);
      removeAnnotations(model, set);
    }

  }

  @Override
  protected void handleContentReplaced(IFileBuffer buffer) {
    if (!(buffer instanceof ITextFileBuffer)) {
      return;
    }
    IDocument document = null;
    ITextFileBuffer textBuffer = (ITextFileBuffer) buffer;
    for (Iterator<Match> matches = fMatchesToAnnotations.keySet().iterator(); matches.hasNext();) {
      Match match = matches.next();
      document = fEditorAcess.getDocument(match);
      if (document != null) {
        break;
      }
    }

    if (document != null && document.equals(textBuffer.getDocument())) {
      Match[] matches = new Match[fMatchesToAnnotations.keySet().size()];
      fMatchesToAnnotations.keySet().toArray(matches);
      removeAll();
      addHighlights(matches);
    }
  }

  private void addAnnotations(IAnnotationModel model,
      Map<Annotation, Position> annotationToPositionMap) {
    if (model instanceof IAnnotationModelExtension) {
      IAnnotationModelExtension ame = (IAnnotationModelExtension) model;
      ame.replaceAnnotations(new Annotation[0], annotationToPositionMap);
    } else {
      for (Iterator<Annotation> elements = annotationToPositionMap.keySet().iterator(); elements.hasNext();) {
        Annotation element = elements.next();
        Position p = annotationToPositionMap.get(element);
        model.addAnnotation(element, p);
      }
    }
  }

  private Position createPosition(Match match) throws BadLocationException {
    Position position = InternalSearchUI.getInstance().getPositionTracker().getCurrentPosition(
        match);
    if (position == null) {
      position = new Position(match.getOffset(), match.getLength());
    } else {
      // need to clone position, can't have it twice in a document.
      position = new Position(position.getOffset(), position.getLength());
    }
    if (match.getBaseUnit() == Match.UNIT_LINE) {
      IDocument doc = fEditorAcess.getDocument(match);
      if (doc != null) {
        position = PositionTracker.convertToCharacterPosition(position, doc);
      } else {
        SearchPlugin.log(new Status(
            IStatus.ERROR,
            SearchPlugin.getID(),
            0,
            SearchMessages.AnnotationHighlighter_error_noDocument,
            null));
        return null;
      }
    }
    return position;
  }

  private Map<Annotation, Position> getMap(
      Map<IAnnotationModel, HashMap<Annotation, Position>> mapsByAnnotationModel, Match match) {
    IAnnotationModel model = fEditorAcess.getAnnotationModel(match);
    if (model == null) {
      return null;
    }
    HashMap<Annotation, Position> map = mapsByAnnotationModel.get(model);
    if (map == null) {
      map = new HashMap<Annotation, Position>();
      mapsByAnnotationModel.put(model, map);
    }
    return map;
  }

  private Set<Annotation> getSet(Map<IAnnotationModel, HashSet<Annotation>> setsByAnnotationModel,
      Match match) {
    IAnnotationModel model = fEditorAcess.getAnnotationModel(match);
    if (model == null) {
      return null;
    }
    HashSet<Annotation> set = setsByAnnotationModel.get(model);
    if (set == null) {
      set = new HashSet<Annotation>();
      setsByAnnotationModel.put(model, set);
    }
    return set;
  }

  /*
   * Removes annotations from the given annotation model. The default implementation works for
   * editors that implement <code>ITextEditor</code>. Subclasses may override this method.
   * 
   * @param annotations A set containing the annotations to be removed.
   * 
   * @see Annotation
   */
  private void removeAnnotations(IAnnotationModel model, Set<Annotation> annotations) {
    if (model instanceof IAnnotationModelExtension) {
      IAnnotationModelExtension ame = (IAnnotationModelExtension) model;
      Annotation[] annotationArray = new Annotation[annotations.size()];
      ame.replaceAnnotations(annotations.toArray(annotationArray), Collections.EMPTY_MAP);
    } else {
      for (Iterator<Annotation> iter = annotations.iterator(); iter.hasNext();) {
        Annotation element = iter.next();
        model.removeAnnotation(element);
      }
    }
  }
}
