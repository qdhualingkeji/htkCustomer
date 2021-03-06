package com.hl.htk_customer.activity;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.hl.htk_customer.R;
import com.hl.htk_customer.base.BaseActivity;
import com.hl.htk_customer.entity.RefreshInfoEntity;
import com.hl.htk_customer.model.CommonMsg;
import com.hl.htk_customer.model.UserInfoManager;
import com.hl.htk_customer.receiver.SMSReceiver;
import com.hl.htk_customer.utils.AsynClient;
import com.hl.htk_customer.utils.GsonHttpResponseHandler;
import com.hl.htk_customer.utils.MyApplication;
import com.hl.htk_customer.utils.MyHttpConfing;
import com.hl.htk_customer.utils.UiFormat;
import com.loopj.android.http.RequestParams;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.jpush.android.api.JPushInterface;

/**
 * Created by Administrator on 2017/7/12.
 */

public class BindPhoneActivity extends BaseActivity implements View.OnClickListener {

    @Bind(R.id.ll_return)
    LinearLayout llReturn;
    @Bind(R.id.tv_title)
    TextView tvTitle;
    @Bind(R.id.et_phoneNumber)
    EditText etPhoneNumber;
    @Bind(R.id.tv_getAuthCode)
    TextView tvGetAuthCode;
    @Bind(R.id.et_authCode)
    EditText etAuthCode;
    @Bind(R.id.btn_login)
    TextView btnLogin;


    private static final int JPUSH_ONE = 10000;

    private TimeCount time;
    private String phoneNumber = ""; //手机号
    private String authCode = "";  //验证码
    private String token = "";
    private String headUrl = "";
    private String name = "";

    private SMSReceiver smeReceiver;
    private IntentFilter filter;
    private int tag = -1;//0wx  1qq

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_login_auth);
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        initWidget();
    }


    private void initWidget() {

        tag = getIntent().getIntExtra("tag", -1);

        token = getIntent().getStringExtra("token");
        headUrl = getIntent().getStringExtra("headUrl");
        name = getIntent().getStringExtra("name");




        smeReceiver = new SMSReceiver(etAuthCode);
        filter = new IntentFilter(SMSReceiver.SMS_RECEIVED_ACTION);
        this.registerReceiver(smeReceiver, filter);

        tvTitle.setText(getResources().getText(R.string.bind_phoneNumnber));
        btnLogin.setText(getResources().getText(R.string.bind));
        llReturn.setOnClickListener(this);
        tvGetAuthCode.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.ll_return:
                finish();
                break;
            case R.id.tv_getAuthCode:

                phoneNumber = etPhoneNumber.getText().toString().trim();

                if (phoneNumber.length() != 11) {
                    showMessage("请输入正确格式的手机号");
                    return;
                }
                etAuthCode.requestFocus(); //获取焦点
                etAuthCode.setText("");
                etAuthCode.setHint("等待自动填充");
                getAuthCode(phoneNumber);
                startTimeCount();

                break;
            case R.id.btn_login:

                phoneNumber = etPhoneNumber.getText().toString().trim();
                authCode = etAuthCode.getText().toString().trim();

                if (phoneNumber.length() != 11) {
                    showMessage("请输入正确格式的手机号");
                    return;
                } else if (TextUtils.isEmpty(authCode)) {
                    showMessage("请输入验证码");
                    return;
                }
                btnLogin.setEnabled(false);

                if(tag == 0){
                    //wx
                    bind();
                }else  if(tag == 1){
                    bindQq();
                }

                break;

        }

    }


    private void getAuthCode(String phoneNumber) {

        RequestParams params = AsynClient.getRequestParams();
        params.put("phone", Long.parseLong(phoneNumber));
        AsynClient.get(MyHttpConfing.getAuth + phoneNumber, this, params, new GsonHttpResponseHandler() {
            @Override
            protected Object parseResponse(String rawJsonData) throws Throwable {
                return null;
            }

            @Override
            public void onFailure(int statusCode, String rawJsonData, Object errorResponse) {
                UiFormat.tryRequest(rawJsonData);
            }

            @Override
            public void onSuccess(int statusCode, String rawJsonResponse, Object response) {
                CommonMsg cErrorMsg = UiFormat.getCommonMsg(rawJsonResponse);
                showMessage(cErrorMsg.getMessage());
            }
        });

    }


    private void startTimeCount() {
        //获取验证码
        tvGetAuthCode.setEnabled(false);
        long endTime = 60 * 1000;
        time = new TimeCount(endTime, 1000);
        time.start();
    }


    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {//计时完毕时触发
            tvGetAuthCode.setEnabled(true);
            tvGetAuthCode.setText("重新验证");
        }

        @Override
        public void onTick(long millisUntilFinished) {//计时过程显示
            tvGetAuthCode.setEnabled(false);
            tvGetAuthCode.setText(millisUntilFinished / 1000 + "s");
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (smeReceiver != null) {
            this.unregisterReceiver(smeReceiver);
        }

        //取消计时
        if (time != null)
            time.cancel();
    }


    private void bind() {
        showChangeDialog("绑定微信");

        RequestParams params = AsynClient.getRequestParams();
        params.put("weChatToken", token);
        params.put("phone", phoneNumber);
        params.put("code", authCode);
        params.put("nickName", name);
        params.put("avatarUrl", headUrl);
        AsynClient.post(MyHttpConfing.bindPhoneNumber, this, params, new GsonHttpResponseHandler() {
            @Override
            protected Object parseResponse(String rawJsonData) throws Throwable {
                return null;
            }

            @Override
            public void onFailure(int statusCode, String rawJsonData, Object errorResponse) {
                UiFormat.tryRequest(rawJsonData);
                btnLogin.setEnabled(true);
                hideChangeDialog();
            }

            @Override
            public void onSuccess(int statusCode, String rawJsonResponse, Object response) {
                Log.i(TAG , rawJsonResponse);
                hideChangeDialog();
                Gson gson = new Gson();
                CommonMsg commonMsg = gson.fromJson(rawJsonResponse, CommonMsg.class);

                if (commonMsg.getCode() == 100) {

                    if (commonMsg.getData() != null){
                        //登录完毕
                        initLoginData(commonMsg , 0);
                        finish();
                    }else {
                        btnLogin.setEnabled(true);
                        showMessage(commonMsg.getMessage());
                    }

                } else {
                    btnLogin.setEnabled(true);
                    showMessage(commonMsg.getMessage());
                }

            }
        });


    }



    private void bindQq() {
        showChangeDialog("绑定QQ");

        RequestParams params = AsynClient.getRequestParams();
        params.put("qqToken", token);
        params.put("phone", phoneNumber);
        params.put("code", authCode);
        params.put("nickName", name);
        params.put("avatarUrl", headUrl);
        AsynClient.post(MyHttpConfing.bindQq, this, params, new GsonHttpResponseHandler() {
            @Override
            protected Object parseResponse(String rawJsonData) throws Throwable {
                return null;
            }

            @Override
            public void onFailure(int statusCode, String rawJsonData, Object errorResponse) {
                UiFormat.tryRequest(rawJsonData);
                btnLogin.setEnabled(true);
                hideChangeDialog();
            }

            @Override
            public void onSuccess(int statusCode, String rawJsonResponse, Object response) {
                Log.i(TAG, rawJsonResponse);
                hideChangeDialog();
                Gson gson = new Gson();
                CommonMsg commonMsg = gson.fromJson(rawJsonResponse, CommonMsg.class);

                if (commonMsg.getCode() == 100) {

                    if (commonMsg.getData() != null){
                        initLoginData(commonMsg , 1);
                        finish();
                    }else {
                        btnLogin.setEnabled(true);
                        showMessage(commonMsg.getMessage());
                    }

                } else {
                    btnLogin.setEnabled(true);
                    showMessage(commonMsg.getMessage());
                }

            }
        });


    }

    /**
     *
     * @param i 0wx  1qq
     */
    private void initLoginData(CommonMsg commonMsg , int i) {
        UserInfoManager userInfoManager = new UserInfoManager(mContext);
        userInfoManager.setISLOGIN(true);
        userInfoManager.setToken(commonMsg.getData().toString());
        if (i == 0){
            userInfoManager.setWeChatStatus(true);
        }else {
            userInfoManager.setQqStatus(true);
        }


        //发送信息，刷新我的界面数据
        EventBus.getDefault().post(new RefreshInfoEntity(true));

        //登录完毕,设置JPUSH的Alias识别
        String alias = commonMsg.getData().toString().replaceAll("-", "");
        alias = alias.trim();
        Log.i(TAG, "onSuccess: " + alias);
        JPushInterface.setAlias(MyApplication.getContext() , JPUSH_ONE  , alias);
    }



    private void finishHome() {

        List<Activity> activities = MyApplication.get();
        for (int i = 0; i < activities.size(); i++) {

            Activity activity = activities.get(i);
            if (activity instanceof HomeActivity) {
                if (activity != null) {
                    MyApplication.activities.remove(i);
                    activity.finish();
                    break;
                }
            }
        }

    }


}
