package com.example.wraith.drone_gc;

/*
树莓派-服务器数据链：
	关于校验：这边使用UDP通信，校验问题可以不管，而且民用也不需要加密

	数据帧格式定义：
	所有数据全部集合成一帧发送
	这里包括树莓派-服务器以及服务器-树莓派
	这里两种数据帧

	Server->Rpi
	帧头：									1字节，  0xAF
	帧长：									1字节，  85
	功能编号：								1字节，  0x01
	飞机ID号									1字节，  0x01->0xFF
	时间同步									5字节，  Hour Minute Second mSecond_H mSecond_L
	航点1 航点2 航点3 航点4 航点5 航点6		72字节， 每个航点4Byte Lat、4Byte Lng、4Byte Alt，所有数据 经度*10^7，高度精确到厘米（乘以100发送）
	额外命令									4字节，  0x00无功能，0x01开始编号

	Rpi->Server
	帧头：									1字节，  0xAF
	帧长：									1字节，  28
	功能编号：								1字节，  0x02
	飞机ID号：								1字节，  0x01->0xFF
	当前位置：								12字节， Lat * 4， Lng * 4， Alt * 4，所有数据 经度*10^7，高度精确到毫米（乘以1000发送）
	姿态角：									6字节，  Pitch * 2， Roll * 2， Yaw * 2，所有数据*100发送
	电池数据：								2字节，
	任务事件：								4字节，


*/
public class Datalink {

	public int ID;					// 其实如果要4个同时运行，在调用Datalink类之前，需要先switch (id)一下

	private String Recv;
	public  String Send;

	// 接收
	public  int    Task_Recv;
	public  float  Pitch, Roll, Yaw;
	public  float  Battery_V;
	public  double Lng, Lat;
	public  double Alt;

	// 发送
	private int Task_Send;
	private int Hour, Minute, Second, mSecond;
	private int Lng_Tgt, Lat_Tgt;
	private int Alt_Tgt;

	public Datalink(int id) {

		// 构造函数
		ID = id;

		Recv = null;
		Send = null;

		Task_Recv = 0x00;
		Pitch = Roll = Yaw = 0.0f;
		Battery_V = 0.0f;
		Lng = Lat = 0.0f;
		Alt = 0.0f;

		Task_Send = 0x00;
		Hour = Minute = Second = mSecond = 0;
		Lat_Tgt = Lng_Tgt = 0;
		Alt_Tgt = 0;

	}// Datalink

	public int Recv_Refine(String str_in) {

		int stat = 0;

		//接收提取数据
		Recv = str_in;

		// 检查帧头
		if (Recv.getBytes()[0] != 0xAF) return 2;

		// 检查ID号
		if (Recv.getBytes()[3] != ID)	return 3;

		// 检查功能
		switch (Recv.getBytes()[2]) {

			case 0x02 : {
				stat = Rfn_Rpi_2_Server();
			} break;
		}
		return stat;

	}// Recv_Refine

	public int Send_Preload(byte id) {

		int stat = 0;

		switch (id) {

			case 0x01 : {


			} break;
		}

		return stat;

	}// Send_Preload

	private int Rfn_Rpi_2_Server() {

		byte temp_1, temp_2, temp_3, temp_4;		// Java中byte是8位，char是16位

		// Lat
		temp_1 = Recv.getBytes()[4];
		temp_2 = Recv.getBytes()[5];
		temp_3 = Recv.getBytes()[6];
		temp_4 = Recv.getBytes()[7];
		Lat = (double)((temp_1 << 24) + (temp_2 << 16) + (temp_3 << 8) + temp_4);
		Lat = Lat / 10000000.0f;

		// Lng
		temp_1 = Recv.getBytes()[8];
		temp_2 = Recv.getBytes()[9];
		temp_3 = Recv.getBytes()[10];
		temp_4 = Recv.getBytes()[11];
		Lng = (double)((temp_1 << 24) + (temp_2 << 16) + (temp_3 << 8) + temp_4);
		Lng = Lng / 10000000.0f;

		// Alt
		temp_1 = Recv.getBytes()[12];
		temp_2 = Recv.getBytes()[13];
		temp_3 = Recv.getBytes()[14];
		temp_4 = Recv.getBytes()[15];
		Alt = (float)((temp_1 << 24) + (temp_2 << 16) + (temp_3 << 8) + temp_4);
		Alt = Alt / 1000.0f;

		// Pitch
		temp_1 = Recv.getBytes()[16];
		temp_2 = Recv.getBytes()[17];
		Pitch  = (float)((temp_1 << 8) + temp_2);
		//Pitch  = Pitch / 100.0f;

		// Roll
		temp_1 = Recv.getBytes()[18];
		temp_2 = Recv.getBytes()[19];
		Roll   = (float)((temp_1 << 8) + temp_2);
		Roll   = Roll  / 100.0f;

		// Yaw
		temp_1 = Recv.getBytes()[20];
		temp_2 = Recv.getBytes()[21];
		Yaw    = (float)((temp_1 << 8) + temp_2);
		Yaw    = Yaw   / 100.0f;

		// Battery
		temp_1 = Recv.getBytes()[22];
		temp_2 = Recv.getBytes()[23];
		Battery_V = (float)((temp_1 << 8) + temp_2);
		Battery_V = Battery_V / 100.0f;

		// Task
		temp_1 = Recv.getBytes()[24];
		temp_2 = Recv.getBytes()[25];
		temp_3 = Recv.getBytes()[26];
		temp_4 = Recv.getBytes()[27];
		Task_Recv = (int)((temp_1 << 24) + (temp_2 << 16) + (temp_3 << 8) + temp_4);

		return 0;

	}// Rfn_Rpi_2_Server

	public int Snd_Update_Link(int task,
							   int hour, int min, int sec, int msec,
							   double lng, double lat, double alt) {

		Task_Send = task;

		Hour    = hour;
		Minute  = min;
		Second  = sec;
		mSecond = msec;

		Lng_Tgt = (int)(lng * 10000000.0f);
		Lat_Tgt = (int)(lat * 10000000.0f);
		Alt_Tgt = (int)(alt * 1000.0f);		// 单位：m

		return 0;
	}// Snd_Update_Link

	private int Snd_Link_Preload() {

		byte str_to_send[] = new byte[85];
		int  float_temp, int_temp;

		int i;

		// 帧头
		str_to_send[0] = (byte)0xAF;

		// 帧长
		str_to_send[1] = (byte)85;

		// 功能
		str_to_send[2] = (byte)0x01;

		// 飞机ID
		str_to_send[3] = (byte)ID;

		// Hour
		str_to_send[4] = (byte)Hour;
		// Minute
		str_to_send[5] = (byte)Minute;
		// Second
		str_to_send[6] = (byte)Second;
		// mSecond
		str_to_send[7] = (byte)(mSecond >> 8);
		str_to_send[8] = (byte)mSecond;



		// Lat
		float_temp = (int)(Lat_Tgt * 10000000.0f);
		str_to_send[9]  = (byte)(float_temp >> 24);
		str_to_send[10] = (byte)(float_temp << 8  >> 24);
		str_to_send[11] = (byte)(float_temp << 16 >> 24);
		str_to_send[12] = (byte)(float_temp << 24 >> 24);

		// Lng
		float_temp = (int)(Lng_Tgt * 10000000.0f);
		str_to_send[13] = (byte)(float_temp >> 24);
		str_to_send[14] = (byte)(float_temp << 8  >> 24);
		str_to_send[15] = (byte)(float_temp << 16 >> 24);
		str_to_send[16] = (byte)(float_temp << 24 >> 24);

		// Alt
		float_temp = (int)(Alt_Tgt * 1000.0f);
		str_to_send[17] = (byte)(float_temp >> 24);
		str_to_send[18] = (byte)(float_temp << 8  >> 24);
		str_to_send[19] = (byte)(float_temp << 16 >> 24);
		str_to_send[20] = (byte)(float_temp << 24 >> 24);


		// Task
		int_temp = Task_Send;
		str_to_send[81] = (byte)(int_temp >> 24);
		str_to_send[82] = (byte)(int_temp << 8  >> 24);
		str_to_send[83] = (byte)(int_temp << 16 >> 24);
		str_to_send[84] = (byte)(int_temp << 24 >> 24);

		Send = str_to_send.toString();

		return 0;

	}// Snd_Link_Preload

	public int Snd_Preload_PlainText()
	{
		// ID:<数据> Hour:<数据> Min:<数据> Sec:<数据> mSec:<数据> Lat:<数据> Lng:<数据> Alt:<数据> Tsk:<数据>{两个空格}

		Send = new String("ID:" + ID + " ");

		// Hour
		Send = Send + "Hour:" + Hour    + " ";
		// Min
		Send = Send + "Min:"  + Minute  + " ";
		// Sec
		Send = Send + "Sec:"  + Second  + " ";
		// mSec
		Send = Send + "mSec:" + mSecond + " ";

		// Lat
		Send = Send + "Lat:"  + Lat_Tgt + " ";
		// Lng
		Send = Send + "Lng:"  + Lng_Tgt + " ";
		// Alt
		Send = Send + "Alt:"  + Alt_Tgt + " ";

		// Task
		Send = Send + "Tsk:"  + Task_Send + " ";

		Send = Send + " ";

		return 0;
	}

	public int Rfn_Rpi_2_Server_PlainText(String str_in) {

		String num_temp;
		String remain;					// 剩余的数据，完成一段就截去一段
		int offset = 0, end = 0;		// offset: 每个数据开始的位置  end: 每个数据结束的位置
		int id = 0;						// 本帧的id

		Recv = str_in;

		remain = Recv.substring(end);

		// 先查ID
		offset = remain.indexOf("ID:") + "ID:".length();	//数据开始的位置
		end    = remain.indexOf(" ");						// 空格的位置

		if (end <= offset ||
			offset <= 0 || end <= 0) return 4;
		num_temp = remain.substring(offset, end);
		remain = remain.substring(end + 1);	// 这一段的结束就是下一段的开始，即砍头

		id = Integer.parseInt(num_temp);

		if (id != ID)	return 3;

		// Lat
		offset = remain.indexOf("Lat:") + "Lat:".length();
		end    = remain.indexOf(" ");
		num_temp = remain.substring(offset, end);
		remain = remain.substring(end + 1);

		Lat = (double)Integer.parseInt(num_temp) / 10000000.0f;

		// Lng
		offset = remain.indexOf("Lng:") + "Lng:".length();
		end    = remain.indexOf(" ");
		num_temp = remain.substring(offset, end);
		remain = remain.substring(end + 1);

		Lng = (double)Integer.parseInt(num_temp) / 10000000.0f;


		// Alt
		offset = remain.indexOf("Alt:") + "Alt:".length();
		end    = remain.indexOf(" ");
		num_temp = remain.substring(offset, end);
		remain = remain.substring(end + 1);

		Alt = (double)Integer.parseInt(num_temp) / 1000.0f;


		// Pitch
		offset = remain.indexOf("Pitch:") + "Pitch:".length();
		end    = remain.indexOf(" ");
		num_temp = remain.substring(offset, end);
		remain = remain.substring(end + 1);

		Pitch = (float)Integer.parseInt(num_temp) / 100.0f;


		// Roll
		offset = remain.indexOf("Roll:") + "Roll:".length();
		end    = remain.indexOf(" ");
		num_temp = remain.substring(offset, end);
		remain = remain.substring(end + 1);

		Roll = (float)Integer.parseInt(num_temp) / 100.0f;



		// Yaw
		offset = remain.indexOf("Yaw:") + "Yaw:".length();
		end    = remain.indexOf(" ");
		num_temp = remain.substring(offset, end);
		remain = remain.substring(end + 1);

		Yaw = (float)Integer.parseInt(num_temp) / 100.0f;


		// Bat
		offset = remain.indexOf("Bat:") + "Bat:".length();
		end    = remain.indexOf(" ");
		num_temp = remain.substring(offset, end);
		remain = remain.substring(end + 1);

		Battery_V = (float)Integer.parseInt(num_temp) / 100.0f;

		// Task
		offset = remain.indexOf("Tsk:") + "Tsk:".length();
		end    = remain.indexOf(" ");
		num_temp = remain.substring(offset, end);
		//remain = remain.substring(end + 1);

		Task_Recv = Integer.parseInt(num_temp);


		return 0;

	}//Rfn_Rpi_2_Server_PlainText
}