package com.sbyb;

import android.app.Activity;
import android.os.Bundle;
import android.widget.VideoView;
import android.widget.MediaController;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;

public class VideoActivity extends Activity {

    String str_video;
    VideoView vv_video;
    private int position = 0;
    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_preview_video);
        init();
    }

    private void init() {

        vv_video = (VideoView) findViewById(R.id.vv_video);

        if (mediaController == null) {
            mediaController = new MediaController(VideoActivity.this);

            // Neo vị trí của MediaController với VideoView.
            mediaController.setAnchorView(vv_video);


            // Sét đặt bộ điều khiển cho VideoView.
            vv_video.setMediaController(mediaController);
        }

        str_video = getIntent().getStringExtra("video");
        vv_video.setVideoPath(str_video);
//        vv_video.start();

        vv_video.requestFocus();

        vv_video.setOnPreparedListener(new OnPreparedListener() {

            public void onPrepared(MediaPlayer mediaPlayer) {


                vv_video.seekTo(position);
                if (position == 0) {
                    vv_video.start();
                }

                // Khi màn hình Video thay đổi kích thước
                mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

                        // Neo lại vị trí của bộ điều khiển của nó vào VideoView.
                        mediaController.setAnchorView(vv_video);
                    }
                });
            }
        });
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        // Lưu lại vị trí file video đang chơi.
        savedInstanceState.putInt("CurrentPosition", vv_video.getCurrentPosition());
        vv_video.pause();
    }


    // Sau khi điện thoại xoay chiều xong. Phương thức này được gọi,
    // bạn cần tái tạo lại ví trí file nhạc đang chơi.
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Lấy lại ví trí video đã chơi.
        position = savedInstanceState.getInt("CurrentPosition");
        vv_video.seekTo(position);
    }

}