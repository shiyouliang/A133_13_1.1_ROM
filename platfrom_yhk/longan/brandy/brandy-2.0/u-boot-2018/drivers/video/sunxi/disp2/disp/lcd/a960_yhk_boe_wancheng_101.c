/* drivers/video/sunxi/disp2/disp/lcd/a960_yhk_boe800x1280_101.c
 *
 * Copyright (c) 2017 Allwinnertech Co., Ltd.
 * Author: zhengxiaobin <zhengxiaobin@allwinnertech.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
*/
#include "he0801a068.h"

static void lcd_power_on(u32 sel);
static void lcd_power_off(u32 sel);
static void lcd_bl_open(u32 sel);
static void lcd_bl_close(u32 sel);

static void lcd_panel_init1(u32 sel);
static void lcd_panel_init2(u32 sel);
static void lcd_panel_exit(u32 sel);

#define panel_reset(sel, val) sunxi_lcd_gpio_set_value(sel, 0, val)

static void lcd_cfg_panel_info(panel_extend_para *info)
{
	u32 i = 0, j = 0;
	u32 items;
	u8 lcd_gamma_tbl[][2] = {
		{0, 0},
		{15, 15},
		{30, 30},
		{45, 45},
		{60, 60},
		{75, 75},
		{90, 90},
		{105, 105},
		{120, 120},
		{135, 135},
		{150, 150},
		{165, 165},
		{180, 180},
		{195, 195},
		{210, 210},
		{225, 225},
		{240, 240},
		{255, 255},
	};

	u32 lcd_cmap_tbl[2][3][4] = {
	{
		{LCD_CMAP_G0, LCD_CMAP_B1, LCD_CMAP_G2, LCD_CMAP_B3},
		{LCD_CMAP_B0, LCD_CMAP_R1, LCD_CMAP_B2, LCD_CMAP_R3},
		{LCD_CMAP_R0, LCD_CMAP_G1, LCD_CMAP_R2, LCD_CMAP_G3},
		},
		{
		{LCD_CMAP_B3, LCD_CMAP_G2, LCD_CMAP_B1, LCD_CMAP_G0},
		{LCD_CMAP_R3, LCD_CMAP_B2, LCD_CMAP_R1, LCD_CMAP_B0},
		{LCD_CMAP_G3, LCD_CMAP_R2, LCD_CMAP_G1, LCD_CMAP_R0},
		},
	};

	items = sizeof(lcd_gamma_tbl) / 2;
	for (i = 0; i < items - 1; i++) {
		u32 num = lcd_gamma_tbl[i+1][0] - lcd_gamma_tbl[i][0];

		for (j = 0; j < num; j++) {
			u32 value = 0;

			value = lcd_gamma_tbl[i][1] +
				((lcd_gamma_tbl[i+1][1] - lcd_gamma_tbl[i][1])
				* j) / num;
			info->lcd_gamma_tbl[lcd_gamma_tbl[i][0] + j] =
							(value<<16)
							+ (value<<8) + value;
		}
	}
	info->lcd_gamma_tbl[255] = (lcd_gamma_tbl[items-1][1]<<16) +
					(lcd_gamma_tbl[items-1][1]<<8)
					+ lcd_gamma_tbl[items-1][1];

	memcpy(info->lcd_cmap_tbl, lcd_cmap_tbl, sizeof(lcd_cmap_tbl));

}

static s32 lcd_open_flow(u32 sel)
{
	LCD_OPEN_FUNC(sel, lcd_power_on, 120);
	LCD_OPEN_FUNC(sel, lcd_panel_init1, 120);
	LCD_OPEN_FUNC(sel, lcd_panel_init2, 1);
	LCD_OPEN_FUNC(sel, sunxi_lcd_tcon_enable, 5);
	LCD_OPEN_FUNC(sel, lcd_bl_open, 0);

	return 0;
}

static s32 lcd_close_flow(u32 sel)
{
	LCD_CLOSE_FUNC(sel, lcd_bl_close, 0);
	LCD_CLOSE_FUNC(sel, lcd_panel_exit, 1);
	LCD_CLOSE_FUNC(sel, sunxi_lcd_tcon_disable, 10);
	LCD_CLOSE_FUNC(sel, lcd_power_off, 0);

	return 0;
}

static void lcd_power_on(u32 sel)
{
	sunxi_lcd_pin_cfg(sel, 1);

	panel_reset(sel, 0);
	sunxi_lcd_power_enable(sel, 0);
	sunxi_lcd_power_enable(sel, 1);
	sunxi_lcd_power_enable(sel, 2);
	sunxi_lcd_delay_ms(5);
	panel_reset(sel, 1);
	sunxi_lcd_delay_ms(10);
	panel_reset(sel, 0);
	sunxi_lcd_delay_ms(10);
	panel_reset(sel, 1);
	sunxi_lcd_delay_ms(120);
}

static void lcd_power_off(u32 sel)
{
	panel_reset(sel, 0);
	sunxi_lcd_delay_ms(1);
	sunxi_lcd_power_disable(sel, 2);
	sunxi_lcd_delay_ms(1);
	sunxi_lcd_power_disable(sel, 1);
	sunxi_lcd_delay_ms(1);
	sunxi_lcd_power_disable(sel, 0);
	sunxi_lcd_pin_cfg(sel, 0);
}

static void lcd_bl_open(u32 sel)
{
	sunxi_lcd_pwm_enable(sel);
	sunxi_lcd_backlight_enable(sel);
}

static void lcd_bl_close(u32 sel)
{
	sunxi_lcd_backlight_disable(sel);
	sunxi_lcd_pwm_disable(sel);
}

static void lcd_panel_init1(u32 sel)
{
	sunxi_lcd_dsi_clk_enable(sel);
	/*JD9365 initial code */
	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe0,0x00);

	//sunxi_lcd_dsi_dcs_write_1para(sel, 0x51,0x80);			//PWM
	//sunxi_lcd_dsi_dcs_write_1para(sel, 0x53,0x2C);

	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe1,0x93);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe2,0x65);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe3,0xf8);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x80,0x03);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe0,0x01);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x00,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x01,0x48);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x0c,0x74);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x17,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x18,0xaf);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x19,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1a,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1b,0xaf);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1c,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x35,0x26);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x37,0x09);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x38,0x04);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x39,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3a,0x01);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3c,0x78);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3d,0xff);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3e,0xff);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3f,0x7f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x40,0x06);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x41,0xa0);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x42,0x81);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x43,0x14);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x44,0x23);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x45,0x28);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x55,0x02);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x57,0x69);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x59,0x0a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5a,0x2a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5b,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5d,0x7f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5e,0x6b);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5f,0x5c);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x60,0x4f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x61,0x4d);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x62,0x3f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x63,0x42);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x64,0x2b);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x65,0x44);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x66,0x43);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x67,0x43);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x68,0x63);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x69,0x52);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6a,0x5a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6b,0x4f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6c,0x4e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6d,0x20);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6e,0x0f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6f,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x70,0x7f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x71,0x6b);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x72,0x5c);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x73,0x4f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x74,0x4d);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x75,0x3f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x76,0x42);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x77,0x2b);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x78,0x44);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x79,0x43);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7a,0x43);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7b,0x63);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7c,0x52);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7d,0x5a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7e,0x4f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7f,0x4e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x80,0x20);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x81,0x0f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x82,0x00);

	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe0,0x02);

	sunxi_lcd_dsi_dcs_write_1para(sel, 0x00,0x02);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x01,0x02);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x02,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x03,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x04,0x1e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x05,0x1e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x06,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x07,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x08,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x09,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x0a,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x0b,0x37);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x0c,0x37);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x0d,0x47);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x0e,0x47);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x0f,0x45);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x10,0x45);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x11,0x4b);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x12,0x4b);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x13,0x49);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x14,0x49);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x15,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x16,0x01);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x17,0x01);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x18,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x19,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1a,0x1e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1b,0x1e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1c,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1d,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1e,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x1f,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x20,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x21,0x37);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x22,0x37);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x23,0x46);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x24,0x46);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x25,0x44);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x26,0x44);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x27,0x4a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x28,0x4a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x29,0x48);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x2a,0x48);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x2b,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x2c,0x01);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x2d,0x01);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x2e,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x2f,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x30,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x31,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x32,0x1e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x33,0x1e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x34,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x35,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x36,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x37,0x37);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x38,0x37);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x39,0x08);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3a,0x08);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3b,0x0a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3c,0x0a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3d,0x04);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3e,0x04);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x3f,0x06);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x40,0x06);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x41,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x42,0x02);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x43,0x02);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x44,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x45,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x46,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x47,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x48,0x1e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x49,0x1e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x4a,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x4b,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x4c,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x4d,0x37);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x4e,0x37);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x4f,0x09);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x50,0x09);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x51,0x0b);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x52,0x0b);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x53,0x05);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x54,0x05);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x55,0x07);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x56,0x07);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x57,0x1f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x58,0x40);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5b,0x30);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5c,0x16);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5d,0x34);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5e,0x05);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x5f,0x02);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x63,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x64,0x6a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x67,0x73);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x68,0x1d);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x69,0x08);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6a,0x6a);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6b,0x08);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6c,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6d,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6e,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x6f,0x88);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x75,0xff);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x77,0xdd);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x78,0x3f);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x79,0x15);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7a,0x17);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7d,0x14);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x7e,0x82);

	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe0,0x03);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x9A,0xFF);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x9B,0xFF);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0xAF,0x01);

	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe0,0x04);

	sunxi_lcd_dsi_dcs_write_1para(sel, 0x00,0x0e);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x02,0xb3);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x09,0x61);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x0e,0x48);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0x37,0x58);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe0,0x00);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe6,0x02);
	sunxi_lcd_dsi_dcs_write_1para(sel, 0xe7,0x0c);
	//sunxi_lcd_dsi_dcs_write_1para(sel, 0x96,0x0A);
	//sunxi_lcd_dsi_dcs_write_1para(sel, 0x96,0x12);
	
/*SLP OUT */
sunxi_lcd_dsi_dcs_write_0para(sel, 0x11); /* SLPOUT */
}

static void lcd_panel_init2(u32 sel)
{
	/*DISP ON */
	sunxi_lcd_dsi_dcs_write_0para(sel, 0x29); /* DSPON */
	sunxi_lcd_delay_ms(5);

	/*--- TE----// */
	// sunxi_lcd_dsi_dcs_write_1para(sel, 0x35, 0x00);
	// sunxi_lcd_dsi_dcs_write_1para(sel, 0xE0,0x00);
}


static void lcd_panel_exit(u32 sel)
{
	sunxi_lcd_dsi_dcs_write_0para(sel, 0x10);
	sunxi_lcd_delay_ms(1);
	sunxi_lcd_dsi_dcs_write_0para(sel, 0x28);
	sunxi_lcd_delay_ms(1);
}

/*sel: 0:lcd0; 1:lcd1*/
static s32 lcd_user_defined_func(u32 sel, u32 para1, u32 para2, u32 para3)
{
	return 0;
}

__lcd_panel_t a960_yhk_boe_wancheng_101_panel = {
	/* panel driver name, must mach the name of
	 * lcd_drv_name in sys_config.fex
	 */
	.name = "a960_yhk_boe_wancheng_101",
	.func = {
		.cfg_panel_info = lcd_cfg_panel_info,
			.cfg_open_flow = lcd_open_flow,
			.cfg_close_flow = lcd_close_flow,
			.lcd_user_defined_func = lcd_user_defined_func,
	},
};
