package com.hyphenate.autoupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

import okhttp3.Call;

/**
 * app 更新
 * Created by liyuzhao on 16/7/25.
 */
public class CheckVersion {

    private int mAppVersionCode = 0;
    private WeakReference<Context> mWrContext;
    private ProgressDialog mAlertDialog;
    private String checkUrl = "";
    private static CheckVersion instance;
    private AlertDialog dialog;

    public static CheckVersion getInstance() {
        if (instance == null) {
            synchronized (CheckVersion.class) {
                if (instance == null) {
                    instance = new CheckVersion();
                }
            }
        }
        return instance;
    }

    public void setUpdateUrl(String updateUrl) {
        this.checkUrl = updateUrl;
    }

    private UpdateEntity mUpdateEntity;

    public void update(Context context) {
        update(context, false);
    }

    public void update(final Context context, final boolean isEnforceCheck) {
        mWrContext = new WeakReference<Context>(context);
        mAppVersionCode = getVersionCode(context);

        if (TextUtils.isEmpty(checkUrl)) {
            Toast.makeText(context, "url不能为空,请设置URL", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpUtils.get().url(checkUrl).build().execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        if (isEnforceCheck) {
                            Toast.makeText(context, context.getString(R.string.current_is_last_version), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onResponse(String response) {
                        loadOnlineData(response, isEnforceCheck);
                    }
                });
            }
        }).start();
    }

    private void loadOnlineData(String json, boolean isEnforceCheck) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            mUpdateEntity = new UpdateEntity(jsonObject);
            if (mAppVersionCode < mUpdateEntity.getVersionCode()) {
                //启动更新
                AlertUpdate();
            } else {
                if (isEnforceCheck) {
                    if (mWrContext != null && mWrContext.get() != null)
                        Toast.makeText(mWrContext.get(), mWrContext.get().getString(R.string.current_is_last_version), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (JSONException e) {
            Log.e("update", "" + e.getMessage());
            if (mWrContext != null && mWrContext.get() != null)
                Toast.makeText(mWrContext.get(), mWrContext.get().getString(R.string.current_is_last_version), Toast.LENGTH_SHORT).show();
        }


    }

    private void updateApp() {
        updateApp(false);
    }

    private boolean fileExists() {
//        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File filePath = mWrContext.get().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        String fileName = getPackageName(mWrContext.get()) + mUpdateEntity.getVersionName() + ".apk";
        File file = new File(filePath, fileName);
        return file.exists();
    }


    private void patchAppStart() {
        if (mWrContext.get() instanceof Activity) {
            ((Activity) mWrContext.get()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mAlertDialog == null) {
                        mAlertDialog = new ProgressDialog(mWrContext.get());
                    }
                    mAlertDialog.setTitle(mWrContext.get().getString(R.string.UMToast_IsUpdating));
                    mAlertDialog.setMessage("正在合并中...");
                    mAlertDialog.setCancelable(false);
                    mAlertDialog.setCanceledOnTouchOutside(false);
                    mAlertDialog.setIndeterminate(true);
                    mAlertDialog.show();

                }
            });
        }

    }

    private void patchAppEnd() {
        if (mWrContext.get() instanceof Activity) {
            ((Activity) mWrContext.get()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mAlertDialog != null && mAlertDialog.isShowing()) {
                        mAlertDialog.dismiss();
                    }
                }
            });
        }

    }


    private void updateApp(boolean isEnforceDown) {
        final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String fileName = getPackageName(mWrContext.get()) + "-" + mUpdateEntity.getVersionName() + ".apk";

        if (!isEnforceDown) {
            File file = new File(filePath, fileName);
            if (file.exists()) {
                install(file);
                return;
            }
        }

        mAlertDialog = new ProgressDialog(mWrContext.get());
        mAlertDialog.setTitle(mWrContext.get().getString(R.string.UMAppUpdate));
        mAlertDialog.setMessage(mWrContext.get().getString(R.string.UMToast_IsUpdating));
        mAlertDialog.setCancelable(false);
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.setIndeterminate(true);
        mAlertDialog.show();

        OkHttpUtils.get().url(mUpdateEntity.getAppUrl()).build().execute(new FileCallBack(filePath, fileName) {
            @Override
            public void inProgress(float progress, long total) {
                mAlertDialog.setMessage("当前下载进度:" + (int) (100 * progress) + "%");
            }

            @Override
            public void onError(Call call, Exception e) {
                //下载失败,是否重试
                resetAlert();
            }

            @Override
            public void onResponse(final File file) {
                //下载成功,开始安装
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        install(file);
                    }
                });

            }

            @Override
            public void onAfter() {
                mAlertDialog.dismiss();
            }
        });

    }

    private Handler mHandler = new Handler();


    private void install(File file) {
        if (!checkMD5(file)) {
            md5Alert();
            return;
        }
        if (mWrContext.get() == null){
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            Uri apkUri = FileProvider.getUriForFile(mWrContext.get(), mWrContext.get().getPackageName() + ".autoupdate", file);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            mWrContext.get().startActivity(install);
        }else{
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mWrContext.get().startActivity(intent);
        }

    }

    private void md5Alert() {
        if (mWrContext == null || mWrContext.get() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mWrContext.get());
        builder.setTitle(mWrContext.get().getString(R.string.UMAppUpdate));
        builder.setMessage(mWrContext.get().getString(R.string.version_error));
        builder.setPositiveButton(mWrContext.get().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateApp(true);
            }
        });
        builder.setNegativeButton(mWrContext.get().getString(R.string.cancel), null);
        builder.show();
    }

    private void resetAlert() {
        if (mWrContext == null || mWrContext.get() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mWrContext.get());
        builder.setTitle(mWrContext.get().getString(R.string.UMAppUpdate));
        builder.setMessage(mWrContext.get().getString(R.string.download_failed));
        builder.setPositiveButton(mWrContext.get().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateApp();
            }
        });
        builder.setNegativeButton(mWrContext.get().getString(R.string.cancel), null);
        builder.show();
    }


    private boolean checkMD5(File file) {
        if (mUpdateEntity.getNewMd5().equals("00000000")) {
            return true;
        }
        String md5Value;
        try {
            md5Value = getMd5ByFile(file);
        } catch (FileNotFoundException e) {
            md5Value = "-1";
        }
        Log.d("md5:", md5Value);
        return md5Value.equals(mUpdateEntity.getNewMd5());
    }

    private String getMd5ByFile(File file) throws FileNotFoundException {
        String value = "";
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }


    /**
     * 获得apk版本号
     *
     * @param context
     * @return
     */
    int getVersionCode(Context context) {
        int versionCode = 0;
        PackageInfo packageInfo = getPackInfo(context);
        if (packageInfo != null) {
            versionCode = packageInfo.versionCode;
        }
        return versionCode;
    }


    /**
     * 获取apkPackageName
     *
     * @param context
     * @return
     */
    public String getPackageName(Context context) {
        String packName = "";
        PackageInfo packInfo = getPackInfo(context);
        if (packInfo != null) {
            packName = packInfo.packageName;
        }
        return packName;
    }


    /**
     * 获得apkinfo
     *
     * @param context
     * @return
     */
    private PackageInfo getPackInfo(Context context) {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名, 0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packInfo;
    }

    private void AlertUpdate() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mWrContext.get(), R.style.MyDialogStyle);
        View dialogView = LayoutInflater.from(mWrContext.get()).inflate(R.layout.update_dialog, null);
        TextView tvContent = (TextView) dialogView.findViewById(R.id.update_content);
        Button updateCancel = (Button) dialogView.findViewById(R.id.update_id_cancel);
        Button updateOk = (Button) dialogView.findViewById(R.id.update_id_ok);
        tvContent.setText(mUpdateEntity.getUpdateString(mWrContext.get(), fileExists()));
        builder.setView(dialogView);
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        if (mUpdateEntity.getIsForce() == 0) {
            if (mAppVersionCode < mUpdateEntity.getPreBaselineCode()) {
                updateCancel.setVisibility(View.GONE);
            } else {
                updateCancel.setVisibility(View.VISIBLE);
            }
        } else {
            updateCancel.setVisibility(View.GONE);
        }
        updateCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        updateOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                updateApp();
            }
        });
        dialog.show();
    }


    public void onDestory() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }
}
