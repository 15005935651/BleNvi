package com.example.axin.blenvi;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;
import com.tencent.connect.UserInfo;
import com.tencent.connect.auth.QQToken;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //百度地图数据
    private LocationClient mLocationClient;
    private MyLocationListener mLocationListener;
    public  static double latitude,longitude;
    private boolean isFirstLocation=true;



    //单车导航
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LatLng startPt,endPt;
    private BikeNavigateHelper mNaviHelper;
    private WalkNavigateHelper mWNaviHelper;
    private BikeNaviLaunchParam param;
    private WalkNaviLaunchParam walkParam;
    private static boolean isPermissionRequested = false;


    // QQ登陆
    private Tencent mTencent;
    private String openidString;
    private TextView username;
    private String QQname;
    private String QQheadURL;
    private CircleImageView qqlogin;
    private boolean isFirstLogin=false;

    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;




 //退出按钮
 private static boolean isExit = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        //个性化地图
//        setMapCustomFile();
        setContentView(R.layout.activity_main);
//        MapView.setMapCustomEnable(true);

        //单车导航
        requestPermission();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //加载百度地图数据
        mMapView = (MapView) findViewById(R.id.bmapView);
        initMap();
        initLocation();

        //单车导航
        final FloatingActionButton bikeBtn = (FloatingActionButton) findViewById(R.id.bikenvi);
        bikeBtn.setTitle("单车导航");
        bikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBikeNavi();
            }
        });
        bikeBtn.setStrokeVisible(false);
        final FloatingActionButton walkBtn = (FloatingActionButton) findViewById(R.id.walknvi);
        walkBtn.setTitle("步行导航");
        walkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // startWalkNavi();
            Intent intent = new Intent(MainActivity.this,PoiSearchDemo.class);
                startActivity(intent);
            }
        });
        try {
            mNaviHelper = BikeNavigateHelper.getInstance();
            mWNaviHelper = WalkNavigateHelper.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
//起始坐标点
        startPt = new LatLng(27.519709,106.92134);
        endPt = new LatLng(27.528423,106.888929);
        param = new BikeNaviLaunchParam().stPt(startPt).endPt(endPt);
        walkParam = new WalkNaviLaunchParam().stPt(startPt).endPt(endPt);
        AppCompatImageButton reloc=(AppCompatImageButton)findViewById(R.id.reloc);
        reloc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBaiduMap.clear();
               isFirstLocation=true;
            }
        });


        View headview=navigationView.inflateHeaderView(R.layout.nav_header_main);
        qqlogin= (CircleImageView) headview.findViewById(R.id.qqlogin);
        username=(TextView)headview.findViewById(R.id.username);
        qqlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFirstLogin==false) {
                    mTencent = Tencent.createInstance("1106522059", getApplicationContext());
                    mTencent.login(MainActivity.this, "all", new BaseUiListener());
                    // showToast("head iv");
                }
                else {
                    Toast.makeText(getApplication(),"已登录",Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPref = getSharedPreferences("user_data", MODE_PRIVATE);
        mEditor = mPref.edit();
        //若之前曾设置过记住用户名，则读取并设置用户名
        if (mPref.getBoolean("is_remember", false)) {
            username.setText(mPref.getString("user_name", ""));
            QQheadURL=mPref.getString("qqHeadUrl", "");
            Picasso.with(getApplicationContext()).load(QQheadURL).into(qqlogin);
//            String temp = mPref.getString("P", "");
//            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(temp.getBytes(), Base64.DEFAULT));
//            qqlogin.setImageDrawable(Drawable.createFromStream(bais, ""));

        }



    }


    //初始化地图
    private void initMap() {

        // 获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        //不显示缩放尺
        mMapView. showScaleControl(true);
        mMapView.setScaleControlPosition(new Point(100, 300));

        // 不显示百度地图Logo
        mMapView.removeViewAt(1);
        // 显示地图缩放控件（按钮控制栏）
        mMapView.showZoomControls(false);
        // 百度地图
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
                                             @Override
                                             public void onMapLoaded() {
                                                 MapView.setMapCustomEnable(true);
                                             }
        });
        // 改变地图状态
        MapStatus mMapStatus = new MapStatus.Builder().zoom(15).build();
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);
        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus arg0) {
            }

            @Override
            public void onMapStatusChangeFinish(MapStatus arg0) {
            }

            @Override
            public void onMapStatusChange(MapStatus arg0) {
            }

            @Override
            public void onMapStatusChangeStart(MapStatus arg0, int arg1) {
                // TODO 自动生成的方法存根

            }
        });

    }

//初始化定位
private void initLocation() {
    // 定位客户端的设置
    mLocationClient = new LocationClient(this);
    mLocationListener = new MyLocationListener();
    // 注册监听
    mLocationClient.registerLocationListener(mLocationListener);
    // 配置定位
    LocationClientOption option = new LocationClientOption();
    option.setCoorType("bd09ll");// 坐标类型
    option.setIsNeedAddress(true);// 可选，设置是否需要地址信息，默认不需要
    option.setOpenGps(true);// 打开Gps
    option.setScanSpan(1000);// 1000毫秒定位一次
    option.setIsNeedLocationPoiList(true);// 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
    mLocationClient.setLocOption(option);

}

    public void setMapCustomFile() {
        FileOutputStream out = null;
        InputStream inputStream = null;
        try {
            inputStream = getAssets().open("custom_map.txt");
            byte[] b = new byte[inputStream.available()];
            inputStream.read(b);

            String moduleName = getFilesDir().getAbsolutePath();
            File f = new File(moduleName + "/" + "custom_map.txt");
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            out = new FileOutputStream(f);
            out.write(b);
          //  Log.i("ss","setCustomMapStylePath->  " + moduleName + "/map_style.txt");
            MapView.setCustomMapStylePath(moduleName + "/custom_map.txt");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        // 开启定位
        mBaiduMap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
            super.onStart();
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();

    }

    // 自定义的定位监听
    private class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null)
                return;
            // 将获取的location信息给百度map
            MyLocationData data = new MyLocationData.Builder().accuracy(0)// location.getRadius()
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .latitude(location.getLatitude()).longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(data);
            if (isFirstLocation) {
                // 获取经纬度
                LatLng center = new LatLng(location.getLatitude()-0.0003, location.getLongitude());
                MapStatusUpdate status = MapStatusUpdateFactory.newLatLngZoom(center, 19.5f);
                mBaiduMap.animateMapStatus(status);// 动画的方式到中间
                latitude = location.getLatitude();    //获取纬度信息
                longitude = location.getLongitude();    //获取经度信息
                isFirstLocation = false;
                //showInfo("当前位置：" + location.getAddrStr());
                //自定义图标
                LatLng point1 = new LatLng(latitude+0.00005,longitude);
                BitmapDescriptor locmaker = BitmapDescriptorFactory
                        .fromResource(R.drawable.bike);
                OverlayOptions mylocation = new MarkerOptions()
                        .position(point1)  //设置Marker的位置
                        .icon(locmaker)  //设置Marker图标
                        .zIndex(9)//设置Marker所在层级
                        .draggable(true);  //设置手势拖拽
                mBaiduMap.addOverlay(mylocation);
                mBaiduMap.setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {
                    public void onMarkerDrag(Marker marker) {
                        //拖拽中
                    }
                    public void onMarkerDragEnd(Marker marker) {
                        //拖拽结束
                    }
                    public void onMarkerDragStart(Marker marker) {
                        //开始拖拽
                    }
                });

            }

        }

    }
//返回键
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!isExit) {
                isExit = true;
                Toast.makeText(getApplicationContext(), "再按一次退出程序",
                        Toast.LENGTH_SHORT).show();
                // 利用handler延迟发送更改状态信息
                mHandler.sendEmptyMessageDelayed(0, 2000);
            } else {
                finish();
                System.exit(0);
            }
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void startBikeNavi() {
        Log.d("View", "startBikeNavi");
        try {
            mNaviHelper.initNaviEngine(this, new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d("View", "engineInitSuccess");
                    routePlanWithParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d("View", "engineInitFail");
                }
            });
        } catch (Exception e) {
            Log.d("Exception", "startBikeNavi");
            e.printStackTrace();
        }
    }

    private void startWalkNavi() {
        Log.d("View", "startBikeNavi");
        try {
            mWNaviHelper.initNaviEngine(this, new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d("View", "engineInitSuccess");
                    routePlanWithWalkParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d("View", "engineInitFail");
                }
            });
        } catch (Exception e) {
            Log.d("Exception", "startBikeNavi");
            e.printStackTrace();
        }
    }

    private void routePlanWithParam() {
        mNaviHelper.routePlanWithParams(param, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d("View", "onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d("View", "onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, BNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d("View", "onRoutePlanFail");
            }

        });
    }
    private void routePlanWithWalkParam() {
        mWNaviHelper.routePlanWithParams(walkParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d("View", "onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d("View", "onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, WNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d("View", "onRoutePlanFail");
            }

        });
    }


    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {

            isPermissionRequested = true;

            ArrayList<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (permissions.size() == 0) {
                return;
            } else {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 0);
            }
        }
    }

//QQ登陆
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Tencent.onActivityResultData(requestCode, resultCode, data, new MainActivity.BaseUiListener());

    if (requestCode == Constants.REQUEST_API) {
        if (resultCode == Constants.REQUEST_LOGIN) {
            Tencent.handleResultData(data, new MainActivity.BaseUiListener());
        }
    }

}

    private class BaseUiListener implements IUiListener {
        public void onComplete(Object response) {
            // TODO Auto-generated method stub
            Toast.makeText(getApplicationContext(), "登录成功", Toast.LENGTH_SHORT).show();
            /*
            * 下面隐藏的是用户登录成功后 登录用户数据的获取的方法
            * 共分为两种  一种是简单的信息的获取,另一种是通过UserInfo类获取用户较为详细的信息
            *有需要看看
            * */
            try {
                //获得的数据是JSON格式的，获得你想获得的内容
                //如果你不知道你能获得什么，看一下下面的LOG
                Log.v("----TAG--", "-------------" + response.toString());
                openidString = ((JSONObject) response).getString("openid");
                mTencent.setOpenId(openidString);

                mTencent.setAccessToken(((JSONObject) response).getString("access_token"), ((JSONObject) response).getString("expires_in"));

                Log.v("TAG", "-------------" + openidString);
                //access_token= ((JSONObject) response).getString("access_token");              //expires_in = ((JSONObject) response).getString("expires_in");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
//            *//**到此已经获得OpneID以及其他你想获得的内容了
//             QQ登录成功了，我们还想获取一些QQ的基本信息，比如昵称，头像什么的，这个时候怎么办？
//             sdk给我们提供了一个类UserInfo，这个类中封装了QQ用户的一些信息，我么可以通过这个类拿到这些信息
//             如何得到这个UserInfo类呢？  *//*

            QQToken qqToken = mTencent.getQQToken();
            UserInfo info = new UserInfo(getApplicationContext(), qqToken);

            //    info.getUserInfo(new BaseUIListener(this,"get_simple_userinfo"));
            info.getUserInfo(new IUiListener() {
                @Override
                public void onComplete(Object o) {
                    //用户信息获取到了

                    try {
                        QQheadURL=((JSONObject) o).getString("figureurl_qq_2");
                        QQname=((JSONObject) o).getString("nickname");
                        Log.v("UserInfo", o.toString());



                        Picasso.with(getApplicationContext()).load(QQheadURL).into(qqlogin);
                        username.setText(QQname);
                        isFirstLogin=true;
                        //保存QQ名称
                        mEditor.putString("user_name", QQname);
                        mEditor.putBoolean("is_remember", true);
                        mEditor.putString("qqHeadUrl", QQheadURL);

                        //保存QQ头像
//                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.id.qqlogin);
//                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
//                        String imageBase64 = new String(Base64.encodeToString(baos.toByteArray(),Base64.DEFAULT));
//                        mEditor.putString("P",imageBase64 );
                        mEditor.commit();


                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(UiError uiError) {
                    Log.v("UserInfo", "onError");
                }

                @Override
                public void onCancel() {
                    Log.v("UserInfo", "onCancel");
                }
            });

        }

        @Override
        public void onError(UiError uiError) {
            Toast.makeText(getApplicationContext(), "onError", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel() {
            Toast.makeText(getApplicationContext(), "onCancel", Toast.LENGTH_SHORT).show();
        }


    }


}
