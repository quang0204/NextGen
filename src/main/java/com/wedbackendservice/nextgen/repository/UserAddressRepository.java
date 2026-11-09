package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    @Query("SELECT a FROM UserAddress a WHERE a.user.userId = :userId AND a.isDefault = true")
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(@Param("userId") Long userId);

    @Query("SELECT a FROM UserAddress a WHERE a.user.userId = :userId ORDER BY a.isDefault DESC, a.addressId DESC")
    List<UserAddress> findByUserIdOrderByIsDefaultDescAddressIdDesc(@Param("userId") Long userId);

    @Query("SELECT count(a) FROM UserAddress a WHERE a.user.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM UserAddress a WHERE a.addressId = :addressId AND a.user.userId = :userId")
    Optional<UserAddress> findByAddressIdAndUserId(@Param("addressId") Long addressId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.userId = :userId")
    void resetDefaultAddressByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserAddress a SET a.isDefault = CASE WHEN a.addressId = :targetAddressId THEN true ELSE false END WHERE a.user.userId = :userId")
    int setDefaultAddressAtomic(@Param("userId") Long userId, @Param("targetAddressId") Long targetAddressId);

    @Query("SELECT a FROM UserAddress a WHERE a.user.userId = :userId ORDER BY a.addressId DESC")
    List<UserAddress> findLatestAddressByUserId(@Param("userId") Long userId);
}
