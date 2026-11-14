package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /** Lấy ảnh của nhiều đánh giá một lần (tránh truy vấn lặp từng dòng ở 12.1 / 12.3). */
    List<ReviewImage> findByReview_ReviewIdInOrderByDisplayOrderAsc(Collection<Long> reviewIds);
}
