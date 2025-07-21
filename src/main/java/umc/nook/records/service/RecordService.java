package umc.nook.records.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.nook.book.domain.Book;
import umc.nook.book.repository.BookRepository;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.records.domain.BookRecord;
import umc.nook.records.domain.ChatRecord;
import umc.nook.records.domain.ChatType;
import umc.nook.records.domain.RecordType;
import umc.nook.records.dto.ChatDTO;
import umc.nook.records.dto.GptDTO;
import umc.nook.records.dto.RecordDTO;
import umc.nook.records.gpt.GptService;
import umc.nook.records.repository.BookRecordRepository;
import umc.nook.records.repository.ChatRecordRepository;
import umc.nook.users.domain.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final GptService gptService;
    private final UserBookshelfRepository userBookshelfRepository;

    private final BookRecordRepository bookRecordRepository;

    private final ChatRecordRepository chatRecordRepository;

    private final BookRepository bookRepository;

    @Transactional
    public ChatDTO.ChatResponseDTO saveChatMessage(User user, ChatDTO.ChatRequestDTO requestDTO) throws JsonProcessingException {
        Book book = bookRepository.findByBookId(requestDTO.getBookId());
        if (book==null)
            throw new CustomException(ErrorCode.BOOK_NOT_EXIST);
        UserBookShelf userBook = userBookshelfRepository.findByUserAndBook(user,book);
        if (userBook==null)
            throw new CustomException(ErrorCode.BOOK_NOT_FOUND);

        // 사용자 메시지 저장
        ChatRecord userMessage = requestDTO.toEntity(ChatType.USER, userBook);
        chatRecordRepository.save(userMessage);

        // GPT 메시지 저장
        GptDTO.ChatRecordDTO gptResponse = gptService.getAssistantMsg(
                requestDTO.getBookId(),
                user.getNickname(),
                book.getAuthor(),
                book.getTitle(),
                requestDTO.getMessage()
        );
        ChatType gptChatType = gptResponse.isEssay() ? ChatType.COMMENT : ChatType.SYSTEM;
        ChatRecord gptMessage = requestDTO.toEntity(gptChatType,userBook);
        chatRecordRepository.save(gptMessage);
        return new ChatDTO.ChatResponseDTO(gptMessage);
    }


    @Transactional
    public List<ChatDTO.ChatResponseDTO> viewChatMessages(User user, Long bookId) {
        // 책 조회
        Book book = bookRepository.findByBookId(bookId);
        if (book == null)
            throw new CustomException(ErrorCode.BOOK_NOT_EXIST);

        // 유저의 해당 책 서재 확인
        UserBookShelf userBook = userBookshelfRepository.findByUserAndBook(user, book);
        if (userBook == null)
            throw new CustomException(ErrorCode.BOOK_NOT_FOUND);

        // 채팅 기록 조회 (시간 순 정렬)
        List<ChatRecord> chatRecords = chatRecordRepository.findByBookshelfOrderByCreatedAtAsc(userBook);

        // DTO로 변환
        return chatRecords.stream()
                .map(ChatDTO.ChatResponseDTO::new)
                .toList();
    }

    @Transactional
    public RecordDTO.SentenceResponseDTO saveSentence(User user, RecordDTO.RecordRequestDTO requestDTO) {
        Book book = bookRepository.findByBookId(requestDTO.getBookId());
        UserBookShelf userBookShelf = userBookshelfRepository.findByUserAndBook(user, book);
        BookRecord sentence = requestDTO.toEntity(userBookShelf);
        sentence.setRecordType(RecordType.RECORD);
        bookRecordRepository.save(sentence);
        return new RecordDTO.SentenceResponseDTO(sentence);
    }

    @Transactional
    public RecordDTO.CommentResponseDTO saveCommentary(User user, RecordDTO.CommentRequestDTO requestDTO) {
        BookRecord parent = bookRecordRepository.findById(requestDTO.getParentRecordId())
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        UserBookShelf userBookShelf = parent.getBookshelf();
        BookRecord comment = requestDTO.toEntity(userBookShelf, parent);
        comment.setRecordType(RecordType.COMMENTARY);
        bookRecordRepository.save(comment);
        return new RecordDTO.CommentResponseDTO(comment);
    }

    @Transactional
    public List<RecordDTO.RecordResponseDTO> viewRecordsByBookId(User user, Long bookId) {
        Book book = bookRepository.findByBookId(bookId);
        UserBookShelf userBookShelf = userBookshelfRepository.findByUserAndBook(user, book);
        List<BookRecord> sentenceList = bookRecordRepository.findAllByBookshelfAndParentIsNullOrderByCreatedDateAsc(userBookShelf);
        return sentenceList.stream()
                .map(RecordDTO.RecordResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecordDTO.SentenceResponseDTO updateSentence(User user, RecordDTO.RecordUpdateRequestDTO updateRequestDTO) {
        BookRecord record = bookRecordRepository.findById(updateRequestDTO.getRecordId())
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        record.updateRecord(updateRequestDTO.getPage(), updateRequestDTO.getContent());
        return new RecordDTO.SentenceResponseDTO(record);
    }

    @Transactional
    public RecordDTO.CommentResponseDTO updateComment(User user, Long commentId, RecordDTO.CommentUpdateRequestDTO updateRequestDTO) {
        BookRecord comment = bookRecordRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        comment.updateCommentary(updateRequestDTO.getContent());
        return new RecordDTO.CommentResponseDTO(comment);
    }

    @Transactional
    public void deleteComment(User user, Long commentId) {
        BookRecord comment = bookRecordRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        bookRecordRepository.delete(comment);
    }

    @Transactional
    public void deleteRecord(User user, Long recordId) {
        BookRecord record = bookRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        // 댓글 먼저 삭제
        List<BookRecord> comments = bookRecordRepository.findAllByParent(record);
        bookRecordRepository.deleteAll(comments);
        // 본문 삭제
        bookRecordRepository.delete(record);
    }


}
