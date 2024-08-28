PRODUCT_PLATFORM_PATH := $(shell dirname $(lastword $(MAKEFILE_LIST)))

TARGET_BOARD_IC := a133
PRODUCT_BRAND := Allwinner
PRODUCT_NAME := ceres_b6
PRODUCT_DEVICE := ceres-b6
PRODUCT_BOARD := b6
PRODUCT_MODEL := SKD820
PRODUCT_MANUFACTURER := Allwinner

PRODUCT_PREBUILT_PATH := longan/out/$(TARGET_BOARD_IC)/$(PRODUCT_BOARD)/android
PRODUCT_DEVICE_PATH := $(PRODUCT_PLATFORM_PATH)/$(PRODUCT_DEVICE)

PRODUCT_BUILD_VENDOR_BOOT_IMAGE := true

CONFIG_LOW_RAM_DEVICE := true
CONFIG_SUPPORT_GMS := false

PRODUCT_COPY_FILES += $(PRODUCT_PREBUILT_PATH)/bImage:kernel

#set speaker project(true: double speaker, false: single speaker)
#set default eq
PRODUCT_PROPERTY_OVERRIDES += \
    ro.vendor.spk_dul.used=false \
    ro.vendor.audio.eq=false
    
PRODUCT_PACKAGES += su
PRODUCT_PACKAGES += MicrosoftEdge
PRODUCT_PACKAGES += AwMusic		  
PRODUCT_PACKAGES += DragonAtt
PRODUCT_PACKAGES += DeviceTestNew 
PRODUCT_PACKAGES += AgeVideoPlayerOldSing
PRODUCT_PACKAGES += SoundRecorder
#PRODUCT_PACKAGES += Android11YHKOTA

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    persist.sys.timezone=America/Los_Angeles \
    persist.sys.country=US \
    persist.sys.language=en

# lmkd
PRODUCT_PROPERTY_OVERRIDES += \
    ro.lmk.downgrade_pressure=90 \
    ro.lmk.upgrade_pressure=60

PRODUCT_PROPERTY_OVERRIDES += \
    ro.sf.lcd_density=160

# set primary display orientation to 270
PRODUCT_PROPERTY_OVERRIDES += \
    ro.surface_flinger.primary_display_orientation=ORIENTATION_0 \
    ro.vendor.sf.rotation=90 \
    ro.vendor.gsi_gsen_rotation=180

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.primary_display.user_rotation=0 \
    ro.minui.default_rotation=ROTATION_TOP \
    ro.recovery.ui.touch_high_threshold=60

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    persist.vendor.SingleCameraId=0

PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.ramconfig=none

PRODUCT_AAPT_CONFIG := mdpi xlarge hdpi xhdpi large
PRODUCT_AAPT_PREF_CONFIG :=mdpi

$(shell python $(PRODUCT_PLATFORM_PATH)/auto_generator.py $(PRODUCT_PLATFORM_PATH) preinstall app)
-include $(PRODUCT_PLATFORM_PATH)/preinstall/preinstall.mk

$(call inherit-product, $(PRODUCT_DEVICE_PATH)/*/config.mk)
$(call inherit-product, $(PRODUCT_PLATFORM_PATH)/common/*/config.mk)
