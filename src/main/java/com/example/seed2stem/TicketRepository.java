package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Most recently submitted tickets first — for the developer's full list. */
    List<Ticket> findAllByOrderBySubmittedAtDesc();

    /** Open tickets only, oldest first — the developer's natural to-do order. */
    List<Ticket> findByStatusOrderBySubmittedAtAsc(TicketStatus status);

    /** Count open tickets — for dashboard badges if wanted later. */
    long countByStatus(TicketStatus status);
}
