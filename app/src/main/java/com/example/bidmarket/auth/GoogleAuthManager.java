
package com.example.bidmarket.auth;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.bidmarket.R;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Gestiona exclusivamente la autenticación
 * de Google mediante Credential Manager y Firebase.
 */
public final class GoogleAuthManager {

    public interface Callback {
        void onSuccess(@NonNull FirebaseUser user);
        void onError(@NonNull String mensaje);
    }

    private final Activity activity;
    private final CredentialManager credentialManager;
    private final FirebaseAuth firebaseAuth;

    public GoogleAuthManager(Activity activity) {
        this.activity = activity;
        this.credentialManager =
                CredentialManager.create(activity);

        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    /**
     * Inicia el flujo explícito del botón Google.
     */
    public void iniciar(@NonNull Callback callback) {

        GetSignInWithGoogleOption opcion =
                new GetSignInWithGoogleOption.Builder(
                        activity.getString(
                                R.string.default_web_client_id
                        )
                ).build();

        GetCredentialRequest solicitud =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(opcion)
                        .build();

        credentialManager.getCredentialAsync(
                activity,
                solicitud,
                null,
                activity.getMainExecutor(),
                new CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException>() {

                    @Override
                    public void onResult(
                            GetCredentialResponse resultado
                    ) {
                        procesarCredencial(
                                resultado.getCredential(),
                                callback
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull GetCredentialException error
                    ) {
                        callback.onError(
                                "No se pudo completar el acceso con Google"
                        );
                    }
                }
        );
    }

    private void procesarCredencial(
            Credential credential,
            Callback callback
    ) {

        if (!(credential instanceof CustomCredential)) {
            callback.onError(
                    "Tipo de credencial no compatible"
            );
            return;
        }

        CustomCredential personalizada =
                (CustomCredential) credential;

        if (!GoogleIdTokenCredential
                .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(personalizada.getType())) {

            callback.onError(
                    "La respuesta no contiene una credencial Google"
            );
            return;
        }

        try {
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(
                            personalizada.getData()
                    );

            autenticarFirebase(
                    googleCredential.getIdToken(),
                    callback
            );

        } catch (IllegalArgumentException error) {
            callback.onError(
                    "La credencial recibida de Google no es válida"
            );
        }
    }

    private void autenticarFirebase(
            String idToken,
            Callback callback
    ) {

        AuthCredential credencial =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null
                );

        firebaseAuth.signInWithCredential(credencial)
                .addOnCompleteListener(activity, tarea -> {

                    if (!tarea.isSuccessful()) {
                        callback.onError(
                                "Firebase rechazó la autenticación"
                        );
                        return;
                    }

                    FirebaseUser usuario =
                            firebaseAuth.getCurrentUser();

                    if (usuario == null) {
                        callback.onError(
                                "No se pudo recuperar la cuenta"
                        );
                        return;
                    }

                    callback.onSuccess(usuario);
                });
    }
}
