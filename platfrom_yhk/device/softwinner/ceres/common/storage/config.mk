LOCAL_MODULE_PATH := $(shell dirname $(lastword $(MAKEFILE_LIST)))

PRODUCT_VIRTUAL_AB ?= true

ifeq ($(PRODUCT_VIRTUAL_AB), true)

# boot hal
PRODUCT_PACKAGES += \
    android.hardware.boot@1.2-service \
    android.hardware.boot@1.2-impl \
    android.hardware.boot@1.2-impl.recovery

# Virtual AB

PRODUCT_PACKAGES += \
   bootctrl.updateboot \
   update_engine_sideload \
   update_boot   \
   update_recovery_boot   \
   update_engine  \
   update_verifier

PRODUCT_PACKAGES_DEBUG += \
    bootctl \
    update_engine_client \
    r.vendor

PRODUCT_HOST_PACKAGES += \
    brillo_update_payload

AB_OTA_UPDATER := true
AB_OTA_PARTITIONS += \
    product \
    system \
    vendor \
    boot \
    vendor_boot \
    dtbo \
    vbmeta \
    vbmeta_vendor \
    vbmeta_system \
    vendor_dlkm \
    system_dlkm \
    init_boot

# Enable Virtual A/B
$(call inherit-product, $(SRC_TARGET_DIR)/product/virtual_ab_ota/compression_with_xor.mk)
endif
TARGET_FSTAB := $(LOCAL_MODULE_PATH)/fstab.sun50iw10p1.temp
$(shell cp $(LOCAL_MODULE_PATH)/fstab.sun50iw10p1 $(TARGET_FSTAB))
ifeq ($(PRODUCT_VIRTUAL_AB), false)
    $(shell sed -i 's/,slotselect//g' $(TARGET_FSTAB))
    $(shell sed -i '/userdata/i \/dev\/block\/by-name\/cache                               \/cache       ext4     noatime,nosuid,nodev,barrier=1,data=ordered,nomblk_io_submit,noauto_da_alloc,errors=panic wait,check,formattable' $(TARGET_FSTAB))
    $(shell sed -i '/userdata/i \/dev\/block\/by-name\/recovery                            \/recovery    emmc     defaults                     defaults' $(TARGET_FSTAB))
endif
BOARD_HAS_SECURE_OS ?= true
ifneq ($(BOARD_HAS_SECURE_OS), true)
    $(shell sed -i 's/,avb=.*$$//g' $(TARGET_FSTAB))
    $(shell sed -i 's/,fileencryption=.*$$//g' $(TARGET_FSTAB))
endif

PRODUCT_COPY_FILES += \
    $(LOCAL_MODULE_PATH)/fstab.nswap:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.nswap \
    $(LOCAL_MODULE_PATH)/fstab.wswap:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.wswap \
    $(LOCAL_MODULE_PATH)/fstab.256m:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.256m \
    $(LOCAL_MODULE_PATH)/fstab.512m:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.512m \
    $(LOCAL_MODULE_PATH)/fstab.1024m:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.1024m \
    $(LOCAL_MODULE_PATH)/fstab.2048m:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.2048m \
    $(LOCAL_MODULE_PATH)/fstab.3072m:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.3072m \
    $(LOCAL_MODULE_PATH)/fstab.4096m:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.4096m \

TARGET_RECOVERY_FSTAB := $(TARGET_FSTAB)
PRODUCT_COPY_FILES += \
    $(TARGET_FSTAB):$(TARGET_COPY_OUT_VENDOR)/etc/fstab.sun50iw10p1 \
    $(TARGET_FSTAB):$(TARGET_COPY_OUT_VENDOR_RAMDISK)/first_stage_ramdisk/fstab.sun50iw10p1 \
    $(TARGET_FSTAB):$(TARGET_COPY_OUT_VENDOR_RAMDISK)/system/etc/recovery.fstab \

PRODUCT_PACKAGES += Update

# storage
PRODUCT_PROPERTY_OVERRIDES += \
    external_storage.casefold.enabled=true \
    external_storage.projid.enabled=true \

# adiantum encryption an incremental
PRODUCT_PROPERTY_OVERRIDES += \
    ro.crypto.volume.contents_mode=aes-256-xts \
    ro.crypto.volume.filenames_mode=aes-256-cts \
    ro.incremental.enable=yes \

