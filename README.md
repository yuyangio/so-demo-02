# 研究 SO 文件中的算法 (中)

[查看原文 >>](https://www.yuyang.io/post/android-use-so-02/)

通过上一篇文章，已经能够引入包含源代码的 SO 项目。但是真正进行逆向的时候，是没有源代码的。这就需要我们直接引用 SO 文件进行分析。本文以[上一篇文章](https://www.yuyang.io/post/android-use-so-01/)生成的 sodemo.apk 为例，介绍在没有源代码的情况下，怎样引用和运行 SO 文件。

### 解析 APK

在拿到 APK 样本以后，一般会使用 [jadx-gui](https://github.com/skylot/jadx) 等工具，先整体浏览一下 APK，看看是否做了加固和混淆，以及调用了哪些函数。以 sodemo.apk 为例，用 Jadx 打开后如下图所示:

![](https://i.loli.net/2019/11/20/mNF9dinf6O3hZJI.png)

我们可以清楚的看到 `System.loadLibrary("translib");` 用于加载 SO 文件，以及 `public native String transMsg(String str);` 用于运行 C 函数。

对于主流 APP，SO 加载一般都分散在各个 Activity 中，并且代码做了混淆。此时，可以通过 Jadx 搜索 `System.loadLibrary` 和 `public native` 两个关键词来列出所有 SO 文件和函数。

知道 SO 文件名称后 ( SO 文件名为 lib 开头，比如本文为 libtranslib.so )，可以通过 [apktool](https://github.com/iBotPeaches/Apktool) 来解压 apk 并获得相应的 SO 文件，也可以直接用解压软件来解压，在 `lib` 文件夹下面取得源文件。

_注:_ apktool 解压 APK 命令为 `apktool d -r sodemo.apk`

### 解析 SO 文件

取得 SO 文件后，就可以着手解析工作了。Android 下的 SO 文件，遵循 [ELF](https://en.wikipedia.org/wiki/Executable_and_Linkable_Format) 文件格式，可以通过诸如 [radare2](https://rada.re/n/radare2.html), [IDA Pro](https://www.hex-rays.com/products/ida/) 这样的工具进行查看，下图是采用 IDA Pro 查看的文件结果:

![](https://i.loli.net/2019/11/21/CTyGVWjtpbDliK3.png)

可以看到 `Java_com_example_sodemo_MainActivity_transMsg` 正是我们需要调用的函数。此外，还需要知道这个 SO 文件的系统架构，可以通过 apktool 解压包位置知道，本文 SO 文件系统架构是 `arm64-v8a`。

_注意:_ 目前主流 APP 的 SO 文件多采用 [ollvm](https://github.com/obfuscator-llvm/obfuscator) 技术进行了混淆，在 IDA 中只能看到 `JNI_onLoad` 函数，其他输出函数均为乱码。对于这种 SO 文件，就需要进行比较艰难的反混淆或者 Hook。篇幅有限，本文不能详述有关技巧，需要的读者可以参考 [这篇](https://bbs.pediy.com/thread-252321.htm) 文章。

### 加载 SO 到 Android 项目

现在可以正是新建一个 Andorid 项目了，建立方式与[上一篇文章](https://www.yuyang.io/post/android-use-so-01/)相同。这里需要注意项目包命名规范，必须符合 JNI 标准。比如 SO 中函数名称是 `Java_com_example_sodemo_MainActivity_transMsg`，那么 Android 项目包的名称就必须是 `com.example.sodemo` 才可以。

项目建立后，需要完成以下 4 个步骤，才可以成功导入 SO 文件。

##### 1. 新建 CMakeLists.txt 并关联 C++ 项目

首先新建 `app/src/main/cpp` 文件夹，并写入 `CMakeLists.txt` 文件如下:

```cmake
cmake_minimum_required(VERSION 3.4.1)
add_library(translib SHARED IMPORTED GLOBAL)
set_target_properties(translib PROPERTIES IMPORT_LOCATION ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtranslib.so)
target_link_libraries(translib)
```

_注意:_ 这里的 `add_library` 中后面为 **IMPORTED GLOBAL**，和有源码的方式不一样。一定要包含 **GLOBAL** 字段，否则会报错。

然后使用 **Link C++ Project** 关联 `CMakeLists.txt`，如下图所示:

![](https://i.loli.net/2019/11/20/QofBARO57NJVh3i.png)

##### 2. 导入 `libtrans.so`

在刚才的 `CMakeLists.txt` 中其实已经标明了导入位置为 `{CMAKE_SOURCE_DIR}/libs/{ANDROID_ABI}/libtranslib.so`, 而 SO 的系统架构从前文得知为 `arm64-v8a`，因此只需建立 `app/src/main/libs/arm64-v8a` 文件夹并把 `libtranslib.so` 放入即可。

##### 3. 修改 gradle.script

完成上述两步后，编译依然报错。发现还要在 Gradle 脚本中标明 JNI 文件夹位置才行。修改 `app/build.gradle` 文件，在 `externalNativeBuild` 下方添加 `sourceSets` 代码:

```gradle
android {
    externalNativeBuild {
        cmake {
            path file('src/main/cpp/CMakeLists.txt')
        }
    }
    sourceSets {
        main {
            jniLibs.srcDirs 'src/main/libs'
        }
    }
}
```

并且在 `defaultConfig` 中添加如下代码:

```gradle
android {
    defaultConfig {
        ndk {
            abiFilters "arm64-v8a"
        }
    }
}
```

##### 4. 修改 Java 文件，引入 SO 文件

当上述步骤完成后，通过修改 `MainActivity.java`，引入 SO 文件并调用，并重新编译项目，就可以正常运行 APP 了。修改后的 Java 代码如下所示:

```java
public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("translib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onCalClick(View view) {
        TextView editText = findViewById(R.id.editText);
        String msg = editText.getText().toString().trim();
        TextView showText = findViewById(R.id.textView);
        showText.setText("cal: " + transMsg(msg));
    }

    public native String transMsg(String msg);
}
```

### 总结

本文简要介绍了怎样在没有源码的情况下，引入 SO 文件。成功引入并运行后，就可以对 APP 算法有一个直观了解，并为后期动态调试 SO 做好准备。
