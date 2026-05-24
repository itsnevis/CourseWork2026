package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";
    private static final String AUTH_URL = "https://android-for-students.ru/coursework/login.php";

    private TextInputEditText etLogin;
    private TextInputEditText etPassword;
    private TextInputEditText etGroup;
    private TextInputLayout tilLogin;
    private TextInputLayout tilPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        etLogin    = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        etGroup    = findViewById(R.id.etGroup);
        tilLogin   = findViewById(R.id.tilLogin);
        tilPassword = findViewById(R.id.tilPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.authProgress);

        etGroup.setText(getString(R.string.default_group));

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String login    = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String group    = etGroup.getText().toString().trim();

        if (!validateInputs(login, password, group)) return;

        setLoadingState(true);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("lgn", login)
                .add("pwd", password)
                .add("g", group)
                .build();

        Request request = new Request.Builder()
                .url(AUTH_URL)
                .post(body)
                .build();

        Log.d(TAG, "Отправка запроса авторизации для: " + login);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Ошибка сети: " + e.getMessage());
                runOnUiThread(() -> {
                    setLoadingState(false);
                    showToast(getString(R.string.error_network) + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Ответ сервера: " + responseBody);
                runOnUiThread(() -> handleServerResponse(responseBody));
            }
        });
    }

    private void handleServerResponse(String responseBody) {
        setLoadingState(false);
        try {
            JSONObject json = new JSONObject(responseBody);
            int resultCode = json.getInt("result_code");

            if (resultCode == 0) {
                Log.d(TAG, "Авторизация успешна");
                String variant = json.optString("variant", "");
                String task    = json.optString("task", "");

                Intent intent = new Intent(this, TaskActivity.class);
                intent.putExtra("variant", variant);
                intent.putExtra("task", task);
                startActivity(intent);
                finish();
            } else {
                String errorMsg = json.optString("error", getString(R.string.error_auth));
                Log.w(TAG, "Ошибка авторизации, код: " + resultCode + ", " + errorMsg);
                showToast(getString(R.string.error_auth_prefix) + errorMsg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка разбора ответа: " + e.getMessage() + " | Ответ: " + responseBody);
            showToast(getString(R.string.error_parse));
        }
    }

    private boolean validateInputs(String login, String password, String group) {
        boolean valid = true;

        tilLogin.setError(null);
        tilPassword.setError(null);

        if (login.isEmpty()) {
            tilLogin.setError(getString(R.string.error_empty_login));
            valid = false;
        } else if (login.length() < 3) {
            tilLogin.setError(getString(R.string.error_short_login));
            valid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_empty_password));
            valid = false;
        } else if (password.length() < 4) {
            tilPassword.setError(getString(R.string.error_short_password));
            valid = false;
        }

        if (group.isEmpty()) {
            showToast(getString(R.string.error_empty_group));
            valid = false;
        }

        return valid;
    }

    private void setLoadingState(boolean loading) {
        btnLogin.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}