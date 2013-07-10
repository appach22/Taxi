package com.taxisoft.taxi7;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import ru.yandex.yandexmapkit.overlay.balloon.BalloonItem;
import ru.yandex.yandexmapkit.utils.GeoPoint;

/**
 * BalloonOverlay.java
 *
 * This file is a part of the Yandex Map Kit.
 *
 * Version for Android © 2012 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://legal.yandex.ru/mapkit/
 *
 */
public class MyLocationBalloonItem extends BalloonItem{

    protected Button mBtnYes;
    protected Button mBtnNo;
    Context mContext;

    public MyLocationBalloonItem(Context context, GeoPoint geoPoint) {
        super(context, geoPoint);
        mContext = context;
    }

    @Override
    public void inflateView(Context context){

        LayoutInflater inflater = LayoutInflater.from( context );
        model = (ViewGroup)inflater.inflate(R.layout.my_location_balloon_layout, null);
    }
    
    @Override
    public void setText(CharSequence text)
    {
    	((TextView)model.findViewById(R.id.tvCurrentLocationAddress)).setText(text);
    }
}