package com.example.wraith.drone_gc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.format.Time;
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

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.DataTruncation;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
		implements	NavigationView.OnNavigationItemSelectedListener,
					LocationSource, AMapLocationListener,AMap.OnMapClickListener{

	public static Context context;

	// UDP相关
	private UDPServer udpServer = null;
	private Datalink  Link_1 = null, Link_2 = null, Link_3 = null, Link_4 = null;
	private int       ID_to_Send    = 0;		// 选择发送的飞机ID号
	private boolean   Flag_Formation = false;	// 编队飞行标识位

	private static int time_to_clear_map = 0;

	///  地图控件相关
	MapView mMapView = null;
	AMap aMap = null;
	MyLocationStyle myLocationStyle;

	/// 定位控件相关
	private LocationSource.OnLocationChangedListener mListener;
	private AMapLocationClient mlocationClient;//定位服务类。此类提供单次定位、持续定位、地理围栏、最后位置相关功能。
	private AMapLocationClientOption mLocationOption;//定位参数设置，通过这个类可以对定位的相关参数进行设置
	private TextView mLocationErrText;

	/// 读取坐标以及控制相关
	double latitude;
	double longitude;
	MarkerOptions DroneLoc_1 = null, DroneLoc_2 = null, DroneLoc_3 = null, DroneLoc_4 = null;	// 飞机在地图上的标记

	// 飞机坐标转换
	CoordinateConverter DroneCov_1, DroneCov_2, DroneCov_3, DroneCov_4;
	// 自身坐标转换
	PositionUtil TgtCov;
	Gps wgs;


	int 	cur_Task = 1;			// 当前任务, 这个还不知道怎么用
	double	target_Alt = 2.5f;		// 目标高度，单位：m，这个也不会用

	// 网络控制相关
	enum net_workmode{Wifi_Router , Wifi_AP, GPRS};
	net_workmode Flag_NetworkWokMode = net_workmode.Wifi_Router;		// 当前选择的网络工作模式

	/// UI控件相关
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

		// 地图初始化
		//获取地图控件引用
		mMapView = (MapView) findViewById(R.id.map);
		//在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
		mMapView.onCreate(savedInstanceState);
		Map_Init();

		/// 显示无人机的状态
		StatText = (TextView)findViewById(R.id.test_main);
		StatText.setAlpha(0.55f);		// 1位不透明

		FloatingActionButton set_dest = (FloatingActionButton) findViewById(R.id.set_dest);
		set_dest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "让飞机飞到这里~", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();

				// 获取时间
				long currentTimeMillis = System.currentTimeMillis();
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(currentTimeMillis);

				int hour    = calendar.get(Calendar.HOUR_OF_DAY);
				int minute  = calendar.get(Calendar.MINUTE);
				int second  = calendar.get(Calendar.SECOND);
				int msecond = calendar.get(Calendar.MILLISECOND);

				// 发送任务______________________________________________________________________
				/// 设置飞机目的地的代码
				if (Flag_Formation == false) {

					//单架飞行
					switch (ID_to_Send) {
						case 1 : {
							Link_1.Snd_Update_Link(cur_Task,
									hour, minute, second, msecond,
									wgs.getWgLon(), wgs.getWgLat(), target_Alt);
							Link_1.Snd_Preload_PlainText();
							udpServer.Tx = Link_1.Send;
						} break;

						case 2 : {
							Link_2.Snd_Update_Link(cur_Task,
									hour, minute, second, msecond,
									wgs.getWgLon(), wgs.getWgLat(), target_Alt);
							Link_2.Snd_Preload_PlainText();
							udpServer.Tx = Link_2.Send;

						} break;

						case 3 : {
							Link_3.Snd_Update_Link(cur_Task,
									hour, minute, second, msecond,
									wgs.getWgLon(), wgs.getWgLat(), target_Alt);
							Link_3.Snd_Preload_PlainText();
							udpServer.Tx = Link_3.Send;

						} break;

						case 4 : {
							Link_4.Snd_Update_Link(cur_Task,
									hour, minute, second, msecond,
									wgs.getWgLon(), wgs.getWgLat(), target_Alt);
							Link_4.Snd_Preload_PlainText();
							udpServer.Tx = Link_4.Send;
						} break;

						default : {
							// default不允许操作，防止误操作
						} break;
					}
				} else {

					// 编队飞行
					// 暂时采用简单的一字编队，向北展开
					double spread = 2f / 100000.0f;	// 队形间距，10^-5大约是1米

					// 1号机
					Link_1.Snd_Update_Link(cur_Task,
							hour, minute, second, msecond,
							wgs.getWgLon(), wgs.getWgLat(), target_Alt);
					Link_1.Snd_Preload_PlainText();
					udpServer.Tx = Link_1.Send;

					// 2号机
					Link_2.Snd_Update_Link(cur_Task,
							hour, minute, second, msecond,
							wgs.getWgLon() + spread, wgs.getWgLat(), target_Alt);
					Link_2.Snd_Preload_PlainText();
					udpServer.Tx = Link_2.Send;

					// 3号机
					Link_3.Snd_Update_Link(cur_Task,
							hour, minute, second, msecond,
							wgs.getWgLon() + spread * 2, wgs.getWgLat(), target_Alt);
					Link_3.Snd_Preload_PlainText();
					udpServer.Tx = Link_3.Send;

					// 4号机
					Link_4.Snd_Update_Link(cur_Task,
							hour, minute, second, msecond,
							wgs.getWgLon() + spread * 3, wgs.getWgLat(), target_Alt);
					Link_4.Snd_Preload_PlainText();
					udpServer.Tx = Link_4.Send;
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


		// 初始化坐标转换
		DroneCov_1 = new CoordinateConverter(context);
		DroneCov_2 = new CoordinateConverter(context);
		DroneCov_3 = new CoordinateConverter(context);
		DroneCov_4 = new CoordinateConverter(context);

		DroneCov_1.from(CoordinateConverter.CoordType.GPS);
		DroneCov_2.from(CoordinateConverter.CoordType.GPS);
		DroneCov_3.from(CoordinateConverter.CoordType.GPS);
		DroneCov_4.from(CoordinateConverter.CoordType.GPS);

		TgtCov = new PositionUtil();
		wgs = new Gps(0.0f, 0.0f);

		// 设置一个初始ID
		ID_to_Send = 2;
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
		mMapView.onDestroy();
	}
	@Override
	protected void onResume() {
		super.onResume();
		//在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
		mMapView.onResume();
	}
	@Override
	protected void onPause() {
		super.onPause();
		//在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
		mMapView.onPause();
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

			// 接收任务______________________________________________________________________
			if (intent.hasExtra("udpRcvMsg")) {
				Message message = new Message();
				message.obj = intent.getStringExtra("udpRcvMsg");
				message.what = 1;
				//Log.i("主界面Broadcast","收到"+message.obj.toString());

				if (message.obj.toString() != null &&
						message.obj.toString().indexOf("Radio Check!") == -1) {

					// 现在有四架飞机，所以第一轮首先查ID
					int id_recv = ID_PreCheck(message.obj.toString());

					switch (id_recv) {
						case 1: {
							Link_1.Rfn_Rpi_2_Server_PlainText(message.obj.toString());

						}
						break;

						case 2: {
							Link_2.Rfn_Rpi_2_Server_PlainText(message.obj.toString());

						}
						break;

						case 3: {
							Link_3.Rfn_Rpi_2_Server_PlainText(message.obj.toString());

						}
						break;

						case 4: {
							Link_4.Rfn_Rpi_2_Server_PlainText(message.obj.toString());

						}
						break;

						/*
						default : {
							udpServer.Tx = "Received!";
						} break;
						*/
					}

				}

				// 显示飞机的信息
				StatText.setText(ID_to_Send + " 号机\n");
				switch (ID_to_Send) {
					case 1: {

						StatText.append("Pitch:	" + Link_1.Pitch + "\n" +
								"Roll:	" + Link_1.Roll + "\n" +
								"Yaw:	" + Link_1.Yaw + "\n" +
								"高度:	" + Link_1.Alt + "m" + "\n" +
								"电池:	" + Link_1.Battery_V + "V");

					}
					break;

					case 2: {

						StatText.append("Pitch:	" + Link_2.Pitch + "\n" +
								"Roll:	" + Link_2.Roll + "\n" +
								"Yaw:	" + Link_2.Yaw + "\n" +
								"高度:	" + Link_2.Alt + "m" + "\n" +
								"电池:	" + Link_2.Battery_V + "V");
					}
					break;

					case 3: {

						StatText.append("Pitch:	" + Link_3.Pitch + "\n" +
								"Roll:	" + Link_3.Roll + "\n" +
								"Yaw:	" + Link_3.Yaw + "\n" +
								"高度:	" + Link_3.Alt + "m" + "\n" +
								"电池:	" + Link_3.Battery_V + "V");

						StatText.append("\n" + "Lat: " + Link_3.Lat + "\n" + "Lng: " + Link_3.Lng);

					}
					break;

					case 4: {
						StatText.append("Pitch:	" + Link_4.Pitch + "\n" +
								"Roll:	" + Link_4.Roll + "\n" +
								"Yaw:	" + Link_4.Yaw + "\n" +
								"高度:	" + Link_4.Alt + "m" + "\n" +
								"电池:	" + Link_4.Battery_V + "V");
					}
					break;

				}


				// 显示4架飞机和当前位置
				LatLng grid_temp = null;

				if (Link_1.Lat != 0.0f && Link_1.Lng != 0.0f) {

					// 1号机
					grid_temp = new LatLng(Link_1.Lat, Link_1.Lng);
					DroneCov_1.coord(grid_temp);
					grid_temp = DroneCov_1.convert();

					DroneLoc_1.position(grid_temp);
					aMap.addMarker(DroneLoc_1);
				}
				if (Link_2.Lat != 0.0f && Link_2.Lng != 0.0f) {

					// 2号机
					grid_temp = new LatLng(Link_2.Lat, Link_2.Lng);
					DroneCov_2.coord(grid_temp);
					grid_temp = DroneCov_2.convert();

					DroneLoc_2.position(grid_temp);
					aMap.addMarker(DroneLoc_2);

				}
				if (Link_3.Lat != 0.0f && Link_3.Lng != 0.0f) {

					// 3号机
					grid_temp = new LatLng(Link_3.Lat, Link_3.Lng);
					DroneCov_3.coord(grid_temp);
					grid_temp = DroneCov_3.convert();

					DroneLoc_3.position(grid_temp);
					aMap.addMarker(DroneLoc_3);
				}
				if (Link_4.Lat != 0.0f && Link_4.Lng != 0.0f) {

					// 4号机
					grid_temp = new LatLng(Link_4.Lat, Link_4.Lng);
					DroneCov_4.coord(grid_temp);
					grid_temp = DroneCov_4.convert();

					DroneLoc_4.position(grid_temp);
					aMap.addMarker(DroneLoc_4);
				}

			}

		}
	};

	// 地图相关___________________________________________________________________________
	private void Map_Init() {

		/// 地图初始化
		if (aMap == null) {
			aMap = mMapView.getMap();

			Location_Init();
			aMap.setOnMapClickListener(this);

		}
		/// 定位初始化
		mLocationErrText = (TextView)findViewById(R.id.location_errInfo_text);
		mLocationErrText.setVisibility(View.GONE);

		// 飞机的标记的初始化
		// 这边初始化4架飞机的原始坐标值
		DroneLoc_1 = new MarkerOptions();
		DroneLoc_1.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_1));
		//DroneLoc_1.position(latLng);

		DroneLoc_2 = new MarkerOptions();
		DroneLoc_2.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_3));

		DroneLoc_3 = new MarkerOptions();
		DroneLoc_3.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_2));

		DroneLoc_4 = new MarkerOptions();
		DroneLoc_4.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_4));

	}

	private void Location_Init()
	{
		myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类
		myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
		myLocationStyle.interval(1000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
		myLocationStyle.showMyLocation(true);   // 显示当前定位位置

		aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
		//aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
		aMap.getUiSettings().setCompassEnabled(true);         // 开指南针
		aMap.getUiSettings().setScaleControlsEnabled(true);   // 开比例尺

		//aMap.setLocationSource(this);
		aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

		// 定位设置
		aMap.setLocationSource(MainActivity.this);//设置定位监听
		aMap.setMyLocationEnabled(true);//设置显示定位层，并可以出发定位
		aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置显示定位按钮

		aMap.moveCamera(CameraUpdateFactory.zoomTo(aMap.getMaxZoomLevel() - 3));
	}

	/**
	 * 定位成功后回调函数
	 */
	@Override
	public void onLocationChanged(AMapLocation amapLocation) {

		if (mListener != null && amapLocation != null) {

			if (amapLocation != null &&
				amapLocation.getErrorCode() == 0) {
				mLocationErrText.setVisibility(View.GONE);
				mListener.onLocationChanged(amapLocation);// 显示系统小蓝点

				// 当飞机存在的时候也要添加
				//aMap.addMarker(otMarkerOptions);

			} else {
				String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
				//Log.e("AmapErr",errText);
				mLocationErrText.setVisibility(View.VISIBLE);
				mLocationErrText.setText(errText);
			}

			// 这里做个补充，如果当工作在Wifi_AP模式下但热点没打开，这里通知用户打
			// 程序自己开太困难了
			if (Flag_NetworkWokMode == net_workmode.Wifi_AP &&
				isWifiApOpen(context) == false) {

				if (amapLocation != null &&
					amapLocation.getErrorCode() == 0) {
					mLocationErrText.setVisibility(View.VISIBLE);
					mLocationErrText.setText("请开启Wifi热点");

				} else {
					mLocationErrText.append("\n" + "请开启Wifi热点");

				}
			}


			// 本来还要做个如果使用高精度定位要开流量，这边算了，因为最原始的包含这个功能

		}

	}// onLocationChanged

	/**
	 * 激活定位
	 */
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		if (mlocationClient == null) {
			mlocationClient = new AMapLocationClient(this);
			mLocationOption = new AMapLocationClientOption();
			//设置定位监听
			mlocationClient.setLocationListener(this);

			// 设置定位模式
			mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);

			//设置定位参数
			mlocationClient.setLocationOption(mLocationOption);
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用onDestroy()方法
			// 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
			mlocationClient.startLocation();
		}
	}

	/**
	 * 停止定位
	 */
	@Override
	public void deactivate() {
		mListener = null;
		if (mlocationClient != null) {
			mlocationClient.stopLocation();
			mlocationClient.onDestroy();
		}
		mlocationClient = null;
	}

	//地图点击事件
	@Override
	public void onMapClick(LatLng latLng) {
	//点击地图后清理图层插上图标，在将其移动到中心位置
		aMap.clear();
		latitude = latLng.latitude;
		longitude = latLng.longitude;
		MarkerOptions otMarkerOptions = new MarkerOptions();
		otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.rotor));
		otMarkerOptions.position(latLng);
		aMap.addMarker(otMarkerOptions);
		//aMap.moveCamera(CameraUpdateFactory.changeLatng(latLng));    // 将选中的点移动到当前位置


		wgs = TgtCov.gcj_To_Gps84(latitude, longitude);
		Toast.makeText(MainActivity.this,"Lat: " + wgs.getWgLat() + " " + "Lng: " + wgs.getWgLon(), Toast.LENGTH_SHORT).show();

	}

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

			if (aMap.getMapType() == AMap.MAP_TYPE_SATELLITE) {
				aMap.setMapType(AMap.MAP_TYPE_NORMAL);
				Toast.makeText(MainActivity.this,"切换至普通地图",Toast.LENGTH_SHORT).show();
			}
			else {
				aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
				Toast.makeText(MainActivity.this,"切换至卫星地图",Toast.LENGTH_SHORT).show();
			}

			return true;
		} else if (id == R.id.action_locate_method) {

			// 切换定位方式
			if (mLocationOption.getLocationMode() == AMapLocationClientOption.AMapLocationMode.Device_Sensors) {
				//设置为高精度定位模式
				mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
				Toast.makeText(MainActivity.this,"切换至高精度定位模式",Toast.LENGTH_SHORT).show();

			} else if (mLocationOption.getLocationMode() == AMapLocationClientOption.AMapLocationMode.Hight_Accuracy) {
				// 设置为单GPS定位模式
				mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);    // Hight_Accuracy
				Toast.makeText(MainActivity.this,"切换至单GPS定位模式",Toast.LENGTH_SHORT).show();

			}

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

			Flag_NetworkWokMode = net_workmode.Wifi_Router;
			Toast.makeText(MainActivity.this, "切换至: Wifi路由器通信", Toast.LENGTH_SHORT).show();

		} else if (id == R.id.set_wifi_ap) {

			Flag_NetworkWokMode = net_workmode.Wifi_AP;
			Toast.makeText(MainActivity.this, "切换至: Wifi热点通信", Toast.LENGTH_SHORT).show();

		} else if (id == R.id.set_gprs) {

			Flag_NetworkWokMode = net_workmode.GPRS;
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

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) {
		} else {
			//如果仅仅是用来判断网络连接
			//则可以使用 cm.getActiveNetworkInfo().isAvailable();
			NetworkInfo[] info = cm.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isWifiApOpen(Context context) {
		try {
			WifiManager manager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE); //通过放射获取 getWifiApState()方法
			Method method = manager.getClass().getDeclaredMethod("getWifiApState"); //调用getWifiApState() ，获取返回值
			int state = (int) method.invoke(manager); //通过放射获取 WIFI_AP的开启状态属性
			Field field = manager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED"); //获取属性值
			int value = (int) field.get(manager); //判断是否开启
			if (state == value) {
				return true;
			} else {
				return false;
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} return false;
	}
}
