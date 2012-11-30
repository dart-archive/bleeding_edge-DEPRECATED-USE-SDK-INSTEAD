MY_LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION :=.cc
include $(MY_LOCAL_PATH)/AndroidEmbedder.mk
include $(MY_LOCAL_PATH)/DartNDK.mk
include $(MY_LOCAL_PATH)/AndroidExtension.mk

$(call import-module,android/native_app_glue)


