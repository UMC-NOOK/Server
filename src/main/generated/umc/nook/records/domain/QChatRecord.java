package umc.nook.records.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatRecord is a Querydsl query type for ChatRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRecord extends EntityPathBase<ChatRecord> {

    private static final long serialVersionUID = 305337049L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatRecord chatRecord = new QChatRecord("chatRecord");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    public final umc.nook.bookshelves.domain.QUserBookShelf bookshelf;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final EnumPath<ChatType> role = createEnum("role", ChatType.class);

    public QChatRecord(String variable) {
        this(ChatRecord.class, forVariable(variable), INITS);
    }

    public QChatRecord(Path<? extends ChatRecord> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatRecord(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatRecord(PathMetadata metadata, PathInits inits) {
        this(ChatRecord.class, metadata, inits);
    }

    public QChatRecord(Class<? extends ChatRecord> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.bookshelf = inits.isInitialized("bookshelf") ? new umc.nook.bookshelves.domain.QUserBookShelf(forProperty("bookshelf"), inits.get("bookshelf")) : null;
    }

}

