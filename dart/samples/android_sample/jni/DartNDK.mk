LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS= -DDEBUG
LOCAL_C_INCLUDES := ../../../runtime ../../../runtime/include ..
LOCAL_CPP_EXTENSION :=.cc
LOCAL_MODULE := DartNDK
LOCAL_SRC_FILES := main.cc eventloop.cc dart_host.cc timer.cc \
    graphics.cc input_service.cc vm_glue.cc
LOCAL_LDLIBS := -landroid -llog -lEGL -lGLESv2 
LOCAL_WHOLE_STATIC_LIBRARIES := android_native_app_glue
LOCAL_SHARED_LIBRARIES := libandroid_embedder

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)
