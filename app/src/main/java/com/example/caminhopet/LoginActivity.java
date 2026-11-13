package com.example.caminhopet;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail, etLoginSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginSenha = findViewById(R.id.etLoginSenha);
        Button btnEntrar = findViewById(R.id.btnEntrar);
        TextView tvEsqueciSenha = findViewById(R.id.tvEsqueciSenha);
        TextView tvCriarConta = findViewById(R.id.tvCriarConta);

        tvEsqueciSenha.setPaintFlags(tvEsqueciSenha.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvCriarConta.setPaintFlags(tvCriarConta.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        btnEntrar.setOnClickListener(v -> validarLogin());
        tvEsqueciSenha.setOnClickListener(v -> abrirDialogRecuperarSenha());

        tvCriarConta.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CadastroUsuarioActivity.class);
            startActivity(intent);
        });
    }

    private void validarLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String senha = etLoginSenha.getText().toString();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.setError("Digite um e-mail válido");
            etLoginEmail.requestFocus();
            return;
        }
        if (senha.isEmpty()) {
            etLoginSenha.setError("Digite sua senha");
            etLoginSenha.requestFocus();
            return;
        }

        GerenciadorUsuarios.autenticarNoFirebase(this, email, senha, (sucesso, mensagem) -> {
            Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
            if (sucesso) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void abrirDialogRecuperarSenha() {
        View viewModal = getLayoutInflater().inflate(R.layout.dialog_recuperar_senha, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(viewModal)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etEmailRecuperacao = viewModal.findViewById(R.id.etEmailRecuperacao);
        Button btnCancelar = viewModal.findViewById(R.id.btnCancelarRecuperacao);
        Button btnEnviar = viewModal.findViewById(R.id.btnEnviarRecuperacao);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnEnviar.setOnClickListener(v -> {
            String email = etEmailRecuperacao.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmailRecuperacao.setError("Digite um e-mail válido");
            } else {
                Toast.makeText(this, "Instruções enviadas para: " + email, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}