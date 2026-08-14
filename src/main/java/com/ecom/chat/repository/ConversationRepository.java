package com.ecom.chat.repository;

import com.ecom.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByCustomerId(String customerId);

    Page<Conversation> findAllByOrderByLastMessageAtDesc(Pageable pageable);
}
