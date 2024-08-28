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

#define REGFLAG_DELAY						0XFE
#define REGFLAG_END_OF_TABLE      						0xFD   // END OF REGISTERS MARKER

struct LCM_setting_table {
	u8 cmd;
	u32 count;
	u8 para_list[64];
};

/* add panel initialization below */
static struct LCM_setting_table lcm_initialization_setting[] = {
{0xFF,3,{0x98,0x81,0x03}},
//GIP_1
{0x01,1,{0x00}},           
{0x02,1,{0x00}},           
{0x03,1,{0x73}},           
{0x04,1,{0x00}},           
{0x05,1,{0x00}},           
{0x06,1,{0x08}},           
{0x07,1,{0x00}},           
{0x08,1,{0x00}},           
{0x09,1,{0x00}},           
{0x0A,1,{0x01}},           
{0x0B,1,{0x01}},           
{0x0C,1,{0x00}},        // STV  DUTY     
{0x0D,1,{0x01}},       //  STV  DUTY         
{0x0E,1,{0x01}},           
{0x0F,1,{0x00}},           
{0x10,1,{0x00}},           
{0x11,1,{0x00}},           
{0x12,1,{0x00}},           
{0x13,1,{0x1F}}, /////////////       // CLK DUTY        
{0x14,1,{0x1F}}, /////////////     //  CLK DUTY
{0x15,1,{0x00}},           
{0x16,1,{0x00}},           
{0x17,1,{0x00}},           
{0x18,1,{0x00}},           
{0x19,1,{0x00}},           
{0x1A,1,{0x00}},           
{0x1B,1,{0x00}},           
{0x1C,1,{0x00}},           
{0x1D,1,{0x00}},           
{0x1E,1,{0x40}},           
{0x1F,1,{0xC0}},           
{0x20,1,{0x06}},           
{0x21,1,{0x01}},           
{0x22,1,{0x06}},           
{0x23,1,{0x01}},           
{0x24,1,{0x88}},           
{0x25,1,{0x88}},           
{0x26,1,{0x00}},           
{0x27,1,{0x00}},           
{0x28,1,{0x3B}},           
{0x29,1,{0x03}},           
{0x2A,1,{0x00}},           
{0x2B,1,{0x00}},           
{0x2C,1,{0x00}},           
{0x2D,1,{0x00}},           
{0x2E,1,{0x00}},           
{0x2F,1,{0x00}},           
{0x30,1,{0x00}},           
{0x31,1,{0x00}},           
{0x32,1,{0x00}},           
{0x33,1,{0x00}},           
{0x34,1,{0x00}},  // GPWR1/2 non overlap time 2.62us           
{0x35,1,{0x00}},           
{0x36,1,{0x00}},           
{0x37,1,{0x00}},           
{0x38,1,{0x00}},           
{0x39,1,{0x00}},           
{0x3A,1,{0x00}},           
{0x3B,1,{0x00}},           
{0x3C,1,{0x00}},           
{0x3D,1,{0x00}},           
{0x3E,1,{0x00}},           
{0x3F,1,{0x00}},           
{0x40,1,{0x00}},           
{0x41,1,{0x00}},           
{0x42,1,{0x00}},           
{0x43,1,{0x00}},           
{0x44,1,{0x00}},

//GIP_2           
{0x50,1,{0x01}},           
{0x51,1,{0x23}},           
{0x52,1,{0x45}},           
{0x53,1,{0x67}},           
{0x54,1,{0x89}},           
{0x55,1,{0xAB}},           
{0x56,1,{0x01}},           
{0x57,1,{0x23}},           
{0x58,1,{0x45}},           
{0x59,1,{0x67}},           
{0x5A,1,{0x89}},           
{0x5B,1,{0xAB}},           
{0x5C,1,{0xCD}},           
{0x5D,1,{0xEF}},     

//GIP_3  
{0x5E,1,{0x00}},           
{0x5F,1,{0x01}},           
{0x60,1,{0x01}},           
{0x61,1,{0x06}},           
{0x62,1,{0x06}},           
{0x63,1,{0x07}},           
{0x64,1,{0x07}},           
{0x65,1,{0x00}},           
{0x66,1,{0x00}},           
{0x67,1,{0x02}},           
{0x68,1,{0x02}},           
{0x69,1,{0x05}},           
{0x6A,1,{0x05}},           
{0x6B,1,{0x02}},           
{0x6C,1,{0x0D}},           
{0x6D,1,{0x0D}},           
{0x6E,1,{0x0C}},           
{0x6F,1,{0x0C}},/////////100           
{0x70,1,{0x0F}},           
{0x71,1,{0x0F}},           
{0x72,1,{0x0E}},           
{0x73,1,{0x0E}},           
{0x74,1,{0x02}},           
{0x75,1,{0x01}},           
{0x76,1,{0x01}},           
{0x77,1,{0x06}},           
{0x78,1,{0x06}},           
{0x79,1,{0x07}},           
{0x7A,1,{0x07}},           
{0x7B,1,{0x00}},           
{0x7C,1,{0x00}},           
{0x7D,1,{0x02}},           
{0x7E,1,{0x02}},           
{0x7F,1,{0x05}},           
{0x80,1,{0x05}},           
{0x81,1,{0x02}},           
{0x82,1,{0x0D}},           
{0x83,1,{0x0D}},           
{0x84,1,{0x0C}},           
{0x85,1,{0x0C}},           
{0x86,1,{0x0F}},           
{0x87,1,{0x0F}},           
{0x88,1,{0x0E}},           
{0x89,1,{0x0E}},           
{0x8A,1,{0x02}},//////////////////127

//Page 4 command;           
{0xFF,3,{0x98,0x81,0x04}}, 
//DCS_Short_Write_1P(0x00,0x00);//此行打开是3LANE 屏蔽默认4LANE           
 
//{0x3B,1,{0xC0}}, ////////////    // ILI4003D sel
{0x6C,1,{0x15}},        //Set VCORE voltage =1.5V           
{0x6E,1,{0x2A}},  ////////////      //di_pwr_reg=0 for power mode 2A //VGH clamp 18V           
{0x6F,1,{0x33}},    //45 //pumping ratio VGH=5x VGL=-3x                     
{0x8D,1,{0x1B}},        //VGL clamp -10V           
{0x87,1,{0xBA}},        //ESD  
{0x3A,1,{0x24}},        //POWER SAVING                     
{0x26,1,{0x76}},           
{0xB2,1,{0xD1}},
           
//{0x2F,1,{0x01 // 进自测模式  

 // Page 1 command              
{0xFF,3,{0x98,0x81,0x01}},        
{0x22,1,{0x0A}},        //BGR, SS           
{0x31,1,{0x00}},        //Zigzag type3 inversion           
//{0x40,1,{0x53}}, ///////////       // ILI4003D sel          
{0x43,1,{0x66}},                 
{0x53,1,{0x40}}, //42                   
{0x50,1,{0x87}},                    
{0x51,1,{0x82}},                     
{0x60,1,{0x15}},  
{0x61,1,{0x01}}, 
{0x62,1,{0x0C}}, 
{0x63,1,{0x00}},//////////////////147          


// Page 0 command           
{0xFF,3,{0x98,0x81,0x00}}, 
{0x35,1,{0x00}},  // TE On
{0x11,0,{0x00}},  // Sleep Out          
{REGFLAG_DELAY,REGFLAG_DELAY,{120}},
{0x29,0,{0x00}}, //  Display On 
{REGFLAG_DELAY,REGFLAG_DELAY,{20}},
	{REGFLAG_END_OF_TABLE,REGFLAG_END_OF_TABLE,{}}
};

static void LCD_panel_init(u32 sel)
{
	__u32 i;
	sunxi_lcd_dsi_clk_enable(sel);
	sunxi_lcd_delay_ms(20);
	pr_err("%s a863_D3_ER88577_sddy kernel.\n", __func__);

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

struct __lcd_panel a863_53_9881_rongfengyuan_panel = {
	/* panel driver name, must mach the name of lcd_drv_name in sys_config.fex */
	.name = "a863_53_9881_rongfengyuan",
	.func = {
		.cfg_panel_info = LCD_cfg_panel_info,
		.cfg_open_flow = LCD_open_flow,
		.cfg_close_flow = LCD_close_flow,
		.lcd_user_defined_func = LCD_user_defined_func,
	},
};
