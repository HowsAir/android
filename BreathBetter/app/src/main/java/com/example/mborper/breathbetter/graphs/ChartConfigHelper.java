package com.example.mborper.breathbetter.graphs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.activities.MainActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import android.graphics.DashPathEffect;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to configure and manage the setup of BarChart for visualizing air quality data.
 * This class contains methods to configure the chart's axes, update the chart with new air quality data,
 * and customize visual elements such as limit lines, colors, and interactions.
 *
 * @author Alejandro Rosado
 * @since  2024-11-10
 * last edited: 2024-12-12
 */
public class ChartConfigHelper {
    private static final String LOG_TAG = "ChartConfigHelper";

    /**
     * Configures the basic setup for the BarChart.
     *
     * @param barChart The BarChart to be configured
     */
    public static void setupChartBasics(BarChart barChart) {
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        // Disable double tap zooming
        barChart.setDoubleTapToZoomEnabled(false);
    }

    /**
     * Configures the X-axis for the BarChart.
     *
     * @param barChart The BarChart to configure
     * @param hours    List of hours to be displayed on X-axis
     */
    public static void configureXAxis(BarChart barChart, ArrayList<String> hours) {
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(12);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return index < hours.size() ? hours.get(index) : "";
            }
        });
    }

    /**
     * Configures the Y-axis for the BarChart.
     *
     * @param barChart The BarChart to configure
     */
    public static void configureYAxis(BarChart barChart) {
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(110f);
        leftAxis.setGranularity(20f);
        leftAxis.setDrawLabels(true);
        leftAxis.setDrawAxisLine(false);

        // Modify limit lines to be behind the bars
        leftAxis.setDrawLimitLinesBehindData(true);

        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 20f) return "Buena";
                if (value == 60f) return "Regular";
                if (value == 100f) return "Peligrosa";
                return "";
            }
        });

        addCustomLimitLines(leftAxis);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    /**
     * Adds custom limit lines to the Y-axis.
     *
     * @param leftAxis The Y-axis to add limit lines to
     */
    private static void addCustomLimitLines(YAxis leftAxis) {
        leftAxis.removeAllLimitLines();

        LimitLine goodLine = createLimitLine(20f);
        LimitLine regularLine = createLimitLine(60f);
        LimitLine dangerousLine = createLimitLine(100f);

        goodLine.setLineColor(Color.parseColor("#80E3E3E3")); // Semi-transparent
        regularLine.setLineColor(Color.parseColor("#80E3E3E3")); // Semi-transparent
        dangerousLine.setLineColor(Color.parseColor("#80E3E3E3")); // Semi-transparent

        leftAxis.addLimitLine(goodLine);
        leftAxis.addLimitLine(regularLine);
        leftAxis.addLimitLine(dangerousLine);
    }

    /**
     * Creates a limit line with specific styling.
     *
     * @param value The Y-axis value where the limit line should be placed
     * @return A styled LimitLine
     */
    private static LimitLine createLimitLine(float value) {
        LimitLine limitLine = new LimitLine(value);
        limitLine.setLineColor(Color.parseColor("#E3E3E3"));
        limitLine.setLineWidth(1f);
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLine.setTextColor(Color.parseColor("#757575"));
        return limitLine;
    }

    /**
     * Updates the BarChart with new air quality readings.
     *
     * @param barChart            The BarChart to update
     * @param airQualityReadings  JsonArray of air quality readings
     * @param context             Context for showing Toast messages
     */
    public static void updateBarChart(BarChart barChart, JsonArray airQualityReadings, Context context) {
        // Verificar si los datos están disponibles
        if (airQualityReadings == null || airQualityReadings.size() == 0) {
            Log.e("ChartConfigHelper", "No air quality readings available");
            clearChart(barChart);
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<Integer> colorList = new ArrayList<>();
        ArrayList<String> hours = new ArrayList<>();

        // Determinar rango de tiempo desde el primer y último timestamp
        LocalDateTime startTime = parseTimestamp(airQualityReadings.get(0).getAsJsonObject().get("timestamp").getAsString());
        LocalDateTime endTime = parseTimestamp(airQualityReadings.get(airQualityReadings.size() - 1).getAsJsonObject().get("timestamp").getAsString());

        // Crear un mapa para almacenar lecturas por hora
        Map<String, JsonObject> readingsByHour = new HashMap<>();
        for (int i = 0; i < airQualityReadings.size(); i++) {
            JsonObject reading = airQualityReadings.get(i).getAsJsonObject();
            String hour = convertTimestampToHour(reading.get("timestamp").getAsString());
            readingsByHour.put(hour, reading);
        }

        // Generar las barras para cada intervalo de 2 horas
        LocalDateTime currentTime = startTime;
        int index = 0;
        while (currentTime.isBefore(endTime)) { // Excluir endTime del bucle
            String hour = currentTime.format(DateTimeFormatter.ofPattern("HH'h'"));
            hours.add(hour);

            JsonObject reading = readingsByHour.get(hour);
            if (reading != null &&
                    reading.has("proportionalValue") &&
                    !reading.get("proportionalValue").isJsonNull()) {

                int proportionalValue = reading.get("proportionalValue").getAsInt();
                String airQuality = reading.has("airQuality") && !reading.get("airQuality").isJsonNull()
                        ? reading.get("airQuality").getAsString()
                        : "No Data";

                entries.add(new BarEntry(index, proportionalValue));
                colorList.add(getColorForAirQuality(airQuality));
            } else {
                // Agregar una barra mínima para las horas sin datos
                entries.add(new BarEntry(index, 0.1f)); // Valor mínimo
                colorList.add(Color.parseColor("#E0E0E0")); // Gris claro
            }

            currentTime = currentTime.plusHours(2); // Incrementar por 2 horas
            index++;
        }

        // Crear y configurar el dataset
        BarDataSet dataSet = new BarDataSet(entries, "Air Quality");
        dataSet.setColors(colorList);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        // Configurar el gráfico
        barChart.setData(barData);
        barChart.setFitBars(true);

        // Configurar ejes
        configureXAxis(barChart, hours);
        configureYAxis(barChart);

        // Asignar un renderer personalizado para esquinas redondeadas
        RoundedBarChartRenderer roundedRenderer = new RoundedBarChartRenderer(barChart, 20f);
        barChart.setRenderer(roundedRenderer);

        // Mostrar advertencias si hay mediciones peligrosas
        boolean hasDangerousMeasurement = colorList.contains(Color.parseColor("#DC2626")); // Rojo

        if (context instanceof MainActivity) {
            ((MainActivity) context).runOnUiThread(() -> {
                ImageView cautionAirIcon = ((MainActivity) context).findViewById(R.id.caution_air_icon);
                TextView textCaution = ((MainActivity) context).findViewById(R.id.text_caution);

                if (hasDangerousMeasurement) {
                    cautionAirIcon.setVisibility(View.VISIBLE);
                    textCaution.setVisibility(View.VISIBLE);
                } else {
                    cautionAirIcon.setVisibility(View.GONE);
                    textCaution.setVisibility(View.GONE);
                }
            });
        }

        barChart.moveViewToX(0); // Restablecer la vista al inicio

        barChart.invalidate();

        // Configurar interacción con la gráfica
        setupChartInteraction(barChart, hours, context);
    }


    /**
     * Helper method to parse a timestamp string into LocalDateTime.
     *
     * @param timestamp The timestamp string to parse
     * @return Parsed LocalDateTime object
     */
    private static LocalDateTime parseTimestamp(String timestamp) {
        return ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Helper method to clear the chart data.
     *
     * @param barChart The BarChart to clear
     */
    private static void clearChart(BarChart barChart) {
        barChart.clear();
        barChart.invalidate();
    }

    /**
     * Adds chart interaction for value selection.
     *
     * @param barChart The BarChart to interact with
     * @param hours    List of hours displayed on the X-axis
     * @param context  Context to show Toast messages on value selection
     */
    private static void setupChartInteraction(BarChart barChart, ArrayList<String> hours, Context context) {
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) h.getX();
                if (index >= 0 && index < hours.size()) {
                    String hour = hours.get(index);
                    float value = e.getY();
                    Toast.makeText(context, "Hora: " + hour + ", Valor: " + value, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {
                // Optional: handle no selection
            }
        });
    }


    /**
     * Converts a timestamp to a local hour format.
     *
     * @param timestamp The timestamp to convert
     * @return Formatted hour string
     */
    private static String convertTimestampToHour(String timestamp) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            // Cambiar el patrón para formato de 24 horas con "h"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH'h'");
            return localDateTime.format(formatter);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error converting timestamp: " + e.getMessage());
            return "";
        }
    }

    /**
     * Determines the color for a bar based on air quality.
     *
     * @param airQuality The air quality status
     * @return Color resource for the bar
     */
    private static int getColorForAirQuality(String airQuality) {
        switch (airQuality) {
            case "Good":
                return Color.parseColor("#16A34A"); // Green
            case "Regular":
                return Color.parseColor("#EAB308"); // Yellow
            case "Bad":
                return Color.parseColor("#DC2626"); // Red
            default:
                return Color.parseColor("#E3E3E3"); // Gray for "No Data"
        }
    }
}