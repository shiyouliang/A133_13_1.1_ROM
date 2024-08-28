/*
 * A V4L2 driver for BF2553L Raw cameras.
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
MODULE_DESCRIPTION("A low-level driver for bf2553L sensors");
MODULE_LICENSE("GPL");

#define MCLK              (24*1000*1000)

/*
 * Our nominal (default) frame rate.
 */

#define SENSOR_FRAME_RATE 30

/*
 * The GC0310 i2c address
 */
#define I2C_ADDR 0x7c

#define SENSOR_NAME "bf2553L_mipi"

/* SENSOR FLIP */
#define bf2553L_HFLIP_EN    1
#define bf2553L_VFLIP_EN    0

#define MaxGainIndex (241)

/*
 * The default register settings
 */

static struct regval_list sensor_default_regs[] = {


};
static struct regval_list sensor_2592x1944p30_regs[] = {
	{0xf2, 0x01},//Reset
	{0x02, 0xaf},
	{0x03, 0x60},
	{0x14, 0xff},
	{0x29, 0x29},//0x2a modify for V4
	{0x2a, 0x2c},
	{0x32, 0xfc},
	{0x40, 0x62},//0x52 modify for V4
	{0xe1, 0x08},	
	{0xe6, 0x73},//9b-93 0709
	{0xe9, 0xf0},
	{0xea, 0xab},
	{0x58, 0x20},
	{0x59, 0x20},
	{0x5a, 0x20},
	{0x5b, 0x20},
	{0x6A, 0x06},//intt_H
	{0x6D, 0xea},//intt_L
	{0x6B, 0x4f},//global gain
	{0x6f, 0x10},//digital gain
	//color gain 1X
	{0x66, 0x90},
	{0x67, 0x10},
	{0x68, 0x10},
	{0x69, 0x10},
	//832M MIPI Setting
	{0x70, 0x08},
	{0x71, 0x07},
	{0x72, 0x16},
	{0x73, 0x09},
	{0x74, 0x08},
	{0x75, 0x06},
	{0x76, 0x30},
	{0x77, 0x02},
	{0x78, 0x10},
	{0x79, 0x0a},
	{0x7d, 0x1f}, // {0x7d, 0x1e}
	//5M Resolution
	{0xe2, 0x02},
	{0xe3, 0x76}, // {0xe3, 0x66}
	{0xe4, 0xd5}, // {0xe4, 0xd0}
	{0x0b, 0xa4}, // add
	{0x31, 0x05}, // add
	{0xe5, 0x06},//MIPICLK:832M //0x06 modify for V4
	{0x00, (0x41 | (bf2553L_HFLIP_EN << 3) | (bf2553L_VFLIP_EN << 2))},
	{0xa0, 0x03},
	{0xca, 0xa0},
	{0xcb, 0x70},
	{0xcc, 0x02},
	{0xcd, 0x22},
	{0xce, 0x04},
	{0xcf, 0x9c},
	{0xf3, 0x00}
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
	//sensor_dbg("sensor_get_exposure = %d\n", info->exp);
	return 0;
}

static int bf2553L_sensor_vts;
static int sensor_s_exp(struct v4l2_subdev *sd, unsigned int exp_val)
{
	unsigned char explow, exphigh;
//	unsigned int all_exp, cal_exp_val;
    unsigned int all_exp;
	struct sensor_info *info = to_state(sd);
	all_exp = exp_val >> 4;

	exphigh  = (unsigned char) ((all_exp >> 8) & 0xff);
	explow  = (unsigned char) (all_exp & 0xff);
	sensor_write(sd, 0x6a, exphigh);
	sensor_write(sd, 0x6d, explow);
	sensor_dbg("sensor_set_exp = %d, Done!\n", exp_val);
	info->exp = exp_val;
	return 0;
}

static unsigned int sensorGainMapping[MaxGainIndex][2] ={
 {64   ,15 },
 {68   ,16 },
 {72   ,17 },
 {76   ,18 },
 {80   ,19 },
 {84   ,20 },
 {88   ,21 },
 {92   ,22 },
 {96   ,23 },
 {100  ,24 },
 {104  ,25 },
 {108  ,26 },
 {112  ,27 },
 {116  ,28 },
 {120  ,29 },
 {124  ,30 },
 {128  ,31 },
 {132  ,32 },
 {136  ,33 },
 {140  ,34 },
 {144  ,35 },
 {148  ,36 },
 {152  ,37 },
 {156  ,38 },
 {160  ,39 },
 {164  ,40 },
 {168  ,41 },
 {172  ,42 },
 {176  ,43 },
 {180  ,44 },
 {184  ,45 },
 {188  ,46 },
 {192  ,47 },
 {196  ,48 },
 {200  ,49 },
 {204  ,50 },
 {208  ,51 },
 {212  ,52 },
 {216  ,53 },
 {220  ,54 },
 {224  ,55 },
 {228  ,56 },
 {232  ,57 },
 {236  ,58 },
 {240  ,59 },
 {244  ,60 },
 {248  ,61 },
 {252  ,62 },
 {256  ,63 },
 {260  ,64 },
 {264  ,65 },
 {268  ,66 },
 {272  ,67 },
 {276  ,68 },
 {280  ,69 },
 {284  ,70 },
 {288  ,71 },
 {292  ,72 },
 {296  ,73 },
 {300  ,74 },
 {304  ,75 },
 {308  ,76 },
 {312  ,77 },
 {316  ,78 },
 {320  ,79 },
 {324  ,80 },
 {328  ,81 },
 {332  ,82 },
 {336  ,83 },
 {340  ,84 },
 {344  ,85 },
 {348  ,86 },
 {352  ,87 },
 {356  ,88 },
 {360  ,89 },
 {364  ,90 },
 {368  ,91 },
 {372  ,92 },
 {376  ,93 },
 {380  ,94 },
 {384  ,95 },
 {388  ,96 },
 {392  ,97 },
 {396  ,98 },
 {400  ,99 },
 {404  ,100},
 {408  ,101},
 {412  ,102},
 {416  ,103},
 {420  ,104},
 {424  ,105},
 {428  ,106},
 {432  ,107},
 {436  ,108},
 {440  ,109},
 {444  ,110},
 {448  ,111},
 {452  ,112},
 {456  ,113},
 {460  ,114},
 {464  ,115},
 {468  ,116},
 {472  ,117},
 {476  ,118},
 {480  ,119},
 {484  ,120},
 {488  ,121},
 {492  ,122},
 {496  ,123},
 {500  ,124},
 {504  ,125},
 {508  ,126},
 {512  ,127},
 {516  ,128},
 {520  ,129},
 {524  ,130},
 {528  ,131},
 {532  ,132},
 {536  ,133},
 {540  ,134},
 {544  ,135},
 {548  ,136},
 {552  ,137},
 {556  ,138},
 {560  ,139},
 {564  ,140},
 {568  ,141},
 {572  ,142},
 {576  ,143},
 {580  ,144},
 {584  ,145},
 {588  ,146},
 {592  ,147},
 {596  ,148},
 {600  ,149},
 {604  ,150},
 {608  ,151},
 {612  ,152},
 {616  ,153},
 {620  ,154},
 {624  ,155},
 {628  ,156},
 {632  ,157},
 {636  ,158},
 {640  ,159},
 {644  ,160},
 {648  ,161},
 {652  ,162},
 {656  ,163},
 {660  ,164},
 {664  ,165},
 {668  ,166},
 {672  ,167},
 {676  ,168},
 {680  ,169},
 {684  ,170},
 {688  ,171},
 {692  ,172},
 {696  ,173},
 {700  ,174},
 {704  ,175},
 {708  ,176},
 {712  ,177},
 {716  ,178},
 {720  ,179},
 {724  ,180},
 {728  ,181},
 {732  ,182},
 {736  ,183},
 {740  ,184},
 {744  ,185},
 {748  ,186},
 {752  ,187},
 {756  ,188},
 {760  ,189},
 {764  ,190},
 {768  ,191},
 {772  ,192},
 {776  ,193},
 {780  ,194},
 {784  ,195},
 {788  ,196},
 {792  ,197},
 {796  ,198},
 {800  ,199},
 {804  ,200},
 {808  ,201},
 {812  ,202},
 {816  ,203},
 {820  ,204},
 {824  ,205},
 {828  ,206},
 {832  ,207},
 {836  ,208},
 {840  ,209},
 {844  ,210},
 {848  ,211},
 {852  ,212},
 {856  ,213},
 {860  ,214},
 {864  ,215},
 {868  ,216},
 {872  ,217},
 {876  ,218},
 {880  ,219},
 {884  ,220},
 {888  ,221},
 {892  ,222},
 {896  ,223},
 {900  ,224},
 {904  ,225},
 {908  ,226},
 {912  ,227},
 {916  ,228},
 {920  ,229},
 {924  ,230},
 {928  ,231},
 {932  ,232},
 {936  ,233},
 {940  ,234},
 {944  ,235},
 {948  ,236},
 {952  ,237},
 {956  ,238},
 {960  ,239},
 {964  ,240},
 {968  ,241},
 {972  ,242},
 {976  ,243},
 {980  ,244},
 {984  ,245},
 {988  ,246},
 {992  ,247},
 {996  ,248},
 {1000 ,249},
 {1004 ,250},
 {1008 ,251},
 {1012 ,252},
 {1016 ,253},
 {1020 ,254},
 {1024 ,255},
};

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
	unsigned int All_gain;
	int iI = 0, ret = 0;
	
	All_gain = gain_val*4;
	if (All_gain < 0x40) /* 64 */
		All_gain = 0x40;

  for (iI = 0; iI < (MaxGainIndex-1); iI++) {
  	if((All_gain >= sensorGainMapping[iI][0])&&(All_gain <= sensorGainMapping[iI+1][0]))
  	{
    	if((All_gain-sensorGainMapping[iI][0]) <= (sensorGainMapping[iI+1][0]-All_gain))
    		ret = (int16_t)sensorGainMapping[iI][1];
    	else
    		ret = (int16_t)sensorGainMapping[iI+1][1];
     break;
  	}
  }

	sensor_write(sd, 0x6b,  ret);

	info->gain = gain_val;
	return 0;
}

static int sensor_s_exp_gain(struct v4l2_subdev *sd,
				struct sensor_exp_gain *exp_gain)
{
	int exp_val = 0, gain_val = 0, shutter = 0, Vb = 0;
	struct sensor_info *info = to_state(sd);

	exp_val = exp_gain->exp_val;
	gain_val = exp_gain->gain_val;
	if (gain_val < 16)
		gain_val = 16;

	shutter = exp_val>>4;
	if (shutter > bf2553L_sensor_vts)
		Vb = shutter - bf2553L_sensor_vts;
	else
		Vb = 0;
	
	sensor_write(sd, 0x07, Vb >> 8);
	sensor_write(sd, 0x06, Vb & 0xff);
	sensor_s_exp(sd, exp_val);
	sensor_s_gain(sd, gain_val);
	sensor_print("sensor_s_exp_gain Vb:%d.\n",Vb);
	sensor_dbg("sensor_set_gain exp = %d, gain = %d Done!\n", exp_val, gain_val);
	info->exp = exp_val;
	info->gain = gain_val;
	return 0;
}

static void sensor_s_sw_stby(struct v4l2_subdev *sd, int on_off)
{

}

/*
 * Stuff that knows about the sensor.
 */
static int sensor_power(struct v4l2_subdev *sd, int on)
{
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
		sensor_print("PWR_ON!\n");
		cci_lock(sd);
		vin_gpio_set_status(sd, PWDN, 1);
		vin_gpio_set_status(sd, RESET, 1);
		usleep_range(100, 120);
		vin_gpio_write(sd, PWDN, CSI_GPIO_LOW);
		vin_gpio_write(sd, RESET, CSI_GPIO_LOW);
		vin_set_pmu_channel(sd, IOVDD, ON);
		usleep_range(100, 120);
		vin_set_pmu_channel(sd, DVDD, ON);
		usleep_range(100, 120);
		vin_set_pmu_channel(sd, AVDD, ON);
		vin_set_pmu_channel(sd, AFVDD, ON);
		usleep_range(200, 220);
		vin_set_mclk_freq(sd, MCLK);
		vin_set_mclk(sd, ON);
		usleep_range(100, 120);
		vin_gpio_write(sd, PWDN, CSI_GPIO_HIGH);
		usleep_range(100, 120);
		vin_gpio_write(sd, RESET, CSI_GPIO_HIGH);
		usleep_range(300, 310);
		vin_set_pmu_channel(sd, CAMERAVDD, ON);/*AFVCC ON*/
		cci_unlock(sd);
		break;
	case PWR_OFF:
		sensor_print("PWR_OFF!\n");
		cci_lock(sd);
		usleep_range(100, 120);
		vin_gpio_write(sd, PWDN, CSI_GPIO_HIGH);
		vin_gpio_write(sd, RESET, CSI_GPIO_HIGH);
		usleep_range(100, 120);
		vin_set_mclk(sd, OFF);
		vin_set_pmu_channel(sd, AVDD, OFF);
		vin_set_pmu_channel(sd, DVDD, OFF);
		vin_set_pmu_channel(sd, IOVDD, OFF);
		vin_set_pmu_channel(sd, AFVDD, OFF);
		usleep_range(100, 120);
		vin_gpio_write(sd, PWDN, CSI_GPIO_LOW);
		vin_gpio_write(sd, RESET, CSI_GPIO_LOW);
		vin_gpio_set_status(sd, POWER_EN, 0);
		usleep_range(100, 120);
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
	unsigned int SENSOR_ID = 0;
	data_type val;
	//int cnt = 0;

	sensor_read(sd, 0xfc, &val);
	SENSOR_ID |= (val << 8);
	sensor_read(sd, 0xfd, &val);
	SENSOR_ID |= (val);
	sensor_print("bf2553L detect V4L2_IDENT_SENSOR = 0x%x\n", SENSOR_ID);
	//while ((SENSOR_ID != 0x2553) && (cnt < 5)) {
	//	sensor_read(sd, 0xf0, &val);
	//	SENSOR_ID |= (val << 8);
	//	sensor_read(sd, 0xf1, &val);
	//	SENSOR_ID |= (val);
	//	sensor_print("retry = %d, V4L2_IDENT_SENSOR = %x\n", cnt, SENSOR_ID);
	//	cnt++;
	//}

	if(SENSOR_ID == 0x2553)
		return 0;
	//printk("bf2553L %s error, chip found is not an target chip.",__func__);
	return -ENODEV;

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
	info->exp = 0;

	info->tpf.numerator = 1;
	info->tpf.denominator = 30; /* 30fps */

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
#if bf2553L_VFLIP_EN		
		.mbus_code = MEDIA_BUS_FMT_SGRBG10_1X10,
#else
		.mbus_code = MEDIA_BUS_FMT_SBGGR10_1X10,
#endif
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
	.width      = 2592,
	.height     = 1944,
	.hoffset    = 0,
	.voffset    = 0,
	.hts        = 1444,
	.vts        = 1966,
	.pclk       = 85200000,
	.mipi_bps   = 852000000,
	.fps_fixed  = 30,
	.bin_factor = 1,
	.intg_min   = 1<<4,
	.intg_max   = 2010<<4,
	.gain_min   = 1<<4,
	.gain_max   = 16<<4,
	.regs       = sensor_2592x1944p30_regs,
	.regs_size  = ARRAY_SIZE(sensor_2592x1944p30_regs),
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
//	int ret,index;
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
	sensor_write_array(sd, sensor_fmt->regs, sensor_fmt->regs_size);
	if (wsize->regs)
		sensor_write_array(sd, wsize->regs, wsize->regs_size);
	if (wsize->set_size)
		wsize->set_size(sd);
	info->width = wsize->width;
	info->height = wsize->height;
	bf2553L_sensor_vts = wsize->vts;
	//data_type val;
	#if 0
	//test	
	for(index = 1;index < 47;index++){
		sensor_read(sd, sensor_2592x1944p30_regs[index].addr, &val);
		sensor_print("bf2553L [%x ,0x%x]\n", sensor_2592x1944p30_regs[index].addr,val);
	}
	//test end
	#endif
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
//	.g_mbus_config = sensor_g_mbus_config,
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
		.addr_width = CCI_BITS_8,
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
	int ret = 0;

	ret = cci_dev_init_helper(&sensor_driver);

	return ret;
}

static __exit void exit_sensor(void)
{
	cci_dev_exit_helper(&sensor_driver);
}

module_init(init_sensor);
module_exit(exit_sensor);
