# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

MY_LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION :=.cc
include $(MY_LOCAL_PATH)/AndroidEmbedder.mk
include $(MY_LOCAL_PATH)/DartNDK.mk

$(call import-module,android/native_app_glue)


