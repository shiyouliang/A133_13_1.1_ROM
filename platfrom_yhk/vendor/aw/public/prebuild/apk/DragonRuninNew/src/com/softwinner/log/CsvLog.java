package com.softwinner.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import android.util.SparseArray;

import com.softwinner.runin.Settings;
import com.softwinner.runin.config.interfaces.IConfiguration;

/**
 * @author zengsc
 * @version date 2013-6-13
 */
public class CsvLog {

	// 以下为csv
	private static final String CSV_FILE_PATH = Settings.LOGCAT_PATH + File.separator + "runinlog.csv";
	private static File mCsvFile;
	private static CSVAnalysis mCSVAnalysis;
	private static String[] mTitle;
	private static SparseArray<String[]> mCsvArrays;
       
        public static void clearCsv(){
            mCsvFile = new File(CSV_FILE_PATH);
            if(mCsvFile.exists()){
                mCsvFile.delete();
            }
	}

	/**
	 * 读取csv
	 */
	public static void loadCsv(boolean reset) {
		mCsvFile = new File(CSV_FILE_PATH);
		mCSVAnalysis = new CSVAnalysis();
		mCsvArrays = new SparseArray<String[]>();
		IConfiguration[] configs = Settings.getForegroundConfig();
		mTitle = new String[configs.length];
		for (int i = 0; i < configs.length; i++) {
			mTitle[i] = configs[i].name();
		}
		mCsvArrays.put(0, mTitle);
		if (mCsvFile.exists() && !reset) {
			Reader reader = null;
			try {
				reader = new BufferedReader(new FileReader(mCsvFile));
				List<List<String>> csv = mCSVAnalysis.readCSVFile(reader);
				int size = csv.size();
				for (int i = 1; i < size; i++) {
					List<String> csvLine = csv.get(i);
					if (csvLine != null) {
						String[] csvLineArray = new String[mTitle.length];
						csvLine.toArray(csvLineArray);
						mCsvArrays.put(i, csvLineArray);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			if (!mCsvFile.getParentFile().exists())
				mCsvFile.getParentFile().mkdirs();
			try {
				if (!mCsvFile.exists())
					mCsvFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			saveCsv();
		}
	}

	/**
	 * 设置结果
	 */
	public static void setResult(int cycle, String name, String result) {
		setResultInner(cycle, name, result);
		saveCsv();
	}

	private static void setResultInner(int cycle, String name, String result) {
		int index = -1;
		for (int i = 0; i < mTitle.length; i++) {
			if (mTitle[i].equals(name)) {
				index = i;
				break;
			}
		}
		if (mCsvArrays == null || mTitle == null || index < 0)
			return;
		String[] csvLine = mCsvArrays.get(cycle);
		if (csvLine == null) {
			csvLine = new String[mTitle.length];
			for (int i = 0; i < mTitle.length; i++) {
				csvLine[i] = "NA";
			}
			mCsvArrays.put(cycle, csvLine);
		}
		csvLine[index] = result;
	}

	/**
	 * 保存csv文件
	 */
	public static void saveCsv() {
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(mCsvFile));
			byte bom[] = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
			writer.write(new String(bom));
			mCSVAnalysis.writeCSVFile(mCsvArrays, writer);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 获取日志文件
	 */
	public static SparseArray<String[]> getCsvArrays() {
		return mCsvArrays;
	}
}
