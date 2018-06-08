# GolderMapDeamo
高德地图的demo
该apk实现的是使用高德地图api实现地址搜索，根据2/公里和1/元来计算费用。
使用高德地图api前需要做以下操作
1.在高德地图官网申请账号获得相应的key值
2.下载相应的SDK
3.将相应的jar包拷贝到lib里面，如果除开jar包后还有其他文件
在src/main目录下新建文件目录jniLibs将其拷贝到这个目录下
4.添加依赖，为了让添加的jar生效需要在app的build.gradle中添加
compile fileTree(include: ['*.jar'], dir: 'libs')
5.在manifest添加key值，在manifest中添加在高德地图中申请的key值
如下
 <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <meta-data
            android:name="com.amap.api.v2.apikey"
            android:value="1b34a51d5eb037c0c0d8b4a28e54f462"></meta-data>
