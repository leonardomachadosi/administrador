package br.ufma.lsdi.administrador.fragments;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.IOException;

import br.ufma.lsdi.administrador.R;
import br.ufma.lsdi.administrador.domain.model.UsuarioLocalizacao;
import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.Callback;
import br.ufma.lsdi.cddl.Subscriber;
import br.ufma.lsdi.cddl.message.ContextMessage;
import br.ufma.lsdi.cddl.type.CDDLConfig;
import br.ufma.lsdi.cddl.type.ClientId;
import br.ufma.lsdi.cddl.type.Host;
import br.ufma.lsdi.cddl.type.Topic;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private UsuarioLocalizacao usuarioLocalizacao;

    private final CDDL cddl = CDDL.getInstance();
    private final String clientId = "ivan.rodrigues@lsdi.ufma.br";
    private Subscriber sub;
    private String customTopic;

    private CDDLConfig config;

    private Gson gson;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        usuarioLocalizacao = (UsuarioLocalizacao) bundle.getSerializable("usuarioLocalizacao");
    }

    @Override
    public void onResume() {
        super.onResume();

        gson = new Gson();
        iniciarCDDL(getActivity());
        customTopic = "ivan.rodrigues@lsdi.ufma.br/UsuarioLocalizacao" + usuarioLocalizacao.getTrajeto().getId();

        sobrescrever(customTopic);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setOnMapClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Add a marker in Sydney and move the camera
        //origem = new LatLng(-2.5497997, -44.2538819);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    private void sobrescrever(String customTopic) {

        sub = Subscriber.of(cddl);
        sub.setCallback(new Callback() {
            @Override
            public void messageArrived(ContextMessage contextMessage) {
                Log.d("Sub", gson.toJson(contextMessage));

                ObjectMapper objectMapper = new ObjectMapper();

                try {
                    String objeto = contextMessage.getBody();
                    UsuarioLocalizacao usuarioLocalizacao = objectMapper.readValue(objeto, UsuarioLocalizacao.class);

                    setMarker(usuarioLocalizacao);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                ;

            }

            @Override
            public void onConnectSuccess() {
                sub.subscribe(Topic.of(customTopic));
                Log.d("Sub", "Conectado para sobrescrever");
            }

            @Override
            public void onSubscribeSuccess(Topic topic) {

                Log.d("Sub", "Sobrescrito com sucesso!");
            }

            @Override
            public void onConnectFailure(Throwable exception) {
                Log.d("Sub", "Erro ao conectar");
            }
        });
        sub.connect();
    }

    public void setMarker(UsuarioLocalizacao usuarioLocalizacao) {
        LatLng now = new LatLng(usuarioLocalizacao.getLocalizacao().getLatitude(), usuarioLocalizacao.getLocalizacao().getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(now)
                .title("Movimentando"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(now));
    }

    public void iniciarCDDL(Context context) {

        config = CDDLConfig.builder()
                .host(Host.of("tcp://lsdi.ufma.br:1883"))
                .clientId(ClientId.of(clientId))
                .build();

        cddl.init(context, config);
        cddl.startScan();

    }

}
