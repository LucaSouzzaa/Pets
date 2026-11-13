package com.example.caminhopet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConversasFragment extends Fragment {

    public static class ItemConversa {
        public String nomePet;
        public String tipoAssunto;
        public String ultimaMensagem;
        public long timestamp;

        public ItemConversa(String nomePet, String tipoAssunto, String ultimaMensagem, long timestamp) {
            this.nomePet = nomePet;
            this.tipoAssunto = tipoAssunto;
            this.ultimaMensagem = ultimaMensagem;
            this.timestamp = timestamp;
        }
    }

    // Mantido por compatibilidade caso algum trecho antigo chame este método
    public static void registrarMensagem(String nomePet, String tipoAssunto, String mensagem) {
    }

    private LinearLayout containerLista;
    private ListenerRegistration listenerFirebase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = requireContext();

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_creme));
        int p20 = dp(context, 20);
        root.setPadding(p20, p20, p20, 0);

        TextView titulo = new TextView(context);
        titulo.setText("Minhas Conversas");
        titulo.setTextSize(26f);
        titulo.setTextColor(ContextCompat.getColor(context, R.color.verde_principal));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(titulo);

        TextView sub = new TextView(context);
        sub.setText("Toque para abrir ou segure em uma conversa para apagá-la.");
        sub.setTextSize(14f);
        sub.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario));
        sub.setPadding(0, dp(context, 4), 0, dp(context, 16));
        root.addView(sub);

        ScrollView scroll = new ScrollView(context);
        containerLista = new LinearLayout(context);
        containerLista.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(containerLista);
        root.addView(scroll);

        escutarConversasEmTempoReal();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerFirebase != null) {
            listenerFirebase.remove();
        }
    }

    private void escutarConversasEmTempoReal() {
        listenerFirebase = FirebaseFirestore.getInstance()
                .collection("mensagens")
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots == null || !isAdded()) return;

                    List<DocumentSnapshot> docs = new ArrayList<>(snapshots.getDocuments());
                    // Ordena da mensagem mais recente para a mais antiga
                    Collections.sort(docs, (d1, d2) -> {
                        Long t1 = d1.getLong("timestamp");
                        Long t2 = d2.getLong("timestamp");
                        if (t1 == null || t2 == null) return 0;
                        return t2.compareTo(t1);
                    });

                    String meuEmail = GerenciadorUsuarios.getEmailLogado(requireContext());
                    Map<String, ItemConversa> mapaConversas = new LinkedHashMap<>();

                    for (DocumentSnapshot doc : docs) {
                        String pet = doc.getString("nomePet");
                        if (pet == null || pet.trim().isEmpty()) continue;
                        String chave = pet.trim();

                        if (!mapaConversas.containsKey(chave)) {
                            String texto = doc.getString("texto");
                            String remetente = doc.getString("remetente");
                            String nomeRem = doc.getString("nomeRemetente");
                            String assunto = doc.getString("tipoAssunto");
                            Long time = doc.getLong("timestamp");

                            boolean fuiEu = !meuEmail.isEmpty() && remetente != null && meuEmail.equalsIgnoreCase(remetente.trim());
                            String prefixo = fuiEu ? "Você: " : ((nomeRem != null && !nomeRem.isEmpty()) ? nomeRem + ": " : "");

                            mapaConversas.put(chave, new ItemConversa(
                                    chave,
                                    assunto != null ? assunto : "contato",
                                    prefixo + (texto != null ? texto : ""),
                                    time != null ? time : 0L
                            ));
                        }
                    }

                    atualizarCardsConversa(new ArrayList<>(mapaConversas.values()));
                });
    }

    private void atualizarCardsConversa(List<ItemConversa> lista) {
        Context context = getContext();
        if (context == null || containerLista == null) return;
        containerLista.removeAllViews();

        if (lista.isEmpty()) {
            TextView vazio = new TextView(context);
            vazio.setText("Nenhuma conversa iniciada ainda.");
            vazio.setTextSize(15f);
            vazio.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario));
            vazio.setPadding(0, dp(context, 20), 0, 0);
            containerLista.addView(vazio);
            return;
        }

        for (ItemConversa item : lista) {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card_pet);
            int pad = dp(context, 16);
            card.setPadding(pad, pad, pad, pad);

            LinearLayout.LayoutParams paramsCard = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsCard.bottomMargin = dp(context, 12);
            card.setLayoutParams(paramsCard);

            ImageView icone = new ImageView(context);
            icone.setImageResource(R.drawable.ic_chat_balao);
            LinearLayout.LayoutParams paramsIcone = new LinearLayout.LayoutParams(dp(context, 36), dp(context, 36));
            paramsIcone.rightMargin = dp(context, 14);
            icone.setLayoutParams(paramsIcone);
            card.addView(icone);

            LinearLayout textos = new LinearLayout(context);
            textos.setOrientation(LinearLayout.VERTICAL);

            TextView tvNome = new TextView(context);
            tvNome.setText("Chat • " + item.nomePet);
            tvNome.setTextSize(17f);
            tvNome.setTextColor(ContextCompat.getColor(context, R.color.texto_escuro));
            tvNome.setTypeface(null, android.graphics.Typeface.BOLD);
            textos.addView(tvNome);

            TextView tvUltima = new TextView(context);
            tvUltima.setText(item.ultimaMensagem);
            tvUltima.setTextSize(14f);
            tvUltima.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario));
            textos.addView(tvUltima);

            card.addView(textos);

            // Toque simples: abre a conversa
            card.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("nome_pet", item.nomePet);
                intent.putExtra("tipo_assunto", item.tipoAssunto);
                startActivity(intent);
            });

            // Segurar o dedo: apaga a conversa inteira para os dois
            card.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Apagar conversa")
                        .setMessage("Deseja apagar toda a conversa sobre " + item.nomePet + " para todos?")
                        .setPositiveButton("Apagar tudo", (dialog, which) -> apagarConversaInteira(item.nomePet))
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            });

            containerLista.addView(card);
        }
    }

    private void apagarConversaInteira(String nomePet) {
        FirebaseFirestore.getInstance()
                .collection("mensagens")
                .whereEqualTo("nomePet", nomePet)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        doc.getReference().delete();
                    }
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Conversa apagada!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int dp(Context context, int valor) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, valor, context.getResources().getDisplayMetrics());
    }
}