/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Size;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.Sliceable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CameraPreferenceController extends BasePreferenceController {
    private Context mContext;
    // init magnification
    private final static float BASE_PIXEL_MAGNIFICATION = 1000 * 1000;
    private final static int SHOW_PIXEL_MAGNIFICATION = 100;

    public CameraPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext=context;
    }

    @Override
    public int getAvailabilityStatus() {
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        List<String> cameraPixels = getCameraPixels();
        String frontcamera = cameraPixels.get(CameraCharacteristics.LENS_FACING_BACK);
        String backcamera = cameraPixels.get(CameraCharacteristics.LENS_FACING_FRONT);
        return mContext.getString(R.string.camera_info_summary,
                frontcamera, backcamera);
        //return mContext.getText(R.string.camera_info_summary);
    }

    public List<String> getCameraPixels() {
        List<String> pixels = Stream.of(
            SystemProperties.get("ro.config.front_camera", "800"),
            SystemProperties.get("ro.config.back_camera", "1300")
        ).collect(Collectors.toList());
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String item : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(item);
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = new Size[0];
                if (streamConfigurationMap != null) {
                    sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                }
                DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                int maxPixel = displayMetrics.widthPixels * displayMetrics.heightPixels;
                for (Size size : sizes) {
                    int sizePixel = size.getHeight() * size.getWidth();
                    if (maxPixel < sizePixel) {
                        maxPixel = sizePixel;
                    }
                }
                pixels.set(Integer.parseInt(item),
                            String.valueOf(Math.round(maxPixel / BASE_PIXEL_MAGNIFICATION) * SHOW_PIXEL_MAGNIFICATION));
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return pixels;
    }
}
