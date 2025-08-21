package umc.nook.records.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.book.domain.Book;
import umc.nook.book.repository.BookRepository;
import umc.nook.bookshelves.domain.UserBookShelf;
import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.records.domain.*;
import umc.nook.records.dto.ChatDTO;
import umc.nook.records.dto.GptDTO;
import umc.nook.records.dto.RecordDTO;
import umc.nook.records.gpt.GptService;
import umc.nook.records.repository.BookRecordRepository;
import umc.nook.records.repository.ChatRecordRepository;
import umc.nook.users.domain.User;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.abbreviate;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecordService {

    private final GptService gptService;
    private final UserBookshelfRepository userBookshelfRepository;

    private final BookRecordRepository bookRecordRepository;

    private final ChatRecordRepository chatRecordRepository;

    private final BookRepository bookRepository;


    // 눅톡 메세지 전송
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
        log.info("[CHAT] 사용자 메시지 저장됨: userId={}, bookId={}, content={}",
                user.getUserId(), book.getBookId(), abbreviate(userMessage.getContent(), 200));

        // GPT 메시지 저장
        GptDTO.ChatRecordDTO gptResponse = gptService.getAssistantMsg(
                userBook.getId(),
                user.getNickname(),
                book.getAuthor(),
                book.getTitle(),
                requestDTO.getMessage()
        );

        log.info("[CHAT] GPT 응답 수신: isEssay={}, contentHead={}",
                gptResponse.getIsEssay(), abbreviate(gptResponse.getContent(), 200));

        ChatType gptChatType = null;
        if (gptResponse.getIsEssay()== "true")
            gptChatType = ChatType.COMMENT;
        else if (gptResponse.getIsEssay() == "false")
            gptChatType = ChatType.SYSTEM;
        log.info("[CHAT] GPT 메시지 role 결정됨: {}", gptChatType);

        ChatRecord gptMessage = ChatRecord.builder()
                .bookshelf(userBook)
                .role(gptChatType)
                .content(gptResponse.getContent())
                .build();
        chatRecordRepository.save(gptMessage);

        log.info("[CHAT] GPT 메시지 저장 완료: bookshelfId={}, role={}, contentHead={}",
                userBook.getId(), gptMessage.getRole(), abbreviate(gptMessage.getContent(), 200));

        return new ChatDTO.ChatResponseDTO(gptMessage);
    }

    // 선택한 책 눅톡 기록 조회
    @Transactional(readOnly=true)
    public List<ChatDTO.ChatResponseDTO> viewChatMessages(User user, Long bookId) {
        Book book = bookRepository.findByBookId(bookId);
        if (book == null)
            throw new CustomException(ErrorCode.BOOK_NOT_EXIST);
        UserBookShelf userBook = userBookshelfRepository.findByUserAndBook(user, book);
        if (userBook == null)
            throw new CustomException(ErrorCode.BOOK_NOT_FOUND);
        List<ChatRecord> chatRecords = chatRecordRepository.findByBookshelfOrderByCreatedDateAsc(userBook);
        return chatRecords.stream()
                .map(ChatDTO.ChatResponseDTO::new)
                .toList();
    }

    // 독서 문장 등록
    @Transactional
    public RecordDTO.SentenceResponseDTO saveSentence(User user, RecordDTO.RecordRequestDTO requestDTO) {
        Book book = bookRepository.findByBookId(requestDTO.getBookId());
        UserBookShelf userBookShelf = userBookshelfRepository.findByUserAndBook(user, book);
        if (userBookShelf == null) {
            throw new CustomException(ErrorCode.BOOK_NOT_FOUND);
        }
        BookRecord sentence = requestDTO.toEntity(userBookShelf);
        sentence.setRecordType(RecordType.RECORD);
        bookRecordRepository.save(sentence);
        return new RecordDTO.SentenceResponseDTO(sentence);
    }

    // 독서 감상 등록
    @Transactional
    public RecordDTO.CommentResponseDTO saveCommentary(User user, RecordDTO.CommentRequestDTO requestDTO) {
        BookRecord parent = null;
        if (requestDTO.getParentRecordId()!=null){
            parent = bookRecordRepository.findById(requestDTO.getParentRecordId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        }
        Book book = bookRepository.findByBookId(requestDTO.getBookId());
        if (book == null)
            throw new CustomException(ErrorCode.BOOK_NOT_EXIST);
        UserBookShelf userBookShelf = userBookshelfRepository.findByUserAndBook(user,book);
        BookRecord comment = requestDTO.toEntity(userBookShelf, parent);
        comment.setRecordType(RecordType.COMMENTARY);
        bookRecordRepository.save(comment);
        return new RecordDTO.CommentResponseDTO(comment);
    }

    // 선택한 책 독서 기록 조회
    @Transactional(readOnly = true)
    public List<RecordDTO.RecordResponseDTO> viewRecordsByBookId(User user, Long bookId) {
        Book book = bookRepository.findByBookId(bookId);
        UserBookShelf userBookShelf = userBookshelfRepository.findByUserAndBook(user, book);
        List<BookRecord> sentenceList = bookRecordRepository.findAllByBookshelfAndParentIsNullOrderByCreatedDateAsc(userBookShelf);
        return sentenceList.stream()
                .map(RecordDTO.RecordResponseDTO::new)
                .collect(Collectors.toList());
    }

    // 문장 수정
    @Transactional
    public RecordDTO.SentenceResponseDTO updateSentence(
            User user,
            RecordDTO.RecordUpdateRequestDTO updateRequestDTO) {
        BookRecord record = bookRecordRepository.findById(updateRequestDTO.getRecordId())
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        if (record.getRecordType()!=RecordType.RECORD)
            throw new CustomException(ErrorCode.INVALID_RECORD_TYPE);
        record.updateRecord(updateRequestDTO.getPage(), updateRequestDTO.getContent());
        return new RecordDTO.SentenceResponseDTO(record);
    }

    // 독서 감상 수정
    @Transactional
    public RecordDTO.CommentResponseDTO updateComment(
            User user,
            Long commentId,
            RecordDTO.CommentUpdateRequestDTO updateRequestDTO) {
        BookRecord comment = bookRecordRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        if (comment.getRecordType()!=RecordType.COMMENTARY)
            throw new CustomException(ErrorCode.INVALID_RECORD_TYPE);
        comment.updateCommentary(updateRequestDTO.getContent());
        return new RecordDTO.CommentResponseDTO(comment);
    }

    // 독서 감상 삭제
    @Transactional
    public void deleteComment(User user, Long commentId) {
        BookRecord comment = bookRecordRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        if (comment.getRecordType()!=RecordType.COMMENTARY)
            throw new CustomException(ErrorCode.INVALID_RECORD_TYPE);
        bookRecordRepository.delete(comment);
    }

    // 독서 문장 삭제
    @Transactional
    public void deleteRecord(User user, Long recordId) {
        BookRecord record = bookRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        if (record.getRecordType()!=RecordType.RECORD)
            throw new CustomException(ErrorCode.INVALID_RECORD_TYPE);
        // 감상 먼저 삭제
        List<BookRecord> comments = bookRecordRepository.findAllByParent(record);
        bookRecordRepository.deleteAll(comments);
        // 본문 삭제
        bookRecordRepository.delete(record);
    }

    // 독서 기록률 조회
    @Transactional(readOnly = true)
    public RecordDTO.MonthlyRecordRateResponseDTO viewRecordRate(User user, Year year) {
        return bookRecordRepository.viewRecordRate(user,year);
    }


    // 눅톡으로 생성된 감상을 내 감상으로 붙여넣기
    @Transactional
    public RecordDTO.CommentResponseDTO saveCommentFromChatRecord(User user, Long chatRecordId) {
        // 채팅 기록 조회
        ChatRecord record = chatRecordRepository.findById(chatRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_RECORD_NOT_FOUND));
        if (record.getRole() != ChatType.COMMENT)
            throw new CustomException(ErrorCode.CHAT_RECORD_MUST_BE_COMMENT);
        // 내용 추출
        String content = record.getContent();
        // BookRecord 생성 및 저장
        BookRecord newComment =  BookRecord.builder()
                .bookshelf(record.getBookshelf())
                .content(content)
                .recordType(RecordType.COMMENTARY)
                .page(null)
                .build();
        bookRecordRepository.save(newComment);
        return new RecordDTO.CommentResponseDTO(newComment);
    }

    // 가장 최근 기록한 책 정보 조회
    @Transactional(readOnly = true)
    public BookShelfDTO.BookThumbnail viewRecentlyRecordedBook(User user) {
        return bookRecordRepository.viewRecentRecordedBook(user)
                .map(b -> new BookShelfDTO.BookThumbnail(
                        b.getBookId(),
                        b.getTitle(),
                        b.getThumbnailUrl()))
                .orElseThrow(()->new CustomException(ErrorCode.RECORD_NOT_EXIST));
    }


    private void validateBookRecord(Long recordId) {
        BookRecord bookRecord = bookRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
    }
}