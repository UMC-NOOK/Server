package app.nook.library.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLibrary is a Querydsl query type for Library
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLibrary extends EntityPathBase<Library> {

    private static final long serialVersionUID = -1510652312L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLibrary library = new QLibrary("library");

    public final app.nook.global.common.QBaseEntity _super = new app.nook.global.common.QBaseEntity(this);

    public final app.nook.book.domain.QBook book;

    public final ListPath<app.nook.timeline.domain.BookTimeLine, app.nook.timeline.domain.QBookTimeLine> bookTimeLines = this.<app.nook.timeline.domain.BookTimeLine, app.nook.timeline.domain.QBookTimeLine>createList("bookTimeLines", app.nook.timeline.domain.BookTimeLine.class, app.nook.timeline.domain.QBookTimeLine.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final DatePath<java.time.LocalDate> endedAt = createDate("endedAt", java.time.LocalDate.class);

    public final ListPath<app.nook.focus.domain.Focus, app.nook.focus.domain.QFocus> focuses = this.<app.nook.focus.domain.Focus, app.nook.focus.domain.QFocus>createList("focuses", app.nook.focus.domain.Focus.class, app.nook.focus.domain.QFocus.class, PathInits.DIRECT2);

    public final NumberPath<Long> focusSec = createNumber("focusSec", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final NumberPath<Integer> page = createNumber("page", Integer.class);

    public final EnumPath<app.nook.library.domain.enums.ReadingStatus> readingStatus = createEnum("readingStatus", app.nook.library.domain.enums.ReadingStatus.class);

    public final DatePath<java.time.LocalDate> startedAt = createDate("startedAt", java.time.LocalDate.class);

    public final app.nook.user.domain.QUser user;

    public QLibrary(String variable) {
        this(Library.class, forVariable(variable), INITS);
    }

    public QLibrary(Path<? extends Library> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLibrary(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLibrary(PathMetadata metadata, PathInits inits) {
        this(Library.class, metadata, inits);
    }

    public QLibrary(Class<? extends Library> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.book = inits.isInitialized("book") ? new app.nook.book.domain.QBook(forProperty("book"), inits.get("book")) : null;
        this.user = inits.isInitialized("user") ? new app.nook.user.domain.QUser(forProperty("user")) : null;
    }

}

