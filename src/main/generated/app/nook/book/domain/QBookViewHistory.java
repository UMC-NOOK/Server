package app.nook.book.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBookViewHistory is a Querydsl query type for BookViewHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBookViewHistory extends EntityPathBase<BookViewHistory> {

    private static final long serialVersionUID = 1067873789L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBookViewHistory bookViewHistory = new QBookViewHistory("bookViewHistory");

    public final app.nook.global.common.QBaseEntity _super = new app.nook.global.common.QBaseEntity(this);

    public final QBook book;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final app.nook.user.domain.QUser user;

    public QBookViewHistory(String variable) {
        this(BookViewHistory.class, forVariable(variable), INITS);
    }

    public QBookViewHistory(Path<? extends BookViewHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBookViewHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBookViewHistory(PathMetadata metadata, PathInits inits) {
        this(BookViewHistory.class, metadata, inits);
    }

    public QBookViewHistory(Class<? extends BookViewHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.book = inits.isInitialized("book") ? new QBook(forProperty("book"), inits.get("book")) : null;
        this.user = inits.isInitialized("user") ? new app.nook.user.domain.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

