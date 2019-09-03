package com.example.petmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

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
        // Obten o SupportMapFragment notifica quando o mapa está pronto para uso.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnDistancia = findViewById(R.id.btnDistance);

        //cria referência para a base "localization" no firebase
        mDataBase = FirebaseDatabase.getInstance().getReference().child("localization");

        //cria um listner para alterar a posição do marcador e recalcular a distancia, sempre que a posição for alterada
        //no firebase, pela movimentação do pet no app DogWalk
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
                            setaDistancia();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    //método sobreescrito da classe OnMapReadyCallback
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        long tempo = 1000; //1 segundo
        float minDistancia = 1; // metros

        // de 1 em 1 segundo ou a cada 1 metro movimentado, o marcador do dono e a distancia são alterados
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
            //quando for detectado movimento do dono esse método é disparado
            public void onLocationChanged(Location location) {
                //se o marcador já existir ele é deletado, para não existir mais de um no mapa
                if (currentLocationMarker2 != null) {
                    currentLocationMarker2.remove();
                }

                dono = new LatLng(location.getLatitude(), location.getLongitude());

                setaDistancia();

                CameraPosition cameraPosition = new CameraPosition.Builder().zoom(15).target(pet).build();
                currentLocationMarker2 = mMap.addMarker(new MarkerOptions().position(dono).title("Sua posição"));
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }, null );

        //seta a posição do pet quando o mapa terminar de carregar
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

    //cria marcador no mapa, referente ao pet
    private void criaMarcador() {
        //se o marcador já existir ele é deletado, para não existir mais de um no mapa
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        currentLocationLatLng = new LatLng(pet.latitude, pet.longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLocationLatLng);
        markerOptions.title("Posição PET");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        currentLocationMarker = mMap.addMarker(markerOptions);

        //executa uma animação de zoom de 15 na posição do marcador
        CameraPosition cameraPosition = new CameraPosition.Builder().zoom(15).target(currentLocationLatLng).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    //calcula e retorna a distância do marcador do pet atá o marcador do dono
    public static double calculaDistancia(Double lat1, double lat2, double lon1, double lon2){
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return 6366000 * c;
    }

    //mostra a distancia calculada na tela
    public void setaDistancia(){

        int aux = (int) calculaDistancia(dono.latitude, pet.latitude, dono.longitude, pet.longitude);

        if(aux != distancia){
            distancia = aux;
            btnDistancia.setText("Distancia: "+distancia+" metros");
        }
    }
}
