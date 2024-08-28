package com.android.soundrecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class MusicAdapter extends BaseAdapter {
    private Context context;
    private List<MusicInfo> files;

    public MusicAdapter(Context context, List<MusicInfo> files) {
        this.context = context;
        this.files = files;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = new ViewHolder();
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.listview_layout, null, false);
            holder.fileName = (TextView) convertView.findViewById(R.id.filename_txt);
            holder.fileTime = (TextView) convertView.findViewById(R.id.filetime_txt);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.fileName.setText(files.get(position).getFileName());
        holder.fileTime.setText(files.get(position).getFileTime());
        return convertView;
    }

    class ViewHolder {
        TextView fileName = null;
        TextView fileTime = null;
    }
}