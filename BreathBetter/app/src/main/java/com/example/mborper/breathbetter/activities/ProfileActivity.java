package com.example.mborper.breathbetter.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProfileActivity handles the user's profile management.
 * This includes the ability to view, edit, and update the user's name, surname, and profile image.
 * <p>
 * The activity also includes handling the bottom navigation for navigating between different sections of the app.
 * <p>
 * The user's data is fetched from the API and displayed on the screen, with the option to update it.
 * Profile picture selection is done through external storage permission and the image is encoded to Base64 for upload.
 *
 * @author Alejandro Rosado
 * @since  2024-11-18
 * last edited: 2024-11-20
 */
public class ProfileActivity extends AppCompatActivity {

    /** Log tag for debugging. */
    private static final String LOG_TAG = "DEVELOPMENT_LOG";

    private Intent serviceIntent = null;
    /** Tracks whether the service is bound to the activity. */
    private boolean isBound = false;

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1001;

    private ImageView imageViewProfile;
    private TextInputEditText etName;
    private TextInputEditText etSurname;
    private TextView textViewProfileEmail;
    private MaterialButton btnSaveChanges;
    private ApiService apiService;
    private SessionManager sessionManager;

    // Variables to detect changes
    private String initialName = "";
    private String initialSurname = "";
    private String initialImageUri = null; // Initial URL of the image
    private Uri selectedImageUri = null;  // URI of the image
    private String selectedImageBase64 = null;

    private MaterialButton btnChangePass;

    /**
     * onCreate initializes the UI components, sets up listeners, and loads the user profile.
     * It also configures the bottom navigation for easy access to other activities.
     * <p>
     * Flow: ProfileActivity -> onCreate() -> loadUserProfile(), setupTextWatchers(), setupBottomNavigation()
     * @param savedInstanceState the saved instance state from a previous instance, if available
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.profile);

        // Start components
        imageViewProfile = findViewById(R.id.ImageViewProfile);
        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        textViewProfileEmail = findViewById(R.id.textViewProfileEmail);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Initial state of button
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray));

        // Monitor changes in fields
        setupTextWatchers();
        imageViewProfile.setOnClickListener(v -> requestImagePickPermission());
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());

        btnChangePass = findViewById(R.id.btnChangePass);
        btnChangePass.setOnClickListener(v -> onTvForgotPassClicked());

        // Upload profile data
        loadUserProfile();

        // Bottom navigation
        setupBottomNavigation();
    }

    /**
     * loadUserProfile fetches the user's profile information from the server and displays it in the UI.
     * It also handles errors in loading the data and displays appropriate messages to the user.
     * <p>
     * Flow: ProfileActivity -> loadUserProfile() -> apiService.getUserProfile() -> onResponse(), onFailure()
     */
    private void loadUserProfile() {
        apiService.getUserProfile().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                TextView tvError = findViewById(R.id.tvError);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject user = response.body().getAsJsonObject("user");
                    if (user != null) {
                        // Assign initial values when loading the profile
                        initialName = user.has("name") ? user.get("name").getAsString() : "";
                        initialSurname = user.has("surnames") ? user.get("surnames").getAsString() : "";
                        initialImageUri = user.has("photoUrl") && !user.get("photoUrl").isJsonNull()
                                ? user.get("photoUrl").getAsString()
                                : null;

                        // Set values in fields
                        etName.setText(initialName);
                        etSurname.setText(initialSurname);
                        textViewProfileEmail.setText(user.has("email") ? user.get("email").getAsString() : "");

                        // Upload profile picture
                        if (initialImageUri != null) {
                            Glide.with(ProfileActivity.this)
                                    .load(initialImageUri)
                                    .placeholder(R.mipmap.placeholder_profile_icon_default_foreground)
                                    .error(R.drawable.placeholder_error_icon)
                                    .into(imageViewProfile);
                        } else {
                            imageViewProfile.setImageResource(R.mipmap.placeholder_profile_icon_default_foreground);
                        }

                        // Reset button (no change after loading profile)
                        btnSaveChanges.setEnabled(false);
                        btnSaveChanges.setBackgroundTintList(ContextCompat.getColorStateList(ProfileActivity.this, R.color.gray));
                    }
                } else {
                    Log.e(LOG_TAG, "Error al cargar perfil: " + response.code());
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText("Error al cargar perfil");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                TextView tvError = findViewById(R.id.tvError);

                Log.e(LOG_TAG, "Fallo de conexión al cargar perfil", t);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Error de conexión");
            }
        });
    }

    /**
     * Opens the forgot password activity
     */
    private void onTvForgotPassClicked() {
        startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
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
                startActivity(new Intent(this, GoalActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.profile) {
                overridePendingTransition(4, 4);
                return true;
            }
            return false;
        });
    }

    /**
     * setupTextWatchers monitors text changes in the name and surname fields and checks if the profile data has been changed.
     * <p>
     * Flow: ProfileActivity -> setupTextWatchers() -> TextWatcher -> checkForChanges()
     */
    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etName.addTextChangedListener(textWatcher);
        etSurname.addTextChangedListener(textWatcher);
    }

    /**
     * checkForChanges compares the current values of the profile fields with the initial ones to determine if any changes have been made.
     * It enables or disables the save changes button accordingly.
     */
    private void checkForChanges() {
        String currentName = etName.getText().toString().trim();
        String currentSurname = etSurname.getText().toString().trim();

        // Compare current values with initial ones
        boolean hasNameChanged = !currentName.equals(initialName);
        boolean hasSurnameChanged = !currentSurname.equals(initialSurname);

        // Check for changes in the image
        boolean hasImageChanged = (selectedImageUri != null) ||
                (initialImageUri == null && selectedImageUri != null) ||
                (initialImageUri != null && selectedImageUri != null && !initialImageUri.equals(selectedImageUri.toString()));

        // Change button state if there are changes
        if (hasNameChanged || hasSurnameChanged || hasImageChanged) {
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
        } else {
            btnSaveChanges.setEnabled(false);
            btnSaveChanges.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray));
        }
    }

    /**
     * Handles the result of image selection from external storage or gallery.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult()
     * @param resultCode Result code returned by the image picker
     * @param data An Intent that contains the selected image data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        TextView tvError = findViewById(R.id.tvError);

        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            try {
                // Display image immediately using Glide
                Glide.with(ProfileActivity.this)
                        .load(selectedImageUri)
                        .placeholder(R.drawable.placeholder_icon)
                        .error(R.drawable.placeholder_error_icon)
                        .into(imageViewProfile);

                // Process the image for later sending
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                // Check changes to activate the save button
                checkForChanges();

                tvError.setVisibility(View.VISIBLE);
                tvError.setTextColor(getResources().getColor(R.color.gray));
                tvError.setText("Imagen seleccionada correctamente");

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error al procesar la imagen: " + e.getMessage());
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Error al seleccionar la imagen");

                selectedImageUri = null; // Reset on error
                // Restore previous image if it exists
                if (initialImageUri != null) {
                    Glide.with(ProfileActivity.this)
                            .load(initialImageUri)
                            .placeholder(R.drawable.placeholder_icon)
                            .error(R.drawable.placeholder_error_icon)
                            .into(imageViewProfile);
                } else {
                    imageViewProfile.setImageResource(R.drawable.placeholder_icon);
                }
            }
        }
    }

    /**
     * Requests permission to access external storage for image selection.
     * Handles different permission models for Android versions.
     */
    private void requestImagePickPermission() {
        TextView tvError = findViewById(R.id.tvError);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+: No permission needed
            openImagePicker();
        } else {
            // Android 10 and previous
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText("Se necesita permiso para acceder a la galería");
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
        }
    }

    /**
     * Opens the image picker intent with specific MIME type restrictions.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_READ_EXTERNAL_STORAGE);
    }

    /**
     * Handles the result of permission request for external storage access.
     *
     * @param requestCode The request code passed in requestPermissions()
     * @param permissions The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        TextView tvError = findViewById(R.id.tvError);

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Permiso de acceso a la galería denegado");
            }
        }
    }

    /**
     * Saves profile changes by updating user name, surname, and profile image.
     *
     * Validates input, prepares multipart request for server, and handles response.
     */
    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();

        TextView tvError = findViewById(R.id.tvError);

        // Check for changes
        boolean hasName = !name.isEmpty();
        boolean hasSurname = !surname.isEmpty();
        boolean hasImage = selectedImageUri != null;

        if (!hasName && !hasSurname && !hasImage) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Proporciona al menos un campo para actualizar");
            return;
        }

        // Create RequestBody for text fields
        RequestBody namePart = hasName ? RequestBody.create(name, okhttp3.MediaType.parse("text/plain")) : null;
        RequestBody surnamePart = hasSurname ? RequestBody.create(surname, okhttp3.MediaType.parse("text/plain")) : null;

        // Create MultipartBody for the image, if available
        MultipartBody.Part photoPart = null;
        if (hasImage) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                File imageFile = new File(getCacheDir(), "profile_image.jpg");
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                }

                RequestBody requestFile = RequestBody.create(imageFile, okhttp3.MediaType.parse("image/jpeg"));
                photoPart = MultipartBody.Part.createFormData("photo", imageFile.getName(), requestFile);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error al procesar la imagen: " + e.getMessage());
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Error al preparar la imagen para subir");
                return;
            }
        }

        // Call the endpoint with the corresponding data
        apiService.updateUserProfile(namePart, surnamePart, photoPart).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setTextColor(getResources().getColor(R.color.gray));
                    tvError.setText("Perfil actualizado correctamente");

                    // Update initial values on successful save
                    initialName = name;
                    initialSurname = surname;

                    if (hasImage) {
                        // Update the initial value of the image
                        initialImageUri = selectedImageUri.toString();

                        // Update the ImageView
                        Glide.with(ProfileActivity.this)
                                .load(selectedImageUri)
                                .into(imageViewProfile);

                        // Reset selectedImageUri
                        selectedImageUri = null;
                    }

                    // Check changes and disable the button if necessary
                    checkForChanges();
                } else {
                    Log.e(LOG_TAG, "Error al actualizar perfil: " + response.code());
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText("Error al actualizar perfil");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(LOG_TAG, "Fallo de conexión al actualizar perfil", t);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Fallo de conexión al actualizar perfil");
            }
        });
    }

    /**
     * Class to handle the logout button and process its click.
     */
    public void onLogoutButtonClicked(View v) {
        // Stop service and clean up
        onStopServiceButtonClicked(null);

        // Clear session
        sessionManager.clearSession();

        // Redirect to login
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    /**
     * Stops the service and unbinds it when the stop service button is clicked.
     *
     * @param v The button view that was clicked.
     */
    public void onStopServiceButtonClicked(View v) {
        TextView tvError = findViewById(R.id.tvError);

        if (serviceIntent != null) {
            try {
                if (isBound) {
                    isBound = false;
                }
                stopService(serviceIntent);
                serviceIntent = null;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error stopping service: " + e.getMessage());
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Error al parar el servicio");
            }
        }
    }
}
