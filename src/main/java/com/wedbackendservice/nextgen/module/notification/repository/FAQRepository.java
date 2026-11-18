package com.wedbackendservice.nextgen.module.notification.repository;

import com.wedbackendservice.nextgen.module.notification.entity.FAQ;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FAQRepository extends JpaRepository<FAQ, Long>, JpaSpecificationExecutor<FAQ> {

  List<FAQ> findByIsActiveTrueOrderByDisplayOrderAsc();

  List<FAQ> findByCategoryIgnoreCaseAndIsActiveTrueOrderByDisplayOrderAsc(String category);

  boolean existsByQuestionIgnoreCase(String question);
}
