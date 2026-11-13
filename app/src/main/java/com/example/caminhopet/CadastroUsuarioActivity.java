package com.example.caminhopet;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.FileOutputStream;

public class CadastroUsuarioActivity extends AppCompatActivity {

    private ImageView ivFotoUsuario;
    private EditText etCadastroNome, etCadastroEmail, etCadastroTelefone, etCadastroSenha;

    private final ActivityResultLauncher<String> selecionarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    AjustadorFoto.abrir(this, uri, bitmapRecortado -> {
                        ivFotoUsuario.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivFotoUsuario.setImageBitmap(bitmapRecortado);
                        salvarFotoPerfilLocal(bitmapRecortado);
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cadastro_usuario);

        CardView cardFotoUsuario = findViewById(R.id.cardFotoUsuario);
        ivFotoUsuario = findViewById(R.id.ivFotoUsuario);
        etCadastroNome = findViewById(R.id.etCadastroNome);
        etCadastroEmail = findViewById(R.id.etCadastroEmail);
        etCadastroTelefone = findViewById(R.id.etCadastroTelefone);
        etCadastroSenha = findViewById(R.id.etCadastroSenha);
        Button btnCadastrarUsuario = findViewById(R.id.btnCadastrarUsuario);
        TextView tvVoltarLogin = findViewById(R.id.tvVoltarLogin);

        tvVoltarLogin.setOnClickListener(v -> finish());
        cardFotoUsuario.setOnClickListener(v -> selecionarFotoLauncher.launch("image/*"));
        btnCadastrarUsuario.setOnClickListener(v -> salvarCadastro());
    }

    private void salvarFotoPerfilLocal(Bitmap bitmap) {
        try {
            FileOutputStream fos = openFileOutput("foto_perfil_usuario.png", MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void salvarCadastro() {
        String nome = etCadastroNome.getText().toString().trim();
        String email = etCadastroEmail.getText().toString().trim();
        String telefone = etCadastroTelefone.getText().toString().trim();
        String senha = etCadastroSenha.getText().toString();

        if (nome.isEmpty()) {
            etCadastroNome.setError("Informe seu nome completo");
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etCadastroEmail.setError("Informe um e-mail válido");
            return;
        }
        if (telefone.isEmpty()) {
            etCadastroTelefone.setError("Informe seu telefone");
            return;
        }
        if (senha.length() < 6) {
            etCadastroSenha.setError("A senha deve ter pelo menos 6 caracteres");
            return;
        }

        GerenciadorUsuarios.cadastrarNoFirebase(this, nome, email, telefone, senha, (sucesso, mensagem) -> {
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
            if (sucesso) {
                finish();
            }
        });
    }
}