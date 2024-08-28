package com.softwinner.runin.config;

import java.io.File;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rogge.FileSelector;
import com.rogge.PopupList;
import com.softwinner.runin.R;
import com.softwinner.runin.Settings;
import com.softwinner.runin.config.interfaces.IConfiguration;
import com.softwinner.xml.Attribute;
import com.softwinner.xml.Node;

/**
 * @author zengsc
 * @version date 2013-6-7
 */
public class TestConfiguration {
    static final String TAG = "Runin-TestConfiguration";
    static final boolean DEBUG = Settings.DEBUG;
    private final Context mContext;
    private final ViewGroup mContainer;
    private final TitleView mTitleView;
    private final PopupList mForegroundList;
    private final String[] FOREGROUND_LIST;
    private final FileSelector mFileSelector;
    private Node mData;

    public TestConfiguration(Context context, ViewGroup configViewGroup) {
        mContext = context;
        mTitleView = new TitleView(configViewGroup);
        mContainer = (ViewGroup) configViewGroup.findViewById(R.id.config_container);
        mForegroundList = new PopupList(mContext);
        IConfiguration[] fores = Settings.getForegroundConfig();
        FOREGROUND_LIST = new String[fores.length];
        for (int i = 0; i < fores.length; i++) {
            FOREGROUND_LIST[i] = fores[i].name();
        }
        mFileSelector = new FileSelector(mContext);
        mFileSelector.setRoot("/mnt");
    }

    /**
     * @author zengsc
     * @version date 2013-6-7
     */
    class TitleView {
        final TextView mTitle;
        final TextView mState;

        public TitleView(View view) {
            mTitle = (TextView) view.findViewById(R.id.config_title);
            mState = (TextView) view.findViewById(R.id.config_state_text);
            // mTitle.setBackgroundResource(android.R.color.holo_blue_dark);
        }

        /**
         * set title
         */
        public void setTitle(String title, boolean fore) {
            mTitle.setText(title);
            mState.setText(fore ? R.string.type_foreground : R.string.type_background);
            mState.setBackgroundResource(fore ? android.R.color.holo_green_light : android.R.color.holo_orange_light);
        }

        /**
         * get title
         */
        public String getTitle() {
            return mTitle.getText().toString().trim();
        }
    }

    /**
     * @author zengsc
     * @version date 2013-6-7
     */
    class AttrView implements TextWatcher {
        final View mView;
        final TextView mLabel;
        final EditText mValue;
        final TextView mUnit;
        Attribute mAttribute;

        public AttrView(Context context) {
            mView = View.inflate(context, R.layout.config_attr, null);
            mLabel = (TextView) mView.findViewById(R.id.attr_label);
            mValue = (EditText) mView.findViewById(R.id.attr_value);
            mUnit = (TextView) mView.findViewById(R.id.attr_unit);
            mValue.addTextChangedListener(this);
            mView.setTag(this);
        }

        /**
         * set value
         */
        public void setValue(Attribute attribute) {
            mAttribute = attribute;
            String label = attribute.getName();
            mLabel.setText(label);
            mValue.setText(attribute.getValue());
            int resid = R.string.unit_none;
            if (Settings.ATTR_DURATION.equals(label) || Settings.ATTR_DELAY.equals(label)) {
                resid = R.string.unit_min;
            } else if (Settings.ATTR_OPEN_DURATION.equals(label) || Settings.ATTR_CLOSE_DURATION.equals(label)) {
                resid = R.string.unit_s;
            } else if (Settings.ATTR_REPEAT_COUNT.equals(label)) {
                resid = R.string.unit_times;
            }else if(Settings.ATTR_MEMSIZE.equals(label)){
                resid = R.string.unit_mb_yhk;
            }
            mUnit.setText(resid);
        }

        /**
         * get view
         */
        public View get() {
            return mView;
        }

        /**
         * get type
         */
        public String getLabel() {
            return mLabel.getText().toString().trim();
        }

        /**
         * get value
         */
        public String getValue() {
            return mValue.getText().toString().trim();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (DEBUG)
                Log.d(TAG, "Attribute:" + s);
            mAttribute.setValue(s.toString());
        }
    }

    /**
     * @author zengsc
     * @version date 2013-6-7
     */
    class NodeTitleView implements View.OnClickListener {
        final View mView;
        final TextView mTitle;
        final Button mAdd;
        IConfiguration.NodeProperties mProperties;

        public NodeTitleView(Context context) {
            mView = View.inflate(mContext, R.layout.config_node_title, null);
            mTitle = (TextView) mView.findViewById(R.id.node_title);
            mAdd = (Button) mView.findViewById(R.id.node_add_button);
            mAdd.setOnClickListener(this);
            mView.setTag(this);
        }

        /**
         * set properties
         */
        public void setProp(IConfiguration.NodeProperties prop) {
            mProperties = prop;
            mTitle.setText(prop.getName());
        }

        /**
         * get view
         */
        public View get() {
            return mView;
        }

        /**
         * get title
         */
        public String getTitle() {
            return mTitle.getText().toString().trim();
        }

        @Override
        public void onClick(View v) {
            if (v == mAdd) {
                if (mView.getParent() != null) {
                    int index = ((ViewGroup) mView.getParent()).indexOfChild(mView);
                    addNodeItem(getTitle(), mProperties.getType(), index + 1);
                }
            }
        }
    }

    /**
     * @author zengsc
     * @version date 2013-6-8
     */
    class NodeItemView implements TextWatcher, View.OnClickListener {
        final View mView;
        final TextView mLabel;
        final EditText mValue;
        final Button mSelect;
        final Button mDelete;
        Node mNode;
        IConfiguration.NodeProperties.TYPE mType;
        FileSelector.OnSelectListener PATH_SELECTLISTENER = new FileSelector.OnSelectListener() {

            @Override
            public void select(String path) {
                mValue.setText(path);
            }
        };
        PopupList.OnItemListener FOREGROUNDTEST_ITEMLISTENER = new PopupList.OnItemListener() {

            @Override
            public boolean itemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = (String) parent.getItemAtPosition(position);
                mValue.setText(name);
                return true;
            }
        };

        public NodeItemView(Context context) {
            mView = View.inflate(mContext, R.layout.config_node_item, null);
            mLabel = (TextView) mView.findViewById(R.id.node_label);
            mValue = (EditText) mView.findViewById(R.id.node_value);
            mSelect = (Button) mView.findViewById(R.id.node_select_button);
            mDelete = (Button) mView.findViewById(R.id.node_delete_button);
            mValue.addTextChangedListener(this);
            mSelect.setOnClickListener(this);
            mDelete.setOnClickListener(this);
            mView.setTag(this);
        }

        /**
         * set value
         */
        public void setValue(Node node, IConfiguration.NodeProperties.TYPE type) {
            mNode = node;
            mType = type;
            mLabel.setText(node.getName());
            mValue.setText(node.getValue());
            if (IConfiguration.NodeProperties.TYPE.STRING == type) {
                mSelect.setVisibility(View.GONE);
            } else {
                mSelect.setVisibility(View.VISIBLE);
            }
        }

        /**
         * get view
         */
        public View get() {
            return mView;
        }

        /**
         * get label
         */
        public String getLabel() {
            return mLabel.getText().toString().trim();
        }

        /**
         * get value
         */
        public String getValue() {
            return mValue.getText().toString().trim();
        }

        @Override
        public void onClick(View v) {
            if (v == mSelect) {
                if (IConfiguration.NodeProperties.TYPE.PATH == mType) {
                    String path = getValue();
                    mFileSelector.setCurrentFile(new File(path));
                    mFileSelector.setOnSelectListener(PATH_SELECTLISTENER);
                    mFileSelector.showAtLocation(mContainer, Gravity.CENTER, 0, 0);
                    Toast.makeText(mContext, R.string.file_selector_tip, Toast.LENGTH_SHORT).show();
                } else if (IConfiguration.NodeProperties.TYPE.FOREGROUNDTEST == mType) {
                    mForegroundList.setAdapter(new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1,
                            FOREGROUND_LIST));
                    mForegroundList.setOnItemListener(FOREGROUNDTEST_ITEMLISTENER);
                    // show dropdown for wrapcontent params
                    final int[] screenLocation = new int[2];
                    v.getLocationOnScreen(screenLocation);
                    final Rect displayFrame = new Rect();
                    v.getWindowVisibleDisplayFrame(displayFrame);
                    int point = displayFrame.bottom - displayFrame.top - v.getHeight();
                    point >>= 1;
                    if (screenLocation[1] < point) {
                        mForegroundList.showAtLocation(mContainer, Gravity.START | Gravity.TOP, screenLocation[0],
                                screenLocation[1] + v.getHeight());
                    } else {
                        mForegroundList.showAtLocation(mContainer, Gravity.START | Gravity.BOTTOM, screenLocation[0],
                                displayFrame.bottom - displayFrame.top - screenLocation[1]);
                    }
                } else {
                    Log.w(TAG, "Unhandle NodeProperties type");
                    // TODO other type
                }
            } else if (v == mDelete) {
                removeNodeItem(mNode, mView);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (DEBUG)
                Log.d(TAG, "NodeItem:" + s);
            mNode.setValue(s.toString().trim());
        }
    }

    class NodeSingleItemView extends NodeItemView {
        public NodeSingleItemView(Context context) {
            super(context);
            mDelete.setVisibility(View.GONE);
        }
    }

    void addNodeItem(String label, IConfiguration.NodeProperties.TYPE type, int index) {
        Node item = new Node(label);
        mData.addNode(item);
        NodeItemView nodeItemView = new NodeItemView(mContext);
        nodeItemView.setValue(item, type);
        mContainer.addView(nodeItemView.get(), index);
    }

    void removeNodeItem(Node item, View view) {
        mData.removeNode(item);
        mContainer.removeView(view);
    }

    /**
     * @param config test config
     * @param fore   is foreground
     * @param data   node data
     */
    public void setConfig(IConfiguration config, boolean fore, Node data) {
        // set title
        mTitleView.setTitle(config.name(), fore);
        if (mData == data)
            return;
        mData = data;
        mContainer.removeAllViews();
        if (data == null)
            return;
        // add attrs
        if (config.attrs() != null) {
            // View space = new View(mContext);
            // space.setBackgroundResource(android.R.color.holo_blue_dark);
            // ViewGroup.LayoutParams spaceParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            // mContainer.addView(space, spaceParams);
            for (String attr : config.attrs()) {
                if (TextUtils.isEmpty(attr))
                    continue;
                Attribute attribute = data.getAttribute(attr);
                if (attribute == null) {
                    attribute = new Attribute(attr, null);
                    data.addAttribute(attribute);
                }
                AttrView attrView = new AttrView(mContext);
                attrView.setValue(attribute);
                mContainer.addView(attrView.get());
            }
        }
        if (config.nodes() != null) {
            View space = new View(mContext);
            space.setBackgroundResource(android.R.color.holo_blue_light);
            ViewGroup.LayoutParams spaceParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            mContainer.addView(space, spaceParams);
            for (IConfiguration.NodeProperties prop : config.nodes()) {
                if (prop.multi()) {
                    // add node title
                    NodeTitleView nodeTitleView = new NodeTitleView(mContext);
                    nodeTitleView.setProp(prop);
                    mContainer.addView(nodeTitleView.get());
                    int size = data.getNNodes();
                    for (int i = 0; i < size; i++) {
                        // add node item
                        Node item = data.getNode(i);
                        if (prop.getName().equals(item.getName())) {
                            NodeItemView nodeItemView = new NodeItemView(mContext);
                            nodeItemView.setValue(item, prop.getType());
                            mContainer.addView(nodeItemView.get());
                        }
                    }
                } else {
                    // single node
                    NodeSingleItemView nodeItemView = new NodeSingleItemView(mContext);
                    mContainer.addView(nodeItemView.get());
                    Node node = null;
                    int size = data.getNNodes();
                    for (int i = 0; i < size; i++) {
                        Node item = data.getNode(i);
                        if (prop.getName().equals(item.getName())) {
                            node = item;
                            break;
                        }
                    }
                    if (node == null) {
                        node = new Node(prop.getName());
                        mData.addNode(node);
                    }
                    nodeItemView.setValue(node, prop.getType());
                }
            }
        }
    }
}
