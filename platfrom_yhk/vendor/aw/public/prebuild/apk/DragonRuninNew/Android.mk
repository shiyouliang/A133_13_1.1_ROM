LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := DragonRuninNew
LOCAL_DEX_PREOPT := false
# We mark this out until Mtp and MediaMetadataRetriever is unhidden.
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := platform
#LOCAL_PRIVILEGED_MODULE := true
LOCAL_REQUIRED_MODULES := runin-new-permissions.xml
LOCAL_PROGUARD_FLAG_FILES := proguard.cfg
#LOCAL_JNI_SHARED_LIBRARIES_ABI := arm64
LOCAL_ENFORCE_USES_LIBRARIES := false
#MY_LOCAL_PREBUILT_JNI_LIBS := \
#    libs/arm64/libnative-lib.so
	
ifeq ($(TARGET_ARCH),arm64) 
    LOCAL_JNI_SHARED_LIBRARIES_ABI := arm64
    MY_LOCAL_PREBUILT_JNI_LIBS := \
        libs/arm64-v8a/libnative-lib.so	
else ifeq ($(TARGET_ARCH),arm)  
    LOCAL_JNI_SHARED_LIBRARIES_ABI := arm
    MY_LOCAL_PREBUILT_JNI_LIBS := \
        libs/armeabi-v7a/libnative-lib.so		
endif
	
MY_APP_LIB_PATH := $(TARGET_OUT)/app/$(LOCAL_PACKAGE_NAME)/lib/$(LOCAL_JNI_SHARED_LIBRARIES_ABI)
ifneq ($(LOCAL_JNI_SHARED_LIBRARIES_ABI), None)
$(warning MY_APP_LIB_PATH=$(MY_APP_LIB_PATH))
LOCAL_POST_INSTALL_CMD :=     mkdir -p $(MY_APP_LIB_PATH)     $(foreach lib, $(MY_LOCAL_PREBUILT_JNI_LIBS), ; cp -f $(LOCAL_PATH)/$(lib) $(MY_APP_LIB_PATH)/$(notdir $(lib)))
endif
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_MODULE := runin-new-permissions.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_RELATIVE_PATH := permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
