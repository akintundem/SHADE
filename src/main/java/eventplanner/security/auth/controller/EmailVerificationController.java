package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.ResendEmailVerificationRequest;
import eventplanner.security.auth.service.AccountRecoveryService;
import eventplanner.common.dto.ApiMessageResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class EmailVerificationController {

    private final AccountRecoveryService accountRecoveryService;

    @Value("${app.asset.bucket:user-shade-auth}")
    private String assetBucket;

    @Value("${logo.url:shade_app_icon.png}")
    private String logoKey;

    public EmailVerificationController(AccountRecoveryService accountRecoveryService) {
        this.accountRecoveryService = accountRecoveryService;
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiMessageResponse> resendVerification(
            @Valid @RequestBody ResendEmailVerificationRequest request) {
        String message = accountRecoveryService.resendVerification(request);
        return ResponseEntity.ok(ApiMessageResponse.success(message));
    }

    /**
     * Verifies email address via token from email link.
     * Returns HTML confirmation page for better user experience.
     * Accepts token as query parameter to match email link format.
     * 
     * @param token The verification token from email link (required)
     * @return HTML response with confirmation message
     */
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmail(@RequestParam(required = true) String token) {
        // Validate token is not empty
        if (token == null || token.trim().isEmpty()) {
            return getErrorResponse("Verification token is required.");
        }
        
        boolean verified = accountRecoveryService.verifyEmailToken(token.trim());
        String logoUrl = resolveLogoUrl();
        
        if (verified) {
            String htmlResponse = buildHtmlResponse(
                    "Email confirmed",
                    "✓",
                    "Email confirmed",
                    "Your email is verified. You can now log in and continue.",
                    logoUrl);
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
        } else {
            String htmlResponse = buildHtmlResponse(
                    "Verification failed",
                    "!",
                    "Verification failed",
                    "Invalid or expired verification link. Please request a new verification email from the login page.",
                    logoUrl);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
        }
    }
    
    private ResponseEntity<String> getErrorResponse(String errorMessage) {
        String htmlResponse = buildHtmlResponse(
            "Verification failed",
            "!",
            "Verification failed",
            errorMessage,
            resolveLogoUrl());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.TEXT_HTML)
            .body(htmlResponse);
    }

    private String buildHtmlResponse(String title, String icon, String headingText, String messageText, String logoUrl) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: #ffffff;
                        color: #0c0c0c;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        padding: 32px;
                    }
                    .card {
                        max-width: 520px;
                        width: 100%%;
                        border: 1px solid #e5e5e5;
                        border-radius: 12px;
                        padding: 32px;
                    }
                    .logo {
                        margin-bottom: 16px;
                    }
                    .logo img {
                        display: block;
                        width: 40px;
                        height: 40px;
                    }
                    .icon {
                        width: 56px;
                        height: 56px;
                        border: 1px solid #0c0c0c;
                        border-radius: 50%%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 28px;
                        margin-bottom: 20px;
                    }
                    h1 {
                        font-size: 28px;
                        font-weight: 800;
                        letter-spacing: -0.3px;
                        margin-bottom: 12px;
                    }
                    p {
                        font-size: 15px;
                        line-height: 1.6;
                        color: #222222;
                        margin-bottom: 28px;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 22px;
                        background: #0c0c0c;
                        color: #ffffff;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 700;
                    }
                </style>
            </head>
            <body>
              <div class="card">
                <div class="logo"><img src="%s" alt="Shade"></div>
                <div class="icon">%s</div>
                <h1>%s</h1>
                <p>%s</p>
                <a href="#" class="button" onclick="window.close(); return false;">Close</a>
              </div>
            </body>
            </html>
            """.formatted(title, logoUrl, icon, headingText, messageText);
    }

    private String resolveLogoUrl() {
        if (logoKey == null || logoKey.isBlank()) {
            return "";
        }
        if (logoKey.startsWith("http://") || logoKey.startsWith("https://")) {
            return logoKey;
        }
        if (assetBucket != null && !assetBucket.isBlank()) {
            return "https://" + assetBucket + ".s3.us-east-2.amazonaws.com/" + logoKey;
        }
        return logoKey;
    }
}
