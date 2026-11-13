package com.example.caminhopet;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.io.InputStream;

public class AjustadorFoto {

    public interface OnFotoAjustadaListener {
        void onFotoPronta(Bitmap bitmapRecortado);
    }

    public static void abrir(Context context, Uri uriImagem, OnFotoAjustadaListener listener) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uriImagem);
            Bitmap bitmapBruto = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            if (bitmapBruto == null) return;

            // Corrige automaticamente a foto que vem deitada da câmera/galeria
            Bitmap bitmapInicial = corrigirRotacaoExif(context, uriImagem, bitmapBruto);

            int padding = dp(context, 20);
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.setPadding(padding, padding, padding, padding);
            root.setBackgroundResource(R.drawable.bg_card_pet);

            TextView titulo = new TextView(context);
            titulo.setText("Ajustar enquadramento");
            titulo.setTextSize(20f);
            titulo.setTextColor(ContextCompat.getColor(context, R.color.verde_principal));
            titulo.setTypeface(null, android.graphics.Typeface.BOLD);
            root.addView(titulo);

            TextView sub = new TextView(context);
            sub.setText("Arraste a foto com o dedo e ajuste o zoom:");
            sub.setTextSize(13f);
            sub.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario));
            sub.setPadding(0, dp(context, 4), 0, dp(context, 12));
            root.addView(sub);

            // Quadro proporcional 1:1 (Quadrado)
            int tamanhoQuadro = dp(context, 240);
            FrameLayout quadro = new FrameLayout(context);
            LinearLayout.LayoutParams paramsQuadro = new LinearLayout.LayoutParams(tamanhoQuadro, tamanhoQuadro);
            quadro.setLayoutParams(paramsQuadro);
            quadro.setBackgroundResource(R.drawable.bg_borda_tracejada);
            quadro.setClipChildren(true);
            quadro.setClipToPadding(true);

            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.MATRIX);
            imageView.setImageBitmap(bitmapInicial);
            quadro.addView(imageView);
            root.addView(quadro);

            final Bitmap[] bitmapAtual = {bitmapInicial};
            final Matrix matrix = new Matrix();
            final float[] ultimoToqueX = {0f};
            final float[] ultimoToqueY = {0f};
            final SeekBar seekZoom = new SeekBar(context);
            final float[] progressoAnterior = {1f};

            // Centraliza e preenche o quadro proporcionalmente
            Runnable centralizarFoto = () -> {
                float vw = quadro.getWidth();
                float vh = quadro.getHeight();
                float bw = bitmapAtual[0].getWidth();
                float bh = bitmapAtual[0].getHeight();

                float escalaBase = Math.max(vw / bw, vh / bh);
                float posX = (vw - bw * escalaBase) / 2f;
                float posY = (vh - bh * escalaBase) / 2f;

                matrix.reset();
                matrix.setScale(escalaBase, escalaBase);
                matrix.postTranslate(posX, posY);
                imageView.setImageMatrix(matrix);
                progressoAnterior[0] = 1f;
                seekZoom.setProgress(0);
            };

            quadro.post(centralizarFoto);

            // Arrastar com o dedo
            imageView.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        ultimoToqueX[0] = event.getX();
                        ultimoToqueY[0] = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - ultimoToqueX[0];
                        float dy = event.getY() - ultimoToqueY[0];
                        matrix.postTranslate(dx, dy);
                        imageView.setImageMatrix(matrix);
                        ultimoToqueX[0] = event.getX();
                        ultimoToqueY[0] = event.getY();
                        break;
                }
                return true;
            });

            // Botão para girar 90 graus caso queira girar manualmente
            TextView btnGirar = new TextView(context);
            btnGirar.setText("↻ Girar foto 90°");
            btnGirar.setTextColor(ContextCompat.getColor(context, R.color.verde_principal));
            btnGirar.setTypeface(null, android.graphics.Typeface.BOLD);
            btnGirar.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 4));
            btnGirar.setOnClickListener(v -> {
                Matrix mRot = new Matrix();
                mRot.postRotate(90);
                bitmapAtual[0] = Bitmap.createBitmap(bitmapAtual[0], 0, 0,
                        bitmapAtual[0].getWidth(), bitmapAtual[0].getHeight(), mRot, true);
                imageView.setImageBitmap(bitmapAtual[0]);
                centralizarFoto.run();
            });
            root.addView(btnGirar);

            // Barra de Zoom
            seekZoom.setMax(200);
            seekZoom.setProgress(0);
            seekZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float novoMultiplicador = 1f + (progress / 100f);
                    float fator = novoMultiplicador / progressoAnterior[0];
                    progressoAnterior[0] = novoMultiplicador;

                    matrix.postScale(fator, fator, quadro.getWidth() / 2f, quadro.getHeight() / 2f);
                    imageView.setImageMatrix(matrix);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            root.addView(seekZoom);

            // Botões Confirmar e Cancelar
            LinearLayout linhaBotoes = new LinearLayout(context);
            linhaBotoes.setOrientation(LinearLayout.HORIZONTAL);
            linhaBotoes.setPadding(0, dp(context, 16), 0, 0);

            Button btnCancelar = new Button(context);
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(context, 48), 1f);
            p1.rightMargin = dp(context, 6);
            btnCancelar.setLayoutParams(p1);
            btnCancelar.setText("Cancelar");
            btnCancelar.setAllCaps(false);
            btnCancelar.setTextColor(Color.WHITE);
            btnCancelar.setBackgroundResource(R.drawable.bg_botao_verde);

            Button btnConfirmar = new Button(context);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(context, 48), 1f);
            p2.leftMargin = dp(context, 6);
            btnConfirmar.setLayoutParams(p2);
            btnConfirmar.setText("Confirmar foto");
            btnConfirmar.setAllCaps(false);
            btnConfirmar.setTextColor(Color.WHITE);
            btnConfirmar.setBackgroundResource(R.drawable.bg_botao_verde);

            linhaBotoes.addView(btnCancelar);
            linhaBotoes.addView(btnConfirmar);
            root.addView(linhaBotoes);

            AlertDialog dialog = new AlertDialog.Builder(context).setView(root).create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            btnCancelar.setOnClickListener(v -> dialog.dismiss());
            btnConfirmar.setOnClickListener(v -> {
                Bitmap recorte = Bitmap.createBitmap(quadro.getWidth(), quadro.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(recorte);
                imageView.draw(canvas);
                dialog.dismiss();
                listener.onFotoPronta(recorte);
            });

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Lê os metadados da foto e deixa ela em pé automaticamente
    private static Bitmap corrigirRotacaoExif(Context context, Uri uri, Bitmap bitmap) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return bitmap;
            ExifInterface exif = new ExifInterface(input);
            int orientacao = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            input.close();

            int graus = 0;
            if (orientacao == ExifInterface.ORIENTATION_ROTATE_90) graus = 90;
            else if (orientacao == ExifInterface.ORIENTATION_ROTATE_180) graus = 180;
            else if (orientacao == ExifInterface.ORIENTATION_ROTATE_270) graus = 270;

            if (graus != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(graus);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (Exception ignored) {}
        return bitmap;
    }

    private static int dp(Context context, int valorDp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, valorDp, context.getResources().getDisplayMetrics());
    }
}