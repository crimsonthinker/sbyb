package com.sbyb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.ActionMenuView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;

public class ScreenSlidePagerActivity extends Activity {

    public static final String APP_NAME = "SBYB";
    android.support.design.widget.BottomNavigationView nav;
    private static final String GALLERY_DIR =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                    File.separator + APP_NAME + File.separator;
//    File parentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    File ourDir; //= new File(parentDir.getAbsolutePath() + File.separator + "Camera");
    File[] files;
    int currIdx = 0; // current file positions
    int numOfFiles = 0;
    /*View Components*/
    ImageView imageView;
    android.support.v7.widget.ActionMenuView aboveMenuView;
    android.support.v7.widget.ActionMenuView bellowMenuView;
    Button galleryBtn;
    Button editBtn;

    ScreenSlidePagerActivity ourActivity;

    String currentViewFile = "";

    // Slider
//    private SliderLayout mDemoSlider;

    /************************CALLBACKS*************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);
//        nav = findViewById(R.id.nav_view);
        if (savedInstanceState == null)
        {
            if ( !refreshDir() )
            {
                Toast.makeText(getParent(), "No files", Toast.LENGTH_LONG).show();
            }
        }

        ourActivity = this;

        //*********Above and bellow Areas
        galleryBtn = findViewById(R.id.galleryBtn);
//        galleryBtn.setVisibility(View.GONE);
        aboveMenuView = findViewById(R.id.aboveMenuView);
        aboveMenuView.setVisibility(View.INVISIBLE);
        bellowMenuView = findViewById(R.id.bellowMenuView);
        bellowMenuView.setVisibility(View.INVISIBLE);
        editBtn = findViewById(R.id.editBtn);
//        belowBtn.setVisibility(View.GONE);


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager);
        mPager.setPageTransformer(true, new ZoomOutPageTransformer());
        pagerAdapter = new ScreenSlidePagerAdapter(this, files, aboveMenuView, bellowMenuView);
        mPager.setAdapter(pagerAdapter);
        mPager.setCurrentItem(currIdx, true);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        currIdx = intent.getIntExtra("POSITION", 0);

        // Bottom Navigation Bar
        nav = findViewById(R.id.nav_view);
        nav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_edit:
                        // On Edit button
                        Intent editIntent = new Intent(ourActivity, EditImageActivity.class);
                        editIntent.putExtra("filepath", currentViewFile );
                        startActivity(editIntent);
                        return true;
                    case R.id.navigation_delete:
                        // On Delete button
                        return true;
                    case R.id.navigation_gallery:
                        // On Gallery button
                        Intent galleryIntent = new Intent(ourActivity , Gallery.class);
                        startActivity(galleryIntent);
                        return true;
                }
                return false;
            }
        });

    }

    /*@Override
    protected void onStop() {
        // To prevent a memory leak on rotation, make sure to call stopAutoCycle() on the slider before activity or fragment is destroyed
        mDemoSlider.stopAutoCycle();
        super.onStop();
    }*/

    @Override
    protected void onResume() {
        super.onResume();

        refreshDir();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        refreshDir();
    }

    @Override
    public void recreate() {
        super.recreate();

        refreshDir();
    }


    private Boolean refreshDir(){
        try {
            ourDir = new File( GALLERY_DIR );
//                    new File(parentDir.getAbsolutePath() + File.separator + "Camera");
            files = ourDir.listFiles();
            numOfFiles = files.length;

            if ( ourDir == null || files == null || numOfFiles == 0) return false;

            if (files != null && numOfFiles > 1) {
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File object1, File object2) {
                        return object2.getName().compareTo(object1.getName());
                    }
                });
            }

            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public static boolean isImageFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }

    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }



    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter pagerAdapter;

    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.activity_screen_slide.activity_screen_slide);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
    }*/

    /*@Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }*/

    public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0f);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0f);
            }
        }
    }

    public class ScreenSlidePagerAdapter extends PagerAdapter {

        Context context;
        File[] files;
        LayoutInflater inflater;
        ImageView imageView;
        VideoView videoView;
        android.support.v7.widget.ActionMenuView aboveMenuView;
        ActionMenuView bellowMenuView;

        MediaController mc;

        public ScreenSlidePagerAdapter(Context context, File[] files
                , android.support.v7.widget.ActionMenuView a
                , android.support.v7.widget.ActionMenuView b) {

            this.context = context;
            this.files = files;

            aboveMenuView = a;
            bellowMenuView = b;
        }

        private Boolean viewImage(int position){
            try
            {
                currentViewFile = files[position].getAbsolutePath();
                Glide.with(context)
                        .load(files[position])
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                // log exception
                                Log.e("TAG", "Error loading image", e);
                                return false; // important to return false so the error placeholder can be placed
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(imageView);
            }
            catch (Exception e){
                return  false;
            }
            return true;
        }

        private Boolean viewVideo(int position){
            try
            {
                currentViewFile = files[position].getAbsolutePath();
                Glide.with(context)
                        .asBitmap()
                        .load(Uri.fromFile( files[position] ) )
                        .placeholder(R.drawable.ic_gallery)
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                // log exception
                                Log.e("TAG", "Error loading image", e);
                                return false; // important to return false so the error placeholder can be placed
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(imageView);
            }
            catch (Exception e){
                return  false;
            }
            return true;
        }

        public void previewVideo(Context context, File file, final VideoView videoView, final ImageView imageView) {
            videoView.setVideoPath(file.getAbsolutePath());

            final MediaController mediaController = new MediaController(context);
//            mediaController.setAnchorView(mc);

            videoView.setMediaController(mediaController);

            mediaController.setMediaPlayer(videoView);

            videoView.setVisibility(View.VISIBLE);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
//                    imageView.setVisibility(View.INVISIBLE);
                    /*mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

                        }
                    });*/
                    mediaController.setAnchorView(videoView);
                    mp.setVolume(0f, 0f);
                    mp.setLooping(true);
                }

            });

            videoView.start();
            videoView.requestFocus();
            imageView.setVisibility(View.INVISIBLE);

        /*try {
            Uri myUri = Uri.fromFile(file); // initialize Uri here
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setLooping(true);
            mediaPlayer.setDataSource(context.getApplicationContext(), myUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }*/


        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {

            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.screen_slide_page, null);

            imageView = v.findViewById(R.id.imageView);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (nav.getVisibility() == View.INVISIBLE)
                    {
//                        aboveMenuView.setVisibility(View.VISIBLE);
//                        bellowMenuView.setVisibility(View.VISIBLE);
                        nav.setVisibility(View.VISIBLE);

                    }
                    else if (nav.getVisibility() == View.VISIBLE){
                        nav.setVisibility(View.INVISIBLE);
//                        aboveMenuView.setVisibility(View.INVISIBLE);
//                        bellowMenuView.setVisibility(View.INVISIBLE);
                    }

                }
            });
            videoView = v.findViewById(R.id.videoView);
            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*if (aboveMenuView.getVisibility() == View.INVISIBLE)
                    {
                        aboveMenuView.setVisibility(View.VISIBLE);
                        bellowMenuView.setVisibility(View.VISIBLE);

                    }
                    else if (aboveMenuView.getVisibility() == View.VISIBLE){
                        aboveMenuView.setVisibility(View.INVISIBLE);
                        bellowMenuView.setVisibility(View.INVISIBLE);
                    }*/
                    if (nav.getVisibility() == View.INVISIBLE)
                    {
                        nav.setVisibility(View.VISIBLE);
                    }
                    else if (nav.getVisibility() == View.VISIBLE){
                        nav.setVisibility(View.INVISIBLE);
                    }

                }
            });

//            mc = v.findViewById(R.id.mcView);

            if ( ScreenSlidePagerActivity.isImageFile( files[position].getAbsolutePath() ) )
            {
                videoView.setVisibility(View.GONE);
//            imageView.setVisibility(View.VISIBLE);
            /*Glide.with(context)
                    .load(files[position])
                    .centerInside()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);*/
                viewImage(position);
            }
            else if ( ScreenSlidePagerActivity.isVideoFile( files[position].getAbsolutePath() ) )
            {
//            imageView.setVisibility(View.GONE);
//            videoView.setVisibility(View.VISIBLE);
                //videoView.setVisibility(View.INVISIBLE);

                viewVideo(position);
//            imageView.setVisibility(View.INVISIBLE);
                previewVideo(context, files[position], videoView, imageView);
            }

            ViewPager vp = (ViewPager)container;
            vp.addView(v, 0);

            return v;
        }

       /* @Override
        public Fragment getItem(int position) {
            return new ScreenSlidePageFragment();
        }*/

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public int getCount() {
            return files.length;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            ViewPager vp = (ViewPager)container;
            View v = (View)object;
            vp.removeView(v);

//        super.destroyItem(container, position, object);


        }
    }


    /*private android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_edit:
                    // On Edit button
                    Intent editIntent = new Intent(ourActivity, EditImageActivity.class);
                    editIntent.putExtra("filepath", files[currIdx].getAbsolutePath() );
                    return true;
                case R.id.navigation_delete:
                    // On Delete button
                    return true;
                case R.id.navigation_gallery:
                    // On Gallery button
                    Intent galleryIntent = new Intent(ourActivity , Gallery.class);
                    startActivity(galleryIntent);
                    return true;
            }
            return false;
        }
    };*/
}

