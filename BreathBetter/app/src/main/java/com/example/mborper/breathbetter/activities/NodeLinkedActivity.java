package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mborper.breathbetter.R;

/**
 * Activity to show a confirmation screen after successfully linking a node
 *
 * @author Assistant
 * @since 2024-11-20
 */
public class NodeLinkedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_linked);

        Button btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v -> {
            // Navigate to MainActivity
            Intent intent = new Intent(NodeLinkedActivity.this, MainActivity.class);
            // Clear the back stack so the user can't go back to previous linking screens
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}