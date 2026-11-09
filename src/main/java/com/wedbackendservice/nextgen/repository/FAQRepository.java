package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FAQRepository extends JpaRepository<FAQ, Long>, JpaSpecificationExecutor<FAQ> {

    List<FAQ> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<FAQ> findByCategoryIgnoreCaseAndIsActiveTrueOrderByDisplayOrderAsc(String category);

    boolean existsByQuestionIgnoreCase(String question);
}
