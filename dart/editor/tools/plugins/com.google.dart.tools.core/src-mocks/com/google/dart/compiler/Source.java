// Copyright (c) 2011, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

public interface Source {

  boolean exists();

  long getLastModified();

  String getName();

  Reader getSourceReader() throws IOException;

  String getUniqueIdentifier();

  URI getUri();
}
