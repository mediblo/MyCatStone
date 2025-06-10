// ProjectionRequestActivity.java
package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

public class ProjectionRequestActivity extends Activity {
    public static final int REQ_MEDIA_PROJ = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mpm.createScreenCaptureIntent();
        startActivityForResult(intent, REQ_MEDIA_PROJ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_MEDIA_PROJ) {
            Intent b = new Intent("com.example.myapplication.ACTION_MEDIA_PROJECTION");
            b.putExtra("resultCode", resultCode);
            b.putExtra("data", data);
            sendBroadcast(b);
        }
        finish();
    }
}