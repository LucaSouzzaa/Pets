package com.example.caminhopet;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout containerMensagens;
    private ScrollView scrollChat;
    private EditText etMensagemChat;
    private String nomePet;
    private String tipoAssunto;
    private String meuEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        TextView tvVoltarChat = findViewById(R.id.tvVoltarChat);
        TextView tvTituloChat = findViewById(R.id.tvTituloChat);
        TextView tvSubtituloChat = findViewById(R.id.tvSubtituloChat);
        containerMensagens = findViewById(R.id.containerMensagens);
        scrollChat = findViewById(R.id.scrollChat);
        etMensagemChat = findViewById(R.id.etMensagemChat);
        Button btnEnviarMensagem = findViewById(R.id.btnEnviarMensagem);

        tvVoltarChat.setOnClickListener(v -> finish());

        nomePet = getIntent().getStringExtra("nome_pet");
        tipoAssunto = getIntent().getStringExtra("tipo_assunto");
        meuEmail = GerenciadorUsuarios.getEmailLogado(this).trim();

        if (nomePet == null) nomePet = "Pet";
        nomePet = nomePet.trim();

        tvTituloChat.setText("Chat • " + nomePet);
        tvSubtituloChat.setText("adocao".equals(tipoAssunto)
                ? "Conversa com o responsável pela adoção"
                : "Conversa com o tutor do animal");

        escutarMensagensEmTempoReal();

        btnEnviarMensagem.setOnClickListener(v -> {
            String texto = etMensagemChat.getText().toString().trim();
            if (!texto.isEmpty()) {
                enviarMensagemFirebase(texto);
                etMensagemChat.setText("");
            }
        });
    }

    private void enviarMensagemFirebase(String texto) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("nomePet", nomePet);
        msg.put("tipoAssunto", tipoAssunto != null ? tipoAssunto : "contato");
        msg.put("remetente", meuEmail);
        msg.put("nomeRemetente", GerenciadorUsuarios.getNomeLogado(this));
        msg.put("texto", texto);
        msg.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("mensagens").add(msg);
    }

    private void escutarMensagensEmTempoReal() {
        FirebaseFirestore.getInstance().collection("mensagens")
                .whereEqualTo("nomePet", nomePet)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots == null) return;
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshots.getDocuments());
                    Collections.sort(docs, (d1, d2) -> {
                        Long t1 = d1.getLong("timestamp");
                        Long t2 = d2.getLong("timestamp");
                        if (t1 == null || t2 == null) return 0;
                        return t1.compareTo(t2);
                    });

                    containerMensagens.removeAllViews();
                    for (DocumentSnapshot doc : docs) {
                        String docId = doc.getId();
                        String texto = doc.getString("texto");
                        String remetente = doc.getString("remetente");
                        String nomeRemetente = doc.getString("nomeRemetente");

                        boolean fuiEu = !meuEmail.isEmpty() && remetente != null
                                && meuEmail.equalsIgnoreCase(remetente.trim());

                        adicionarBolhaMensagem(docId, texto, nomeRemetente, fuiEu);
                    }
                });
    }

    private void adicionarBolhaMensagem(String docId, String texto, String nomeRemetente, boolean enviadaPeloUsuario) {
        // Linha horizontal que ocupa 100% da largura para garantir que Direita e Esquerda nunca se misturem
        LinearLayout linha = new LinearLayout(this);
        linha.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        linha.setOrientation(LinearLayout.VERTICAL);
        linha.setPadding(0, dp(6), 0, dp(6));
        linha.setGravity(enviadaPeloUsuario ? Gravity.END : Gravity.START);

        LinearLayout bolha = new LinearLayout(this);
        bolha.setOrientation(LinearLayout.VERTICAL);
        bolha.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams paramsBolha = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bolha.setLayoutParams(paramsBolha);

        if (enviadaPeloUsuario) {
            // Mensagem enviada por mim -> Canto Direito em Verde
            bolha.setBackgroundResource(R.drawable.bg_botao_verde);
        } else {
            // Mensagem recebida da outra pessoa -> Canto Esquerdo em Branco com o nome dela
            bolha.setBackgroundResource(R.drawable.bg_card_pet);

            if (nomeRemetente != null && !nomeRemetente.isEmpty()) {
                TextView tvAutor = new TextView(this);
                tvAutor.setText(nomeRemetente);
                tvAutor.setTextSize(12f);
                tvAutor.setTypeface(null, android.graphics.Typeface.BOLD);
                tvAutor.setTextColor(ContextCompat.getColor(this, R.color.verde_principal));
                tvAutor.setPadding(0, 0, 0, dp(2));
                bolha.addView(tvAutor);
            }
        }

        TextView tvTexto = new TextView(this);
        tvTexto.setText(texto != null ? texto : "");
        tvTexto.setTextSize(15f);
        tvTexto.setMaxWidth(dp(260));
        tvTexto.setTextColor(ContextCompat.getColor(this,
                enviadaPeloUsuario ? R.color.white : R.color.texto_escuro));
        bolha.addView(tvTexto);

        // Segurar o dedo na mensagem para apagar para os dois (estilo WhatsApp)
        bolha.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Apagar mensagem")
                    .setMessage("Deseja apagar esta mensagem para todos?")
                    .setPositiveButton("Apagar", (dialog, which) -> {
                        FirebaseFirestore.getInstance()
                                .collection("mensagens")
                                .document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "Mensagem apagada!", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true;
        });

        linha.addView(bolha);
        containerMensagens.addView(linha);
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int valorDp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, valorDp, getResources().getDisplayMetrics());
    }
}