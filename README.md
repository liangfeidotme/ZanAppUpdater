ZanAppUpdater
===

* 强制更新
* 提示更新
* 提供应用市场下载
* 提供浏览器下载
* 支持 Android N - FileProvider

依赖
---

```groovy
compile 'com.youzan.mobile:appupdater:1.0.0-SNAPSHOT'
```

用法
---

### 强制升级

```java
new AppUpdater.Builder(this)
    .url("https://dl.yzcdn.cn/koudaitong.apk")
    .title("版本更新啦")
    .content("1. 很牛逼\n2. 很厉害\n3. 吊炸天")
    .app("有赞微商城")
    .description("做生意 用有赞")
    .force(true)
    .build()
    .update();
```

### 提示升级

```java
new AppUpdater.Builder(this)
    .url("https://dl.yzcdn.cn/koudaitong.apk")
    .title("版本更新啦")
    .content("1. 很牛逼\n2. 很厉害\n3. 吊炸天")
    .app("有赞微商城")
    .description("做生意 用有赞")
    .force(false)
    .build()
    .update();
```

### 优势

* 通过 DownloadManager 下载，减少 App 负担
* 通过 Receiver 接收下载完成的广播，即使 App 被杀死依然会弹出安装界面
* 提供市场下载选项，以防下载失败
* 总之，更新率会提高很多

### 作者

* 梁飞
