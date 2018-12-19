package com.example.tanph.shumusic.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.service.MediaPlayerService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionRequestActivity extends AppCompatActivity {

    private static final int MULTIPLE_PERMISSION_REQUEST_CODE = 1;
    private static String[] PERMISSIONS_LIST = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private boolean mBound = false;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder localBinder =
                    (MediaPlayerService.LocalBinder) service;
            MediaPlayerService mediaPlayerService = localBinder.getService();
            MyApplication.setService(mediaPlayerService);
            mBound = true;

            Log.d("SHU","Service Bounded, Launch Main Activity");
            startActivity(new Intent(PermissionRequestActivity.this,MainActivity.class));
            finish();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound=false;
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SHU","oncreate of permision");
        if(checkAndRequestPermissions())
        {
            BindService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBound)
        {
            unbindService(mServiceConnection);
            mBound=false;
            Log.d("SHU","Service Unbounded from Permission");
        }
    }

    private void BindService() {
        Log.d("SHU","Service is getting Binded");
        Intent intent = new Intent(this,MediaPlayerService.class);
        startService(intent);
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);

    }

    private boolean checkAndRequestPermissions()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int permissionReadPhoneState = checkSelfPermission(PERMISSIONS_LIST[0]);
            int permissionStorage = ContextCompat.checkSelfPermission(this,PERMISSIONS_LIST[1]);

            List<String> listPermissionNeeded = new ArrayList<>();

            if(permissionReadPhoneState != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionNeeded.add(PERMISSIONS_LIST[0]);
            }

            if(permissionStorage != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionNeeded.add(PERMISSIONS_LIST[1]);
            }

            if(!listPermissionNeeded.isEmpty())
            {
                ActivityCompat.requestPermissions(this,listPermissionNeeded.toArray(new String[listPermissionNeeded.size()]),MULTIPLE_PERMISSION_REQUEST_CODE);
                return false;
            }
            else
            {
                return true;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case MULTIPLE_PERMISSION_REQUEST_CODE:
                Map<String,Integer> perms = new HashMap<>();

                //Initialize the map with both permissions
                perms.put(Manifest.permission.READ_PHONE_STATE,PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE,PackageManager.PERMISSION_GRANTED);

                //fill with actual result from the user
                for(int i=0;i<permissions.length;i++)
                {
                    perms.put(permissions[i],grantResults[i]);
                }

                //Check for both permissions

                if(perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                        perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                {
                    //Both permission has been granted
                    BindService();
                }
                else
                {
                    //Some permissions are not granted
                    //So if the user has not clicked on "do not ask again" check box
                    //Then show him the suggestion as to why this app need these permissions

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_PHONE_STATE) &&
                            ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    {
                        showDailogOK("Phone state and Storage permissions are required for this app",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which)
                                        {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                checkAndRequestPermissions();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                                intent.addCategory(Intent.CATEGORY_HOME);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish();
                                                break;
                                        }
                                    }
                                });
                    }
                    else if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_PHONE_STATE))
                    {
                        showDailogOK("Phone state permission required for this app",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which)
                                        {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                checkAndRequestPermissions();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                                intent.addCategory(Intent.CATEGORY_HOME);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish();
                                                break;
                                        }
                                    }
                                });
                    }
                    else if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    {
                        showDailogOK("External Storage permission required for this app",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which)
                                        {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                checkAndRequestPermissions();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                                intent.addCategory(Intent.CATEGORY_HOME);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish();
                                                break;
                                        }
                                    }
                                });
                    }

                    else
                    {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(this,"Revoke permissions in settings",Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
        }
    }

    private void showDailogOK(String message, DialogInterface.OnClickListener clickListener)
    {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Ok",clickListener)
                .setNegativeButton("Cancel",clickListener)
                .create()
                .show();
    }
}

