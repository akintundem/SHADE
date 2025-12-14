package eventplanner.features.event.service;

import eventplanner.common.storage.s3.services.S3StorageService;
import eventplanner.features.event.dto.request.EventPostCreateRequest;
import eventplanner.features.event.dto.request.EventPostUpdateRequest;
import eventplanner.features.event.dto.response.EventPostResponse;
import eventplanner.features.event.entity.EventPost;
import eventplanner.features.event.entity.EventStoredObject;
import eventplanner.features.event.repository.EventPostRepository;
import eventplanner.features.event.repository.EventStoredObjectRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EventPostService {

    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final String EVENT_BUCKET_ALIAS = "event";

    private final EventAccessControlService accessControlService;
    private final EventPostRepository postRepository;
    private final EventStoredObjectRepository storedObjectRepository;
    private final S3StorageService storageService;

    public EventPostResponse create(UUID eventId, UserPrincipal principal, EventPostCreateRequest request) {
        accessControlService.requireMediaUpload(principal, eventId);
        EventPost.PostType type = parseType(request.getType());

        EventPost post = new EventPost();
        post.setEventId(eventId);
        post.setPostType(type);
        post.setContent(request.getContent());
        post.setCreatedBy(principal != null ? principal.getId() : null);

        if (type == EventPost.PostType.IMAGE || type == EventPost.PostType.VIDEO) {
            if (request.getMediaObjectId() == null) {
                throw new IllegalArgumentException("mediaObjectId is required for " + type);
            }
            EventStoredObject obj = requireStoredObject(eventId, request.getMediaObjectId());
            post.setMediaObjectId(obj.getId());
        }

        return toResponse(postRepository.save(post), eventId);
    }

    public List<EventPostResponse> list(UUID eventId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        return postRepository.findByEventIdOrderByCreatedAtDesc(eventId)
            .stream()
            .map(p -> toResponse(p, eventId))
            .collect(Collectors.toList());
    }

    public EventPostResponse get(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaView(principal, eventId);
        EventPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!eventId.equals(post.getEventId())) {
            throw new IllegalArgumentException("Post not found");
        }
        return toResponse(post, eventId);
    }

    public EventPostResponse update(UUID eventId, UUID postId, UserPrincipal principal, EventPostUpdateRequest request) {
        accessControlService.requireMediaManage(principal, eventId);
        EventPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!eventId.equals(post.getEventId())) {
            throw new IllegalArgumentException("Post not found");
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getMediaObjectId() != null) {
            EventStoredObject obj = requireStoredObject(eventId, request.getMediaObjectId());
            post.setMediaObjectId(obj.getId());
        }
        return toResponse(post, eventId);
    }

    public void delete(UUID eventId, UUID postId, UserPrincipal principal) {
        accessControlService.requireMediaManage(principal, eventId);
        EventPost post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!eventId.equals(post.getEventId())) {
            throw new IllegalArgumentException("Post not found");
        }
        postRepository.delete(post);
    }

    private EventPostResponse toResponse(EventPost post, UUID eventId) {
        EventPostResponse resp = new EventPostResponse();
        resp.setId(post.getId());
        resp.setEventId(eventId);
        resp.setType(post.getPostType().name());
        resp.setContent(post.getContent());
        resp.setMediaObjectId(post.getMediaObjectId());
        resp.setCreatedBy(post.getCreatedBy());
        resp.setCreatedAt(post.getCreatedAt());
        resp.setUpdatedAt(post.getUpdatedAt());

        if (post.getMediaObjectId() != null) {
            EventStoredObject obj = storedObjectRepository.findById(post.getMediaObjectId()).orElse(null);
            if (obj != null && eventId.equals(obj.getEventId())) {
                URL presignedGet = storageService.generatePresignedGetUrl(EVENT_BUCKET_ALIAS, obj.getObjectKey(), DOWNLOAD_URL_TTL);
                resp.setMediaUrl(presignedGet.toString());
            }
        }
        return resp;
    }

    private EventStoredObject requireStoredObject(UUID eventId, UUID objectId) {
        EventStoredObject obj = storedObjectRepository.findById(objectId)
            .orElseThrow(() -> new IllegalArgumentException("Media object not found"));
        if (!eventId.equals(obj.getEventId())) {
            throw new IllegalArgumentException("Media object not found");
        }
        return obj;
    }

    private EventPost.PostType parseType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return EventPost.PostType.TEXT;
        }
        try {
            return EventPost.PostType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid post type");
        }
    }
}


