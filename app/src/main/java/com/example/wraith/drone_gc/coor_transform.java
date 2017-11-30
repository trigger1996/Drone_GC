package com.example.wraith.drone_gc;

import com.amap.api.maps.model.LatLng;

/**
 *
 *
 https://github.com/JackZhouCn/JZLocationConverter

 * Created by Wandergis on 2015/7/8.

 * 提供了百度坐标（BD09）、国测局坐标（火星坐标，GCJ02）、和WGS84坐标系之间的转换

 */

//UMD魔法代码

// if the module has no dependencies, the above pattern can be simplified to


public class coor_transform {

	//定义一些常量
	double x_PI = 3.14159265358979324 * 3000.0 / 180.0;
	double PI = 3.1415926535897932384626;
	double a = 6378245.0;
	double ee = 0.00669342162296594323;

	// 坐标存储
	public LatLng g_gcj02;
	public LatLng g_wgs84;
	public coor_transform() {

		// 构造
		g_gcj02 = new LatLng(0, 0);
		g_wgs84 = new LatLng(0, 0);
	}

	public coor_transform(LatLng gcj02_in) {

		// 构造，利用高德的原始条件
		g_gcj02 = gcj02_in;
		g_wgs84 = gcj02towgs84(g_gcj02.longitude, g_gcj02.latitude);
	}

	/**
	 *
	 * 百度坐标系 (BD-09) 与 火星坐标系 (GCJ-02)的转换
	 * 即 百度 转 谷歌、高德
	 * @param bd_lon
	 * @param bd_lat
	 * @returns {*[]}
	 *
	 */
	public LatLng bd09togcj02(double bd_lon, double bd_lat) {

		double x = bd_lon - 0.0065;
		double y = bd_lat - 0.006;

		double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_PI);
		double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_PI);

		double gg_lng = z * Math.cos(theta);
		double gg_lat = z * Math.sin(theta);

		LatLng ret = new LatLng(gg_lat, gg_lng);

		g_gcj02 = ret;
		return ret;

	}

	/**

	 * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换
	 * 即谷歌、高德 转 百度
	 * @param lng
	 * @param lat
	 * @returns {*[]}

	 */
	public LatLng gcj02tobd09(double lng, double lat) {

		double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * x_PI);
		double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * x_PI);

		double bd_lng = z * Math.cos(theta) + 0.0065;
		double bd_lat = z * Math.sin(theta) + 0.006;

		LatLng ret = new LatLng(bd_lat, bd_lng);

		return ret;

	}

	/**

	 * WGS84转GCj02
	 * @param lng
	 * @param lat
	 * @returns {*[]}

	 */

	public LatLng wgs84togcj02(double lng, double lat) {


		if (out_of_china(lng, lat)) {

			LatLng ret = new LatLng(lat, lng);
			return ret;

		} else {

			double dlat = transformlat(lng - 105.0, lat - 35.0);
			double dlng = transformlng(lng - 105.0, lat - 35.0);

			double radlat = lat / 180.0 * PI;

			double magic = Math.sin(radlat);
			magic = 1 - ee * magic * magic;
			double sqrtmagic = Math.sqrt(magic);

			dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);
			dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI);

			double mglat = lat + dlat;
			double mglng = lng + dlng;

			LatLng ret = new LatLng(mglat, mglng);
			g_gcj02 = ret;
			return ret;

		}

	}

	/**

	 * GCJ02 转换为 WGS84
	 * @param lng
	 * @param lat
	 * @returns {*[]}

	 */

	public LatLng gcj02towgs84(double lng, double lat) {

		if (out_of_china(lng, lat)) {

			LatLng ret = new LatLng(lat, lng);
			return ret;

		} else {

			double dlat = transformlat(lng - 105.0, lat - 35.0);
			double dlng = transformlng(lng - 105.0, lat - 35.0);

			double radlat = lat / 180.0 * PI;
			double magic = Math.sin(radlat);
			magic = 1 - ee * magic * magic;
			double sqrtmagic = Math.sqrt(magic);

			dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);

			dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI);

			double mglat = lat + dlat;
			double mglng = lng + dlng;

			LatLng ret = new LatLng(lat * 2 - mglat, lng * 2 - mglng);
			g_wgs84 = ret;
			return ret;

		}

	}

	private double transformlat(double lng, double lat) {

		double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));

		ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
		ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;

		return ret;

	};



	private double transformlng(double lng, double lat) {

		double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
		ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
		ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;

		return ret;

	};



	/**

	 * 判断是否在国内，不在国内则不做偏移
	 * @param lng
	 * @param lat
	 * @returns {boolean}

	 */

	private boolean out_of_china(double lng, double lat) {

		// 纬度3.86~53.55,经度73.66~135.05
		return !(lng > 73.66 && lng < 135.05 && lat > 3.86 && lat < 53.55);

	};

}