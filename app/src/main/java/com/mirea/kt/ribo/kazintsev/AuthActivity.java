package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.mirea.kt.ribo.kazintsev.R;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        TextInputEditText etLogin = findViewById(R.id.etLogin);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        TextInputEditText etGroup = findViewById(R.id.etGroup);
        Button btn = findViewById(R.id.btnLogin);
        etGroup.setText("RIBO-04-24");

        btn.setOnClickListener(v -> {
            String l = etLogin.getText().toString().trim();
            String p = etPassword.getText().toString().trim();
            String g = etGroup.getText().toString().trim();

            String url = "https://android-for-students.ru/coursework/login.php";
            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("lgn", l)
                    .add("pwd", p)
                    .add("g", g)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e("AUTH_ERR", e.toString());
                        Toast.makeText(
                                AuthActivity.this,
                                "Ошибка сети: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String res = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            Log.d("SERVER_RESPONSE", res);
                            JSONObject jo = new JSONObject(res);
                            if (jo.getInt("result_code") == 0) {
                                Intent i = new Intent(AuthActivity.this, TaskActivity.class);
                                i.putExtra("variant", jo.getString("variant"));
                                i.putExtra("task", jo.getString("task"));
                                startActivity(i);
                                finish();
                            } else {
                                Toast.makeText(
                                        AuthActivity.this,
                                        "Ошибка сервера: "
                                                + jo.optInt("result_code"),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        } catch (Exception e) {
                            Log.e("JSON_ERR", res);
                            Toast.makeText(
                                    AuthActivity.this,
                                    "Ошибка JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                }
            });
        });
    }
}