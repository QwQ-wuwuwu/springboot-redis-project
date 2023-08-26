package com.example.repository;

import com.example.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,String> {
    @Query(value = "from Ticket where origin=:origin and departure=:departure")
    List<Ticket> getTickets(@Param("origin") String origin,@Param("departure") String departure);
    @Query("from Ticket where id=:id")
    Ticket getTicket(@Param("id") String id);
}
