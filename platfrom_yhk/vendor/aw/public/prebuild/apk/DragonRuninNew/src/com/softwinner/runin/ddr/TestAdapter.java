package com.softwinner.runin.ddr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.lenovo.dramtest.DRAMTest;
import com.softwinner.runin.R;

import java.util.List;

/* loaded from: classes.dex */
public class TestAdapter extends ArrayAdapter<DRAMTest> {
    private final int layout;

    public TestAdapter(Context context, int i, List<DRAMTest> list) {
        super(context, i, list);
        this.layout = i;
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        DRAMTest item = getItem(i);
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(this.layout, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.textFileSize = (TextView) view.findViewById(R.id.fileSize);
            viewHolder.textCurrentCycles = (TextView) view.findViewById(R.id.cycles);
            viewHolder.textState = (TextView) view.findViewById(R.id.state);
            viewHolder.textErrorCount = (TextView) view.findViewById(R.id.errorCount);
            viewHolder.textErrorData = (TextView) view.findViewById(R.id.errorData);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.textCurrentCycles.setText(item.getCyclesInfo());
        viewHolder.textFileSize.setText((item.getFileSize() / 1048576) + " MB");
        viewHolder.textState.setText(getStatusText(item.getStatus()));
        viewHolder.textErrorData.setText(item.getErrorData());
        TextView textView = viewHolder.textErrorCount;
        textView.setText(item.getErrorCount() + "");
        return view;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.clock.pt1.keeptesting.ddr.TestAdapter$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$example$lenovo$dramtest$DRAMTest$Status;

        static {
            int[] iArr = new int[DRAMTest.Status.values().length];
            $SwitchMap$com$example$lenovo$dramtest$DRAMTest$Status = iArr;
            try {
                iArr[DRAMTest.Status.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$example$lenovo$dramtest$DRAMTest$Status[DRAMTest.Status.TESTING.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$example$lenovo$dramtest$DRAMTest$Status[DRAMTest.Status.FINISH.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
        }
    }

    private int getStatusText(DRAMTest.Status status) {
        int i = AnonymousClass1.$SwitchMap$com$example$lenovo$dramtest$DRAMTest$Status[status.ordinal()];
        return i != 2 ? i != 3 ? R.string.ddr_test_no_test_running_text : R.string.ddr_test_success_text : R.string.ddr_test_testing_status_doing;
    }

    /* loaded from: classes.dex */
    private static class ViewHolder {
        TextView textCurrentCycles;
        TextView textErrorCount;
        TextView textErrorData;
        TextView textFileSize;
        TextView textState;

        ViewHolder() {
        }
    }
}