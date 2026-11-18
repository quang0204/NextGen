package com.wedbackendservice.nextgen.module.product.repository;

import com.wedbackendservice.nextgen.module.product.entity.ReviewImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

  /** Lấy ảnh của nhiều đánh giá một lần (tránh truy vấn lặp từng dòng ở 12.1 / 12.3). */
  List<ReviewImage> findByReview_ReviewIdInOrderByDisplayOrderAsc(Collection<Long> reviewIds);
}
