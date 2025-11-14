package eventplanner.security.auth.controller;

import eventplanner.security.auth.dto.req.ResendEmailVerificationRequest;
import eventplanner.security.auth.service.AccountRecoveryService;
import eventplanner.common.dto.ApiMessageResponse;
import jakarta.validation.Valid;
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
     * @param token The verification token from email link
     * @return HTML response with confirmation message
     */
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        boolean verified = accountRecoveryService.verifyEmailToken(token);
        
        if (verified) {
            String htmlResponse = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Email Verified - SHDE</title>
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            padding: 20px;
                        }
                        .container {
                            text-align: center;
                            padding: 48px 32px;
                            background: white;
                            border-radius: 12px;
                            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                            max-width: 500px;
                            width: 100%;
                        }
                        .icon {
                            width: 80px;
                            height: 80px;
                            margin: 0 auto 24px;
                            background: #10b981;
                            border-radius: 50%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-size: 40px;
                            color: white;
                        }
                        h1 {
                            color: #1f2937;
                            margin-bottom: 16px;
                            font-size: 28px;
                            font-weight: 600;
                        }
                        p {
                            color: #6b7280;
                            font-size: 16px;
                            line-height: 1.6;
                            margin-bottom: 32px;
                        }
                        .button {
                            display: inline-block;
                            padding: 12px 32px;
                            background: #667eea;
                            color: white;
                            text-decoration: none;
                            border-radius: 8px;
                            font-weight: 500;
                            transition: background 0.2s;
                        }
                        .button:hover {
                            background: #5568d3;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon">✓</div>
                        <h1>Email Confirmed</h1>
                        <p>Your email has been successfully verified. You can now log in to your account and complete your profile.</p>
                        <a href="#" class="button" onclick="window.close(); return false;">Close</a>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
        } else {
            String htmlResponse = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Verification Failed - SHDE</title>
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                            padding: 20px;
                        }
                        .container {
                            text-align: center;
                            padding: 48px 32px;
                            background: white;
                            border-radius: 12px;
                            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                            max-width: 500px;
                            width: 100%;
                        }
                        .icon {
                            width: 80px;
                            height: 80px;
                            margin: 0 auto 24px;
                            background: #ef4444;
                            border-radius: 50%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-size: 40px;
                            color: white;
                        }
                        h1 {
                            color: #1f2937;
                            margin-bottom: 16px;
                            font-size: 28px;
                            font-weight: 600;
                        }
                        p {
                            color: #6b7280;
                            font-size: 16px;
                            line-height: 1.6;
                            margin-bottom: 32px;
                        }
                        .button {
                            display: inline-block;
                            padding: 12px 32px;
                            background: #667eea;
                            color: white;
                            text-decoration: none;
                            border-radius: 8px;
                            font-weight: 500;
                            transition: background 0.2s;
                        }
                        .button:hover {
                            background: #5568d3;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon">✗</div>
                        <h1>Verification Failed</h1>
                        <p>Invalid or expired verification token. Please request a new verification email from the login page.</p>
                        <a href="#" class="button" onclick="window.close(); return false;">Close</a>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
        }
    }
}
