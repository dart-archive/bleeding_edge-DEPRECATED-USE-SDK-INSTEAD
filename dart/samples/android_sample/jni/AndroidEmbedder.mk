LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := android_embedder
LOCAL_SRC_FILES := ../../../out/android/$(BUILDTYPE)/obj.target/samples/android_embedder/libandroid_embedder.so
include $(PREBUILT_SHARED_LIBRARY)


