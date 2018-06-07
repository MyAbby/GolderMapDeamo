package com.example.tingwang.goldermapdemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.DistanceItem;
import com.amap.api.services.route.DistanceResult;
import com.amap.api.services.route.DistanceSearch;
import com.amap.api.services.route.DriveRouteResult;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements AMapLocationListener, LocationSource,GeocodeSearch.OnGeocodeSearchListener,DistanceSearch.OnDistanceSearchListener
,View.OnClickListener{
    private static final String TAG = "tingting/MainActivity";

    private static final String LOCATION_CODE = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final int ERROR_CODE = 0;

    private static final int SERACH_SUCCUSS_CODE = 1000;

    private static final int PERMISSION_CODE = 1;

    private AMapLocationClient mLocationClient = null;
    //private AMapLocationListener mLocationListener;
    private AMapLocationClientOption mLocationClientOption;
    private OnLocationChangedListener mListener;
    private MyLocationStyle mLocationStyle;
    private MapView mapView;
    private AMap map;

    private GeocodeSearch mGeocodeSearch;

    private DistanceSearch mDistanceSearch;

    private EditText mEditText;
    private Button mButton;
    private TextView mTextView;
    private DialogInterface mDialog;

    private String showText;
    private String city = "成都";

    private double startLat, startLong ,endLat, endLong;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_main);
        requestPermission();
        checkNetWork();
        mEditText = (EditText) findViewById(R.id.destination);
        mTextView = (TextView) findViewById(R.id.showResult);
        mButton = (Button) findViewById(R.id.search);
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        //初始化定位蓝点样式类
        mLocationStyle = new MyLocationStyle();
        //设置连续模式下定位间隔
        mLocationStyle.interval(2000);
        //蓝点展现模式
        mLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW);
        mLocationStyle.anchor(0.0f, 0.5f);
        // 自定义定位蓝点图标
        mLocationStyle.myLocationIcon(
                BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
        // 自定义精度范围的圆形边框颜色
        mLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
        // 自定义精度范围的圆形边框宽度
        mLocationStyle.strokeWidth(0);
        // 设置圆形的填充颜色
        mLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));
        //是否显示定位小蓝点
        mLocationStyle.showMyLocation(true);
        if (map == null) {
            map = mapView.getMap();
            // 设置定位监听
            map.setLocationSource(this);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            map.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
            // 设置定位蓝点的style
            map.setMyLocationStyle(mLocationStyle);
            // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
            map.setMyLocationEnabled(true);
        }

        mButton.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        requestPermission();
        if (mDialog != null){
            if (Util.isWifiEnabled(this) || Util.isNetworkAvailable(this)) {
                mDialog.dismiss();
            }
        }
    }

    private void requestPermission() {
        if (!Util.hasPermission(LOCATION_CODE, getApplicationContext())) {
            ActivityCompat.requestPermissions(this, new String[]{LOCATION_CODE}, PERMISSION_CODE);
        }
    }

    private void checkNetWork() {
        Log.d(TAG, "checkNetWork: 网络是否连接" + Util.isWifiEnabled(this));
        Log.d(TAG, "checkNetWork: wifi是否打开" + Util.isNetworkAvailable(this));
        if (!Util.isWifiEnabled(this) && !Util.isNetworkAvailable(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("网络设置提醒")
                    .setMessage("网络未连接会导致定位出现偏差，是否进行设置")
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mDialog = dialog;
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent;
                            if (!Util.isNOrLater()){
                                intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                            } else {
                                intent = new Intent();
                                ComponentName component = new ComponentName(
                                        "com.android.settings",
                                        "com.android.settings.WirelessSettings");
                                intent.setComponent(component);
                                intent.setAction("android.intent.action.VIEW");
                            }
                            if (intent !=null){
                                startActivity(intent);
                            }
                        }
                    });
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions.length > 0 && requestCode == PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                finish();
            }
        }
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation != null && aMapLocation.getErrorCode() == ERROR_CODE) {
                mListener.onLocationChanged(aMapLocation);
                String province = aMapLocation.getProvince();
                city = aMapLocation.getCity();
                String district = aMapLocation.getDistrict();
                String street = aMapLocation.getStreet();
                String streetNum = aMapLocation.getStreetNum();
                startLat = aMapLocation.getLatitude();
                startLong = aMapLocation.getLongitude();
               // Log.d(TAG, "onLocationChanged: 起始经度纬度 " + startLat + " "+startLong);

            } else {
                Log.d(TAG, "onLocationChanged: error " + aMapLocation.getErrorCode() +
                        "info " + aMapLocation.getErrorInfo());
                showText = "定位失败,请检查网络";

            }
        }

    }

    private void searchAdress(){
        mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(this);
        GeocodeQuery query = new GeocodeQuery(mEditText.getText().toString(),city);
        mGeocodeSearch.getFromLocationNameAsyn(query);
    }

    private void searchDistanch(){
        Log.d(TAG, "searchDistanch: ..");
        mDistanceSearch = new DistanceSearch(this);
        mDistanceSearch.setDistanceSearchListener(this);
        DistanceSearch.DistanceQuery distanceQuery = new DistanceSearch.DistanceQuery();
        LatLonPoint start = new LatLonPoint(startLat, startLong);
        List<LatLonPoint> latLonPoints = new ArrayList<LatLonPoint>();
        latLonPoints.add(start);
        LatLonPoint dest = new LatLonPoint(endLat, endLong);
        distanceQuery.setOrigins(latLonPoints);
        distanceQuery.setDestination(dest);
        //设置测量方式，支持直线和驾车
        distanceQuery.setType(DistanceSearch.TYPE_DRIVING_DISTANCE);
        mDistanceSearch.calculateRouteDistanceAsyn(distanceQuery);

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            //初始化定位
            mLocationClient = new AMapLocationClient(this);
            //设置定位回调监听器
            mLocationClient.setLocationListener(this);
            //初始化定位参数
            mLocationClientOption = new AMapLocationClientOption();
            //设置高精度定位模式
            mLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationClientOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Transport);
            mLocationClientOption.setInterval(3000);//定位连续模式
            mLocationClientOption.setNeedAddress(true);
            mLocationClientOption.setHttpTimeOut(5000);
            mLocationClient.setLocationOption(mLocationClientOption);
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }

    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.startLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;

    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
        //Log.d(TAG, "onGeocodeSearched: ....");
        if (i == SERACH_SUCCUSS_CODE){
            if (geocodeResult != null && geocodeResult.getGeocodeAddressList() != null
                    && geocodeResult.getGeocodeAddressList().size() > 0) {
                GeocodeAddress address = geocodeResult.getGeocodeAddressList().get(0);
                String addressName = "目的地经纬度值:" + address.getLatLonPoint() + "\n位置描述:"
                        + address.getFormatAddress();
                //Log.d(TAG, "onGeocodeSearched: " + addressName);
                //获取到的经纬度
                LatLonPoint latLongPoint = address.getLatLonPoint();
                endLat =  latLongPoint.getLatitude();
                endLong = latLongPoint.getLongitude();
                //Log.d("111", Double.toString(endLat));
                searchDistanch();
            }

        }

    }

    @Override
    public void onDistanceSearched(DistanceResult distanceResult, int i) {
        Log.d(TAG, "onDistanceSearched: i = "+ i);
        if (i == SERACH_SUCCUSS_CODE){
            List<DistanceItem> list = distanceResult.getDistanceResults();
            Log.d(TAG, "onDistanceSearched: list " + list.size());
            for (DistanceItem item : list){
                float distance = item.getDistance() / (float) 1000;
                float time = item.getDuration() /(float) 60;
                float money = distance * 2 + 1 * time ;
                java.text.DecimalFormat df = new java.text.DecimalFormat("#.0");
                df.format(money);
                mTextView.setVisibility(View.VISIBLE);
                showText = "此次行程大约" + df.format(distance) +"公里" +"大约需要花费" + df.format(time) + "分钟"
                        +"\n" + "\n" + "预计" +df.format(money) + "元";
                mTextView.setText(showText);
                Log.d(TAG, "onDistanceSearched: distance =" + distance);
                Log.d(TAG, "onDistanceSearched: time = "+ time);
            }

        }

    }

    @Override
    public void onClick(View v) {
        hideSoftInput();
        searchAdress();
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftInput(){
        InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }
    private boolean checkGpsSevice() {
        LocationManager manager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        boolean gps = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }

        return false;
    }
}
