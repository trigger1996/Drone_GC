package com.example.wraith.drone_gc;

import android.content.Intent;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/*

	Socket收到的东西通过BroadCast出去
	要发送的通信通过直接改UDPServer的类来实现

	后者效率低了一点，这是因为进程同步还是要时间的，而且安卓本身就不是为了高速运行而生的
	但是速度应该凑合着还能用

*/

class UDPServer implements Runnable {

	private boolean socket_should_exit;

	public  String  Tx;

	@Override
	public void run() {
		try{
			socket_should_exit = false;
			DatagramSocket ds = new DatagramSocket(50000);
			byte[] buf = new byte[1024];
			DatagramPacket dp = new DatagramPacket(buf,1024);

			while (!socket_should_exit) {

				ds.receive(dp);
				String data = new String(dp.getData(), dp.getOffset(), dp.getLength());		// 0<->dp.getOffset()

				Log.i("[UDP Received]:", data);

				InetAddress addr = dp.getAddress();
				int port = dp.getPort();
				//byte[] echo = ("From Server:echo.........." + Tx).getBytes();
				byte echo[] = Tx.getBytes();
				DatagramPacket dp2 = new DatagramPacket(echo, echo.length, addr, port);
				ds.send(dp2);

				//将收到的消息发给主界面
				Intent RcvIntent = new Intent();
				RcvIntent.setAction("udpRcvMsg");
				RcvIntent.putExtra("udpRcvMsg", data);
				MainActivity.context.sendBroadcast(RcvIntent);		// Main的onCreate要加个context = this;

		}
		ds.close();
		}catch (Exception e){
			Log.i("[UDP Exception]:", e.getMessage());
		}
	}



}// class UDPServer implements Runnable