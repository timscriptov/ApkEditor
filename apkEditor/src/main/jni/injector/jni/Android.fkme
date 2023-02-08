
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog
#LOCAL_ARM_MODE := arm
LOCAL_MODULE    := fkme
LOCAL_SRC_FILES := libfkme.c memsearch.c
include $(BUILD_SHARED_LIBRARY)

