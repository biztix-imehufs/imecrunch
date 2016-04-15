package com.hufs.ime.imecrunch;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by macbook on 4/14/16.
 */
public class SensorListAdapter extends BaseAdapter {

    private ArrayList<SensorItem> sensorItems = new ArrayList<>();
    private LayoutInflater inflater;

    public SensorListAdapter(Context context, ArrayList<SensorItem> sensorItems) {
        this.sensorItems = sensorItems;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return sensorItems.size();
    }

    @Override
    public Object getItem(int position) {
        return sensorItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView sensorName, sensorValue;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_sensors, null);
            holder = new ViewHolder();
            holder.sensorName = (TextView) convertView.findViewById(R.id.txt_sensor_name);
            holder.sensorValue = (TextView) convertView.findViewById(R.id.txt_sensor_value);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.sensorName.setText(sensorItems.get(position).getSensorName());
        holder.sensorValue.setText(sensorItems.get(position).getSensorValue());
        return convertView;
    }

}
