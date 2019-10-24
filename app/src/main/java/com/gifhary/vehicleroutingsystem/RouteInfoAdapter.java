package com.gifhary.vehicleroutingsystem;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class RouteInfoAdapter extends ArrayAdapter<RouteInfo> {
    private static final String TAG = "RouteInfoAdapter";

    private Context context;
    private int resource;

    public RouteInfoAdapter(Context context, int resource, ArrayList<RouteInfo> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String routeName = getItem(position).getRouteName();
        String distance = getItem(position).getDistance();
        String duration = getItem(position).getDuration();
        String durationInTraffic = getItem(position).getDurationInTraffic();

        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(resource, parent, false);

        TextView tvRouteName = convertView.findViewById(R.id.routeID);
        TextView tvDistance = convertView.findViewById(R.id.distance);
        TextView tvDuration = convertView.findViewById(R.id.duration);
        TextView tvDurationInTraffic = convertView.findViewById(R.id.durationInTraffic);

        tvRouteName.setText(routeName);
        tvDistance.setText(distance);
        tvDuration.setText(duration);
        tvDurationInTraffic.setText(durationInTraffic);

        return convertView;
    }
}
