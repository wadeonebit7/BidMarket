
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
 * Solicita una credencial de Google mediante Credential Manager y la usa para
 * autenticar al usuario en Firebase. No crea ni consulta el perfil local.
 */
public final class GoogleAuthManager {

    /**
     * Recibe el resultado del acceso con Google.
     */
    public interface Callback {
        /**
         * Informa que Firebase autenticó la cuenta de Google.
         *
         * @param user usuario autenticado en Firebase.
         */
        void onSuccess(@NonNull FirebaseUser user);
        /**
         * Informa que no se pudo completar el acceso.
         *
         * @param mensaje texto que explica por qué falló el acceso.
         */
        void onError(@NonNull String mensaje);
    }

    private final Activity activity;
    private final CredentialManager credentialManager;
    private final FirebaseAuth firebaseAuth;

    /**
     * Prepara los servicios que se usan para iniciar sesión con Google.
     *
     * @param activity pantalla desde la que se solicita la credencial.
     */
    public GoogleAuthManager(Activity activity) {
        this.activity = activity;
        this.credentialManager =
                CredentialManager.create(activity);

        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    /**
     * Solicita la credencial al tocar el botón de Google y procesa la respuesta.
     *
     * @param callback recibe el usuario autenticado o un mensaje de error.
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

                    /**
                     * Entrega la credencial recibida al método que la valida.
                     *
                     * @param resultado respuesta del selector de credenciales.
                     */
                    @Override
                    public void onResult(
                            GetCredentialResponse resultado
                    ) {
                        procesarCredencial(
                                resultado.getCredential(),
                                callback
                        );
                    }

                    /**
                     * Avisa que no se pudo obtener la credencial de Google.
                     *
                     * @param error error informado por Credential Manager.
                     */
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

    /**
     * Comprueba que la credencial sea de Google y extrae su token para Firebase.
     * Si la credencial no es compatible o no se puede leer, avisa al callback.
     *
     * @param credential credencial devuelta por el selector.
     * @param callback recibe el resultado de la autenticación o un mensaje de error.
     */
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

    /**
     * Inicia sesión en Firebase con el token de Google y comunica el resultado.
     *
     * @param idToken token de identidad recibido de Google.
     * @param callback recibe el usuario de Firebase o un mensaje de error.
     */
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
