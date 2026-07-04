package juribook.lawyer_service.event;

import juribook.lawyer_service.entity.Review;

public interface ReviewEventPublisher {

    void publishReviewCreated(Review review);
}