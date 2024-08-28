package com.softwinner.runin.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.softwinner.runin.R;
import com.softwinner.runin.Settings;
import com.softwinner.runin.config.interfaces.IConfiguration;
import com.softwinner.xml.Node;

/**
 * Node Adapter
 * @author zengsc
 * @version date 2013-5-16
 */
public class NodeAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {
	static final String TAG = "Runin-NodeAdapter";
	static final boolean DEBUG = Settings.DEBUG;
	private final Context mContext;
	private final IConfiguration[] mForegrounds = Settings.getForegroundConfig();
	private final IConfiguration[] mBackgrounds = Settings.getBackgroundConfig();
	private List<IConfiguration> mConfigs;
	private Map<String, Node> mData;
	private Map<String, Node> mRemoveData;
	private Node mNode;

	private int mSelection;
	private OnSelectionChangeListener mOnSelectionChangeListener;

	public NodeAdapter(Context context) {
		mContext = context;
		mConfigs = new ArrayList<IConfiguration>();
		mData = new HashMap<String, Node>();
		mRemoveData = new HashMap<String, Node>();
	}

	/**
	 * set OnSelectionChangeListener
	 */
	public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
		mOnSelectionChangeListener = listener;
	}

	/**
	 * selection change and switch change listener
	 */
	public interface OnSelectionChangeListener {
		public void onSelection(int position);
	}

	class Holder {
		TextView name;
		Switch switchWidget;
	}

	/**
	 * set data
	 */
	public void setData(Node node) {
		mConfigs.clear();
		mData.clear();
		mRemoveData.clear();
		mNode = node;
		Node fore = node.getNode(Settings.NODE_FOREGROUND);
		Node back = node.getNode(Settings.NODE_BACKGROUND);
		Node item;
		String key;
		for (IConfiguration config : mForegrounds) {
			mConfigs.add(config);
			key = config.name();
			item = fore.getNode(key);
			mData.put(key, item);
		}
		for (IConfiguration config : mBackgrounds) {
			mConfigs.add(config);
			key = config.name();
			item = back.getNode(key);
			mData.put(key, item);
		}
		// initialize selection
		mSelection = -1;
		setSelection(0);
		// notifyDataSetChanged();
	}

	/**
	 * set selection
	 */
	public void setSelection(int position) {
		if (mSelection == position)
			return;
		mSelection = position;
		notifyDataSetChanged();
		if (mOnSelectionChangeListener != null)
			mOnSelectionChangeListener.onSelection(position);
	}

	/**
	 * is foreground
	 */
	public boolean isForeground(int position) {
		return position < mForegrounds.length;
	}

	/**
	 * get config
	 */
	public IConfiguration getConfig(int position) {
		if (position < 0 || position >= getCount())
			return null;
		else
			return mConfigs.get(position);
	}

	/**
	 * get key
	 */
	public String getKey(int position) {
		if (position < 0 || position >= getCount())
			return null;
		else
			return mConfigs.get(position).name();
	}

	@Override
	public int getCount() {
		return mConfigs.size();
	}

	@Override
	public Node getItem(int position) {
		if (position < 0 || position >= getCount())
			return null;
		else
			return mData.get(getKey(position));
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		if (convertView == null) {
			convertView = View.inflate(mContext, R.layout.configuration_item, null);
			holder = new Holder();
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.switchWidget = (Switch) convertView.findViewById(R.id.switch_widget);
			holder.switchWidget.setOnCheckedChangeListener(this);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		holder.switchWidget.setTag(position);

		if (mSelection == position) {
			convertView.setBackgroundResource(android.R.color.holo_blue_dark);
		} else {
			convertView.setBackgroundResource(android.R.color.transparent);
		}
		holder.name.setText(getKey(position));
		Node item = getItem(position);
		holder.switchWidget.setChecked(item != null);
		return convertView;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int position = (Integer) buttonView.getTag();
		String key = getKey(position);
		Node node;
		node = mData.get(key);
		if (isChecked) {
			if (node == null) {
				node = mRemoveData.get(key);
				if (node == null) {
					node = new Node(key);
				}
				mData.put(key, node);
			}
		} else {
			if (node != null) {
				mRemoveData.put(key, node);
			}
			mData.remove(key);
		}
		if (mSelection == position)
			if (mOnSelectionChangeListener != null)
				mOnSelectionChangeListener.onSelection(position);
	}

	/**
	 * update node
	 */
	public Node updateNode() {
		mNode.removeAllNodes();
		Node fore = new Node(Settings.NODE_FOREGROUND);
		Node back = new Node(Settings.NODE_BACKGROUND);
		mNode.addNode(fore);
		mNode.addNode(back);
		Node item;
		String key;
		for (IConfiguration config : mForegrounds) {
			key = config.name();
			item = mData.get(key);
			if (item != null) {
				fore.addNode(item);
			}
		}
		for (IConfiguration config : mBackgrounds) {
			key = config.name();
			item = mData.get(key);
			if (item != null) {
				back.addNode(item);
			}
		}
		if (DEBUG)
			Log.d(TAG, mNode.toString());
		return mNode;
	}
}
