package com.sbyb;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import java.util.concurrent.ExecutionException;

public class ScreenSlidePagerActivity extends Activity {

    /************************** Attributes ********************************************************/
    public static final String APP_NAME = "SBYB";
    private static final String GALLERY_DIR =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                    File.separator + APP_NAME + File.separator;
    File ourDir;
    File[] files;
    int currIdx = 0; // current file positions
    int numOfFiles = 0;

    ScreenSlidePagerActivity ourActivity;

    String currentViewFile = "";

    // Slider
//    private SliderLayout mDemoSlider;

    android.support.design.widget.BottomNavigationView nav;
    private int count = 0;

    /*@Override
    protected void onStop() {
        // To prevent a memory leak on rotation, make sure to call stopAutoCycle() on the slider
         before activity or fragment is destroyed
        mDemoSlider.stopAutoCycle();
        super.onStop();
    }*/
    private Boolean returnFromEdit = false;
    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter pagerAdapter = null;

    public static boolean isImageFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }

    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    /************************CALLBACKS*************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);
        if (savedInstanceState == null)
        {
            try {
                if ( !new RefreshDirTask().execute().get() )
                {
                    Toast.makeText(getParent(), "No files", Toast.LENGTH_LONG).show();
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Get the Intent that started this activity and extract the string
            Intent intent = getIntent();
            currIdx = intent.getIntExtra("POSITION", 0);
        }

        ourActivity = this;

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager);
        mPager.setPageTransformer(true, new ZoomOutPageTransformer() );
        pagerAdapter = new ScreenSlidePagerAdapter(this, files);
        final ScreenSlidePagerAdapter pA = (ScreenSlidePagerAdapter)pagerAdapter;
        mPager.setAdapter(pagerAdapter);
        mPager.setCurrentItem(currIdx, true);
        final ViewPager.OnPageChangeListener onPageChangeListener =
                new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                System.out.println("onPageSelected " + i);
                VideoView videoView = pA.getVideoView();

                if (ScreenSlidePagerActivity.isImageFile( files[i].getAbsolutePath() ) ) {

//                    videoView.setVisibility(View.GONE);
                    showBottomNavigationView(true);

                }
                else if ( ScreenSlidePagerActivity.isVideoFile( files[i].getAbsolutePath() ) ) {
                    showBottomNavigationView(false);

//                    pA.getVideoView().setVisibility(View.VISIBLE);
//                    pA.previewVideo( files[i], i );
//                    pA.getImageView().setVisibility(View.INVISIBLE);
                    videoView.bringToFront();
                    videoView.requestFocus();
                    videoView.start();
                    if ( videoView.isPlaying() ) System.out.println("video " + i + " isPlaying");
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        };
        mPager.addOnPageChangeListener(onPageChangeListener);
        mPager.post(new Runnable(){
            @Override
            public void run() {
                onPageChangeListener.onPageSelected( mPager.getCurrentItem() );
            }
        });
        // Bottom Navigation Bar
        nav = findViewById(R.id.nav_view);
        nav.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_edit:
                        // On Edit button
                        Intent editIntent = new Intent(ourActivity, EditImageActivity.class);
                        currentViewFile = files[mPager.getCurrentItem()].getAbsolutePath();
//                        Toast.makeText(ourActivity, currentViewFile, Toast.LENGTH_LONG).show();
                        editIntent.putExtra("filepath", currentViewFile );
                        startActivity(editIntent);
                        returnFromEdit = true;
                        return true;
                    case R.id.navigation_delete:
                        // On Delete button
                        AlertDialog diaBox = AskOption();
                        diaBox.show();
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

    @Override
    protected void onResume() {
        super.onResume();

        ++count;
        // onResume is called twice when launching activity
        if (count > 2) {
            try {
                new RefreshDirTask().execute().get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //TODO: Update data set for pagerAdapter
            ScreenSlidePagerAdapter pA = (ScreenSlidePagerAdapter)pagerAdapter;
            if (pA.getCount() != files.length) {
                pA.files = files;
                pA.notifyDataSetChanged();
            }
            if (returnFromEdit) {

                mPager.setCurrentItem(0, true);
                returnFromEdit = false;
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        ++count;
    }

    /***************************** Utility methods ************************************************/

    private Boolean refreshDir() {
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
//                        return object2.getName().compareTo(object1.getName());
                        long r = object2.lastModified() - object1.lastModified();
                        return ( r > 0 ) ? 1 : ( (r < 0) ? -1 : 0 );
                    }
                });
            }

            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    private AlertDialog AskOption() {
        AlertDialog myQuittingDialogBox =new AlertDialog.Builder(this)
                //set message, title, and icon
                .setTitle("Delete")
                .setMessage("Do you want to Delete")
//                .setIcon(R.drawable.delete)

                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        //your deleting code
                        currIdx = mPager.getCurrentItem();
                        if ( files[currIdx].exists() ) {
                            if ( files[currIdx].delete() ) {
                                Toast.makeText(ourActivity,
                                        "file Deleted :" + files[currIdx].getPath(),
                                        Toast.LENGTH_LONG).show();

                                /*LayoutInflater inflater =
                                        (LayoutInflater)( (Context)ourActivity )
                                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                View v = inflater.inflate(R.layout.screen_slide_page, mPager
                                        , false);
                                pagerAdapter.destroyItem(mPager, currIdx, v);*/
                                ScreenSlidePagerAdapter pA = (ScreenSlidePagerAdapter)pagerAdapter;
                                try {
                                    new RefreshDirTask().execute().get();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                int nextIdx = pA.nextItemIdx;
                                ( pA ).files = files;
                                pagerAdapter.notifyDataSetChanged();
                                System.out.println("curr " + currIdx + " next " + pA.nextItemIdx);
                                if (nextIdx > currIdx) {
                                    mPager.setCurrentItem(currIdx
                                            , true);
                                }
                                else {
                                    mPager.setCurrentItem(nextIdx, true);
                                }

                            } else {
                                Toast.makeText(ourActivity,
                                        "file not Deleted :" + files[currIdx].getPath(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        dialog.dismiss();
                    }

                })

                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();

                    }
                })
                .create();
        return myQuittingDialogBox;

    }

    private void showBottomNavigationView(Boolean isImage) {
        if (isImage) nav.getMenu().findItem(R.id.navigation_edit).setVisible(true);
        else nav.getMenu().findItem(R.id.navigation_edit).setVisible(false);
    }

    /********************************** Slider zone ***********************************************/
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /***** Internal class for refresh directory on new thread ******/
    private class RefreshDirTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... avoid) {
            return refreshDir();
        }
    }

    /************************ Transformer internal class ******************/
    public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.97f;//0.85f;
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
//                view.setAlpha(MIN_ALPHA +
//                        (scaleFactor - MIN_SCALE) /
//                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));
                view.setAlpha(1f);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0f);
            }
        }
    }

    /************** Adapter internal class ******************/
    public class ScreenSlidePagerAdapter extends PagerAdapter {

        Context context;
        public  File[] files;
        LayoutInflater inflater;
        ImageView imageView;
        VideoView videoView;

//        MediaController mc;
        // next item
        public int nextItemIdx;
        // check if the file is video type or image type
        Boolean isVideo = false;

        /***************** Constructor ************************************************************/
        public ScreenSlidePagerAdapter(final Context context, final File[] files){

            this.context = context;
            this.files = files;

        }

        /*************************** Utility methods **********************************************/
        private Boolean viewImage(int position){
            try
            {
                System.out.println("viewImage " + position);
                Glide.with(context)
                        .load(files[position])
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model
                                    , Target<Drawable> target, boolean isFirstResource) {
                                // log exception
                                Log.e("TAG", "Error loading image", e);
                                return false; // important to return false so the error placeholder can be placed
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model
                                    , Target<Drawable> target, DataSource dataSource
                                    , boolean isFirstResource) {
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
                System.out.println("viewVideo " + position);
                Glide.with(context)
                        .asBitmap()
                        .load(Uri.fromFile( files[position] ) )
                        .placeholder(R.drawable.play)
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model
                                    , Target<Bitmap> target, boolean isFirstResource) {
                                // log exception
                                Log.e("TAG", "Error loading image", e);
                                return false; // important to return false so the error placeholder can be placed
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model
                                    , Target<Bitmap> target, DataSource dataSource
                                    , boolean isFirstResource) {
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

        public void previewVideo( File file, final int position){// {Context context,
            videoView.setVideoPath( file.getAbsolutePath() );
            videoView.seekTo(1);
            /*final MediaController mediaController = new MediaController(context);
            mediaController.setAnchorView(mc);
            videoView.setMediaController(mediaController);
            mediaController.setMediaPlayer(videoView);*/

//            videoView.setVisibility(View.VISIBLE);
//            viewVideo(position);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mp) {
//                    imageView.setVisibility(View.INVISIBLE);
//                    videoView.setVisibility(View.INVISIBLE);
//                    viewVideo(position);
                    System.out.println("onPrepared " + position);
//                    imageView.setVisibility(View.INVISIBLE);
                    videoView.setVisibility(View.VISIBLE);
                    mp.setVolume(0f, 0f);
                    mp.setLooping(true);
                }

            });

            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    videoView.setVisibility(View.INVISIBLE);
//                    imageView.setVisibility(View.VISIBLE);
                    return false;
                }
            });

//            videoView.start();
//            if (videoView.isPlaying() )
//                videoView.setVisibility(View.VISIBLE);
//            videoView.requestFocus();
//            imageView.setVisibility(View.INVISIBLE);

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

        // Getter
        public ImageView getImageView() {
            return imageView;
        }
        public VideoView getVideoView() {
            return videoView;
        }

        // Bottom navigation view display
        private void showHideBottomNavigationView(boolean isImage) {
            if (nav.getVisibility() == View.INVISIBLE)
            {
                mPager.setBackgroundColor(0xB3FFFFFF);
                nav.setBackgroundColor(0x80FFFFFF);
                nav.bringToFront();
                nav.setVisibility(View.VISIBLE);
            }
            else if (nav.getVisibility() == View.VISIBLE){
                mPager.setBackgroundColor(Color.BLACK);
                nav.setVisibility(View.INVISIBLE);
            }
        }

        /************************ Callbacks *******************************************************/
        @Override
        public int getItemPosition(@NonNull Object object) {

            return POSITION_NONE;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            nextItemIdx = position;
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.screen_slide_page, container, false);

            imageView = v.findViewById(R.id.imageView);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showHideBottomNavigationView(true);
                }
            });
            videoView = v.findViewById(R.id.videoView);
            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showHideBottomNavigationView(false);
                }
            });

//            mc = v.findViewById(R.id.mcView);

            if (ScreenSlidePagerActivity.isImageFile( files[position].getAbsolutePath() ) ) {
                System.out.println("InstantiateImage " + position);
//                imageView.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.GONE);
                viewImage(position);
            } else if (ScreenSlidePagerActivity.isVideoFile( files[position].getAbsolutePath() ) ) {
//                videoView.setVisibility(View.INVISIBLE);
//                nav.getMenu().findItem(R.id.navigation_edit).setVisible(false);
//                videoView.setVisibility(View.VISIBLE);
//                viewVideo(position);
//                imageView.bringToFront();
                //, videoView, imageView);
                System.out.println("InstantiateVideo " + position);
//                imageView.setVisibility(View.GONE);
                videoView.setAlpha(1f);

                previewVideo(files[position], position);
            }


            ViewPager vp = (ViewPager)container;
            vp.addView(v);

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
        public void destroyItem(@NonNull ViewGroup container, int position
                , @NonNull Object object) {
            ViewPager vp = (ViewPager)container;
            View v = (View)object;
            vp.removeView(v);

//        super.destroyItem(container, position, object);

        }
    }

}

