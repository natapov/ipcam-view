package com.github.niqdev.ipcam;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegSurfaceView;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;

public class IpCamTwoActivity extends AppCompatActivity {

    private static final int TIMEOUT = 5;

    @BindView(R.id.mjpegViewDefault1)
    MjpegSurfaceView mjpegView1;

    @BindView(R.id.mjpegViewDefault2)
    MjpegSurfaceView mjpegView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipcam_two_camera);
        ButterKnife.bind(this);
    }

    private void loadIpCam1() {
        new Mjpeg()
                .open("https://app.punyapat.me/mjpeg-server/mjpeg", TIMEOUT)
                .subscribe(
                        inputStream -> {
                            mjpegView1.setSource(inputStream);
                            mjpegView1.setDisplayMode(MjpegSurfaceView.DisplayMode.BEST_FIT);
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
                        });
    }

    private void loadIpCam2() {
        new Mjpeg()
                .open("https://app.punyapat.me/mjpeg-server/mjpeg", TIMEOUT)
                .subscribe(
                        inputStream -> {
                            mjpegView2.setSource(inputStream);
                            mjpegView2.setDisplayMode(MjpegSurfaceView.DisplayMode.BEST_FIT);
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
                        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIpCam1();
        loadIpCam2();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mjpegView1.stopPlayback();
        mjpegView2.stopPlayback();
    }

}
