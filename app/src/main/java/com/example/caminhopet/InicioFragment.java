package com.example.caminhopet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class InicioFragment extends Fragment {

    // =========================================================================
    // MODELOS E INTERFACE DA DOG CEO API (Conforme documento do projeto)
    // =========================================================================

    public static class PetViewModel {
        private String nome;
        private String imagem;

        public PetViewModel(String nome, String imagem) {
            this.nome = nome;
            this.imagem = imagem;
        }

        public String getNome() { return nome; }
        public String getImagem() { return imagem; }
    }

    public static class DogApiResponse {
        private Map<String, List<String>> message;
        private String status;

        public Map<String, List<String>> getMessage() { return message; }
        public String getStatus() { return status; }
    }

    public static class DogImageResponse {
        private String message;
        private String status;

        public String getMessage() { return message; }
        public String getStatus() { return status; }
    }

    public interface DogApiService {
        @GET("breeds/list/all")
        Call<DogApiResponse> listarRacas();

        @GET("breed/{raca}/images/random")
        Call<DogImageResponse> buscarImagemPorRaca(@Path("raca") String raca);
    }

    // =========================================================================
    // ATRIBUTOS DA TELA INICIAL
    // =========================================================================

    private EditText etBuscarPet;
    private TextView btnFiltroTodos, btnFiltroPerdidos, btnFiltroEncontrados;
    private RecyclerView rvPetsInicio;

    private PetInicioAdapter adapter;
    private final List<Pet> listaCompleta = new ArrayList<>();
    private final List<Pet> listaRacasApi = new ArrayList<>();
    private String categoriaFiltroAtual = "Todos";

    private DogApiService apiService;
    private final ExecutorService executorImagem = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable buscaApiPendente;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inicio, container, false);

        etBuscarPet = view.findViewById(R.id.etBuscarPet);
        btnFiltroTodos = view.findViewById(R.id.btnFiltroTodos);
        btnFiltroPerdidos = view.findViewById(R.id.btnFiltroPerdidos);
        btnFiltroEncontrados = view.findViewById(R.id.btnFiltroEncontrados);
        rvPetsInicio = view.findViewById(R.id.rvPetsInicio);

        rvPetsInicio.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new PetInicioAdapter(getContext(), listaCompleta, pet -> {
            Intent intent = new Intent(getContext(), DetalhesAnimalActivity.class);
            intent.putExtra("pet_selecionado", pet);
            startActivity(intent);
        });
        rvPetsInicio.setAdapter(adapter);

        // Configura o Retrofit apontando para a Dog CEO API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://dog.ceo/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(DogApiService.class);

        configurarFiltros();
        configurarBusca();
        carregarRacasIniciaisDaApi();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarPetsDoFirebase();
    }

    private void carregarPetsDoFirebase() {
        FirebaseFirestore.getInstance()
                .collection("pets")
                .whereEqualTo("tipoLista", "inicio")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    Pet.listaInicio.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Pet p = doc.toObject(Pet.class);
                        if (p != null) {
                            p.setDocumentoId(doc.getId());
                            Pet.listaInicio.add(p);
                        }
                    }
                    sincronizarListas();
                });
    }

    // Consome https://dog.ceo/api/breeds/list/all e busca as fotos das raças
    private void carregarRacasIniciaisDaApi() {
        if (!listaRacasApi.isEmpty()) return;

        apiService.listarRacas().enqueue(new Callback<DogApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<DogApiResponse> call, @NonNull Response<DogApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getMessage() != null) {
                    List<String> todasAsRacas = new ArrayList<>(response.body().getMessage().keySet());
                    int limite = Math.min(8, todasAsRacas.size());

                    for (int i = 0; i < limite; i++) {
                        buscarCardDaRacaNaApi(todasAsRacas.get(i), false);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<DogApiResponse> call, @NonNull Throwable t) {}
        });
    }

    // Consome https://dog.ceo/api/breed/{raca}/images/random e cria o card na lista
    private void buscarCardDaRacaNaApi(String nomeRaca, boolean exibirNoTopo) {
        apiService.buscarImagemPorRaca(nomeRaca.toLowerCase()).enqueue(new Callback<DogImageResponse>() {
            @Override
            public void onResponse(@NonNull Call<DogImageResponse> call, @NonNull Response<DogImageResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equalsIgnoreCase(response.body().getStatus())) {
                    String urlFoto = response.body().getMessage();
                    PetViewModel viewModel = new PetViewModel(nomeRaca.toUpperCase(), urlFoto);

                    executorImagem.execute(() -> {
                        Bitmap bmp = baixarImagemUrl(viewModel.getImagem());
                        if (bmp != null) {
                            String fotoBase64 = Pet.bitmapParaBase64(bmp);
                            Pet petApi = new Pet(
                                    null,
                                    "Raça: " + viewModel.getNome(),
                                    "Encontrado",
                                    "Cão",
                                    "Área Educativa",
                                    "Médio",
                                    viewModel.getNome(),
                                    "Conheça os Pets (Dog CEO API)",
                                    "Fonte: Dog CEO API",
                                    "Raça consultada dinamicamente na Dog CEO API para a área educativa Conheça os Pets.",
                                    "",
                                    "",
                                    fotoBase64
                            );

                            mainHandler.post(() -> {
                                if (!isAdded()) return;
                                if (exibirNoTopo) {
                                    listaRacasApi.add(0, petApi);
                                } else {
                                    listaRacasApi.add(petApi);
                                }
                                sincronizarListas();
                            });
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<DogImageResponse> call, @NonNull Throwable t) {}
        });
    }

    private void sincronizarListas() {
        listaCompleta.clear();
        listaCompleta.addAll(Pet.listaInicio);
        listaCompleta.addAll(listaRacasApi);
        aplicarFiltroEBusca();
    }

    private void configurarFiltros() {
        btnFiltroTodos.setOnClickListener(v -> atualizarEstiloFiltro("Todos", btnFiltroTodos));
        btnFiltroPerdidos.setOnClickListener(v -> atualizarEstiloFiltro("Perdido", btnFiltroPerdidos));
        btnFiltroEncontrados.setOnClickListener(v -> atualizarEstiloFiltro("Encontrado", btnFiltroEncontrados));
    }

    private void atualizarEstiloFiltro(String categoria, TextView botaoSelecionado) {
        categoriaFiltroAtual = categoria;

        resetarBotao(btnFiltroTodos);
        resetarBotao(btnFiltroPerdidos);
        resetarBotao(btnFiltroEncontrados);

        botaoSelecionado.setBackgroundResource(R.drawable.bg_chip_selecionado);
        botaoSelecionado.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        aplicarFiltroEBusca();
    }

    private void resetarBotao(TextView botao) {
        botao.setBackgroundResource(R.drawable.bg_chip_normal);
        botao.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_escuro));
    }

    private void configurarBusca() {
        etBuscarPet.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (buscaApiPendente != null) {
                    mainHandler.removeCallbacks(buscaApiPendente);
                }
                aplicarFiltroEBusca();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void aplicarFiltroEBusca() {
        if (etBuscarPet == null) return;
        String textoBusca = etBuscarPet.getText().toString().toLowerCase().trim();
        List<Pet> listaFiltrada = new ArrayList<>();

        for (Pet pet : listaCompleta) {
            boolean bateCategoria = categoriaFiltroAtual.equals("Todos") ||
                    pet.getStatus().equalsIgnoreCase(categoriaFiltroAtual);

            boolean bateBusca = textoBusca.isEmpty() ||
                    palavraComecaCom(pet.getNome(), textoBusca) ||
                    palavraComecaCom(pet.getRaca(), textoBusca) ||
                    palavraComecaCom(pet.getCor(), textoBusca) ||
                    palavraComecaCom(pet.getUltimoLocalVisto(), textoBusca);

            if (bateCategoria && bateBusca) {
                listaFiltrada.add(pet);
            }
        }

        adapter.atualizarLista(listaFiltrada);

        // Se o usuário digitou o nome de uma raça que ainda não está na tela, busca na Dog CEO API
        if (listaFiltrada.isEmpty() && textoBusca.length() >= 3) {
            buscaApiPendente = () -> buscarCardDaRacaNaApi(textoBusca, true);
            mainHandler.postDelayed(buscaApiPendente, 500);
        }
    }

    private boolean palavraComecaCom(String textoCampo, String busca) {
        if (textoCampo == null) return false;
        String[] palavras = textoCampo.toLowerCase().split("[\\s,:]+");
        for (String p : palavras) {
            if (p.startsWith(busca)) {
                return true;
            }
        }
        return false;
    }

    private Bitmap baixarImagemUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoInput(true);
            conn.connect();
            InputStream input = conn.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            return null;
        }
    }
}