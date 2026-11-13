package com.example.caminhopet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class DetalhesAnimalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_animal);

        TextView tvVoltarDetalhes = findViewById(R.id.tvVoltarDetalhes);
        TextView tvBadgeDetalhe = findViewById(R.id.tvBadgeDetalhe);
        TextView tvNomeDetalhe = findViewById(R.id.tvNomeDetalhe);
        TextView tvEspecieDetalhe = findViewById(R.id.tvEspecieDetalhe);
        TextView tvCorDetalhe = findViewById(R.id.tvCorDetalhe);
        TextView tvPorteDetalhe = findViewById(R.id.tvPorteDetalhe);
        TextView tvRacaDetalhe = findViewById(R.id.tvRacaDetalhe);
        TextView tvLocalDetalhe = findViewById(R.id.tvLocalDetalhe);
        TextView tvDescricaoDetalhe = findViewById(R.id.tvDescricaoDetalhe);
        ImageView ivFotoDetalhe = findViewById(R.id.ivFotoDetalhe);
        LinearLayout btnAbrirMapa = findViewById(R.id.btnAbrirMapa);
        Button btnEntrarContato = findViewById(R.id.btnEntrarContato);
        Button btnMarcarEncontrado = findViewById(R.id.btnMarcarEncontrado);

        btnMarcarEncontrado.setText("Marcar como encontrado");
        tvVoltarDetalhes.setOnClickListener(v -> finish());

        Pet pet = (Pet) getIntent().getSerializableExtra("pet_selecionado");

        if (pet != null) {
            String emailLogado = GerenciadorUsuarios.getEmailLogado(this);
            boolean souDonoDoAlerta = !emailLogado.isEmpty() && emailLogado.equalsIgnoreCase(pet.getEmailAutor());

            atualizarBadgeNaTela(tvBadgeDetalhe, btnMarcarEncontrado, pet.getStatus(), souDonoDoAlerta);

            tvNomeDetalhe.setText(pet.getNome());
            tvEspecieDetalhe.setText(pet.getEspecie());
            tvCorDetalhe.setText(pet.getCor());
            tvPorteDetalhe.setText(pet.getPorte());
            tvRacaDetalhe.setText(pet.getRaca());
            tvLocalDetalhe.setText(pet.getUltimoLocalVisto());
            tvDescricaoDetalhe.setText(pet.getDescricao());

            Bitmap bmp = Pet.base64ParaBitmap(pet.getFotoBase64());
            if (bmp != null && ivFotoDetalhe != null) {
                ivFotoDetalhe.setImageTintList(null);
                ivFotoDetalhe.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                ivFotoDetalhe.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                ivFotoDetalhe.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivFotoDetalhe.setImageBitmap(bmp);
                if (ivFotoDetalhe.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) ivFotoDetalhe.getParent();
                    if (parent.getChildCount() > 1) {
                        parent.getChildAt(1).setVisibility(View.GONE);
                    }
                }
            }

            btnAbrirMapa.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(pet.getUltimoLocalVisto()));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                startActivity(mapIntent);
            });

            btnEntrarContato.setOnClickListener(v -> {
                Intent intent = new Intent(DetalhesAnimalActivity.this, ChatActivity.class);
                intent.putExtra("nome_pet", pet.getNome());
                intent.putExtra("tipo_assunto", "contato");
                startActivity(intent);
            });

            btnMarcarEncontrado.setOnClickListener(v -> {
                pet.setStatus("Encontrado");
                if (pet.getDocumentoId() != null) {
                    FirebaseFirestore.getInstance()
                            .collection("pets")
                            .document(pet.getDocumentoId())
                            .update("status", "Encontrado", "tempoStatus", "Encontrado recentemente");
                }
                atualizarBadgeNaTela(tvBadgeDetalhe, btnMarcarEncontrado, "Encontrado", souDonoDoAlerta);
                Toast.makeText(this, "Status atualizado para: Encontrado!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void atualizarBadgeNaTela(TextView tvBadge, Button btnMarcarEncontrado, String status, boolean souDonoDoAlerta) {
        tvBadge.setText(status);
        if ("Encontrado".equalsIgnoreCase(status)) {
            tvBadge.setBackgroundResource(R.drawable.bg_badge_encontrado);
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_encontrado_texto));
            btnMarcarEncontrado.setVisibility(View.GONE);
        } else {
            tvBadge.setBackgroundResource(R.drawable.bg_badge_perdido);
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_perdido_texto));
            btnMarcarEncontrado.setVisibility(souDonoDoAlerta ? View.VISIBLE : View.GONE);
        }
    }
}