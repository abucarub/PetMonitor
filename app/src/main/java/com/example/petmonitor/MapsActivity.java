package com.example.petmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnDistancia;
    private Marker currentLocationMarker;
    private Marker currentLocationMarker2;
    private LatLng currentLocationLatLng;
    int distancia = 0;

    LatLng pet = null;
    LatLng dono = null;
    private DatabaseReference mDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnDistancia = findViewById(R.id.btnDistance);
        //startGettingLocations();
        mDataBase = FirebaseDatabase.getInstance().getReference().child("localization");

        mDataBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                final ArrayList<String> labels = new ArrayList<String>();
                for (DataSnapshot data : dataSnapshot.getChildren()){
                    final LocalizationPet loc = data.getValue(LocalizationPet.class);
                    if (loc.getLatitude() != 0) {
                        pet = new LatLng(loc.getLatitude(), loc.getLongitude());
                        criaMarcador();
                        if(dono!=null) {
                            calculaDistancia();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        long tempo = 1000; //5 minutos
        float minDistancia = 1; // 30 metros

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER , tempo , minDistancia,  new LocationListener() {

            @Override
            public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
                Toast.makeText(getApplicationContext(), "Status alterado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderEnabled(String arg0) {
                Toast.makeText(getApplicationContext(), "Provider Habilitado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderDisabled(String arg0) {
                Toast.makeText(getApplicationContext(), "Provider Desabilitado", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onLocationChanged(Location location) {

                if (currentLocationMarker2 != null) {
                    currentLocationMarker2.remove();
                }

                dono = new LatLng(location.getLatitude(), location.getLongitude());

                calculaDistancia();

                CameraPosition cameraPosition = new CameraPosition.Builder().zoom(15).target(pet).build();

                currentLocationMarker2 = mMap.addMarker(new MarkerOptions().position(dono).title("Sua posição"));

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                //Toast.makeText(getApplicationContext(), "Distancia de "+distancia+" metros", Toast.LENGTH_LONG).show();
            }
        }, null );

        setPetPosition();
    }

    private void setPetPosition() {
        mDataBase.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Get map of users in datasnapshot
                        final ArrayList<String> labels = new ArrayList<String>();
                        for (DataSnapshot data : dataSnapshot.getChildren()){
                            final LocalizationPet loc = data.getValue(LocalizationPet.class);
                            if (loc.getLatitude() != 0) {
                                pet = new LatLng(loc.getLatitude(), loc.getLongitude());
                                criaMarcador();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });

    }

    private void criaMarcador() {
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        currentLocationLatLng = new LatLng(pet.latitude, pet.longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLocationLatLng);
        markerOptions.title("Posição PET");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        currentLocationMarker = mMap.addMarker(markerOptions);

        CameraPosition cameraPosition = new CameraPosition.Builder().zoom(15).target(currentLocationLatLng).build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public static double distance(Double lat1, double lat2,  double lon1, double lon2){
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return 6366000 * c;
    }

    public void calculaDistancia(){

        int aux = (int) distance(dono.latitude, pet.latitude, dono.longitude, pet.longitude);

        if(aux != distancia){
            distancia = aux;
            btnDistancia.setText("Distancia: "+distancia+" metros");
        }
    }
}
