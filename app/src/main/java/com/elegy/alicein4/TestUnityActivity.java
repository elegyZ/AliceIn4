package com.elegy.alicein4;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

public class TestUnityActivity extends UnityPlayerActivity
{
    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent)
    {
        if (i == KeyEvent.KEYCODE_BACK)
        {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onKeyDown(i, keyEvent);
    }
}
