package umc.nook.records.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBookRecord is a Querydsl query type for BookRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBookRecord extends EntityPathBase<BookRecord> {

    private static final long serialVersionUID = -33486422L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBookRecord bookRecord = new QBookRecord("bookRecord");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    public final umc.nook.bookshelves.domain.QUserBookShelf bookshelf;

    public final ListPath<BookRecord, QBookRecord> comments = this.<BookRecord, QBookRecord>createList("comments", BookRecord.class, QBookRecord.class, PathInits.DIRECT2);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath page = createString("page");

    public final QBookRecord parent;

    public final EnumPath<RecordType> recordType = createEnum("recordType", RecordType.class);

    public QBookRecord(String variable) {
        this(BookRecord.class, forVariable(variable), INITS);
    }

    public QBookRecord(Path<? extends BookRecord> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBookRecord(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBookRecord(PathMetadata metadata, PathInits inits) {
        this(BookRecord.class, metadata, inits);
    }

    public QBookRecord(Class<? extends BookRecord> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.bookshelf = inits.isInitialized("bookshelf") ? new umc.nook.bookshelves.domain.QUserBookShelf(forProperty("bookshelf"), inits.get("bookshelf")) : null;
        this.parent = inits.isInitialized("parent") ? new QBookRecord(forProperty("parent"), inits.get("parent")) : null;
    }

}

