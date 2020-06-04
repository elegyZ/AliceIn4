package com.elegy.alicein4;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.view.View.OnClickListener;

public class MainActivity extends AppCompatActivity
{

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    private Button btn_start;
    private Button btn_unity;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isSdkConfigured())                 //To make sure the IndoorAtlas SDK words.
        {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.configuration_incomplete_title)
                    .setMessage(R.string.configuration_incomplete_message)
                    .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {finish();}
                    }).show();
            return;
        }

        ensurePermissions();

        btn_start = (Button)this.findViewById(R.id.button_start);
        btn_start.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, SimpleActivity.class);
                startActivity(intent);
            }
        });

        btn_unity = (Button) this.findViewById(R.id.button_unity);
        btn_unity.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, TestUnityActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Checks the app has the correct api.
     */
    private boolean isSdkConfigured()
    {
        return !"api-key-not-set".equals(getString(R.string.indooratlas_api_key)) && !"api-secret-not-set".equals(getString(R.string.indooratlas_api_secret));
    }

    /**
     * Checks that we have access to required information, if not ask for users permission.
     */
    private void ensurePermissions()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // We dont have access to FINE_LOCATION (Required by Google Maps example)
            // IndoorAtlas SDK has minimum requirement of COARSE_LOCATION to enable WiFi scanning
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.location_permission_request_title)
                        .setMessage(R.string.location_permission_request_rationale)
                        .setPositiveButton(R.string.permission_button_accept, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Log.d(TAG, "request permissions");
                                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_CODE_ACCESS_COARSE_LOCATION);
                            }
                        })
                        .setNegativeButton(R.string.permission_button_deny, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Toast.makeText(MainActivity.this, R.string.location_permission_denied_message, Toast.LENGTH_LONG).show();
                            }
                        })
                        .show();

            } else {
                // ask user for permission
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_ACCESS_COARSE_LOCATION:
                if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
                {
                    Toast.makeText(this, R.string.location_permission_denied_message, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
