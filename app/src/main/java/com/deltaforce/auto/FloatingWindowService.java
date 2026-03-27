package com.deltaforce.auto;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    
    private TextView tvStatus, tvRound, tvLoot;
    private Button btnStart, btnStop, btnClose;
    private Button btnRoute, btnEquip, btnMap, btnLog;
    
    private Handler handler;
    private Runnable updateRunnable;
    private int roundCount = 0;
    private int lootCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        createFloatingWindow();
        startStatusUpdate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createFloatingWindow() {
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 300;

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null);
        windowManager.addView(floatingView, params);

        initViews();
        setupButtons();
        setupDrag();
    }

    private void initViews() {
        tvStatus = floatingView.findViewById(R.id.tvFloatingStatus);
        tvRound = floatingView.findViewById(R.id.tvFloatingRound);
        tvLoot = floatingView.findViewById(R.id.tvFloatingLoot);
        
        btnStart = floatingView.findViewById(R.id.btnFloatingStart);
        btnStop = floatingView.findViewById(R.id.btnFloatingStop);
        btnClose = floatingView.findViewById(R.id.btnClose);
        
        btnRoute = floatingView.findViewById(R.id.btnFloatingRoute);
        btnEquip = floatingView.findViewById(R.id.btnFloatingEquip);
        btnMap = floatingView.findViewById(R.id.btnFloatingMap);
        btnLog = floatingView.findViewById(R.id.btnFloatingLog);
    }

    private void setupButtons() {
        // 开始按钮
        btnStart.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
                openMainActivity();
                return;
            }
            startBot();
        });

        // 停止按钮
        btnStop.setOnClickListener(v -> stopBot());

        // 关闭按钮
        btnClose.setOnClickListener(v -> {
            stopBot();
            stopSelf();
        });

        // 路线按钮 - 打开主界面
        btnRoute.setOnClickListener(v -> openMainActivity());

        // 装备按钮 - 打开主界面
        btnEquip.setOnClickListener(v -> openMainActivity());

        // 地图按钮 - 显示说明
        btnMap.setOnClickListener(v -> {
            Toast.makeText(this, "坐标说明:\nX: 横向位置(0-100)\nY: 纵向位置(0-100)\n50,50=屏幕中心", Toast.LENGTH_LONG).show();
        });

        // 日志按钮
        btnLog.setOnClickListener(v -> {
            Toast.makeText(this, "日志功能请在主界面查看", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDrag() {
        // 整个悬浮窗可拖动
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                            isDragging = true;
                        }
                        if (isDragging) {
                            params.x = initialX + deltaX;
                            params.y = initialY + deltaY;
                            try {
                                windowManager.updateViewLayout(floatingView, params);
                            } catch (Exception e) {
                                // Ignore
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void startBot() {
        roundCount = 0;
        updateStatus("启动中...", true);
        
        Intent intent = new Intent(this, AutoService.class);
        intent.setAction("START");
        startService(intent);
        
        Toast.makeText(this, "脚本已启动", Toast.LENGTH_SHORT).show();
    }

    private void stopBot() {
        updateStatus("已停止", false);
        
        Intent intent = new Intent(this, AutoService.class);
        intent.setAction("STOP");
        startService(intent);
        
        Toast.makeText(this, "脚本已停止", Toast.LENGTH_SHORT).show();
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private boolean isAccessibilityEnabled() {
        return AutoService.isRunning() || isAccessibilityServiceEnabled();
    }

    private boolean isAccessibilityServiceEnabled() {
        try {
            String enabledServices = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            return enabledServices != null && enabledServices.contains(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    private void startStatusUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateStatusFromService();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
    }

    private void updateStatusFromService() {
        if (AutoService.isRunning()) {
            updateStatus("运行中", true);
        } else {
            updateStatus("未运行", false);
        }
    }

    private void updateStatus(String status, boolean running) {
        if (floatingView != null && tvStatus != null) {
            tvStatus.setText("● " + status);
            if (running) {
                tvStatus.setTextColor(0xFF3fb950); // 绿色
            } else {
                tvStatus.setTextColor(0xFFf85149); // 红色
            }
            
            if (tvRound != null) {
                tvRound.setText(String.valueOf(roundCount));
            }
            if (tvLoot != null) {
                tvLoot.setText(String.valueOf(lootCount));
            }
        }
    }

    public void setRoundCount(int count) {
        this.roundCount = count;
        updateStatus("运行中", true);
    }

    public void setLootCount(int count) {
        this.lootCount = count;
        updateStatus("运行中", true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}