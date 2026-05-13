package com.example.ufswriteperf;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int TOTAL_BYTES = 2 * 1024 * 1024;
    private static final int CHUNK_BYTES = 4096;

    private Spinner spinnerPreset;
    private EditText editPath;
    private Button btnRun;
    private TextView txtStatus;
    private LineChartView chartView;

    private final List<PresetPath> presets = new ArrayList<PresetPath>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerPreset = (Spinner) findViewById(R.id.spinnerPreset);
        editPath = (EditText) findViewById(R.id.editPath);
        btnRun = (Button) findViewById(R.id.btnRun);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        chartView = (LineChartView) findViewById(R.id.chartView);

        initPresets();
        setupPresetSelector();

        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest();
            }
        });
    }

    private void initPresets() {
        presets.clear();
        presets.add(new PresetPath("私有文件目录", new File(getFilesDir(), "perf_test.bin").getAbsolutePath()));
        presets.add(new PresetPath("私有缓存目录", new File(getCacheDir(), "perf_test.bin").getAbsolutePath()));

        File extFiles = getExternalFilesDir(null);
        if (extFiles != null) {
            presets.add(new PresetPath("外部私有文件目录", new File(extFiles, "perf_test.bin").getAbsolutePath()));
        }

        File sharedDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        presets.add(new PresetPath("共享目录(Downloads)", new File(sharedDir, "perf_test.bin").getAbsolutePath()));
    }

    private void setupPresetSelector() {
        List<String> names = new ArrayList<String>();
        for (PresetPath preset : presets) {
            names.add(preset.label);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPreset.setAdapter(adapter);

        spinnerPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editPath.setText(presets.get(position).path);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (!presets.isEmpty()) {
            editPath.setText(presets.get(0).path);
        }
    }

    private void startTest() {
        final String targetPath = editPath.getText().toString().trim();
        if (targetPath.length() == 0) {
            txtStatus.setText("路径不能为空");
            return;
        }

        chartView.setPoints(new ArrayList<LineChartView.DataPoint>());
        txtStatus.setText("测试中...");
        btnRun.setEnabled(false);

        new WritePerfTask().execute(targetPath);
    }

    private class WritePerfTask extends AsyncTask<String, String, List<LineChartView.DataPoint>> {
        private long totalTimeMs;

        @Override
        protected List<LineChartView.DataPoint> doInBackground(String... params) {
            String path = params[0];
            List<LineChartView.DataPoint> points = new ArrayList<LineChartView.DataPoint>();

            byte[] chunk = new byte[CHUNK_BYTES];
            new Random().nextBytes(chunk);

            FileOutputStream outputStream = null;
            long startNs = System.nanoTime();
            int written = 0;

            try {
                File file = new File(path);
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                outputStream = new FileOutputStream(file, false);

                while (written < TOTAL_BYTES) {
                    outputStream.write(chunk);
                    written += CHUNK_BYTES;
                    long elapsedMs = (System.nanoTime() - startNs) / 1000000L;
                    points.add(new LineChartView.DataPoint(written, elapsedMs));
                }

                outputStream.flush();
                outputStream.getFD().sync();
                totalTimeMs = (System.nanoTime() - startNs) / 1000000L;
            } catch (Exception e) {
                publishProgress("测试失败: " + e.getMessage());
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception ignored) {
                    }
                }
            }

            return points;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            txtStatus.setText(values[0]);
        }

        @Override
        protected void onPostExecute(List<LineChartView.DataPoint> points) {
            chartView.setPoints(points);
            btnRun.setEnabled(true);
            if (points.isEmpty()) {
                txtStatus.setText("未生成有效数据，请检查路径/权限");
            } else {
                txtStatus.setText(String.format(Locale.US,
                        "完成: 共写入 %.2f MB, 总耗时 %d ms, 采样点 %d",
                        TOTAL_BYTES / 1024f / 1024f,
                        totalTimeMs,
                        points.size()));
            }
        }
    }

    private static class PresetPath {
        final String label;
        final String path;

        PresetPath(String label, String path) {
            this.label = label;
            this.path = path;
        }
    }
}
