TARGET_BOARD_IC := a133
PRODUCT_BRAND := Allwinner
PRODUCT_BOARD := y13
PRODUCT_MODEL := y13
PRODUCT_MANUFACTURER := Allwinner

PRODUCT_PREBUILT_PATH := longan/out/$(TARGET_BOARD_IC)/$(PRODUCT_BOARD)/android
PRODUCT_DEVICE_PATH := $(PRODUCT_PLATFORM_PATH)/$(PRODUCT_DEVICE)

PRODUCT_BUILD_VENDOR_BOOT_IMAGE := true

CONFIG_SUPPORT_GMS := true

CONFIG_SUPPORT_CAMERA2 := true

PRODUCT_COPY_FILES += $(PRODUCT_PREBUILT_PATH)/bImage:kernel

#set speaker project(true: double speaker, false: single speaker)
#set default eq
PRODUCT_PROPERTY_OVERRIDES += \
    ro.vendor.spk_dul.used=true \
    ro.vendor.audio.eq=false

PRODUCT_PACKAGES += \
    DragonAtt \
    SoundRecorder \
    AWCamera \
    DeviceTestNew \
    AgeVideoPlayerOldSing

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    persist.sys.timezone=Asia/Shanghai \
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
    ro.surface_flinger.primary_display_orientation=ORIENTATION_90 \
    ro.vendor.sf.rotation=270 \
    ro.vendor.gsi_gsen_rotation=180

# close boot music
#PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    persist.sys.bootanim.play_sound=0

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.primary_display.user_rotation=0 \
    ro.minui.default_rotation=ROTATION_RIGHT \
    ro.recovery.ui.touch_high_threshold=60 \
    ro.qq.camera.sensor=3

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    persist.teclast.enable=1

#add teclast prop
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.config.back_camera=500 \
    ro.config.front_camera=200 \
    ro.config.cpu_cores=4 \
    ro.config.media_vol_steps=15 \
    ro.config.media_vol_default=11 \
    ro.config.alarm_vol_steps=7 \
    ro.config.alarm_vol_default=5 \
    ro.config.system_vol_steps=15 \
    ro.config.system_vol_default=11 \
    ro.config.vc_call_vol_steps=5 \
    ro.config.vc_call_vol_default=4

PRODUCT_AAPT_CONFIG := mdpi tvdpi xlarge hdpi xhdpi large
PRODUCT_AAPT_PREF_CONFIG := mdpi

#yhk add
#add by heml,for install apk before systemready
PRODUCT_COPY_FILES += \
   	$(call find-copy-subdir-files,*.apk,$(PRODUCT_PLATFORM_PATH)/precopy,/product/preinstall-inboot)

#add by heml,for Screen unlock distance
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    persist.sys.screen_unlock_distance=true

$(call inherit-product, $(PRODUCT_DEVICE_PATH)/*/config.mk)
$(call inherit-product, $(PRODUCT_PLATFORM_PATH)/common/*/config.mk)
