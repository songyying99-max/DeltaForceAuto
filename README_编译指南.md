# 三角洲行动助手 - APK编译指南

## 方法一：GitHub Actions 自动编译（最简单，推荐）

### 步骤：

**第1步：注册GitHub账号**
- 访问 https://github.com 注册账号

**第2步：创建仓库**
- 点击右上角 "+" → "New repository"
- 仓库名输入：`DeltaForceAuto`
- 选择 "Public"
- 点击 "Create repository"

**第3步：上传项目文件**
- 方式A：使用GitHub网页上传
  1. 点击 "uploading an existing file"
  2. 把 `DeltaForceAuto` 文件夹内所有文件拖进去
  3. 点击 "Commit changes"

- 方式B：使用Git命令（需要安装Git）
  ```bash
  cd DeltaForceAuto
  git init
  git add .
  git commit -m "Initial commit"
  git branch -M main
  git remote add origin https://github.com/你的用户名/DeltaForceAuto.git
  git push -u origin main
  ```

**第4步：等待自动编译**
- 上传完成后，GitHub会自动开始编译
- 点击仓库顶部的 "Actions" 标签查看进度
- 等待编译完成（约3-5分钟）

**第5步：下载APK**
- 编译完成后，点击编译任务
- 在页面底部 "Artifacts" 区域点击 "delta-force-auto-apk" 下载
- 解压后得到 `app-debug.apk`

**第6步：安装到手机**
- 把APK传到手机
- 允许安装未知来源应用
- 安装APK


## 方法二：在线编译服务

### 使用 Appetize.io
1. 访问 https://appetize.io
2. 上传项目文件
3. 在线编译和测试

### 使用 Bitrise
1. 访问 https://bitrise.io
2. 连接GitHub仓库
3. 配置自动编译


## 方法三：本地编译（需要Android Studio）

### 第1步：安装Android Studio
- 下载：https://developer.android.com/studio
- 安装时选择标准配置

### 第2步：打开项目
- 启动Android Studio
- File → Open → 选择 `DeltaForceAuto` 文件夹
- 等待Gradle同步完成

### 第3步：编译APK
- Build → Build Bundle(s) / APK(s) → Build APK(s)
- 等待编译完成
- 点击通知中的 "locate" 找到APK

### APK位置
```
DeltaForceAuto/app/build/outputs/apk/debug/app-debug.apk
```


## 文件夹结构说明

```
DeltaForceAuto/
├── .github/workflows/build.yml   ← GitHub Actions配置
├── app/
│   ├── src/main/
│   │   ├── java/com/deltaforce/auto/
│   │   │   ├── MainActivity.java
│   │   │   ├── AutoService.java
│   │   │   └── FloatingWindowService.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── xml/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── gradlew
└── gradle.properties
```


## 常见问题

### Q: GitHub上传时提示文件太大
A: 删除以下文件夹后再上传：
- `.gradle`
- `build`
- `app/build`

### Q: Actions编译失败
A: 检查以下内容：
1. 是否上传了所有必要文件
2. build.gradle语法是否正确
3. 查看Actions日志中的错误信息

### Q: 下载的APK无法安装
A: 
1. 确保手机系统版本 >= Android 7.0
2. 在设置中允许安装未知来源应用
3. 检查APK文件是否完整

### Q: 如何重新编译
A: 
- 修改代码后提交到GitHub
- Actions会自动重新编译
- 或在Actions页面点击 "Run workflow" 手动触发


## 联系方式

如有问题，请提供：
1. 错误截图
2. Actions编译日志
3. 手机型号和系统版本