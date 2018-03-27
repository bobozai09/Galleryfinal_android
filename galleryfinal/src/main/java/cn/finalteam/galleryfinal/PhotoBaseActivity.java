/*
 * Copyright (C) 2014 pengjianbo(pengjianbosoft@gmail.com), Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package cn.finalteam.galleryfinal;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.finalteam.galleryfinal.model.PhotoInfo;
import cn.finalteam.galleryfinal.permission.EasyPermissions;
import cn.finalteam.galleryfinal.utils.ILogger;
import cn.finalteam.galleryfinal.utils.MediaScanner;
import cn.finalteam.galleryfinal.utils.Utils;
import cn.finalteam.toolsfinal.ActivityManager;
import cn.finalteam.toolsfinal.DateUtils;
import cn.finalteam.toolsfinal.DeviceUtils;
import cn.finalteam.toolsfinal.StringUtils;
import cn.finalteam.toolsfinal.io.FileUtils;

import static cn.finalteam.galleryfinal.GalleryFinal.TAKE_REQUEST_CODE;

/**
 * Desction:
 * Author:pengjianbo
 * Date:15/10/10 下午5:46
 */
public abstract class PhotoBaseActivity extends Activity implements EasyPermissions.PermissionCallbacks {

    protected static String mPhotoTargetFolder;

    private Uri mTakePhotoUri;
    private MediaScanner mMediaScanner;

    protected int mScreenWidth = 720;
    protected int mScreenHeight = 1280;

    protected boolean mTakePhotoAction;//打开相机动作

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("takePhotoUri", mTakePhotoUri);
        outState.putString("photoTargetFolder", mPhotoTargetFolder);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTakePhotoUri = savedInstanceState.getParcelable("takePhotoUri");
        mPhotoTargetFolder = savedInstanceState.getString("photoTargetFolder");
    }

    protected Handler mFinishHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            finishGalleryFinalPage();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        ActivityManager.getActivityManager().addActivity(this);
        mMediaScanner = new MediaScanner(this);
        DisplayMetrics dm = DeviceUtils.getScreenPix(this);
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;

        // TODO add by JQ.Hu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            if (GalleryFinal.getCoreConfig() != null)
                tintManager.setStatusBarTintColor(GalleryFinal.getCoreConfig().getThemeConfig().getTitleBarBgColor());//通知栏所需颜色
        }

    }

    @TargetApi(19)
    public void setTranslucentStatus(boolean on) {// TODO add by JQ.Hu
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaScanner != null) {
            mMediaScanner.unScanFile();
        }
        ActivityManager.getActivityManager().finishActivity(this);
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void requestCameraPermission() {
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            EasyPermissions.requestPermissions(this, "使用手机相机拍照",
                    GalleryFinal.PERMISSIONS_CODE_CAMERA, Manifest.permission.CAMERA);
        }
    }

    /**
     * 拍照
     */
    protected void takePhotoAction() {
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            requestCameraPermission();
            return;
        }

        if (!DeviceUtils.existSDCard()) {
            String errormsg = getString(R.string.empty_sdcard);
            toast(errormsg);
            if (mTakePhotoAction) {
                resultFailure(errormsg, true);
            }
            return;
        }

        File takePhotoFolder = null;
        if (StringUtils.isEmpty(mPhotoTargetFolder)) {
            takePhotoFolder = GalleryFinal.getCoreConfig().getTakePhotoFolder();
        } else {
            takePhotoFolder = new File(mPhotoTargetFolder);
        }
        boolean suc = FileUtils.mkdirs(takePhotoFolder);
        File toFile = new File(takePhotoFolder, "IMG" + DateUtils.format(new Date(), "yyyyMMddHHmmss") + ".jpg");

        ILogger.d("create folder=" + toFile.getAbsolutePath());
        if (suc) {
            mTakePhotoUri = Uri.fromFile(toFile);
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mTakePhotoUri);
//            startActivityForResult(captureIntent, TAKE_REQUEST_CODE);
            if (android.os.Build.VERSION.SDK_INT < 24) {// TODO: Modify by hutuge  适配7.0系统，否则崩溃
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mTakePhotoUri);
                startActivityForResult(captureIntent, TAKE_REQUEST_CODE);
            } else {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(MediaStore.Images.Media.DATA, toFile.getAbsolutePath());
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(captureIntent, TAKE_REQUEST_CODE);
            }
        } else {
            takePhotoFailure();
            ILogger.e("create file failure");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && mTakePhotoUri != null) {
                final String path = mTakePhotoUri.getPath();
                if (new File(path).exists()) {
                    final PhotoInfo info = new PhotoInfo();
                    info.setPhotoId(Utils.getRandom(10000, 99999));
                    info.setPhotoPath(path);
                    updateGallery(path);
                    takeResult(info);
                } else {
                    takePhotoFailure();
                }
            } else {
                takePhotoFailure();
            }
        }
    }

    private void takePhotoFailure() {
        String errormsg = getString(R.string.take_photo_fail);
        if (mTakePhotoAction) {
            resultFailure(errormsg, true);
        } else {
            toast(errormsg);
        }
    }

    /**
     * 更新相册
     */
    private void updateGallery(String filePath) {
        if (mMediaScanner != null) {
            mMediaScanner.scanFile(filePath, "image/jpeg");
        }
    }

    protected void resultData(ArrayList<PhotoInfo> photoList) {
        GalleryFinal.OnHanlderResultCallback callback = GalleryFinal.getCallback();
        int requestCode = GalleryFinal.getRequestCode();
        if (callback != null) {
            if (photoList != null && photoList.size() > 0) {
                callback.onHanlderSuccess(requestCode, photoList);
            } else {
                callback.onHanlderFailure(requestCode, getString(R.string.photo_list_empty));
            }
        }
        finishGalleryFinalPage();
    }

    protected void resultFailureDelayed(String errormsg, boolean delayFinish) {
        GalleryFinal.OnHanlderResultCallback callback = GalleryFinal.getCallback();
        int requestCode = GalleryFinal.getRequestCode();
        if (callback != null) {
            callback.onHanlderFailure(requestCode, errormsg);
        }
        if (delayFinish) {
            mFinishHanlder.sendEmptyMessageDelayed(0, 500);
        } else {
            finishGalleryFinalPage();
        }
    }

    protected void resultFailure(String errormsg, boolean delayFinish) {
        GalleryFinal.OnHanlderResultCallback callback = GalleryFinal.getCallback();
        int requestCode = GalleryFinal.getRequestCode();
        if (callback != null) {
            callback.onHanlderFailure(requestCode, errormsg);
        }
        if (delayFinish) {
            finishGalleryFinalPage();
        } else {
            finishGalleryFinalPage();
        }
    }

    private void finishGalleryFinalPage() {
        ActivityManager.getActivityManager().finishActivity(PhotoEditActivity.class);
        ActivityManager.getActivityManager().finishActivity(PhotoSelectActivity.class);
        Global.mPhotoSelectActivity = null;
        System.gc();
    }

    protected abstract void takeResult(PhotoInfo info);

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(List<String> list) {
        if (list.contains(Manifest.permission.CAMERA) && mTakePhotoAction) {
            takePhotoAction();
        }
    }

    @Override
    public void onPermissionsDenied(List<String> list) {
    }
}
