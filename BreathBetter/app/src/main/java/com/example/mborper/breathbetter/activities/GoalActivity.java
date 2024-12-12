package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.graphs.SemicircleProgressView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * Activity responsible for displaying the goal screen and handling bottom navigation interactions.
 * This activity provides navigation to different parts of the app using a BottomNavigationView.
 *
 * @author Alejandro Rosado
 * @since  2024-11-19
 * last updated: 2024-12-12
 */
public class GoalActivity extends AppCompatActivity {

    private SemicircleProgressView semicircleProgressView;
    private TextView textTotalDistance;
    private TextView textProgressPercentage;

    private ApiService apiService;

    private TableLayout tableMonthlyObjectives;
    private List<TableRow> objectiveRows;
    private boolean isAscendingPremio = false;
    private boolean isAscendingCiudad = false;
    private boolean isAscendingFecha = false;

    /**
     * Called when the activity is created.
     * <p>
     * Initializes the activity by setting the layout and setting up the bottom navigation menu.
     * @param savedInstanceState The saved state of the activity, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        textTotalDistance = findViewById(R.id.text_total_distance);
        textProgressPercentage = findViewById(R.id.text_progress_percentage);
        semicircleProgressView = findViewById(R.id.semicircle_progress);

        // Initialize ApiService using ApiClient
        apiService = ApiClient.getClient(this).create(ApiService.class);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.target);

        tableMonthlyObjectives = findViewById(R.id.table_monthly_objectives);
        objectiveRows = new ArrayList<>();

        // Populate objectiveRows with existing rows (excluding header)
        for (int i = 1; i < tableMonthlyObjectives.getChildCount(); i++) {
            objectiveRows.add((TableRow) tableMonthlyObjectives.getChildAt(i));
        }

        setupSortingListeners();

        // Setup bottom navigation
        setupBottomNavigation();

        // Fetch current month's distance
        getCurrentMonthDistance();
    }

    /**
     * Configures the bottom navigation menu and defines the behavior when each item is selected.
     * <p>
     * Each navigation item (home, map, target, profile) will start a new activity or perform a transition
     * when selected. If the current item is the same as the one already selected, no action is performed.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (item.getItemId() == R.id.map) {
                startActivity(new Intent(this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.target) {
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    /**
     * Fetches the total distance traveled in the current month from the API and updates the UI.
     * Handles success and failure responses gracefully.
     */
    private void getCurrentMonthDistance() {
        apiService.getCurrentMonthDistance().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalDistance = response.body().get("currentMonthDistance").getAsInt();
                    updateDistanceAndProgress(totalDistance);
                } else {
                    // Handle error scenario
                    textTotalDistance.setText("Error fetching distance");
                    textProgressPercentage.setText("0%");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Handle network error
                textTotalDistance.setText("Network error");
                textProgressPercentage.setText("0%");
            }
        });
    }

    /**
     * Updates the UI with the total distance traveled and progress percentage.
     * <p>
     * Integer -> updateDistanceAndProgress(totalDistance) -> void
     * @param totalDistance Total distance traveled in meters.
     */
    private void updateDistanceAndProgress(int totalDistance) {
        int goalDistance = 20000; // 20 km in meters
        int progress = (int) ((totalDistance * 100.0f) / goalDistance);

        // Update semicircle progress
        semicircleProgressView.setProgress(progress);

        // Update total distance text (convert to km for better readability)
        textTotalDistance.setText(String.format("%.2f km", totalDistance / 1000.0f));

        // Update progress percentage
        textProgressPercentage.setText(String.format("%d%%", progress));
    }

    /**
     * Sets up sorting listeners for table headers and defines behavior for sorting columns.
     */
    private void setupSortingListeners() {
        TextView headerPremio = findViewById(R.id.header_premio);
        TextView headerCiudad = findViewById(R.id.header_ciudad);
        TextView headerFecha = findViewById(R.id.header_fecha);

        headerPremio.setOnClickListener(v -> {
            rotateHeader(headerPremio, isAscendingPremio);
            sortTable(0, isAscendingPremio = !isAscendingPremio);
        });
        headerCiudad.setOnClickListener(v -> {
            rotateHeader(headerCiudad, isAscendingCiudad);
            sortTable(1, isAscendingCiudad = !isAscendingCiudad);
        });
        headerFecha.setOnClickListener(v -> {
            rotateHeader(headerFecha, isAscendingFecha);
            sortTable(2, isAscendingFecha = !isAscendingFecha);
        });
    }

    /**
     * Toggles the sort indicator (▲/▼) in the table header.
     * <p>
     * TextView -> rotateHeader(header, isCurrentlyAscending) -> void
     * @param header The header TextView to update.
     * @param isCurrentlyAscending Current sorting order.
     */
    private void rotateHeader(TextView header, boolean isCurrentlyAscending) {
        header.setText(header.getText().toString().replace(isCurrentlyAscending ? "▼" : "▲",
                isCurrentlyAscending ? "▲" : "▼"));
    }

    /**
     * Sorts the table rows based on the specified column index and order.
     * <p>
     * Integer, Boolean -> sortTable(columnIndex, isAscending) -> void
     * @param columnIndex The index of the column to sort by.
     * @param isAscending Whether to sort in ascending order.
     */
    private void sortTable(int columnIndex, boolean isAscending) {
        // Remove existing rows (except header)
        for (int i = tableMonthlyObjectives.getChildCount() - 1; i > 0; i--) {
            tableMonthlyObjectives.removeViewAt(i);
        }

        // Sort rows
        Collections.sort(objectiveRows, new Comparator<TableRow>() {
            @Override
            public int compare(TableRow row1, TableRow row2) {
                TextView cell1 = (TextView) ((TableRow) row1).getChildAt(columnIndex);
                TextView cell2 = (TextView) ((TableRow) row2).getChildAt(columnIndex);

                int result;
                if (columnIndex == 2) { // Fecha
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        result = sdf.parse(cell1.getText().toString())
                                .compareTo(sdf.parse(cell2.getText().toString()));
                    } catch (ParseException e) {
                        result = cell1.getText().toString().compareTo(cell2.getText().toString());
                    }
                } else {
                    result = cell1.getText().toString().compareTo(cell2.getText().toString());
                }

                return isAscending ? result : -result;
            }
        });

        // Add sorted rows back to table
        for (TableRow row : objectiveRows) {
            tableMonthlyObjectives.addView(row);
        }
    }

}
