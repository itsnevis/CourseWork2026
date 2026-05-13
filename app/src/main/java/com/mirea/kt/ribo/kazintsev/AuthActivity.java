package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        TextInputEditText etLogin = findViewById(R.id.etLogin);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        TextInputEditText etGroup = findViewById(R.id.etGroup);
        Button btn = findViewById(R.id.btnLogin);
        etGroup.setText("РИБО-04-24");

        btn.setOnClickListener(v -> {
            String l = etLogin.getText().toString().trim();
            String p = etPassword.getText().toString().trim();
            String g = etGroup.getText().toString().trim();

            // ПРОБУЕМ HTTP (без S), так как сервер учебный и SSL может глючить
            String url = "http://android-for-students.ru/coursework/login.php";

            StringRequest sr = new StringRequest(Request.Method.POST, url,
                    res -> {
                        try {
                            JSONObject jo = new JSONObject(res);
                            if (jo.getInt("result_code") == 1) {
                                Intent i = new Intent(this, TaskActivity.class);
                                i.putExtra("variant", jo.getString("variant"));
                                i.putExtra("task", jo.getString("task"));
                                startActivity(i);
                                finish();
                            } else {
                                Toast.makeText(this, "Ошибка сервера: " + jo.optInt("result_code"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("AUTH_ERR", "Response: " + res);
                            Toast.makeText(this, "Ошибка данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    },
                    err -> {
                        String message = "Ошибка сети";
                        if (err instanceof TimeoutError) message = "Сервер не отвечает (Тайм-аут)";
                        else if (err instanceof NoConnectionError) message = "Нет интернета на устройстве";

                        Log.e("AUTH_ERR", "Detail: " + err.toString());
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> map = new HashMap<>();
                    map.put("lgn", l);
                    map.put("pwd", p);
                    map.put("g", g);
                    return map;
                }
            };
            Volley.newRequestQueue(this).add(sr);
        });
    }
}