package com.example.repository;

import com.example.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon,Long> {
    // 使用乐观锁解决超卖问题
    @Modifying
    @Query(value = "update Coupon set leftNumber=leftNumber-1 where id=:id and leftNumber>0")
    int updateCoupon(@Param("id") long id);
}
