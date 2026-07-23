package com.atharva.dealership.purchase;

import com.atharva.dealership.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query("select p from Purchase p where p.user = :user order by p.purchasedAt desc, p.id desc")
    List<Purchase> findByUserOrderByPurchasedAtDesc(@Param("user") User user);
}
