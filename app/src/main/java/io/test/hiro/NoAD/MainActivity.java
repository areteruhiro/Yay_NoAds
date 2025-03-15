package io.test.hiro.NoAD;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String CONFIG_FILE_NAME = "ad_config.txt";
    private static final String DIRECTORY_NAME = "NoAd Module";
    private String currentSelectedFileName;
    private Switch logSwitch;
    private EditText adClassesEditText;
    private Button saveButton;
    private Button loadFilesButton;
    private RecyclerView filesRecyclerView;
    private FilesAdapter filesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ストレージ権限リクエスト
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUIComponents();
        setupLogSwitch();
        setupSaveButton();
        setupLoadFilesButton();
        loadAdConfigFiles(); // アプリ起動時にファイルをロード
    }

    private void initializeUIComponents() {
        logSwitch = findViewById(R.id.logSwitch);
        adClassesEditText = findViewById(R.id.adClassesEditText);
        saveButton = findViewById(R.id.saveButton);
        loadFilesButton = findViewById(R.id.loadFilesButton);
        filesRecyclerView = findViewById(R.id.filesRecyclerView);
        filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        filesAdapter = new FilesAdapter(new ArrayList<>(), this::onFileClick);
        filesRecyclerView.setAdapter(filesAdapter);
    }

    private void setupLogSwitch() {
        logSwitch.setChecked(getLogFile().exists());
        logSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkStoragePermission()) {
                handleLogSwitchChange(isChecked);
            }
        });
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                saveAdClassesToFile(adClassesEditText.getText().toString());
                restartApp();
            }
        });
    }

    private void setupLoadFilesButton() {
        loadFilesButton.setOnClickListener(v -> loadAdConfigFiles());
    }

    private void loadAdConfigFiles() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME);
        File[] files = directory.listFiles((dir, name) -> name.endsWith("_ad_config.txt"));
        if (files != null) {
            List<String> fileNames = new ArrayList<>();
            for (File file : files) {
                fileNames.add(file.getName());
            }
            filesAdapter.updateFileList(fileNames);
        } else {
            showToast("No config files found");
        }
    }

    private void onFileClick(String fileName) {
        currentSelectedFileName = fileName; // 選択ファイル名を保持
        File configFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                DIRECTORY_NAME + File.separator + fileName);
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(configFile), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            adClassesEditText.setText(content.toString().trim());


            TextView selectedFileTextView = findViewById(R.id.selectedFileTextView);
            selectedFileTextView.setText("Editing: " + fileName);

        } catch (IOException e) {
            handleFileError(e);
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void handleLogSwitchChange(boolean isChecked) {
        File logFile = getLogFile();
        if (isChecked) {
            createLogFile(logFile);
        } else {
            deleteLogFile(logFile);
        }
    }

    private void createLogFile(File logFile) {
        try {
            if (!logFile.getParentFile().exists() && !logFile.getParentFile().mkdirs()) {
                showToast("Failed to create directory");
                return;
            }
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile), "UTF-8"))) {
                writer.write("Logging enabled");
                showToast("Log ON");
            }
        } catch (IOException e) {
            handleFileError(e);
        }
    }

    private void deleteLogFile(File logFile) {
        if (logFile.exists() && logFile.delete()) {
            showToast("Log OFF");
        } else {
            showToast("No log file to delete");
        }
    }


    private void saveAdClassesToFile(String adClasses) {
        // 現在選択中のファイル名があればそれを使用、なければデフォルト
        String fileName = (currentSelectedFileName != null) ?
                currentSelectedFileName : CONFIG_FILE_NAME;

        File configFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                DIRECTORY_NAME + File.separator + fileName);

        try {
            if (!configFile.getParentFile().exists() &&
                    !configFile.getParentFile().mkdirs()) {
                showToast("Failed to create directory");
                return;
            }

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"))) {

                writer.write(adClasses);
                showToast("Saved to: " + fileName);

                // 保存後にファイルリストを更新
                loadAdConfigFiles();

            }
        } catch (IOException e) {
            handleFileError(e);
        }
    }


    private File getConfigFile() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME + File.separator + CONFIG_FILE_NAME);
    }

    private File getLogFile() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME + File.separator + "log.txt");
    }

    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        finishAffinity();
    }

    private void handleFileError(Exception e) {
        e.printStackTrace();
        showToast("File operation failed");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {
        private List<String> fileNames;
        private final OnFileClickListener onFileClickListener;

        public FilesAdapter(List<String> fileNames, OnFileClickListener onFileClickListener) {
            this.fileNames = fileNames;
            this.onFileClickListener = onFileClickListener;
        }

        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            String fileName = fileNames.get(position);
            holder.bind(fileName, onFileClickListener);
        }

        @Override
        public int getItemCount() {
            return fileNames.size();
        }

        public void updateFileList(List<String> newFileNames) {
            this.fileNames = newFileNames;
            notifyDataSetChanged();
        }

        class FileViewHolder extends RecyclerView.ViewHolder {
            private final TextView textView;

            public FileViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }

            public void bind(String fileName, OnFileClickListener listener) {
                textView.setText(fileName);
                itemView.setOnClickListener(v -> listener.onFileClick(fileName));
            }
        }
    }

    interface OnFileClickListener {
        void onFileClick(String fileName);
    }
}