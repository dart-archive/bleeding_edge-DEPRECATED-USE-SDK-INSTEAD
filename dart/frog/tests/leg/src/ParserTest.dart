// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('parser_helper.dart');

void main() {
  parseStatement('List<T> t;');
  parseStatement('List<List<T>> t;');
  parseStatement('List<List<List<T>>> t;');
  parseStatement('List<List<List<List<T>>>> t;');
  parseStatement('List<List<List<List<List<T>>>>> t;');

  parseStatement('List<List<T> > t;');
  parseStatement('List<List<List<T> >> t;');
  parseStatement('List<List<List<List<T> >>> t;');
  parseStatement('List<List<List<List<List<T> >>>> t;');

  parseStatement('List<List<List<T> > > t;');
  parseStatement('List<List<List<List<T> > >> t;');
  parseStatement('List<List<List<List<List<T> > >>> t;');

  parseStatement('List<List<List<List<T> > > > t;');
  parseStatement('List<List<List<List<List<T> > > >> t;');

  parseStatement('List<List<List<List<List<T> > > > > t;');

  parseStatement('List<List<List<List<List<T> >> >> t;');

  parseStatement('List<List<List<List<List<T> >>> > t;');

  parseStatement('List<List<List<List<List<T >>> >> t;');
}
