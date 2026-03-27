package com.deltaforce.auto;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvRound, tvLoot, tvLog;
    private EditText etPrimaryWeapon, etArmor, etHelmet, etRoute;
    private Button btnStart, btnStop, btnPermission, btnFloatingWindow;
    private Button btnSaveEquip, btnSaveRoute, btnLoadRoute;
    private Button btnPreset1, btnPreset2, btnPreset3, btnPreset4;
    private Button btnClearLog;
    
    private boolean isRunning = false;
    private SharedPreferences prefs;
    private StringBuilder logBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("delta_force_config", MODE_PRIVATE);
        
        initViews();
        setupListeners();
        loadConfig();
        updateStatus();
        
        addLog("应用已启动");
    }

    private void initViews() {
        // 状态
        tvStatus = findViewById(R.id.tvStatus);
        tvRound = findViewById(R.id.tvRound);
        tvLoot = findViewById(R.id.tvLoot);
        tvLog = findViewById(R.id.tvLog);
        
        // 装备配置
        etPrimaryWeapon = findViewById(R.id.etPrimaryWeapon);
        etArmor = findViewById(R.id.etArmor);
        etHelmet = findViewById(R.id.etHelmet);
        
        // 路线配置
        etRoute = findViewById(R.id.etRoute);
        
        // 按钮
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnPermission = findViewById(R.id.btnPermission);
        btnFloatingWindow = findViewById(R.id.btnFloatingWindow);
        btnSaveEquip = findViewById(R.id.btnSaveEquip);
        btnSaveRoute = findViewById(R.id.btnSaveRoute);
        btnLoadRoute = findViewById(R.id.btnLoadRoute);
        btnPreset1 = findViewById(R.id.btnPreset1);
        btnPreset2 = findViewById(R.id.btnPreset2);
        btnPreset3 = findViewById(R.id.btnPreset3);
        btnPreset4 = findViewById(R.id.btnPreset4);
        btnClearLog = findViewById(R.id.btnClearLog);
    }

    private void setupListeners() {
        btnStart.setOnClickListener(v -> startBot());
        btnStop.setOnClickListener(v -> stopBot());
        btnPermission.setOnClickListener(v -> openAccessibilitySettings());
        btnFloatingWindow.setOnClickListener(v -> showFloatingWindow());
        
        btnSaveEquip.setOnClickListener(v -> saveEquipConfig());
        btnSaveRoute.setOnClickListener(v -> saveRouteConfig());
        btnLoadRoute.setOnClickListener(v -> loadRouteConfig());
        
        btnPreset1.setOnClickListener(v -> loadPreset("factory"));
        btnPreset2.setOnClickListener(v -> loadPreset("forest"));
        btnPreset3.setOnClickListener(v -> loadPreset("city"));
        btnPreset4.setOnClickListener(v -> loadPreset("mountain"));
        
        btnClearLog.setOnClickListener(v -> clearLog());
    }

    private void startBot() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
            openAccessibilitySettings();
            return;
        }

        // 保存当前配置
        saveConfig();

        Intent intent = new Intent(this, AutoService.class);
        intent.setAction("START");
        startService(intent);

        isRunning = true;
        updateStatus();
        addLog("脚本已启动");
        Toast.makeText(this, "脚本已启动，请切换到游戏", Toast.LENGTH_LONG).show();
    }

    private void stopBot() {
        Intent intent = new Intent(this, AutoService.class);
        intent.setAction("STOP");
        startService(intent);

        isRunning = false;
        updateStatus();
        addLog("脚本已停止");
    }

    private void showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 100);
                Toast.makeText(this, "请允许悬浮窗权限", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent intent = new Intent(this, FloatingWindowService.class);
        startService(intent);
        
        addLog("悬浮窗已显示");
        Toast.makeText(this, "悬浮窗已显示", Toast.LENGTH_SHORT).show();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "请找到\"三角洲行动助手\"并开启", Toast.LENGTH_LONG).show();
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            for (AccessibilityServiceInfo info : am.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (info.getId().contains(getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void saveEquipConfig() {
        prefs.edit()
                .putString("primary_weapon", etPrimaryWeapon.getText().toString())
                .putString("armor", etArmor.getText().toString())
                .putString("helmet", etHelmet.getText().toString())
                .apply();
        addLog("装备配置已保存");
        Toast.makeText(this, "装备配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void saveRouteConfig() {
        prefs.edit()
                .putString("route", etRoute.getText().toString())
                .apply();
        addLog("路线配置已保存");
        Toast.makeText(this, "路线配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void loadRouteConfig() {
        String route = prefs.getString("route", "");
        etRoute.setText(route);
        addLog("路线配置已加载");
    }

    private void loadPreset(String type) {
        String preset = "";
        switch (type) {
            case "factory":
                preset = "出生点,50,80,0\n仓库,25,60,6\n车间,50,40,6\n撤离点,50,15,0";
                break;
            case "forest":
                preset = "营地,50,85,0\n伐木场,30,60,6\n木屋,60,40,6\n撤离点,70,15,0";
                break;
            case "city":
                preset = "郊区,20,80,0\n商业区,40,55,6\n住宅区,65,55,6\n撤离点,50,10,0";
                break;
            case "mountain":
                preset = "山脚,50,90,0\n矿洞,30,60,6\n山腰,50,40,6\n撤离点,50,10,0";
                break;
        }
        etRoute.setText(preset);
        addLog("已加载预设路线");
        Toast.makeText(this, "已加载预设路线", Toast.LENGTH_SHORT).show();
    }

    private void saveConfig() {
        prefs.edit()
                .putString("primary_weapon", etPrimaryWeapon.getText().toString())
                .putString("armor", etArmor.getText().toString())
                .putString("helmet", etHelmet.getText().toString())
                .putString("route", etRoute.getText().toString())
                .apply();
    }

    private void loadConfig() {
        etPrimaryWeapon.setText(prefs.getString("primary_weapon", "步枪"));
        etArmor.setText(prefs.getString("armor", "防弹衣"));
        etHelmet.setText(prefs.getString("helmet", "头盔"));
        
        String defaultRoute = "起点,50,50,0\n物资点1,30,30,5\n物资点2,70,30,5\n物资点3,50,70,5\n撤离点,50,20,0";
        etRoute.setText(prefs.getString("route", defaultRoute));
    }

    private void updateStatus() {
        if (isRunning) {
            tvStatus.setText("状态: 运行中");
            tvStatus.setTextColor(0xFF3fb950);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        } else {
            tvStatus.setText("状态: 未运行");
            tvStatus.setTextColor(0xFFf85149);
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    public void updateStats(int round, int loot) {
        runOnUiThread(() -> {
            tvRound.setText("轮次: " + round);
            tvLoot.setText("拾取: " + loot);
        });
    }

    public void addLog(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        logBuilder.insert(0, "[" + time + "] " + message + "\n");
        if (logBuilder.length() > 5000) {
            logBuilder.setLength(5000);
        }
        if (tvLog != null) {
            tvLog.setText(logBuilder.toString());
        }
    }

    private void clearLog() {
        logBuilder.setLength(0);
        tvLog.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}