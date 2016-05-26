GenesisUpdater
===

* 作者：石头
* 文档：http://doc.qima-inc.com/display/engineer/GenesisUpdaterAndroid

**版本检查与规则由项目决定，Updater提供多种下载方式、显示、安装。**

###1.依赖
```groovy
compile 'com.youzan.mobile:genesis:0.4.2-SNAPSHOT'
```

###2.配置
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

<service android:name="com.youzan.genesis.UpdateAppService"/>
```

###3.调用
####3.1直接下载
```java
new UpdateApp.Builder(attachActivity,getString(R.string.app_name),downloadUrl)
        .build()
        .download();
```
####3.2提示下载
```java
new UpdateApp.Builder(TabMainActivity.this, getString(R.string.app_name), downloadUrl)
        .title("title")
        .content("content")
        .cancelableDialog(false)
        .build()
        .showDialog();
```
####3.3静默下载
```java
new UpdateApp.Builder(MainActivity.this,
        "有赞", "http://www.eoemarket.com/download/356118_0")
        .silent(true)
        .addSilentListener(new UpdateApp.OnSilentDownloadListener() {
            @Override
            public void onSuccess(Uri fileUri) {
                Toast.makeText(MainActivity.this, "Success: " + fileUri.toString(),
                        Toast.LENGTH_LONG).show();
            }
 
            @Override
            public void onError(String msg) {
                Toast.makeText(MainActivity.this, "Failed: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        })
        .build()
        .download();
```


