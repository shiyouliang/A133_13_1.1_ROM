CONFIG_SUPPORT_GMS ?= true

ifeq ($(CONFIG_SUPPORT_GMS),true)
    ifeq ($(wildcard vendor/partner_gms/products),)
        CONFIG_SUPPORT_GMS := false
        $(warning CONFIG_SUPPORT_GMS true but no gms packages found, force set it to false)
    endif
endif

ifeq ($(CONFIG_SUPPORT_GMS),true)
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.com.google.clientidbase=android-hena

# for setup wizard debug
# set gms checkin timeout to 300s to avoid frequent connection timout
# set phenotype sync timeout to 300s to avoid frequent phenotype sync timeout
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    log.tag.SetupWizard=VERBOSE \
    setupwizard.gms_checkin_timeout_ms=300000 \
    setupwizard.phenotype_sync_timeout_ms=3000000

ifeq ($(CONFIG_LOW_RAM_DEVICE),true)
    MODULE_BUILD_FROM_SOURCE := false
    # Setting this to true or deleting it will result in build failures.
    # check the known issues section below.
    MAINLINE_COMPRESS_APEX_ALL := false
    $(call inherit-product-if-exists, vendor/partner_modules/build/mainline_modules_low_ram.mk)
    # include gms package for 2gb go. Start from Android13, go device must have 2GB memory at least
    $(call inherit-product-if-exists, vendor/partner_gms/products/gms_go_2gb_eea_v2_type4c.mk)
else
    $(call inherit-product-if-exists, vendor/partner_modules/build/mainline_modules.mk)
    ifeq ($(CONFIG_3GB_DEVICE), true)
        # include gms package for 3gb normal device.
        $(call inherit-product-if-exists, vendor/partner_gms/products/gms_3gb.mk)
    else
        # include gms package for at least 4gb normal device.
        $(call inherit-product-if-exists, vendor/partner_gms/products/gms.mk)
    endif# ifeq ($(CONFIG_3GB_DEVICE),true)
endif # ifeq ($(CONFIG_LOW_RAM_DEVICE),true)
PRODUCT_PACKAGES += GooglePackageInstallerOverlay
# GMS mada Compliance Checklist M53 request EmergencyInfo
PRODUCT_PACKAGES += EmergencyInfo
else # CONFIG_SUPPORT_GMS == false
    # we build module from source if not support GMS
    MODULE_BUILD_FROM_SOURCE := true
endif # ifeq ($(CONFIG_SUPPORT_GMS),true)
