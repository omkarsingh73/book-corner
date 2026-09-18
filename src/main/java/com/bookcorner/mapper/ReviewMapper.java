package com.bookcorner.mapper;

import com.bookcorner.dto.review.ReviewDto;
import com.bookcorner.dto.review.SubmitReviewRequest;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.review.ReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Customer Reviews and ratings.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface ReviewMapper {

    @Mapping(target = "reviewId", source = "id")
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "authorName", source = "user", qualifiedByName = "formatReviewerName")
    @Mapping(target = "rating", source = "ratingStars")
    @Mapping(target = "reviewTitle", source = "reviewTitle")
    @Mapping(target = "reviewBody", source = "reviewBody")
    @Mapping(target = "helpfulVotes", source = "helpfulVotesCount")
    ReviewDto toReviewDto(ReviewEntity entity);

    List<ReviewDto> toReviewDtoList(List<ReviewEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "ratingStars", source = "rating")
    @Mapping(target = "isVerifiedPurchase", constant = "false")
    @Mapping(target = "helpfulVotesCount", constant = "0")
    @Mapping(target = "unhelpfulVotesCount", constant = "0")
    @Mapping(target = "moderationStatus", constant = "APPROVED")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    ReviewEntity toReviewEntity(SubmitReviewRequest request);

    @Named("formatReviewerName")
    default String formatReviewerName(UserEntity user) {
        if (user == null) return "Verified Customer";
        return user.getFirstName() + " " + (user.getLastName() != null && !user.getLastName().isBlank() ? user.getLastName().charAt(0) + "." : "");
    }
}
