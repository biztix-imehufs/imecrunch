package listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hufs.ime.imecrunch.R;

import java.util.ArrayList;

/**
 * Created by macbook on 4/14/16.
 */
public class MovementSensorListAdapter extends BaseAdapter {

    private ArrayList<MovementSensorItem> movementSensorItems = new ArrayList<>();
    private LayoutInflater inflater;

    public MovementSensorListAdapter(Context context, ArrayList<MovementSensorItem> movementSensorItems) {
        this.movementSensorItems = movementSensorItems;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return movementSensorItems.size();
    }

    @Override
    public Object getItem(int position) {
        return movementSensorItems.get(position);
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
            convertView = inflater.inflate(R.layout.list_item_movement_sensors, null);
            holder = new ViewHolder();
            holder.sensorName = (TextView) convertView.findViewById(R.id.txt_sensor_name);
            holder.sensorValue = (TextView) convertView.findViewById(R.id.txt_sensor_value);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.sensorName.setText(movementSensorItems.get(position).getSensorName());
        holder.sensorValue.setText(movementSensorItems.get(position).getSensorValue());
        return convertView;
    }

}
