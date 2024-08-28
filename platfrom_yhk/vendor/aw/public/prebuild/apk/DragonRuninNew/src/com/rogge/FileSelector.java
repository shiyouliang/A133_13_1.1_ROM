package com.rogge;

import java.io.File;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.softwinner.runin.R;

/**
 * 文件选择
 * @author zengsc
 * @version date 2013-6-8
 */
public class FileSelector extends PopupWindow implements AdapterView.OnItemClickListener,
		AdapterView.OnItemLongClickListener, View.OnClickListener {
	static final String ROOT_PATH = File.separator;
	final Context mContext;
	private LinearLayout mView;
	private TextView mPath;
	private Button mBack;
	private Button mOk;
	private ListView mListView;
	private File mCurrentFile;
	private FileAdapter mFileAdapter;
	private String mRootPath = ROOT_PATH;

	public FileSelector(Context context) {
		super(context);
		mContext = context;

		init();
	}

	private void init() {
		// int padding = mContext.getResources().getDimensionPixelSize(R.dimen.listview_padding);
		// mView = new LinearLayout(mContext);
		// mView.setPadding(padding, padding, padding, padding);
		// mView.setOrientation(LinearLayout.VERTICAL);
		// // top linear
		// LinearLayout topLayout = new LinearLayout(mContext);
		// LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
		// LinearLayout.LayoutParams.WRAP_CONTENT);
		// mView.addView(topLayout, topParams);
		// topLayout.setOrientation(LinearLayout.HORIZONTAL);
		// // path and button
		// mPath = new TextView(mContext);
		// LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(0,
		// LinearLayout.LayoutParams.WRAP_CONTENT);
		// pathParams.weight = 1;
		// topLayout.addView(mPath, pathParams);
		// mBack = new Button(mContext);
		// mOk = new Button(mContext);
		// topLayout.addView(mBack);
		// topLayout.addView(mOk);
		// mBack.setText(R.string.file_selector_button_back);
		// mOk.setText(R.string.file_selector_button_ok);
		// // space
		// View space = new View(mContext);
		// space.setBackgroundResource(android.R.color.holo_blue_light);
		// ViewGroup.LayoutParams spaceParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
		// mView.addView(space, spaceParams);
		// // listview
		// mListView = new ListView(mContext);
		// LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
		// LinearLayout.LayoutParams.MATCH_PARENT);
		// mView.addView(mListView, listParams);
		// mListView.setDivider(null);
		// mListView.setDividerHeight(10);
		// mListView.setCacheColorHint(Color.TRANSPARENT);
		mView = (LinearLayout) View.inflate(mContext, R.layout.file_selector, null);
		mPath = (TextView) mView.findViewById(R.id.file_selector_path_text);
		mBack = (Button) mView.findViewById(R.id.file_selector_back_button);
		mOk = (Button) mView.findViewById(R.id.file_selector_ok_button);
		mListView = (ListView) mView.findViewById(R.id.file_selector_file_list);

		mBack.setOnClickListener(this);
		mOk.setOnClickListener(this);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		// adapter
		mFileAdapter = new FileAdapter(mContext);
		mListView.setAdapter(mFileAdapter);
		mFileAdapter.setSelection(-1);

		setBackgroundDrawable(mContext.getResources().getDrawable(android.R.drawable.alert_dark_frame));
		// setBackgroundDrawable(new ColorDrawable());
		setContentView(mView);
		// 设置PopupWindow可获得焦点
		setFocusable(true);
		// 设置PopupWindow可触摸
		setTouchable(true);
		// 设置非PopupWindow区域可触摸
		setOutsideTouchable(false);

		setFullscreen();
	}

	/**
	 * 适应内容
	 */
	public void setWrapContent() {
		setWidth(LayoutParams.WRAP_CONTENT);
		setHeight(LayoutParams.WRAP_CONTENT);
	}

	/**
	 * 全屏
	 */
	public void setFullscreen() {
		setWidth(LayoutParams.MATCH_PARENT);
		setHeight(LayoutParams.MATCH_PARENT);
	}

	public void setCurrentFile(File file) {
		mFileAdapter.setSelection(-1);
		if (file == null || !file.exists())
			file = new File(mRootPath);
		if (!file.isDirectory())
			file = file.getParentFile();
		mCurrentFile = file;
		mPath.setText(mCurrentFile.getAbsolutePath());
		mFileAdapter.setFile(mCurrentFile);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mFileAdapter.setSelection(position);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		File selectFile = (File) mListView.getItemAtPosition(position);
		if (selectFile.isDirectory()) {
			setCurrentFile(selectFile);
			return true;
		}
		return false;
	}

	private OnSelectListener mOnSelectListener;

	/**
	 * 设置item点击事件
	 */
	public void setOnSelectListener(OnSelectListener listener) {
		mOnSelectListener = listener;
	}

	/**
	 * 点击监听
	 * 
	 * @author Zengsc
	 */
	public interface OnSelectListener {
		void select(String path);
	}

	/**
	 * 取得选中项数据
	 */
	public Object getSelectedItem() {
		return mFileAdapter.getItem(mFileAdapter.getSeleciton());
	}

	@Override
	public void onClick(View v) {
		if (v == mBack) {
			if (mCurrentFile == null || mCurrentFile.getParentFile() == null || mCurrentFile.getAbsolutePath().equals(mRootPath)) {
				if (isShowing())
					dismiss();
			} else {
				setCurrentFile(mCurrentFile.getParentFile());
			}
		} else if (v == mOk) {
			if (mOnSelectListener != null) {
				if (mFileAdapter.getSeleciton() < 0)
					mOnSelectListener.select(mCurrentFile.getAbsolutePath());
				else
					mOnSelectListener.select(mFileAdapter.getItem(mFileAdapter.getSeleciton()).getAbsolutePath());
			}
			if (isShowing())
				dismiss();
		}
	}

	public void setRoot(String root) {
		mRootPath = root;
	}

	/**
	 * @author zengsc
	 * @version date 2013-6-8
	 */
	public class FileAdapter extends BaseAdapter {
		final Context mContext;
		File[] mFiles;
		private int mSelection;

		public FileAdapter(Context context) {
			mContext = context;
		}

		class Holder {
			ImageView icon;
			TextView name;
			TextView info;
		}

		/**
		 * 设置显示的文件夹
		 */
		public void setFile(File file) {
			if (file == null || !file.exists() || !file.isDirectory())
				return;
			mFiles = file.listFiles();
			notifyDataSetChanged();
		}

		/**
		 * set selected position
		 */
		public void setSelection(int position) {
			mSelection = position;
			notifyDataSetChanged();
		}

		/**
		 * get selected position
		 */
		public int getSeleciton() {
			return mSelection;
		}

		@Override
		public int getCount() {
			if (mFiles == null)
				return 0;
			return mFiles.length;
		}

		@Override
		public File getItem(int position) {
			if (position < 0 || position >= getCount())
				return null;
			return mFiles[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder;
			if (convertView == null) {
				convertView = View.inflate(mContext, R.layout.file_selector_item, null);
				holder = new Holder();
				convertView.setTag(holder);
				holder.icon = (ImageView) convertView.findViewById(R.id.file_selector_item_icon);
				holder.name = (TextView) convertView.findViewById(R.id.file_selector_item_name);
				holder.info = (TextView) convertView.findViewById(R.id.file_selector_item_info);
				// LinearLayout layout = new LinearLayout(mContext);
				// convertView = layout;
				// holder = new Holder();
				// convertView.setTag(holder);
				// layout.setOrientation(LinearLayout.HORIZONTAL);
				// holder.icon = new ImageView(mContext);
				// layout.addView(holder.icon);
				// holder.name = new TextView(mContext);
				// LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
				// LinearLayout.LayoutParams.WRAP_CONTENT);
				// nameParams.weight = 1;
				// layout.addView(holder.name, nameParams);
				// holder.info = new TextView(mContext);
				// layout.addView(holder.info);
			} else {
				holder = (Holder) convertView.getTag();
			}
			if (mSelection == position) {
				convertView.setBackgroundResource(android.R.color.holo_blue_dark);
			} else {
				convertView.setBackgroundResource(android.R.color.transparent);
			}
			File file = getItem(position);
			holder.icon.setImageResource(file.isDirectory() ? R.drawable.folder : R.drawable.document);
			holder.name.setText(file.getName());
			holder.info.setText("" + file.length());
			return convertView;
		}

	}
}
