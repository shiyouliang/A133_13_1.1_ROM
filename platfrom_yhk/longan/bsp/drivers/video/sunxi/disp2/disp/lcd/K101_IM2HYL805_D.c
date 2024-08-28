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
{0xFF,3,{0x98,0x81,0x03}},

//GIP_1

{0x01,1,{0x00}},
{0x02,1,{0x00}},
{0x03,1,{0x53}},        
{0x04,1,{0x13}},        
{0x05,1,{0x00}},        
{0x06,1,{0x04}},        
{0x07,1,{0x00}},      
{0x08,1,{0x00}},       
{0x09,1,{0x22}},   
{0x0a,1,{0x22}},       
{0x0b,1,{0x00}},        
{0x0c,1,{0x01}},      
{0x0d,1,{0x00}},        
{0x0e,1,{0x00}},       
{0x0f,1,{0x25}},        //CLK丁禯_25_4.6us
{0x10,1,{0x25}},       //CLK丁禯_25_4.6us
{0x11,1,{0x00}},           
{0x12,1,{0x00}},        
{0x13,1,{0x00}},      
{0x14,1,{0x00}},
{0x15,1,{0x00}},        
{0x16,1,{0x00}},       
{0x17,1,{0x00}},        
{0x18,1,{0x00}},       
{0x19,1,{0x00}},
{0x1a,1,{0x00}},
{0x1b,1,{0x00}},   
{0x1c,1,{0x00}},
{0x1d,1,{0x00}},
{0x1e,1,{0x44}},        
{0x1f,1,{0x80}},        
{0x20,1,{0x02}},        //CLKA_Rise
{0x21,1,{0x03}},        //CLKA_Fall
{0x22,1,{0x00}},        
{0x23,1,{0x00}},        
{0x24,1,{0x00}},
{0x25,1,{0x00}},
{0x26,1,{0x00}},
{0x27,1,{0x00}},
{0x28,1,{0x33}},      
{0x29,1,{0x03}},       
{0x2a,1,{0x00}},  
{0x2b,1,{0x00}},
{0x2c,1,{0x00}},      
{0x2d,1,{0x00}},       
{0x2e,1,{0x00}},            
{0x2f,1,{0x00}},     
{0x30,1,{0x00}},
{0x31,1,{0x00}},
{0x32,1,{0x00}},      
{0x33,1,{0x00}},
{0x34,1,{0x04}},       //GPWR1/2 non overlap time 2.62us
{0x35,1,{0x00}},             
{0x36,1,{0x00}},
{0x37,1,{0x00}},       
{0x38,1,{0x3C}},	//FOR GPWR1/2 cycle 2 s  
{0x39,1,{0x00}},
{0x3a,1,{0x40}}, 
{0x3b,1,{0x40}},
{0x3c,1,{0x00}},
{0x3d,1,{0x00}},
{0x3e,1,{0x00}},
{0x3f,1,{0x00}},
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
{0x55,1,{0xab}},
{0x56,1,{0x01}},
{0x57,1,{0x23}},
{0x58,1,{0x45}},
{0x59,1,{0x67}},
{0x5a,1,{0x89}},
{0x5b,1,{0xab}},
{0x5c,1,{0xcd}},
{0x5d,1,{0xef}},

//GIP_3
{0x5e,1,{0x11}},
{0x5f,1,{0x01}},     //GOUT_L1  FW
{0x60,1,{0x00}},    //GOUT_L2  BW
{0x61,1,{0x15}},    //GOUT_L3  GPWR1
{0x62,1,{0x14}},     //GOUT_L4  GPWR2
{0x63,1,{0x0C}},     //GOUT_L5  CLK1_R
{0x64,1,{0x0D}},    //GOUT_L6  CLK2_R
{0x65,1,{0x0E}},    //GOUT_L7  CLK3_R
{0x66,1,{0x0F}},     //GOUT_L8  CLK4_R
{0x67,1,{0x06}},    //GOUT_L9  STV1_R
{0x68,1,{0x02}},        
{0x69,1,{0x02}},      
{0x6a,1,{0x02}},      
{0x6b,1,{0x02}},       
{0x6c,1,{0x02}},          
{0x6d,1,{0x02}},       
{0x6e,1,{0x08}},     //GOUT_L16  STV2_R   
{0x6f,1,{0x02}},     //GOUT_L17  VGL
{0x70,1,{0x02}},     //GOUT_L18  VGL
{0x71,1,{0x02}},    //GOUT_L19  VGL
{0x72,1,{0x02}},     
{0x73,1,{0x02}},    
{0x74,1,{0x02}},    
  
{0x75,1,{0x01}},       
{0x76,1,{0x00}},        
{0x77,1,{0x15}},     //BW_CGOUT_L[3]    
{0x78,1,{0x14}},     //BW_CGOUT_L[4]    
{0x79,1,{0x0C}},     //BW_CGOUT_L[5]     
{0x7a,1,{0x0D}},     //BW_CGOUT_L[6]     
{0x7b,1,{0x0E}},     //BW_CGOUT_L[7]   
{0x7c,1,{0x0F}},     //BW_CGOUT_L[8]    
{0x7d,1,{0x08}},     //BW_CGOUT_L[9]      
{0x7e,1,{0x02}},     //BW_CGOUT_L[10]
{0x7f,1,{0x02}},     //BW_CGOUT_L[11]    
{0x80,1,{0x02}},     //BW_CGOUT_L[12]   
{0x81,1,{0x02}},     //BW_CGOUT_L[13] 
{0x82,1,{0x02}},     //BW_CGOUT_L[14]      
{0x83,1,{0x02}},     //BW_CGOUT_L[15]   
{0x84,1,{0x06}},     //BW_CGOUT_L[16]      
{0x85,1,{0x02}},     //BW_CGOUT_L[17]
{0x86,1,{0x02}},     //BW_CGOUT_L[18]
{0x87,1,{0x02}},     //BW_CGOUT_L[19]
{0x88,1,{0x02}},     //BW_CGOUT_L[20]   
{0x89,1,{0x02}},     //BW_CGOUT_L[21]   
{0x8A,1,{0x02}},     //BW_CGOUT_L[22]   



//CMD_Page 4
{0xFF,3,{0x98,0x81,0x04}},

{0x6C,1,{0x15}},
{0x6E,1,{0x2A}},           //VGH 15V
{0x6F,1,{0x35}},           // reg vcl + pumping ratio VGH=3x VGL=-2.5x
{0x3A,1,{0x24}},        //A4     //POWER SAVING
{0x8D,1,{0x14}},           //VGL -10V
{0x87,1,{0xBA}},           //ESD
{0x26,1,{0x76}},
{0xB2,1,{0xD1}},
{0x35,1,{0x1F}},      
{0x39,1,{0x00}},
{0xB5,1,{0x27}},           //gamma bias
{0x31,1,{0x75}},
{0x3B,1,{0x98}},  			
{0x30,1,{0x03}},
{0x33,1,{0x14}},
{0x38,1,{0x02}},
{0x7A,1,{0x00}},
      			
//CMD_Page 1
{0xFF,3,{0x98,0x81,0x01}},
{0x22,1,{0x0A}},          //BGR, SS
{0x31,1,{0x00}},          //Column inversion
{0x35,1,{0x07}},
{0x53,1,{0x6E}},          //VCOM1
{0x55,1,{0x40}},          //VCOM2 
{0x50,1,{0x85}},   // 4.3 95          //VREG1OUT 4.5V
{0x51,1,{0x85}},    //4.3 90          //VREG2OUT -4.5V
{0x60,1,{0x1F}},   //  SDT=2.8 
{0x62,1,{0x07}},
{0x63,1,{0x00}},
//============Gamma START=============

{0xA0,1,{0x08}},
{0xA1,1,{0x14}},
{0xA2,1,{0x1E}},
{0xA3,1,{0x12}},
{0xA4,1,{0x13}},
{0xA5,1,{0x24}},
{0xA6,1,{0x18}},
{0xA7,1,{0x1A}},
{0xA8,1,{0x56}},
{0xA9,1,{0x18}},
{0xAA,1,{0x25}},
{0xAB,1,{0x57}},
{0xAC,1,{0x22}},
{0xAD,1,{0x24}},
{0xAE,1,{0x58}},
{0xAF,1,{0x2B}},
{0xB0,1,{0x2E}},
{0xB1,1,{0x4D}},
{0xB2,1,{0x5B}},
{0xB3,1,{0x3F}},



//Neg Register
{0xC0,1,{0x08}},
{0xC1,1,{0x14}},
{0xC2,1,{0x1E}},
{0xC3,1,{0x12}},
{0xC4,1,{0x13}},
{0xC5,1,{0x24}},
{0xC6,1,{0x17}},
{0xC7,1,{0x1A}},
{0xC8,1,{0x56}},
{0xC9,1,{0x18}},
{0xCA,1,{0x25}},
{0xCB,1,{0x56}},
{0xCC,1,{0x22}},
{0xCD,1,{0x24}},
{0xCE,1,{0x58}},
{0xCF,1,{0x2B}},
{0xD0,1,{0x2D}},
{0xD1,1,{0x4E}},
{0xD2,1,{0x5C}},
{0xD3,1,{0x3F}},


//============ Gamma END===========			
			

//CMD_Page 0			
{0xFF,3,{0x98,0x81,0x00}},

{0x11,1,{0x00}},
{REGFLAG_DELAY,120,{}},//120MS


{0x29,1,{0x00}},
{REGFLAG_DELAY,5,{}},

{0x35,1,{0x00}},

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

struct __lcd_panel K101_IM2HYL805_D_panel = {
	/* panel driver name, must mach the name of lcd_drv_name in sys_config.fex */
	.name = "K101_IM2HYL805_D",
	.func = {
		.cfg_panel_info = LCD_cfg_panel_info,
		.cfg_open_flow = LCD_open_flow,
		.cfg_close_flow = LCD_close_flow,
		.lcd_user_defined_func = LCD_user_defined_func,
	},
};
