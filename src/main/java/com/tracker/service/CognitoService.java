package com.tracker.service;

import com.tracker.model.AuthRequest;
import com.tracker.model.AuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CognitoService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;
    private final String clientId;

    public CognitoService(
            CognitoIdentityProviderClient cognitoClient,
            @Qualifier("cognitoUserPoolId") String userPoolId,
            @Qualifier("cognitoClientId") String clientId) {
        this.cognitoClient = cognitoClient;
        this.userPoolId = userPoolId;
        this.clientId = clientId;
    }

    public AuthResponse signUp(AuthRequest request) {
        try {
            log.info("Signing up user with phone: {}", request.getPhoneNumber());

            AttributeType phoneAttr = AttributeType.builder()
                    .name("phone_number")
                    .value(request.getPhoneNumber())
                    .build();

            SignUpRequest.Builder signUpRequestBuilder = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(request.getPhoneNumber())
                    .password(request.getPassword())
                    .userAttributes(phoneAttr);

            if (request.getName() != null && !request.getName().isEmpty()) {
                AttributeType nameAttr = AttributeType.builder()
                        .name("name")
                        .value(request.getName())
                        .build();
                signUpRequestBuilder.userAttributes(phoneAttr, nameAttr);
            }

            SignUpResponse signUpResponse = cognitoClient.signUp(signUpRequestBuilder.build());

            log.info("User signed up successfully. UserSub: {}", signUpResponse.userSub());

            // Auto-confirm the user since we don't have SMS verification configured
            if (!signUpResponse.userConfirmed()) {
                try {
                    AdminConfirmSignUpRequest confirmRequest = AdminConfirmSignUpRequest.builder()
                            .userPoolId(userPoolId)
                            .username(request.getPhoneNumber())
                            .build();
                    cognitoClient.adminConfirmSignUp(confirmRequest);
                    log.info("User auto-confirmed: {}", request.getPhoneNumber());
                } catch (Exception e) {
                    log.warn("Failed to auto-confirm user: {}", e.getMessage());
                }
            }

            return AuthResponse.builder()
                    .success(true)
                    .message("Account created successfully")
                    .cognitoUserId(signUpResponse.userSub())
                    .requiresVerification(false)
                    .build();

        } catch (UsernameExistsException e) {
            log.warn("User already exists: {}", request.getPhoneNumber());
            return AuthResponse.builder()
                    .success(false)
                    .message("An account with this phone number already exists")
                    .build();
        } catch (InvalidPasswordException e) {
            log.warn("Invalid password: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("Password must be at least 8 characters")
                    .build();
        } catch (Exception e) {
            log.error("Sign up failed: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("Sign up failed: " + e.getMessage())
                    .build();
        }
    }

    public AuthResponse confirmSignUp(AuthRequest request) {
        try {
            log.info("Confirming sign up for user: {}", request.getPhoneNumber());

            ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
                    .clientId(clientId)
                    .username(request.getPhoneNumber())
                    .confirmationCode(request.getVerificationCode())
                    .build();

            cognitoClient.confirmSignUp(confirmRequest);

            log.info("User confirmed successfully: {}", request.getPhoneNumber());

            return AuthResponse.builder()
                    .success(true)
                    .message("Phone number verified successfully")
                    .build();

        } catch (CodeMismatchException e) {
            log.warn("Invalid verification code for user: {}", request.getPhoneNumber());
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid verification code")
                    .build();
        } catch (ExpiredCodeException e) {
            log.warn("Verification code expired for user: {}", request.getPhoneNumber());
            return AuthResponse.builder()
                    .success(false)
                    .message("Verification code has expired. Please request a new one.")
                    .build();
        } catch (Exception e) {
            log.error("Confirm sign up failed: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("Verification failed: " + e.getMessage())
                    .build();
        }
    }

    public AuthResponse signIn(AuthRequest request) {
        try {
            log.info("Signing in user: {}", request.getPhoneNumber());

            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", request.getPhoneNumber());
            authParams.put("PASSWORD", request.getPassword());

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientId)
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            if (authResponse.challengeName() != null) {
                log.info("Auth challenge required: {}", authResponse.challengeName());
                return AuthResponse.builder()
                        .success(false)
                        .message("Additional verification required")
                        .requiresVerification(true)
                        .build();
            }

            AuthenticationResultType result = authResponse.authenticationResult();

            log.info("User signed in successfully: {}", request.getPhoneNumber());

            return AuthResponse.builder()
                    .success(true)
                    .message("Sign in successful")
                    .accessToken(result.accessToken())
                    .refreshToken(result.refreshToken())
                    .build();

        } catch (NotAuthorizedException e) {
            log.warn("Invalid credentials for user: {}", request.getPhoneNumber());
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid phone number or password")
                    .build();
        } catch (UserNotConfirmedException e) {
            log.warn("User not confirmed: {}, attempting auto-confirm", request.getPhoneNumber());
            try {
                AdminConfirmSignUpRequest confirmRequest = AdminConfirmSignUpRequest.builder()
                        .userPoolId(userPoolId)
                        .username(request.getPhoneNumber())
                        .build();
                cognitoClient.adminConfirmSignUp(confirmRequest);
                log.info("User auto-confirmed during sign-in: {}", request.getPhoneNumber());

                // Retry sign-in after confirmation
                return signIn(request);
            } catch (Exception confirmError) {
                log.error("Failed to auto-confirm user during sign-in: {}", confirmError.getMessage());
                return AuthResponse.builder()
                        .success(false)
                        .message("Account needs verification. Please contact support.")
                        .requiresVerification(true)
                        .build();
            }
        } catch (UserNotFoundException e) {
            log.warn("User not found: {}", request.getPhoneNumber());
            return AuthResponse.builder()
                    .success(false)
                    .message("No account found with this phone number")
                    .build();
        } catch (Exception e) {
            log.error("Sign in failed: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("Sign in failed: " + e.getMessage())
                    .build();
        }
    }

    public AuthResponse resendConfirmationCode(String phoneNumber) {
        try {
            log.info("Resending confirmation code to: {}", phoneNumber);

            ResendConfirmationCodeRequest request = ResendConfirmationCodeRequest.builder()
                    .clientId(clientId)
                    .username(phoneNumber)
                    .build();

            cognitoClient.resendConfirmationCode(request);

            return AuthResponse.builder()
                    .success(true)
                    .message("Verification code resent to your phone")
                    .build();

        } catch (Exception e) {
            log.error("Failed to resend confirmation code: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("Failed to resend code: " + e.getMessage())
                    .build();
        }
    }

    public String getUserIdFromToken(String accessToken) {
        try {
            GetUserRequest request = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();

            GetUserResponse response = cognitoClient.getUser(request);
            return response.username();
        } catch (Exception e) {
            log.error("Failed to get user from token: {}", e.getMessage());
            return null;
        }
    }
}
