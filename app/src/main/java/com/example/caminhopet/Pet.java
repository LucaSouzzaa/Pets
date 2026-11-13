package com.example.caminhopet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Pet implements Serializable {
    private String documentoId;
    private String nome;
    private String status;
    private String especie;
    private String cor;
    private String porte;
    private String raca;
    private String ultimoLocalVisto;
    private String tempoStatus;
    private String descricao;
    private String telefoneTutor;
    private String emailAutor;
    private String fotoBase64;

    public static final List<Pet> listaInicio = new ArrayList<>();
    public static final List<Pet> listaAdocao = new ArrayList<>();

    public Pet() {
        // Construtor vazio exigido pelo Firebase
    }

    public Pet(String documentoId, String nome, String status, String especie, String cor, String porte,
               String raca, String ultimoLocalVisto, String tempoStatus, String descricao,
               String telefoneTutor, String emailAutor, String fotoBase64) {
        this.documentoId = documentoId;
        this.nome = nome;
        this.status = status;
        this.especie = especie;
        this.cor = cor;
        this.porte = porte;
        this.raca = raca;
        this.ultimoLocalVisto = ultimoLocalVisto;
        this.tempoStatus = tempoStatus;
        this.descricao = descricao;
        this.telefoneTutor = telefoneTutor;
        this.emailAutor = emailAutor;
        this.fotoBase64 = fotoBase64;
    }

    public static String bitmapParaBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    public static Bitmap base64ParaBitmap(String base64Str) {
        if (base64Str == null || base64Str.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    public String getDocumentoId() { return documentoId; }
    public void setDocumentoId(String documentoId) { this.documentoId = documentoId; }
    public String getNome() { return nome; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEspecie() { return especie; }
    public String getCor() { return cor; }
    public String getPorte() { return porte; }
    public String getRaca() { return raca; }
    public String getUltimoLocalVisto() { return ultimoLocalVisto; }
    public String getTempoStatus() { return tempoStatus; }
    public void setTempoStatus(String tempoStatus) { this.tempoStatus = tempoStatus; }
    public String getDescricao() { return descricao; }
    public String getTelefoneTutor() { return telefoneTutor; }
    public String getEmailAutor() { return emailAutor; }
    public String getFotoBase64() { return fotoBase64; }
    public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }
}