/*
 * A V4L2 driver for sc201cs Raw cameras.
 *
 * Copyright (c) 2017 by Allwinnertech Co., Ltd.  http://www.allwinnertech.com
 *
 * Authors:  Zhao Wei <zhaowei@allwinnertech.com>
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
MODULE_DESCRIPTION("A low-level driver for sc201cs sensors");
MODULE_LICENSE("GPL");

#define MCLK			  (24*1000*1000)
#define V4L2_IDENT_SENSOR 0xeb2c

/*
 * Our nominal (default) frame rate.
 */

#define SENSOR_FRAME_RATE 30

/*
 * The sc201cs i2c address
 */
#define I2C_ADDR 0x60

#define SC201CS_SENSOR_GAIN_MAP_SIZE         6
#define SC201CS_SENSOR_GAIN_MAX_VALID_INDEX  6
static u32 SC201CS_AGC_Param[SC201CS_SENSOR_GAIN_MAP_SIZE][2] = {
{16, 0x00}, 
{32, 0x01}, 
{64, 0x03}, 
{128, 0x07}, 
{256, 0x0f}, 
{512, 0x1f}, 
};

#define SENSOR_NAME "sc201cs_mipi"


struct cfg_array {		/* coming later */
	struct regval_list *regs;
	int size;
};
/*
 * The default register settings
 */

static struct regval_list sensor_default_regs[] = {
// MCLK 24Mhz, 720Mbps per lane
// MIPI-1Lane, 1600 x 1200 @ 30fps	
// Version: V01P10
	{0x0103,0x01},
	{0x0100,0x00},
	{0x36e9,0x80},
	{0x36ea,0x0f},  //HTS 1920
	{0x36eb,0x25},
	{0x36ed,0x04},   //VTS 1250
	{0x36e9,0x01},
	{0x301f,0x01},
	{0x3248,0x02},
	{0x3253,0x0a},
	{0x3301,0xff},
	{0x3302,0xff},
	{0x3303,0x10},
	{0x3306,0x28},
	{0x3307,0x02},
	{0x330a,0x00},
	{0x330b,0xb0},
	{0x3318,0x02},
	{0x3320,0x06},
	{0x3321,0x02},
	{0x3326,0x12},
	{0x3327,0x0e},
	{0x3328,0x03},
	{0x3329,0x0f},
	{0x3364,0x0f},
	{0x33b3,0x40},
	{0x33f9,0x2c},
	{0x33fb,0x38},
	{0x33fc,0x0f},
	{0x33fd,0x1f},
	{0x349f,0x03},
	{0x34a6,0x01},
	{0x34a7,0x1f},
	{0x34a8,0x40},
	{0x34a9,0x30},
	{0x34ab,0xa6},
	{0x34ad,0xa6},
	{0x3622,0x60},
	{0x3625,0x08},
	{0x3630,0xa8},
	{0x3631,0x84},
	{0x3632,0x90},
	{0x3633,0x43},
	{0x3634,0x09},
	{0x3635,0x82},
	{0x3636,0x48},
	{0x3637,0xe4},
	{0x3641,0x22},
	{0x3670,0x0e},
	{0x3674,0xc0},
	{0x3675,0xc0},
	{0x3676,0xc0},
	{0x3677,0x86},
	{0x3678,0x88},
	{0x3679,0x8c},
	{0x367c,0x01},
	{0x367d,0x0f},
	{0x367e,0x01},
	{0x367f,0x0f},
	{0x3690,0x43},
	{0x3691,0x43},
	{0x3692,0x53},
	{0x369c,0x01},
	{0x369d,0x1f},
	{0x3900,0x0d},
	{0x3904,0x06},
	{0x3905,0x98},
	{0x391b,0x81},
	{0x391c,0x10},
	{0x391d,0x19},
	{0x3949,0xc8},
	{0x394b,0x64},
	{0x3952,0x02},
	{0x3e00,0x00},
	{0x3e01,0x4d},
	{0x3e02,0xe0},
	{0x4502,0x34},
	{0x4509,0x30},
	{0x0100,0x01},
	{REG_DLY,0x10},
};

static struct regval_list sensor_1600_1200_regs[] = {
// MCLK 24Mhz, 720Mbps per lane
// MIPI-1Lane, 1600 x 1200 @ 30fps	
// Version: V01P10
	{0x0103,0x01},
	{0x0100,0x00},
	{0x36e9,0x80},
	{0x36ea,0x0f},  //HTS 1920
	{0x36eb,0x25},
	{0x36ed,0x04},   //VTS 1250
	{0x36e9,0x01},
	{0x301f,0x01},
	{0x3248,0x02},
	{0x3253,0x0a},
	{0x3301,0xff},
	{0x3302,0xff},
	{0x3303,0x10},
	{0x3306,0x28},
	{0x3307,0x02},
	{0x330a,0x00},
	{0x330b,0xb0},
	{0x3318,0x02},
	{0x3320,0x06},
	{0x3321,0x02},
	{0x3326,0x12},
	{0x3327,0x0e},
	{0x3328,0x03},
	{0x3329,0x0f},
	{0x3364,0x0f},
	{0x33b3,0x40},
	{0x33f9,0x2c},
	{0x33fb,0x38},
	{0x33fc,0x0f},
	{0x33fd,0x1f},
	{0x349f,0x03},
	{0x34a6,0x01},
	{0x34a7,0x1f},
	{0x34a8,0x40},
	{0x34a9,0x30},
	{0x34ab,0xa6},
	{0x34ad,0xa6},
	{0x3622,0x60},
	{0x3625,0x08},
	{0x3630,0xa8},
	{0x3631,0x84},
	{0x3632,0x90},
	{0x3633,0x43},
	{0x3634,0x09},
	{0x3635,0x82},
	{0x3636,0x48},
	{0x3637,0xe4},
	{0x3641,0x22},
	{0x3670,0x0e},
	{0x3674,0xc0},
	{0x3675,0xc0},
	{0x3676,0xc0},
	{0x3677,0x86},
	{0x3678,0x88},
	{0x3679,0x8c},
	{0x367c,0x01},
	{0x367d,0x0f},
	{0x367e,0x01},
	{0x367f,0x0f},
	{0x3690,0x43},
	{0x3691,0x43},
	{0x3692,0x53},
	{0x369c,0x01},
	{0x369d,0x1f},
	{0x3900,0x0d},
	{0x3904,0x06},
	{0x3905,0x98},
	{0x391b,0x81},
	{0x391c,0x10},
	{0x391d,0x19},
	{0x3949,0xc8},
	{0x394b,0x64},
	{0x3952,0x02},
	{0x3e00,0x00},
	{0x3e01,0x4d},
	{0x3e02,0xe0},
	{0x4502,0x34},
	{0x4509,0x30},
	{0x0100,0x01},
	{REG_DLY,0x10},
};

/*
 * Here we'll try to encapsulate the changes for just the output
 * video format.
 *
 */

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

static int sc201cs_sensor_vts;
/*static int sc201cs_sensor_svr;*/
//static unsigned char shutter_delay = 1;
//static unsigned char shutter_delay_cnt;
//static unsigned char fps_change_flag;
//static unsigned char reg_hold;

static int sensor_s_exp(struct v4l2_subdev *sd, unsigned int exp_val)
{
	unsigned char explow, expmid, exphigh;
	struct sensor_info *info = to_state(sd);

	if (exp_val > 0x1ffff)
		exp_val = 0x1ffff;
	if (exp_val < 4)
		exp_val = 4;

	exphigh = (unsigned char)((0x0f000 & exp_val) >> 12);
	expmid 	= (unsigned char)((0x00ff0 & exp_val) >> 4);
	explow 	= (unsigned char)((0x0000f & exp_val) << 4);

	sensor_write(sd, 0x3e00, exphigh);
	sensor_write(sd, 0x3e01, expmid);
	sensor_write(sd, 0x3e02, explow);
	
	info->exp = exp_val;

	sensor_dbg("sensor_set_exposure = %d, 0x3e00 = 0x%2x, 0x3e01 = 0x%2x, 0x3e02 = 0x%2x \n", info->exp, exphigh, expmid, explow);
	return 0;
}

static int sensor_g_gain(struct v4l2_subdev *sd, __s32 *value)
{
	struct sensor_info *info = to_state(sd);
	*value = info->gain;
	sensor_dbg("sensor_get_gain = %d\n", info->gain);
	return 0;
}

static int sensor_s_gain(struct v4l2_subdev *sd, int gain_val)
{
	struct sensor_info *info = to_state(sd);
	//unsigned char Againlow = 0, Againhigh = 0;
	unsigned char Dgainlow = 0, Dgainhigh = 0, Dgain = 0;
    int gain_index;
	
	for (gain_index = SC201CS_SENSOR_GAIN_MAX_VALID_INDEX - 1; gain_index >= 0; gain_index--)
      if (gain_val >= SC201CS_AGC_Param[gain_index][0])
        break;	
    Dgain =	gain_val*64/SC201CS_AGC_Param[gain_index][0];
	if((Dgain>=0x10)&&(Dgain <2*0x10))	
	{
		Dgainhigh = 0x00;
		Dgainlow = Dgain;
	}
	else if((Dgain>=2*0x10)&&(Dgain <4*0x10))	
	{
		Dgainhigh = 0x01;
		Dgainlow = Dgain/2;	
	}	
	else if((Dgain>=4*0x10)&&(Dgain <8*0x10))	
	{
		Dgainhigh = 0x03;
		Dgainlow = Dgain/4;	
	}
	
	
	sensor_write(sd, 0x3e06, Dgainhigh);
	sensor_write(sd, 0x3e07, Dgainlow);
	//sensor_write(sd, 0x3e08, Againhigh);
	sensor_write(sd, 0x3e09, SC201CS_AGC_Param[gain_index][1]);

	info->gain = gain_val;
	//sensor_dbg("sensor_set_gain = %d, 0x3e06 = 0x%2x, 0x3e07 = 0x%2x, 0x3e08 = 0x%2x, 0x3e09 = 0x%2x \n", info->gain, Dgainhigh, Dgainlow, Againhigh, Againlow);
	//sensor_dbg("sensor_set_gain = %d, 0x3e06 = 0x%2x, 0x3e07 = 0x%2x,  0x3e09 = 0x%2x \n", info->gain, Dgainhigh, Dgainlow,  Againlow);

	return 0;
}

static int sensor_s_exp_gain(struct v4l2_subdev *sd,
			     struct sensor_exp_gain *exp_gain)
{
        int exp_val, gain_val,shutter,frame_length;  
	unsigned char explow=0,expmid=0,exphigh=0;
	unsigned char Againlow=0,Dgainlow=0,Dgainhigh=0, Dgain = 0;
	int gain_index;
	struct sensor_info *info = to_state(sd);


	exp_val = exp_gain->exp_val;
	gain_val = exp_gain->gain_val;
	
	if(gain_val<1*16)
	gain_val=16;
	if(gain_val>63*16)
		gain_val=63*16;
	
	
	
	for (gain_index = SC201CS_SENSOR_GAIN_MAX_VALID_INDEX - 1; gain_index >= 0; gain_index--)
      if (gain_val >= SC201CS_AGC_Param[gain_index][0])
        break;	
    Dgain =	gain_val*64/SC201CS_AGC_Param[gain_index][0];
	if((Dgain>=0x10)&&(Dgain <2*0x10))	
	{
		Dgainhigh = 0x00;
		Dgainlow = Dgain;
	}
	else if((Dgain>=2*0x10)&&(Dgain <4*0x10))	
	{
		Dgainhigh = 0x01;
		Dgainlow = Dgain/2;	
	}	
	else if((Dgain>=4*0x10)&&(Dgain <8*0x10))	
	{
		Dgainhigh = 0x03;
		Dgainlow = Dgain/4;	
	}
	
	
	if (exp_val > 0x1ffff)
		exp_val = 0x1ffff;
	if (exp_val < 4)
		exp_val = 4;

	shutter = exp_val;    
	if(shutter  > sc201cs_sensor_vts - 4)
		frame_length = shutter + 4;
	else
		frame_length = sc201cs_sensor_vts;
	
	
	
	exphigh = (unsigned char)((0x0f000 & exp_val) >> 12);
	expmid 	= (unsigned char)((0x00ff0 & exp_val) >> 4);
	explow 	= (unsigned char)((0x0000f & exp_val) << 4);

  
	

	sensor_write(sd, 0x320e, (frame_length >> 8) & 0xff);
	sensor_write(sd, 0x320f, (frame_length & 0xff));
	sensor_write(sd, 0x3e06, Dgainhigh);
	sensor_write(sd, 0x3e07, Dgainlow);
	//sensor_write(sd, 0x3e08, Againhigh);
	sensor_write(sd, 0x3e09, SC201CS_AGC_Param[gain_index][1]);
	Againlow = SC201CS_AGC_Param[gain_index][1];  
	sensor_write(sd, 0x3e00, exphigh);
	sensor_write(sd, 0x3e01, expmid);
	sensor_write(sd, 0x3e02, explow);
	info->exp = exp_val;
	info->gain = gain_val;

	sensor_dbg("sensor_s_exp = %d, 0x3e00 = 0x%2x, 0x3e01 = 0x%2x, 0x3e02 = 0x%2x ,fps = %d \n", info->exp, exphigh, expmid, explow, 72000000/(1920*frame_length));
	sensor_dbg("sensor_s_gain = %d, 0x3e06 = 0x%2x, 0x3e07 = 0x%2x,  0x3e09 = 0x%2x \n", info->gain, Dgainhigh, Dgainlow,  Againlow);
	return 0;
}

static int sensor_s_sw_stby(struct v4l2_subdev *sd, int on_off)
{
	int ret;
	data_type rdval;

	ret = sensor_read(sd, 0x0100, &rdval);
	if (ret != 0)
		return ret;

	if (on_off == STBY_ON)
		ret = sensor_write(sd, 0x0100, rdval&0xfe);
	else
		ret = sensor_write(sd, 0x0100, rdval|0x01);
	return ret;
}

/*
 * Stuff that knows about the sensor.
 */
static int sensor_power(struct v4l2_subdev *sd, int on)
{
	int ret = 0;

	switch (on) {
	case STBY_ON:
		sensor_dbg("STBY_ON!\n");
		cci_lock(sd);
		ret = sensor_s_sw_stby(sd, STBY_ON);
		if (ret < 0)
			sensor_err("soft stby falied!\n");
		usleep_range(10000, 12000);
		cci_unlock(sd);
		break;
	case STBY_OFF:
		sensor_dbg("STBY_OFF!\n");
		cci_lock(sd);
		usleep_range(10000, 12000);
		ret = sensor_s_sw_stby(sd, STBY_OFF);
		if (ret < 0)
			sensor_err("soft stby off falied!\n");
		cci_unlock(sd);
		break;
	case PWR_ON:
		sensor_dbg("PWR_ON!\n");
		cci_lock(sd);
		vin_gpio_set_status(sd, PWDN, 1);
		vin_gpio_set_status(sd, RESET, 1);
		vin_gpio_set_status(sd, POWER_EN, 1);
		vin_gpio_write(sd, RESET, CSI_GPIO_LOW);
		vin_gpio_write(sd, PWDN, CSI_GPIO_LOW);
		vin_gpio_write(sd, POWER_EN, CSI_GPIO_HIGH);
		vin_set_pmu_channel(sd, IOVDD, ON);
		vin_set_pmu_channel(sd, DVDD, ON);
		vin_set_pmu_channel(sd, AVDD, ON);
		usleep_range(10000, 12000);
		vin_gpio_write(sd, RESET, CSI_GPIO_HIGH);
		vin_gpio_write(sd, PWDN, CSI_GPIO_HIGH);
		usleep_range(10000, 12000);
		vin_set_mclk(sd, ON);
		usleep_range(10000, 12000);
		vin_set_mclk_freq(sd, MCLK);
		usleep_range(30000, 32000);
		cci_unlock(sd);
		break;
	case PWR_OFF:
		sensor_dbg("PWR_OFF!\n");
		cci_lock(sd);
		vin_gpio_set_status(sd, PWDN, 1);
		vin_gpio_set_status(sd, RESET, 1);
		vin_gpio_write(sd, RESET, CSI_GPIO_LOW);
		vin_gpio_write(sd, PWDN, CSI_GPIO_LOW);
		vin_set_mclk(sd, OFF);
		vin_set_pmu_channel(sd, AFVDD, OFF);
		vin_set_pmu_channel(sd, AVDD, OFF);
		vin_set_pmu_channel(sd, IOVDD, OFF);
		vin_set_pmu_channel(sd, DVDD, OFF);
		vin_gpio_write(sd, POWER_EN, CSI_GPIO_LOW);
		vin_gpio_set_status(sd, RESET, 0);
		vin_gpio_set_status(sd, PWDN, 0);
		vin_gpio_set_status(sd, POWER_EN, 0);
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
		usleep_range(1000, 1200);
		break;
	case 1:
		vin_gpio_write(sd, RESET, CSI_GPIO_LOW);
		usleep_range(1000, 1200);
		break;
	default:
		return -EINVAL;
	}
	return 0;
}

static int sensor_detect(struct v4l2_subdev *sd)
{
	unsigned int SENSOR_ID = 0;
	data_type rdval;
	int cnt = 0;

	sensor_read(sd, 0x3107, &rdval);
	SENSOR_ID |= (rdval << 8);
	sensor_read(sd, 0x3108, &rdval);
	SENSOR_ID |= (rdval);
	sensor_print("V4L2_IDENT_SENSOR = 0x%x\n", SENSOR_ID);

	while ((SENSOR_ID != V4L2_IDENT_SENSOR) && (cnt < 5)) {
		sensor_read(sd, 0x3107, &rdval);
		SENSOR_ID |= (rdval << 8);
		sensor_read(sd, 0x3108, &rdval);
		SENSOR_ID |= (rdval);
		sensor_print("retry = %d, V4L2_IDENT_SENSOR = %x\n",
			cnt, SENSOR_ID);
		cnt++;
		}
	if (SENSOR_ID != V4L2_IDENT_SENSOR)
		return -ENODEV;

	return 0;
}

static int sensor_init(struct v4l2_subdev *sd, u32 val)
{
	int ret;
	struct sensor_info *info = to_state(sd);

	sensor_dbg("sensor_init\n");

	/*Make sure it is a target sensor */
	ret = sensor_detect(sd);
	if (ret) {
		sensor_err("chip found is not an target chip.\n");
		return ret;
	}

	info->focus_status = 0;
	info->low_speed = 0;
	info->width = 1600;
	info->height = 1200;
	info->hflip = 0;
	info->vflip = 0;
	info->gain = 0;

	info->tpf.numerator = 1;
	info->tpf.denominator = 30;	/* 30fps */

	info->preview_first_flag = 1;

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
		break;
	case VIDIOC_VIN_SENSOR_EXP_GAIN:
		ret = sensor_s_exp_gain(sd, (struct sensor_exp_gain *)arg);
		break;
	case VIDIOC_VIN_SENSOR_CFG_REQ:
		sensor_cfg_req(sd, (struct sensor_config *)arg);
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
	/* 1600*1200 */
	{
	.width = 1600,
	.height = 1200,
	.hoffset = 0,
	.voffset = 0,
	.hts = 1920,
	.vts = 1250,
	.pclk = 72 * 1000 * 1000,
	.mipi_bps = 720 * 1000 * 1000,
	.fps_fixed = 30,
	.bin_factor = 1,
	.intg_min = 1 << 4,
	.intg_max = 1250 << 4,
	.gain_min = 1 << 4,
	.gain_max = 128 << 4,
	.regs = sensor_1600_1200_regs,
	.regs_size = ARRAY_SIZE(sensor_1600_1200_regs),
	.set_size = NULL,
	 },

};

#define N_WIN_SIZES (ARRAY_SIZE(sensor_win_sizes))

static int sensor_reg_init(struct sensor_info *info)
{

	int ret = 0;
	struct v4l2_subdev *sd = &info->sd;
	struct sensor_format_struct *sensor_fmt = info->fmt;
	struct sensor_win_size *wsize = info->current_wins;

	ret = sensor_write_array(sd, sensor_default_regs,
				   ARRAY_SIZE(sensor_default_regs));
	if (ret < 0) {
		sensor_err("write sensor_default_regs error\n");
		return ret;
	}

	sensor_write_array(sd, sensor_fmt->regs, sensor_fmt->regs_size);

	if (wsize->regs)
		sensor_write_array(sd, wsize->regs, wsize->regs_size);

	if (wsize->set_size)
		wsize->set_size(sd);

	info->width = wsize->width;
	info->height = wsize->height;
	info->exp = 0;
	info->gain = 0;
	sc201cs_sensor_vts = wsize->vts;
	sensor_print("s_fmt set width = %d, height = %d\n", wsize->width,
			  wsize->height);

	return 0;
}

static int sensor_s_stream(struct v4l2_subdev *sd, int enable)
{
	struct sensor_info *info = to_state(sd);

	sensor_print("%s on = %d, %d*%d %x\n", __func__, enable,
		  info->current_wins->width,
		  info->current_wins->height, info->fmt->mbus_code);

	if (!enable)
		return 0;
	return sensor_reg_init(info);
}

static int sensor_g_mbus_config(struct v4l2_subdev *sd, unsigned int pad,
				struct v4l2_mbus_config *cfg)
{
	cfg->type = V4L2_MBUS_CSI2_DPHY;
	cfg->flags = 0 | V4L2_MBUS_CSI2_1_LANE | V4L2_MBUS_CSI2_CHANNEL_0;
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

static const struct v4l2_ctrl_config sensor_custom_ctrls[] = {
	{
		.ops = &sensor_ctrl_ops,
		.id = V4L2_CID_FRAME_RATE,
		.name = "frame rate",
		.type = V4L2_CTRL_TYPE_INTEGER,
		.min = 15,
		.max = 30,
		.step = 1,
		.def = 120,
	},
};

static int sensor_init_controls(struct v4l2_subdev *sd, const struct v4l2_ctrl_ops *ops)
{
	struct sensor_info *info = to_state(sd);
	struct v4l2_ctrl_handler *handler = &info->handler;
	struct v4l2_ctrl *ctrl;
	int i;
	int ret = 0;

	v4l2_ctrl_handler_init(handler, 2 + ARRAY_SIZE(sensor_custom_ctrls));

	v4l2_ctrl_new_std(handler, ops, V4L2_CID_GAIN, 1 * 1600,
				  256 * 1600, 1, 1 * 1600);
	ctrl = v4l2_ctrl_new_std(handler, ops, V4L2_CID_EXPOSURE, 0,
				  65536 * 16, 1, 0);
	if (ctrl != NULL)
		ctrl->flags |= V4L2_CTRL_FLAG_VOLATILE;
	for (i = 0; i < ARRAY_SIZE(sensor_custom_ctrls); i++)
		v4l2_ctrl_new_custom(handler, &sensor_custom_ctrls[i], NULL);

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
	sd = &info->sd;
	cci_dev_probe_helper(sd, client, &sensor_ops, &cci_drv);
	sensor_init_controls(sd, &sensor_ctrl_ops);

	mutex_init(&info->lock);
	
#ifdef CONFIG_SAME_I2C
	info->sensor_i2c_addr = I2C_ADDR >> 1;
#endif

	info->fmt = &sensor_formats[0];
	info->fmt_pt = &sensor_formats[0];
	info->win_pt = &sensor_win_sizes[0];
	info->fmt_num = N_FMTS;
	info->win_size_num = N_WIN_SIZES;
	info->sensor_field = V4L2_FIELD_NONE;
	info->af_first_flag = 1;

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
