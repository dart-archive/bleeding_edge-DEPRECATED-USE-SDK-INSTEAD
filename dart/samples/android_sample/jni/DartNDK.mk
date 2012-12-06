# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS= -DDEBUG
LOCAL_C_INCLUDES := ../../../runtime ../../../runtime/include ..
LOCAL_CPP_EXTENSION :=.cc
LOCAL_MODULE := DartNDK
LOCAL_SRC_FILES := main.cc eventloop.cc dart_host.cc timer.cc \
    graphics.cc input_service.cc sound_service.cc vm_glue.cc android_extension.cc
LOCAL_LDLIBS := -landroid -llog -lEGL -lGLESv2 -lOpenSLES
LOCAL_WHOLE_STATIC_LIBRARIES := android_native_app_glue
LOCAL_SHARED_LIBRARIES := libandroid_embedder

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)

