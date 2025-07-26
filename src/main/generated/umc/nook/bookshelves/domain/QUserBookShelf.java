package umc.nook.bookshelves.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserBookShelf is a Querydsl query type for UserBookShelf
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserBookShelf extends EntityPathBase<UserBookShelf> {

    private static final long serialVersionUID = -966357975L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserBookShelf userBookShelf = new QUserBookShelf("userBookShelf");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    public final umc.nook.book.domain.QBook book;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final EnumPath<ReadingStatus> readingStatus = createEnum("readingStatus", ReadingStatus.class);

    public final DatePath<java.time.LocalDate> recordedAt = createDate("recordedAt", java.time.LocalDate.class);

    public final ListPath<umc.nook.records.domain.BookRecord, umc.nook.records.domain.QBookRecord> records = this.<umc.nook.records.domain.BookRecord, umc.nook.records.domain.QBookRecord>createList("records", umc.nook.records.domain.BookRecord.class, umc.nook.records.domain.QBookRecord.class, PathInits.DIRECT2);

    public final umc.nook.users.domain.QUser user;

    public QUserBookShelf(String variable) {
        this(UserBookShelf.class, forVariable(variable), INITS);
    }

    public QUserBookShelf(Path<? extends UserBookShelf> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserBookShelf(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserBookShelf(PathMetadata metadata, PathInits inits) {
        this(UserBookShelf.class, metadata, inits);
    }

    public QUserBookShelf(Class<? extends UserBookShelf> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.book = inits.isInitialized("book") ? new umc.nook.book.domain.QBook(forProperty("book"), inits.get("book")) : null;
        this.user = inits.isInitialized("user") ? new umc.nook.users.domain.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

