package com.rogge;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

/**
 * 弹出列表
 * 
 * @author Zengsc
 */
public class PopupList extends PopupWindow implements AdapterView.OnItemClickListener {
	final Context mContext;
	private ListView mListView;
	private int mSelectedPosition;
	private int mMaxRows = 0;

	public PopupList(Context context) {
		super(context);
		mContext = context;

		init();
	}

	private void init() {
		mSelectedPosition = -1;

		mListView = new ListView(mContext) {
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
				View child = getChildAt(0);
				if (mMaxRows > 0 && child != null) {
					int tmpHeight = child.getHeight() * mMaxRows + getDividerHeight() * (mMaxRows - 1)
							+ getPaddingTop() + getPaddingBottom();
					tmpHeight = Math.min(tmpHeight, MeasureSpec.getSize(heightMeasureSpec));
					heightMeasureSpec = MeasureSpec.getMode(heightMeasureSpec);
					heightMeasureSpec += tmpHeight;
				}
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
		};
		mListView.setDivider(null);
		// mListView.setBackgroundResource(android.R.drawable.alert_light_frame);
		mListView.setCacheColorHint(Color.TRANSPARENT);
		// mListView.setSelector(new ColorDrawable());
		// mListView.setVerticalScrollBarEnabled(false);

		mListView.setOnItemClickListener(this);

		setBackgroundDrawable(mContext.getResources().getDrawable(android.R.drawable.alert_dark_frame));
		// setBackgroundDrawable(new ColorDrawable());
		setContentView(mListView);
		// 设置PopupWindow可获得焦点
		setFocusable(true);
		// 设置PopupWindow可触摸
		setTouchable(true);
		// 设置非PopupWindow区域可触摸
		setOutsideTouchable(false);

		setWrapContent();
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

	public ListAdapter getAdapter() {
		return mListView.getAdapter();
	}

	public void setAdapter(ListAdapter adapter) {
		mListView.setAdapter(adapter);
	}

	/**
	 * 设置最高显示行数
	 */
	public void setMaxRows(int rows) {
		mMaxRows = rows;
		mListView.requestLayout();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mSelectedPosition = position;
		boolean dismiss = true;
		if (mOnItemListener != null)
			dismiss = mOnItemListener.itemClick(parent, view, position, id);
		if (isShowing() && dismiss)
			dismiss();
	}

	private OnItemListener mOnItemListener;

	/**
	 * 设置item点击事件
	 */
	public void setOnItemListener(OnItemListener listener) {
		mOnItemListener = listener;
	}

	/**
	 * 点击监听
	 * 
	 * @author Zengsc
	 */
	public interface OnItemListener {
		/**
		 * @param parent
		 * @param view
		 * @param position
		 * @param id
		 * @return dismiss list
		 */
		boolean itemClick(AdapterView<?> parent, View view, int position, long id);
	}

	/**
	 * 取得选中项数据
	 */
	public Object getSelectedItem() {
		return mListView.getItemAtPosition(mSelectedPosition);
	}

	/**
	 * 取得选中项的位置
	 */
	public int getSelectedPosition() {
		return mSelectedPosition;
	}

	/**
	 * 设置选中，会触发点击事件
	 */
	public void setSelection(int position) {
		mSelectedPosition = position;
		mListView.setSelection(position);
		if (mOnItemListener != null) {
			mOnItemListener.itemClick(mListView, null, position, mListView.getAdapter().getItemId(position));
		}
	}
}
