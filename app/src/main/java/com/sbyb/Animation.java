package com.sbyb;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.animation.Animation;

import java.util.List;

class AsyncAnimation extends AsyncTask<AnimationParam,Void, Integer> {
    @Override
    protected Integer doInBackground(AnimationParam... params) {
        Context mContext = params[0].mContext;
        View object = params[0].obj;
        List<Animation> animations = params[0].animation;
        for(int i = 0 ; i< animations.size(); ++i)
            object.startAnimation(animations.get(i));
        return 0;
    }
}

class AnimationParam{
    public Context mContext;
    public View obj;
    public List<Animation> animation;
    AnimationParam(Context c,View v, List<Animation> a){
        this.mContext = c;
        this.obj = v;
        this.animation = a;
    }
}