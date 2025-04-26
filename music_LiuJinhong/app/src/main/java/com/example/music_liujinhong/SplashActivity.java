package com.example.music_liujinhong;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.text.SpannableString;

import com.example.music_liujinhong.home.MainActivity;

public class SplashActivity extends AppCompatActivity {
    private View view;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_HAS_AGREED = "hasAgreedToTerms";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 使用自定义布局
        view = LayoutInflater.from(this).inflate(R.layout.dialog_terms, null);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 检查是否已经同意条款
                if (hasUserAgreed()) {
                    // 已同意则直接进入主页
                    proceedToMain();
                } else {
                    // 显示条款弹窗
                    showTermsDialog();
                }
            }
        }, 1000);
    }
    /**
     * 判断用户是否已经同意条例
     */
    private boolean hasUserAgreed() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_HAS_AGREED, false);
    }

    /**
     *保存用户条例状态
     */
    private void saveAgreement(boolean agreed) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_HAS_AGREED, agreed);
        editor.apply();
    }

    /**
     * 从闪屏页跳转到主页面
     */
    private void proceedToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /**
     * 定义条例弹窗的内容，并弹出
     */
    public void showTermsDialog() {
        // 使用AlertDialog.Builder构建弹窗口
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogTheme)
                .setCancelable(false);

        // 获取布局中的TextView
        TextView messageTextView = view.findViewById(R.id.welcome);

        // 创建一个SpannableString对象
        SpannableString spannableString = new SpannableString("欢迎使用音乐社区，我们将严格遵守相关法律和隐私政策保护您的个人隐私，请您阅读并同意《用户协议》与《隐私政策》。");

        // 设置《用户协议》的颜色为#3482FF，并添加点击事件
        ClickableSpan userAgreementSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // 跳转到浏览器打开用户协议链接
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mi.com"));
                startActivity(browserIntent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#3482FF")); // 设置颜色
                ds.setUnderlineText(false); // 去掉下划线
            }
        };
        int start = spannableString.toString().indexOf("《用户协议》");
        int end = start + "《用户协议》".length();
        spannableString.setSpan(userAgreementSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置《隐私政策》的颜色为透明度0.8的#3482FF，并添加点击事件
        ClickableSpan privacyPolicySpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // 跳转到浏览器打开隐私政策链接
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.xiaomiev.com/"));
                startActivity(browserIntent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.argb(204, 52, 130, 255)); // 设置透明度0.8的#3482FF
                ds.setUnderlineText(false); // 去掉下划线
            }
        };
        start = spannableString.toString().indexOf("《隐私政策》");
        end = start + "《隐私政策》".length();
        spannableString.setSpan(privacyPolicySpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置TextView的文本和点击事件
        messageTextView.setText(spannableString);
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance()); // 允许点击

        builder.setView(view);
        // 创建并显示弹窗
        AlertDialog dialog = builder.create();

//         设置“不同意”按钮
        TextView disagree = view.findViewById(R.id.disagree);
        disagree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // 先关闭弹窗
                // 退出应用
                finish();
            }
        });

        Button agree = view.findViewById(R.id.agree);
        agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // 先关闭弹窗
                saveAgreement(true); // 保存同意状态
                proceedToMain();     // 进入主界面
            }
        });
        //禁用点击外部关闭弹窗
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}