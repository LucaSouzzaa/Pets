package com.example.caminhopet;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CadastrarAdocaoActivity extends AppCompatActivity {

    private LinearLayout btnFotoAdocao;
    private ImageView ivPreviewFotoAdocao;
    private TextView tvTextoFotoAdocao;
    private EditText etNomeAdocao, etIdadeAdocao, etDescricaoAdocao;
    private TextView btnAdocaoCao, btnAdocaoGato, btnAdocaoOutro;
    private TextView btnPortePequeno, btnPorteMedio, btnPorteGrande;
    private TextView btnSexoMacho, btnSexoFemea;
    private CheckBox cbVacinado, cbCastrado;
    private Button btnSalvarAdocao;

    private String especie = "Cão";
    private String porte = "Pequeno";
    private String sexo = "Macho";
    private String fotoBase64 = "";

    private final ActivityResultLauncher<String> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    AjustadorFoto.abrir(this, uri, bitmapRecortado -> {
                        fotoBase64 = Pet.bitmapParaBase64(bitmapRecortado);
                        tvTextoFotoAdocao.setVisibility(View.GONE);
                        ivPreviewFotoAdocao.setImageTintList(null);
                        ivPreviewFotoAdocao.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                        ivPreviewFotoAdocao.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                        ivPreviewFotoAdocao.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivPreviewFotoAdocao.setImageBitmap(bitmapRecortado);
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_adocao);

        btnFotoAdocao = findViewById(R.id.btnFotoAdocao);
        ivPreviewFotoAdocao = findViewById(R.id.ivPreviewFotoAdocao);
        tvTextoFotoAdocao = findViewById(R.id.tvTextoFotoAdocao);
        etNomeAdocao = findViewById(R.id.etNomeAdocao);
        etIdadeAdocao = findViewById(R.id.etIdadeAdocao);
        etDescricaoAdocao = findViewById(R.id.etDescricaoAdocao);

        btnAdocaoCao = findViewById(R.id.btnAdocaoCao);
        btnAdocaoGato = findViewById(R.id.btnAdocaoGato);
        btnAdocaoOutro = findViewById(R.id.btnAdocaoOutro);

        btnPortePequeno = findViewById(R.id.btnPortePequeno);
        btnPorteMedio = findViewById(R.id.btnPorteMedio);
        btnPorteGrande = findViewById(R.id.btnPorteGrande);

        btnSexoMacho = findViewById(R.id.btnSexoMacho);
        btnSexoFemea = findViewById(R.id.btnSexoFemea);

        cbVacinado = findViewById(R.id.cbVacinado);
        cbCastrado = findViewById(R.id.cbCastrado);
        btnSalvarAdocao = findViewById(R.id.btnSalvarAdocao);

        btnFotoAdocao.setOnClickListener(v -> galeriaLauncher.launch("image/*"));

        btnAdocaoCao.setOnClickListener(v -> { especie = "Cão"; destacar(btnAdocaoCao, btnAdocaoGato, btnAdocaoOutro); });
        btnAdocaoGato.setOnClickListener(v -> { especie = "Gato"; destacar(btnAdocaoGato, btnAdocaoCao, btnAdocaoOutro); });
        btnAdocaoOutro.setOnClickListener(v -> { especie = "Outro"; destacar(btnAdocaoOutro, btnAdocaoCao, btnAdocaoGato); });

        btnPortePequeno.setOnClickListener(v -> { porte = "Pequeno"; destacar(btnPortePequeno, btnPorteMedio, btnPorteGrande); });
        btnPorteMedio.setOnClickListener(v -> { porte = "Médio"; destacar(btnPorteMedio, btnPortePequeno, btnPorteGrande); });
        btnPorteGrande.setOnClickListener(v -> { porte = "Grande"; destacar(btnPorteGrande, btnPortePequeno, btnPorteMedio); });

        btnSexoMacho.setOnClickListener(v -> { sexo = "Macho"; destacar(btnSexoMacho, btnSexoFemea); });
        btnSexoFemea.setOnClickListener(v -> { sexo = "Fêmea"; destacar(btnSexoFemea, btnSexoMacho); });

        btnSalvarAdocao.setOnClickListener(v -> salvarPetAdocao());
    }

    private void destacar(TextView ativo, TextView... inativos) {
        ativo.setBackgroundResource(R.drawable.bg_botao_verde);
        ativo.setTextColor(ContextCompat.getColor(this, R.color.white));
        for (TextView inativo : inativos) {
            inativo.setBackgroundResource(R.drawable.bg_input);
            inativo.setTextColor(ContextCompat.getColor(this, R.color.texto_escuro));
        }
    }

    private void salvarPetAdocao() {
        String nome = etNomeAdocao.getText().toString().trim();
        String idade = etIdadeAdocao.getText().toString().trim();
        String descricao = etDescricaoAdocao.getText().toString().trim();

        if (nome.isEmpty() || idade.isEmpty()) {
            Toast.makeText(this, "Preencha pelo menos o nome e a idade do pet!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSalvarAdocao.setEnabled(false);
        btnSalvarAdocao.setText("Cadastrando...");

        String extras = sexo + (cbVacinado.isChecked() ? " • Vacinado" : "") + (cbCastrado.isChecked() ? " • Castrado" : "");

        Map<String, Object> dadosPet = new HashMap<>();
        dadosPet.put("tipoLista", "adocao");
        dadosPet.put("nome", nome);
        dadosPet.put("status", "Para adoção");
        dadosPet.put("especie", especie);
        dadosPet.put("cor", idade);
        dadosPet.put("porte", porte);
        dadosPet.put("raca", extras);
        dadosPet.put("ultimoLocalVisto", "São Paulo, SP");
        dadosPet.put("tempoStatus", GerenciadorUsuarios.getNomeLogado(this));
        dadosPet.put("descricao", descricao.isEmpty() ? extras : descricao + " (" + extras + ")");
        dadosPet.put("telefoneTutor", GerenciadorUsuarios.getTelefoneLogado(this));
        dadosPet.put("emailAutor", GerenciadorUsuarios.getEmailLogado(this));
        dadosPet.put("fotoBase64", fotoBase64);
        dadosPet.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("pets").add(dadosPet)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, nome + " cadastrado(a) para adoção!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSalvarAdocao.setEnabled(true);
                    btnSalvarAdocao.setText("Cadastrar");
                    Toast.makeText(this, "Erro ao cadastrar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}