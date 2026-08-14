package com.ecom.user.service;

import com.ecom.common.dto.PageResponse;
import com.ecom.common.enums.RoleType;
import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.user.dto.ChangePasswordRequest;
import com.ecom.user.dto.UpdateProfileRequest;
import com.ecom.user.dto.UserResponse;
import com.ecom.user.entity.User;
import com.ecom.user.mapper.UserMapper;
import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;



    // CUSTOMER SELF-SERVICE


    // getOwnProfile()

    public UserResponse getOwnProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }


    // updateOwnProfile()

    public UserResponse updateOwnProfile(String userId, UpdateProfileRequest request){

        User user = userRepository.findById(userId)
                .orElseThrow( () -> new ResourceNotFoundException("User not found"));


        if (request.getFirstName() != null){
            user.setFirstName(request.getFirstName().trim());
        }
        if(request.getLastName() != null){
            user.setLastName(request.getLastName().trim());
        }
        if(request.getPhoneNumber() != null){
            user.setPhoneNumber(request.getPhoneNumber());
        }


        User updated = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        return userMapper.toResponse(updated);

    }


    // changePassword()

    public void changePassword(String userId, ChangePasswordRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow( () -> new ResourceNotFoundException("User not found"));


        if(!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())){
            throw new BusinessException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", userId);


        /*
         SECURITY NOTE: In a more complete implementation, we would also, call refreshTokenRepository.deleteAllByUserId(userId) here to force re-login on all other devices after a password change. Left as an extension point — wire it in when integrating modules.
         */

    }


//    updateProfilePicture()

    public UserResponse updateProfilePicture(String userId, String imageUrl) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setProfilePictureUrl(imageUrl);
        User updated = userRepository.save(user);
        log.info("Profile picture updated for user: {}", userId);
        return userMapper.toResponse(updated);
    }



    // ADMIN MANAGEMENT

    // getAllUser()

    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage = userRepository.findAll(pageable);


        return PageResponse.of(
                userPage.getContent().stream().map(userMapper::toResponse).toList(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }


    // searchUsers()

    public PageResponse<UserResponse> searchUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Criteria criteria = new Criteria().orOperator(
                Criteria.where("first_name").regex(keyword, "i"),
                Criteria.where("last_name").regex(keyword, "i"),
                Criteria.where("email").regex(keyword, "i")
        );

        Query query = new Query(criteria).with(pageable);

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), User.class);

        var users = mongoTemplate.find(query, User.class);

        return PageResponse.of(
                users.stream().map(userMapper::toResponse).toList(),
                page,
                size,
                total,
                (int) Math.ceil((double) total / size)
        );
    }


    // toggleUserActive()

    public UserResponse toggleUserActive(String userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(active);
        User updated = userRepository.save(user);
        log.warn("User {} {} by admin", userId, active ? "activated" : "deactivated");
        return userMapper.toResponse(updated);


        /*
         * IMPORTANT: This does NOT invalidate their CURRENT active session/token.
         * Their existing access token remains valid until it naturally expires  (max 15 min).
         * For instant lockout, you'd also blacklist their tokens
         * and delete their refresh tokens here — left as an extension point
         * once the redis/auth modules are fully wired together.
         */

    }


    // updateUserRole()

    public UserResponse updateUserRole(String userId, RoleType newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RoleType oldRole = user.getRole();
        user.setRole(newRole);
        User updated = userRepository.save(user);

        log.warn("User {} role changed from {} to {} by admin", userId, oldRole, newRole);
        return userMapper.toResponse(updated);
    }


    // getUserById()

    public UserResponse getUserById(String userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

}
