// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef ANDROID_EXTENSION_H
#define ANDROID_EXTENSION_H

#include "include/dart_api.h"

Dart_NativeFunction ResolveName(Dart_Handle name, int argc);

void PlayBackground(const char* path);
void StopBackground();

#endif
