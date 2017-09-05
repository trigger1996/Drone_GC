package com.example.wraith.drone_gc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.DataTruncation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	public static Context context;

	private UDPServer udpServer = null;
	private Datalink  Link_1 = null, Link_2 = null, Link_3 = null, Link_4 = null;
	private int       ID_to_Send    = 0;		// 选择发送的飞机ID号
	private boolean   Flag_Formation = false;	// 编队飞行标识位

	private TextView StatText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		context = this;

		// UDP初始化
		UDP_Init();

		/// 显示无人机的状态
		StatText = (TextView)findViewById(R.id.test_main);
		StatText.setAlpha(0.55f);		// 1位不透明

		FloatingActionButton set_dest = (FloatingActionButton) findViewById(R.id.set_dest);
		set_dest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "让飞机飞到这里~", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();

				/// 设置飞机目的地的代码
				if (Flag_Formation == false) {

					//单架飞行
					switch (ID_to_Send) {
						case 1 : {

						} break;

						case 2 : {

						} break;

						case 3 : {
							Link_3.Snd_Update_Link(3,
									19, 52, 49, 555,
									-118.1234567f, 23.4567891f, 2.5f);
							Link_3.Snd_Preload_PlainText();
							udpServer.Tx = Link_3.Send;

						} break;

						case 4 : {

						} break;

						default : {

						} break;
					}
				} else {

					// 编队飞行
				}
			}
		});

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);
	}

	// UDP相关____________________________________________________________________________
	private void UDP_Init() {

		//建立线程池
		ExecutorService exec = Executors.newCachedThreadPool();
		udpServer = new UDPServer();
		exec.execute(udpServer);

		Link_1 = new Datalink(0x01);
		Link_2 = new Datalink(0x02);
		Link_3 = new Datalink(0x03);
		Link_4 = new Datalink(0x04);

		Link_1.Snd_Preload_PlainText();		// 这边装Link的初值，所有值都是0
		Link_2.Snd_Preload_PlainText();
		Link_3.Snd_Preload_PlainText();
		Link_4.Snd_Preload_PlainText();

		udpServer.Tx = Link_1.Send;			// 先随便装一个，防止爆炸

		bindReceiver();

		Toast.makeText(MainActivity.this, "开启服务端", Toast.LENGTH_SHORT).show();
	}

	private void bindReceiver(){
		IntentFilter udpRcvIntentFilter = new IntentFilter("udpRcvMsg");
		registerReceiver(broadcastReceiver,udpRcvIntentFilter);
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.hasExtra("udpRcvMsg"))  {
				Message message = new Message();
				message.obj = intent.getStringExtra("udpRcvMsg");
				message.what = 1;
				//Log.i("主界面Broadcast","收到"+message.obj.toString());

				if (message.obj.toString() != null &&
					message.obj.toString().indexOf("Radio Check!") == -1) {

					// 现在有四架飞机，所以第一轮首先查ID
					int id_recv = ID_PreCheck(message.obj.toString());

					StatText.setText(id_recv + " 号机\n");

					switch (id_recv) {
						case 1: {
							Link_1.Rfn_Rpi_2_Server_PlainText(message.obj.toString());

							StatText.append("Pitch:	"	+ Link_1.Pitch		+ "\n"	+
											"Roll:	"	+ Link_1.Roll		+ "\n"	+
											"Yaw:	"	+ Link_1.Yaw		+ "\n"	+
											"高度:	"	+ Link_1.Alt		+ "m"	+ "\n"	+
											"电池:	"	+ Link_1.Battery_V	+ "V");

						} break;

						case 2: {
							Link_2.Rfn_Rpi_2_Server_PlainText(message.obj.toString());

							StatText.append("Pitch:	"	+ Link_2.Pitch		+ "\n"	+
											"Roll:	"	+ Link_2.Roll		+ "\n"	+
											"Yaw:	"	+ Link_2.Yaw		+ "\n"	+
											"高度:	"	+ Link_2.Alt		+ "m"	+ "\n"	+
											"电池:	"	+ Link_2.Battery_V	+ "V");
						} break;

						case 3: {
							Link_3.Rfn_Rpi_2_Server_PlainText(message.obj.toString());

							StatText.append("Pitch:	"	+ Link_3.Pitch		+ "\n"	+
											"Roll:	"	+ Link_3.Roll		+ "\n"	+
											"Yaw:	"	+ Link_3.Yaw		+ "\n"	+
											"高度:	"	+ Link_3.Alt		+ "m"	+ "\n"	+
											"电池:	"	+ Link_3.Battery_V	+ "V");
						} break;

						case 4: {
							Link_4.Rfn_Rpi_2_Server_PlainText(message.obj.toString());

							StatText.append("Pitch:	"	+ Link_4.Pitch		+ "\n"	+
											"Roll:	"	+ Link_4.Roll		+ "\n"	+
											"Yaw:	"	+ Link_4.Yaw		+ "\n"	+
											"高度:	"	+ Link_4.Alt		+ "m"	+ "\n"	+
											"电池:	"	+ Link_4.Battery_V	+ "V");
						} break;

						/*
						default : {
							udpServer.Tx = "Received!";
						} break;
						*/
					}

				}
			}
		}
	};

	// UI控制，监听以及回调_________________________________________________________________
	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_map_sw) {
			return true;
		} else if (id == R.id.action_formation) {

			// 选择编队飞行
			Flag_Formation = true;
			ID_to_Send = 0;

			Toast.makeText(MainActivity.this, "切换至编队飞行", Toast.LENGTH_SHORT).show();

			return true;
		} else if (id == R.id.action_settings) {
			return true;
		}


		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		// 选择发送的飞机的ID号
		if (id == R.id.set_drone_1) {

			ID_to_Send = 1;
			Toast.makeText(MainActivity.this, "选择无人机1", Toast.LENGTH_SHORT).show();

		} else if (id == R.id.set_drone_2) {

			ID_to_Send = 2;
			Toast.makeText(MainActivity.this, "选择无人机2", Toast.LENGTH_SHORT).show();

		} else if (id == R.id.set_drone_3) {

			ID_to_Send = 3;
			Toast.makeText(MainActivity.this, "选择无人机3", Toast.LENGTH_SHORT).show();

		} else if (id == R.id.set_drone_4) {

			ID_to_Send = 4;
			Toast.makeText(MainActivity.this, "选择无人机4", Toast.LENGTH_SHORT).show();

		} else if (id == R.id.set_wifi_router) {


			Toast.makeText(MainActivity.this, "切换至: Wifi路由器通信", Toast.LENGTH_SHORT).show();

		} else if (id == R.id.set_wifi_ap) {

			Toast.makeText(MainActivity.this, "切换至: Wifi热点通信", Toast.LENGTH_SHORT).show();

		} else if (id == R.id.set_gprs) {

			Toast.makeText(MainActivity.this, "切换至: GPRS通信", Toast.LENGTH_SHORT).show();
		}


		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}


	// 其他函数_____________________________________________________

	private int ID_PreCheck(String in) {

		// 接收的时候用来查ID的代码
		String str;
		String num_temp;
		int offset = 0, end = 0;		// offset: 每个数据开始的位置  end: 每个数据结束的位置
		int id = 0;

		str = in;

		// 测试代码
		//Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();

		offset = str.indexOf("ID:") + "ID:".length();    //数据开始的位置
		end = str.indexOf(" ");                        // 空格的位置

		if (end <= offset ||
			offset <= 0 || end <= 0) return -1;
		num_temp = str.substring(offset, end);

		id = Integer.parseInt(num_temp);

		return id;
	}
}
