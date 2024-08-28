/*
 * Allwinner SoCs display driver.
 *
 * Copyright (C) 2016 Allwinner.
 *
 * This file is licensed under the terms of the GNU General Public
 * License version 2.  This program is licensed "as is" without any
 * warranty of any kind, whether express or implied.
 */
#include <linux/io.h>
#include "panels.h"

static void LCD_power_on(u32 sel);
static void LCD_power_off(u32 sel);
static void LCD_bl_open(u32 sel);
static void LCD_bl_close(u32 sel);

static void LCD_panel_init(u32 sel);
static void LCD_panel_exit(u32 sel);

#define panel_reset(val) sunxi_lcd_gpio_set_value(sel, 0, val)

#define IO_BASE	(0x0300B000UL)
#define PB_CTRL_OFFSET	0x28
#define PB_DATA_OFFSET	0x34
static void set_pb8_output(void)
{
	u32 reg_val;	
	reg_val = readl(ioremap(IO_BASE + PB_CTRL_OFFSET, 4));	
	printk("PB ctrl reg_val = 0x%x\n",reg_val);
	reg_val |= (0x1 << 0);
	reg_val &= ~(0x1 << 1);
	reg_val &= ~(0x1 << 2);
	writel(reg_val, ioremap(IO_BASE + PB_CTRL_OFFSET, 4));
	printk("PB ctrl reg_val = 0x%x\n",reg_val);
}

static void set_pb8_data(u32 value)
{
	u32 reg_val;	
	reg_val = readl(ioremap(IO_BASE + PB_DATA_OFFSET, 4));
	printk("PB data reg_val = 0x%x\n",reg_val);
	if(value == 1){
		reg_val |= (0x1 << 8);
		writel(reg_val, ioremap(IO_BASE + PB_DATA_OFFSET, 4));
		printk("PB ctrl reg_val = 0x%x\n",reg_val);
	}else if(value == 0){
		reg_val &= ~(0x1 << 8);
		writel(reg_val, ioremap(IO_BASE + PB_DATA_OFFSET, 4));
		printk("PB ctrl reg_val = 0x%x\n",reg_val);
	}else{
		printk("set pb8 data err!\n");
		return;
	}
}

static void LCD_cfg_panel_info(struct panel_extend_para *info)
{
	u32 i = 0, j = 0;
	u32 items;
	u8 lcd_gamma_tbl[][2] = {
		// {input value, corrected value}
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

	items = sizeof(lcd_gamma_tbl)/2;
	for (i = 0; i < items - 1; i++) {
		u32 num = lcd_gamma_tbl[i+1][0] - lcd_gamma_tbl[i][0];

		for (j = 0; j < num; j++) {
			u32 value = 0;

			value = lcd_gamma_tbl[i][1] + ((lcd_gamma_tbl[i+1][1] - lcd_gamma_tbl[i][1]) * j)/num;
			info->lcd_gamma_tbl[lcd_gamma_tbl[i][0] + j] = (value<<16) + (value<<8) + value;
		}
	}
	info->lcd_gamma_tbl[255] = (lcd_gamma_tbl[items-1][1]<<16) + (lcd_gamma_tbl[items-1][1]<<8) + lcd_gamma_tbl[items-1][1];

	memcpy(info->lcd_cmap_tbl, lcd_cmap_tbl, sizeof(lcd_cmap_tbl));

}

static s32 LCD_open_flow(u32 sel)
{
	LCD_OPEN_FUNC(sel, LCD_power_on, 100);           //open lcd power, and delay 50ms
	LCD_OPEN_FUNC(sel, LCD_panel_init, 1);         //open lcd power, than delay 200ms
	LCD_OPEN_FUNC(sel, sunxi_lcd_tcon_enable, 5);   //open lcd controller, and delay 100ms
	LCD_OPEN_FUNC(sel, LCD_bl_open, 0);              //open lcd backlight, and delay 0ms

	return 0;
}

static s32 LCD_close_flow(u32 sel)
{
	LCD_CLOSE_FUNC(sel, LCD_bl_close, 0);          //close lcd backlight, and delay 0ms
	LCD_CLOSE_FUNC(sel, sunxi_lcd_tcon_disable, 1); //close lcd controller, and delay 0ms
	LCD_CLOSE_FUNC(sel, LCD_panel_exit,	10);         //open lcd power, than delay 200ms
	LCD_CLOSE_FUNC(sel, LCD_power_off, 0);         //close lcd power, and delay 500ms

	return 0;
}

static void LCD_power_on(u32 sel)
{
	set_pb8_output();
	panel_reset(0);
	sunxi_lcd_power_enable(sel, 0);//config lcd_power pin to open lcd power
	sunxi_lcd_delay_ms(5);
	sunxi_lcd_power_enable(sel, 1); //config lcd_power pin to open lcd power1
	sunxi_lcd_delay_ms(5);
	sunxi_lcd_power_enable(sel, 2);//config lcd_power pin to open lcd power2
	sunxi_lcd_delay_ms(5);

	sunxi_lcd_delay_ms(10);
	panel_reset(1);
	sunxi_lcd_delay_ms(10);
	panel_reset(0);
	sunxi_lcd_delay_ms(10);
	panel_reset(1);
	sunxi_lcd_delay_ms(120);

	sunxi_lcd_pin_cfg(sel, 1);
}

static void LCD_power_off(u32 sel)
{
	sunxi_lcd_pin_cfg(sel, 0);
	sunxi_lcd_delay_ms(20);
	panel_reset(0);
	sunxi_lcd_delay_ms(5);
	sunxi_lcd_power_disable(sel, 2); //config lcd_power pin to close lcd power2
	sunxi_lcd_delay_ms(5);
	sunxi_lcd_power_disable(sel, 1); //config lcd_power pin to close lcd power1
	sunxi_lcd_delay_ms(5);
	sunxi_lcd_power_disable(sel, 0); //config lcd_power pin to close lcd power
}

static void LCD_bl_open(u32 sel)
{
	sunxi_lcd_pwm_enable(sel);
	sunxi_lcd_delay_ms(50);
	set_pb8_data(1);
	sunxi_lcd_backlight_enable(sel);//config lcd_bl_en pin to open lcd backlight
}

static void LCD_bl_close(u32 sel)
{
	set_pb8_data(0);
	sunxi_lcd_backlight_disable(sel);//config lcd_bl_en pin to close lcd backlight
	sunxi_lcd_delay_ms(20);
	sunxi_lcd_pwm_disable(sel);
}

#define REGFLAG_DELAY             						0XFE
#define REGFLAG_END_OF_TABLE      						0xFD   // END OF REGISTERS MARKER

struct LCM_setting_table {
	u8 cmd;
	u32 count;
	u8 para_list[64];
};

/*add panel initialization below*/

static struct LCM_setting_table lcm_initialization_setting[] = {
{0xFF,5,{0xFF,0x98,0x06,0x04,0x01}},     // Change to Page 1																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x08,1,{0x10}},                 // output SDA																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x21,1,{0x01}},                 // DE = 1 Active																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x30,1,{0x01}},                 // 480 X 854																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x31,1,{0x00}},                 // 2-dot Inversion																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x40,1,{0x16}},                 // DDVDH/DDVDL 																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x41,1,{0x33}},                 // DDVDH/DDVDL CP																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x42,1,{0x03}},                 // VGH/VGL 																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x43,1,{0x09}},                 // VGH CP 																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x44,1,{0x06}},                 // VGL CP																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x50,1,{0x88}},                // VGMP																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x51,1,{0x88}},                 // VGMN																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x52,1,{0x00}},                   //Flicker																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x53,1,{0x3D}},                  //Flicker  41	
//DELAY,10
{0x55,1,{0x49}},                  //Flicker  41																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														
//DELAY,10																																																																																																																																																																																																																																																											
{0x60,1,{0x07}},                 // SDTI																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x61,1,{0x00}},                // CRTI																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x62,1,{0x07}},                 // EQTI																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x63,1,{0x00}},                // PCTI																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
//++++++++++++++++++ Gamma Setting ++++++++++++++++++//																																																																																																																																																																																																																																																											
{0xA0,1,{0x00}},  // Gamma 0 																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA1,1,{0x09}},  // Gamma 4 																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA2,1,{0x11}},  // Gamma 8																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA3,1,{0x0B}},  // Gamma 16																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA4,1,{0x05}},  // Gamma 24																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA5,1,{0x08}},  // Gamma 52																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA6,1,{0x06}},  // Gamma 80																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA7,1,{0x04}},  // Gamma 108																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA8,1,{0x09}},  // Gamma 147																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xA9,1,{0x0C}},  // Gamma 175																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xAA,1,{0x15}},  // Gamma 203																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xAB,1,{0x08}},  // Gamma 231																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xAC,1,{0x0F}},  // Gamma 239																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xAD,1,{0x12}},  // Gamma 247																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xAE,1,{0x09}},  // Gamma 251																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xAF,1,{0x00}},  // Gamma 255																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
///==============Nagitive																																																																																																																																																																																																																																																											
{0xC0,1,{0x00}},  // Gamma 0 																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC1,1,{0x09}},  // Gamma 4																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC2,1,{0x10}},  // Gamma 8																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC3,1,{0x0C}},  // Gamma 16																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC4,1,{0x05}},  // Gamma 24																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC5,1,{0x08}},  // Gamma 52																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC6,1,{0x06}},  // Gamma 80																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC7,1,{0x04}},  // Gamma 108																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC8,1,{0x08}},  // Gamma 147																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xC9,1,{0x0C}},  // Gamma 175																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xCA,1,{0x14}},  // Gamma 203																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xCB,1,{0x08}},  // Gamma 231																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xCC,1,{0x0F}},  // Gamma 239																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xCD,1,{0x11}},  // Gamma 247																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xCE,1,{0x09}},  // Gamma 251																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0xCF,1,{0x00}},  // Gamma 255																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
																																																																																																																																																																																																																																																											
//+++++++++++++++++++++++++++++++++++++++++++++++++++//																																																																																																																																																																																																																																																											
																																																																																																																																																																																																																																																											
//****************************************************************************//																																																																																																																																																																																																																																																											
//****************************** Page 6 Command ******************************//																																																																																																																																																																																																																																																											
//****************************************************************************//																																																																																																																																																																																																																																																											
{0xFF,5,{0xFF,0x98,0x06,0x04,0x06}},     // Change to Page 6																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x00,1,{0x20}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x01,1,{0x0A}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x02,1,{0x00}},    																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x03,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x04,1,{0x01}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x05,1,{0x01}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x06,1,{0x98}},    																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x07,1,{0x06}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x08,1,{0x01}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x09,1,{0x80}},    																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x0A,1,{0x00}},    																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x0B,1,{0x00}},    																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x0C,1,{0x01}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x0D,1,{0x01}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x0E,1,{0x05}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x0F,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x10,1,{0xF0}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x11,1,{0xF4}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x12,1,{0x01}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x13,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x14,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x15,1,{0xC0}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x16,1,{0x08}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x17,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x18,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x19,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x1A,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x1B,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x1C,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x1D,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
																																																																																																																																																																																																																																																											
{0x20,1,{0x01}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x21,1,{0x23}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x22,1,{0x45}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x23,1,{0x67}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x24,1,{0x01}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x25,1,{0x23}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x26,1,{0x45}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x27,1,{0x67}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
																																																																																																																																																																																																																																																											
{0x30,1,{0x11}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x31,1,{0x11}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x32,1,{0x00}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x33,1,{0xEE}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x34,1,{0xFF}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x35,1,{0xBB}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x36,1,{0xAA}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x37,1,{0xDD}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x38,1,{0xCC}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x39,1,{0x66}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x3A,1,{0x77}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x3B,1,{0x22}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x3C,1,{0x22}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x3D,1,{0x22}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x3E,1,{0x22}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x3F,1,{0x22}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x40,1,{0x22}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
																																																																																																																																																																																																																																																											
{0xFF,5,{0xFF,0x98,0x06,0x04,0x07}},     // Change to Page 7																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x17,1,{0x22}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x02,1,{0x77}},																																																																																																																																																																																																																																																											
//DELAY,10																																																																																																																																																																																																																																																											
{0x26,1,{0xB2}},																																																																																																																																																																																																																																																											
//****************************************************************************//																																																																																																																																																																																																																																																											
																																																																																																																																																																																																																																																											
{0xFF,5,{0xFF,0x98,0x06,0x04,0x00}},     // Change to Page 0																																																																																																																																																																																																																																																											
//DELAY,10
{0x3A,1,{0x55}}, 
{0x36,1,{0x00}},

{0x11,1,{0x00}},
{REGFLAG_DELAY,120,{}},//120MS


{0x29,1,{0x00}},
{REGFLAG_DELAY,5,{}},

//{0x35,1,{0x00}},

//{0xE0,1,{0x00}},
	{REGFLAG_END_OF_TABLE,REGFLAG_END_OF_TABLE,{}}
};

static void LCD_panel_init(u32 sel)
{
	__u32 i;
	char model_name[25];
	disp_sys_script_get_item("lcd0", "lcd_model_name",  (int *)model_name, 25);
	sunxi_lcd_dsi_clk_enable(sel);
	sunxi_lcd_delay_ms(20);
	//sunxi_lcd_dsi_dcs_write_0para(sel, DSI_DCS_SOFT_RESET);
	//sunxi_lcd_delay_ms(10);

	for (i = 0; ; i++) {
			if (lcm_initialization_setting[i].count == REGFLAG_END_OF_TABLE)
				break;
			else if (lcm_initialization_setting[i].count == REGFLAG_DELAY)
				sunxi_lcd_delay_ms(lcm_initialization_setting[i].para_list[0]);
#ifdef SUPPORT_DSI
			else
				dsi_dcs_wr(sel, lcm_initialization_setting[i].cmd, lcm_initialization_setting[i].para_list, lcm_initialization_setting[i].count);
#endif
		//break;
	}

	return;
}

static void LCD_panel_exit(u32 sel)
{
	sunxi_lcd_dsi_dcs_write_0para(sel, DSI_DCS_SET_DISPLAY_OFF);
	sunxi_lcd_delay_ms(20);
	sunxi_lcd_dsi_dcs_write_0para(sel, DSI_DCS_ENTER_SLEEP_MODE);
	sunxi_lcd_delay_ms(80);

	return ;
}

//sel: 0:lcd0; 1:lcd1
static s32 LCD_user_defined_func(u32 sel, u32 para1, u32 para2, u32 para3)
{
	return 0;
}

struct __lcd_panel HSD050B8W8_IPS_panel = {
	/* panel driver name, must mach the name of lcd_drv_name in sys_config.fex */
	.name = "HSD050B8W8_IPS",
	.func = {
		.cfg_panel_info = LCD_cfg_panel_info,
		.cfg_open_flow = LCD_open_flow,
		.cfg_close_flow = LCD_close_flow,
		.lcd_user_defined_func = LCD_user_defined_func,
	},
};
