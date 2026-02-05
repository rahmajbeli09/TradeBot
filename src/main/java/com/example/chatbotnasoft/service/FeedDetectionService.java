package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.entity.Feed;
import com.example.chatbotnasoft.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedDetectionService {

    private final FeedRepository feedRepository;

    public boolean isMsgTypeKnown(String msgType) {
        log.debug("🔍 Vérification du msg-type dans MongoDB: '{}'", msgType);
        
        Optional<Feed> feed = feedRepository.findByMsgTypeAndIsActive(msgType, true);
        
        boolean isKnown = feed.isPresent();
        
        if (isKnown) {
            log.debug("✅ Msg-type '{}' connu dans la base de données", msgType);
        } else {
            log.info("❌ Msg-type '{}' inconnu - anonymisation requise", msgType);
        }
        
        return isKnown;
    }

    public List<String> getKnownMsgTypes(List<String> msgTypes) {
        log.debug("🔍 Recherche de {} msg-types dans la base de données", msgTypes.size());
        
        List<Feed> knownFeeds = feedRepository.findByMsgTypeInAndIsActive(msgTypes, true);
        List<String> knownMsgTypes = knownFeeds.stream()
                .map(Feed::getMsgType)
                .toList();
        
        log.debug("✅ {} msg-types connus sur {} recherchés", knownMsgTypes.size(), msgTypes.size());
        
        return knownMsgTypes;
    }

    public Feed createFeedType(String msgType, String description) {
        log.info("📝 Création d'un nouveau msg-type: '{}' - '{}'", msgType, description);
        
        Feed newFeed = new Feed(msgType, description);
        Feed saved = feedRepository.save(newFeed);
        
        log.info("✅ Msg-type '{}' créé avec ID: {}", msgType, saved.getId());
        return saved;
    }

    public List<Feed> getAllActiveFeeds() {
        return feedRepository.findByIsActive(true);
    }

    public long countActiveFeeds() {
        return feedRepository.findByIsActive(true).size();
    }
}
