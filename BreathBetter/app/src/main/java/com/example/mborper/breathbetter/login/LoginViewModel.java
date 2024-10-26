package com.example.mborper.breathbetter.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.pojo.LoginRequest;
import com.example.mborper.breathbetter.login.pojo.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {
    private final MutableLiveData<LoginState> loginState = new MutableLiveData<>();
    private final ApiService apiService;

    public LoginViewModel() {
        // Aquí cambias a FakeApiService en lugar de RetrofitClient
        // Para la versión real
        //apiService = RetrofitClient.getInstance().getApiService();

        // Para la versión fake (durante pruebas)
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public LiveData<LoginState> getLoginState() {
        return loginState;
    }

    public void login(String email, String password) {
        loginState.setValue(LoginState.loading());

        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Cambia a LoginState.success() cuando la respuesta sea válida
                    loginState.setValue(LoginState.success(response.body()));
                } else {
                    loginState.setValue(LoginState.error("Invalid credentials"));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loginState.setValue(LoginState.error(t.getMessage()));
            }
        });
    }
}
