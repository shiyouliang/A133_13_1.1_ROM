LOCAL_MODULE_PATH := $(shell dirname $(lastword $(MAKEFILE_LIST)))

PRODUCT_HAS_UVC_CAMERA := false
# copy files
PRODUCT_COPY_FILES += \
    $(LOCAL_MODULE_PATH)/camera.cfg:$(TARGET_COPY_OUT_VENDOR)/etc/camera.cfg \
    $(LOCAL_MODULE_PATH)/hawkview/sensor_list_cfg.ini:vendor/etc/hawkview/sensor_list_cfg.ini \
    $(LOCAL_MODULE_PATH)/init.camera.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.camera.rc

ifeq ($(PRODUCT_HAS_UVC_CAMERA),true)
PRODUCT_COPY_FILES += \
    $(LOCAL_MODULE_PATH)/external_camera_config.xml:$(TARGET_COPY_OUT_VENDOR)/etc/external_camera_config.xml
else
PRODUCT_COPY_FILES += \
    $(LOCAL_MODULE_PATH)/media_profiles.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_profiles_V1_0.xml
endif
