package com.androidsystem.locationtracker.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.androidsystem.locationtracker.R;

import java.util.ArrayList;

public class CustomListAdapter extends ArrayAdapter<String> {
    private final Context mContext;
    private final ArrayList<String> mLatitudeList;
    private final ArrayList<String> mLongitudeList;
    private final ArrayList<String> mTimeList;

    public CustomListAdapter(Context context, ArrayList<String> latitudeList, ArrayList<String> longitudeList, ArrayList<String> timeList) {
        super(context, R.layout.location_list_item);
        this.mContext = context;
        this.mLatitudeList = latitudeList;
        this.mLongitudeList = longitudeList;
        this.mTimeList = timeList;
    }

    @Override
    public int getCount() {
        return mLatitudeList.size();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("ViewHolder") View rowView = inflater.inflate(R.layout.location_list_item, parent, false);

        TextView latitudeTextView = rowView.findViewById(R.id.latitudeTextView);
        TextView longitudeTextView = rowView.findViewById(R.id.longitudeTextView);
        TextView timeTextView = rowView.findViewById(R.id.datetimeTextView);

        latitudeTextView.setText(mLatitudeList.get(position));
        longitudeTextView.setText(mLongitudeList.get(position));
        timeTextView.setText(mTimeList.get(position));

        return rowView;
    }
}
