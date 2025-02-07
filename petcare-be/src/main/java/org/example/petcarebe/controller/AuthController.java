package org.example.petcarebe.controller;

import com.google.api.client.http.javanet.NetHttpTransport;
import org.example.petcarebe.dto.request.AuthRequest;
import org.example.petcarebe.dto.request.RegisterRequest;
import org.example.petcarebe.dto.request.OtpRequest;
import org.example.petcarebe.dto.response.FacebookResponse;
import org.example.petcarebe.dto.response.JwtResponse;
import org.example.petcarebe.model.User;
import org.example.petcarebe.service.EmailService;
import org.example.petcarebe.service.JwtService;
import org.example.petcarebe.service.OtpService;
import org.example.petcarebe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.exception.FacebookOAuthException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String GOOGLE_CLIENT_ID = "854614351620-s8cmgi8ticqj4p2jlqedf4drbis3s7oj.apps.googleusercontent.com";

    // Register
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Kiểm tra email đã tồn tại
            if (userService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: Email đã được sử dụng.");
            }

            // Lưu thông tin đăng ký tạm thời
            otpService.saveRegistrationRequest(registerRequest);

            // Tạo và gửi OTP
            otpService.generateOtp(registerRequest.getEmail());

            return ResponseEntity.ok("OTP has been sent to " + registerRequest.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody OtpRequest otpRequest) {
        String email = otpRequest.getEmail();
        String otp = otpRequest.getOtp();

        if (otpService.validateOtp(email, otp)) {
            // Xác thực OTP thành công, kiểm tra đăng ký tạm thời
            RegisterRequest registrationRequest = otpService.getRegistrationRequest(email);
            if (registrationRequest == null) {
                return ResponseEntity.badRequest().body("No registration request found.");
            }

            // Tạo tài khoản người dùng
            User newUser = new User();
            newUser.setEmail(registrationRequest.getEmail());
            newUser.setPassword(registrationRequest.getPassword()); // Hash password trước khi lưu
            newUser.setFullName(registrationRequest.getFullName());

            userService.saveUser(newUser);

            // Xóa OTP và thông tin đăng ký tạm thời
            otpService.clearOtp(email);
            otpService.removeRegistrationRequest(email);

            return ResponseEntity.ok("Account created successfully!");
        } else {
            return ResponseEntity.badRequest().body("Invalid OTP");
        }
    }

    // Resend OTP
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email không được để trống.");
        }

        // Kiểm tra xem email có trong danh sách yêu cầu đăng ký hay không
        RegisterRequest registrationRequest = otpService.getRegistrationRequest(email);
        if (registrationRequest == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy yêu cầu đăng ký cho email này.");
        }

        try {
            // Tạo OTP mới
            otpService.generateOtp(email);

            return ResponseEntity.ok("Mã OTP đã được gửi lại thành công.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể gửi lại mã OTP. Vui lòng thử lại sau.");
        }
    }

    // Login
    @PostMapping("/login")
    public JwtResponse login(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userService.findByEmail(authRequest.getEmail());
        String jwt = jwtService.generateToken(userService.loadUserByUsername(authRequest.getEmail()),user);

        return JwtResponse.builder()
                .accessToken(jwt)
                .userId(user.getUserId()) // Chuyển đổi userId từ long sang String
                .fullName(String.valueOf(user.getFullName())) // Ép kiểu fullName về String
                .roleName(userService.getUserRole(user))
                .phone(user.getPhone())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .isStatus(user.isStatus())
                .registration_date(user.getRegistration_date())
                .totalSpent(user.getTotalSpent())
                .build();

    }

    // Google login
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> requestBody) {
        try {
            // Lấy token từ JSON body
            String token = requestBody.get("token");
            System.out.println("Received token: " + token); // Ghi log token nhận được

            // Khởi tạo GoogleIdTokenVerifier
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JSON_FACTORY)
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID)) // Thay bằng client ID của bạn
                    .build();

            // Xác thực và parse token
            GoogleIdToken googleIdToken = verifier.verify(token);
            if (googleIdToken != null) {
                GoogleIdToken.Payload payload = googleIdToken.getPayload();

                String email = payload.getEmail();
                String fullName = (String) payload.get("name"); // Lấy fullName từ payload
                String picture = (String) payload.get("picture"); // Lấy avatar từ Google, nếu cần

                // Kiểm tra xem người dùng đã tồn tại trong cơ sở dữ liệu chưa
                User user = userService.findByEmail(email);

                if (user == null) {
                    // Nếu người dùng chưa tồn tại, tạo một tài khoản mới
                    user = new User();
                    user.setEmail(email);
                    user.setFullName(fullName); // Cập nhật với thông tin từ Google
//                    user.setAvatarUrl(picture); // Nếu bạn muốn lưu ảnh đại diện từ Google
                    user.setPassword("");
                    user.setStatus(true); // Mặc định tài khoản đã được kích hoạt
                    user.setImageUrl(picture); // Lưu ảnh đại diện từ Google
                    userService.saveUser(user); // Lưu thông tin người dùng mới vào database
                }


                // Tạo đối tượng Authentication mà không cần password
                Authentication authentication = new PreAuthenticatedAuthenticationToken(
                        userService.loadUserByUsername(email), // principal
                        null // không cần mật khẩu
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Tạo JWT token
                String jwt = jwtService.generateToken(userService.loadUserByUsername(email), user);

                JwtResponse response = JwtResponse.builder()
                        .accessToken(jwt)
                        .userId(user.getUserId()) // Chuyển đổi userId từ long sang String
                        .fullName(user.getFullName())
                        .roleName(userService.getUserRole(user)) // Lấy vai trò của user, nếu có
                        .email(user.getEmail())
                        .imageUrl(user.getImageUrl())
                        .isStatus(user.isStatus())
                        .totalSpent(user.getTotalSpent())
                        .build();

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid ID token. Token could not be verified.");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Ghi log lỗi
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error: " + e.getMessage());
        }
    }

    // Facebook login
    @PostMapping("/facebook-login")
    public ResponseEntity<?> facebookLogin(@RequestBody FacebookResponse facebookUserDTO) {
        // Lấy accessToken từ yêu cầu
        String accessToken = facebookUserDTO.getAccessToken();

        // Log thông tin người dùng để kiểm tra
        System.out.println("Facebook User DTO: " + facebookUserDTO);

        try {
            // Kiểm tra nếu accessToken là null hoặc rỗng
            if (accessToken == null || accessToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Access token cannot be null or empty."));
            }

            // Khởi tạo FacebookClient để xác thực token
            FacebookClient facebookClient = new DefaultFacebookClient(accessToken, Version.LATEST);


            // Không cần gọi lại, đã có thông tin trong facebookUserDTO
            String email = facebookUserDTO.getEmail();
            String fullName = facebookUserDTO.getName();
            // Kiểm tra xem người dùng đã tồn tại trong cơ sở dữ liệu chưa
            User user = userService.findByEmail(email);

            if (user == null) {
                // Nếu người dùng chưa tồn tại, tạo một tài khoản mới
                user = new User();
                user.setEmail(email); // Có thể để trống nếu không có email
                user.setFullName(fullName);
                user.setPassword(""); // Không cần mật khẩu cho Facebook
                user.setStatus(true); // Mặc định tài khoản đã được kích hoạt
                userService.saveUser(user); // Lưu thông tin người dùng mới vào database

            }

            // Tạo JWT token
            String jwt = jwtService.generateToken(userService.loadUserByUsername(user.getEmail()), user);

            JwtResponse response = JwtResponse.builder()
                    .accessToken(jwt)
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .roleName(userService.getUserRole(user))
                    .email(user.getEmail())
                    .imageUrl(user.getImageUrl())
                    .isStatus(user.isStatus())
                    .totalSpent(user.getTotalSpent())
                    .build();

            return ResponseEntity.ok(response);

        } catch (FacebookOAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid access token."));
        } catch (Exception e) {
            e.printStackTrace(); // In chi tiết lỗi ra console
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "details", e.getMessage()));
        }
    }
}
