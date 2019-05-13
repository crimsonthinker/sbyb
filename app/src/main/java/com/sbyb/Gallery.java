package com.sbyb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class Gallery extends AppCompatActivity {
    ImageView image_photo;
    ImageView image_video;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);
        image_photo = (ImageView) findViewById(R.id.Photo_);
        image_video = (ImageView) findViewById(R.id.Video_);

        image_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Gallery.this,AlbumPhotoActivity.class);
                startActivity(intent);
            }
        });

        image_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Gallery.this,AlbumVideoActivity.class);
                startActivity(intent);
            }
        });
    }
}
