package com.example.caminhopet;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GerenciadorUsuarios {

    private static final String PREF_SESSAO = "sessao_atual_caminhopet";

    public interface Callback {
        void onResultado(boolean sucesso, String mensagem);
    }

    public static void cadastrarNoFirebase(Context context, String nome, String email, String telefone, String senha, Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String chaveEmail = email.toLowerCase().trim();

        db.collection("usuarios").document(chaveEmail).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onResultado(false, "Este e-mail já está cadastrado!");
                    } else {
                        Map<String, Object> dados = new HashMap<>();
                        dados.put("nome", nome.trim());
                        dados.put("email", chaveEmail);
                        dados.put("telefone", telefone.trim());
                        dados.put("senha", senha);

                        db.collection("usuarios").document(chaveEmail).set(dados)
                                .addOnSuccessListener(aVoid -> callback.onResultado(true, "Conta criada com sucesso!"))
                                .addOnFailureListener(e -> callback.onResultado(false, "Erro ao salvar: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onResultado(false, "Erro de conexão com o banco."));
    }

    public static void autenticarNoFirebase(Context context, String email, String senhaDigitada, Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String chaveEmail = email.toLowerCase().trim();

        db.collection("usuarios").document(chaveEmail).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String senhaBanco = doc.getString("senha");
                        if (senhaBanco != null && senhaBanco.equals(senhaDigitada)) {
                            String nome = doc.getString("nome");
                            String telefone = doc.getString("telefone");
                            salvarSessaoLocal(context, chaveEmail, nome != null ? nome : "Tutor", telefone != null ? telefone : "");
                            callback.onResultado(true, "Bem-vindo(a), " + nome + "!");
                        } else {
                            callback.onResultado(false, "Senha incorreta!");
                        }
                    } else {
                        callback.onResultado(false, "E-mail não encontrado! Crie sua conta.");
                    }
                })
                .addOnFailureListener(e -> callback.onResultado(false, "Falha ao conectar ao banco de dados."));
    }

    public static void atualizarPerfil(Context context, String novoNome, String novoTelefone, String novaSenha) {
        String emailLogado = getEmailLogado(context);
        if (emailLogado.isEmpty()) return;

        Map<String, Object> atualizacao = new HashMap<>();
        atualizacao.put("nome", novoNome.trim());
        atualizacao.put("telefone", novoTelefone.trim());
        if (novaSenha != null && !novaSenha.isEmpty()) {
            atualizacao.put("senha", novaSenha);
        }

        FirebaseFirestore.getInstance().collection("usuarios").document(emailLogado).update(atualizacao);
        salvarSessaoLocal(context, emailLogado, novoNome.trim(), novoTelefone.trim());
    }

    private static void salvarSessaoLocal(Context context, String email, String nome, String telefone) {
        SharedPreferences sessao = context.getSharedPreferences(PREF_SESSAO, Context.MODE_PRIVATE);
        sessao.edit()
                .putString("email_logado", email)
                .putString("nome_logado", nome)
                .putString("telefone_logado", telefone)
                .apply();
    }

    public static String getEmailLogado(Context context) {
        return context.getSharedPreferences(PREF_SESSAO, Context.MODE_PRIVATE).getString("email_logado", "");
    }

    public static String getNomeLogado(Context context) {
        return context.getSharedPreferences(PREF_SESSAO, Context.MODE_PRIVATE).getString("nome_logado", "Tutor");
    }

    public static String getTelefoneLogado(Context context) {
        return context.getSharedPreferences(PREF_SESSAO, Context.MODE_PRIVATE).getString("telefone_logado", "");
    }

    public static void logout(Context context) {
        context.getSharedPreferences(PREF_SESSAO, Context.MODE_PRIVATE).edit().clear().apply();
    }
}