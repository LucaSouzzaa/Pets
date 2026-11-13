package com.example.caminhopet;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CadastrarPerdidoActivity extends AppCompatActivity {

    private ScrollView scrollCadastrarPerdido;
    private LinearLayout btnAdicionarFotoPerdido;
    private ImageView ivPreviewFotoPerdido;
    private TextView tvTextoFotoPerdido;
    private TextView btnEspecieCao, btnEspecieGato, btnEspecieOutro;
    private EditText etNomePerdido, etRacaPerdido, etCorPerdido, etLocalPerdido, etDataPerdido, etObservacoesPerdido;
    private Button btnPublicarAlerta;

    private String especieSelecionada = "Cão";
    private String fotoBase64 = "";

    private final ActivityResultLauncher<String> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    AjustadorFoto.abrir(this, uri, bitmapRecortado -> {
                        fotoBase64 = Pet.bitmapParaBase64(bitmapRecortado);
                        tvTextoFotoPerdido.setVisibility(View.GONE);
                        ivPreviewFotoPerdido.setImageTintList(null);
                        ivPreviewFotoPerdido.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                        ivPreviewFotoPerdido.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                        ivPreviewFotoPerdido.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivPreviewFotoPerdido.setImageBitmap(bitmapRecortado);
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_perdido);

        scrollCadastrarPerdido = findViewById(R.id.scrollCadastrarPerdido);
        btnAdicionarFotoPerdido = findViewById(R.id.btnAdicionarFotoPerdido);
        ivPreviewFotoPerdido = findViewById(R.id.ivPreviewFotoPerdido);
        tvTextoFotoPerdido = findViewById(R.id.tvTextoFotoPerdido);
        btnEspecieCao = findViewById(R.id.btnEspecieCao);
        btnEspecieGato = findViewById(R.id.btnEspecieGato);
        btnEspecieOutro = findViewById(R.id.btnEspecieOutro);
        etNomePerdido = findViewById(R.id.etNomePerdido);
        etRacaPerdido = findViewById(R.id.etRacaPerdido);
        etCorPerdido = findViewById(R.id.etCorPerdido);
        etLocalPerdido = findViewById(R.id.etLocalPerdido);
        etDataPerdido = findViewById(R.id.etDataPerdido);
        etObservacoesPerdido = findViewById(R.id.etObservacoesPerdido);
        btnPublicarAlerta = findViewById(R.id.btnPublicarAlerta);

        btnAdicionarFotoPerdido.setOnClickListener(v -> galeriaLauncher.launch("image/*"));

        btnEspecieCao.setOnClickListener(v -> selecionarEspecie("Cão", btnEspecieCao));
        btnEspecieGato.setOnClickListener(v -> selecionarEspecie("Gato", btnEspecieGato));
        btnEspecieOutro.setOnClickListener(v -> selecionarEspecie("Outro", btnEspecieOutro));

        etDataPerdido.setOnClickListener(v -> abrirCalendario());

        // Garante que nos aparelhos Samsung a tela suba automaticamente ao tocar em Observações
        etObservacoesPerdido.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                rolarParaObservacoes();
            }
        });
        etObservacoesPerdido.setOnClickListener(v -> rolarParaObservacoes());

        btnPublicarAlerta.setOnClickListener(v -> publicarAlerta());
    }

    private void rolarParaObservacoes() {
        scrollCadastrarPerdido.postDelayed(() ->
                scrollCadastrarPerdido.smoothScrollTo(0, etObservacoesPerdido.getTop() - 40), 300);
    }

    private void selecionarEspecie(String especie, TextView botaoAtivo) {
        especieSelecionada = especie;
        resetarBotao(btnEspecieCao);
        resetarBotao(btnEspecieGato);
        resetarBotao(btnEspecieOutro);
        botaoAtivo.setBackgroundResource(R.drawable.bg_botao_verde);
        botaoAtivo.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void resetarBotao(TextView botao) {
        botao.setBackgroundResource(R.drawable.bg_input);
        botao.setTextColor(ContextCompat.getColor(this, R.color.texto_escuro));
    }

    private void abrirCalendario() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dataFormatada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            etDataPerdido.setText(dataFormatada);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void publicarAlerta() {
        String nome = etNomePerdido.getText().toString().trim();
        String raca = etRacaPerdido.getText().toString().trim();
        String cor = etCorPerdido.getText().toString().trim();
        String local = etLocalPerdido.getText().toString().trim();
        String data = etDataPerdido.getText().toString().trim();
        String obs = etObservacoesPerdido.getText().toString().trim();

        if (local.isEmpty() || data.isEmpty()) {
            Toast.makeText(this, "Informe o último local visto e a data!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nome.isEmpty()) nome = "Sem nome";
        if (raca.isEmpty()) raca = "SRD";
        if (cor.isEmpty()) cor = "Não informada";

        btnPublicarAlerta.setEnabled(false);
        btnPublicarAlerta.setText("Publicando...");

        Map<String, Object> dadosPet = new HashMap<>();
        dadosPet.put("tipoLista", "inicio");
        dadosPet.put("nome", nome);
        dadosPet.put("status", "Perdido");
        dadosPet.put("especie", especieSelecionada);
        dadosPet.put("cor", cor);
        dadosPet.put("porte", "Médio");
        dadosPet.put("raca", raca);
        dadosPet.put("ultimoLocalVisto", local);
        dadosPet.put("tempoStatus", "Desaparecido em " + data);
        dadosPet.put("descricao", obs.isEmpty() ? "Sem observações adicionais." : obs);
        dadosPet.put("telefoneTutor", GerenciadorUsuarios.getTelefoneLogado(this));
        dadosPet.put("emailAutor", GerenciadorUsuarios.getEmailLogado(this));
        dadosPet.put("fotoBase64", fotoBase64);
        dadosPet.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("pets").add(dadosPet)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Alerta publicado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublicarAlerta.setEnabled(true);
                    btnPublicarAlerta.setText("Publicar alerta");
                    Toast.makeText(this, "Erro ao publicar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}