package com.hyphenate.autoupdate;

import android.content.Context;

import org.json.JSONObject;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 */
public class UpdateEntity extends BaseEntity implements Serializable {
    /**
     * update : Yes
     * versionCode : 20
     * versionName : v2.0
     * isForce : 0
     * preBaselineCode : 0
     * appUrl : http://downloads.easemob.com/downloads/easemob_kefu_mobile_v1.9r2.apk
     * updateLog : xxx
     * delta : false
     * newMd5 : xxx
     * targetSize : 601132
     */

    private boolean hasUpdate = false;
    private int versionCode;
    private String versionName;
    private int isForce;
    private int preBaselineCode;
    private String appUrl;
    private String updateLog;
    private boolean delta;
    private String newMd5;
    private String targetSize;
    private int deltaVersion;
    private String deltaUrl;
    private String deltaSize;

    public boolean isHasUpdate() {
        return hasUpdate;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public boolean isDelta() {
        return delta;
    }

    public int getIsForce() {
        return isForce;
    }

    public String getNewMd5() {
        return newMd5;
    }

    public int getPreBaselineCode() {
        return preBaselineCode;
    }

    public String getTargetSize() {
        return targetSize;
    }

    public String getUpdateLog() {
        return updateLog;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getDeltaUrl() {
        return deltaUrl;
    }

    public int getDeltaVersion(){
        return deltaVersion;
    }

    public UpdateEntity(JSONObject jsonObject) {
        super(jsonObject);
        this.parseJson(jsonObject);
    }

    private void parseJson(JSONObject jsonObject) {
        try {
            this.hasUpdate = "Yes".equalsIgnoreCase(jsonObject.optString("update"));
            if (this.hasUpdate) {
                this.updateLog = jsonObject.getString("updateLog");
                this.versionCode = jsonObject.getInt("versionCode");
                this.versionName = jsonObject.getString("versionName");
                this.isForce = jsonObject.getInt("isForce");
                this.preBaselineCode = jsonObject.getInt("preBaselineCode");
                this.appUrl = jsonObject.getString("appUrl");
                this.newMd5 = jsonObject.getString("newMd5");
                this.targetSize = jsonObject.getString("targetSize");

                if (jsonObject.has("delta")){
                    this.delta = jsonObject.getBoolean("delta");
                }
                if (jsonObject.has("deltaVersion")){
                    this.deltaVersion = jsonObject.getInt("deltaVersion");
                }
                if (jsonObject.has("deltaUrl")){
                    this.deltaUrl = jsonObject.getString("deltaUrl");
                }
                if (jsonObject.has("deltaSize")){
                    this.deltaSize = jsonObject.getString("deltaSize");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getUpdateString(Context context, boolean fileIsDownloaded){
        String newVersionStr = context.getString(R.string.UMNewVersion);
        String targetSizeStr = context.getString(R.string.UMTargetSize);
        String var5 = context.getString(R.string.UMUpdateSize);
        String updateContentStr = context.getString(R.string.UMUpdateContent);
        String dialogInstallAPKStr = context.getString(R.string.UMDialog_InstallAPK);
        if (fileIsDownloaded){
            return String.format("%s %s\n%s\n\n%s\n%s\n", newVersionStr, this.versionName, dialogInstallAPKStr, updateContentStr, this.updateLog);
        }else if(this.delta && this.deltaVersion == CheckVersion.getInstance().getVersionCode(context)){
            return String.format("%s %s\n%s %s\n\n%s\n%s\n", newVersionStr, this.versionName, var5, formatSize(this.deltaSize), updateContentStr, this.updateLog);
        }else{
            return String.format("%s %s\n%s %s\n\n%s\n%s\n", newVersionStr, this.versionName, targetSizeStr, formatSize(this.targetSize), updateContentStr, this.updateLog);
        }
    }


    public static String formatSize(String targetSize) {
        String formatedString = "";
        long longSize = 0L;
        try {
            longSize = Long.valueOf(targetSize).longValue();
        } catch (NumberFormatException e) {
            return targetSize;
        }

        if (longSize < 1024L) {
            formatedString = (int) longSize + "B";
        } else {
            DecimalFormat decimalFormat;
            if (longSize < 1048576L) {
                decimalFormat = new DecimalFormat("#0.00");
                formatedString = decimalFormat.format((double) ((float) longSize) / 1024.0D) + "K";
            } else if (longSize < 1073741824L) {
                decimalFormat = new DecimalFormat("#0.00");
                formatedString = decimalFormat.format((double) ((float) longSize) / 1048576.0D) + "M";
            } else {
                decimalFormat = new DecimalFormat("#0.00");
                formatedString = decimalFormat.format((double) ((float) longSize) / 1.073741824E9D) + "G";
            }
        }

        return formatedString;
    }



}
