package com.example.caminhopet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PerfilFragment extends Fragment {

    private CardView cardFotoPerfil;
    private ImageView ivFotoPerfil;
    private EditText etPerfilNome, etPerfilEmail, etPerfilTelefone, etPerfilSenha;

    // Abre a galeria e já atualiza a foto da tela e da Navbar na hora!
    private final ActivityResultLauncher<String> galeriaPerfilLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    AjustadorFoto.abrir(requireContext(), uri, this::atualizarFotoDePerfil);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        cardFotoPerfil = view.findViewById(R.id.cardFotoPerfil);
        ivFotoPerfil = view.findViewById(R.id.ivFotoPerfil);
        TextView tvTrocarFotoPerfil = view.findViewById(R.id.tvTrocarFotoPerfil);
        etPerfilNome = view.findViewById(R.id.etPerfilNome);
        etPerfilEmail = view.findViewById(R.id.etPerfilEmail);
        etPerfilTelefone = view.findViewById(R.id.etPerfilTelefone);
        etPerfilSenha = view.findViewById(R.id.etPerfilSenha);
        Button btnSalvarPerfil = view.findViewById(R.id.btnSalvarPerfil);
        Button btnSairConta = view.findViewById(R.id.btnSairConta);

        carregarDadosAtuais();

        // Tanto clicar no círculo quanto no texto abre a galeria de fotos
        cardFotoPerfil.setOnClickListener(v -> galeriaPerfilLauncher.launch("image/*"));
        tvTrocarFotoPerfil.setOnClickListener(v -> galeriaPerfilLauncher.launch("image/*"));

        btnSalvarPerfil.setOnClickListener(v -> salvarAlteracoes());

        btnSairConta.setOnClickListener(v -> {
            GerenciadorUsuarios.logout(requireContext());
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }

    private void carregarDadosAtuais() {
        Context context = requireContext();
        etPerfilNome.setText(GerenciadorUsuarios.getNomeLogado(context));
        etPerfilEmail.setText(GerenciadorUsuarios.getEmailLogado(context));
        etPerfilTelefone.setText(GerenciadorUsuarios.getTelefoneLogado(context));

        File arquivoFoto = new File(context.getFilesDir(), "foto_perfil_usuario.png");
        if (arquivoFoto.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(arquivoFoto.getAbsolutePath());
            ivFotoPerfil.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivFotoPerfil.setImageBitmap(bitmap);
        }
    }

   private void atualizarFotoDePerfil(Bitmap bitmap) {
        try {
            FileOutputStream fos = requireContext().openFileOutput("foto_perfil_usuario.png", Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();

            ivFotoPerfil.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivFotoPerfil.setImageBitmap(bitmap);

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).carregarFotoPerfilNaNavbar();
            }
            Toast.makeText(getContext(), "Foto ajustada e atualizada!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void salvarAlteracoes() {
        String nome = etPerfilNome.getText().toString().trim();
        String telefone = etPerfilTelefone.getText().toString().trim();
        String novaSenha = etPerfilSenha.getText().toString();

        if (nome.isEmpty() || telefone.isEmpty()) {
            Toast.makeText(getContext(), "Nome e telefone não podem ficar vazios!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!novaSenha.isEmpty() && novaSenha.length() < 6) {
            etPerfilSenha.setError("A nova senha deve ter pelo menos 6 caracteres");
            return;
        }

        GerenciadorUsuarios.atualizarPerfil(requireContext(), nome, telefone, novaSenha);
        etPerfilSenha.setText("");
        Toast.makeText(getContext(), "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show();
    }
}