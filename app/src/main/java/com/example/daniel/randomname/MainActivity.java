package com.example.daniel.randomname;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    //Constants
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;


    //Classes
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Api api;


    //Views
    @BindView(R.id.locationMainTextView)
    TextView locationMainTextView;
    @BindView(R.id.nameMainTextView)
    TextView nameMainTextView;
    @BindView(R.id.generateNameMainButton)
    Button generateNameMainButton;

    //Variables
    private String countryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        generateNameMainButton.setEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                requestLocation();
            }
        } else {
            requestLocation();
        }
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl("https://uinames.com/api/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = retrofitBuilder.build();
        api = retrofit.create(Api.class);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                requestLocation();
                break;
        }
    }


    private void requestLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                for (final Location location : locationResult.getLocations()) {
                    Thread geocoderThread = new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.ENGLISH);
                            try {
                                final List<Address> list = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                countryName = list.get(0).getCountryName();
                                runOnUiThread(new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        locationMainTextView.setText("Current location: " + list.get(0).getCountryName());
                                        generateNameMainButton.setEnabled(true);
                                    }
                                }));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    geocoderThread.start();
                }
            }
        };

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        });
    }

    @OnClick(R.id.generateNameMainButton)
    public void onViewClicked() {
        Call<Person> call = api.getPerson(countryName);
        call.enqueue(new Callback<Person>() {
            @Override
            public void onResponse(Call<Person> call, Response<Person> response) {
                Person person = response.body();
                nameMainTextView.setText(person.getName() + " " + person.getSurname());
            }

            @Override
            public void onFailure(Call<Person> call, Throwable t) {
                Log.e("response", t.toString());
                Toast.makeText(MainActivity.this, "NO RESPONSE", Toast.LENGTH_LONG).show();
            }
        });
    }
}