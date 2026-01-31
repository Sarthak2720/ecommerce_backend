package com.styliste.repository;

import com.styliste.entity.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, Long> {
    // Fetch all timeline events for a specific order, sorted by time
    List<OrderTimeline> findByOrderIdOrderByTimestampAsc(Long orderId);
}