package com.cardsystem.repository;

import com.cardsystem.models.UnallocatedPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnallocatedPaymentRepository extends JpaRepository<UnallocatedPayment, Long> {

    List<UnallocatedPayment> findByStatus(UnallocatedPayment.Status status);

    boolean existsByTransId(String transId);
}
