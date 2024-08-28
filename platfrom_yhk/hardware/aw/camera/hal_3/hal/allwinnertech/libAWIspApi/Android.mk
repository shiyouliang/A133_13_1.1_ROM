LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    AWIspApi.cpp

LOCAL_SHARED_LIBRARIES := \
    libisp libisp_ini libcutils liblog

ifeq (isp_522, $(LIB_ISP_VERSION))
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/libisp_522/include/V4l2Camera \
    $(LOCAL_PATH)/libisp_522/include/device \
    $(LOCAL_PATH)/libisp_522/include \
    $(LOCAL_PATH)/libisp_522/isp_dev \
    $(LOCAL_PATH)/libisp_522/isp_tuning \
    $(LOCAL_PATH)/libisp_522 \
    system/core/include \
    $(LOCAL_PATH)/
else ifeq (isp_601, $(LIB_ISP_VERSION))
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/libisp_601/include/V4l2Camera \
    $(LOCAL_PATH)/libisp_601/include/device \
    $(LOCAL_PATH)/libisp_601/include \
    $(LOCAL_PATH)/libisp_601/isp_dev \
    $(LOCAL_PATH)/libisp_601/isp_tuning \
    $(LOCAL_PATH)/libisp_601 \
    system/core/include \
    $(LOCAL_PATH)/
else
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/libisp_500/include/V4l2Camera \
    $(LOCAL_PATH)/libisp_500/include/device \
    $(LOCAL_PATH)/libisp_500/include \
    $(LOCAL_PATH)/libisp_500/isp_dev \
    $(LOCAL_PATH)/libisp_500/isp_tuning \
    $(LOCAL_PATH)/libisp_500 \
    system/core/include \
    $(LOCAL_PATH)/
endif

LOCAL_CFLAGS += -Wno-multichar -Wno-unused-parameter -Wno-incompatible-pointer-types-discards-qualifiers

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE:= libAWIspApi

LOCAL_PROPRIETARY_MODULE := true

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
