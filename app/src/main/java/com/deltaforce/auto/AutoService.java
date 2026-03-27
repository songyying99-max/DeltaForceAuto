package com.deltaforce.auto;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class AutoService extends AccessibilityService {

    private static final String TAG = "DeltaForceAuto";
    private static final String GAME_PACKAGE = "com.tencent.tmgp.dfm";
    
    private static AutoService instance;
    
    private Handler handler;
    private boolean isRunning = false;
    private int roundCount = 0;
    private int lootCount = 0;
    
    // 配置
    private String primaryWeapon = "步枪";
    private String armor = "防弹衣";
    private String helmet = "头盔";
    private List<RoutePoint> routePoints = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handler = new Handler(Looper.getMainLooper());
        loadConfig();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START".equals(action)) {
                loadConfig(); // 重新加载配置
                startAutoScript();
            } else if ("STOP".equals(action)) {
                stopAutoScript();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isRunning) return;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    @Override
    public void onDestroy() {
        stopAutoScript();
        instance = null;
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }

    public static boolean isRunning() {
        return instance != null && instance.isRunning;
    }

    private void loadConfig() {
        SharedPreferences prefs = getSharedPreferences("delta_force_config", MODE_PRIVATE);
        primaryWeapon = prefs.getString("primary_weapon", "步枪");
        armor = prefs.getString("armor", "防弹衣");
        helmet = prefs.getString("helmet", "头盔");
        
        // 解析路线
        routePoints.clear();
        String routeStr = prefs.getString("route", "起点,50,50,0\n物资点1,30,30,5\n物资点2,70,30,5\n撤离点,50,20,0");
        String[] lines = routeStr.split("\n");
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 4) {
                try {
                    routePoints.add(new RoutePoint(
                        parts[0].trim(),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim())
                    ));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed to parse route point: " + line);
                }
            }
        }
        
        Log.d(TAG, "Config loaded: " + primaryWeapon + ", route points: " + routePoints.size());
    }

    private void startAutoScript() {
        if (isRunning) return;
        
        isRunning = true;
        roundCount = 0;
        lootCount = 0;
        
        Log.d(TAG, "Auto script started");
        
        // 启动游戏
        launchGame();
        
        // 开始主循环
        handler.post(this::mainLoop);
    }

    private void stopAutoScript() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Auto script stopped");
    }

    private void launchGame() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
            Log.d(TAG, "Launching game...");
        }
    }

    private void mainLoop() {
        if (!isRunning) return;

        handler.postDelayed(() -> {
            try {
                roundCount++;
                lootCount = 0;
                Log.d(TAG, "=== Round " + roundCount + " ===");
                
                // 执行游戏流程
                executeGameFlow();
                
                // 继续下一轮
                if (isRunning) {
                    mainLoop();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in main loop: " + e.getMessage());
                if (isRunning) {
                    handler.postDelayed(this::mainLoop, 3000);
                }
            }
        }, 2000);
    }

    private void executeGameFlow() {
        // 1. 点击开始游戏
        if (clickByText("开始游戏")) {
            sleep(2000);
        }

        // 2. 选择装备
        selectLoadout();
        sleep(1000);

        // 3. 确认进入
        clickByText("确认");
        sleep(5000);

        // 4. 按路线执行
        executeRoute();

        // 5. 结算
        clickByText("继续");
        sleep(1000);
        clickByText("返回");
        sleep(2000);
    }

    private void selectLoadout() {
        Log.d(TAG, "Selecting loadout: " + primaryWeapon);
        
        // 选择主武器
        clickByText(primaryWeapon);
        sleep(500);
        
        // 选择防具
        clickByText(armor);
        sleep(500);
        
        // 选择头盔
        clickByText(helmet);
        sleep(500);
        
        // 确认装备
        clickByText("确认装备");
        sleep(1000);
    }

    private void executeRoute() {
        Log.d(TAG, "Executing route with " + routePoints.size() + " points");
        
        for (int i = 0; i < routePoints.size() && isRunning; i++) {
            RoutePoint point = routePoints.get(i);
            Log.d(TAG, "Going to: " + point.name);
            
            // 检查是否死亡
            if (checkGameOver()) {
                break;
            }
            
            // 移动到目标点
            moveToPosition(point.x, point.y);
            
            // 等待并搜索物资
            if (point.wait > 0) {
                Log.d(TAG, "Searching at " + point.name + " for " + point.wait + " seconds");
                searchAndLoot(point.wait * 1000);
            }
        }
        
        // 最后执行撤离
        if (isRunning && !checkGameOver()) {
            extract();
        }
    }

    private void moveToPosition(int xPercent, int yPercent) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        
        int targetX = screenWidth * xPercent / 100;
        int targetY = screenHeight * yPercent / 100;
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        int dx = targetX - centerX;
        int dy = targetY - centerY;
        
        // 旋转视角
        if (Math.abs(dx) > screenWidth * 0.3) {
            rotateView(dx > 0 ? 100 : -100);
        }
        
        // 移动（使用虚拟摇杆位置）
        int joystickX = 200;
        int joystickY = screenHeight - 300;
        int moveX = (int)(dx * 0.3);
        int moveY = (int)(dy * 0.3);
        
        swipe(joystickX, joystickY, joystickX + moveX, joystickY + moveY, 500);
        sleep(1000);
    }

    private void rotateView(int amount) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        
        int viewX = screenWidth - 100;
        int viewY = screenHeight / 2;
        
        swipe(viewX, viewY, viewX + amount, viewY, 200);
        sleep(500);
    }

    private void searchAndLoot(long duration) {
        long endTime = System.currentTimeMillis() + duration;
        
        while (System.currentTimeMillis() < endTime && isRunning) {
            // 查找拾取按钮
            if (clickByText("拾取")) {
                lootCount++;
                Log.d(TAG, "Looted item #" + lootCount);
                sleep(300);
                
                // 处理背包满
                if (findNodeByText("背包已满") != null) {
                    clickByText("确定");
                }
            }
            
            // 小范围移动搜索
            randomSmallMove();
            sleep(500);
        }
    }

    private void extract() {
        Log.d(TAG, "Extracting...");
        
        // 寻找撤离按钮
        if (clickByText("撤离")) {
            sleep(2000);
            clickByText("确认");
        }
        
        sleep(5000);
    }

    private boolean checkGameOver() {
        if (findNodeByText("你已阵亡") != null) {
            Log.d(TAG, "Character died");
            clickByText("继续");
            return true;
        }
        
        if (findNodeByText("结算") != null) {
            return true;
        }
        
        return false;
    }

    private void randomMove() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float startX = 200 + (float)(Math.random() * 100);
        float startY = screenHeight - 300 + (float)(Math.random() * 100);
        float endX = startX + (float)(Math.random() * 200 - 100);
        float endY = startY + (float)(Math.random() * 200 - 100);
        
        swipe((int)startX, (int)startY, (int)endX, (int)endY, 500);
    }

    private void randomSmallMove() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int jx = 200;
        int jy = screenHeight - 300;
        int dx = (int)((Math.random() - 0.5) * 50);
        int dy = (int)((Math.random() - 0.5) * 50);
        
        swipe(jx, jy, jx + dx, jy + dy, 300);
    }

    private void swipe(int startX, int startY, int endX, int endY, int duration) {
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        
        dispatchGesture(builder.build(), null, null);
    }

    private boolean clickByText(String text) {
        AccessibilityNodeInfo node = findNodeByText(text);
        if (node != null) {
            boolean result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            node.recycle();
            return result;
        }
        return false;
    }

    private AccessibilityNodeInfo findNodeByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        
        AccessibilityNodeInfo result = findNodeByTextRecursive(root, text);
        root.recycle();
        return result;
    }

    private AccessibilityNodeInfo findNodeByTextRecursive(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().contains(text)) {
            return AccessibilityNodeInfo.obtain(node);
        }
        
        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.toString().contains(text)) {
            return AccessibilityNodeInfo.obtain(node);
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByTextRecursive(child, text);
                child.recycle();
                if (result != null) return result;
            }
        }
        
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 路线点数据类
    private static class RoutePoint {
        String name;
        int x, y, wait;
        
        RoutePoint(String name, int x, int y, int wait) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.wait = wait;
        }
    }
}