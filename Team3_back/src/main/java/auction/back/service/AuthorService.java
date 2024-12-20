package auction.back.service;

import auction.back.domain.Follow;
import auction.back.domain.Picture;
import auction.back.domain.User;
import auction.back.dto.request.AuthorFollowRequestDto;
import auction.back.dto.response.AuthorDetailResponseDto;
import auction.back.dto.response.AuthorViewResponseDto;
import auction.back.repository.AuthorRepository;
import auction.back.repository.FollowRepository;
import auction.back.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public String authorFollow(AuthorFollowRequestDto authorFollowRequestDto) {
        User user = userRepository.findById(authorFollowRequestDto.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        User author = userRepository.findById(authorFollowRequestDto.authorId())
                .orElseThrow(() -> new EntityNotFoundException("Author not found"));

        if (!author.isAuthor()) {
            throw new IllegalArgumentException("Target user is not an author");
        }

        Optional<Follow> existingFollow = followRepository
                .findByUserIdAndAuthorId(authorFollowRequestDto.userId(), authorFollowRequestDto.authorId());

        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            return "팔로우가 취소되었습니다.";
        } else {
            Follow newFollow = Follow.builder()
                    .user(user)
                    .author(author)
                    .build();
            followRepository.save(newFollow);
            return "팔로우가 추가되었습니다.";
        }
    }

    public List<AuthorViewResponseDto> mainView(Boolean sortByFollow) {
        List<User> authors;
        if (Boolean.TRUE.equals(sortByFollow)) {
            authors = authorRepository.findAllAuthorsOrderByFollowCountAndName();
        } else {
            authors = authorRepository.findAllAuthorsOrderByName();
        }

        return authors.stream()
                .map(AuthorViewResponseDto::of)
                .collect(Collectors.toList());
    }

    public AuthorDetailResponseDto authorDetailView(Long userId) {
        User author = authorRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Author not found with id: " + userId));

        if (!author.isAuthor()) {
            throw new IllegalArgumentException("User with id: " + userId + " is not an author");
        }

        int followersCount = authorRepository.countFollowersByAuthorId(userId);
        List<Picture> recentPictures = authorRepository.findRecentPicturesByAuthorId(userId, PageRequest.of(0, 5));

        return AuthorDetailResponseDto.of(author, followersCount, recentPictures);
    }
}
