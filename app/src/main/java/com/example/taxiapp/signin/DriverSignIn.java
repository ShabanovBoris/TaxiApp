package com.example.taxiapp.signin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.taxiapp.DriverMapsActivity;
import com.example.taxiapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DriverSignIn extends AppCompatActivity {

    public static final String TAG = "DriverSignIn";

    private TextInputLayout textInputEmail,
            textInputName,
            textInputPassword,
            textInputConfirmPassword;
    private Button logInSignUpButton;
    private TextView toggleLogonSignUpTextView;

    private boolean isLoginModeActive;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_sign_in);


        textInputEmail = findViewById(R.id.textInputEmail);
        textInputName = findViewById(R.id.textInputName);
        textInputPassword = findViewById(R.id.textInputPassword);
        textInputConfirmPassword = findViewById(R.id.textInputPasswordCofirm);

        logInSignUpButton = findViewById(R.id.logInSignUpButton);
        toggleLogonSignUpTextView = findViewById(R.id.toggleLogInSignUpButton);

        auth = FirebaseAuth.getInstance();



        if (auth.getCurrentUser() != null){
            startActivity(new Intent(this, DriverMapsActivity.class));
        }



    }

    private  boolean validatePassword() {

        if (isLoginModeActive) {
            String passwordInput = textInputPassword.getEditText().getText().toString()
                    .trim();

            if (passwordInput.isEmpty()) {
                textInputPassword.setError("Please input your Password");
                return false;
            } else if (passwordInput.length() < 7) {
                textInputPassword.setError("Password length have to be more than 6 chars");
                return false;
            } else {
                textInputPassword.setError("");
                return true;
            }
        } else {
            String passwordInput = textInputPassword.getEditText().getText().toString()
                    .trim();
            String confirmPasswordInput = textInputConfirmPassword.getEditText().getText().toString()
                    .trim();

            if (passwordInput.isEmpty()) {
                textInputPassword.setError("Please input your Password");
                return false;
            } else if (passwordInput.length() < 7) {
                textInputPassword.setError("Password length have to be more than 6 chars");
                return false;
            } else if (!passwordInput.equals(confirmPasswordInput)) {
                textInputPassword.setError("Passwords have to match");
                return false;
            } else {
                textInputPassword.setError("");
                return true;
            }

        }
    }


    private  boolean validateName(){

            String nameInput = textInputName.getEditText().getText().toString()
                    .trim();

            if (nameInput.isEmpty()) {
                textInputEmail.setError("Please input your name");
                return false;
            } else if (nameInput.length() > 15) {
                textInputEmail.setError("Name length have to be less 15 chars");
                return false;
            } else {
                textInputEmail.setError("");
                return true;
            }

    }

    private  boolean validateEmail(){
        String emailInput = textInputEmail.getEditText().getText().toString()
                .trim();

        if(emailInput.isEmpty()){
            textInputEmail.setError("Please input your email");
            return false;
        }else {
            textInputEmail.setError("");
            return  true;
        }

    }

    public void logInSignUpUser(View view) {



        if(isLoginModeActive) {
            if (!validateEmail() || !validatePassword())
            {
                return;
            }

            auth.signInWithEmailAndPassword(
                    textInputEmail.getEditText().getText().toString().trim(),
                    textInputPassword.getEditText().getText().toString().trim())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = auth.getCurrentUser();
                                startActivity(new Intent(DriverSignIn.this, DriverMapsActivity.class));
                                //updateUI(user);
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(DriverSignIn.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                //updateUI(null);
                                // ...
                            }

                            // ...
                        }
                    });

        } else {
            if (!validateEmail() || !validateName() || !validatePassword())
            {
                return;
            }
            auth.createUserWithEmailAndPassword(
                    textInputEmail.getEditText().getText().toString().trim(),
                    textInputPassword.getEditText().getText().toString().trim())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser user = auth.getCurrentUser();
                                startActivity(new Intent(DriverSignIn.this, DriverMapsActivity.class));
                                //updateUI(user);
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(DriverSignIn.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                // updateUI(null);
                            }

                            // ...
                        }
                    });

        }




/***********************************************************************************************************/
        String userInput = new StringBuilder()
                .append("Email: ").append(textInputEmail.getEditText().getText().toString().trim())
                .append("\nName: ").append(textInputName.getEditText().getText().toString().trim())
                .append("\nPassword: ").append(textInputPassword.getEditText().getText().toString().trim())
                .toString();


        Toast.makeText(this,userInput,Toast.LENGTH_LONG).show();
    }

    public void toggleLogInSignUpUser(View view) {

        if(isLoginModeActive){
            isLoginModeActive = false;
            logInSignUpButton.setText("Sign up");
            toggleLogonSignUpTextView.setText("or Log In");

            textInputConfirmPassword.setVisibility(View.VISIBLE);
            textInputName.setVisibility(View.VISIBLE);

        }else {
            isLoginModeActive = true;
            logInSignUpButton.setText("Log In");
            toggleLogonSignUpTextView.setText("or Sign up");

            textInputConfirmPassword.setVisibility(View.GONE);
            textInputName.setVisibility(View.GONE);

        }
    }
}