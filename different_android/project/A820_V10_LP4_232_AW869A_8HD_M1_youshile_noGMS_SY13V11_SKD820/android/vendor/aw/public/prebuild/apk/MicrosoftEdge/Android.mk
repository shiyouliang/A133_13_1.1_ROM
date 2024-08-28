LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := MicrosoftEdge
LOCAL_DEX_PREOPT := false
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_PRIVILEGED_MODULE := true
LOCAL_PRODUCT_MODULE := true
LOCAL_CERTIFICATE := PRESIGNED
##################导入lib目录下的所有.so##############
JNI_LIBS :=
$(foreach FILE,$(shell find $(LOCAL_PATH)/lib -name *.so), $(eval JNI_LIBS += $(FILE)))
LOCAL_PREBUILT_JNI_LIBS := $(subst $(LOCAL_PATH),,$(JNI_LIBS))
#####################################################
LOCAL_SRC_FILES := MicrosoftEdge.apk
LOCAL_OPTIONAL_USES_LIBRARIES := org.apache.http.legacy androidx.window.extensions androidx.window.sidecar android.ext.adservices
LOCAL_REPLACE_PREBUILT_APK_INSTALLED := $(LOCAL_PATH)/$(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#LOCAL_PATH := $(my-dir)
#
#include $(CLEAR_VARS)
#LOCAL_MODULE := MicrosoftEdge
#LOCAL_MODULE_CLASS := APPS
#LOCAL_MODULE_PATH := $(TARGET_OUT)/app
#LOCAL_SRC_FILES := $(LOCAL_MODULE)$(COMMON_ANDROID_PACKAGE_SUFFIX)
#LOCAL_CERTIFICATE := PRESIGNED
#LOCAL_DEX_PREOPT := true
#LOCAL_MODULE_TAGS := optional
#LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
#LOCAL_JNI_SHARED_LIBRARIES_ABI := arm
#MY_LOCAL_PREBUILT_JNI_LIBS := \
#	lib/arm/libOneAuth.so\
#	lib/arm/libc++_shared.so\
#	lib/arm/libchrome.so\
#	lib/arm/libchrome_crashpad_handler.so\
#	lib/arm/liblearning_tools.so\
#	lib/arm/libmip_core.so\
#	lib/arm/libmip_protection_sdk.so\
#	lib/arm/libmip_upe_sdk.so\
#	lib/arm/liboneds.so\
#	lib/arm/libsmartscreenn.so\
#	lib/arm/libtelclient.so\
#	lib/arm/libwns_push_client.so\
#	lib/arm/libmsaoaidauth.so\
#	lib/arm/libmsaoaidsec.so\
#	lib/arm/libctxlog.so\
#	lib/arm/liblog4cpp.so\
#	lib/arm/libCoreSdkCrypto.so\
#	lib/arm/libfullsslsdk.so\
#	lib/arm/libbingopus.so\
#	lib/arm/libnative-lib.so\
#
#MY_APP_LIB_PATH := $(TARGET_OUT)/app/$(LOCAL_MODULE)/lib/$(LOCAL_JNI_SHARED_LIBRARIES_ABI)
#ifneq ($(LOCAL_JNI_SHARED_LIBRARIES_ABI), None)
#$(warning MY_APP_LIB_PATH=$(MY_APP_LIB_PATH))
#LOCAL_POST_INSTALL_CMD :=     mkdir -p $(MY_APP_LIB_PATH)     $(foreach lib, $(MY_LOCAL_PREBUILT_JNI_LIBS), ; cp -f $(LOCAL_PATH)/$(lib) $(MY_APP_LIB_PATH)/$(notdir $(lib)))
#endif
#include $(BUILD_PREBUILT)

