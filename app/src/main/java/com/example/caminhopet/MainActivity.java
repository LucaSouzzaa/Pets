package com.example.caminhopet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setItemIconTintList(null);

        carregarFotoPerfilNaNavbar();

        if (savedInstanceState == null) {
            carregarFragment(new InicioFragment());
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                carregarFragment(new InicioFragment());
                return true;
            } else if (id == R.id.nav_cadastrar) {
                abrirOpcoesCadastro();
                return false;
            } else if (id == R.id.nav_adocao) {
                carregarFragment(new AdocaoFragment());
                return true;
            } else if (id == R.id.nav_chat) {
                carregarFragment(new ConversasFragment());
                return true;
            } else if (id == R.id.nav_perfil) {
                carregarFragment(new PerfilFragment());
                return true;
            }
            return false;
        });
    }

    private void carregarFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void abrirOpcoesCadastro() {
        View viewModal = LayoutInflater.from(this).inflate(R.layout.dialog_opcoes_cadastro, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(viewModal)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnModalPerdido = viewModal.findViewById(R.id.btnModalPerdido);
        Button btnModalAdocao = viewModal.findViewById(R.id.btnModalAdocao);
        TextView tvCancelarModal = viewModal.findViewById(R.id.tvCancelarModal);

        btnModalPerdido.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, CadastrarPerdidoActivity.class));
        });

        btnModalAdocao.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, CadastrarAdocaoActivity.class));
        });

        tvCancelarModal.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public void carregarFotoPerfilNaNavbar() {
        try {
            File arquivoFoto = new File(getFilesDir(), "foto_perfil_usuario.png");
            if (arquivoFoto.exists()) {
                Bitmap bitmapOriginal = BitmapFactory.decodeFile(arquivoFoto.getAbsolutePath());
                if (bitmapOriginal != null) {
                    Bitmap bitmapRedondo = recortarEmCirculo(bitmapOriginal);
                    BitmapDrawable iconeUsuario = new BitmapDrawable(getResources(), bitmapRedondo);
                    bottomNavigation.setItemIconTintList(null);
                    bottomNavigation.getMenu().findItem(R.id.nav_perfil).setIcon(iconeUsuario);
                }
            } else {
                bottomNavigation.getMenu().findItem(R.id.nav_perfil).setIcon(R.drawable.ic_perfil_padrao);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap recortarEmCirculo(Bitmap bitmap) {
        int tamanho = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap saida = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(saida);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, tamanho, tamanho);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(tamanho / 2f, tamanho / 2f, tamanho / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return Bitmap.createScaledBitmap(saida, 96, 96, true);
    }
}