package org.example.petcarebe.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import org.example.petcarebe.dto.UpdateUserDTO;
import org.example.petcarebe.dto.request.ChangePasswordRequest;
import org.example.petcarebe.dto.request.UserUpdateRequest;
import org.example.petcarebe.model.User;
import org.example.petcarebe.model.UserRole;
import org.example.petcarebe.repository.RoleRepository;
import org.example.petcarebe.repository.UserRepository;
import org.example.petcarebe.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.example.petcarebe.model.Role;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    @Lazy
    private EmailService emailService;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        // ✅ Trả về UserDetails mà không cần quyền
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isStatus(), // Kiểm tra trạng thái tài khoản
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                new ArrayList<>() // Không có quyền nhưng vẫn cho phép đăng nhập
        );
    }

    public void saveUser(User user) {
        boolean isNewUser = (user.getUserId() == null || !userRepository.existsById(user.getUserId()));

//        // Mã hóa mật khẩu trước khi lưu (chỉ mã hóa nếu là mật khẩu mới)
//        if (isNewUser || user.getPassword() != null) {
//            user.setPassword(passwordEncoder.encode(user.getPassword()));
//        }

        user.setStatus(true); // Đặt trạng thái mặc định là true khi tạo
        user.setRegistration_date(LocalDate.now());
        user.setFullName(user.getFullName());

        // Lưu user vào database
        userRepository.save(user);

        // Nếu là tài khoản mới, gán quyền mặc định "USER"
        if (isNewUser) {
            Role defaultRole = roleRepository.findByRoleName("USER");
            if (defaultRole != null) {
                assignRoleToUser(user, defaultRole);
            }
        }
    }


    public void save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Mã hóa mật khẩu trước khi lưu
        userRepository.save(user);
    }

    public User updateUser(Long userId, UpdateUserDTO updateUserRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        try {
            // Update user data
            user.setFullName(updateUserRequest.getFullName());
            user.setPhone(updateUserRequest.getPhone());
            user.setEmail(updateUserRequest.getEmail());
            user.setStatus(updateUserRequest.isStatus());
            user.setImageUrl(updateUserRequest.getImageUrl());


            // Save the updated user to the database
            return userRepository.save(user);
        } catch (Exception ex) {
            throw new RuntimeException("Error updating user with ID: " + userId, ex);
        }
    }

    public User updateUserAccount(Long userId, UserUpdateRequest updateRequest, String loggedInUserId) {
        // Kiểm tra xem userId trong URL có khớp với userId của người dùng đã đăng nhập không
        if (!userId.toString().equals(loggedInUserId)) {
            throw new AccessDeniedException("You are not authorized to update this user's information");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        try {
            // Chỉ cập nhật fullname và phone nếu có giá trị
            if (updateRequest.getFullName() != null) {
                user.setFullName(updateRequest.getFullName());
            }
            if (updateRequest.getPhone() != null) {
                user.setPhone(updateRequest.getPhone());
            }

            return userRepository.save(user);
        } catch (Exception ex) {
            throw new RuntimeException("Error updating user with ID: " + userId, ex);
        }
    }


    //     Phương thức kiểm tra email tồn tại trong hệ thống
    public boolean checkIfEmailExists(String email) {
        User user = userRepository.findByEmail(email);
        return user != null;  // Kiểm tra nếu user khác null, có nghĩa là email tồn tại
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> System.out.println("User: " + user.getFullName() + ", Status: " + user.isStatus()));
        return users;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public void delete(User user) {
        userRepository.delete(user); // Xóa người dùng
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void assignRoleToUser(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }

    public String getUserRole(User user) {
        UserRole userRole = userRoleRepository.findByUser(user);
        if (userRole != null) {
            return userRole.getRole().getRoleName();
        }
        return null;
    }

    public User findByUserUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null); // Tìm người dùng theo ID
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = userRepository.findById(Long.parseLong(request.getUserId()))
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được giống với mật khẩu hiện tại");
        }

        String encryptedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encryptedPassword);
        userRepository.save(user);

        // Gửi email trong background (không cần chờ)
        CompletableFuture.runAsync(() -> sendPasswordChangeNotification(user.getEmail(), user.getFullName()));

        // Trả về ngay sau khi lưu
    }

    @Async
    public void sendPasswordChangeNotification(String userEmail, String userName) {
        MimeMessage message = emailService.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(userEmail);
            helper.setSubject("Thông Báo Thay Đổi Mật Khẩu từ PetCare");

            // Sử dụng template email
            String emailContent = generatePasswordChangeEmailContent(userName);
            helper.setText(emailContent, true); // true để nội dung HTML được render
            emailService.send(message);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    private String generatePasswordChangeEmailContent(String userName) {
        // Dùng StringBuilder hoặc một công cụ template để xây dựng nội dung
        return new StringBuilder()
                .append("<div style='background-color: #f4f4f4; padding: 20px;'>")
                .append("<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);'>")
                .append("<div style='background-color: #00b7c0; padding: 15px; text-align: center;'>")
                .append("<h1 style='color: #ffffff; font-family: Arial, sans-serif;'>PetCare</h1>")
                .append("</div>")
                .append("<div style='padding: 20px; font-family: Arial, sans-serif;'>")
                .append("<h2>Kính gửi Quý khách ").append(userName).append(",</h2>")
                .append("<p>Chúng tôi xin thông báo rằng mật khẩu của tài khoản của Quý khách đã được thay đổi thành công.</p>")
                .append("<p>Nếu Quý khách không thực hiện thay đổi này, vui lòng nhấp vào liên kết dưới đây để đặt lại mật khẩu:</p>")
                .append("<a href='https://petcare.com/reset-password' style='display: inline-block; background-color: #00b7c0; color: #ffffff; padding: 10px 20px; border-radius: 5px; text-decoration: none;'>Đặt lại mật khẩu</a>")
                .append("<p>Nếu liên kết trên không hoạt động, vui lòng sao chép và dán URL sau vào trình duyệt:</p>")
                .append("<p>https://petcare.com/reset-password</p>")
                .append("<p>Trân trọng,<br>PetCare<br>Thành phố Cần Thơ<br>Hotline: 0987654321</p>")
                .append("</div>")
                .append("</div>")
                .append("</div>")
                .toString();
    }

    // Trong UserService
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User updateAvatar(Long userId, UpdateUserDTO imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        user.setImageUrl(imageUrl.getImageUrl());
        userRepository.save(user);
        return user;
    }

}
