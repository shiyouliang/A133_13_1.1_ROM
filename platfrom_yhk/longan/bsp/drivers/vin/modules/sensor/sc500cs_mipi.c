/*
 * A V4L2 driver for GC2355 Raw cameras.
 *
 * Copyright (c) 2018 by Allwinnertech Co., Ltd.  http://www.allwinnertech.com
 *
 * Authors:  Zheng ZeQun <zequnzheng@allwinnertech.com>
 *    Liang WeiJie <liangweijie@allwinnertech.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 */

#include <linux/init.h>
#include <linux/module.h>
#include <linux/slab.h>
#include <linux/i2c.h>
#include <linux/delay.h>
#include <linux/videodev2.h>
#include <linux/clk.h>
#include <media/v4l2-device.h>
#include <media/v4l2-mediabus.h>
#include <linux/io.h>

#include "camera.h"
#include "sensor_helper.h"

MODULE_AUTHOR("lwj");
MODULE_DESCRIPTION("A low-level driver for sc500cs sensors");
MODULE_LICENSE("GPL");

#define MCLK              (24*1000*1000)
#define V4L2_IDENT_SENSOR 0xee27  //sc500cs chip ID

/*
 * Our nominal (default) frame rate.
 */

#define SENSOR_FRAME_RATE 30

/*
 * The GC0310 i2c address
 */
#define I2C_ADDR 0x6c

#define SENSOR_NAME "sc500cs_mipi"


/*
 * The default register settings
 */

static struct regval_list sensor_default_regs[] = {
    //2592x1944@30fps 2-lane 960mbps/lane
	{0x0103, 0x01},
	{0x0100, 0x00},
	{0x36e9, 0x80},
	{0x36f9, 0x80},
	{0x36ea, 0x3c},
	{0x36ec, 0x0b},
	{0x36fd, 0x14},
	{0x36e9, 0x04},
	{0x36f9, 0x04},
	{0x301f, 0x01},
	{0x3106, 0x01},
	{0x320c, 0x06},
	{0x320d, 0x40},
	{0x3249, 0x0f},
	{0x3253, 0x06},
	{0x3271, 0x13},
	{0x3273, 0x13},
	{0x3301, 0x0a},
	{0x3309, 0x60},
	{0x330a, 0x00},
	{0x330b, 0xe8},
	{0x331e, 0x41},
	{0x331f, 0x51},
	{0x3320, 0x04},
	{0x3333, 0x10},
	{0x335d, 0x60},
	{0x3364, 0x56},
	{0x336b, 0x08},
	{0x3390, 0x08},
	{0x3391, 0x18},
	{0x3392, 0x38},
	{0x3393, 0x0a},
	{0x3394, 0x24},
	{0x3395, 0x40},
	{0x33ad, 0x29},
	{0x341c, 0x04},
	{0x341d, 0x04},
	{0x341e, 0x03},
	{0x3425, 0x00},
	{0x3426, 0x00},
	{0x3622, 0xc7},
	{0x3632, 0x44},
	{0x3636, 0x6e}, 
	{0x3637, 0x08},
	{0x3638, 0x0a},
	{0x3651, 0x9d},
	{0x3670, 0x4b},
	{0x3674, 0xc0},
	{0x3675, 0x58},
	{0x3676, 0x5a},
	{0x367c, 0x18},
	{0x367d, 0x38},
	{0x3690, 0x43},
	{0x3691, 0x53},
	{0x3692, 0x53},
	{0x3699, 0x08},
	{0x369a, 0x10},
	{0x369b, 0x1f},
	{0x369c, 0x18},
	{0x369d, 0x38},
	{0x36a2, 0x08},
	{0x36a3, 0x18},
	{0x36a6, 0x08},
	{0x36a7, 0x18},
	{0x36ab, 0x40},
	{0x36ac, 0x40},
	{0x36ad, 0x40},
	{0x3901, 0x00},
	{0x3904, 0x0c},
	{0x3906, 0x3a},
	{0x391d, 0x14},
	{0x3e01, 0xf9},
	{0x3e02, 0x80},
	{0x4000, 0x00},
	{0x4001, 0x04},
	{0x4002, 0xaa},
	{0x4003, 0xaa},
	{0x4004, 0xaa},
	{0x4005, 0x00},
	{0x4006, 0x07},
	{0x4007, 0xa5},
	{0x4008, 0x00},
	{0x4009, 0xc8},
	{0x440e, 0x02},
	{0x4509, 0x28},
	{0x4837, 0x21},
	{0x5000, 0x0e},
	{0x0100, 0x01},
	{0x302d, 0x00},
	{REG_DLY,0x10},
};
static struct regval_list sensor_fmt_raw[] = {

};

/*
 * Code for dealing with controls.
 * fill with different sensor module
 * different sensor module has different settings here
 * if not support the follow function ,retrun -EINVAL
 */

static int sensor_g_exp(struct v4l2_subdev *sd, __s32 *value)
{
	struct sensor_info *info = to_state(sd);

	*value = info->exp;
	sensor_dbg("sensor_get_exposure = %d\n", info->exp);
	return 0;
}

static int sensor_s_exp(struct v4l2_subdev *sd, unsigned int exp_val)
{
	unsigned int shutter;
	unsigned char explow, expmid, exphigh;
	struct sensor_info *info = to_state(sd);

	if (exp_val > 0x3ffff)
		exp_val = 0x3ffff;
	if (exp_val < 4*16)
		exp_val = 4*16;
	
	shutter = exp_val >> 3;

	exphigh = (unsigned char)((0xff000 & shutter) >> 12);
	expmid 	= (unsigned char)((0x00ff0 & shutter) >> 4);
	explow 	= (unsigned char)((0x0000f & shutter) << 4);

	sensor_write(sd, 0x3e00, exphigh);
	sensor_write(sd, 0x3e01, expmid);
	sensor_write(sd, 0x3e02, explow);

	info->exp = exp_val;

	sensor_dbg("sensor_set_exposure = %d, shutter = %d, 0x3e00 = 0x%2x, 0x3e01 = 0x%2x, 0x3e02 = 0x%2x \n", info->exp, shutter, exphigh, expmid, explow);
	return 0;
}

static int sensor_g_gain(struct v4l2_subdev *sd, __s32 *value)
{
	struct sensor_info *info = to_state(sd);
	*value = info->gain;
	sensor_dbg("sensor_get_gain = %d\n", info->gain);
	return 0;
}

static int sensor_s_gain(struct v4l2_subdev *sd, unsigned int gain_val)
{
	struct sensor_info *info = to_state(sd);
	unsigned char Againlow = 0, Againhigh = 0, Dgainlow = 0, Dgainhigh = 0;

	if (gain_val < 1 * 32)
		gain_val = 32;
	if (gain_val > 63 * 32)
		gain_val = 63 * 32;
	
	if (gain_val < 2 * 32) {
		Dgainhigh = 0x00;
		Dgainlow  = 0x80;
		Againhigh = 0x03;
		Againlow  = gain_val;
	} else if (gain_val < 4 * 32) {
		Dgainhigh = 0x00;
		Dgainlow  = 0x80;
		Againhigh = 0x07;
		Againlow  = gain_val/2;
	} else if (gain_val < 8 * 32) {
		Dgainhigh = 0x00;
		Dgainlow  = 0x80;
		Againhigh = 0x0f;
		Againlow  = gain_val/4;
	} else if (gain_val < 1575 * 32 / 100) {
		Dgainhigh = 0x00;
		Dgainlow  = 0x80;
		Againhigh = 0x1f;
		Againlow  = gain_val/8;
	} else if (gain_val < 315 * 32 / 10) {
		Dgainhigh = 0x00;
		Dgainlow  = gain_val/504*128;
		Againhigh = 0x1f;
		Againlow  = 0x3f;
	} else if (gain_val < 63 * 32) {
		Dgainhigh = 0x01;
		Dgainlow  = gain_val/504*64;
		Againhigh = 0x1f;
		Againlow  = 0x3f;
	} else {
		Dgainhigh = 0x03;
		Dgainlow  = gain_val/504*32;
		Againhigh = 0x1f;
		Againlow  = 0x3f;
	}
	
	sensor_write(sd, 0x3e06, Dgainhigh);
	sensor_write(sd, 0x3e07, Dgainlow);
	sensor_write(sd, 0x3e08, Againhigh);
	sensor_write(sd, 0x3e09, Againlow);

	info->gain = gain_val;
	sensor_dbg("sensor_set_gain = %d, 0x3e06 = 0x%2x, 0x3e07 = 0x%2x, 0x3e08 = 0x%2x, 0x3e09 = 0x%2x \n", info->gain, Dgainhigh, Dgainlow, Againhigh, Againlow);

	return 0;
}

static int sc500cs_sensor_vts = 0;
static int sensor_s_exp_gain(struct v4l2_subdev *sd,
				struct sensor_exp_gain *exp_gain)
{
        int exp_val, gain_val,shutter,frame_length;  
	unsigned char explow=0,expmid=0,exphigh=0;
	unsigned char Againlow=0,Againhigh=0,Dgainlow=0,Dgainhigh=0;  
	struct sensor_info *info = to_state(sd);

	exp_val = exp_gain->exp_val;
	gain_val = exp_gain->gain_val;
	if(gain_val<1*32)
		gain_val=32;
	if(gain_val>63*32)
		gain_val=63*32;

	if(exp_val>0x3ffff)
		exp_val=0x3ffff;
	if (exp_val < 4*16)
		exp_val = 4*16;
  
	if (gain_val < 2 * 32) {
		Dgainhigh = 0x00;
		Dgainlow  = 0x80;
		Againhigh = 0x03;
		Againlow  = gain_val;
	} else if (gain_val < 4 * 32) {
		Dgainhigh = 0x00;
		Dgainlow  = 0x80;
		Againhigh = 0x07;
		Againlow  = gain_val/2;
	} else if (gain_val < 8 * 32) {
		Dgainhigh = 0x00;
		Dgainlow  = 0x80;
		Againhigh = 0x0f;
		Againlow  = gain_val/4;
	} else if (gain_val < 1575 * 32 / 100) {
		Dgainhigh = 0x00;
		Dgainlow  = 0x80;
		Againhigh = 0x1f;
		Againlow  = gain_val/8;
	} else if (gain_val < 315 * 32 / 10) {
		Dgainhigh = 0x00;
		Dgainlow  = gain_val/504*128;
		Againhigh = 0x1f;
		Againlow  = 0x3f;
	} else if (gain_val < 63 * 32) {
		Dgainhigh = 0x01;
		Dgainlow  = gain_val/504*64;
		Againhigh = 0x1f;
		Againlow  = 0x3f;
	} else {
		Dgainhigh = 0x03;
		Dgainlow  = gain_val/504*32;
		Againhigh = 0x1f;
		Againlow  = 0x3f;
	}
  
	shutter = exp_val>>3;    
	if(shutter  > sc500cs_sensor_vts*2 - 10)
		frame_length = (shutter + 10)/2;
	else
		frame_length = sc500cs_sensor_vts;
	
	exphigh = (unsigned char)((0xff000 & shutter) >> 12);
	expmid 	= (unsigned char)((0x00ff0 & shutter) >> 4);
	explow 	= (unsigned char)((0x0000f & shutter) << 4);

	sensor_write(sd, 0x320e, (frame_length >> 8) & 0xff);
	sensor_write(sd, 0x320f, (frame_length & 0xff));
	sensor_write(sd, 0x3e06, Dgainhigh);
	sensor_write(sd, 0x3e07, Dgainlow);
	sensor_write(sd, 0x3e08, Againhigh);
	sensor_write(sd, 0x3e09, Againlow);  
	sensor_write(sd, 0x3e00, exphigh);
	sensor_write(sd, 0x3e01, expmid);
	sensor_write(sd, 0x3e02, explow);
	info->exp = exp_val;
	info->gain = gain_val;

	sensor_dbg("sc500cs_sensor_vts = %d, frame_length = %d\n", sc500cs_sensor_vts, frame_length);
	sensor_dbg("sensor_s_exp_gain = %d, shutter = %d, 0x3e00 = 0x%2x, 0x3e01 = 0x%2x, 0x3e02 = 0x%2x, fps = %d \n", info->exp, shutter, exphigh, expmid, explow, 96000000/(1600*frame_length));
	sensor_dbg("sensor_s_exp_gain = %d, 0x3e06 = 0x%2x, 0x3e07 = 0x%2x, 0x3e08 = 0x%2x, 0x3e09 = 0x%2x \n", info->gain, Dgainhigh, Dgainlow, Againhigh, Againlow);
	return 0;
}

static int sensor_s_sw_stby(struct v4l2_subdev *sd, int on_off)
{
	int ret = 0;
	return ret;
}

/*
 * Stuff that knows about the sensor.
 */
static int sensor_power(struct v4l2_subdev *sd, int on)
{
	printk("jeff sc500cs sensor_power\n");
	switch (on) {
	case STBY_ON:
		sensor_print("STBY_ON!\n");
        cci_lock(sd);
        sensor_s_sw_stby(sd, STBY_ON);
        usleep_range(1000, 1200);
        cci_unlock(sd);
        break;
	case STBY_OFF:
		sensor_print("STBY_OFF!\n");
        cci_lock(sd);
        usleep_range(1000, 1200);
        sensor_s_sw_stby(sd, STBY_OFF);
        cci_unlock(sd);	
		break;
	case PWR_ON:
		sensor_print("PWR_ON!100\n");
		cci_lock(sd);
		vin_gpio_set_status(sd, PWDN, 1);
		vin_gpio_write(sd, RESET, CSI_GPIO_HIGH);
		vin_gpio_set_status(sd, POWER_EN, 1);
		vin_gpio_write(sd, PWDN, CSI_GPIO_LOW);
		vin_gpio_write(sd, RESET, CSI_GPIO_LOW);
		vin_gpio_write(sd, POWER_EN, CSI_GPIO_HIGH);
		usleep_range(7000, 8000);
		vin_set_pmu_channel(sd, IOVDD, ON);
		usleep_range(7000, 8000);
		vin_set_pmu_channel(sd, AVDD, ON);
		vin_set_pmu_channel(sd, AFVDD, ON);
		usleep_range(7000, 8000);
		vin_set_pmu_channel(sd, DVDD, ON);
		usleep_range(7000, 8000);
		vin_set_mclk_freq(sd, MCLK);
		vin_set_mclk(sd, ON);
		usleep_range(10000, 12000);
		vin_gpio_write(sd, RESET, CSI_GPIO_HIGH);
		vin_gpio_write(sd, PWDN, CSI_GPIO_HIGH);
		vin_set_pmu_channel(sd, CAMERAVDD, ON);/*AFVCC ON*/
		usleep_range(10000, 12000);
		cci_unlock(sd);
		break;
	case PWR_OFF:
		sensor_print("PWR_OFF!\n");
		cci_lock(sd);
		vin_gpio_write(sd, PWDN, CSI_GPIO_HIGH);
		vin_gpio_write(sd, RESET, CSI_GPIO_HIGH);
		vin_set_mclk(sd, OFF);
		usleep_range(7000, 8000);
		vin_set_pmu_channel(sd, DVDD, OFF);
		vin_gpio_write(sd, PWDN, CSI_GPIO_LOW);
		vin_gpio_write(sd, RESET, CSI_GPIO_LOW);
		vin_gpio_write(sd, POWER_EN, CSI_GPIO_LOW);
		vin_set_pmu_channel(sd, AVDD, OFF);
		vin_set_pmu_channel(sd, IOVDD, OFF);
		vin_set_pmu_channel(sd, AFVDD, OFF);
		vin_set_pmu_channel(sd, CAMERAVDD, OFF);/*AFVCC ON*/
		cci_unlock(sd);
		break;
	default:
		return -EINVAL;
	}

	return 0;
}

static int sensor_reset(struct v4l2_subdev *sd, u32 val)
{
	switch (val) {
	case 0:
		vin_gpio_write(sd, RESET, CSI_GPIO_HIGH);
		usleep_range(100, 120);
		break;
	case 1:
		vin_gpio_write(sd, RESET, CSI_GPIO_LOW);
		usleep_range(100, 120);
		break;
	default:
		return -EINVAL;
	}
	return 0;
}

static int sensor_detect(struct v4l2_subdev *sd)
{
	data_type rdval;

	sensor_read(sd, 0x3107, &rdval);
	sensor_print("0x3107 =  0x%x\n", rdval);
	if(rdval != 0xee){
		sensor_print("sc500cs %s error, chip found is not an target chip",__func__);
		return -ENODEV;
	}
	sensor_read(sd, 0x3108, &rdval);
	sensor_print("0x3108 =  0x%x\n", rdval);
	if(rdval != 0x27){
		sensor_print("sc500cs %s error, chip found is not an target chip",__func__);
		return -ENODEV;
	}
	return 0;
}

static int sensor_init(struct v4l2_subdev *sd, u32 val)
{
	int ret;
	struct sensor_info *info = to_state(sd);

	sensor_print("sensor_init\n");

	/*Make sure it is a target sensor */
	ret = sensor_detect(sd);
	if (ret) {
		sensor_err("chip found is not an target chip.\n");
		return ret;
	}

	info->focus_status = 0;
	info->low_speed = 0;
	info->width = 2592;
	info->height = 1944;
	info->hflip = 0;
	info->vflip = 0;
	info->gain = 0;

	info->tpf.numerator = 1;
	info->tpf.denominator = 30;	/* 30fps */

	return 0;
}

static long sensor_ioctl(struct v4l2_subdev *sd, unsigned int cmd, void *arg)
{
	int ret = 0;
	struct sensor_info *info = to_state(sd);

	switch (cmd) {
	case GET_CURRENT_WIN_CFG:
		if (info->current_wins != NULL) {
			memcpy(arg, info->current_wins,
				sizeof(struct sensor_win_size));
			ret = 0;
		} else {
			sensor_err("empty wins!\n");
			ret = -1;
		}
		break;
	case SET_FPS:
		ret = 0;
		break;
	case VIDIOC_VIN_SENSOR_EXP_GAIN:
		ret = sensor_s_exp_gain(sd, (struct sensor_exp_gain *)arg);
		break;
	case VIDIOC_VIN_SENSOR_CFG_REQ:
		sensor_cfg_req(sd, (struct sensor_config *)arg);
		break;
	case VIDIOC_VIN_ACT_INIT:
		ret = actuator_init(sd, (struct actuator_para *)arg);
		break;
	case VIDIOC_VIN_ACT_SET_CODE:
		ret = actuator_set_code(sd, (struct actuator_ctrl *)arg);
		break;
	case VIDIOC_VIN_FLASH_EN:
		ret = flash_en(sd, (struct flash_para *)arg);
		break;
	default:
		return -EINVAL;
	}
	return ret;
}

/*
 * Store information about the video data format.
 */
static struct sensor_format_struct sensor_formats[] = {
	{
		.desc = "Raw RGB Bayer",
		.mbus_code = MEDIA_BUS_FMT_SBGGR10_1X10,
		.regs = sensor_fmt_raw,
		.regs_size = ARRAY_SIZE(sensor_fmt_raw),
		.bpp = 1
	},
};
#define N_FMTS ARRAY_SIZE(sensor_formats)

/*
 * Then there is the issue of window sizes.  Try to capture the info here.
 */

static struct sensor_win_size sensor_win_sizes[] = {
	{
	  .width	  = 2592,
	  .height	  = 1944,
	  .hoffset	  = 0,
	  .voffset	  = 0,
	  .hts		  = 1600,
	  .vts		  = 2000,
	  .pclk 	  = 96*1000*1000,
	  .mipi_bps   = 960*1000*1000,
	  .fps_fixed  = 30,
	  .bin_factor = 1,
	  .intg_min   = 4 << 4,
	  .intg_max   = (2000*30/5-10)  << 4,  //5fps
	  .gain_min   = 1<<5,
	  .gain_max   = 63<<5,
	  .regs 	  = sensor_default_regs,//
	  .regs_size  = ARRAY_SIZE(sensor_default_regs),//
	  .set_size   = NULL,
	},
};

#define N_WIN_SIZES (ARRAY_SIZE(sensor_win_sizes))

static int sensor_g_mbus_config(struct v4l2_subdev *sd, unsigned int pad,
				struct v4l2_mbus_config *cfg)
{
	cfg->type = V4L2_MBUS_CSI2_DPHY;
	cfg->flags = 0 | V4L2_MBUS_CSI2_2_LANE | V4L2_MBUS_CSI2_CHANNEL_0;

	return 0;
}

static int sensor_g_ctrl(struct v4l2_ctrl *ctrl)
{
	struct sensor_info *info =
			container_of(ctrl->handler, struct sensor_info, handler);
	struct v4l2_subdev *sd = &info->sd;

	switch (ctrl->id) {
	case V4L2_CID_GAIN:
		return sensor_g_gain(sd, &ctrl->val);
	case V4L2_CID_EXPOSURE:
		return sensor_g_exp(sd, &ctrl->val);
	}
	return -EINVAL;
}

static int sensor_s_ctrl(struct v4l2_ctrl *ctrl)
{
	struct sensor_info *info =
			container_of(ctrl->handler, struct sensor_info, handler);
	struct v4l2_subdev *sd = &info->sd;

	switch (ctrl->id) {
	case V4L2_CID_GAIN:
		return sensor_s_gain(sd, ctrl->val);
	case V4L2_CID_EXPOSURE:
		return sensor_s_exp(sd, ctrl->val);
	}
	return -EINVAL;
}

static int sensor_reg_init(struct sensor_info *info)
{
	int ret;
	struct v4l2_subdev *sd = &info->sd;
	struct sensor_format_struct *sensor_fmt = info->fmt;
	struct sensor_win_size *wsize = info->current_wins;

	ret = sensor_write_array(sd, sensor_default_regs,
				ARRAY_SIZE(sensor_default_regs));
	if (ret < 0) {
		sensor_err("write sensor_default_regs error\n");
		return ret;
	}

	sensor_print("sensor_reg_init\n");

	sensor_write_array(sd, sensor_fmt->regs, sensor_fmt->regs_size);

	if (wsize->regs)
		sensor_write_array(sd, wsize->regs, wsize->regs_size);

	if (wsize->set_size)
		wsize->set_size(sd);

	info->width = wsize->width;
	info->height = wsize->height;
	info->exp = 0;
	info->gain = 0;
	sc500cs_sensor_vts = wsize->vts;
	sensor_print("s_fmt set width = %d, height = %d\n", wsize->width,
				wsize->height);

	return 0;
}

static int sensor_s_stream(struct v4l2_subdev *sd, int enable)
{
	struct sensor_info *info = to_state(sd);

	sensor_print("%s on = %d, %d*%d fps: %d code: %x\n", __func__, enable,
			info->current_wins->width, info->current_wins->height,
			info->current_wins->fps_fixed, info->fmt->mbus_code);

	if (!enable)
		return 0;

	return sensor_reg_init(info);
}


/* ----------------------------------------------------------------------- */

static const struct v4l2_ctrl_ops sensor_ctrl_ops = {
	.g_volatile_ctrl = sensor_g_ctrl,
	.s_ctrl = sensor_s_ctrl,
};

static const struct v4l2_subdev_core_ops sensor_core_ops = {
	.reset = sensor_reset,
	.init = sensor_init,
	.s_power = sensor_power,
	.ioctl = sensor_ioctl,
#ifdef CONFIG_COMPAT
	.compat_ioctl32 = sensor_compat_ioctl32,
#endif
};

static const struct v4l2_subdev_video_ops sensor_video_ops = {
	.s_stream = sensor_s_stream,

};

static const struct v4l2_subdev_pad_ops sensor_pad_ops = {
	.enum_mbus_code = sensor_enum_mbus_code,
	.enum_frame_size = sensor_enum_frame_size,
	.get_fmt = sensor_get_fmt,
	.set_fmt = sensor_set_fmt,
	.get_mbus_config = sensor_g_mbus_config,
};

static const struct v4l2_subdev_ops sensor_ops = {
	.core = &sensor_core_ops,
	.video = &sensor_video_ops,
	.pad = &sensor_pad_ops,
};

/* ----------------------------------------------------------------------- */
static struct cci_driver cci_drv = {
		.name = SENSOR_NAME,
		.addr_width = CCI_BITS_16,
		.data_width = CCI_BITS_8,
};

static int sensor_init_controls(struct v4l2_subdev *sd, const struct v4l2_ctrl_ops *ops)
{
	struct sensor_info *info = to_state(sd);
	struct v4l2_ctrl_handler *handler = &info->handler;
	struct v4l2_ctrl *ctrl;
	int ret = 0;

	v4l2_ctrl_handler_init(handler, 2);

	v4l2_ctrl_new_std(handler, ops, V4L2_CID_GAIN, 1 * 1600,
				256 * 1600, 1, 1 * 1600);
	ctrl = v4l2_ctrl_new_std(handler, ops, V4L2_CID_EXPOSURE, 0,
				65536 * 16, 1, 0);
	if (ctrl != NULL)
		ctrl->flags |= V4L2_CTRL_FLAG_VOLATILE;

	if (handler->error) {
		ret = handler->error;
		v4l2_ctrl_handler_free(handler);
	}

	sd->ctrl_handler = handler;

	return ret;
}

static int sensor_probe(struct i2c_client *client,
			const struct i2c_device_id *id)
{
	struct v4l2_subdev *sd;
	struct sensor_info *info;

	info = kzalloc(sizeof(struct sensor_info), GFP_KERNEL);
	if (info == NULL)
		return -ENOMEM;

	mutex_init(&info->lock);
	sd = &info->sd;

	cci_dev_probe_helper(sd, client, &sensor_ops, &cci_drv);
	sensor_init_controls(sd, &sensor_ctrl_ops);

#ifdef CONFIG_SAME_I2C
    info->sensor_i2c_addr = I2C_ADDR >> 1;
#endif
	info->fmt = &sensor_formats[0];
	info->fmt_pt = &sensor_formats[0];
	info->win_pt = &sensor_win_sizes[0];
	info->fmt_num = N_FMTS;
	info->win_size_num = N_WIN_SIZES;
	info->sensor_field = V4L2_FIELD_NONE;
	info->stream_seq = MIPI_BEFORE_SENSOR;
	info->af_first_flag = 1;
	//info->time_hs = 0x23;
	//info->time_hs = 0x09;
	//info->time_hs = 0xa0;
	info->exp = 0;
	info->gain = 0;

	return 0;
}

static int sensor_remove(struct i2c_client *client)
{
	struct v4l2_subdev *sd;

	sd = cci_dev_remove_helper(client, &cci_drv);

	kfree(to_state(sd));
	return 0;
}

static const struct i2c_device_id sensor_id[] = {
	{SENSOR_NAME, 0},
	{}
};

MODULE_DEVICE_TABLE(i2c, sensor_id);

static struct i2c_driver sensor_driver = {
		.driver = {
				.owner = THIS_MODULE,
				.name = SENSOR_NAME,
				},
		.probe = sensor_probe,
		.remove = sensor_remove,
		.id_table = sensor_id,
};
static __init int init_sensor(void)
{
	return cci_dev_init_helper(&sensor_driver);
}

static __exit void exit_sensor(void)
{
	cci_dev_exit_helper(&sensor_driver);
}

module_init(init_sensor);
module_exit(exit_sensor);
