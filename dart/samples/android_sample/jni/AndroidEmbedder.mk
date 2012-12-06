# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := android_embedder
LOCAL_SRC_FILES := ../../../out/android/$(BUILDTYPE)/obj.target/runtime/libandroid_embedder.so
include $(PREBUILT_SHARED_LIBRARY)


