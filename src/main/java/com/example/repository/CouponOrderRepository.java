package com.example.repository;

import com.example.entity.CouponOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponOrderRepository extends JpaRepository<CouponOrder,Long> {
    @Query("from CouponOrder where userId=:id")
    CouponOrder findById(@Param("id") String id);
}
