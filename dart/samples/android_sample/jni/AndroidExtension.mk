LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS= -DDEBUG
LOCAL_C_INCLUDES := ../../../runtime ../../../runtime/include
LOCAL_CPP_EXTENSION :=.cc
LOCAL_MODULE := android_extension
LOCAL_SRC_FILES := android_extension.cc
LOCAL_LDLIBS := -landroid -llog -lEGL -lGLESv2 
LOCAL_SHARED_LIBRARIES := libandroid_embedder

include $(BUILD_SHARED_LIBRARY)

