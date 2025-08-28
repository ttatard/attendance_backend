package com.example.attendance.repository;

import com.example.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    List<User> findAllByIsDeletedFalse();
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.id = :userId")
    void softDelete(@Param("userId") Long userId);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findByActiveEmail(@Param("email") String email);
    
    @Modifying
    @Query("UPDATE User u SET u.isDeactivated = :status WHERE u.email = :email")
    void updateDeactivationStatus(@Param("email") String email, @Param("status") boolean status);

    @Query("SELECT u.isDeactivated FROM User u WHERE u.email = :email")
    boolean isDeactivated(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.accountType = 'USER'")
    List<User> findAllNonAdminUsers();

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.accountType != 'SYSTEM_OWNER'")
    List<User> findAllNonSystemOwnerUsers();

    // Add these new methods to fix the compilation errors
    List<User> findByAccountType(User.AccountType accountType);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.accountType = :accountType")
    List<User> findByAccountTypeAndNotDeleted(@Param("accountType") User.AccountType accountType);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.accountType = 'SYSTEM_OWNER'")
    List<User> findAllSystemOwners();
}