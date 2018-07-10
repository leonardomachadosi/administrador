package br.ufma.lsdi.administrador.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.ufma.lsdi.administrador.R;
import br.ufma.lsdi.administrador.domain.enuns.StatusEnum;
import br.ufma.lsdi.administrador.domain.model.Trajeto;
import br.ufma.lsdi.administrador.domain.model.UsuarioLocalizacao;
import br.ufma.lsdi.administrador.domain.model.auxiliary.Parado;
import br.ufma.lsdi.administrador.domain.model.auxiliary.Resultado;
import br.ufma.lsdi.administrador.retrofit.RetrofitInicializador;
import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.Callback;
import br.ufma.lsdi.cddl.Subscriber;
import br.ufma.lsdi.cddl.message.ContextMessage;
import br.ufma.lsdi.cddl.type.CDDLConfig;
import br.ufma.lsdi.cddl.type.ClientId;
import br.ufma.lsdi.cddl.type.Host;
import br.ufma.lsdi.cddl.type.Topic;
import retrofit2.Call;
import retrofit2.Response;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String GOOGLE_KEY_DIRECTIONS = "AIzaSyB299emCUuKMkZHXIHQc5u1Po7ZnrEA3S0";

    private GoogleMap mMap;
    private UsuarioLocalizacao usuarioLocalizacao;

    private final CDDL cddl = CDDL.getInstance();
    private final String clientId = "ivan.rodrigues@lsdi.ufma.br";
    private Subscriber sub;
    private String customTopic;

    private CDDLConfig config;

    private Gson gson;
    private Resultado resultado;

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
        resultado = new Resultado();
        usuarioLocalizacao = (UsuarioLocalizacao) bundle.getSerializable("usuarioLocalizacao");
    }

    @Override
    public void onResume() {
        super.onResume();

        gson = new Gson();
        iniciarCDDL(getActivity());
        customTopic = "ivan.rodrigues@lsdi.ufma.br/UsuarioLocalizacao" + usuarioLocalizacao.getTrajeto().getId();
        sobrescrever(customTopic);
        sobrescreverFimRota("ivan.rodrigues@lsdi.ufma.br/Fim" + usuarioLocalizacao.getTrajeto().getId());
        try {
            if (mMap != null) {
                mMap.clear();
            }
            getStatusUsuario(usuarioLocalizacao);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }


    private void updateUsuarioLocalizacao() throws Exception {
        Call<UsuarioLocalizacao> call = new RetrofitInicializador().getUusarioLocalizacaoByTrajeto().getUusarioLocalizacaoByTrajeto(usuarioLocalizacao.getTrajeto());
        try {
            call.enqueue(new retrofit2.Callback<UsuarioLocalizacao>() {
                @Override
                public void onResponse(Call<UsuarioLocalizacao> call, Response<UsuarioLocalizacao> response) {

                    if (response.body() != null) {
                        usuarioLocalizacao = new UsuarioLocalizacao();
                        usuarioLocalizacao = response.body();

                        try {
                            getStatusUsuario(usuarioLocalizacao);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }

                @Override
                public void onFailure(Call<UsuarioLocalizacao> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void sobrescreverFimRota(String customTopic) {
        Subscriber subscriberFim = Subscriber.of(cddl);

        sub.setCallback(new Callback() {
            @Override
            public void messageArrived(ContextMessage contextMessage) {
                Log.d("subscriberFim", gson.toJson(contextMessage));

            }

            @Override
            public void onConnectSuccess() {
                subscriberFim.subscribe(Topic.of(customTopic));
                Log.d("subscriberFim", "Conectado para sobrescrever");
            }

            @Override
            public void onSubscribeSuccess(Topic topic) {
                try {
                    updateUsuarioLocalizacao();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("subscriberFim", "Sobrescrito com sucesso!");
            }

            @Override
            public void onConnectFailure(Throwable exception) {
                Log.d("Sub", "Erro ao conectar");
            }
        });
        subscriberFim.connect();
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
                    UsuarioLocalizacao userLocation = objectMapper.readValue(objeto, UsuarioLocalizacao.class);

                    setMarker(userLocation);

                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    public void setMarker(UsuarioLocalizacao user) {
        LatLng now = new LatLng(user.getLocalizacao().getLatitude(), user.getLocalizacao().getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(now)
                .title("Movimentando"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(now));

    }

    public void iniciarCDDL(Context context) {

        config = CDDLConfig.builder()
                //.host(Host.of("tcp://iot.eclipse.org:1883"))
                .host(Host.of("tcp://lsdi.ufma.br:1883"))
                .clientId(ClientId.of(clientId))
                .build();

        cddl.init(context, config);
        cddl.startScan();

    }

    private void rota(UsuarioLocalizacao usuarioLocalizacao, int color) {
        GoogleDirection.withServerKey(GOOGLE_KEY_DIRECTIONS)
                .from(new LatLng(usuarioLocalizacao.getTrajeto().getLatitudeInicial(), usuarioLocalizacao.getTrajeto().getLongitudeInicial()))
                .to(new LatLng(usuarioLocalizacao.getTrajeto().getLatitudeFinal(), usuarioLocalizacao.getTrajeto().getLongitudeFinal()))
                .avoid(AvoidType.FERRIES)
                .avoid(AvoidType.HIGHWAYS)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            // Do something
                            Route route = direction.getRouteList().get(0);
                            int legCount = route.getLegList().size();
                            for (int index = 0; index < legCount; index++) {
                                Leg leg = route.getLegList().get(index);
                                // mMap.addMarker(new MarkerOptions().position(leg.getStartLocation().getCoordination()).title(leg.getStartAddress()));
                                if (index == legCount - 1) {
                                    //     mMap.addMarker(new MarkerOptions().position(leg.getEndLocation().getCoordination()).title(leg.getEndAddress()));
                                }
                                List<Step> stepList = leg.getStepList();
                                ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(getActivity(),
                                        stepList, 5, color, 3, Color.BLUE);
                                for (PolylineOptions polylineOption : polylineOptionList) {
                                    mMap.addPolyline(polylineOption);
                                }
                            }
                            setCameraWithCoordinationBounds(route);

                        } else {
                            // Do something
                            Toast.makeText(getActivity(), "Erro na conexão", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something
                        t.printStackTrace();
                    }
                });

    }

    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void getStatusUsuario(UsuarioLocalizacao usuarioLocalizacao) throws Exception {
        try {
            Call<Resultado> call = new RetrofitInicializador().getStatusUsuario().getStatusUsuario(new Resultado(usuarioLocalizacao.getTrajeto().getId()));
            call.enqueue(new retrofit2.Callback<Resultado>() {
                @Override
                public void onResponse(Call<Resultado> call, Response<Resultado> response) {

                    if (response.body() != null) {
                        resultado = response.body();

                        if (resultado.getCorrendo() != null) {

                            //TRAJETO PERCORRIDO CORRENDO
                            Trajeto trajetoCorrendo = new Trajeto();
                            UsuarioLocalizacao userCorrendo = new UsuarioLocalizacao();
                            trajetoCorrendo.setLatitudeInicial(resultado.getCorrendo().getLatideInicial());
                            trajetoCorrendo.setLongitudeInicial(resultado.getCorrendo().getLongitudeInicial());
                            trajetoCorrendo.setLatitudeFinal(resultado.getCorrendo().getLatitudeFinal());
                            trajetoCorrendo.setLongitudeFinal(resultado.getCorrendo().getLongitudeFinal());
                            userCorrendo.setTrajeto(trajetoCorrendo);
                            rota(userCorrendo, Color.GREEN);
                        }

                        if (resultado.getParados() != null && !resultado.getParados().isEmpty()) {
                            //PONTOS NO QUAL O USUÁRIO FICOU PARADO
                            for (Parado parado : resultado.getParados()) {
                                setCustomMarker(parado);

                            }
                        }

                        if (resultado.getAndando() != null) {
                            //TRAJETO PERCORRIDO ANDANDO
                            Trajeto trajetoAndando = new Trajeto();
                            UsuarioLocalizacao userAndando = new UsuarioLocalizacao();
                            trajetoAndando.setLatitudeInicial(resultado.getAndando().getLatideInicial());
                            trajetoAndando.setLongitudeInicial(resultado.getAndando().getLongitudeInicial());
                            trajetoAndando.setLatitudeFinal(resultado.getAndando().getLatitudeFinal());
                            trajetoAndando.setLongitudeFinal(resultado.getAndando().getLongitudeFinal());
                            userAndando.setTrajeto(trajetoAndando);
                            rota(userAndando, Color.BLUE);
                        }


                    }

                }

                @Override
                public void onFailure(Call<Resultado> call, Throwable t) {
                    t.printStackTrace();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setCustomMarker(Parado parado) {
        LatLng now = new LatLng(parado.getLatitude(), parado.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(now)
                .title("Tempo Parado: " + parado.getSegundo() + " segundos"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(now));
    }

}
