package org.example.petcarebe.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import org.example.petcarebe.dto.UpdateUserDTO;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // 1. UserDetailsService method
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isStatus(),
                true,
                true,
                true,
                new ArrayList<>()
        );
    }

    // 2. CRUD methods
    public void saveUser(User user) {
        boolean isNewUser = (user.getUserId() == null || !userRepository.existsById(user.getUserId()));
        user.setStatus(true);
        user.setRegistration_date(LocalDate.now());
        user.setFullName(user.getFullName());
        userRepository.save(user);
        if (isNewUser) {
            Role defaultRole = roleRepository.findByRoleName("USER");
            if (defaultRole != null) {
                assignRoleToUser(user, defaultRole);
            }
        }
    }

    public User saveStaff(User staff) {
        User existingUser = userRepository.findByEmail(staff.getEmail().toLowerCase());
        if (existingUser != null) {
            throw new RuntimeException("Email đã tồn tại. Vui lòng chọn email khác.");
        }
        boolean isNewUser = (staff.getUserId() == null);
        if (staff.getPassword() != null && !staff.getPassword().isEmpty()) {
            staff.setPassword(passwordEncoder.encode(staff.getPassword()));
        } else {
            throw new RuntimeException("Mật khẩu không được để trống.");
        }
        staff.setStatus(true);
        staff.setRegistration_date(LocalDate.now());

        // Xử lý userRoles từ request (nếu có)
        if (staff.getUserRoles() != null && !staff.getUserRoles().isEmpty()) {
            Set<Role> persistedRoles = new HashSet<>();
            for (Role role : staff.getUserRoles()) {
                Role existingRole = roleRepository.findByRoleName(role.getRoleName());
                if (existingRole == null) {
                    throw new RuntimeException("Vai trò " + role.getRoleName() + " không tồn tại.");
                }
                persistedRoles.add(existingRole);
            }
            staff.setUserRoles(persistedRoles);
        }

        User savedStaff = userRepository.save(staff);

        // Gán role "STAFF" nếu là user mới và chưa có userRoles
        if (isNewUser && (staff.getUserRoles() == null || staff.getUserRoles().isEmpty())) {
            Role staffRole = roleRepository.findByRoleName("STAFF");
            if (staffRole == null) {
                throw new RuntimeException("Role STAFF không tồn tại, vui lòng kiểm tra lại dữ liệu.");
            }
            assignRoleToUser(savedStaff, staffRole);
        }

        return savedStaff;
    }

    @Transactional
    public User updateStaff(Long userId, User staff) {
        User existingStaff = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + userId));

        System.out.println("Received staff: " + staff);
        System.out.println("Received isStatus: " + staff.isStatus());

        User existingUserWithEmail = userRepository.findByEmail(staff.getEmail().toLowerCase());
        if (existingUserWithEmail != null && !existingUserWithEmail.getUserId().equals(userId)) {
            throw new RuntimeException("Email đã được sử dụng bởi người dùng khác.");
        }

        existingStaff.setEmail(staff.getEmail().toLowerCase());
        existingStaff.setFullName(staff.getFullName());
        existingStaff.setPhone(staff.getPhone());
        existingStaff.setStatus(staff.isStatus());
        existingStaff.setImageUrl(staff.getImageUrl());

        if (staff.getPassword() != null && !staff.getPassword().trim().isEmpty()) {
            existingStaff.setPassword(passwordEncoder.encode(staff.getPassword()));
        }

        // Chỉ cập nhật userRoles nếu danh sách mới khác với danh sách hiện tại
        if (staff.getUserRoles() != null && !staff.getUserRoles().isEmpty()) {
            Set<Role> currentRoles = existingStaff.getUserRoles();
            Set<Role> newRoles = new HashSet<>();
            for (Role role : staff.getUserRoles()) {
                Role existingRole = roleRepository.findByRoleName(role.getRoleName());
                if (existingRole == null) {
                    throw new RuntimeException("Vai trò " + role.getRoleName() + " không tồn tại.");
                }
                newRoles.add(existingRole);
            }
            // So sánh danh sách vai trò hiện tại và mới
            if (!currentRoles.equals(newRoles)) {
                existingStaff.getUserRoles().clear();
                for (Role role : newRoles) {
                    assignRoleToUser(existingStaff, role);
                }
            }
        }

        User updatedStaff = userRepository.save(existingStaff);
        System.out.println("After save - Updated isStatus: " + updatedStaff.isStatus());
        return updatedStaff;
    }

    public void save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public User updateUser(Long userId, UpdateUserDTO updateUserRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        try {
            user.setFullName(updateUserRequest.getFullName());
            user.setPhone(updateUserRequest.getPhone());
            user.setEmail(updateUserRequest.getEmail());
            user.setStatus(updateUserRequest.isStatus());
            user.setImageUrl(updateUserRequest.getImageUrl());
            return userRepository.save(user);
        } catch (Exception ex) {
            throw new RuntimeException("Error updating user with ID: " + userId, ex);
        }
    }

    public User updateUserAccount(Long userId, UserUpdateRequest updateRequest, String loggedInUserId) {
        if (!userId.toString().equals(loggedInUserId)) {
            throw new AccessDeniedException("You are not authorized to update this user's information");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        try {
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

    public void delete(User user) {
        userRepository.delete(user);
    }

    // 3. Query methods
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> System.out.println("User: " + user.getFullName() + ", Status: " + user.isStatus()));
        return users;
    }

    public List<User> getAllStaff() {
        List<User> staff = userRepository.findByRole("STAFF");
        if (staff.isEmpty()) {
            System.out.println("Không tìm thấy user có role STAFF");
        } else {
            staff.forEach(user -> System.out.println("User: " + user.getFullName() + ", Role: " + user.getUserRoles()));
        }
        return staff;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findByUserUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public boolean checkIfEmailExists(String email) {
        User user = userRepository.findByEmail(email);
        return user != null;
    }

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

    // 4. Role management
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

    // 5. Email notification
    @Async
    public void sendPasswordChangeNotification(String userEmail, String userName) {
        MimeMessage message = emailService.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(userEmail);
            helper.setSubject("Thông Báo Thay Đổi Mật Khẩu từ PetCare");
            String emailContent = generatePasswordChangeEmailContent(userName);
            helper.setText(emailContent, true);
            emailService.send(message);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    private String generatePasswordChangeEmailContent(String userName) {
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
}